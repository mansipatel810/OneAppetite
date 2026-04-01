package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    List<MenuItem> findByVendor(User vendor);
}
