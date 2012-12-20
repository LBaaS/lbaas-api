package org.pem.lbaas.handlers.tenant;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pem.lbaas.persistency.DeviceModelAccessException;
import org.pem.lbaas.persistency.DeviceUsage;
import org.pem.lbaas.security.KeystoneAuthFilter;

	
@Path("/")
public class VersionHandler {
	
   private static Logger logger = Logger.getLogger(VersionHandler.class);	
   
   protected static String CURRENT_VERSION      = "v1.1";
   protected static String LAST_UPDATED         = "2012-12-18T18:30:02.25Z";
   protected static String ATLAS_API_1_1        = "http://wiki.openstack.org/Atlas-LB";
   
   protected static String JSON_VERSIONS        = "versions";
   protected static String JSON_VERSION         = "version";
   protected static String JSON_VERSION_ID      = "id";
   protected static String JSON_VERSION_STATUS  = "status";
   protected static String JSON_VERSION_UPDATED = "updated";
   protected static String JSON_LINK_REL        = "rel";
   protected static String JSON_LINK_REF        = "href";
   protected static String JSON_LINKS           = "links";
   protected static String JSON_MEDIA_BASE      = "base";
   protected static String JSON_MEDIA_TYPES     ="media-types";
   
   
 
   
   @GET
   @Produces("application/json")
   public String limits(@Context HttpServletRequest request) {
	   	  	   
	   if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get / request cannot be authenticated", 401);  //  bad auth
	    }
	    
	   logger.info("Get / " + KeystoneAuthFilter.toString(request));
	    
	   JSONObject jsonResponseObject=new JSONObject();
	   JSONArray versions = new JSONArray();
	   JSONObject version=new JSONObject();
	   JSONArray links = new JSONArray();
	   JSONObject link=new JSONObject();
	   
	   
	   try {

		   version.put(JSON_VERSION_ID, CURRENT_VERSION);
		   version.put(JSON_VERSION_STATUS, "CURRENT");
		   version.put(JSON_VERSION_UPDATED, LAST_UPDATED);
		   
		   link.put(JSON_LINK_REL, "self");
		   link.put(JSON_LINK_REF, ATLAS_API_1_1);
		   links.put(link);
		   version.put(JSON_LINKS, links);
		   
		   versions.put(version);
	      
	      
	      jsonResponseObject.put(JSON_VERSIONS , versions);
	    
	      return jsonResponseObject.toString();
	   
	   }
	   catch ( JSONException jsone) {
		   throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
	   }
   }
   
   
   @GET
   @Path("/v1.1")
   @Produces("application/json")
   public String v11(@Context HttpServletRequest request) 
   {
      logger.info("GET /v1.1");
		
	   if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /limits request cannot be authenticated", 401);  //  bad auth
	    }
	    
	   logger.info("Get / " + KeystoneAuthFilter.toString(request));
	    
	   JSONObject jsonResponseObject=new JSONObject();
	   JSONObject version=new JSONObject();
	   JSONArray links = new JSONArray();
	   JSONObject link=new JSONObject();
	   JSONArray medias = new JSONArray();
	   JSONObject media =new JSONObject();
	   
	   
	   try {

		   version.put(JSON_VERSION_ID, CURRENT_VERSION);
		   version.put(JSON_VERSION_STATUS, "CURRENT");
		   version.put(JSON_VERSION_UPDATED, LAST_UPDATED);
		   
		   link.put(JSON_LINK_REL, "self");
		   link.put(JSON_LINK_REF, ATLAS_API_1_1);
		   links.put(link);
		   version.put(JSON_LINKS, links);
		   
		   media.put(JSON_MEDIA_BASE,"application/json");
		   medias.put(media);
		   version.put(JSON_MEDIA_TYPES, medias);
		   	      	      
	       jsonResponseObject.put(JSON_VERSION , version);
	    
	      return jsonResponseObject.toString();
	   
	   }
	   catch ( JSONException jsone) {
		   throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
	   }
   }
   
   
   
}
