package org.pem.lbaas.handlers.tenant;

/**
 * pemellquist@gmail.com
 */

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;


public class LBaaSException extends WebApplicationException {
	private static Logger logger = Logger.getLogger(LBaaSException.class);
	
	protected static String errorBody( String message, int status) {
		JSONObject jsonDevice=new JSONObject();
		   try {		  				
		      jsonDevice.put("code", status);
		      String codeMessage=null;
		      if (Response.Status.fromStatusCode(status)==null)
		    	  codeMessage = new Integer(status).toString();	
		      else
		    	  codeMessage = Response.Status.fromStatusCode(status).toString();
			  jsonDevice.put("message", codeMessage );
			  jsonDevice.put("details", message);
			  				  			   			   
			  return jsonDevice.toString();
	      }
		  catch ( JSONException jsone) {
	         return "error encoding fault body";
		   }
	}

	public LBaaSException( String message, int status) {		
		super(Response.status(status).entity( errorBody(message,status) ).type("application/json").build());	
		logger.warn("Error Response : " + status + " message :" + message);
	}
	
}
