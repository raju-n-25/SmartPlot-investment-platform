package com.example.spip_draft.service.impl;

import com.example.spip_draft.dto.document.UserDocumentResponse;
import com.example.spip_draft.dto.document.UserDocumentUploadRequest;
import com.example.spip_draft.entity.Builder;
import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.entity.User;
import com.example.spip_draft.entity.UserDocument;
import com.example.spip_draft.enums.DocumentStatus;
import com.example.spip_draft.enums.DocumentType;
import com.example.spip_draft.enums.KycStatus;
import com.example.spip_draft.repository.BuilderRepository;
import com.example.spip_draft.repository.InvestorRepository;
import com.example.spip_draft.repository.UserDocumentRepository;
import com.example.spip_draft.service.BuilderService;
import com.example.spip_draft.service.FileStorageService;
import com.example.spip_draft.service.UserDocumentService;
import com.example.spip_draft.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDocumentServiceImpl implements UserDocumentService {

    private final UserDocumentRepository userDocumentRepository;
    private final BuilderService builderService;
    private final BuilderRepository builderRepository;
    private final InvestorRepository investorRepository;
    private final FileStorageService fileStorageService;

    @Override
    public UserDocumentResponse uploadBuilderDocument(UserDocumentUploadRequest request) {
        return saveBuilderDocument(
                request.getDocumentType(),
                request.getFileUrl(),
                request.getOriginalFileName(),
                request.getResubmissionReason()
        );
    }

    @Override
    public UserDocumentResponse uploadBuilderDocument(
            DocumentType documentType,
            MultipartFile file,
            String originalFileName,
            String resubmissionReason
    ) {
        String storedFileUrl = fileStorageService.storeBuilderDocument(file);

        String finalOriginalName = originalFileName;
        if (finalOriginalName == null || finalOriginalName.isBlank()) {
            finalOriginalName = file.getOriginalFilename();
        }

        return saveBuilderDocument(
                documentType,
                storedFileUrl,
                finalOriginalName,
                resubmissionReason
        );
    }

    private UserDocumentResponse saveBuilderDocument(
            DocumentType documentType,
            String fileUrl,
            String originalFileName,
            String resubmissionReason
    ) {
        Builder builder = builderService.getBuilderForCurrentTenant();
        User user = builder.getUser();

        if (user == null) {
            throw new RuntimeException("User not found for builder");
        }

        UserDocument latestCurrent = userDocumentRepository
                .findFirstByUserIdAndDocumentTypeAndCurrentTrueOrderByCreatedAtDesc(
                        user.getId(),
                        documentType
                )
                .orElse(null);

        int nextVersion = 1;

        if (latestCurrent != null) {
            if (latestCurrent.getStatus() == DocumentStatus.PENDING) {
                throw new RuntimeException("This document is under review and cannot be changed yet.");
            }

            if ((latestCurrent.getStatus() == DocumentStatus.REJECTED
                    || latestCurrent.getStatus() == DocumentStatus.APPROVED)
                    && (resubmissionReason == null || resubmissionReason.isBlank())) {
                throw new RuntimeException("Resubmission reason is required.");
            }

            latestCurrent.setCurrent(false);
            userDocumentRepository.save(latestCurrent);
            nextVersion = latestCurrent.getVersionNumber() + 1;
        }

        UserDocument doc = new UserDocument();
        doc.setUser(user);
        doc.setRole(user.getRole());
        doc.setDocumentType(documentType);
        doc.setFileUrl(fileUrl);
        doc.setOriginalFileName(originalFileName);
        doc.setStatus(DocumentStatus.PENDING);
        doc.setSubmittedAt(LocalDateTime.now());
        doc.setReviewedAt(null);
        doc.setRejectionReason(null);
        doc.setResubmissionReason(resubmissionReason);
        doc.setVersionNumber(nextVersion);
        doc.setCurrent(true);

        userDocumentRepository.save(doc);

        refreshBuilderKycStatus(user.getId());

        return mapToResponse(doc);
    }

    @Override
    public List<UserDocumentResponse> getCurrentBuilderDocuments() {
        Builder builder = builderService.getBuilderForCurrentTenant();
        User user = builder.getUser();

        if (user == null) {
            throw new RuntimeException("User not found for builder");
        }

        return userDocumentRepository.findByUserIdAndCurrentTrueOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<UserDocumentResponse> getAllBuilderDocuments() {
        Builder builder = builderService.getBuilderForCurrentTenant();
        User user = builder.getUser();

        if (user == null) {
            throw new RuntimeException("User not found for builder");
        }

        return userDocumentRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public UserDocumentResponse uploadInvestorDocument(UserDocumentUploadRequest request) {
        return saveInvestorDocument(
                request.getDocumentType(),
                request.getFileUrl(),
                request.getOriginalFileName(),
                request.getResubmissionReason()
        );
    }

    @Override
    public UserDocumentResponse uploadInvestorDocument(
            DocumentType documentType,
            MultipartFile file,
            String originalFileName,
            String resubmissionReason
    ) {
        String storedFileUrl = fileStorageService.storeInvestorDocument(file);

        String finalOriginalName = originalFileName;
        if (finalOriginalName == null || finalOriginalName.isBlank()) {
            finalOriginalName = file.getOriginalFilename();
        }

        return saveInvestorDocument(
                documentType,
                storedFileUrl,
                finalOriginalName,
                resubmissionReason
        );
    }

    private UserDocumentResponse saveInvestorDocument(
            DocumentType documentType,
            String fileUrl,
            String originalFileName,
            String resubmissionReason
    ) {
        Investor investor = getCurrentInvestor();
        User user = investor.getUser();

        if (user == null) {
            throw new RuntimeException("User not found for investor");
        }

        UserDocument latestCurrent = userDocumentRepository
                .findFirstByUserIdAndDocumentTypeAndCurrentTrueOrderByCreatedAtDesc(
                        user.getId(),
                        documentType
                )
                .orElse(null);

        int nextVersion = 1;

        if (latestCurrent != null) {
            if (latestCurrent.getStatus() == DocumentStatus.PENDING) {
                throw new RuntimeException("This document is under review and cannot be changed yet.");
            }

            if ((latestCurrent.getStatus() == DocumentStatus.REJECTED
                    || latestCurrent.getStatus() == DocumentStatus.APPROVED)
                    && (resubmissionReason == null || resubmissionReason.isBlank())) {
                throw new RuntimeException("Resubmission reason is required.");
            }

            latestCurrent.setCurrent(false);
            userDocumentRepository.save(latestCurrent);
            nextVersion = latestCurrent.getVersionNumber() + 1;
        }

        UserDocument doc = new UserDocument();
        doc.setUser(user);
        doc.setRole(user.getRole());
        doc.setDocumentType(documentType);
        doc.setFileUrl(fileUrl);
        doc.setOriginalFileName(originalFileName);
        doc.setStatus(DocumentStatus.PENDING);
        doc.setSubmittedAt(LocalDateTime.now());
        doc.setReviewedAt(null);
        doc.setRejectionReason(null);
        doc.setResubmissionReason(resubmissionReason);
        doc.setVersionNumber(nextVersion);
        doc.setCurrent(true);

        userDocumentRepository.save(doc);

        refreshInvestorApprovalStatus(user.getId());

        return mapToResponse(doc);
    }

    @Override
    public List<UserDocumentResponse> getCurrentInvestorDocuments() {
        Investor investor = getCurrentInvestor();
        User user = investor.getUser();

        if (user == null) {
            throw new RuntimeException("User not found for investor");
        }

        return userDocumentRepository.findByUserIdAndCurrentTrueOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<UserDocumentResponse> getAllInvestorDocuments() {
        Investor investor = getCurrentInvestor();
        User user = investor.getUser();

        if (user == null) {
            throw new RuntimeException("User not found for investor");
        }

        return userDocumentRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<UserDocumentResponse> getPendingDocuments(String role) {
        return userDocumentRepository.findByRoleAndStatusOrderByCreatedAtAsc(role, DocumentStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    @Override
    public List<UserDocumentResponse> getCurrentDocumentsByRole(String role) {
        return userDocumentRepository.findByRoleAndCurrentTrueOrderByCreatedAtDesc(role)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public UserDocumentResponse approveDocument(Long documentId) {
        UserDocument doc = userDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        doc.setStatus(DocumentStatus.APPROVED);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setRejectionReason(null);

        userDocumentRepository.save(doc);

        if ("ROLE_BUILDER".equalsIgnoreCase(doc.getRole())) {
            refreshBuilderKycStatus(doc.getUser().getId());
        }

        if ("ROLE_INVESTOR".equalsIgnoreCase(doc.getRole())) {
            refreshInvestorApprovalStatus(doc.getUser().getId());
        }

        return mapToResponse(doc);
    }

    @Override
    public UserDocumentResponse rejectDocument(Long documentId, String rejectionReason) {
        UserDocument doc = userDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        doc.setStatus(DocumentStatus.REJECTED);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setRejectionReason(rejectionReason);

        userDocumentRepository.save(doc);

        if ("ROLE_BUILDER".equalsIgnoreCase(doc.getRole())) {
            refreshBuilderKycStatus(doc.getUser().getId());
        }

        if ("ROLE_INVESTOR".equalsIgnoreCase(doc.getRole())) {
            refreshInvestorApprovalStatus(doc.getUser().getId());
        }

        return mapToResponse(doc);
    }

    @Override
    public void refreshBuilderKycStatus(Long userId) {
        Builder builder = builderRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Builder not found for user"));

        List<UserDocument> currentDocs =
                userDocumentRepository.findByUserIdAndCurrentTrueOrderByCreatedAtDesc(userId);

        DocumentType[] mandatoryDocs = {
                DocumentType.PAN_CARD,
                DocumentType.GST_CERTIFICATE,
                DocumentType.COMPANY_REGISTRATION,
                DocumentType.LAND_OWNERSHIP_PROOF
        };

        boolean anyRejected = false;
        boolean allApproved = true;

        for (DocumentType requiredType : mandatoryDocs) {
            UserDocument doc = currentDocs.stream()
                    .filter(d -> d.getDocumentType() == requiredType)
                    .findFirst()
                    .orElse(null);

            if (doc == null) {
                builder.setKycStatus(KycStatus.PENDING);
                builder.setKycVerifiedAt(null);
                builder.setKycRejectionReason("Mandatory documents are incomplete.");
                builderRepository.save(builder);
                return;
            }

            if (doc.getStatus() == DocumentStatus.REJECTED) {
                anyRejected = true;
            }

            if (doc.getStatus() != DocumentStatus.APPROVED) {
                allApproved = false;
            }
        }

        if (anyRejected) {
            builder.setKycStatus(KycStatus.REJECTED);
            builder.setKycVerifiedAt(LocalDateTime.now());
            builder.setKycRejectionReason("One or more mandatory documents were rejected.");
        } else if (allApproved) {
            builder.setKycStatus(KycStatus.APPROVED);
            builder.setKycVerifiedAt(LocalDateTime.now());
            builder.setKycRejectionReason(null);
        } else {
            builder.setKycStatus(KycStatus.PENDING);
            builder.setKycVerifiedAt(null);
            builder.setKycRejectionReason(null);
        }

        builderRepository.save(builder);
    }

    @Override
    public void refreshInvestorApprovalStatus(Long userId) {
        Investor investor = investorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Investor not found for user"));

        List<UserDocument> currentDocs =
                userDocumentRepository.findByUserIdAndCurrentTrueOrderByCreatedAtDesc(userId);

        DocumentType[] mandatoryDocs = {
                DocumentType.ID_PROOF,
                DocumentType.ADDRESS_PROOF
        };

        boolean allApproved = true;

        for (DocumentType requiredType : mandatoryDocs) {
            UserDocument doc = currentDocs.stream()
                    .filter(d -> d.getDocumentType() == requiredType)
                    .findFirst()
                    .orElse(null);

            if (doc == null) {
                investor.setApproved(false);
                investorRepository.save(investor);
                return;
            }

            if (doc.getStatus() != DocumentStatus.APPROVED) {
                allApproved = false;
            }
        }

        investor.setApproved(allApproved);
        investorRepository.save(investor);
    }

    @Override
    public List<UserDocumentResponse> getBuilderCurrentDocumentsForAdmin(Long builderId) {
        Builder builder = builderRepository.findById(builderId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));

        User user = builder.getUser();
        if (user == null) {
            throw new RuntimeException("User not found for builder");
        }

        return userDocumentRepository
                .findByUserIdAndRoleAndCurrentTrueOrderByCreatedAtDesc(user.getId(), "ROLE_BUILDER")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<UserDocumentResponse> getBuilderDocumentHistoryForAdmin(Long builderId) {
        Builder builder = builderRepository.findById(builderId)
                .orElseThrow(() -> new RuntimeException("Builder not found"));

        User user = builder.getUser();
        if (user == null) {
            throw new RuntimeException("User not found for builder");
        }

        return userDocumentRepository
                .findByUserIdAndRoleOrderByCreatedAtDesc(user.getId(), "ROLE_BUILDER")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<UserDocumentResponse> getDocumentsByRoleAndStatus(String role, String status) {
        DocumentStatus documentStatus = DocumentStatus.valueOf(status);

        return userDocumentRepository
                .findByRoleAndStatusOrderByCreatedAtDesc(role, documentStatus)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Investor getCurrentInvestor() {
        Long userId = SecurityUtil.getCurrentUserId();
        Long tenantId = SecurityUtil.getCurrentTenantId();

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User not found");
        }

        if (tenantId == null) {
            throw new RuntimeException("Unauthorized: Tenant not found");
        }

        return investorRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("Investor not found"));
    }

    private String resolveDisplayName(UserDocument doc) {
        if (doc == null || doc.getUser() == null) {
            return "Unknown User";
        }

        User user = doc.getUser();
        String role = user.getRole();

        if ("ROLE_BUILDER".equalsIgnoreCase(role)) {
            Builder builder = builderRepository.findByUserId(user.getId()).orElse(null);

            if (builder != null && builder.getCompanyName() != null && !builder.getCompanyName().isBlank()) {
                return builder.getCompanyName();
            }
        }

        String fullName = user.getFullName();
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }

        return "Unknown User";
    }

    private UserDocumentResponse mapToResponse(UserDocument doc) {
        UserDocumentResponse response = new UserDocumentResponse();

        response.setId(doc.getId());
        response.setUserId(doc.getUser() != null ? doc.getUser().getId() : null);
        response.setRole(doc.getRole());

        response.setDisplayName(resolveDisplayName(doc));
        response.setEmail(doc.getUser() != null ? doc.getUser().getEmail() : null);

        response.setDocumentType(doc.getDocumentType() != null ? doc.getDocumentType().name() : null);
        response.setFileUrl(doc.getFileUrl());
        response.setOriginalFileName(doc.getOriginalFileName());
        response.setStatus(doc.getStatus() != null ? doc.getStatus().name() : null);
        response.setSubmittedAt(doc.getSubmittedAt() != null ? doc.getSubmittedAt().toString() : null);
        response.setReviewedAt(doc.getReviewedAt() != null ? doc.getReviewedAt().toString() : null);
        response.setRejectionReason(doc.getRejectionReason());
        response.setResubmissionReason(doc.getResubmissionReason());
        response.setVersionNumber(doc.getVersionNumber());
        response.setCurrent(doc.isCurrent());
        response.setProfilePhotoUrl(
                doc.getUser() != null ? doc.getUser().getProfilePhotoUrl() : null
        );

        if (doc.getUser() != null && "ROLE_INVESTOR".equalsIgnoreCase(doc.getRole())) {
            investorRepository.findByUserId(doc.getUser().getId())
                    .ifPresent(investor -> response.setInvestorApproved(investor.isApproved()));
        }

        return response;
    }
}