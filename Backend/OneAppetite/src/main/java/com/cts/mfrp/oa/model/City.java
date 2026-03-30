package com.cts.mfrp.oa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer city_id;

    @Column(name = "city_name")
    private String cityName;

    @JsonManagedReference
    @OneToMany(mappedBy = "city")
    private List<Campus> campuses;
}