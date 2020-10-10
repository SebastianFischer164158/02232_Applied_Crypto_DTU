/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatapplication_server.components.base;

import chatapplication_server.exception.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * This interface is supported by all ChatApplication components that support automatic start-up and 
 * shut down by the Component Manager.
 *
 * It contains the method declarations for initializing, shutting down and running a component.
 * 
 * @author atgianne
 */
public interface IComponent 
{
    /**
     * Method declaration for initializing the component
     * IMPORTANT This method must not block
     */
    abstract public void initialize() throws Exception;
    
    /**
     * Method declaration for signaling the component that it must terminate
     * its execution.
     */
    abstract public void shutdown();
    
    /**
     * Method declaration of the component's main function containing the
     * main component logic.
     */
    abstract public void componentMain();
}
