package com.example.spip_draft.service;

import com.example.spip_draft.dto.investor.InvestorProfileResponse;

import com.example.spip_draft.entity.Investor;
import com.example.spip_draft.exception.ResourceNotFoundException;
import com.example.spip_draft.repository.InvestorRepository;
import com.example.spip_draft.repository.UserRepository;
import com.example.spip_draft.util.SecurityUtil;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestorService {

    private final InvestorRepository investorRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    // =========================
    // SAVE INVESTOR
    // =========================
    public Investor saveInvestor(Investor investor) {
        return investorRepository.save(investor);
    }

    // =========================
    // GET ALL INVESTORS (ADMIN)
    // =========================
    public List<Investor> getAllInvestors() {
        return investorRepository.findAll();
    }

    // =========================
    // GET INVESTOR BY ID (ADMIN)
    // =========================
    public Investor getInvestorById(Long id) {
        return investorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Investor not found with id: " + id));
    }

    // =========================
    // GET CURRENT INVESTOR ENTITY
    // PLATFORM-SCOPED: resolve by user only
    // =========================
    public Investor getCurrentInvestor() {

        Long userId = SecurityUtil.getCurrentUserId();

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User not found");
        }

        return investorRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Investor not found"));
    }

    // =========================
    // GET CURRENT INVESTOR PROFILE DTO
    // =========================
    public InvestorProfileResponse getCurrentInvestorProfile() {
        Investor investor = getCurrentInvestor();
        return mapToProfileResponse(investor);
    }

    // =========================
    // UPDATE PROFILE PHOTO
    // =========================
    public InvestorProfileResponse updateProfilePhoto(String photoUrl) {
        Investor investor = getCurrentInvestor();

        if (investor.getUser() == null) {
            throw new RuntimeException("User not found for investor");
        }

        investor.getUser().setProfilePhotoUrl(photoUrl);
        userRepository.save(investor.getUser());

        return mapToProfileResponse(investor);
    }

    // =========================
    // APPROVE INVESTOR (ADMIN)
    // =========================
    public void approveInvestor(Long id) {

        Investor investor = investorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Investor not found"));

        investor.setApproved(true);
        investorRepository.save(investor);
    }

    // =========================
    // DELETE INVESTOR
    // =========================
    public void deleteInvestor(Long id) {
        Investor investor = investorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));

        long activeBookings = bookingRepository.countByInvestorIdAndStatusIn(
                investor.getId(),
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
            throw new RuntimeException("Cannot delete investor because active bookings/investments exist");
        }

        if (investor.getUser() != null) {
            investor.getUser().setActive(false);
            userRepository.save(investor.getUser());
        }
    }

    private InvestorProfileResponse mapToProfileResponse(Investor investor) {
        InvestorProfileResponse response = new InvestorProfileResponse();

        response.setId(investor.getId());
        response.setUserId(investor.getUser() != null ? investor.getUser().getId() : null);
        response.setTenantId(investor.getTenantId());

        response.setFullName(
                investor.getUser() != null ? investor.getUser().getFullName() : null
        );
        response.setEmail(
                investor.getUser() != null ? investor.getUser().getEmail() : null
        );
        response.setPanCard(investor.getPanCard());

        response.setWalletBalance(investor.getWalletBalance());
        response.setRiskScore(investor.getRiskScore());
        response.setApproved(investor.isApproved());

        response.setProfilePhotoUrl(
                investor.getUser() != null ? investor.getUser().getProfilePhotoUrl() : null
        );

        return response;
    }
}