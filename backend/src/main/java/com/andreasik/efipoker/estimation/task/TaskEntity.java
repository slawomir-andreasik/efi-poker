package com.andreasik.efipoker.estimation.task;

import com.andreasik.efipoker.estimation.room.RoomEntity;
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
@Table(name = "tasks")
public class TaskEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @ToString.Include
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private RoomEntity room;

  @Column(nullable = false)
  @ToString.Include
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  @ToString.Include
  private int sortOrder = 0;

  @Column(name = "final_estimate", length = 10)
  private String finalEstimate;

  @Column(nullable = false)
  @Builder.Default
  private boolean revealed = false;

  @Column(nullable = false)
  @Builder.Default
  @ToString.Include
  private boolean active = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    TaskEntity other = (TaskEntity) o;
    return id != null && id.equals(other.getId());
  }

  @Override
  public final int hashCode() {
    return getClass().hashCode();
  }
}
