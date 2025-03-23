package com.pos.posApps.Util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class Hash {

    private static final int SALT_LENGTH = 256;
    private static final int ITERATIONS = 210000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";

    // Generate a random salt
    public static byte[] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    //Convert salt from byte to string
    public static String saltToString(byte[] saltInput){
        return Base64.getEncoder().encodeToString(saltInput);
    }

    //Convert salt from string to byte
    public static byte[] stringToSalt(String saltInput){
        return Base64.getDecoder().decode(saltInput);
    }

    // Hash the password using PBKDF2
    public static String hashPassword(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] hashedPassword = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hashedPassword);
    }
}