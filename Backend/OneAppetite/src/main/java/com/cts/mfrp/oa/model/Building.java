package com.cts.mfrp.oa.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "buildings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Building {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer buildingId;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @Column(name = "building_name")
    private String buildingName;
}