package com.andreasik.efipoker.project;

import com.andreasik.efipoker.auth.UserEntityMapper;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = EfiMapperConfig.class, uses = UserEntityMapper.class)
public interface ProjectEntityMapper {

  Project toDomain(ProjectEntity entity);

  List<Project> toDomainList(List<ProjectEntity> entities);

  ProjectEntity toEntity(Project domain);
}
