package com.example.bankcards.exception;

/**
 * Исключение при обращении к несуществующему ресурсу.
 *
 * <p>Выбрасывается когда карта, пользователь или токен не найдены в БД.
 * Возвращает клиенту HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}
