package com.vinayakbhat.bookingSuite.Booking.Service;

import com.stripe.model.Event;

public interface paymentService {
    void capturePayment(Event event);
}

