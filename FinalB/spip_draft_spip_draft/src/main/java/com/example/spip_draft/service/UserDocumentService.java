package com.example.spip_draft.service;

import com.example.spip_draft.dto.document.UserDocumentResponse;
import com.example.spip_draft.dto.document.UserDocumentUploadRequest;
import com.example.spip_draft.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserDocumentService {

    UserDocumentResponse uploadBuilderDocument(UserDocumentUploadRequest request);

    UserDocumentResponse uploadBuilderDocument(
            DocumentType documentType,
            MultipartFile file,
            String originalFileName,
            String resubmissionReason
    );

    List<UserDocumentResponse> getCurrentBuilderDocuments();

    List<UserDocumentResponse> getAllBuilderDocuments();

    UserDocumentResponse uploadInvestorDocument(UserDocumentUploadRequest request);

    // ✅ NEW
    UserDocumentResponse uploadInvestorDocument(
            DocumentType documentType,
            MultipartFile file,
            String originalFileName,
            String resubmissionReason
    );

    List<UserDocumentResponse> getCurrentInvestorDocuments();

    List<UserDocumentResponse> getAllInvestorDocuments();

    List<UserDocumentResponse> getPendingDocuments(String role);
    List<UserDocumentResponse> getCurrentDocumentsByRole(String role);

    UserDocumentResponse approveDocument(Long documentId);

    UserDocumentResponse rejectDocument(Long documentId, String rejectionReason);

    void refreshBuilderKycStatus(Long userId);

    void refreshInvestorApprovalStatus(Long userId);

    List<UserDocumentResponse> getBuilderCurrentDocumentsForAdmin(Long builderId);

    List<UserDocumentResponse> getBuilderDocumentHistoryForAdmin(Long builderId);

    List<UserDocumentResponse> getDocumentsByRoleAndStatus(String role, String status);
}