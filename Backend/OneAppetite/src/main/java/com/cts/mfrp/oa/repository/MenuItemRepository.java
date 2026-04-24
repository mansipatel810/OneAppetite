package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    List<MenuItem> findByVendor(User vendor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MenuItem m where m.itemId = :id")
    Optional<MenuItem> findForUpdate(@Param("id") Integer id);

    Optional<MenuItem> findFirstByVendor_UserIdAndItemNameIgnoreCaseAndMealCourseIgnoreCase(
            Integer vendorId, String itemName, String mealCourse);
}
