package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class TokenService {

    @Autowired
    private OrderRepository orderRepo;

    private final SecureRandom rng = new SecureRandom();

    public String generateUniqueToken() {
        String token;
        do {
            token = "OA-" + (1000 + rng.nextInt(9000));
        } while (orderRepo.existsByTokenNumber(token));
        return token;
    }
}
