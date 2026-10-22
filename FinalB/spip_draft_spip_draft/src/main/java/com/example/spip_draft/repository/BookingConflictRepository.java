package com.example.spip_draft.repository;

import com.example.spip_draft.entity.BookingConflictLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingConflictRepository
        extends JpaRepository<BookingConflictLog, Long> {
}