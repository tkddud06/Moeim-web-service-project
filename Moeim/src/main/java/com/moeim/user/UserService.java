package com.moeim.user;

import com.moeim.category.CategoryRepository;
import com.moeim.global.ImageResizeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public User signup(String email, String password, String nickname, String bio, List<Integer> categoryIds) {

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .bio(bio)
                .build();
        if (categoryIds != null) {
            user.updateInterests(new HashSet<>(categoryIds));
        } else {
            user.updateInterests(new HashSet<>()); // 빈 리스트 처리
        }
        user.setProfilePublic(false);
        return userRepository.save(user);
    }

    // 로그인 로직
    public User login(String email, String password) {

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) return null;

        User user = userOptional.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null; // 비밀번호 불일치
        }

        return user;
    }

    // ID로 유저 조회
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
    }

    /**
     * 프로필 수정
     * - nickname, bio, profilePicture, profilePublic(프로필 공개 여부)을 갱신
     */
    public User updateProfile(Long userId,
                              String nickname,
                              String bio,
                              MultipartFile profileImage,
                              boolean profilePublic,
                              List<Integer> categoryIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 엔티티에 적용
        user.updateProfile(nickname, bio, profileImage);
        user.setProfilePublic(profilePublic);   //  공개 여부 반영

        // 관심사 카테고리 업데이트
        if (categoryIds != null) {
            user.updateInterests(new HashSet<>(categoryIds));
        } else {
            user.updateInterests(new HashSet<>()); // 빈 리스트 처리
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            long maxSize = 2 * 1024 * 1024; // 2MB 예시
            if (profileImage.getSize() > maxSize) {
                throw new IllegalArgumentException("프로필 이미지는 2MB 이하만 업로드할 수 있습니다.");
            }

            try {
                byte[] originalBytes = profileImage.getBytes();
                String contentType = profileImage.getContentType();

                // ContentType에서 출력 포맷 결정 (예: "image/png" -> "png")
                String outputFormat = contentType.substring(contentType.indexOf('/') + 1);

                // 원본 바이트를 240*240 픽셀로 리사이징
                byte[] resizedBytes = ImageResizeUtil.resize(originalBytes, outputFormat);

                // 리사이징된 바이트와 ContentType 저장
                user.setProfileImage(resizedBytes);
                user.setProfileImageType(contentType);
            } catch (IOException e) {
                // IOException 발생 시 처리 로직
                e.printStackTrace();
                throw new RuntimeException("이미지 파일을 처리하는 중 오류가 발생했습니다.", e);
            }
        }

        return userRepository.save(user);
    }

    /**
     * 마이페이지에서 '현재 비밀번호'를 확인하고 새 비밀번호로 변경
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 간단한 길이 체크
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
        }

        // 엔티티 메서드 이름 통일 (updatePassword 사용)
        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 닉네임으로 로그인 아이디(이메일) 찾기
     */
    @Transactional(readOnly = true)
    public String findLoginIdByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .map(User::getEmail)   // 여기서 "아이디 = 이메일"로 반환
                .orElse(null);
    }

    /**
     * 닉네임 + 이메일로 사용자 찾기 (비밀번호 찾기용)
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmailAndNickname(String email, String nickname) {
        return userRepository.findByEmailAndNickname(email, nickname);
    }

    /**
     * 비밀번호 재설정 (아이디/이메일 인증 이후 단계)
     */
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 여기서도 changePassword 가 아니라 updatePassword 로 이름 통일
        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 유저 관심사 업데이트 메소드
    public void updateUserInterests(Long userId, List<Integer> categoryIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 중복 제거를 위해 Set으로 변환하여 저장
        user.updateInterests(new HashSet<>(categoryIds));
    }
}