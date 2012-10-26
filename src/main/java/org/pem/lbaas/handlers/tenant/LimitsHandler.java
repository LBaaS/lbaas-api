package org.pem.lbaas.handlers.tenant;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.log4j.Logger;

	
@Path("/limits")
public class LimitsHandler {
   private static Logger logger = Logger.getLogger(LimitsHandler.class);
   
   @GET
   @Produces("application/json")
   public String limits() {
	   return "not implemented";
   }
}

