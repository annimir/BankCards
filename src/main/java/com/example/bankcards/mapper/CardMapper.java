package com.example.bankcards.mapper;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper для преобразования {@link com.example.bankcards.entity.Card} в DTO.
 *
 * <p>Реализация генерируется MapStruct при компиляции — нулевые накладные расходы в runtime.
 * {@code componentModel = "spring"} делает бин доступным через {@code @RequiredArgsConstructor}.
 * Заменяет ручной {@code toResponse()} в {@code CardService}.
 */
@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(source = "owner.id",       target = "ownerId")
    @Mapping(source = "owner.username", target = "ownerUsername")
    CardDTO.CardResponse toResponse(Card card);
}