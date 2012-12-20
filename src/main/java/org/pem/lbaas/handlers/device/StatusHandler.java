package org.pem.lbaas.handlers.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pem.lbaas.handlers.tenant.LBaaSException;
import org.pem.lbaas.messaging.LBaaSTaskManager;
import org.pem.lbaas.persistency.DeviceDataModel;
import org.pem.lbaas.persistency.DeviceModelAccessException;
import org.pem.lbaas.persistency.DeviceUsage;

@Path("/status")
public class StatusHandler {
   private static Logger logger = Logger.getLogger(StatusHandler.class);
   private static DeviceDataModel deviceModel = new DeviceDataModel();
   	  		
   // JSON names
   protected final String JSON_STATUS              = "systemStatus";
   protected final String JSON_NAME                = "builName";
   protected final String JSON_VERSION             = "buildVersion";
   protected final String JSON_BUILD_DATE          = "buildDate";
   protected final String JSON_DB_ACCESS           = "databaseAccess";
   protected final String JSON_GEARMAN_ACCESS      = "gearManJobServerAccess";
   
   public  String convertStreamToString(InputStream is) throws IOException {
       if (is != null) {
           StringBuilder sb = new StringBuilder();
           String line;

           try {
              BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
               while ((line = reader.readLine()) != null) {
                   sb.append(line);
               }
           } finally {
               is.close();
           }
           return sb.toString();
       } else {       
           return "";
       }
   }
	
   String resourceToString(String name) throws IOException {
	  ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	  InputStream resource = classLoader.getResourceAsStream(name);
	  return convertStreamToString(resource);
   }
	
   @GET
   @Produces("application/json")
   public String status(@Context HttpServletRequest request ) {
      JSONObject jsonResponseObject=new JSONObject();
      JSONObject status=new JSONObject();
	   	   
      try {			   
         status.put(JSON_NAME, resourceToString("buildname.txt") );
         status.put(JSON_VERSION, resourceToString("buildversion.txt") );
         status.put(JSON_BUILD_DATE, resourceToString("builddate.txt") );
         
         DeviceUsage usage=null;
         String dbAccess="PASS";
         try {
            usage = deviceModel.getUsage();
         }
         catch (DeviceModelAccessException dme) {
        	 dbAccess="FAIL";
         }
         status.put(JSON_DB_ACCESS, dbAccess );
         
         LBaaSTaskManager taskManager = new LBaaSTaskManager();
         status.put(JSON_GEARMAN_ACCESS, taskManager.serverCount() );
		   
	     jsonResponseObject.put(JSON_STATUS , status);
	     return jsonResponseObject.toString();	      
	  }
	  catch ( JSONException jsone) {
		  throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
      }	
	  catch ( IOException ioe) {
		  throw new LBaaSException("internal server error JSON exception :" + ioe.toString(), 500);  //  internal error
      }
	  
	}
	

}
