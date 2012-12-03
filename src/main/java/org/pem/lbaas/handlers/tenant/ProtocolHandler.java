package org.pem.lbaas.handlers.tenant;

/**
 * pemellquist@gmail.com
 */

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;


import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pem.lbaas.datamodel.Protocol;
import org.pem.lbaas.security.KeystoneAuthFilter;

	
@Path("/protocols")
public class ProtocolHandler {
   private static Logger logger = Logger.getLogger(ProtocolHandler.class);
   private static String HTTP = "HTTP";
   private static String TCP = "TCP";
   public  static String DEFAULT_PROTOCOL = HTTP;
   public  static int    DEFAULT_PORT = 80;
   
   @SuppressWarnings("serial")
   static List<Protocol> protocols = new ArrayList<Protocol>() {{
	     add( new Protocol(HTTP, 80));
	     add( new Protocol(TCP,443));
      }};
   
   
   public static boolean exists( String protocol) {
	   for (int x=0;x<protocols.size();x++) {
		   if ( protocols.get(x).getName().equalsIgnoreCase(protocol))
			   return true;
	   }
	   return false;
   }
   
   public static Integer getPort( String protocol) {
	   for (int x=0;x<protocols.size();x++) {
		   if ( protocols.get(x).getName().equalsIgnoreCase(protocol))
			   return protocols.get(x).getPort();
	   }
	   return 0;
   }
      
   @GET
   @Produces("application/json")
   public String get(@Context HttpServletRequest request ) {
	    
	    if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /protocols request cannot be authenticated", 401);  //  bad auth
	    }
	    
	    logger.info("Get /protocols " + KeystoneAuthFilter.toString(request));
		
	    JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		try {	
		   for (int x=0;x<protocols.size();x++) {
			   JSONObject jsonProtocol=new JSONObject();
			   jsonProtocol.put("name", protocols.get(x).getName());
			   if (protocols.get(x).getPort().intValue()==0)
			      jsonProtocol.put("port","*");
			   else
			      jsonProtocol.put("port", protocols.get(x).getPort());
			   
			   jsonArray.put(jsonProtocol);
		   }
			
		   jsonObject.put("protocols",jsonArray);		   				   	   		   
		   return jsonObject.toString();
		}
		catch ( JSONException jsone) {
			logger.error("Internal Server error 500, JSON exception :" + jsone.toString());
			WebApplicationException wae = new WebApplicationException(500);   // internal server error
			throw wae;
		}
   }
   
}
