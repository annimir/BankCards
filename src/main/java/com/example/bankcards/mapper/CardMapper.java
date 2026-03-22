package com.example.bankcards.mapper;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper — генерирует реализацию при компиляции.
 * Заменяет ручной toResponse() в CardService.
 * componentModel = "spring" → бин доступен через @Autowired / @RequiredArgsConstructor.
 */
@Mapper(componentModel = "spring")
public interface CardMapper {

    @Mapping(source = "owner.id",       target = "ownerId")
    @Mapping(source = "owner.username", target = "ownerUsername")
    CardDTO.CardResponse toResponse(Card card);
}