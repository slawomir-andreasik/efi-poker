package com.andreasik.efipoker.estimation.estimate;

import com.andreasik.efipoker.estimation.task.TaskEntityMapper;
import com.andreasik.efipoker.participant.ParticipantEntityMapper;
import com.andreasik.efipoker.shared.mapper.EfiMapperConfig;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(
    config = EfiMapperConfig.class,
    uses = {TaskEntityMapper.class, ParticipantEntityMapper.class})
public interface EstimateEntityMapper {

  Estimate toDomain(EstimateEntity entity);

  List<Estimate> toDomainList(List<EstimateEntity> entities);

  EstimateEntity toEntity(Estimate domain);
}
