package org.pem.lbaas.handlers.tenant;

/**
 * pemellquist@gmail.com
 */

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


@Path("/algorithms")
public class AlgorithmsHandler {
   private static Logger logger = Logger.getLogger(AlgorithmsHandler.class);
   private static String ROUND_ROBIN       = "ROUND_ROBIN";
   private static String LEAST_CONNECTIONS = "LEAST_CONNECTIONS";
   public  static String DEFAULT_ALGO      = ROUND_ROBIN;
   
   @SuppressWarnings("serial")
   static List<String> algorithms = new ArrayList<String>() {{
	     add( new String(ROUND_ROBIN));
	     add( new String(LEAST_CONNECTIONS));
      }};
   
   
   public static boolean exists( String name) {
	   for (int x=0;x<algorithms.size();x++) {
		   if ( algorithms.get(x).equalsIgnoreCase(name))
			   return true;
	   }
	   return false;
   }
   
      
   @GET
   @Produces("application/json")
   public String get() {
	   logger.info("GET algorithms");
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		try {	
		   for (int x=0;x<algorithms.size();x++) {
			   JSONObject jsonAlgorithm=new JSONObject();
			   jsonAlgorithm.put("name", algorithms.get(x));
			  			   
			   jsonArray.put(jsonAlgorithm);
		   }
			
		   jsonObject.put("algorithms",jsonArray);		   				   	   		   
		   return jsonObject.toString();
		}
		catch ( JSONException jsone) {
			logger.error("Internal Server error 500, JSON exception :" + jsone.toString());
			WebApplicationException wae = new WebApplicationException(500);    
			throw wae;
		}
   }
   
}
