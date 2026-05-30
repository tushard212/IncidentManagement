package com.incidenthub.service;

import com.incidenthub.model.Incident;
import com.incidenthub.model.NotificationLog;
import com.incidenthub.model.User;
import com.incidenthub.repository.NotificationLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final NotificationLogRepository notificationLogRepository;

  @Value("${app.mail.from:noreply@incidenthub.com}")
  private String fromEmail;

  @Value("${app.mail.enabled:false}")
  private boolean mailEnabled;

  @Async
  public void sendIncidentCreatedNotification(Incident incident, User assignee) {
    if (assignee.getEmail() == null)
      return;

    String subject = "[IncidentHub] New Incident #" + incident.getId() + " - " + incident.getTitle();

    if (!mailEnabled) {
      saveLog(assignee.getEmail(), assignee.getFullName(), subject,
          NotificationLog.NotificationType.INCIDENT_CREATED, NotificationLog.NotificationStatus.SENT, incident, null);
      log.info("Notification logged (mail disabled) - {} to {}", subject, assignee.getEmail());
      return;
    }

    Context context = new Context();
    context.setVariable("incident", incident);
    context.setVariable("assignee", assignee);
    context.setVariable("action", "created");

    String html = templateEngine.process("incident-notification", context);
    sendEmail(assignee.getEmail(), assignee.getFullName(), subject,
        html, NotificationLog.NotificationType.INCIDENT_CREATED, incident);
  }

  @Async
  public void sendIncidentEscalatedNotification(Incident incident, User assignee) {
    if (assignee.getEmail() == null)
      return;

    String subject = "[IncidentHub] SLA BREACH - Incident #" + incident.getId() + " Escalated";

    if (!mailEnabled) {
      saveLog(assignee.getEmail(), assignee.getFullName(), subject,
          NotificationLog.NotificationType.INCIDENT_ESCALATED, NotificationLog.NotificationStatus.SENT, incident, null);
      log.info("Notification logged (mail disabled) - {} to {}", subject, assignee.getEmail());
      return;
    }

    Context context = new Context();
    context.setVariable("incident", incident);
    context.setVariable("assignee", assignee);
    context.setVariable("action", "escalated");

    String html = templateEngine.process("incident-notification", context);
    sendEmail(assignee.getEmail(), assignee.getFullName(), subject,
        html, NotificationLog.NotificationType.INCIDENT_ESCALATED, incident);
  }

  @Async
  public void sendIncidentResolvedNotification(Incident incident, User reporter) {
    if (reporter.getEmail() == null)
      return;

    String subject = "[IncidentHub] Incident #" + incident.getId() + " Resolved";

    if (!mailEnabled) {
      saveLog(reporter.getEmail(), reporter.getFullName(), subject,
          NotificationLog.NotificationType.INCIDENT_RESOLVED, NotificationLog.NotificationStatus.SENT, incident, null);
      log.info("Notification logged (mail disabled) - {} to {}", subject, reporter.getEmail());
      return;
    }

    Context context = new Context();
    context.setVariable("incident", incident);
    context.setVariable("assignee", reporter);
    context.setVariable("action", "resolved");

    String html = templateEngine.process("incident-notification", context);
    sendEmail(reporter.getEmail(), reporter.getFullName(), subject,
        html, NotificationLog.NotificationType.INCIDENT_RESOLVED, incident);
  }

  private void sendEmail(String to, String recipientName, String subject, String htmlContent,
      NotificationLog.NotificationType type, Incident incident) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);
      mailSender.send(message);
      log.info("Email sent to {} - {}", to, subject);

      saveLog(to, recipientName, subject, type, NotificationLog.NotificationStatus.SENT, incident, null);
    } catch (MessagingException e) {
      log.error("Failed to send email to {}: {}", to, e.getMessage());
      saveLog(to, recipientName, subject, type, NotificationLog.NotificationStatus.FAILED, incident, e.getMessage());
    }
  }

  private void saveLog(String email, String name, String subject,
      NotificationLog.NotificationType type, NotificationLog.NotificationStatus status,
      Incident incident, String errorMessage) {
    try {
      NotificationLog logEntry = NotificationLog.builder()
          .recipientEmail(email)
          .recipientName(name)
          .subject(subject)
          .type(type)
          .status(status)
          .incidentId(incident.getId())
          .incidentTitle(incident.getTitle())
          .sentAt(LocalDateTime.now())
          .errorMessage(errorMessage)
          .build();
      notificationLogRepository.save(logEntry);
    } catch (Exception e) {
      log.error("Failed to save notification log: {}", e.getMessage());
    }
  }
}
