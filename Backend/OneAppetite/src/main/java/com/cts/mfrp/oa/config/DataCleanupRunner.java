package com.cts.mfrp.oa.config;

import com.cts.mfrp.oa.model.MenuItem;
import com.cts.mfrp.oa.repository.MenuItemRepository;
import com.cts.mfrp.oa.repository.OrderItemRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class DataCleanupRunner {

    @Bean
    @Order(10)
    public ApplicationRunner menuItemDedupeRunner(MenuItemRepository menuRepo, OrderItemRepository orderItemRepo) {
        return args -> {
            List<MenuItem> all = menuRepo.findAll();
            Map<String, List<MenuItem>> groups = new HashMap<>();
            for (MenuItem m : all) {
                if (m.getVendor() == null || m.getItemName() == null || m.getMealCourse() == null) continue;
                String key = m.getVendor().getUserId() + "||"
                        + m.getItemName().trim().toLowerCase(Locale.ROOT) + "||"
                        + m.getMealCourse().trim().toLowerCase(Locale.ROOT);
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
            }

            int deleted = 0;
            int skipped = 0;
            for (List<MenuItem> group : groups.values()) {
                if (group.size() <= 1) continue;
                group.sort(Comparator.comparing(MenuItem::getItemId));
                for (int i = 1; i < group.size(); i++) {
                    MenuItem dup = group.get(i);
                    if (orderItemRepo.existsByMenuItem_ItemId(dup.getItemId())) {
                        skipped++;
                        continue;
                    }
                    menuRepo.delete(dup);
                    deleted++;
                }
            }

            if (deleted > 0 || skipped > 0) {
                System.out.println("[DataCleanupRunner] duplicate menu items deleted=" + deleted + ", skipped-due-to-order-refs=" + skipped);
            }
        };
    }
}
