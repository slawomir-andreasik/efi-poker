package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.project.ProjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "rooms")
public class RoomEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @ToString.Include
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity project;

  @Column(nullable = false, unique = true, length = 7)
  @ToString.Include
  private String slug;

  @Column(nullable = false)
  @ToString.Include
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "room_type", nullable = false, length = 20)
  @Builder.Default
  @ToString.Include
  private String roomType = "ASYNC";

  @Column(nullable = true)
  private Instant deadline;

  @Column(nullable = false, length = 20)
  @ToString.Include
  private String status;

  @Column(length = 500)
  private String topic;

  @Column(name = "round_number", nullable = false)
  @Builder.Default
  private int roundNumber = 1;

  @Column(name = "auto_reveal_on_deadline", nullable = false)
  @Builder.Default
  private boolean autoRevealOnDeadline = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (status == null) {
      status = RoomStatus.OPEN.name();
    }
    if (roomType == null) {
      roomType = "ASYNC";
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    RoomEntity other = (RoomEntity) o;
    return id != null && id.equals(other.getId());
  }

  @Override
  public final int hashCode() {
    return getClass().hashCode();
  }
}
