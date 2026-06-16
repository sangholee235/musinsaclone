package com.musinsaclone.auth.service;

import com.musinsaclone.auth.dto.LoginRequest;
import com.musinsaclone.auth.dto.SignupRequest;
import com.musinsaclone.auth.dto.TokenResponse;
import com.musinsaclone.auth.jwt.JwtProvider;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // Redis 대신 임시 인메모리 저장 (로컬 개발용)
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.badRequest("이미 사용 중인 이메일입니다.");
        }

        userRepository.save(User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .role(User.Role.USER)
                .point(0)
                .build());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> BusinessException.badRequest("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        refreshTokenStore.put("refresh:" + user.getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validate(refreshToken)) {
            throw BusinessException.badRequest("유효하지 않은 토큰입니다.");
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String stored = refreshTokenStore.get("refresh:" + userId);
        if (!refreshToken.equals(stored)) {
            throw BusinessException.badRequest("만료된 토큰입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtProvider.createAccessToken(userId, user.getRole().name());
        return new TokenResponse(newAccessToken, refreshToken);
    }

    public void logout(Long userId) {
        refreshTokenStore.remove("refresh:" + userId);
    }
}
