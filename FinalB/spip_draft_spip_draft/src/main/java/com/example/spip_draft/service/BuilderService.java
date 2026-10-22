package com.example.spip_draft.service;

import com.example.spip_draft.dto.builder.BuilderKycUpdateRequest;
import com.example.spip_draft.dto.builder.BuilderProfileResponse;
import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.KycStatus;
import com.example.spip_draft.multitenant.TenantContext;
import com.example.spip_draft.repository.BookingRepository;
import com.example.spip_draft.repository.BuilderRepository;
import com.example.spip_draft.repository.ProjectRepository;
import com.example.spip_draft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BuilderService {

    private final BuilderRepository builderRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ProjectRepository projectRepository;
    private final BookingRepository bookingRepository;

    public List<BuilderProfileResponse> getAllBuilders() {
        return builderRepository.findAll()
                .stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    public Builder getBuilderById(Long builderId) {
        return builderRepository.findById(builderId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));
    }

    public Builder getBuilderForCurrentTenant() {
        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new RuntimeException("Unauthorized tenant access");
        }

        return builderRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));
    }

    public BuilderProfileResponse getCurrentBuilderProfile() {
        Builder builder = getBuilderForCurrentTenant();
        return mapToProfileResponse(builder);
    }

    public BuilderProfileResponse updateCurrentBuilderKyc(BuilderKycUpdateRequest request) {
        Builder builder = getBuilderForCurrentTenant();

        builder.setKycDocumentUrl(request.getKycDocumentUrl());
        builder.setKycStatus(KycStatus.PENDING);
        builder.setKycSubmittedAt(LocalDateTime.now());
        builder.setKycVerifiedAt(null);
        builder.setKycRejectionReason(null);

        builderRepository.save(builder);
        return mapToProfileResponse(builder);
    }

    public BuilderProfileResponse updateProfilePhoto(String photoUrl) {
        Builder builder = getBuilderForCurrentTenant();

        if (builder.getUser() == null) {
            throw new RuntimeException("User not found for builder");
        }

        builder.getUser().setProfilePhotoUrl(photoUrl);
        userRepository.save(builder.getUser());

        return mapToProfileResponse(builder);
    }

    public BuilderProfileResponse uploadProfilePhoto(MultipartFile file) {
        Builder builder = getBuilderForCurrentTenant();

        if (builder.getUser() == null) {
            throw new RuntimeException("User not found for builder");
        }

        String storedFilePath = fileStorageService.storeProfilePhoto(file);

        builder.getUser().setProfilePhotoUrl(storedFilePath);
        userRepository.save(builder.getUser());

        return mapToProfileResponse(builder);
    }

    public void approveBuilder(Long builderId) {
        Builder builder = builderRepository.findById(builderId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));

        builder.setKycStatus(KycStatus.APPROVED);
        builder.setKycVerifiedAt(LocalDateTime.now());
        builder.setKycRejectionReason(null);

        builderRepository.save(builder);
    }

    public void rejectBuilder(Long builderId, String rejectionReason) {
        Builder builder = builderRepository.findById(builderId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));

        builder.setKycStatus(KycStatus.REJECTED);
        builder.setKycVerifiedAt(LocalDateTime.now());
        builder.setKycRejectionReason(rejectionReason);

        builderRepository.save(builder);
    }

    public void deleteBuilder(Long builderId) {
        Builder builder = builderRepository.findById(builderId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));

        long activeBookings = bookingRepository.countByPlot_Project_Builder_IdAndStatusIn(
                builderId,
                List.of(
                        BookingStatus.TOKEN_BLOCKED,
                        BookingStatus.TOKEN_PAID,
                        BookingStatus.AWAITING_BUILDER_APPROVAL,
                        BookingStatus.PAYMENT_OPTION_PENDING,
                        BookingStatus.BUILDER_APPROVED,
                        BookingStatus.REAPPROVAL_REQUESTED,
                        BookingStatus.EMI_ACTIVE,
                        BookingStatus.MEETING_HOLD,
                        BookingStatus.CONFIRMED
                )
        );

        if (activeBookings > 0) {
            throw new RuntimeException("Cannot delete builder because active bookings, sold plots, or investments exist");
        }

        long projectCount = projectRepository.findByBuilderId(builderId).size();

        if (projectCount > 0) {
            throw new RuntimeException("Builder has projects. Delete or deactivate projects first.");
        }

        if (builder.getUser() != null) {
            builder.getUser().setActive(false);
            userRepository.save(builder.getUser());
        }
    }

    public List<BuilderProfileResponse> getPendingKycBuilders() {
        return builderRepository.findByKycStatus(KycStatus.PENDING)
                .stream()
                .map(this::mapToProfileResponse)
                .toList();
    }
    public BuilderProfileResponse getBuilderProfileById(Long builderId) {
        Builder builder = builderRepository.findById(builderId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));

        return mapToProfileResponse(builder);
    }

    private BuilderProfileResponse mapToProfileResponse(Builder builder) {
        BuilderProfileResponse response = new BuilderProfileResponse();

        response.setId(builder.getId());
        response.setTenantId(builder.getTenantId());

        if (builder.getUser() != null) {
            response.setUserId(builder.getUser().getId());
            response.setFirstName(builder.getUser().getFirstName());
            response.setLastName(builder.getUser().getLastName());
            response.setFullName(builder.getUser().getFullName());
            response.setEmail(builder.getUser().getEmail());
            response.setProfilePhotoUrl(builder.getUser().getProfilePhotoUrl());
        }

        response.setCompanyName(builder.getCompanyName());
        response.setCompanyRegistration(builder.getCompanyRegistration());
        response.setPanNumber(builder.getPanNumber());
        response.setGstNumber(builder.getGstNumber());

        response.setKycDocumentUrl(builder.getKycDocumentUrl());
        response.setKycStatus(builder.getKycStatus() != null ? builder.getKycStatus().name() : null);
        response.setKycSubmittedAt(builder.getKycSubmittedAt() != null ? builder.getKycSubmittedAt().toString() : null);
        response.setKycVerifiedAt(builder.getKycVerifiedAt() != null ? builder.getKycVerifiedAt().toString() : null);
        response.setKycRejectionReason(builder.getKycRejectionReason());

        response.setRating(builder.getRating());

        return response;
    }
}