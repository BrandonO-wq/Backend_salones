package com.sportsbooking.mainapi.jpa.repository;

import com.sportsbooking.mainapi.jpa.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.sportsbooking.mainapi.jpa.entity.enums.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    boolean existsByRelatedIdAndType(UUID relatedId, NotificationType type);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") UUID userId);
}
