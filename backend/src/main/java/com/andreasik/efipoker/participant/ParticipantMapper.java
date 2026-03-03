package com.andreasik.efipoker.participant;

import com.andreasik.efipoker.api.model.ParticipantResponse;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = EfiMapperConfig.class)
public interface ParticipantMapper {

  ParticipantResponse toResponse(Participant participant);

  List<ParticipantResponse> toResponseList(List<Participant> participants);
}
