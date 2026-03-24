package com.example.bankcards.mapper;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper для преобразования {@link com.example.bankcards.entity.User} в DTO.
 *
 * <p>Реализация генерируется при компиляции. Заменяет ручной {@code toResponse()}
 * в {@code UserService}. Пароль не входит в {@code UserResponse} — маппинг это гарантирует.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO.UserResponse toResponse(User user);
}
