package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PagedResponse;
import com.ecommerce.common.security.UserPrincipal;
import com.ecommerce.user.dto.*;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and address management")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileDTO profile = userService.getMyProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileDTO profile = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<AddressDTO>> addAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressDTO addressDTO) {
        AddressDTO address = userService.addAddress(principal.getId(), addressDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Address added", address));
    }

    @GetMapping("/addresses")
    @Operation(summary = "Get all addresses for current user")
    public ResponseEntity<ApiResponse<List<AddressDTO>>> getAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<AddressDTO> addresses = userService.getAddresses(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId) {
        userService.deleteAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }

    @GetMapping("/vendors/{id}")
    @Operation(summary = "Get vendor public profile")
    public ResponseEntity<ApiResponse<VendorProfileDTO>> getVendorProfile(@PathVariable Long id) {
        VendorProfileDTO vendor = userService.getVendorProfile(id);
        return ResponseEntity.ok(ApiResponse.success(vendor));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: list all users (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileDTO>>> getAllProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        Page<UserProfileDTO> result = userService.getAllProfiles(
                PageRequest.of(page, size, Sort.by(sortBy).descending()));

        PagedResponse<UserProfileDTO> pagedResponse = PagedResponse.of(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: delete user profile")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(@PathVariable Long id) {
        userService.deleteProfile(id);
        return ResponseEntity.ok(ApiResponse.success("User profile deleted", null));
    }
}
