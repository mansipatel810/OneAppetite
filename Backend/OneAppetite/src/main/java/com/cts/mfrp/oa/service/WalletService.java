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
    private final NotificationService notificationService;

    public WalletService(UserRepository userRepository,
                         NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
        notificationService.push(userId,
                String.format("Wallet topped up by ₹%.0f", request.amount()));
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

    /**
     * Credits earnings to a vendor's wallet. Used by the order placement flow
     * to settle each vendor's share of a (potentially multi-vendor) order so
     * the money actually moves rather than disappearing.
     *
     * Failures here are intentionally swallowed inside placeOrder — see
     * OrderItemService — so a credit problem does not abort the customer's
     * payment. If the vendor row is missing, we log and return null.
     */
    @Transactional
    public WalletResponse creditVendor(Integer vendorId, double amount) {
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with ID: " + vendorId));
        if (vendor.getRole() != Role.VENDOR) {
            // Don't block the customer payment; just refuse the credit.
            return new WalletResponse(vendor.getUserId(),
                    vendor.getWalletBalance() == null ? 0.0 : vendor.getWalletBalance());
        }
        double current = vendor.getWalletBalance() == null ? 0.0 : vendor.getWalletBalance();
        vendor.setWalletBalance(current + amount);
        User saved = userRepository.save(vendor);
        notificationService.push(vendorId,
                String.format("₹%.0f credited to your earnings", amount));
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
