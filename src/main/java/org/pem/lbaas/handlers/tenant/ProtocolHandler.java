package org.pem.lbaas.handlers.tenant;

/**
 * pemellquist@gmail.com
 */

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
   private static int    MIN_PORT = 1;
   private static int    MAX_PORT = 65535;
   private static final String IPV4_PATTERN = 
       "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
       "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
       "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
       "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
   
	   
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
   
   public static int toPort(String port) {
	   int value=-1;
	   
	   try {
	      value = Integer.parseInt(port);
	      if ((value>=MIN_PORT)&&(value<=MAX_PORT))
	    	  return value;
	      else
	    	  return -1;
	   }
	   catch (NumberFormatException nfe) {
	      return -1;
	   }
   }
   
   public static boolean supports( String protocol, int port ) {
	   for (int x=0;x<protocols.size();x++) {
		   if (protocols.get(x).getName().equalsIgnoreCase(protocol))
			   if (protocols.get(x).getPort().intValue() == port)
				   return true;
			   else
				   return false;
	   }
	   return false;
   }
   
   public final static boolean validateIPv4Address(String  ipAddress)
   {
      Pattern pattern = Pattern.compile(IPV4_PATTERN);
	  Matcher matcher = pattern.matcher(ipAddress);
	  return matcher.matches();           
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
