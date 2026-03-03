package com.andreasik.efipoker.estimation.room;

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
import java.math.BigDecimal;
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
@Table(name = "round_history")
public class RoundHistoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @ToString.Include
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private RoomEntity room;

  @Column(name = "round_number", nullable = false)
  @ToString.Include
  private int roundNumber;

  @Column(length = 500)
  @ToString.Include
  private String topic;

  @Column(name = "average_pts", precision = 5, scale = 2)
  private BigDecimal averagePts;

  @Column(name = "median_pts", precision = 5, scale = 2)
  private BigDecimal medianPts;

  @Column(name = "vote_count", nullable = false)
  @Builder.Default
  private int voteCount = 0;

  @Column(name = "votes_json", nullable = false, columnDefinition = "TEXT")
  @Builder.Default
  private String votesJson = "[]";

  @Column(name = "completed_at", nullable = false, updatable = false)
  private Instant completedAt;

  @PrePersist
  protected void onCreate() {
    if (completedAt == null) {
      completedAt = Instant.now();
    }
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    RoundHistoryEntity other = (RoundHistoryEntity) o;
    return id != null && id.equals(other.getId());
  }

  @Override
  public final int hashCode() {
    return getClass().hashCode();
  }
}
