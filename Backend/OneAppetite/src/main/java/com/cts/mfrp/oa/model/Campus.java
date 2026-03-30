package com.cts.mfrp.oa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CAMPUSES")
@Data
@NoArgsConstructor
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campus_id")
    private Integer campusId;

    @ManyToOne
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "campus_name", nullable = false, length = 100)
    private String campusName;

    @Column(length = 255)
    private String address;
}
