package com.vinayakbhat.bookingSuite.Booking.Service;

import com.vinayakbhat.bookingSuite.Booking.Entity.Booking;

public interface CheckoutService {
    String getCheckoutSession(Booking booking,String successUrl,String failureUrl);
}
