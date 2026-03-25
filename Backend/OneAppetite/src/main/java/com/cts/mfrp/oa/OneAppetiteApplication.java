package com.cts.mfrp.oa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OneAppetiteApplication {

	public static void main(String[] args) {

        SpringApplication.run(OneAppetiteApplication.class, args);
        System.out.println("hello");
        System.out.println(" OneAppetite Backend is Running on Port 8081!");
	}

}
