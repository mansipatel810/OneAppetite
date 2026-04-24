package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.MenuItemResponse;
import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.service.AuthGuardService;
import com.cts.mfrp.oa.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuItemController {

    @Autowired private MenuItemService menuItemService;
    @Autowired private AuthGuardService authGuard;

    // CREATE
    @PostMapping("/vendor/{vendorId}")
    public MenuItemResponse addMenuItem(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @RequestBody MenuItem item,
            @PathVariable Integer vendorId) {
        authGuard.verifyVendorSelf(callerId, vendorId);
        return menuItemService.addMenuItem(item, vendorId);
    }

    // READ
    @GetMapping("/vendor/{vendorId}")
    public List<MenuItemResponse> getMenuItemsByVendor(@PathVariable Integer vendorId) {
        return menuItemService.getMenuItemsByVendor(vendorId);
    }

    // UPDATE
    @PutMapping("/vendor/{vendorId}/item/{itemId}")
    public MenuItemResponse updateMenuItem(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer vendorId,
            @PathVariable Integer itemId,
            @RequestBody MenuItem updatedItem) {
        authGuard.verifyVendorSelf(callerId, vendorId);
        return menuItemService.updateMenuItem(vendorId, itemId, updatedItem);
    }

    // DELETE
    @DeleteMapping("/vendor/{vendorId}/item/{itemId}")
    public void deleteMenuItem(
            @RequestHeader(value = "X-User-Id", required = false) Integer callerId,
            @PathVariable Integer vendorId,
            @PathVariable Integer itemId) {
        authGuard.verifyVendorSelf(callerId, vendorId);
        menuItemService.deleteMenuItem(vendorId, itemId);
    }
    @GetMapping("/vendor/{vendorId}/breakfast")
    public List<MenuItemResponse> getBreakfastItems(@PathVariable Integer vendorId) {
        return menuItemService.getMenuItemsByVendor(vendorId)
                .stream()
                .filter(m -> m.mealCourse().equalsIgnoreCase("Breakfast"))
                .collect(Collectors.toList());
    }

    @GetMapping("/vendor/{vendorId}/lunch")
    public List<MenuItemResponse> getLunchItems(@PathVariable Integer vendorId) {
        return menuItemService.getMenuItemsByVendor(vendorId)
                .stream()
                .filter(m -> m.mealCourse().equalsIgnoreCase("Lunch"))
                .collect(Collectors.toList());
    }

    @GetMapping("/vendor/{vendorId}/dinner")
    public List<MenuItemResponse> getDinnerItems(@PathVariable Integer vendorId) {
        return menuItemService.getMenuItemsByVendor(vendorId)
                .stream()
                .filter(m -> m.mealCourse().equalsIgnoreCase("Dinner"))
                .collect(Collectors.toList());
    }

}
