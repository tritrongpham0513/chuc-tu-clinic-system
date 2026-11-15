package com.chuctupk.service;

import com.chuctupk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(Long userId, int newCount, LocalDateTime lockedUntil) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginCount(newCount);
            user.setLockedUntil(lockedUntil);
            userRepository.saveAndFlush(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
            userRepository.saveAndFlush(user);
        });
    }
}
