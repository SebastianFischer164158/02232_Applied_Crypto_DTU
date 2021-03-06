/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatapplication_server.components.ClientSocketEngine;

import SocketActionMessages.ChatMessage;
import chatapplication_server.components.ConfigManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

// Own written cryptoManager
import crypto.cryptoManager;

import static crypto.cryptoManager.*;

/**
 *
 * @author atgianne
 */
public class P2PClient extends JFrame implements ActionListener 
{
    private String host;
    private String port;
    private final JTextField tfServer;
    private final JTextField tfPort;
    private final JTextField tfsPort;
    private final JLabel label;
    private final JTextField tf;
    private final JTextArea ta;
    protected boolean keepGoing;
    JButton Send, stopStart;
    JButton connectStop;
    
    /** Client Socket and output stream... */
    Socket socket = null;
    ObjectOutputStream sOutput;
    
    private ListenFromClient clientServer;
    
    /** Flag indicating whether the Socket Server is running at one of the Clients... */
    boolean isRunning;
    
    /** Flag indicating whether another client is connected to the Socket Server... */
    boolean isConnected;

    /** Define 2048 bits p & g as defined in Java 8 docs (ref.) */
    private final BigInteger p = new BigInteger("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80" +
            "b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b" +
            "1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff2" +
            "2203199dd14801c7", 16);

    private final BigInteger g = new BigInteger("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3" +
            "d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2b" +
            "b3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfec" +
            "f492a", 16);

    /** Setup Diffie Hellman Properties flags*/
    private Boolean diffieExchange = false;
    private Boolean secretSend = false;
    private volatile Boolean peerSecretReceived = false;
    private BigInteger diffieSecret;
    private SecretKeySpec sharedSecret;
    private BigInteger receivedPeerKey;

    /** Certificate Setup */
    private Boolean iAmAlice = false;
    public static String AliceKeyStorePass = "password";
    public static String Alicealias = "alice";

    private volatile Boolean receivedPeerCertificate = false;
    java.security.cert.Certificate peerCertificate;
    private static PublicKey PeerPublicKey; //gets set by a client.
    private static PrivateKey MyPrivKey;
    private Boolean certSent = false;
    BigInteger calcedSenderValue = null;



    P2PClient(){
        super("P2P Client Chat");
        host=ConfigManager.getInstance().getValue( "Server.Address" );
        port=ConfigManager.getInstance().getValue( "Server.PortNumber" );
        
        // The NorthPanel with:
        JPanel northPanel = new JPanel(new GridLayout(3,1));
        // the server name anmd the port number
        JPanel serverAndPort = new JPanel(new GridLayout(1,5, 1, 3));
        // the two JTextField with default value for server address and port number
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);
        
        tfsPort=new JTextField(5);
        tfsPort.setHorizontalAlignment(SwingConstants.RIGHT);
        stopStart=new JButton("Start");
        stopStart.addActionListener(this);

        serverAndPort.add(new JLabel("Receiver's Port No:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel("Receiver's IP Add:  "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel(""));
        // adds the Server an port field to the GUI
        northPanel.add(serverAndPort);

        // the Label and the TextField
        label = new JLabel("Enter message below", SwingConstants.LEFT);
        northPanel.add(label);
        tf = new JTextField();
        tf.setBackground(Color.WHITE);
        northPanel.add(tf);
        add(northPanel, BorderLayout.NORTH);
        
        // The CenterPanel which is the chat room
        ta = new JTextArea(" ", 80, 80);
        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);

//        ta2 = new JTextArea(80,80);
//        ta2.setEditable(false);
//        centerPanel.add(new JScrollPane(ta2));   
        add(centerPanel, BorderLayout.CENTER);
        
        connectStop = new JButton( "Connect" );
        connectStop.addActionListener(this);
        
        Send = new JButton("Send");
        Send.addActionListener(this);
        Send.setVisible( false );
        JPanel southPanel = new JPanel();
        southPanel.add( connectStop );
        southPanel.add(Send);
        southPanel.add(stopStart);
        JLabel lbl=new JLabel("Sender's Port No:");
        southPanel.add(lbl);
        tfsPort.setText("0");
        southPanel.add(tfsPort);
        add(southPanel, BorderLayout.SOUTH);
        
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

//        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        tf.requestFocus();
        
        isRunning = false;
        isConnected = false;
    }

    /** Function to generate a random big integer from a defined bitSize*/
    private BigInteger GenerateBigInteger(int bitSize)
    {
        return new BigInteger(bitSize, new SecureRandom());
    }

    /** Function to perform Sha256 and output a secret key spec AES*/
    private SecretKeySpec PerformSha256(BigInteger agreedScret) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] encodedhash = digest.digest(agreedScret.toString().getBytes(StandardCharsets.UTF_8));

        return new SecretKeySpec(encodedhash, "AES");
    }
    public void CertificateExchange() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        // Certificate
        java.security.cert.Certificate MyCert;

        /** Either a peer will be Bob or Alice */
        String keyStore = BobKeyStore;
        String keyStorePass = BobKeyStorePass;
        String alias = Bobalias;

        if (iAmAlice)
        {
            keyStore = AliceKeyStore;
            keyStorePass = AliceKeyStorePass;
            alias = Alicealias;
        }
        /** Extracting own private key from the keyStore*/
        MyPrivKey = cryptoManager.ExtractPrivKeyFromJKS(keyStore, keyStorePass, alias, keyStorePass);

        /** Sends peers own certificate to the counter peer */
        if (!certSent){
            MyCert = ExtractCertFromJKS(keyStore, keyStorePass, alias);
            cryptoManager.SendCert(MyCert, sOutput);
            certSent = true;
        }

        // Wait for PeerCertificate
        while(!receivedPeerCertificate)
        {
        }

        /** Verify that the Certificate was signed by the trusted CA!*/
        cryptoManager.VerifyCert(peerCertificate, RootCAPubKey);
        PeerPublicKey = cryptoManager.ExtractPubKeyFromCert(peerCertificate);
    }

    public void DiffieHellmanExchange() throws Exception
    {
        // We should always send our secret.
        if (!secretSend)
        {
            /** Prepare Diffiehellman exchange */
            diffieSecret = GenerateBigInteger(2048);
            calcedSenderValue = g.modPow(diffieSecret, p);
            byte[] calcedSenderValue_bytes = calcedSenderValue.toByteArray();
            /** Declare arrays for the signature, and the DH parameter*/
            byte[] signatureBytes = cryptoManager.SignMsg(calcedSenderValue_bytes, MyPrivKey); //always 512
            byte[] signature_and_calcedSenderValue = new byte[signatureBytes.length + calcedSenderValue_bytes.length];
            //byte[] calcedSenderValue_encrypted = cryptoManager.encrypt_RSA(MyPrivKey, calcedSenderValue.toByteArray());
            /** Combine the digital signature and the DH Parmeter */
            System.arraycopy(signatureBytes, 0, signature_and_calcedSenderValue, 0, signatureBytes.length);
            System.arraycopy(calcedSenderValue_bytes, 0, signature_and_calcedSenderValue, signatureBytes.length, calcedSenderValue_bytes.length);
            /** send signed calculated values */
            sOutput.writeObject(signature_and_calcedSenderValue);
            secretSend = true;
        }

        // Get stuck here until the thread of receiving messages, receives the public key from counter peer.
        // While loop is controlled by a volatile variable meaning that it is visible to all threads.
        while(!peerSecretReceived)
        {
        }

        BigInteger agreedSecret = receivedPeerKey.modPow(diffieSecret, p);
        sharedSecret = PerformSha256(agreedSecret);

        /** State that diffie hellman exchange is done */
        diffieExchange = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        
        if ( o == connectStop )
        {
            if ( connectStop.getText().equals( "Connect" ) && !isConnected)
            {
                if ( tfPort.getText().equals( ConfigManager.getInstance().getValue( "Server.PortNumber" ) ) )
                {
                    display( "Cannot give the same port number as the Chat Application Server - Please give the port number of the peer client to communicate!\n" );
                    return;
                }
                
                /** Connect to the Socket Server instantiated by the other client... */
                this.connect();
            }
            else if ( connectStop.getText().equals( "Disconnect" ) && isConnected)
            {
                this.disconnect();
            }
        }
        else if ( o == Send )
        {
            String msg = tf.getText();
            /** Try to send the message to the other communicating party, if we have been connected... */
            if (isConnected)
            {
                //Whenever send is pressed and diffie Hellman exchange isn't performed, it should be.
                if (!diffieExchange){
                    try {
                        /** Starting both Certificate and Diffie Hellman exchange, when 'send' is pressed the first time.*/
                        /** The first to press 'send' will be intialized as Alice.*/
                        iAmAlice = true;
                        CertificateExchange();
                        DiffieHellmanExchange();

                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                // From the point where Diffiehellman exchange is done, the message should just be encrypted and send.
                try {
                    this.send( cryptoManager.encrypt(msg, sharedSecret));
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
        else if(o == stopStart)
        {
            if ( stopStart.getText().equals( "Start" ) && !isRunning)
            {
                clientServer = new ListenFromClient();
                clientServer.start();
                isRunning = true;
                stopStart.setText( "Stop" );
            }
            else if ( stopStart.getText().equals( "Stop" ) && isRunning)
            {
                clientServer.shutDown();
                clientServer.stop();
                isRunning = false;
                stopStart.setText( "Start" );
            }
        }
    }
    
    public void display(String str) {
        ta.append(str + "\n");
        ta.setCaretPosition(ta.getText().length() - 1);
    }
    
    /**
     * Method that is invoked when a client wants to connect to the Socket Server spawn from another client in order to initiate their P2P communication.
     * 
     * @return TRUE if the connection was successful; FALSE otherwise 
     */
    public boolean connect()
    {
        /* Try to connect to the Socket Server... */
        try {
                if (!isConnected)
                {
                    socket = new Socket(tfServer.getText(), Integer.parseInt(tfPort.getText()));
                   
                    sOutput = new ObjectOutputStream(socket.getOutputStream());
                    isConnected = true;
                    Send.setVisible( true );
                    connectStop.setText( "Disconnect" );
                    
                    return true;
                }
            } 
            catch (IOException eIO) {
                    display("The Socket Server from the other side has not been fired up!!\nException creating new Input/output Streams: " + eIO.getMessage() + "\n");
                    isConnected = false;
                    Send.setVisible( false );
                    connectStop.setText( "Connect" );
                    return false;
            }
            // if it failed not much I can so
            catch(Exception ec) {
                    display("Error connecting to server:" + ec.getMessage() + "\n");
                    isConnected = false;
                    Send.setVisible( false );
                    connectStop.setText( "Connect" );
                    return false;
            }
        
        return true;
    }
    
    /**
     * Method that is invoked when we want do disconnect from a Socket Server (spawn by another client); this, basically, reflects the stopping of a P2P communication
     * 
     * @return TRUE if the disconnect was successful; FALSE, otherwise 
     */
    public boolean disconnect()
    {
        /** Disconnect from the Socket Server that we are connected... */
        try
        {
            if (isConnected)
            {
                /** First, close the output stream... */
                sOutput.close();
                
                /** Then, close the socket... */
                socket.close();
                
                /** Re-initialize the parameters... */
                isConnected = false;
                Send.setVisible( false );
                connectStop.setText( "Connect" );
                
                return true;
            }
        }
        catch( IOException ioe )
        {
            display( "Error closing the socket and output stream: " + ioe.getMessage() + "\n" );
            
            /** Re-initialize the parameters... */
            isConnected = false;
            Send.setVisible( false );
            connectStop.setText( "Connect" );
            return false;
        }
        
        return true;
    }
    
    public boolean send(String str) throws Exception {
        try {

            sOutput.writeObject(new ChatMessage(str.length(), str));

            if (diffieExchange)
                display("You: " + cryptoManager.decrypt(str, sharedSecret));
            else
                System.out.println("You: " + str);


        } catch (IOException ex) {
            display("The Client's Server Socket was closed!!\nException creating output stream: " + ex.getMessage());
            this.disconnect();
            return false;
        }

         return true;
    }

    private class ListenFromClient extends Thread
    {
        ServerSocket serverSocket;
        Socket socket;
        ObjectInputStream sInput = null;
        boolean clientConnect = false;
        
            public ListenFromClient() 
            {
                try
                {
                    // the socket used by the server
                    serverSocket = new ServerSocket(Integer.parseInt(tfsPort.getText()));
                    ta.append("Server is listening on port:"+tfsPort.getText() + "\n");
                    ta.setCaretPosition(ta.getText().length() - 1);
                    keepGoing = true;
                }
                catch ( IOException ioe )
                {
                    System.out.println("[P2PClient]:: Error firing up Socket Server " + ioe.getMessage());
                }
            }

            @Override
            public void run() 
            {
                // infinite loop to wait for messages
                while(keepGoing) 
                {
                    /** Wait only when there are no connections... */
                    try
                    {
                        if ( !clientConnect )
                        {
                            socket = serverSocket.accept();  	// accept connection
                            sInput = new ObjectInputStream(socket.getInputStream());
                            clientConnect = true;
                        }
                    }
                    catch (IOException ex)
                    {
                            display("The Socket Server was closed: " + ex.getMessage());
                    }
                    
                    // format message saying we are waiting
                    try {
                        /** First message that will be received is the certificate */
                        if (!receivedPeerCertificate)
                        {
                            peerCertificate = cryptoManager.ReceiveCert(sInput);
                            receivedPeerCertificate = true;
                            CertificateExchange();
                        }
                        else {
                            /** Second message to be received is the counter peer diffiehellman property. */
                            if (!peerSecretReceived){
                                byte[] msg = (byte[]) sInput.readObject();
                                /** Declare arrays for the signature and the DH Param*/
                                byte[] signature = new byte[512];
                                byte[] DHParam = Arrays.copyOfRange(msg, 512, msg.length);
                                /** Extract the digital signature and DH paramtere*/
                                System.arraycopy(msg, 0, signature, 0, signature.length);
                                /** Verify the signature */
                                if(cryptoManager.VerifySign(DHParam, signature, PeerPublicKey)) {
                                    System.out.println("Signature matches, extracting the DH Param");
                                    byte[] decryptedMessage = DHParam;//cryptoManager.decrypt_RSA(PeerPublicKey, msg);
                                    receivedPeerKey = new BigInteger(decryptedMessage);
                                    peerSecretReceived = true;
                                    DiffieHellmanExchange();
                                }
                            }
                            else {
                                String msg = ((ChatMessage) sInput.readObject()).getMessage();
                                System.out.println(socket.getInetAddress()+": " + socket.getPort() + ": " + msg);
                                display(socket.getInetAddress()+": " + socket.getPort() + ": " + cryptoManager.decrypt(msg, sharedSecret));
                            }
                        }
                    }
                    catch (IOException ex) 
                    {
                            display("Could not ready correctly the messages from the connected client: " + ex.getMessage());
                            clientConnect = false;
                    }  
                    catch (Exception ex) {
                        Logger.getLogger(P2PClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
	}
            
        public void shutDown()
        {
            try
            {
                keepGoing = false;
                if ( socket != null )
                {
                    sInput.close();
                    socket.close();
                }
                
                if (serverSocket != null)
                {
                    serverSocket.close();
                }
            }
            catch ( IOException ioe )
            {
                 System.out.println("[P2PClient]:: Error closing Socket Server " + ioe.getMessage());
            }
        }
    }
}
