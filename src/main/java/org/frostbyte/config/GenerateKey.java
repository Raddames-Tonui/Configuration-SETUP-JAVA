package org.frostbyte.config;

import java.security.SecureRandom;
import java.util.Base64;

public class GenerateKey {
    public static void main(String[] args) {
        byte[] key = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(key);
        String base64Key = Base64.getEncoder().encodeToString(key);
        System.out.println("CONFIG_MASTER_KEY=" + base64Key);
    }
}
