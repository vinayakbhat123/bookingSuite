package com.vinayakbhat.bookingSuite.util;
import java.security.SecureRandom;
public final class RoomNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private RoomNumberGenerator() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Generates a random room number in the format: {floor}{roomIndex}-{letter}
     * Examples: 101-A, 102-C, 405-H
     *
     * @param floor The floor number (e.g., 1, 2, 10)
     * @return Generated room number String
     */
    public static String generateRoomNumber(int floor) {
        // Random 2-digit room number per floor (01 to 99)
        int roomIndex = RANDOM.nextInt(99) + 1;
        
        // Random uppercase letter (A-Z)
        char suffix = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));

        return String.format("%d%02d-%c", floor, roomIndex, suffix);
    }
}