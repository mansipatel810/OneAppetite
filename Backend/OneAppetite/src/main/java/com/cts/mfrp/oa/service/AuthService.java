package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.RegisterRequest;
import com.cts.mfrp.oa.dto.response.UserResponse;
import com.cts.mfrp.oa.exception.EmailAlreadyExistsException;
import com.cts.mfrp.oa.exception.InvalidEmailDomainException;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.UserRepository;
import com.cts.mfrp.oa.security.UserDetailsServiceImpl;
import com.cts.mfrp.oa.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> ALLOWED_DOMAINS = Set.of("cognizant.com", "cts.com");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.email());
        }

        String domain = request.email().substring(request.email().indexOf('@') + 1).toLowerCase();
        if (!ALLOWED_DOMAINS.contains(domain)) {
            throw new InvalidEmailDomainException("Email domain not allowed. Use a corporate email.");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.EMPLOYEE);
        user.setIsActive(true);

        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(saved.getEmail()));

        return new UserResponse(
                saved.getUserId(),
                saved.getName(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getRole().name(),
                token
        );
    }
}
