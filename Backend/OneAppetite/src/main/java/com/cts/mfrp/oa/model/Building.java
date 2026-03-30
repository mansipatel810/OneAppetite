package com.cts.mfrp.oa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "BUILDINGS")
@Data
@NoArgsConstructor
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private Integer buildingId;

    @ManyToOne
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @Column(name = "building_name", nullable = false, length = 100)
    private String buildingName;
}
