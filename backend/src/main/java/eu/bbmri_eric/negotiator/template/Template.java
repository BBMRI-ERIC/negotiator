package eu.bbmri_eric.negotiator.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "template")
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
  private String content;

  @Builder.Default
  @NotNull(message = "Must have a value")
  private boolean isCustomized = false;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  @Builder.Default
  private LocalDateTime updatedAt = LocalDateTime.now();

  public Template(Long id, String name, String content) {
    this.id = id;
    this.name = name;
    this.content = content;
    this.updatedAt = LocalDateTime.now();
  }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Template template = (Template) o;
    return isCustomized == template.isCustomized
        && Objects.equals(id, template.id)
        && Objects.equals(name, template.name)
        && Objects.equals(content, template.content)
        && Objects.equals(updatedAt, template.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, content, isCustomized, updatedAt);
  }

  @Override
  public String toString() {
    return "Template{" + "id=" + id + ", name='" + name + '\'' + ", updatedAt=" + updatedAt + '}';
  }
}
