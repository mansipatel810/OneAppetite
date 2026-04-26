package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.response.MenuItemResponse;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.MenuItemRepository;
import com.cts.mfrp.oa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuItemService {

    @Autowired private MenuItemRepository menuItemRepo;
    @Autowired private UserRepository userRepo;

    private User validateVendor(Integer vendorId) {
        User vendor = userRepo.findById(vendorId).orElseThrow();
        if (vendor.getRole() != Role.VENDOR) {
            throw new IllegalArgumentException("User is not a vendor");
        }
        return vendor;
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getItemId(),
                item.getItemName(),
                item.getCategory(),
                item.getMealCourse(),
                item.getDietaryType(),
                item.getPrice(),
                item.getQuantityAvailable(),
                item.getIsInStock(),
                item.getImageUrl(),
                item.getVendor().getUserId(),
                item.getVendor().getVendorName(),
                item.getVendor().getVendorDescription(),
                item.getVendor().getVendorType()
        );
    }

    // CREATE
    public MenuItemResponse addMenuItem(MenuItem item, Integer vendorId) {
        User vendor = validateVendor(vendorId);
        item.setVendor(vendor);
        MenuItem saved = menuItemRepo.save(item);
        return toResponse(saved);
    }

    // READ
    public List<MenuItemResponse> getMenuItemsByVendor(Integer vendorId) {
        User vendor = validateVendor(vendorId);
        return menuItemRepo.findByVendor(vendor).stream()
                .map(this::toResponse)
                .toList();
    }

    public MenuItemResponse toggleStock(Integer vendorId, Integer itemId) {
        User vendor = validateVendor(vendorId);
        MenuItem existing = menuItemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));

        if (!existing.getVendor().equals(vendor)) {
            throw new IllegalArgumentException("Vendor does not own this menu item");
        }

        existing.setIsInStock(!existing.getIsInStock());
        if (existing.getQuantityAvailable() == null) {
            existing.setQuantityAvailable(0);
        }
        MenuItem saved = menuItemRepo.save(existing);
        return toResponse(saved);
    }

    // UPDATE
    public MenuItemResponse updateMenuItem(Integer vendorId, Integer itemId, MenuItem updatedItem) {
        User vendor = validateVendor(vendorId);
        MenuItem existing = menuItemRepo.findById(itemId).orElseThrow();

        if (!existing.getVendor().equals(vendor)) {
            throw new IllegalArgumentException("Vendor does not own this menu item");
        }

        existing.setItemName(updatedItem.getItemName());
        existing.setCategory(updatedItem.getCategory());
        existing.setMealCourse(updatedItem.getMealCourse());
        existing.setDietaryType(updatedItem.getDietaryType());
        existing.setPrice(updatedItem.getPrice());
        existing.setQuantityAvailable(updatedItem.getQuantityAvailable());
        existing.setIsInStock(updatedItem.getIsInStock());
        existing.setImageUrl(updatedItem.getImageUrl());

        MenuItem saved = menuItemRepo.save(existing);
        return toResponse(saved);
    }

    // DELETE
    public void deleteMenuItem(Integer vendorId, Integer itemId) {
        User vendor = validateVendor(vendorId);
        MenuItem existing = menuItemRepo.findById(itemId).orElseThrow();

        if (!existing.getVendor().equals(vendor)) {
            throw new IllegalArgumentException("Vendor does not own this menu item");
        }

        menuItemRepo.delete(existing);
    }
}
