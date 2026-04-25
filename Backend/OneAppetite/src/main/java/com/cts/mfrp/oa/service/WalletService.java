package com.cts.mfrp.oa.service;

import com.cts.mfrp.oa.dto.request.TopUpRequest;
import com.cts.mfrp.oa.dto.response.WalletResponse;
import com.cts.mfrp.oa.exception.InsufficientBalanceException;
import com.cts.mfrp.oa.exception.ResourceNotFoundException;
import com.cts.mfrp.oa.exception.TopUpLimitExceededException;
import com.cts.mfrp.oa.model.Role;
import com.cts.mfrp.oa.model.User;
import com.cts.mfrp.oa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final double dailyTopUpCap;

    public WalletService(UserRepository userRepository,
                         @Value("${app.wallet.daily-topup-cap:100000.0}") double dailyTopUpCap) {
        this.userRepository = userRepository;
        this.dailyTopUpCap = dailyTopUpCap;
    }

    public WalletResponse getBalance(Integer userId) {
        User user = loadEmployee(userId);
        return new WalletResponse(user.getUserId(), user.getWalletBalance());
    }

    @Transactional
    public WalletResponse topUp(Integer userId, TopUpRequest request) {
        User user = loadEmployeeForUpdate(userId);

        LocalDate today = LocalDate.now();
        LocalDate lastDate = user.getDailyTopUpDate();
        double dailyTotal = user.getDailyTopUpTotal() == null ? 0.0 : user.getDailyTopUpTotal();
        if (lastDate == null || !lastDate.equals(today)) {
            dailyTotal = 0.0;
            user.setDailyTopUpDate(today);
        }

        double projected = dailyTotal + request.amount();
        if (projected > dailyTopUpCap) {
            double remaining = Math.max(0.0, dailyTopUpCap - dailyTotal);
            throw new TopUpLimitExceededException(
                    "Daily top-up cap of " + dailyTopUpCap + " would be exceeded. Remaining today: " + remaining);
        }

        double current = user.getWalletBalance() == null ? 0.0 : user.getWalletBalance();
        user.setWalletBalance(current + request.amount());
        user.setDailyTopUpTotal(projected);
        User saved = userRepository.save(user);
        return new WalletResponse(saved.getUserId(), saved.getWalletBalance());
    }

    @Transactional
    public WalletResponse debit(Integer userId, double amount) {
        User user = loadEmployeeForUpdate(userId);
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

    private User loadEmployeeForUpdate(Integer userId) {
        User user = userRepository.findForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (user.getRole() != Role.EMPLOYEE) {
            throw new InsufficientBalanceException("Wallet is available only for employee accounts.");
        }
        return user;
    }
}
