package com.incidenthub.service;

import com.incidenthub.model.Incident;
import com.incidenthub.model.User;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Value("${app.mail.from:noreply@incidenthub.com}")
  private String fromEmail;

  @Value("${app.mail.enabled:false}")
  private boolean mailEnabled;

  @Async
  public void sendIncidentCreatedNotification(Incident incident, User assignee) {
    if (!mailEnabled || assignee.getEmail() == null)
      return;

    Context context = new Context();
    context.setVariable("incident", incident);
    context.setVariable("assignee", assignee);
    context.setVariable("action", "created");

    String html = templateEngine.process("incident-notification", context);
    sendEmail(assignee.getEmail(), "[IncidentHub] New Incident #" + incident.getId() + " - " + incident.getTitle(),
        html);
  }

  @Async
  public void sendIncidentEscalatedNotification(Incident incident, User assignee) {
    if (!mailEnabled || assignee.getEmail() == null)
      return;

    Context context = new Context();
    context.setVariable("incident", incident);
    context.setVariable("assignee", assignee);
    context.setVariable("action", "escalated");

    String html = templateEngine.process("incident-notification", context);
    sendEmail(assignee.getEmail(), "[IncidentHub] SLA BREACH - Incident #" + incident.getId() + " Escalated", html);
  }

  @Async
  public void sendIncidentResolvedNotification(Incident incident, User reporter) {
    if (!mailEnabled || reporter.getEmail() == null)
      return;

    Context context = new Context();
    context.setVariable("incident", incident);
    context.setVariable("assignee", reporter);
    context.setVariable("action", "resolved");

    String html = templateEngine.process("incident-notification", context);
    sendEmail(reporter.getEmail(), "[IncidentHub] Incident #" + incident.getId() + " Resolved", html);
  }

  private void sendEmail(String to, String subject, String htmlContent) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);
      mailSender.send(message);
      log.info("Email sent to {} - {}", to, subject);
    } catch (MessagingException e) {
      log.error("Failed to send email to {}: {}", to, e.getMessage());
    }
  }
}
