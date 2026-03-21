package com.andreasik.efipoker.participant;

import com.andreasik.efipoker.api.model.ParticipantResponse;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = EfiMapperConfig.class)
public interface ParticipantMapper {

  @Mapping(target = "token", ignore = true)
  @Mapping(target = "tokenExpiresAt", ignore = true)
  ParticipantResponse toResponse(Participant participant);

  List<ParticipantResponse> toResponseList(List<Participant> participants);
}
