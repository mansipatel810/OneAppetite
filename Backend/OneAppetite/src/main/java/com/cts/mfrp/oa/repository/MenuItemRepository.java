package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.model.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    List<MenuItem> findByVendor(User vendor);

    /**
     * Pessimistic-write lookup used during order placement. Acquires a row
     * lock that other transactions can't read-for-update or write until the
     * calling transaction commits — serializes the "two users buy the last
     * item" scenario so the second one sees the post-decrement quantity and
     * is rejected cleanly.
     *
     * <p>The 5-second timeout means a stuck placement won't deadlock the
     * whole menu — the second transaction times out and rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({ @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") })
    @Query("select m from MenuItem m where m.itemId = :id")
    Optional<MenuItem> findByIdForUpdate(@Param("id") Integer id);
}
