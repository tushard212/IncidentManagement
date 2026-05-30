package com.incidenthub.service;

import com.incidenthub.model.ShortenedUrl;
import com.incidenthub.repository.ShortenedUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

  private final ShortenedUrlRepository repository;
  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final int CODE_LENGTH = 7;
  private static final SecureRandom random = new SecureRandom();

  @Transactional
  public ShortenedUrl shortenUrl(String originalUrl, String username, Integer expiryDays) {
    String code = generateUniqueCode();

    ShortenedUrl url = ShortenedUrl.builder()
        .shortCode(code)
        .originalUrl(originalUrl)
        .createdBy(username)
        .createdAt(LocalDateTime.now())
        .expiresAt(expiryDays != null ? LocalDateTime.now().plusDays(expiryDays) : null)
        .clickCount(0L)
        .build();

    return repository.save(url);
  }

  @Transactional
  public String resolveUrl(String shortCode) {
    ShortenedUrl url = repository.findByShortCode(shortCode)
        .orElseThrow(() -> new RuntimeException("Short URL not found: " + shortCode));

    if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("This short URL has expired");
    }

    url.setClickCount(url.getClickCount() + 1);
    repository.save(url);

    return url.getOriginalUrl();
  }

  public ShortenedUrl getUrlStats(String shortCode, String username) {
    ShortenedUrl url = repository.findByShortCode(shortCode)
        .orElseThrow(() -> new RuntimeException("Short URL not found: " + shortCode));

    if (!url.getCreatedBy().equals(username)) {
      throw new RuntimeException("Access denied");
    }

    return url;
  }

  public List<ShortenedUrl> getUserUrls(String username) {
    return repository.findByCreatedByOrderByCreatedAtDesc(username);
  }

  @Transactional
  public void deleteUrl(String shortCode, String username) {
    ShortenedUrl url = repository.findByShortCode(shortCode)
        .orElseThrow(() -> new RuntimeException("Short URL not found"));

    if (!url.getCreatedBy().equals(username)) {
      throw new RuntimeException("Access denied");
    }

    repository.delete(url);
  }

  private String generateUniqueCode() {
    String code;
    do {
      StringBuilder sb = new StringBuilder(CODE_LENGTH);
      for (int i = 0; i < CODE_LENGTH; i++) {
        sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
      }
      code = sb.toString();
    } while (repository.existsByShortCode(code));
    return code;
  }
}
