package com.cts.mfrp.oa.controller;

import com.cts.mfrp.oa.dto.response.MenuItemResponse;
import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuItemController {

    @Autowired private MenuItemService menuItemService;

    // CREATE
    @PostMapping("/vendor/{vendorId}")
    public MenuItemResponse addMenuItem(@RequestBody MenuItem item, @PathVariable Integer vendorId) {
        return menuItemService.addMenuItem(item, vendorId);
    }

    // READ
    @GetMapping("/vendor/{vendorId}")
    public List<MenuItemResponse> getMenuItemsByVendor(@PathVariable Integer vendorId) {
        return menuItemService.getMenuItemsByVendor(vendorId);
    }

    // UPDATE
    @PutMapping("/vendor/{vendorId}/item/{itemId}")
    public MenuItemResponse updateMenuItem(@PathVariable Integer vendorId,
                                           @PathVariable Integer itemId,
                                           @RequestBody MenuItem updatedItem) {
        return menuItemService.updateMenuItem(vendorId, itemId, updatedItem);
    }

    // DELETE
    @DeleteMapping("/vendor/{vendorId}/item/{itemId}")
    public void deleteMenuItem(@PathVariable Integer vendorId,
                               @PathVariable Integer itemId) {
        menuItemService.deleteMenuItem(vendorId, itemId);
    }
}
