package com.bakersfield.repository;

import com.bakersfield.model.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
  Optional<Tag> findBySlug(String slug);
  boolean existsBySlug(String slug);
}
