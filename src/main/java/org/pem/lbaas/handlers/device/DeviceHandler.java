package org.pem.lbaas.handlers.device;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces; 

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pem.lbaas.datamodel.Device;
import org.pem.lbaas.persistency.DeviceDataModel;
import org.pem.lbaas.persistency.DeviceModelAccessException;
import org.pem.lbaas.persistency.DeviceUsage;
import org.pem.lbaas.handlers.tenant.LBaaSException;
import org.pem.lbaas.handlers.tenant.LimitsHandler;
import org.pem.lbaas.handlers.tenant.ProtocolAddressException;
import org.pem.lbaas.handlers.tenant.ProtocolHandler;
import org.pem.lbaas.handlers.tenant.VipException;

import javax.ws.rs.WebApplicationException;


/**
 * DeviceHandler JAX-RS REST handler for devices
 * @author peter
 *
 */

@Path("/devices")
public class DeviceHandler {
   private static Logger logger = Logger.getLogger(DeviceHandler.class);
   private static DeviceDataModel deviceModel = new DeviceDataModel();
   public final String DEFAULT_TYPE       = "HAProxy";
   public final int    LB_UNASSIGNED      = 0;
	
   // JSON names
   protected final String JSON_DEVICES        = "devices";
   protected final String JSON_ID             = "id";
   protected final String JSON_NAME           = "name";
   protected final String JSON_FLOAT_ADDRESS  = "floatingIpAddr";
   protected final String JSON_PUBLIC_ADDRESS = "publicIpAddr";
   protected final String JSON_AZ              = "az";
   protected final String JSON_LOADBALANCERS  = "loadbalancers";
   protected final String JSON_CREATED        = "created";
   protected final String JSON_UPDATED        = "updated";
   protected final String JSON_TYPE           = "type";
   protected final String JSON_STATUS         = "status";
   protected final String JSON_TOTAL_DEVICES  = "total";
   protected final String JSON_FREE_DEVICES   = "free";
   protected final String JSON_TAKEN_DEVICES  = "taken";
	
	
   /**
    * Convert a Device object to JSON
    * @param device
    * @return JSON encoded Device
    * @throws JSONException
    */
   protected String deviceToJson(Device device) throws JSONException {		
      JSONObject jsonDevice=new JSONObject();
      try {		  				
         jsonDevice.put(JSON_ID, device.getId());
         jsonDevice.put(JSON_NAME, device.getName());
         jsonDevice.put(JSON_FLOAT_ADDRESS, device.getAddress());
         jsonDevice.put(JSON_PUBLIC_ADDRESS, device.getPublicIP());
         jsonDevice.put(JSON_AZ, device.getAz());
         jsonDevice.put(JSON_LOADBALANCERS, DeviceDataModel.lbIdsToJson(device.lbIds));
         jsonDevice.put(JSON_TYPE, device.getLbType());
         jsonDevice.put(JSON_STATUS, device.getStatus());	
         jsonDevice.put(JSON_CREATED, device.getCreated());
         jsonDevice.put(JSON_UPDATED, device.getUpdated());
			  			   			   
         return jsonDevice.toString();
      }
      catch ( JSONException jsone) {
         throw jsone;
      }
   }
   
	
   /**
    * Get All devices
    * @return JSON array of devices
    */
   @GET
   @Produces("application/json")
   public String getAll() {
      logger.info("GET devices");
      JSONObject jsonObject = new JSONObject();
      JSONArray jsonArray = new JSONArray();
      List<Device> devices =null;
		
      try {
         devices = deviceModel.getDevices(null);
      }
      catch ( DeviceModelAccessException dme) {
         throw new LBaaSException(dme.message, 500);                                   
      }
		
      try {	
         for (int x=0;x<devices.size();x++) {
            JSONObject jsonDevice=new JSONObject();
            jsonDevice.put(JSON_ID, devices.get(x).getId());
            jsonDevice.put(JSON_NAME, devices.get(x).getName());
            jsonDevice.put(JSON_FLOAT_ADDRESS, devices.get(x).getAddress());
            jsonDevice.put(JSON_PUBLIC_ADDRESS, devices.get(x).getPublicIP());
            jsonDevice.put(JSON_AZ, devices.get(x).getAz());
            jsonDevice.put(JSON_LOADBALANCERS, DeviceDataModel.lbIdsToJson(devices.get(x).lbIds));
            jsonDevice.put(JSON_TYPE, devices.get(x).getLbType());
            jsonDevice.put(JSON_STATUS, devices.get(x).getStatus());
            jsonDevice.put(JSON_CREATED, devices.get(x).getCreated());
            jsonDevice.put(JSON_UPDATED, devices.get(x).getUpdated());
			   
            jsonArray.put(jsonDevice);
         }			
         jsonObject.put(JSON_DEVICES,jsonArray);		   				   	   		   
         return jsonObject.toString();
      }
      catch ( JSONException jsone) {			
         throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);  
      }
   }
   
	
    /**
     * Get a specific Device object referenced by its id
     * @param id
     * @return JSON encoded Device
     */
	@GET
	@Path("/{id}")
	@Produces("application/json")
	public String getDevice(@PathParam("id") String id) 
	{
		logger.info("GET device : " + id);
		Device device = null;
		
		long longId=0;
		try {
		   longId= Long.parseLong(id);		         
		} 
		catch (NumberFormatException nfe) {
			   throw new LBaaSException("id : " + id + " is not a valid id",404);
	    }		
		
		try {
		   device = deviceModel.getDevice(longId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}
		
		if ( device == null) {
			throw new LBaaSException("device id:" + id + " not found", 404);   
		}		
		
		try {
			return deviceToJson(device);
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);   
		} 
	}
	
   /**
    * Get usage stats on all Devices	
    * @return JSON encoded usage stats
    */
   @GET
   @Path("/usage")
   @Produces("application/json")
   public String usage() 
   {
      logger.info("GET usage");
		
      DeviceUsage usage=null;
      try {
         usage = deviceModel.getUsage();
      }
      catch (DeviceModelAccessException dme) {
    	  throw new LBaaSException( dme.message,500);
      }
      
      JSONObject jsonUsage = new JSONObject();
      try {		  				
         jsonUsage.put(JSON_TOTAL_DEVICES, usage.total);
         jsonUsage.put(JSON_FREE_DEVICES, usage.free);
         jsonUsage.put(JSON_TAKEN_DEVICES, usage.taken);
			  				  			   			   
         return jsonUsage.toString();
      }
      catch ( JSONException jsone) {
         throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);   
      }
   }
   
	
   /**
    * Create a new device using POST'ed JSON body
    * @param content
    * @return newly created Device with all fields
    */
   @POST
   @Consumes("application/json")
   @Produces("application/json")
   public String post(String content) {
      logger.info("POST devices");
		
      // process POSTed body		
      Device device = new Device();
	  Long id= new Long(0);
      try {
         JSONObject jsonObject=new JSONObject(content);
		   
         // name
         if ( jsonObject.has(JSON_NAME)) {
            String name = (String) jsonObject.get(JSON_NAME);
            if ( name.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
            	throw new LBaaSException("'name' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400);  
            device.setName(name);
            logger.info("   name : " + name);
            if ( deviceModel.existsName(name)) {
               throw new LBaaSException("device name : " + name + " already exists", 400);   
            }
         }
         else {
            throw new LBaaSException("POST requires 'name' in request body", 400);   
         }
                  		   
         // floating IP address
         if ( jsonObject.has(JSON_FLOAT_ADDRESS)) {
            String address = (String) jsonObject.get(JSON_FLOAT_ADDRESS);
            if ( ! ProtocolHandler.validateIPv4Address(address)) 
				throw new LBaaSException("not a valid IPV4 floating address : " + address + " for node definition",400);			   
            
            if ( address.length() > LimitsHandler.LIMIT_MAX_ADDR_SIZE)
            	throw new LBaaSException("'address' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_ADDR_SIZE, 400);
            device.setAddress(address);
            logger.info("   address : " + address); 
         }
         else {
            throw new LBaaSException("POST requires 'floatingIpAddr' in request body", 400);   
         }
         
         // public IP adddress
         if ( jsonObject.has(JSON_PUBLIC_ADDRESS)) {
            String address = (String) jsonObject.get(JSON_PUBLIC_ADDRESS);
            if ( ! ProtocolHandler.validateIPv4Address(address)) 
				throw new LBaaSException("not a valid IPV4 public address : " + address + " for node definition",400);			   
            
            if ( address.length() > LimitsHandler.LIMIT_MAX_ADDR_SIZE)
            	throw new LBaaSException("'address' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_ADDR_SIZE, 400);
            device.setPublicIP(address);
            logger.info("   address : " + address); 
         }
         else {
            throw new LBaaSException("POST requires 'publicIpAddr' in request body", 400);   
         }
         
         // AZ
         if ( jsonObject.has(JSON_AZ)) {
            int az = jsonObject.getInt(JSON_AZ);       	 
        	logger.info("   AZ : " + az); 
        	device.setAz(az);        	              	           	 
         }
         else {
            throw new LBaaSException("POST requires 'az' in request body", 400);   
         }
         
         
         // Type
         if ( jsonObject.has(JSON_TYPE)) {
            String type = (String) jsonObject.get(JSON_TYPE);  
            if (type.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
            	throw new LBaaSException("type string exceeds limit of :" + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400); 
            device.setLbType(type);
            logger.info("   type : " + type); 
         }
         else {
            throw new LBaaSException("POST requires 'type' in request body", 400);   
         }
                  
		   		  	    
         device.lbIds.clear();	   
		   
         device.setStatus(Device.STATUS_OFFLINE);		   
		  		   
         id = deviceModel.createDevice(device);		   	
		   
      }
      catch ( DeviceModelAccessException dme) {
         throw new LBaaSException(dme.message, 500);
      }
		
      catch (JSONException jsone) {
         throw new LBaaSException("Submitted JSON Exception : " + jsone.toString(), 400);  
      }
		
      // read Device back from data model, will have new generated id		
      Device deviceResponse = null;
      try {
         deviceResponse = deviceModel.getDevice(id);	
         return deviceToJson(deviceResponse);
      }
      catch ( DeviceModelAccessException dme) {
         throw new LBaaSException(dme.message, 500);
      }
      catch ( JSONException jsone) {
         throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);
      } 							
   }	
	
   /**
    * Delete a Device
    * @param id
    */
   @DELETE
   @Path("/{id}")
   @Produces("application/json")
   public void deleteDevice(@PathParam("id") String id) 
   {
      logger.info("DELETE device : " + id);
		
      long longId=0;
	  try {
	     longId= Long.parseLong(id);		         
      } 
      catch (NumberFormatException nfe) {
         throw new LBaaSException("id : " + id + " is not a valid id",404);
      }	
      Device device = null;
      
      try {
          device = deviceModel.getDevice(longId);
       }
       catch ( DeviceModelAccessException dme) {
          throw new LBaaSException(dme.message, 500);
       }
       
       if ( device == null) {
          throw new LBaaSException("could not find id : " + id + " on put : " + id, 404);
       }
       
       if ( !device.lbIds.isEmpty()) {
    	   throw new LBaaSException("can not delete, device still in use by loadbalancer : " + device.lbIds, 403);
       }
       
      
      int deleteCount=0;
      try {
         deleteCount = deviceModel.deleteDevice(longId);
      }
      catch ( DeviceModelAccessException dme) {
         throw new LBaaSException(dme.message, 500);
      }
		
      if (deleteCount==0) {
         throw new LBaaSException("could not find id : " + id + " on delete : ", 404);			
      }
   }
	
	
   /**
    * Update an existing device
    * @param id
    * @param content
    * @return changed device
    */
   @PUT
   @Path("/{id}")
   @Consumes("application/json")
   @Produces("application/json")
   public String updateDevice(@PathParam("id") String id, String content) 
   {
      logger.info("PUT devices : " + id);
      Device device = null;
		
      long longId=0;
	  try {
	     longId= Long.parseLong(id);		         
      } 
      catch (NumberFormatException nfe) {
         throw new LBaaSException("id : " + id + " is not a valid id",404);
      }	
      
      try {
         device = deviceModel.getDevice(longId);
      }
      catch ( DeviceModelAccessException dme) {
         throw new LBaaSException(dme.message, 500);
      }
      
      if ( device == null) {
         throw new LBaaSException("could not find id : " + id + " on put : " + id, 404);
      }
		
      String name, address;
      JSONObject jsonObject=null;
		
      try {
         jsonObject=new JSONObject(content);
      }
      catch (JSONException jsone) {
         throw new LBaaSException("Submitted JSON Exception : " + jsone.toString(), 400);  
      }
		
      // change the device name
      try {
         name = (String) jsonObject.get(JSON_NAME);
         if ( name.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
         	throw new LBaaSException("'name' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400); 
         device.setName(name);
         logger.info("name = " + name);
         try {
            if (deviceModel.existsName(name)) {
			      throw new LBaaSException("device name : " + name + " already exists", 400);  
            }
         }
         catch (DeviceModelAccessException dme) {
             throw new LBaaSException(dme.message, 500);
         }
      }
      catch (JSONException e) {
         name =null;
      }
		
      // change the floating IP address
      try {
         address = (String) jsonObject.get(JSON_FLOAT_ADDRESS);
         
         if ( ! ProtocolHandler.validateIPv4Address(address)) 
				throw new LBaaSException("not a valid IPV4 address : " + address + " for node definition",400);
         
         if ( address.length() > LimitsHandler.LIMIT_MAX_ADDR_SIZE)
         	throw new LBaaSException("'address' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_ADDR_SIZE, 400);
         device.setAddress(address);
         logger.info("address = " + address);
      }
      catch (JSONException e) {
			address=null;
      }
		
      if ((name==null) && (address==null)) {
         throw new LBaaSException("missing 'name' and 'address' ", 400);  
      }
		
      try {
         deviceModel.setDevice(device);
      }
      catch (DeviceModelAccessException dme) {
          throw new LBaaSException(dme.message, 500);
      }
      
      // return back entire device with changes
      Device deviceResponse = null;
      try {
         deviceResponse = deviceModel.getDevice(new Long(device.getId()));	
         return deviceToJson(deviceResponse);
      }
      catch ( DeviceModelAccessException dme) {
         throw new LBaaSException(dme.message, 500);
      }
      catch ( JSONException jsone) {
         throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);
      } 
										
	}

		
}
