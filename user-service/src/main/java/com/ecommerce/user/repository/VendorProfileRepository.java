package com.ecommerce.user.repository;

import com.ecommerce.user.entity.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {

    Optional<VendorProfile> findByUserProfileAuthUserId(Long authUserId);

    Optional<VendorProfile> findByUserProfileId(Long userProfileId);
}
