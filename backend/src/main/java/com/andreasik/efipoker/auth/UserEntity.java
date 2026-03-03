package com.andreasik.efipoker.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "users")
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @ToString.Include
  private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  @ToString.Include
  private String username;

  @Column @ToString.Include private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "auth_provider", nullable = false, length = 20)
  @Builder.Default
  private String authProvider = "LOCAL";

  @Column(name = "auth_provider_id")
  private String authProviderId;

  @Column(nullable = false, length = 20)
  @Builder.Default
  @ToString.Include
  private String role = "USER";

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

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
    UserEntity other = (UserEntity) o;
    return id != null && id.equals(other.getId());
  }

  @Override
  public final int hashCode() {
    return getClass().hashCode();
  }
}
