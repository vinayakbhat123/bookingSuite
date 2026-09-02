package com.vinayakbhat.bookingSuite.Booking.Service.Impl;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.vinayakbhat.bookingSuite.Booking.Entity.Booking;
import com.vinayakbhat.bookingSuite.Booking.Enum.BookingStatus;
import com.vinayakbhat.bookingSuite.Booking.Service.paymentService;
import com.vinayakbhat.bookingSuite.Booking.repository.BookingRepository;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import com.vinayakbhat.bookingSuite.Inventory.repository.InventoryRepository;
import com.vinayakbhat.bookingSuite.common.security.Exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
public class PaymentServiceImpl implements paymentService {

    private final BookingRepository bookingRepository;
    private final InventoryRepository inventoryRepository;
    private final StripeClient stripeClient;

    public PaymentServiceImpl(BookingRepository bookingRepository, InventoryRepository inventoryRepository, StripeClient stripeClient) {
        this.bookingRepository = bookingRepository;
        this.inventoryRepository = inventoryRepository;
        this.stripeClient = stripeClient;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        // 1. Fail fast on unhandled events (No DB interaction yet, safe to return)
        if (!"checkout.session.completed".equals(event.getType())) {
            log.warn("Unhandled Stripe event type: {}, id: {}", event.getType(), event.getId());
            return;
        }

        log.info("Processing checkout.session.completed event: {}", event.getId());

        try {
            // 2. Prevent silent failures on deserialization
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) {
                throw new IllegalStateException("Stripe session deserialization failed for event: " + event.getId());
            }

            String sessionId = session.getId();
            Booking booking = bookingRepository.findByStripeSessionId(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found for Stripe Session Id: " + sessionId));

            // 3. Idempotency Check
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                log.info("Booking {} is already CONFIRMED. Ignoring duplicate webhook.", booking.getId());
                return;
            }

            // 4. Validate Inventory Lock
            List<Inventory> inventoryList = inventoryRepository.findAvailableInventoryForUpdate(
                    booking.getRoom().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getRoomsCount()
            );

            long dateCount = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
            if (inventoryList == null || inventoryList.size() != dateCount) {
                throw new IllegalStateException("Inventory shortage for booking " + booking.getId() + ". Payment succeeded but rooms are unavailable.");
            }

            // 5. Commit changes
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            inventoryRepository.confirmReservedBooking(
                    booking.getRoom().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getRoomsCount()
            );

            log.info("Capture Payment Successful for booking ID: {}", booking.getId());

        } catch (CannotAcquireLockException e) {
            // Spring throws this if the Pessimistic Lock times out.
            // This is a RETRYABLE error. We log it and re-throw it so the Controller returns a 500,
            // prompting Stripe to retry the webhook later when the database is less busy.
            log.error("Database lock timeout while processing payment for event: {}. Stripe should retry.", event.getId(), e);
            throw e;

        } catch (ResourceNotFoundException e) {
            // A race condition can occur where the webhook arrives BEFORE your local DB commits the initial booking.
            // Rethrowing allows Stripe to retry in a few minutes, by which time the DB might have the booking.
            log.error("Resource not found for event {}: {}", event.getId(), e.getMessage());
            throw e;

        } catch (IllegalStateException e) {
            log.error("CRITICAL BUSINESS ERROR for event {}: {}", event.getId(), e.getMessage());

            try {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null && session.getPaymentIntent() != null) {
                    String sessionId = session.getId();
                    Booking booking = bookingRepository.findByStripeSessionId(sessionId)
                            .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found for Stripe Session Id: " + sessionId));

                    // 1. Issue the Refund
                    RefundCreateParams params = RefundCreateParams.builder()
                            .setPaymentIntent(session.getPaymentIntent())
                            .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                            .build();
                    Refund refund = stripeClient.v1().refunds().create(params);
                    log.info("Refund successful. Stripe Refund ID: {}", refund.getId());

                    // 2. Mark Booking as Cancelled/Refunded
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                }
            } catch (StripeException stripeException) {
                log.error("URGENT: Failed to issue automatic refund for event {}.", event.getId(), stripeException);
                throw new RuntimeException("Refund processing failed", stripeException);
            }

            // CRITICAL FIX: Do NOT put `throw e;` or `throw new RuntimeException()` here.
            // Let the block end naturally so the Controller returns 200 OK.
        } catch (Exception e) {
            // Catch-all for unexpected DB failures, NullPointerExceptions, or network drops.
            log.error("Unexpected system error processing Stripe event {}: {}", event.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process payment event", e); // Wrap and rethrow
        }
    }
}