package com.andreasik.efipoker.auth;

import com.andreasik.efipoker.api.AdminApi;
import com.andreasik.efipoker.api.model.AdminCreateUserRequest;
import com.andreasik.efipoker.api.model.AdminResetPasswordRequest;
import com.andreasik.efipoker.api.model.AdminUpdateUserRequest;
import com.andreasik.efipoker.api.model.AdminUserResponse;
import com.andreasik.efipoker.api.model.PagedUsersResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AdminController implements AdminApi {

  private final UserService userService;
  private final UserMapper userMapper;

  @Override
  public ResponseEntity<PagedUsersResponse> listUsers(
      Integer page, Integer size, @Nullable String search) {
    log.debug("GET /admin/users page={}, size={}, search={}", page, size, search);

    Page<User> users = userService.listUsers(page, size, search);

    PagedUsersResponse response =
        new PagedUsersResponse()
            .content(userMapper.toAdminResponseList(users.getContent()))
            .totalElements(users.getTotalElements())
            .totalPages(users.getTotalPages())
            .page(users.getNumber())
            .size(users.getSize());

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AdminUserResponse> createUser(
      AdminCreateUserRequest adminCreateUserRequest) {
    log.debug("POST /admin/users username={}", adminCreateUserRequest.getUsername());

    User user =
        userService.adminCreateUser(
            adminCreateUserRequest.getUsername(),
            adminCreateUserRequest.getPassword(),
            adminCreateUserRequest.getEmail(),
            adminCreateUserRequest.getRole().getValue());

    return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toAdminResponse(user));
  }

  @Override
  public ResponseEntity<AdminUserResponse> getUser(UUID id) {
    log.debug("GET /admin/users/{}", id);

    User user =
        userService
            .findById(id)
            .orElseThrow(
                () ->
                    new com.andreasik.efipoker.shared.exception.ResourceNotFoundException(
                        "User not found: " + id));

    return ResponseEntity.ok(userMapper.toAdminResponse(user));
  }

  @Override
  public ResponseEntity<AdminUserResponse> updateUser(
      UUID id, AdminUpdateUserRequest adminUpdateUserRequest) {
    log.debug("PATCH /admin/users/{}", id);

    String email = adminUpdateUserRequest.getEmail();
    String role =
        adminUpdateUserRequest.getRole() != null
            ? adminUpdateUserRequest.getRole().getValue()
            : null;

    User user = userService.adminUpdateUser(id, email, role);
    return ResponseEntity.ok(userMapper.toAdminResponse(user));
  }

  @Override
  public ResponseEntity<Void> adminResetPassword(
      UUID id, AdminResetPasswordRequest adminResetPasswordRequest) {
    log.debug("PUT /admin/users/{}/password", id);

    userService.adminResetPassword(id, adminResetPasswordRequest.getNewPassword());

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteUser(UUID id) {
    log.debug("DELETE /admin/users/{}", id);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UUID currentUserId = UUID.fromString(authentication.getName());

    userService.deleteUser(id, currentUserId);
    return ResponseEntity.noContent().build();
  }
}
