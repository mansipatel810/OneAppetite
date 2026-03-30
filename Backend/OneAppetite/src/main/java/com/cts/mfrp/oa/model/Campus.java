package com.cts.mfrp.oa.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "campuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Campus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer campus_id;


    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;

    @Column(name = "campus_name")
    private String campusName;

    private String address;

    @JsonManagedReference
    @OneToMany(mappedBy = "campus")
    private List<Building> buildings;


}