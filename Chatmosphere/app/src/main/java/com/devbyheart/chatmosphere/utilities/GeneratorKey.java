package com.devbyheart.chatmosphere.utilities;

import java.security.SecureRandom;

public class GeneratorKey {
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String ALL_CHARACTERS = LOWERCASE + UPPERCASE + DIGITS;

    public static String generateSecureKey(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALL_CHARACTERS.length());
            sb.append(ALL_CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}