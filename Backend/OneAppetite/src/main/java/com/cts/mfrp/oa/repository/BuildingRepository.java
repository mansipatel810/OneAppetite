package com.cts.mfrp.oa.repository;

import com.cts.mfrp.oa.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Integer> {
    @Query("SELECT b FROM Building b WHERE b.campus.campus_id = :campusId")
    List<Building> findByCampusId(@Param("campusId") Integer campusId);
}