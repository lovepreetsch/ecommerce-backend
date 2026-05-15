package com.ecommerce.user.service;

import com.ecommerce.common.event.UserRegisteredEvent;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.security.UserPrincipal;
import com.ecommerce.user.dto.*;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.entity.VendorProfile;
import com.ecommerce.user.repository.AddressRepository;
import com.ecommerce.user.repository.UserProfileRepository;
import com.ecommerce.user.repository.VendorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final AddressRepository addressRepository;
    private final VendorProfileRepository vendorProfileRepository;

    /**
     * Create user profile from UserRegisteredEvent (via RabbitMQ).
     */
    @Transactional
    public void createProfileFromEvent(UserRegisteredEvent event) {
        if (userProfileRepository.existsByAuthUserId(event.getUserId())) {
            log.warn("Profile already exists for authUserId: {}", event.getUserId());
            return;
        }

        UserProfile profile = UserProfile.builder()
                .authUserId(event.getUserId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .build();

        profile = userProfileRepository.save(profile);
        log.info("Created user profile for: {} (authUserId: {})", event.getEmail(), event.getUserId());

        // Auto-create vendor profile if role is VENDOR
        if ("ROLE_VENDOR".equals(event.getRole())) {
            VendorProfile vendorProfile = VendorProfile.builder()
                    .userProfile(profile)
                    .storeName(event.getFirstName() + "'s Store")
                    .build();
            vendorProfileRepository.save(vendorProfile);
            log.info("Created vendor profile for: {}", event.getEmail());
        }
    }

    /**
     * Get current user's profile.
     */
    public UserProfileDTO getMyProfile(Long authUserId) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "authUserId", authUserId));
        return mapToDTO(profile);
    }

    /**
     * Update current user's profile.
     */
    @Transactional
    public UserProfileDTO updateProfile(Long authUserId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "authUserId", authUserId));

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        profile.setAvatarUrl(request.getAvatarUrl());

        profile = userProfileRepository.save(profile);
        log.info("Updated profile for authUserId: {}", authUserId);
        return mapToDTO(profile);
    }

    /**
     * Add a new address for the current user.
     */
    @Transactional
    public AddressDTO addAddress(Long authUserId, AddressDTO addressDTO) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "authUserId", authUserId));

        // If new address is default, unset other defaults
        if (addressDTO.isDefault()) {
            profile.getAddresses().forEach(a -> a.setDefault(false));
        }

        Address address = Address.builder()
                .userProfile(profile)
                .street(addressDTO.getStreet())
                .city(addressDTO.getCity())
                .state(addressDTO.getState())
                .zipCode(addressDTO.getZipCode())
                .country(addressDTO.getCountry())
                .isDefault(addressDTO.isDefault())
                .build();

        address = addressRepository.save(address);
        return mapAddressToDTO(address);
    }

    /**
     * Get all addresses for the current user.
     */
    public List<AddressDTO> getAddresses(Long authUserId) {
        return addressRepository.findByUserProfileAuthUserId(authUserId).stream()
                .map(this::mapAddressToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Delete an address.
     */
    @Transactional
    public void deleteAddress(Long authUserId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (!address.getUserProfile().getAuthUserId().equals(authUserId)) {
            throw new ResourceNotFoundException("Address", "id", addressId);
        }

        addressRepository.delete(address);
    }

    /**
     * Get vendor public profile.
     */
    public VendorProfileDTO getVendorProfile(Long vendorProfileId) {
        VendorProfile vendorProfile = vendorProfileRepository.findById(vendorProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("VendorProfile", "id", vendorProfileId));
        return mapVendorToDTO(vendorProfile);
    }

    /**
     * Admin: list all user profiles (paginated).
     */
    public Page<UserProfileDTO> getAllProfiles(Pageable pageable) {
        return userProfileRepository.findAll(pageable).map(this::mapToDTO);
    }

    @Transactional
    public void deleteProfile(Long id) {
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "id", id));
        userProfileRepository.delete(profile);
        log.info("Deleted user profile id: {}", id);
    }

    // ===== Mapping =====

    private UserProfileDTO mapToDTO(UserProfile profile) {
        UserProfileDTO dto = UserProfileDTO.builder()
                .id(profile.getId())
                .authUserId(profile.getAuthUserId())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phone(profile.getPhone())
                .avatarUrl(profile.getAvatarUrl())
                .build();

        if (profile.getVendorProfile() != null) {
            dto.setVendorProfile(mapVendorToDTO(profile.getVendorProfile()));
        }

        return dto;
    }

    private AddressDTO mapAddressToDTO(Address address) {
        return AddressDTO.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .isDefault(address.isDefault())
                .build();
    }

    private VendorProfileDTO mapVendorToDTO(VendorProfile vp) {
        return VendorProfileDTO.builder()
                .id(vp.getId())
                .storeName(vp.getStoreName())
                .storeDescription(vp.getStoreDescription())
                .logoUrl(vp.getLogoUrl())
                .verified(vp.isVerified())
                .rating(vp.getRating())
                .totalProducts(vp.getTotalProducts())
                .build();
    }
}
