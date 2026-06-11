package dev.aegeanship.jobtracker.common.mapper;

import java.util.List;

public interface BaseMapper<E , REQ, RES> {

    E toEntity(REQ request);

    RES toResponse(E entity);

    default List<RES> toResponseList(List<E> entities){
        return entities.stream().map(this::toResponse).toList();
    }
}
