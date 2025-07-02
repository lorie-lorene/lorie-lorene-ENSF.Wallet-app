package com.Authentication.authService.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.Authentication.authService.model.User;
import com.Authentication.authService.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch the user from the database by email (username)
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

       SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());

        // Return a new org.springframework.security.core.userdetails.User with authorities
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), 
                user.getPassword(), 
                List.of(authority)
        );
    }
}
