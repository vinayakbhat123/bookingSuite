package com.vinayakbhat.bookingSuite.Auth.Service.Impl;

import com.vinayakbhat.bookingSuite.Auth.Entity.EmailOtp;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Auth.Service.EmailOtpAuthService;
import com.vinayakbhat.bookingSuite.Auth.dto.LoginResponse;
import com.vinayakbhat.bookingSuite.Auth.repository.EmailOtpRepository;
import com.vinayakbhat.bookingSuite.Auth.repository.UserRepository;
import com.vinayakbhat.bookingSuite.common.notification.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class EmailOtpAuthServiceImpl implements EmailOtpAuthService {
    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final JWTService jwtService;
    private final EmailService emailService;

    public EmailOtpAuthServiceImpl(EmailOtpRepository otpRepository, UserRepository userRepository, JavaMailSender mailSender, JWTService jwtService, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void sendOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required");
        }
        String normalizedEmail = email.toLowerCase().trim();
        String otp = String.format("%06d", new Random().nextInt(1000000));
        // Case-insensitive lookup to prevent duplicate key constraint violations
        EmailOtp emailOtp = otpRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
            EmailOtp newOtp = new EmailOtp();
            newOtp.setEmail(normalizedEmail);
            return newOtp;
        });
        emailOtp.setOtpCode(otp);
        emailOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        // Save and flush immediately to force SQL execution and catch errors
        otpRepository.saveAndFlush(emailOtp);
        // Sending mail
        Map<String, Object> emailVariables = new HashMap<>();
        emailVariables.put("userEmail", normalizedEmail);
        emailVariables.put("otp", otp);
        emailService.sendDynamicHtmlEmail(
                email, "Your BookingSuite Verification Code", "send-otp",
                emailVariables
        );
    }

    @Override
    @Transactional
    public LoginResponse verifyOtpAndLogin(String email, String otpCode) {
        EmailOtp storedOtp = otpRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("OTP not requested or expired"));

        if (!storedOtp.getOtpCode().equals(otpCode) || LocalDateTime.now().isAfter(storedOtp.getExpiresAt())) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        User user = (User) userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Auto-register user if they don't exist yet
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName("Guest");
                    newUser.setRoles(Set.of(Role.GUEST));
                    return userRepository.save(newUser);
                });

        otpRepository.deleteByEmail(email); // Invalidate OTP after success
        String accessToken = jwtService.generateAccessToken(user);

        return new LoginResponse(accessToken);
    }
    // Runs every 5 minutes (300,000 milliseconds)
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void purgeExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpRepository.deleteByExpiresAtBefore(now);
    }
}
