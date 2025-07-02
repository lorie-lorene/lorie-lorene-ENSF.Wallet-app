package com.Authentication.authService.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.Authentication.authService.model.User;

@RepositoryRestResource
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}