package crypto;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

public class cryptoManager {
    public static final int GCM_TAG_LENGTH = 16;
    private static final int GCM_IV_LENGTH = 12;
    public static byte[] key_o = new byte[]{'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0'};
    public final static SecretKeySpec key = new SecretKeySpec(key_o, "AES");
    //aes 256 key (32 bytes = 256 bits


    public static String encrypt(String plaintext, SecretKey masterkey) throws Exception {
        byte[] IV = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);
        cipher.init(Cipher.ENCRYPT_MODE, masterkey, ivSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(UTF_8));
        byte[] IV_and_ciphertext = new byte[IV.length + ciphertext.length];

        System.arraycopy(IV, 0, IV_and_ciphertext, 0, IV.length);
        System.arraycopy(ciphertext, 0, IV_and_ciphertext, IV.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(IV_and_ciphertext);
    }

    public static String decrypt(String ciphertext, SecretKey masterkey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(ciphertext);

        byte[] IV_extracted = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV_extracted);
        cipher.init(Cipher.DECRYPT_MODE, masterkey, ivSpec);


        byte[] ciphertext_extracted = Arrays.copyOfRange(decoded, IV_extracted.length, decoded.length);
        //doFinal(byte[] input, int inputOffset, int inputLen)
        //Encrypts or decrypts data in a single-part operation, or finishes a multiple-part operation.
        //byte[] decrypted = cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH);

        byte [] decrypted = cipher.doFinal(ciphertext_extracted);

        return new String(decrypted, UTF_8);
    }
}


//    public static String encrypt_dos(String plaintext, SecretKey key) throws GeneralSecurityException {
//        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//
//        byte[] IV = new byte[12];
//        SecureRandom random = new SecureRandom();
//        random.nextBytes(IV);
//
//
//        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);
//        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
//
//        byte[] plaintextbyte = plaintext.getBytes();
//        byte[] ivCTAndTag = new byte[12 + cipher.getOutputSize(plaintextbyte.length)];
//        System.arraycopy(IV, 0, ivCTAndTag, 0, 12);
//
//        cipher.doFinal(plaintextbyte, 0, plaintextbyte.length, ivCTAndTag, 12);
//
//        return Base64.getEncoder().encodeToString(ivCTAndTag);
//    }
//
//    public static String decrypt_dos(String ciphertext, SecretKey key) throws GeneralSecurityException {
//        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//
//        byte[] ivAndCTWithTag = Base64.getDecoder().decode(ciphertext);
//
//        GCMParameterSpec spec = new GCMParameterSpec(128, ivAndCTWithTag, 0, 12);
//        cipher.init(Cipher.DECRYPT_MODE, key, spec);
//
//        byte[] plaintext = cipher.doFinal(ivAndCTWithTag, 12, ivAndCTWithTag.length - 12);
//
//        return new String(plaintext, UTF_8);
//    }
//
//}


