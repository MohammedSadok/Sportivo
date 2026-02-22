package com.sadok.sportivo.users;

import java.util.UUID;

import com.sadok.sportivo.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String username;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;
}
