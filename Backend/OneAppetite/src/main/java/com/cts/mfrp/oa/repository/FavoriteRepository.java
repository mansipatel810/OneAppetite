package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.FavoriteItem;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<FavoriteItem, Integer> {
    List<FavoriteItem> findByUser(User user);
    Optional<FavoriteItem> findByUserAndMenuItem(User user, MenuItem menuItem);
    boolean existsByUserAndMenuItem(User user, MenuItem menuItem);
}