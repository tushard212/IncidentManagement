package com.incidenthub.service;

import com.incidenthub.model.Attachment;
import com.incidenthub.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

  private final AttachmentRepository attachmentRepository;

  @Value("${app.upload.dir:uploads}")
  private String uploadDir;

  private static final List<String> ALLOWED_TYPES = List.of(
      "image/png", "image/jpeg", "image/gif", "image/webp",
      "application/pdf", "text/plain", "text/csv",
      "application/json", "application/xml");

  public Attachment storeFile(MultipartFile file, Long incidentId, String username) throws IOException {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
      throw new IllegalArgumentException("File type not allowed: " + contentType);
    }

    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    Files.createDirectories(uploadPath);

    String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename()
        .replaceAll("[^a-zA-Z0-9._-]", "_");

    Path targetLocation = uploadPath.resolve(storedName);
    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

    Attachment attachment = Attachment.builder()
        .fileName(file.getOriginalFilename())
        .storedName(storedName)
        .contentType(contentType)
        .fileSize(file.getSize())
        .incidentId(incidentId)
        .uploadedBy(username)
        .uploadedAt(LocalDateTime.now())
        .build();

    log.info("File stored: {} for incident #{} by {}", storedName, incidentId, username);
    return attachmentRepository.save(attachment);
  }

  public Resource loadFile(Long attachmentId) throws MalformedURLException {
    Attachment attachment = attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new RuntimeException("Attachment not found"));

    Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(attachment.getStoredName());
    Resource resource = new UrlResource(filePath.toUri());

    if (!resource.exists()) {
      throw new RuntimeException("File not found on disk: " + attachment.getStoredName());
    }

    return resource;
  }

  public Attachment getAttachment(Long attachmentId) {
    return attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new RuntimeException("Attachment not found"));
  }

  public List<Attachment> getAttachmentsByIncident(Long incidentId) {
    return attachmentRepository.findByIncidentIdOrderByUploadedAtDesc(incidentId);
  }

  public void deleteAttachment(Long attachmentId, String username) throws IOException {
    Attachment attachment = attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new RuntimeException("Attachment not found"));

    if (!attachment.getUploadedBy().equals(username)) {
      throw new RuntimeException("Access denied");
    }

    Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(attachment.getStoredName());
    Files.deleteIfExists(filePath);
    attachmentRepository.delete(attachment);
    log.info("Attachment deleted: {} by {}", attachment.getFileName(), username);
  }
}
