package com.andreasik.efipoker.estimation.estimate;

import com.andreasik.efipoker.api.model.EstimateResponse;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = EfiMapperConfig.class)
public interface EstimateMapper {

  @Mapping(source = "participant.id", target = "participantId")
  @Mapping(source = "participant.nickname", target = "participantNickname")
  EstimateResponse toResponse(Estimate estimate);
}
