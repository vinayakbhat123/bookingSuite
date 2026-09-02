package com.vinayakbhat.bookingSuite.Booking.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.vinayakbhat.bookingSuite.Booking.Service.paymentService;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingRequest;
import com.vinayakbhat.bookingSuite.Booking.dto.BookingResponse;
import com.vinayakbhat.bookingSuite.common.security.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class StripeWebhookController {
    private final paymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(paymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<String>> capturePayment(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) throws SignatureVerificationException {
       try{
           Event event = Webhook.constructEvent(payload, signature, webhookSecret);
           log.info("Stripe webhook received: type={}, id={}", event.getType(), event.getId());
           paymentService.capturePayment(event);
           return ResponseEntity.ok(ApiResponse.success("Webhooks events successfully sent by Stripe"));

       } catch (SignatureVerificationException e){
           throw new RuntimeException("Signature verification failed: {}"+e.getMessage());
       }
    }
}
