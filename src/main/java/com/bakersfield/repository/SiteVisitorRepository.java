package com.bakersfield.repository;

import com.bakersfield.model.SiteVisitor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SiteVisitorRepository extends JpaRepository<SiteVisitor, Long> {
  Optional<SiteVisitor> findByVisitorId(String visitorId);

  @Query("select coalesce(sum(s.visitCount), 0) from SiteVisitor s")
  Long sumVisitCount();
}