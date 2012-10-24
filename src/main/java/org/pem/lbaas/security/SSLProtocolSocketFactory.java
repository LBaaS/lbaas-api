package org.pem.lbaas.security;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManager;
import org.apache.commons.httpclient.HttpClientError;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.UnknownHostException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.log4j.Logger;

import javax.net.SocketFactory;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

/**
 * Custom SecureProtocolSocketFactory allows using your own trustore and keystore which are passed in
 * @author pemellquist@gmail.com
 *
 */
public class SSLProtocolSocketFactory implements SecureProtocolSocketFactory {
   private SSLContext      sslcontext = null;
   private static          TrustManagerFactory tmf = null;
   private static          KeyManagerFactory kmf = null;
   private static boolean  devMode=false;
   
   protected static Logger logger = Logger.getLogger(SSLProtocolSocketFactory.class);
		
   /**
    * Constructor
    * @param trustMgrFactory      trustore to use
    * @param keyManagerFactory    keystore to use
    * @param developmentMode      dev mode to true allow inspecting and loggin cert information ( should always be false unless for development debugging )
    */
   public SSLProtocolSocketFactory(TrustManagerFactory trustMgrFactory, KeyManagerFactory keyManagerFactory, boolean developmentMode) {
	  super();
	  tmf = trustMgrFactory;
	  kmf = keyManagerFactory;
	  devMode = developmentMode;
   }
		
   /**
    * create an SSL context using the truststore and keystore loaded from the constructor
    * @return
    */
   private static SSLContext createSSLContext() {
	      try {
	          SSLContext context = SSLContext.getInstance("SSL");
	          if (devMode) {
	         	 context.init( null, new TrustManager[] {new RESTX509TrustManager(null)}, null); 
	          } else {
	             TrustManager[] trustManagers = tmf.getTrustManagers();
	             if ( kmf!=null) {
	                KeyManager[] keyManagers = kmf.getKeyManagers();
	                
	                // key manager and trust manager
	                context.init(keyManagers,trustManagers,null);
	             }
	             else
	             	// no key managers
	             	context.init(null,trustManagers,null);
	          }
	          return context;
	       } 
	       catch (Exception e) {
	    	  logger.error(e.toString());
	          throw new HttpClientError(e.toString());
	 	  }
   }
   
   /**
    * return the current SSL context
    * @return SSLContext
    */
   private SSLContext getSSLContext() {
      if (this.sslcontext == null) {
         this.sslcontext = createSSLContext();
      }
      return this.sslcontext;
   }

   /**
    * Create a socket using custom SSLContext
    */
   public Socket createSocket( String host, int port, InetAddress clientHost, int clientPort) throws IOException, UnknownHostException {
      return getSSLContext().getSocketFactory().createSocket( host, port, clientHost, clientPort );
   }
  
  
   /**
    * Create a socket version, with HttpConnectionParams
    */
   public Socket createSocket( final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params ) throws IOException, UnknownHostException, ConnectTimeoutException {
      if (params == null) {
         throw new IllegalArgumentException("Parameters may not be null");
      }
      int timeout = params.getConnectionTimeout();
      SocketFactory socketfactory = getSSLContext().getSocketFactory();
      if (timeout == 0) {
         return socketfactory.createSocket(host, port, localAddress, localPort);
      } else {
   	     Socket socket = socketfactory.createSocket();
   	     SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
   	     SocketAddress remoteaddr = new InetSocketAddress(host, port);
   	     socket.bind(localaddr);
   	     socket.connect(remoteaddr, timeout);
   	     return socket;
   	   }
   }
   

   /**
    * Create a socket, version with host and port only
    */
   public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
      return getSSLContext().getSocketFactory().createSocket( host, port );
   }
   	
   /**
    * Create a socket, version with autoClose
    */
   public Socket createSocket( Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
	   return getSSLContext().getSocketFactory().createSocket( socket, host, port, autoClose );
   }
   

   /**
    * equals test
    */
   public boolean equals(Object obj) {
      return ((obj != null) && obj.getClass().equals(SSLProtocolSocketFactory.class));
   }
   
   /**
    * hashcode
    */
   public int hashCode() {
      return SSLProtocolSocketFactory.class.hashCode();
   }
   
   	
}
   
   
   
   
