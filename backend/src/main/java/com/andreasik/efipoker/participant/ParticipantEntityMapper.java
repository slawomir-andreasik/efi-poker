package com.andreasik.efipoker.participant;

import com.andreasik.efipoker.project.ProjectEntityMapper;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = EfiMapperConfig.class, uses = ProjectEntityMapper.class)
public interface ParticipantEntityMapper {

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "invitedRoomIds", ignore = true)
  Participant toDomain(ParticipantEntity entity);

  List<Participant> toDomainList(List<ParticipantEntity> entities);

  @Mapping(target = "user", ignore = true)
  ParticipantEntity toEntity(Participant domain);
}
