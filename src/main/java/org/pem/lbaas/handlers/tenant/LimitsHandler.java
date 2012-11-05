package org.pem.lbaas.handlers.tenant;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.log4j.Logger;

	
@Path("/limits")
public class LimitsHandler {
   public static final int LIMIT_MAX_NAME_SIZE = 64;
   public static final int LIMIT_MAX_ADDR_SIZE = 64;
   
   private static Logger logger = Logger.getLogger(LimitsHandler.class);
   
   @GET
   @Produces("application/json")
   public String limits() {
	   return "not implemented";
   }
}

