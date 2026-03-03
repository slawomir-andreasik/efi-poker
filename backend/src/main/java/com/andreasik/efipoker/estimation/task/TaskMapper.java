package com.andreasik.efipoker.estimation.task;

import com.andreasik.efipoker.api.model.TaskResponse;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = EfiMapperConfig.class)
public interface TaskMapper {

  @Mapping(source = "room.id", target = "roomId")
  TaskResponse toResponse(Task task);

  List<TaskResponse> toResponseList(List<Task> tasks);
}
