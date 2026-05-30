package com.incidenthub.repository;

import com.incidenthub.model.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {

  Optional<ShortenedUrl> findByShortCode(String shortCode);

  boolean existsByShortCode(String shortCode);

  List<ShortenedUrl> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
