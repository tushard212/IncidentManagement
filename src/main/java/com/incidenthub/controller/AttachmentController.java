package com.incidenthub.controller;

import com.incidenthub.model.Attachment;
import com.incidenthub.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents/{incidentId}/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "File attachments for incidents")
public class AttachmentController {

  private final FileStorageService fileStorageService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload file", description = "Upload a file attachment to an incident")
  public ResponseEntity<?> uploadFile(
      @PathVariable Long incidentId,
      @RequestParam("file") MultipartFile file,
      Authentication auth) {
    try {
      Attachment attachment = fileStorageService.storeFile(file, incidentId, auth.getName());
      return ResponseEntity.ok(Map.of(
          "id", attachment.getId(),
          "fileName", attachment.getFileName(),
          "contentType", attachment.getContentType(),
          "fileSize", attachment.getFileSize(),
          "uploadedAt", attachment.getUploadedAt().toString()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (IOException e) {
      return ResponseEntity.internalServerError().body(Map.of("error", "Failed to store file"));
    }
  }

  @GetMapping
  @Operation(summary = "List attachments", description = "Get all attachments for an incident")
  public ResponseEntity<List<Attachment>> getAttachments(@PathVariable Long incidentId) {
    return ResponseEntity.ok(fileStorageService.getAttachmentsByIncident(incidentId));
  }

  @GetMapping("/{attachmentId}/download")
  public ResponseEntity<Resource> downloadFile(
      @PathVariable Long incidentId,
      @PathVariable Long attachmentId) {
    try {
      Attachment attachment = fileStorageService.getAttachment(attachmentId);
      Resource resource = fileStorageService.loadFile(attachmentId);

      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(attachment.getContentType()))
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
          .body(resource);
    } catch (Exception e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{attachmentId}")
  public ResponseEntity<?> deleteFile(
      @PathVariable Long incidentId,
      @PathVariable Long attachmentId,
      Authentication auth) {
    try {
      fileStorageService.deleteAttachment(attachmentId, auth.getName());
      return ResponseEntity.ok(Map.of("message", "File deleted"));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
