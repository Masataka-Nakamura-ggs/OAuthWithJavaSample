package com.example.demo.repository;

import com.example.demo.entity.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// UserIdentityRepository.java
@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, String> {
    Optional<UserIdentity> findByProviderNameAndProviderUserId(String providerName, String providerUserId);
}