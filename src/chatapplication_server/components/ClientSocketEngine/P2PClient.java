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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import crypto.cryptoManager;

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
    JButton send, start;

    // Diffie Hellman properties
    private Boolean diffieExchange = false;
    private Boolean secretSend = false;
    private Boolean peerSecretReceived = false;
    private BigInteger calcedSenderValue;
    private BigInteger diffieSecret, agreedSecret;
    //2048 bits p & g as defined in Java 8 docs
    private final BigInteger p = new BigInteger("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b65126" +
            "69455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7fe" +
            "b7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7", 16);

    private final BigInteger g = new BigInteger("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782" +
            "675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916e" +
            "a37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a", 16);

    private Boolean delayedMessageExist = false;
    private String delayedMessage;

    private SecretKeySpec sharedSecret;

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
        start=new JButton("Start");
        start.addActionListener(this);

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
        
        
        send = new JButton("Send");
        send.addActionListener(this);
        JPanel southPanel = new JPanel();
        southPanel.add(send);
        southPanel.add(start);
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
    }

    private BigInteger GenerateBigInteger(int bitSize)
    {
        return new BigInteger(bitSize, new SecureRandom());
    }

    private SecretKeySpec PerformSha256(BigInteger agreedScret) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] encodedhash = digest.digest(agreedScret.toString().getBytes(StandardCharsets.UTF_8));

        System.out.println("After Sha256 encode: "+ Arrays.toString(encodedhash));
        return new SecretKeySpec(encodedhash, "AES");
    }
    
    @Override
    public void actionPerformed(ActionEvent e){
        Object o = e.getSource();
        if(o == send){
            if ( tfPort.getText().equals( ConfigManager.getInstance().getValue( "Server.PortNumber" ) ) )
            {
                display( "Cannot give the same port number as the Chat Application Server - Please give the port number of the peer client to communicate!\n" );
                return;
            }

            if(!diffieExchange){
                if (!secretSend){
                    diffieSecret = GenerateBigInteger(2048);
                    calcedSenderValue = g.modPow(diffieSecret, p);
                    this.send(String.valueOf(calcedSenderValue));
                    secretSend = true;
                }
                delayedMessageExist = true;
                delayedMessage = tf.getText();
            } else{
                try {
                    this.send(cryptoManager.encrypt(tf.getText(), sharedSecret));
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
        if(o == start){
            new ListenFromClient().start();
        }
    }
    
    public void display(String str) {
        ta.append(str + "\n");
        ta.setCaretPosition(ta.getText().length() - 1);
    }
    
    public boolean send(String str){
        Socket socket;
        ObjectOutputStream sOutput;		// to write on the socket
        // try to connect to the server
            try {
                    socket = new Socket(tfServer.getText(), Integer.parseInt(tfPort.getText()));
            } 
            // if it failed not much I can so
            catch(Exception ec) {
                    display("Error connecting to server:" + ec.getMessage() + "\n");
                    return false;
            }

            /* Creating both Data Stream */
            try
            {
//			sInput  = new ObjectInputStream(socket.getInputStream());
                    sOutput = new ObjectOutputStream(socket.getOutputStream());
            }
            catch (IOException eIO) {
                    display("Exception creating new Input/output Streams: " + eIO);
                    return false;
            }

        try {
            if (diffieExchange){
                sOutput.writeObject(new ChatMessage(str.length(), str));
                display("You: " + cryptoManager.decrypt(str,sharedSecret));
            }

            sOutput.writeObject(new ChatMessage(str.length(), str));
            System.out.println("You: " + str);
            System.out.println("------------------------------------------------------");
            sOutput.close();
            socket.close();
        } catch (IOException ex) {
            display("Exception creating new Input/output Streams: " + ex);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }
    
    private class ListenFromClient extends Thread{
            public ListenFromClient() {
                keepGoing=true;
            }

            @Override
            public void run(){
                try 
		{ 
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(Integer.parseInt(tfsPort.getText()));
                        //display("Server is listening on port:"+tfsPort.getText());
                        ta.append("Server is listening on port:"+tfsPort.getText() + "\n");
                        ta.setCaretPosition(ta.getText().length() - 1);

			// infinite loop to wait for connections
			while(keepGoing) 
			{
                            // format message saying we are waiting

                            Socket socket = serverSocket.accept();  	// accept connection

                            ObjectInputStream sInput=null;		// to write on the socket

                            /* Creating both Data Stream */
                            try
                            {
                                    sInput = new ObjectInputStream(socket.getInputStream());
                            }
                            catch (IOException eIO) {
                                    display("Exception creating new Input/output Streams: " + eIO);
                            }
                            try {
                                String msg = ((ChatMessage) sInput.readObject()).getMessage();
                                if (!peerSecretReceived){
                                    System.out.println("Msg:"+msg);

                                    if (!secretSend){
                                        diffieSecret = GenerateBigInteger(2048);
                                        calcedSenderValue = g.modPow(diffieSecret, p);
                                        send(String.valueOf(calcedSenderValue));
                                        secretSend = true;
                                    }

                                    BigInteger receivedPeerKey = new BigInteger(msg);
                                    agreedSecret = receivedPeerKey.modPow(diffieSecret, p);


                                    sharedSecret = PerformSha256(agreedSecret);

                                    System.out.println("SharedSecret: "+sharedSecret.hashCode());
                                    System.out.println("p: " + p);
                                    System.out.println("g: " + g);
                                    System.out.println("Sender calculated Value: " + calcedSenderValue);
                                    System.out.println("My choosen Secret: " + diffieSecret);
                                    System.out.println("Agreed Secret: " + agreedSecret);
                                    System.out.println("------------------------------------------------------");

                                    diffieExchange = true;
                                    peerSecretReceived = true;

                                    if (delayedMessageExist){
                                        send(cryptoManager.encrypt(delayedMessage,sharedSecret));
                                        delayedMessageExist = false;
                                    }

                                } else {
                                    System.out.println("p: " + p);
                                    System.out.println("g: " + g);
                                    System.out.println("Sender calculated Value: " + calcedSenderValue);
                                    System.out.println("My choosen Secret: " + diffieSecret);
                                    System.out.println("Agreed Secret: " + agreedSecret);

                                    display(socket.getInetAddress()+": " + socket.getPort() + ": " + cryptoManager.decrypt(msg, sharedSecret));

                                    System.out.println("------------------------------------------------------");
                                }
                                sInput.close();
                                socket.close();
                            } catch (IOException ex) {
                                display("Exception creating new Input/output Streams: " + ex);
                            } catch (Exception ex) {
                                Logger.getLogger(P2PClient.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }
		}
            // something went bad
            catch (IOException e) {
    //            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
    //			display(msg);
            }
        }
    }
}
