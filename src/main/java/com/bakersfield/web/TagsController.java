package com.bakersfield.web;

import com.bakersfield.model.Tag;
import com.bakersfield.repository.TagRepository;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class TagsController {
  private final TagRepository tagRepository;

  public TagsController(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
  }

  @GetMapping
  public List<TagResponse> getAll() {
    return tagRepository.findAll().stream().map(TagResponse::from).toList();
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
