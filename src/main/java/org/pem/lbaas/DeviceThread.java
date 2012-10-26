package org.pem.lbaas;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;


public class DeviceThread extends Thread {
	
   private static Logger logger = Logger.getLogger(DeviceThread.class);
	
   protected LbaasConfig lbaasConfig=null;
	
   public DeviceThread(LbaasConfig config)
   {
	   lbaasConfig=config;
   }


   public void run()
   {	
      try {
    	  
	     Server server = new Server();
	     
	     Connector restconnector = new SelectChannelConnector();			   		   
		 restconnector.setPort(lbaasConfig.adminPort);
		 server.addConnector(restconnector);			   
		 ServletHolder sh = new ServletHolder();
		 sh.setName("device");
		 sh.setClassName("com.sun.jersey.spi.container.servlet.ServletContainer");
		 sh.setInitParameter("com.sun.jersey.config.property.packages", "org.pem.lbaas.handlers.device");
		 ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		 context.setContextPath("/");
		 server.setHandler(context); 
		 context.addServlet(sh, "/*");	
						    				   
         server.start();
		 server.join();
      }
      catch ( Exception e) {			 
         logger.error(e);
         return;
      }
		   	      
   }

}
