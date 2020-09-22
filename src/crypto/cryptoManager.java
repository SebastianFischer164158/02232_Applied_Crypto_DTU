package crypto;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Cryptomanager class responsible for all AES-GCM operations
 * Defines 4 hardcoded keys for 4 hardcoded client usernames
 *
 */
public class cryptoManager {
    /** Use for GCM... */
    public static final int GCM_TAG_LENGTH = 16;
    private static final int GCM_IV_LENGTH = 12;

    /** Hardcoded keys as allowed in the assignment... (256 bits) */
    public static byte[] key_S = new byte[]{'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0'};
    public final static SecretKeySpec keySebastian = new SecretKeySpec(key_S, "AES");

    public static byte[] key_M = new byte[]{'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1'};
    public final static SecretKeySpec keyMagnus = new SecretKeySpec(key_M, "AES");

    public static byte[] key_MB = new byte[]{'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '2'};
    public final static SecretKeySpec keyMathias = new SecretKeySpec(key_MB, "AES");

    public static byte[] key_F = new byte[]{'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '3'};
    public final static SecretKeySpec keyFrederik = new SecretKeySpec(key_F, "AES");

    /** Encrypt function takes a String plaintext and a SecretKey masterkey.. */
    public static String encrypt(String plaintext, SecretKey masterkey) throws Exception {
        /** Generate a random 12 byte IV! MUST BE UNIQUE AND NEVER RE-USED... */
        byte[] IV = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);
        /** Setup GCM AES with the IV and secret key ... */
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);
        cipher.init(Cipher.ENCRYPT_MODE, masterkey, ivSpec);
        /** Encrypt the plaintext and define a new byte array for the IV + ciphertext*/
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(UTF_8));
        byte[] IV_and_ciphertext = new byte[IV.length + ciphertext.length];
        /** Put in the arrays */
        System.arraycopy(IV, 0, IV_and_ciphertext, 0, IV.length);
        System.arraycopy(ciphertext, 0, IV_and_ciphertext, IV.length, ciphertext.length);
        /** Return the IV + Ciphertext base64 encoded string... */
        return Base64.getEncoder().encodeToString(IV_and_ciphertext);
    }
    /** Decrypt function takes a String plaintext and a SecretKey masterkey.. */
    public static String decrypt(String ciphertext, SecretKey masterkey) throws Exception {
        /** Decode from b64 */
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] IV_extracted = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
        /** Setup AES GCM decrypt mode*/
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV_extracted);
        cipher.init(Cipher.DECRYPT_MODE, masterkey, ivSpec);

        /** Extract the cipher text, as the ciphertext contains IV + encryptedtxt*/
        byte[] ciphertext_extracted = Arrays.copyOfRange(decoded, IV_extracted.length, decoded.length);
        byte [] decrypted = cipher.doFinal(ciphertext_extracted);
        /** Return the now decrypted plaintext */
        return new String(decrypted, UTF_8);
    }
}
