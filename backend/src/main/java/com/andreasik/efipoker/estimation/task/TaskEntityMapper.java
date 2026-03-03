package com.andreasik.efipoker.estimation.task;

import com.andreasik.efipoker.estimation.room.RoomEntityMapper;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = EfiMapperConfig.class, uses = RoomEntityMapper.class)
public interface TaskEntityMapper {

  Task toDomain(TaskEntity entity);

  List<Task> toDomainList(List<TaskEntity> entities);

  TaskEntity toEntity(Task domain);
}
