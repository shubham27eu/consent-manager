package com.consentmanager.utils;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionUtilTest {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtilTest.class);

    @Test
    void testGenerateAesKeyString() throws NoSuchAlgorithmException {
        String keyString = EncryptionUtil.generateAesKeyString();
        assertNotNull(keyString);
        assertFalse(keyString.isEmpty());
        logger.info("Generated AES Key String: {}", keyString);
        // We could also try to decode it to check length if needed, but not strictly necessary for this test
    }

    @Test
    void testAesEncryptionDecryption_validKey() {
        try {
            String keyString = EncryptionUtil.generateAesKeyString();
            String originalText = "Hello, AES! This is a secret message.";

            String encryptedText = EncryptionUtil.encryptAES(originalText, keyString);
            assertNotNull(encryptedText);
            assertNotEquals(originalText, encryptedText);
            logger.info("AES Encrypted: {}", encryptedText);

            String decryptedText = EncryptionUtil.decryptAES(encryptedText, keyString);
            assertEquals(originalText, decryptedText);
            logger.info("AES Decrypted: {}", decryptedText);

        } catch (NoSuchAlgorithmException e) {
            fail("AES algorithm not found", e);
        }
    }

    @Test
    void testAesEncryptionDecryption_differentKeysFail() throws NoSuchAlgorithmException {
        String keyString1 = EncryptionUtil.generateAesKeyString();
        String keyString2 = EncryptionUtil.generateAesKeyString();
        assertNotEquals(keyString1, keyString2);

        String originalText = "Test with different keys";
        String encryptedText = EncryptionUtil.encryptAES(originalText, keyString1);

        assertThrows(RuntimeException.class, () -> {
            EncryptionUtil.decryptAES(encryptedText, keyString2);
        }, "Decryption with a different key should fail (likely throw RuntimeException wrapping crypto error)");
    }


    @Test
    void testGenerateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPair keyPair = EncryptionUtil.generateRsaKeyPair();
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
        assertEquals("RSA", keyPair.getPublic().getAlgorithm());
        assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
        logger.info("RSA Key Pair generated successfully.");
    }

    @Test
    void testRsaKeyToStringAndBackConversion() throws NoSuchAlgorithmException {
        KeyPair keyPair = EncryptionUtil.generateRsaKeyPair();
        PublicKey originalPublicKey = keyPair.getPublic();
        PrivateKey originalPrivateKey = keyPair.getPrivate();

        String publicKeyStr = EncryptionUtil.publicKeyToString(originalPublicKey);
        assertNotNull(publicKeyStr);
        logger.info("Public Key String: {}", publicKeyStr.substring(0, Math.min(30, publicKeyStr.length()))+ "...");


        String privateKeyStr = EncryptionUtil.privateKeyToString(originalPrivateKey);
        assertNotNull(privateKeyStr);
        // logger.info("Private Key String: {}", privateKeyStr); // Avoid logging full private key

        PublicKey restoredPublicKey = EncryptionUtil.stringToPublicKey(publicKeyStr);
        PrivateKey restoredPrivateKey = EncryptionUtil.stringToPrivateKey(privateKeyStr);

        assertEquals(originalPublicKey, restoredPublicKey, "Restored public key should match original.");
        assertEquals(originalPrivateKey, restoredPrivateKey, "Restored private key should match original.");
        logger.info("RSA Key to/from string conversion successful.");
    }

    @Test
    void testRsaEncryptionDecryption_withKeyObjects() throws NoSuchAlgorithmException {
        KeyPair keyPair = EncryptionUtil.generateRsaKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String originalData = "This is some data to be RSA encrypted, perhaps an AES key.";

        String encryptedData = EncryptionUtil.encryptRSA(originalData, publicKey);
        assertNotNull(encryptedData);
        assertNotEquals(originalData, encryptedData);
        logger.info("RSA Encrypted (Key Objects): {}", encryptedData.substring(0, Math.min(30, encryptedData.length()))+ "...");


        String decryptedData = EncryptionUtil.decryptRSA(encryptedData, privateKey);
        assertEquals(originalData, decryptedData);
        logger.info("RSA Decrypted (Key Objects): {}", decryptedData);
    }

    @Test
    void testRsaEncryptionDecryption_withStringKeys() throws NoSuchAlgorithmException {
        KeyPair keyPair = EncryptionUtil.generateRsaKeyPair();
        String publicKeyStr = EncryptionUtil.publicKeyToString(keyPair.getPublic());
        String privateKeyStr = EncryptionUtil.privateKeyToString(keyPair.getPrivate());

        String originalData = "Another piece of data for RSA, via string keys!";

        String encryptedData = EncryptionUtil.encryptRSA(originalData, publicKeyStr);
        assertNotNull(encryptedData);
        assertNotEquals(originalData, encryptedData);
        logger.info("RSA Encrypted (String Keys): {}", encryptedData.substring(0, Math.min(30, encryptedData.length()))+ "...");

        String decryptedData = EncryptionUtil.decryptRSA(encryptedData, privateKeyStr);
        assertEquals(originalData, decryptedData);
        logger.info("RSA Decrypted (String Keys): {}", decryptedData);
    }

    @Test
    void testRsaEncryptWithPublicKey_DecryptWithDifferentPrivateKey_Fails() throws NoSuchAlgorithmException {
        KeyPair keyPair1 = EncryptionUtil.generateRsaKeyPair();
        KeyPair keyPair2 = EncryptionUtil.generateRsaKeyPair(); // Different key pair

        PublicKey publicKey1 = keyPair1.getPublic();
        PrivateKey privateKey2 = keyPair2.getPrivate(); // Mismatched private key

        String originalData = "Sensitive data";
        String encryptedData = EncryptionUtil.encryptRSA(originalData, publicKey1);

        assertThrows(RuntimeException.class, () -> {
            EncryptionUtil.decryptRSA(encryptedData, privateKey2);
        }, "Decryption with a mismatched private key should fail.");
    }
}
