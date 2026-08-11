package com.skala.shopping.notificationservice.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByMemberId(UUID memberId, Pageable pageable);

    Optional<Notification> findByIdAndMemberId(UUID id, UUID memberId);

    long countByMemberIdAndReadAtIsNull(UUID memberId);
}
