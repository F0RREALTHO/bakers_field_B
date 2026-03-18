package com.bakersfield.web;

import com.bakersfield.model.Tag;
import com.bakersfield.repository.TagRepository;
import com.bakersfield.service.DataChangePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/tags")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class AdminTagsController {
  private final TagRepository tagRepository;
  private final DataChangePublisher dataChangePublisher;

  public AdminTagsController(TagRepository tagRepository, DataChangePublisher dataChangePublisher) {
    this.tagRepository = tagRepository;
    this.dataChangePublisher = dataChangePublisher;
  }

  @GetMapping
  public List<TagResponse> getAll() {
    return tagRepository.findAll().stream().map(TagResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TagResponse create(@Valid @RequestBody TagRequest request) {
    String slug = normalizeSlug(request.name());
    if (tagRepository.existsBySlug(slug)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag already exists");
    }
    Tag tag = new Tag();
    tag.setName(request.name().trim());
    tag.setSlug(slug);
    tag.setTextColor(normalizeColor(request.textColor(), "#7a3f0c"));
    tag.setBackgroundColor(normalizeColor(request.backgroundColor(), "#fde6c2"));
    TagResponse response = TagResponse.from(tagRepository.save(tag));
    dataChangePublisher.publish("tags");
    dataChangePublisher.publish("products");
    return response;
  }

  @PutMapping("/{id}")
  public TagResponse update(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
    Tag tag = tagRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    String slug = normalizeSlug(request.name());
    tagRepository.findBySlug(slug)
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag already exists");
        });
    tag.setName(request.name().trim());
    tag.setSlug(slug);
    tag.setTextColor(normalizeColor(request.textColor(), "#7a3f0c"));
    tag.setBackgroundColor(normalizeColor(request.backgroundColor(), "#fde6c2"));
    TagResponse response = TagResponse.from(tagRepository.save(tag));
    dataChangePublisher.publish("tags");
    dataChangePublisher.publish("products");
    return response;
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    Tag tag = tagRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    if (!tag.getProducts().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag is in use");
    }
    tagRepository.delete(tag);
    dataChangePublisher.publish("tags");
  }

  private String normalizeSlug(String name) {
    return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
  }

  private String normalizeColor(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  public record TagRequest(
      @NotBlank String name,
      String textColor,
      String backgroundColor) {
  }

  public record TagResponse(
      Long id,
      String name,
      String slug,
      String textColor,
      String backgroundColor) {

    public static TagResponse from(Tag tag) {
      return new TagResponse(
          tag.getId(),
          tag.getName(),
          tag.getSlug(),
          tag.getTextColor(),
          tag.getBackgroundColor());
    }
  }
}
