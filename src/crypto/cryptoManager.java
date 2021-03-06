package crypto;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Cryptomanager class responsible for all AES-GCM operations, Publickey crypto, certificates, etc.
 *
 */
public class cryptoManager {
    /** Use for GCM... */
    public static final int GCM_TAG_LENGTH = 16;
    private static final int GCM_IV_LENGTH = 12;

    // if executing jar file from /out then move ServerKeyStore.jks to /out folder, and remove absolute path to rel.
    public static String ServerKeyStore = "D:\\Projects\\02232_Applied_Crypto_DTU\\ServerKeyStore.jks";
    public static String ServerKeyStorePass = "password";
    public static String Serveralias = "server";

    public static String AliceKeyStore = "D:\\Projects\\02232_Applied_Crypto_DTU\\AliceKeyStore.jks";
    public static String AliceKeyStorePass = "password";
    public static String Alicealias = "alice";

    public static String BobKeyStore = "D:\\Projects\\02232_Applied_Crypto_DTU\\BobKeyStore.jks";
    public static String BobKeyStorePass = "password";
    public static String Bobalias = "bob";

    public static PublicKey ServerPubKey_ServSide; //gets set by server side
    public static PublicKey ServerPubKey_ClientSide; //gets set by a client.
    public static PublicKey ClientPubKey; //gets set by a client.
    public final static String RootCACert_path = "D:\\Projects\\02232_Applied_Crypto_DTU\\rootca.cer";
    public static Certificate RootCACert = null;

    static {
        try {
            /** Extract the RootCA Certificate from the path defined above */
            RootCACert = ExtractCerFromPath(RootCACert_path);
        } catch (FileNotFoundException | CertificateException e) {
            e.printStackTrace();
        }
    }
    /** Extract the Public Key from the Certificate of the RootCA*/
    public final static PublicKey RootCAPubKey = ExtractPubKeyFromCert(RootCACert);
    /**HashMap used by the Server to store the connecting Client's PublicKey*/
    public static HashMap<String,PublicKey> Clients_PublicKeys_ServerSide = new HashMap<>();
    /**HashMap used by the Server to store the connecting Client's random generated AES key*/
    public static HashMap<String,SecretKey> Clients_SecretKeys_ServerSide = new HashMap<>();
    /** Variables used by the Client to assign their received AES key by the Server*/
    public static byte [] AES_s_client_key;
    public static SecretKeySpec AES_secret_client_key = null;

    /** Encrypt function takes a String plaintext and a SecretKey masterkey.. to perform AES256gcm*/
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

    public static PublicKey ExtractPubKeyFromCert(Certificate Certificate) {
        /** Method to extract the public key from a certificate*/
        X509Certificate c = (X509Certificate) Certificate;
        return c.getPublicKey();
    }
    public static PrivateKey ExtractPrivKeyFromJKS(String keyStore, String KeyStorePass, String alias, String keyPass)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        /**Method to extract the private key from a JKS*/
        java.security.KeyStore ks = java.security.KeyStore.getInstance( "JKS" );
        java.io.FileInputStream ksfis = new java.io.FileInputStream( keyStore );
        java.io.BufferedInputStream ksbufin = new java.io.BufferedInputStream( ksfis );
        ks.load( ksbufin, KeyStorePass.toCharArray() );
        return (PrivateKey) ks.getKey( alias, keyPass.toCharArray() );

    }

    public static Certificate ExtractCertFromJKS(String keyStore, String KeyStorePass, String alias)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        /**Method to extract certificate from a JKS*/
        java.security.KeyStore ks = java.security.KeyStore.getInstance( "JKS" );
        java.io.FileInputStream ksfis = new java.io.FileInputStream( keyStore );
        java.io.BufferedInputStream ksbufin = new java.io.BufferedInputStream( ksfis );
        ks.load( ksbufin, KeyStorePass.toCharArray() );
        return ks.getCertificate(alias);
    }

    public static Certificate ExtractCerFromPath(String path) throws FileNotFoundException, CertificateException {
        /**Method to extract certificate from a path, e.g. C:/SomeDir/AnotherDir/xx.cer*/
        FileInputStream fr = new FileInputStream(path);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        return cf.generateCertificate(fr);
    }

    public static void SendCert(java.security.cert.Certificate Cert, ObjectOutputStream socketWriter){
        /**Method to transmit a certificate with a ObjectOutputStream*/
        try {
            socketWriter.writeObject(Cert);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static java.security.cert.Certificate ReceiveCert(ObjectInputStream socketReader) {
        /**Method to Receive a certificate from a ObjectInputStream*/
        java.security.cert.Certificate Cert = null;
        try {
            Cert = (java.security.cert.Certificate) socketReader.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Cert;
    }

    public static byte[] encrypt_RSA(Key key, byte[] plaintext) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        /**Method to use RSA encryption with a public key*/
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plaintext);
    }

    public static byte[] decrypt_RSA(Key key, byte[] ciphertext) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        /**Method to use RSA decryption with a public key*/
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(ciphertext);
    }

    public static byte[] SignMsg(byte[] plaintext, PrivateKey privkey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        /**Method to create a digital signature of a message*/
        Signature signer = Signature.getInstance("SHA256WithRSA");
        signer.initSign(privkey);
        signer.update(plaintext);
        return signer.sign();
    }

    public static boolean VerifySign(byte[] plaintext, byte[] signature, PublicKey pubkey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        /**Method to verify a digital signature */
        Signature signer = Signature.getInstance("SHA256WithRSA");
        signer.initVerify(pubkey);
        signer.update(plaintext);
        return signer.verify(signature);
    }

    public static void VerifyCert(java.security.cert.Certificate cert, PublicKey pubkey) {
        /**Method verify a certificate was signed with the private key corresponding to the public key given*/
        try{
            cert.verify(pubkey);
            System.out.println("Certificate Verified!");
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
            System.out.println("Certificate NOT Verified!");
            e.printStackTrace();
        }




    }

}
