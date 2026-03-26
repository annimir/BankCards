package com.example.bankcards.service;

import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.impl.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@code UserDetailsService} для Spring Security.
 *
 * <p>Загружает пользователя по username из БД для аутентификации.
 * {@link com.example.bankcards.entity.User} реализует {@code UserDetails} напрямую,
 * поэтому адаптер не нужен — возвращается сама сущность.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsService implements UserDetailsServiceImpl {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
