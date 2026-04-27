package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Building;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);

    // Custom query to fetch vendors by building (kept for backwards compat)
    List<User> findByBuildingAndRole(Building building, Role role);

    /**
     * Vendors whose primary or additional building matches the given ID.
     * `LEFT JOIN` on additionalBuildings + `DISTINCT` so we don't return the
     * same vendor twice when both their primary and an extra building match.
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN u.additionalBuildings ab " +
           "WHERE u.role = :role " +
           "AND (u.building.buildingId = :buildingId OR ab.buildingId = :buildingId)")
    List<User> findVendorsByBuildingId(
            @Param("buildingId") Integer buildingId,
            @Param("role") Role role);

    List<User> findByRole(Role role);
}
