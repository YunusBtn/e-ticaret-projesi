package com.yunus.service;

import com.yunus.dto.auth.LoginRequest;
import com.yunus.dto.auth.RegisterRequest;
import com.yunus.dto.auth.AuthResponse;
import com.yunus.entity.User;
import com.yunus.enums.Role;
import com.yunus.exception.BusinessException;
import com.yunus.exception.ErrorType;
import com.yunus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
        }catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorType.DUPLICATE_ENTRY, "Kullanıcı adı veya e-posta adresi");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());

        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .message("Kayıt Başarılı")
                .build();
    }


    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .username(request.getUsername())
                .message("Giriş Başarılı")
                .build();


    }


}
