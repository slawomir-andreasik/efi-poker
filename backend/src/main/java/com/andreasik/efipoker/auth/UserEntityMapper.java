package com.andreasik.efipoker.auth;

import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = EfiMapperConfig.class)
public interface UserEntityMapper {

  User toDomain(UserEntity entity);

  List<User> toDomainList(List<UserEntity> entities);

  UserEntity toEntity(User domain);
}
