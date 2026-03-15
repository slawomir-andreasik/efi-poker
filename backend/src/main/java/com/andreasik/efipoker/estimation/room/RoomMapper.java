package com.andreasik.efipoker.estimation.room;

import com.andreasik.efipoker.api.model.RoomResponse;
import com.andreasik.efipoker.api.model.RoomSlugResponse;
import com.andreasik.efipoker.api.model.RoomStatus;
import com.andreasik.efipoker.api.model.RoomType;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = EfiMapperConfig.class)
public interface RoomMapper {

  @Mapping(source = "project.id", target = "projectId")
  RoomResponse toResponse(Room room);

  List<RoomResponse> toResponseList(List<Room> rooms);

  @Mapping(source = "id", target = "roomId")
  @Mapping(source = "title", target = "roomTitle")
  @Mapping(source = "slug", target = "roomSlug")
  @Mapping(source = "project.slug", target = "projectSlug")
  @Mapping(source = "project.name", target = "projectName")
  RoomSlugResponse toSlugResponse(Room room);

  default RoomStatus mapStatus(String status) {
    if (status == null) {
      return null;
    }
    return RoomStatus.fromValue(status);
  }

  default RoomType mapRoomType(com.andreasik.efipoker.estimation.room.RoomType roomType) {
    if (roomType == null) return null;
    return RoomType.fromValue(roomType.name());
  }
}
