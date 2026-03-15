package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.project.ProjectEntityMapper;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = EfiMapperConfig.class, uses = ProjectEntityMapper.class)
public interface RoomEntityMapper {

  Room toDomain(RoomEntity entity);

  List<Room> toDomainList(List<RoomEntity> entities);

  RoomEntity toEntity(Room domain);

  default RoomType mapRoomType(String roomType) {
    return roomType == null ? null : RoomType.valueOf(roomType);
  }

  default String mapRoomTypeToString(RoomType roomType) {
    return roomType == null ? null : roomType.name();
  }
}
