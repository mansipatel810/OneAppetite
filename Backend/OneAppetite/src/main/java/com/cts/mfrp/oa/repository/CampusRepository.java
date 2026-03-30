package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Integer> {
    @Query("SELECT c FROM Campus c WHERE c.city.city_id = :cityId")
    List<Campus> findByCityId(@Param("cityId") Integer cityId);
}