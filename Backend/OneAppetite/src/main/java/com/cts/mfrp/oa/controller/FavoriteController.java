package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.MenuItemResponse;
import com.cts.mfrp.oa.model.FavoriteItem;
import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.FavoriteRepository;
import com.cts.mfrp.oa.repository.MenuItemRepository;
import com.cts.mfrp.oa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class FavoriteController {

    @Autowired private FavoriteRepository favoriteRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private MenuItemRepository menuItemRepo;

    @GetMapping("/{userId}/favorites")
    public List<MenuItemResponse> getFavorites(@PathVariable Integer userId) {
        User user = userRepo.findById(userId).orElseThrow();
        return favoriteRepo.findByUser(user).stream()
                .map(f -> toResponse(f.getMenuItem()))
                .toList();
    }

    @PostMapping("/{userId}/favorites/{itemId}")
    public String addFavorite(@PathVariable Integer userId, @PathVariable Integer itemId) {
        User user = userRepo.findById(userId).orElseThrow();
        MenuItem item = menuItemRepo.findById(itemId).orElseThrow();
        if (!favoriteRepo.existsByUserAndMenuItem(user, item)) {
            FavoriteItem fav = new FavoriteItem();
            fav.setUser(user);
            fav.setMenuItem(item);
            favoriteRepo.save(fav);
        }
        return "Added to favorites";
    }

    @DeleteMapping("/{userId}/favorites/{itemId}")
    public String removeFavorite(@PathVariable Integer userId, @PathVariable Integer itemId) {
        User user = userRepo.findById(userId).orElseThrow();
        MenuItem item = menuItemRepo.findById(itemId).orElseThrow();
        favoriteRepo.findByUserAndMenuItem(user, item).ifPresent(favoriteRepo::delete);
        return "Removed from favorites";
    }

    private MenuItemResponse toResponse(MenuItem m) {
        return new MenuItemResponse(
                m.getItemId(), m.getItemName(), m.getCategory(),
                m.getMealCourse(), m.getDietaryType(), m.getPrice(),
                m.getQuantityAvailable(), m.getIsInStock(), m.getImageUrl(),
                m.getVendor().getUserId(), m.getVendor().getVendorName(),
                m.getVendor().getVendorDescription(),m.getVendor().getVendorType()
        );
    }
}