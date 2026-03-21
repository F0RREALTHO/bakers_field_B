package com.bakersfield.web;

import com.bakersfield.model.SiteVisitor;
import com.bakersfield.repository.SiteVisitorRepository;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
  private final SiteVisitorRepository siteVisitorRepository;

  public AnalyticsController(SiteVisitorRepository siteVisitorRepository) {
    this.siteVisitorRepository = siteVisitorRepository;
  }

  @PostMapping("/visit")
  @ResponseStatus(HttpStatus.CREATED)
  public VisitResponse trackVisit(@Valid @RequestBody VisitRequest request) {
    Instant now = Instant.now();
    SiteVisitor visitor = siteVisitorRepository.findByVisitorId(request.visitorId())
        .orElseGet(() -> {
          SiteVisitor created = new SiteVisitor();
          created.setVisitorId(request.visitorId());
          created.setFirstVisitedAt(now);
          created.setVisitCount(0L);
          return created;
        });

    visitor.setLastVisitedAt(now);
    visitor.setVisitCount((visitor.getVisitCount() == null ? 0L : visitor.getVisitCount()) + 1L);

    SiteVisitor saved = siteVisitorRepository.save(visitor);
    return new VisitResponse(saved.getVisitCount());
  }

  public record VisitRequest(@NotBlank String visitorId) {
  }

  public record VisitResponse(Long visitCount) {
  }
}