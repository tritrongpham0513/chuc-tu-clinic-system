package com.chuctupk.repository;

import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    List<User> findByRole(Role role);
    List<User> findByOrderByCreatedAtDesc();
    List<User> findByRoleOrderByCreatedAtDesc(Role role);
}
