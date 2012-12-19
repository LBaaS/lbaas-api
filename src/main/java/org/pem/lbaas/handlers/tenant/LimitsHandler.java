package org.pem.lbaas.handlers.tenant;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.pem.lbaas.Lbaas;
import org.pem.lbaas.security.KeystoneAuthFilter;

	
@Path("/v1.1/limits")
public class LimitsHandler {
   public static final int LIMIT_MAX_NAME_SIZE = 128;
   public static final int LIMIT_MAX_ADDR_SIZE = 128;
   
   public    static String JSON_LIMITS            = "limits";
   public    static String JSON_ABSOLUTE          = "absolute";	
   protected static String JSON_VALUES            = "values";
   protected static String JSON_MAX_LBS           = "maxLoadBalancers";
   protected static String JSON_MAX_NODES_PER_LB  = "maxNodesPerLoadBalancer";
   protected static String JSON_MAX_VIPS_PER_LB   = "maxVIPsPerLoadBalancer";
   protected static String JSON_MAC_NAME_LEN      = "maxLoadBalancerNameLength";
   
   private static Logger logger = Logger.getLogger(LimitsHandler.class);
   
   @GET
   @Produces("application/json")
   public String limits(@Context HttpServletRequest request) {
	   
	   if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /limits request cannot be authenticated", 401);  //  bad auth
	    }
	    
	   logger.info("Get /limits " + KeystoneAuthFilter.toString(request));
	    
	   JSONObject jsonResponseObject=new JSONObject();
	   JSONObject limits=new JSONObject();
	   JSONObject absolute=new JSONObject();
	   JSONObject values=new JSONObject();
	   
	   try {

	      values.put(JSON_MAX_LBS,Lbaas.lbaasConfig.maxLbs);
	      values.put(JSON_MAX_NODES_PER_LB,Lbaas.lbaasConfig.maxNodesperLb);
	      values.put(JSON_MAX_VIPS_PER_LB,Lbaas.lbaasConfig.maxVipsPerLb);
	      values.put(JSON_MAC_NAME_LEN,LIMIT_MAX_NAME_SIZE);
	      
	      absolute.put(JSON_VALUES, values);
	      
	      limits.put(JSON_ABSOLUTE, absolute);
	      
	      jsonResponseObject.put(JSON_LIMITS , limits);
	    
	      return jsonResponseObject.toString();
	   
	   }
	   catch ( JSONException jsone) {
		   throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
	   }
   }
}

