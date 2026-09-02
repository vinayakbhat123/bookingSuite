package com.vinayakbhat.bookingSuite.Booking.Service.Impl;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Booking.Entity.Booking;
import com.vinayakbhat.bookingSuite.Booking.Service.CheckoutService;
import com.vinayakbhat.bookingSuite.Booking.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {
    private final BookingRepository bookingRepository;
    private final StripeClient stripeClient;

    public CheckoutServiceImpl(BookingRepository bookingRepository, StripeClient stripeClient) {
        this.bookingRepository = bookingRepository;
        this.stripeClient = stripeClient;
    }

    @Override
    @Transactional
    public String getCheckoutSession(Booking booking, String successUrl, String failureUrl) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try{
            assert user != null;
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getName())
                    .build();
            Customer customer = stripeClient.v1().customers().create(customerParams);

            String formattedSuccessUrl = successUrl.contains("?")
                    ? successUrl + "&bookingId=" + booking.getId() + "&session_id={CHECKOUT_SESSION_ID}"
                    : successUrl + "?bookingId=" + booking.getId() + "&session_id={CHECKOUT_SESSION_ID}";

            String formattedFailureUrl = failureUrl.contains("?")
                    ? failureUrl + "&bookingId=" + booking.getId()
                    : failureUrl + "?bookingId=" + booking.getId();

            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setCustomer(customer.getId())
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                    .setSuccessUrl(formattedSuccessUrl)
                    .setCancelUrl(formattedFailureUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr")
                                                    .setUnitAmount(booking.getPrice().multiply(BigDecimal.valueOf(100)).longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(booking.getHotel().getHotelName()+":"+booking.getRoom().getRoomType())
                                                                    .setDescription("Booking Id:"+booking.getId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )

                                    .build()
                    )
                    .build();
            Session session = stripeClient.v1().checkout().sessions().create(sessionParams);
            booking.setStripeSessionId(session.getId());
            bookingRepository.save(booking);
            return session.getUrl();

        } catch(StripeException ex){
            throw new RuntimeException(ex);
        }

    }
}
