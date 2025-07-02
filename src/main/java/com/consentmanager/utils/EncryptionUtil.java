package com.consentmanager.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);

    private static final String AES = "AES";
    private static final String RSA = "RSA";
    private static final int RSA_KEY_SIZE = 2048; // Standard RSA key size

    // For simulating Provider's/System's RSA key pair for encrypting DataItem's AES key
    public static final String SYSTEM_RSA_PUBLIC_KEY_STRING;
    private static final String SYSTEM_RSA_PRIVATE_KEY_STRING;

    static {
        try {
            KeyPair systemKeyPair = generateRsaKeyPair();
            SYSTEM_RSA_PUBLIC_KEY_STRING = publicKeyToString(systemKeyPair.getPublic());
            SYSTEM_RSA_PRIVATE_KEY_STRING = privateKeyToString(systemKeyPair.getPrivate());
            logger.info("Initialized System RSA Public Key: {}", SYSTEM_RSA_PUBLIC_KEY_STRING);
            // DO NOT log the private key in a real application
            // logger.info("Initialized System RSA Private Key (FOR DEMO ONLY): {}", SYSTEM_RSA_PRIVATE_KEY_STRING);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to initialize system RSA key pair.", e);
            throw new RuntimeException("Could not initialize system RSA key pair", e);
        }
    }

    /**
     * Returns the system's hardcoded/static private RSA key string.
     * WARNING: This is for demonstration/simulation purposes only where the system acts as a key holder.
     * In a real production system, private keys must be managed with extreme care and not exposed like this.
     * @return The system's private RSA key as a Base64 encoded string.
     */
    public static String getSystemRsaPrivateKeyString() {
        return SYSTEM_RSA_PRIVATE_KEY_STRING;
    }

    // --- AES Methods ---

    public static byte[] encryptAES(byte[] plainBytes, String keyString) {
        try {
            SecretKey secretKey = aesKeyFromString(keyString);
            Cipher cipher = Cipher.getInstance(AES); // Consider AES/GCM/NoPadding for more security if IVs are managed
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(plainBytes);
        } catch (Exception e) {
            logger.error("AES Byte Encryption error: {}", e.getMessage(), e);
            throw new RuntimeException("AES Byte Encryption error", e);
        }
    }

    public static byte[] decryptAES(byte[] encryptedBytes, String keyString) {
        try {
            SecretKey secretKey = aesKeyFromString(keyString);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedBytes);
        } catch (Exception e) {
            logger.error("AES Byte Decryption error: {}", e.getMessage(), e);
            throw new RuntimeException("AES Byte Decryption error", e);
        }
    }


    /**
     * Generates a new AES key.
     * @return Base64 encoded string of the AES key.
     * @throws NoSuchAlgorithmException if AES algorithm is not available.
     */
    public static String generateAesKeyString() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES);
        keyGen.init(256); // AES-256
        SecretKey secretKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    private static SecretKey aesKeyFromString(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, AES);
    }

    /**
     * Encrypts text using AES.
     * @param plainText The text to encrypt.
     * @param keyString Base64 encoded AES key.
     * @return Base64 encoded encrypted string.
     */
    public static String encryptAES(String plainText, String keyString) {
        try {
            SecretKey secretKey = aesKeyFromString(keyString);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.error("AES Encryption error: {}", e.getMessage(), e);
            throw new RuntimeException("AES Encryption error", e);
        }
    }

    /**
     * Decrypts text using AES.
     * @param encryptedText Base64 encoded encrypted text.
     * @param keyString Base64 encoded AES key.
     * @return Decrypted string.
     */
    public static String decryptAES(String encryptedText, String keyString) {
        try {
            SecretKey secretKey = aesKeyFromString(keyString);
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("AES Decryption error: {}", e.getMessage(), e);
            throw new RuntimeException("AES Decryption error", e);
        }
    }

    // --- RSA Methods ---

    /**
     * Generates an RSA KeyPair.
     * @return KeyPair object.
     * @throws NoSuchAlgorithmException if RSA algorithm is not available.
     */
    public static KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA);
        keyPairGen.initialize(RSA_KEY_SIZE);
        return keyPairGen.generateKeyPair();
    }

    /**
     * Converts a PublicKey to its Base64 encoded string representation.
     * @param publicKey The PublicKey object.
     * @return Base64 encoded string.
     */
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Converts a PrivateKey to its Base64 encoded string representation.
     * @param privateKey The PrivateKey object.
     * @return Base64 encoded string.
     */
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Converts a Base64 encoded string back to a PublicKey.
     * @param keyString Base64 encoded public key string.
     * @return PublicKey object.
     */
    public static PublicKey stringToPublicKey(String keyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            logger.error("Error converting string to PublicKey: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting string to PublicKey", e);
        }
    }

    /**
     * Converts a Base64 encoded string back to a PrivateKey.
     * @param keyString Base64 encoded private key string.
     * @return PrivateKey object.
     */
    public static PrivateKey stringToPrivateKey(String keyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            logger.error("Error converting string to PrivateKey: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting string to PrivateKey", e);
        }
    }

    /**
     * Encrypts data using an RSA Public Key.
     * @param data The data to encrypt (e.g., an AES key string).
     * @param publicKey The RSA PublicKey.
     * @return Base64 encoded encrypted string.
     */
    public static String encryptRSA(String data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.error("RSA Encryption error: {}", e.getMessage(), e);
            throw new RuntimeException("RSA Encryption error", e);
        }
    }

    /**
     * Encrypts data using an RSA Public Key (from string).
     * @param data The data to encrypt (e.g., an AES key string).
     * @param publicKeyString Base64 encoded RSA PublicKey.
     * @return Base64 encoded encrypted string.
     */
    public static String encryptRSA(String data, String publicKeyString) {
        return encryptRSA(data, stringToPublicKey(publicKeyString));
    }


    /**
     * Decrypts data using an RSA Private Key.
     * @param encryptedData Base64 encoded encrypted data.
     * @param privateKey The RSA PrivateKey.
     * @return Decrypted string.
     */
    public static String decryptRSA(String encryptedData, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("RSA Decryption error: {}", e.getMessage(), e);
            throw new RuntimeException("RSA Decryption error", e);
        }
    }

    /**
     * Decrypts data using an RSA Private Key (from string).
     * @param encryptedData Base64 encoded encrypted data.
     * @param privateKeyString Base64 encoded RSA PrivateKey.
     * @return Decrypted string.
     */
    public static String decryptRSA(String encryptedData, String privateKeyString) {
        return decryptRSA(encryptedData, stringToPrivateKey(privateKeyString));
    }

    public static void main(String[] args) {
        try {
            // AES Test
            String aesKey = generateAesKeyString();
            logger.info("Generated AES Key: {}", aesKey);
            String originalText = "This is a secret message for AES!";
            String encryptedAesText = encryptAES(originalText, aesKey);
            logger.info("AES Encrypted: {}", encryptedAesText);
            String decryptedAesText = decryptAES(encryptedAesText, aesKey);
            logger.info("AES Decrypted: {}", decryptedAesText);
            assert originalText.equals(decryptedAesText);
            logger.info("AES test successful.");

            // RSA Test
            KeyPair keyPair = generateRsaKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            String publicKeyStr = publicKeyToString(publicKey);
            String privateKeyStr = privateKeyToString(privateKey);
            logger.info("RSA Public Key: {}", publicKeyStr);
            // logger.info("RSA Private Key: {}", privateKeyStr); // Don't log private key typically

            String originalRsaText = "This is a secret message for RSA!"; // Often an AES key
            String encryptedRsaText = encryptRSA(originalRsaText, publicKey);
            logger.info("RSA Encrypted: {}", encryptedRsaText);
            String decryptedRsaText = decryptRSA(encryptedRsaText, privateKey);
            logger.info("RSA Decrypted: {}", decryptedRsaText);
            assert originalRsaText.equals(decryptedRsaText);
            logger.info("RSA test with Key objects successful.");

            String encryptedRsaTextFromStringKey = encryptRSA(originalRsaText, publicKeyStr);
            logger.info("RSA Encrypted (from string pubkey): {}", encryptedRsaTextFromStringKey);
            String decryptedRsaTextFromStringKey = decryptRSA(encryptedRsaTextFromStringKey, privateKeyStr);
            logger.info("RSA Decrypted (from string privkey): {}", decryptedRsaTextFromStringKey);
            assert originalRsaText.equals(decryptedRsaTextFromStringKey);
            logger.info("RSA test with String keys successful.");


        } catch (Exception e) {
            logger.error("EncryptionUtil main test failed.", e);
        }
    }
}
