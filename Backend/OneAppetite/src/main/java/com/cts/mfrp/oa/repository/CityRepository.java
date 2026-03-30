package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {}