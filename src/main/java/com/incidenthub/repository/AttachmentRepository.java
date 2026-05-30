package com.incidenthub.repository;

import com.incidenthub.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByIncidentIdOrderByUploadedAtDesc(Long incidentId);

  long countByIncidentId(Long incidentId);
}
