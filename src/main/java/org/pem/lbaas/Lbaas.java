package org.pem.lbaas;

/**
 * pemellquist@gmail.com
 */

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.apache.log4j.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.pem.lbaas.security.KeystoneAuthFilter;

import com.hp.csbu.cc.middleware.*;
import com.sun.jersey.api.core.ResourceConfig;



public class Lbaas 
{
	private static Logger logger = Logger.getLogger(Lbaas.class);
	
	public static LbaasConfig lbaasConfig;
	
	public Lbaas()
	{
		logger.info("LBaaS API Server");
	}
		
	
	@SuppressWarnings("deprecation")
	public void run( String[] args)
	{   
		 lbaasConfig = new LbaasConfig();
		 
		 if (!lbaasConfig.load(args[0])) {
			 logger.error("unable to load lbaas config file : " + args[0]);
			 return;
		 }
		 lbaasConfig.log();
		
		 try {	    	  
            Server server = new Server();  
            				      			   			
			SslSocketConnector sslConnector = new SslSocketConnector();
			sslConnector.setPort(lbaasConfig.apiPort);
			sslConnector.setKeyPassword(lbaasConfig.keystorePwd);
      	    sslConnector.setKeystore(lbaasConfig.keystore);  
      	    server.addConnector(sslConnector);
						
			ServletHolder sh = new ServletHolder();
			sh.setName("lbaas");
			sh.setClassName("com.sun.jersey.spi.container.servlet.ServletContainer");
			sh.setInitParameter("com.sun.jersey.config.property.packages", "org.pem.lbaas.handlers.tenant");
			sh.setInitParameter(ResourceConfig.FEATURE_NORMALIZE_URI, "true");
			sh.setInitParameter(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH, "true");
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
						
			context.setContextPath("/");
			KeystoneAuthFilter.setupAuthFilter(context,lbaasConfig);
			context.addServlet(sh, "/*");
			server.setHandler(context); 
				
			
		    DeviceThread deviceThread = new DeviceThread(lbaasConfig);
		    deviceThread.start();
			    				   
		    server.start();  
		    server.join();
		  }
		  catch ( Exception e) {			 
		     logger.error(e);
		     return;
		  }
	}
	
    public static void main( String[] args )
    {   
    	System.out.println("main");
    	if (args.length<1) {
    		System.out.println("not enough args provided!");
    		System.out.println("Lbaas <configfile>");
    		System.out.println("");
    		return;
    	}
    	
    	Lbaas lbaas = new Lbaas();
    	lbaas.run(args);       	    	    	    	    	
    }
}
