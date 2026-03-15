package com.andreasik.efipoker.auth;

import com.andreasik.efipoker.api.model.UserResponse;
import com.andreasik.efipoker.api.model.UserRole;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = EfiMapperConfig.class)
public interface UserMapper {

  @Mapping(target = "hasPassword", expression = "java(user.passwordHash() != null)")
  UserResponse toResponse(User user);

  com.andreasik.efipoker.api.model.AdminUserResponse toAdminResponse(User user);

  List<com.andreasik.efipoker.api.model.AdminUserResponse> toAdminResponseList(List<User> users);

  default UserRole mapRole(String role) {
    if (role == null) {
      return null;
    }
    return UserRole.fromValue(role);
  }
}
