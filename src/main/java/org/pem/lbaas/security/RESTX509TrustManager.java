package org.pem.lbaas.security;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
	
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

/**
 * RESTX509TrustManager is a custom X509TrustManager which is part of this project for development only and allows inspection and debugging of SSL / TLS certs.
 * NOTE! this trust manager should not be activated in an operational product, instead use actual an truststore and keystore with proper signing.
 * @author pemellquist@gmail.com
 *
 */
public class RESTX509TrustManager implements X509TrustManager
{
   private X509TrustManager standardTrustManager = null;
   protected static Logger logger = Logger.getLogger(RESTX509TrustManager.class);

   /**
    * Constructor
    * @param keystore
    * @throws NoSuchAlgorithmException
    * @throws KeyStoreException
    */
   public RESTX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
      super();
      TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      factory.init(keystore);
      TrustManager[] trustmanagers = factory.getTrustManagers();
      if (trustmanagers.length == 0) {
    	 logger.error("no trust manager found");
         throw new NoSuchAlgorithmException("no trust manager found");
      }
      this.standardTrustManager = (X509TrustManager)trustmanagers[0];
   }

   /**
    * check trusted client
    */
   public void checkClientTrusted(X509Certificate[] certificates,String authType) throws CertificateException {
       standardTrustManager.checkClientTrusted(certificates,authType);
   }

   /**
    * check trusted server
    */
   public void checkServerTrusted(X509Certificate[] certificates,String authType) throws CertificateException {
      if ((certificates != null)) {
      logger.info("Server certificate chain:");
         for (int i = 0; i < certificates.length; i++) 
		    logger.info("X509Certificate[" + i + "]=" + certificates[i]);
	   }
	  if ((certificates != null) && (certificates.length == 1)) {
	     certificates[0].checkValidity();
	  } else {
	     standardTrustManager.checkServerTrusted(certificates,authType);
	  }
   }

   /**
    * get issuers
    */
   public X509Certificate[] getAcceptedIssuers() {
      return this.standardTrustManager.getAcceptedIssuers();
   }
}
