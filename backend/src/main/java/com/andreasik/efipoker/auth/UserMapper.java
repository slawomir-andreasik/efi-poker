package com.andreasik.efipoker.auth;

import com.andreasik.efipoker.api.model.AdminUserResponse;
import com.andreasik.efipoker.api.model.UserResponse;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = EfiMapperConfig.class)
public interface UserMapper {

  UserResponse toResponse(User user);

  AdminUserResponse toAdminResponse(User user);

  List<AdminUserResponse> toAdminResponseList(List<User> users);

  default UserResponse.RoleEnum mapRole(String role) {
    if (role == null) {
      return null;
    }
    return UserResponse.RoleEnum.fromValue(role);
  }

  default AdminUserResponse.RoleEnum mapAdminRole(String role) {
    if (role == null) {
      return null;
    }
    return AdminUserResponse.RoleEnum.fromValue(role);
  }
}
