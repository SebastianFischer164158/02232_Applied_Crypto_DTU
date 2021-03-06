/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatapplication_server.components.ClientSocketEngine;

import SocketActionMessages.ChatMessage;
import chatapplication_server.ComponentManager;
import chatapplication_server.components.ConfigManager;
import chatapplication_server.components.base.GenericThreadedComponent;
import chatapplication_server.statistics.ServerStatistics;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;

import crypto.cryptoManager;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import static crypto.cryptoManager.*;

/**
 *
 * @author atgianne
 */
public class ClientEngine extends GenericThreadedComponent 
{

    /** Instance of the ConfigManager component */
    ConfigManager configManager;
    
    /** Object for printing the secure socket server configuration properties */
    ServerStatistics lotusStat;
    
    /** Flag indicating whether the Socket Server is running.... */
    boolean isRunning;
    
    /** The Socket connection to the Chat Application Server */
    private Socket socket;
    
    /** Socket Stream reader/writer that will be used throughout the whole connection... */
    private ObjectOutputStream socketWriter;
    private ObjectInputStream socketReader;
    
    /**
     * Singleton instance of the SocketServerEngine component
     */
    private static ClientEngine componentInstance = null;
    
    /**
     * Creates a new instance of SocketServerEngine
     */
    public ClientEngine() {
        isRunning = false;
    }
    
    /**
     * Make sure that we can only get one instance of the SocketServerEngine component.
     * Implementation of the static getInstance() method.
     */
    public static ClientEngine getInstance()
    {
        if ( componentInstance == null )
            componentInstance = new ClientEngine();
        
        return componentInstance;
    }
    
     /**
     * Implementation of IComponent.initialize method().
     * This method is called upon initialize of the ClientEngine component and handles any configuration that needs to be
     * done in the client before it connects to the Chat Application Server.
     * 
     * @see //IComponent interface.
     */
    public void initialize() throws Exception {
        /** Get the running instance of the Configuration Manager component */
        configManager = ConfigManager.getInstance();
                
        /** For printing the configuration properties of the secure socket server */
        lotusStat = new ServerStatistics();
        String UserName = configManager.getValue( "Client.Username" );
        
        /** Try and connect to the server... */
        try
        {
            socket = new Socket( configManager.getValue( "Server.Address" ), configManager.getValueInt( "Server.PortNumber" ) );
        }
        catch ( Exception e )
        {
            display( "Error connecting to the server:" + e.getMessage() + "\n" );
            ClientSocketGUI.getInstance().loginFailed();
            return;
        }
        
        /** Print that the connection was accepted */
        display( "Connection accepted: " + socket.getInetAddress() + ":" + socket.getPort() + "\n" );
        
        /** Create the read/write object streams... */
        try
        {
            /** Set up the stream reader/writer for this socket connection... */
            socketWriter = new ObjectOutputStream( socket.getOutputStream() );
            socketReader = new ObjectInputStream( socket.getInputStream() );

            /** First the client receives the certificate from the server*/

            java.security.cert.Certificate ServerCert = cryptoManager.ReceiveCert(socketReader);
            System.out.println("<<<<<<<<<<<<<<<<Server Cert Received>>>>>>>>>>>>>>>>>>");
            System.out.println(ServerCert);
            System.out.println("<<<<<<<<<<<<<<<<END Server Cert Received END>>>>>>>>>>>>>>>>>>");
            /** Verify that the certificate was signed by the trusted CA!*/
            cryptoManager.VerifyCert(ServerCert, RootCAPubKey);
            cryptoManager.ServerPubKey_ClientSide = cryptoManager.ExtractPubKeyFromCert(ServerCert);
            System.out.println("Extracted Server PublicKey : \n" + ServerPubKey_ClientSide);


            /** we then extract the client's respective certificate from the JKS and send it off to the server*/

            java.security.cert.Certificate ClientCert = null;
            String ClientKeyStore = null;
            String ClientKeyStorePass = null;
            String Clientalias = null;
            try {
                if(UserName.equals("alice")) {
                    /**Extract the certificate from the keystore*/
                    ClientCert = ExtractCertFromJKS(cryptoManager.AliceKeyStore, cryptoManager.AliceKeyStorePass,
                            cryptoManager.Alicealias);
                    /** Extract the public key from the certificate*/
                    ClientPubKey = ExtractPubKeyFromCert(ClientCert); //this should maybe be done earlier
                    /**Set the relevant variables*/
                    ClientKeyStore = AliceKeyStore;
                    ClientKeyStorePass = AliceKeyStorePass;
                    Clientalias = Alicealias;
                }
                if(UserName.equals("bob")) {
                    /** Just like above, but just with bob.*/
                    ClientCert = ExtractCertFromJKS(cryptoManager.BobKeyStore, cryptoManager.BobKeyStorePass,
                            cryptoManager.Bobalias);
                    ClientPubKey = ExtractPubKeyFromCert(ClientCert); //this should maybe be done earlier
                    ClientKeyStore = BobKeyStore;
                    ClientKeyStorePass = BobKeyStorePass;
                    Clientalias = Bobalias;
                }
                cryptoManager.SendCert(ClientCert, socketWriter);
                System.out.println("Sent " + UserName + " Cert TO SERVER");
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            /** Receive the Signature + encrypted symmetric key */
            byte[] signature_and_Encrypted_AES_key_from_server = (byte[]) socketReader.readObject();
            /** Extract Private Key of the Client from JKS */
            PrivateKey ClientPrivateKey = ExtractPrivKeyFromJKS(ClientKeyStore, ClientKeyStorePass, Clientalias, ClientKeyStorePass);
            /** Decrypt the received symmetric key with public crypto RSA*/

            byte[] signature = new byte[512];
            byte[] encrypted_aes_key = new byte[512];
            /** Extract the digital signature and the encrypted AES key*/
            System.arraycopy(signature_and_Encrypted_AES_key_from_server, 0, signature, 0, signature.length);
            System.arraycopy(signature_and_Encrypted_AES_key_from_server, 512, encrypted_aes_key, 0, encrypted_aes_key.length);
            /** Decrypt the AES key */
            AES_s_client_key = cryptoManager.decrypt_RSA(ClientPrivateKey, encrypted_aes_key);
            /**Verify the digital signature for authenticity and integrity */
            if(cryptoManager.VerifySign(AES_s_client_key, signature, ServerPubKey_ClientSide)){
                System.out.println("Signature matches! - Extracting Secret AES Key");
                /** set the SecretKeySpec for AES, based on the now decrypted AES key*/
                AES_secret_client_key = new SecretKeySpec(AES_s_client_key, "AES");
                /** Start the ListeFromServer thread... */
                new ListenFromServer().start();

            }
            else{
                throw new Exception("ERROR - VERIFICATION OF SIGNATURE FAILED!");
            }

        }
        catch (IOException | ClassNotFoundException ioe )
        {
            display( "Exception creating new Input/Output Streams: " + ioe + "\n");
            ComponentManager.getInstance().fatalException(ioe);
        }

        /** Send our username to the server... */
        try
        {
            /**Encrypt the username with the symmetric key in order to stop impersonation attacks*/
            String UserNameEncrypted = encrypt(UserName, AES_secret_client_key);
            System.out.println("Sending encrypted username to server"+ UserNameEncrypted);
            socketWriter.writeObject(UserNameEncrypted);
        }
        catch ( IOException ioe )
        {
            display( "Exception during login: " + ioe );
            shutdown();
            ComponentManager.getInstance().fatalException(ioe);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            e.printStackTrace();
        }

        super.initialize();
    }
    
    /**
     * Method for displaying a message in the Client GUI
     * 
     * @msg The string message to be displayed
     */
    private void display( String msg )
    {
        ClientSocketGUI.getInstance().append( msg );
    }
    
    /**
     * Method for sending a message to the server
     * 
     * @param msg The message to be sent
     */
    public void sendMessage( ChatMessage msg ){

        try
        {
            /** Encrypt the message to be sent to the server*/

            String ciphertext = cryptoManager.encrypt(msg.getMessage(), AES_secret_client_key);
            ChatMessage result = new ChatMessage(msg.getType(), ciphertext);
            System.out.println("ENCRYPTED RESULT: " + result.getMessage());
            socketWriter.writeObject(result);

        }
        catch( IOException e )
        {
            System.out.println("Aloha");
            display( "Exception writing to server: " + e );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Method holding the main logic of the Client Engine. It basically waits for inputs from the user to be sent to the Server.
     */
    public void componentMain()
    {
        while ( !mustShutdown )
        {
            /** Wait messages from the user... */
            try
            {
                Thread.sleep( 7000 );
            }
            catch ( InterruptedException ie )
            {
                
            }
            
            // read message from user
            //String msg = scan.nextLine();
            String msg = ClientSocketGUI.getInstance().getPublicMsgToBeSent();
            if ( msg.equals( "" ) )
                continue;
                
            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("LOGOUT")) {
                    sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                    // break to do the disconnect
                    break;
            }
            // message WhoIsIn
            else if(msg.equalsIgnoreCase("WHOISIN")) {
                    sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));				
            }
            else if (msg.equalsIgnoreCase("PRIVATEMESSAGE")){				// default to ordinary message
                    sendMessage(new ChatMessage(ChatMessage.PRIVATEMESSAGE, msg));
            }
            else  {				// default to ordinary message
                    sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        
        shutdown();
    }
    
    public ObjectInputStream getStreamReader()
    {
        return socketReader;
    }
    
     /**
     * Override GenericThreadedComponent.shutdown() method.
     * Signal and wait until the ClientEngine thread, holding the secure socket connection, stops.
     * 
     * 
     * @see GenericThreadedComponent
     */
    public void shutdown() 
    {
         /** Close the secure socket server */
        try
        {
            synchronized ( socket)
            {                
                /** Shut down the Client Socket */
                socketReader.close();
                socketWriter.close();
                socket.close();
                
                isRunning = false;
                
                
                /** Print in the Event area of the Server Windows GUI the close operation of the Socket Server... */
                ClientSocketGUI.getInstance().append( "[CCEngine]:: Shutting down the Client Engine....COMPLETE (" + lotusStat.getCurrentDate() + ")\n" );
            }
        }
        catch ( Exception e )
        {
            /** Print to the logging stream that shutting down the Central System socket server failed */
            ClientSocketGUI.getInstance().append("[CCEngine]: Failed shutting down CS socket server -- " + e.getMessage() + " (" + lotusStat.getCurrentDate() + ")\n");
        }
        
        /** Invoke our parent's method to stop the thread running the secure socket server... */
        super.shutdown();
    }

}
