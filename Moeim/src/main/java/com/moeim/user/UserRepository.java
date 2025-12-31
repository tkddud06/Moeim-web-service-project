// src/main/java/com/moeim/user/UserRepository.java
package com.moeim.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
    Optional<User> findByEmailAndNickname(String email, String nickname);

    boolean existsByEmail(String email);      // 이메일 중복 체크
    boolean existsByNickname(String nickname); // 닉네임 중복 체크
}
