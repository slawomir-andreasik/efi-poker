package com.andreasik.efipoker.project;

import com.andreasik.efipoker.api.model.ProjectAdminResponse;
import com.andreasik.efipoker.api.model.ProjectResponse;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = EfiMapperConfig.class)
public interface ProjectMapper {

  ProjectResponse toResponse(Project project);

  ProjectAdminResponse toAdminResponse(Project project);

  List<ProjectResponse> toResponseList(List<Project> projects);

  List<ProjectAdminResponse> toAdminResponseList(List<Project> projects);
}
