package com.cts.mfrp.oa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menu_items")
@Data
@NoArgsConstructor
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "meal_course", nullable = false, length = 50)
    private String mealCourse;

    @Column(name = "dietary_type", nullable = false, length = 50)
    private String dietaryType;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "is_in_stock", nullable = false)
    private Boolean isInStock = true;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor; // vendor is a User with Role.VENDOR

    private Integer minPrepTime;

    /**
     * Optimistic-locking version column. Hibernate increments this on every
     * UPDATE and rejects the save with ObjectOptimisticLockingFailureException
     * if another transaction has already moved the version forward — our
     * backstop against the "two users buy the last item" race condition.
     *
     * The cart placement flow ALSO acquires a pessimistic write lock on each
     * row (see {@link com.cts.mfrp.oa.repository.MenuItemRepository#findByIdForUpdate}),
     * so the @Version field is mostly belt-and-braces but matters for any code
     * path that updates the menu item without going through that lock.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
