package com.yunus.service;

import com.yunus.dto.auth.LoginRequest;
import com.yunus.dto.auth.RefreshRequest;
import com.yunus.dto.auth.RegisterRequest;
import com.yunus.dto.auth.AuthResponse;
import com.yunus.entity.RefreshToken;
import com.yunus.entity.User;
import com.yunus.enums.Role;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.model.UserPrincipal;
import com.yunus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;


    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorType.DUPLICATE_ENTRY, "Kullanıcı adı");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorType.DUPLICATE_ENTRY, "e-posta");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();


        User savedUser;

        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorType.DUPLICATE_ENTRY, "Kullanıcı adı veya e-posta adresi");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());

        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(savedUser, "Kayıt Başarılı");
    }


    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return buildAuthResponse(principal.getUser(), "Giriş Başarılı");


    }


    public AuthResponse refresh(RefreshRequest request) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(request.getRefreshToken());
        String accessToken = jwtService.generateToken(new UserPrincipal(rotated.user()));
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(rotated.refreshToken())
                .username(rotated.user().getUsername())
                .message("Oturum Başarılı")
                .build();


    }

    public void logout(RefreshRequest request) {
        refreshTokenService.logout(request.getRefreshToken());
    }

    private AuthResponse buildAuthResponse(User user, String message) {
        String accessToken = jwtService.generateToken(new UserPrincipal(user));
        String refreshToken = refreshTokenService.createForLogin(user);
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .message(message)
                .build();
    }


}
