package eu.bbmri_eric.negotiator.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entity representing a template stored in the database. Templates contain HTML content used for
 * sending notifications and generating PDFs.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
class Template {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Template name cannot be blank")
  @Size(max = 255, message = "Template name must not exceed 255 characters")
  @Column(nullable = false, unique = true)
  private String name;

  @NotBlank(message = "HTML content cannot be blank")
  @Column(columnDefinition = "TEXT", nullable = false)
  private String html;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Template template = (Template) o;
    return Objects.equals(id, template.id)
        && Objects.equals(name, template.name)
        && Objects.equals(html, template.html)
        && Objects.equals(updatedAt, template.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, html, updatedAt);
  }

  @Override
  public String toString() {
    return "Template{" + "id=" + id + ", name='" + name + '\'' + ", updatedAt=" + updatedAt + '}';
  }
}
