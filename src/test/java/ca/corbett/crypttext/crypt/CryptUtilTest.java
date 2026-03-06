package ca.corbett.crypttext.crypt;

import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CryptUtilTest {

    // ------ Encryption tests ---------------

    @Test
    public void encrypt_withValidDataAndPassword_shouldSucceed() throws Exception {
        // GIVEN valid input parameters:
        String password = "mysecretpassword";
        String plaintext = "This is a secret message.";

        // WHEN we encrypt the plaintext:
        byte[] encrypted = CryptUtil.encrypt(password, plaintext.getBytes());

        // THEN we should get non-null data back:
        assertNotNull(encrypted);
        assertNotEquals(0, encrypted.length);
    }

    @Test
    public void encrypt_withNullPassword_shouldThrowException() throws Exception {
        // GIVEN a null password:
        String password = null;
        String plaintext = "This is a secret message.";

        // WHEN we attempt to encrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.encrypt(password, plaintext.getBytes());
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    @Test
    public void encrypt_withEmptyPassword_shouldThrowException() throws Exception {
        // GIVEN an empty password:
        String password = "";
        String plaintext = "This is a secret message.";

        // WHEN we attempt to encrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.encrypt(password, plaintext.getBytes());
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    @Test
    public void encrypt_withNullPlaintext_shouldThrowException() throws Exception {
        // GIVEN a null plaintext:
        String password = "mysecretpassword";
        byte[] plaintext = null;

        // WHEN we attempt to encrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.encrypt(password, plaintext);
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    // ------ Decryption tests ---------------

    @Test
    public void decrypt_withValidInput_shouldSucceed() throws Exception {
        // GIVEN valid encrypted data:
        final String plainText = "A super secret message.";
        final String password = "password";
        final byte[] encrypted = CryptUtil.encrypt(password, plainText.getBytes());

        // WHEN we try to decrypt it:
        byte[] decrypted = CryptUtil.decrypt(password, encrypted);

        // THEN we should get back the original plaintext:
        assertEquals(plainText, new String(decrypted));
    }

    @Test
    public void decrypt_withWrongPassword_shouldFail() throws Exception {
        // GIVEN valid encrypted data:
        final String plainText = "A super secret message.";
        final String password = "password";
        final byte[] encrypted = CryptUtil.encrypt(password, plainText.getBytes());

        try {
            // WHEN we try to decrypt it with the wrong password:
            CryptUtil.decrypt("wrong!", encrypted);
            fail("Wrong password should have thrown, but didn't.");
        }
        catch (AEADBadTagException expected) {
            // THEN we should get an AEADBadTagException, which indicates authentication failure:
        }
    }

    @Test
    public void decrypt_withNullPassword_shouldThrowException() throws Exception {
        // GIVEN a null password:
        String password = null;
        byte[] encrypted = new byte[16]; // dummy data

        // WHEN we attempt to decrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.decrypt(password, encrypted);
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    @Test
    public void decrypt_withEmptyPassword_shouldThrowException() throws Exception {
        // GIVEN an empty password:
        String password = "";
        byte[] encrypted = new byte[16]; // dummy data

        // WHEN we attempt to decrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.decrypt(password, encrypted);
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    @Test
    public void decrypt_withInvalidEncryptedData_shouldThrowException() throws Exception {
        // GIVEN invalid encrypted data (too short):
        String password = "password";
        byte[] encrypted = new byte[10]; // too short to contain salt + iv + ciphertext

        // WHEN we attempt to decrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.decrypt(password, encrypted);
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    @Test
    public void decrypt_withNullEncryptedData_shouldThrowException() throws Exception {
        // GIVEN null encrypted data:
        String password = "password";
        byte[] encrypted = null;

        // WHEN we attempt to decrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.decrypt(password, encrypted);
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    // ------ Encoding/decoding tests ---------------

    @Test
    public void encryptAndEncode_withValidInput_shouldSucceed() throws Exception {
        // GIVEN valid input parameters:
        String password = "mysecretpassword";
        String plaintext = "This is a secret message.";

        // WHEN we encrypt and encode the plaintext:
        String encoded = CryptUtil.encryptAndEncode(password, plaintext.getBytes());

        // THEN we should get non-null data back in Mime Base64 format:
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty(), "Encoded string should not be empty");
        assertTrue(isValidBase64(encoded), "Encoded string should be valid Base64");
    }

    @Test
    public void decodeAndDecrypt_withValidInput_shouldSucceed() throws Exception {
        // GIVEN valid encoded data:
        final String plainText = "A super secret message.";
        final String password = "password";
        final String encoded = CryptUtil.encryptAndEncode(password, plainText.getBytes());

        // WHEN we try to decode and decrypt it:
        byte[] decrypted = CryptUtil.decodeAndDecrypt(password, encoded);

        // THEN we should get back the original plaintext:
        assertEquals(plainText, new String(decrypted));
    }

    @Test
    public void decodeAndDecrypt_withInvalidBase64_shouldThrowException() throws Exception {
        // GIVEN an invalid Base64 string:
        String password = "password";
        String invalidBase64 = "This is not valid Base64!";

        // WHEN we attempt to decode and decrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.decodeAndDecrypt(password, invalidBase64);
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    @Test
    public void decodeAndDecrypt_withNullEncodedString_shouldThrowException() throws Exception {
        // GIVEN a null encoded string:
        String password = "password";
        String encoded = null;

        // WHEN we attempt to decode and decrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.decodeAndDecrypt(password, encoded);
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    @Test
    public void decodeAndDecrypt_withEmptyEncodedString_shouldThrowException() throws Exception {
        // GIVEN an empty encoded string:
        String password = "password";
        String encoded = "";

        // WHEN we attempt to decode and decrypt, THEN we should get an IllegalArgumentException:
        try {
            CryptUtil.decodeAndDecrypt(password, encoded);
            fail("Expected IllegalArgumentException was not thrown");
        }
        catch (IllegalArgumentException ignored) {
            // Expected exception
        }
    }

    @Test
    public void decodeAndDecrypt_withExtraLineBreaks_shouldSucceed() throws Exception {
        // GIVEN valid encoded data:
        final String plainText = "A super secret message.";
        final String password = "password";
        final String encoded = CryptUtil.encryptAndEncode(password, plainText.getBytes());

        // And GIVEN a bunch of extra line breaks before and after the encoded data:
        final String modifiedEncoded = "\n\n\n" + encoded + "\n\n\n";

        // WHEN we try to decode and decrypt it:
        byte[] decrypted = CryptUtil.decodeAndDecrypt(password, modifiedEncoded);

        // THEN we should still get back the original plaintext - the line breaks should be ignored.
        assertEquals(plainText, new String(decrypted));
    }

    // ------ Wrapping/unwrapping tests ---------------

    @Test
    public void encryptAndWrap_givenValidInput_shouldSucceed() throws Exception {
        // GIVEN valid input parameters:
        String password = "mysecretpassword";
        String plaintext = "This is a secret message.";

        // WHEN we encrypt and wrap the plaintext:
        String wrapped = CryptUtil.encryptAndWrap(password, plaintext);

        // THEN we should get a non-null wrapped string that contains the expected header and valid Base64-encoded data:
        assertNotNull(wrapped);
        assertTrue(wrapped.contains(CryptUtil.CRYPT_HEADER), "Wrapped string should contain the expected header");
        String base64Part = wrapped.substring(wrapped.indexOf(CryptUtil.CRYPT_HEADER) + CryptUtil.CRYPT_HEADER.length())
                                   .trim();
        assertTrue(isValidBase64(base64Part), "Base64 part of wrapped string should be valid Base64");
    }

    @Test
    public void unwrapAndDecrypt_givenValidWrappedData_shouldSucceed() throws Exception {
        // GIVEN valid input parameters:
        String password = "mysecretpassword";
        String plaintext = "This is a secret message.";
        String wrapped = CryptUtil.encryptAndWrap(password, plaintext);

        // WHEN we unwrap and decrypt the wrapped data:
        String decrypted = CryptUtil.unwrapAndDecrypt(password, wrapped);

        // THEN we should get back the original plaintext:
        assertEquals(plaintext, decrypted);
    }


    /**
     * A quick utility method to check if a string is valid Base64-encoded data in Mime format.
     */
    private boolean isValidBase64(String str) {
        try {
            Base64.getMimeDecoder().decode(str);
            return true;
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }
}