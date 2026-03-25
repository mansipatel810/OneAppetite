package com.cts.mfrp.oa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseCheckController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/db-check")
    public String checkConnection() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                return "OneAppetite is connected to Aiven MySQL!";
            } else {
                return "Connection established, but query failed.";
            }
        } catch (Exception e) {
            return "Connection Failed: " + e.getMessage();
        }
    }
}