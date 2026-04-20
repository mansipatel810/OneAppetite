package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.TopUpRequest;
import com.cts.mfrp.oa.dto.response.WalletResponse;
import com.cts.mfrp.oa.exception.InsufficientBalanceException;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final UserRepository userRepository;

    public WalletService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public WalletResponse getBalance(Integer userId) {
        User user = loadEmployee(userId);
        return new WalletResponse(user.getUserId(), user.getWalletBalance());
    }

    @Transactional
    public WalletResponse topUp(Integer userId, TopUpRequest request) {
        User user = loadEmployee(userId);
        double current = user.getWalletBalance() == null ? 0.0 : user.getWalletBalance();
        user.setWalletBalance(current + request.amount());
        User saved = userRepository.save(user);
        return new WalletResponse(saved.getUserId(), saved.getWalletBalance());
    }

    @Transactional
    public WalletResponse debit(Integer userId, double amount) {
        User user = loadEmployee(userId);
        double current = user.getWalletBalance() == null ? 0.0 : user.getWalletBalance();
        if (current < amount) {
            throw new InsufficientBalanceException(
                    "Insufficient wallet balance. Required: " + amount + ", Available: " + current);
        }
        user.setWalletBalance(current - amount);
        User saved = userRepository.save(user);
        return new WalletResponse(saved.getUserId(), saved.getWalletBalance());
    }

    private User loadEmployee(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (user.getRole() != Role.EMPLOYEE) {
            throw new InsufficientBalanceException("Wallet is available only for employee accounts.");
        }
        return user;
    }
}
