package com.incidenthub.controller;

import com.incidenthub.model.ShortenedUrl;
import com.incidenthub.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

  private final UrlShortenerService urlShortenerService;

  @PostMapping("/api/urls/shorten")
  public ResponseEntity<?> shortenUrl(@RequestBody Map<String, Object> request, Authentication auth) {
    String originalUrl = (String) request.get("url");
    Integer expiryDays = request.get("expiryDays") != null ? ((Number) request.get("expiryDays")).intValue() : null;

    if (originalUrl == null || originalUrl.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
    }

    if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
      return ResponseEntity.badRequest().body(Map.of("error", "URL must start with http:// or https://"));
    }

    ShortenedUrl shortened = urlShortenerService.shortenUrl(originalUrl, auth.getName(), expiryDays);
    return ResponseEntity.ok(Map.of(
        "shortCode", shortened.getShortCode(),
        "shortUrl", "/s/" + shortened.getShortCode(),
        "originalUrl", shortened.getOriginalUrl(),
        "expiresAt", shortened.getExpiresAt() != null ? shortened.getExpiresAt().toString() : ""));
  }

  @GetMapping("/s/{shortCode}")
  public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
    try {
      String originalUrl = urlShortenerService.resolveUrl(shortCode);
      HttpHeaders headers = new HttpHeaders();
      headers.setLocation(URI.create(originalUrl));
      return new ResponseEntity<>(headers, HttpStatus.FOUND);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/api/urls")
  public ResponseEntity<List<ShortenedUrl>> getUserUrls(Authentication auth) {
    return ResponseEntity.ok(urlShortenerService.getUserUrls(auth.getName()));
  }

  @GetMapping("/api/urls/{shortCode}/stats")
  public ResponseEntity<?> getUrlStats(@PathVariable String shortCode, Authentication auth) {
    try {
      ShortenedUrl url = urlShortenerService.getUrlStats(shortCode, auth.getName());
      return ResponseEntity.ok(url);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }
  }

  @DeleteMapping("/api/urls/{shortCode}")
  public ResponseEntity<?> deleteUrl(@PathVariable String shortCode, Authentication auth) {
    try {
      urlShortenerService.deleteUrl(shortCode, auth.getName());
      return ResponseEntity.ok(Map.of("message", "URL deleted"));
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }
  }
}
