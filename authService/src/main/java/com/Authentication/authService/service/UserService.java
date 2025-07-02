package com.Authentication.authService.service;

import com.Authentication.authService.dto.UserProfileResponse;
import com.Authentication.authService.model.User;
import com.Authentication.authService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return mapToUserProfile(user);
    }

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserProfile)
                .collect(Collectors.toList());
    }

    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal == null) {
            return "Anonymous";
        }
        
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        
        return principal.toString();
    }

    private UserProfileResponse mapToUserProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}