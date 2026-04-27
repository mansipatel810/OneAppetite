package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findTop20ByUserIdOrderByTimestampDesc(Integer userId);

    long countByUserIdAndIsReadFalse(Integer userId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.userId = :userId and n.isRead = false")
    int markAllAsRead(@Param("userId") Integer userId);
}
