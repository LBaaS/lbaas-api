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
import org.pem.lbaas.persistency.DeviceUsage;
import org.pem.lbaas.handlers.tenant.LBaaSException;

import javax.ws.rs.WebApplicationException;

@Path("/devices")
public class DeviceHandler {

	private static Logger logger = Logger.getLogger(DeviceHandler.class);
	public final String DEFAULT_TYPE = "HAProxy";
	
	public final String JSON_DEVICES       = "devices";
	public final String JSON_ID            = "id";
	public final String JSON_NAME          = "name";
	public final String JSON_ADDRESS       = "address";
	public final String JSON_LOADBALANCER  = "loadbalancer";
	public final String JSON_CREATED       = "created";
	public final String JSON_UPDATED       = "updated";
	public final String JSON_TYPE          = "type";
	public final String JSON_STATUS        = "status";
	public final String JSON_TOTAL_DEVICES = "total";
	public final String JSON_FREE_DEVICES  = "free";
	public final String JSON_TAKEN_DEVICES = "taken";
	
	
    protected String deviceToJson(Device device) throws JSONException {		
	   JSONObject jsonDevice=new JSONObject();
	   try {		  				
	      jsonDevice.put(JSON_ID, device.getId());
		  jsonDevice.put(JSON_NAME, device.getName());
		  jsonDevice.put(JSON_ADDRESS, device.getAddress());
		  jsonDevice.put(JSON_LOADBALANCER, device.getLbId());
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
	
	@GET
	@Produces("application/json")
	public String getAll() {
		logger.info("GET devices");
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		DeviceDataModel deviceModel = new DeviceDataModel();
		List<Device> devices = deviceModel.getDevices();
		try {	
		   for (int x=0;x<devices.size();x++) {
			   JSONObject jsonDevice=new JSONObject();
			   jsonDevice.put(JSON_ID, devices.get(x).getId());
			   jsonDevice.put(JSON_NAME, devices.get(x).getName());
			   jsonDevice.put(JSON_ADDRESS, devices.get(x).getAddress());
			   jsonDevice.put(JSON_LOADBALANCER, devices.get(x).getLbId());
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
			throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);  //  internal error
		}
	}
	
	@GET
	@Path("/{id}")
	@Produces("application/json")
	public String getLb(@PathParam("id") String id) 
	{
		logger.info("GET device : " + id);
		DeviceDataModel deviceModel = new DeviceDataModel();
		Integer devId = new Integer(id);
		Device device = deviceModel.getDevice(devId);
		if ( device == null) {
			WebApplicationException wae = new WebApplicationException(404);
			throw wae;
		}		
		
		try {
			return deviceToJson(device);
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);  //  internal error
		} 
	
	}
	
	@GET
	@Path("/usage")
	@Produces("application/json")
	public String usage() 
	{
		logger.info("GET usage");
		DeviceDataModel deviceModel = new DeviceDataModel();
		DeviceUsage usage = deviceModel.getUsage();
		JSONObject jsonUsage = new JSONObject();
		try {		  				
			jsonUsage.put(JSON_TOTAL_DEVICES, usage.total);
			jsonUsage.put(JSON_FREE_DEVICES, usage.free);
			jsonUsage.put(JSON_TAKEN_DEVICES, usage.taken);
			  				  			   			   
			return jsonUsage.toString();
	      }
		  catch ( JSONException jsone) {
			  throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);  //  internal error
		   }
	}
	
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public String post(String content) {
		logger.info("POST devices");
		
		// process POSTed body
		DeviceDataModel deviceModel = new DeviceDataModel();
		Device device = new Device();
		Integer id=0;
		try {
		   JSONObject jsonObject=new JSONObject(content);
		   
		   // name
		   if ( jsonObject.has(JSON_NAME)) {
			   String name = (String) jsonObject.get(JSON_NAME);
			   device.setName(name);
			   logger.info("   name : " + name);
			   if ( deviceModel.existsName(name)) {
				   throw new LBaaSException("device name : " + name + " already exists", 400);  //  bad request
			   }
		   }
		   else {
			   throw new LBaaSException("POST requires 'name' in request body", 400);  //  bad request
		   }
		   
		   // adddress
		   if ( jsonObject.has(JSON_ADDRESS)) {
			   String address = (String) jsonObject.get(JSON_ADDRESS);
			   device.setAddress(address);
			   logger.info("   address : " + address); 
		   }
		   else {
			   throw new LBaaSException("POST requires 'address' in request body", 400);  //  bad request
		   }
		   
		  	 
		   // default loadbalancer to 0 which means unassigned
		   device.setLbId(new Integer(0 ));		   
		   
		   // type  
		   device.setLbType(DEFAULT_TYPE);
		   
		   // default status to offline
		   device.setStatus(Device.STATUS_OFFLINE);		   
		  		   
		   // create new Device
		   id = deviceModel.createDevice(device);		   	
		   
		}
		catch (JSONException jsone) {
			throw new LBaaSException("Submitted JSON Exception : " + jsone.toString(), 400);  //  bad request
		}
		
		// read Device back from data model
		Device deviceResponse = deviceModel.getDevice(id);
		
		try {
		   return deviceToJson(deviceResponse);
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("Internal JSON Exception : " + jsone.toString(), 500);  //  internal error
		} 
								
	}	
	
	
	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	public void deleteLb(@PathParam("id") String id) 
	{
		logger.info("DELETE loadbalancer : " + id);
		DeviceDataModel deviceModel = new DeviceDataModel();
		Integer devId = new Integer(id);
		int deleteCount = deviceModel.deleteDevice(devId);
		if (deleteCount==0) {
			throw new LBaaSException("could not find id : " + id + " on delete : ", 404);           // not found			
		}
	}
	
	
	@PUT
	@Path("/{id}")
	@Consumes("application/json")
	public void updateLb(@PathParam("id") String id, String content) 
	{
		logger.info("PUT devices : " + id);
		DeviceDataModel deviceModel = new DeviceDataModel();
		Integer devId = new Integer(id);
		Device device = deviceModel.getDevice(devId);
		if ( device == null) {
			throw new LBaaSException("could not find id : " + id + " on put : " + id, 404);           // not found
		}
		
		String name, address;
		JSONObject jsonObject=null;
		
		try {
		  jsonObject=new JSONObject(content);
		}
		catch (JSONException jsone) {
			throw new LBaaSException("Submitted JSON Exception : " + jsone.toString(), 400);  //  bad request
		}
		
		// change the device name
		try {
		   name = (String) jsonObject.get(JSON_NAME);
		   device.setName(name);
		   logger.info("name = " + name);
		   if ( deviceModel.existsName(name)) {
			   throw new LBaaSException("device name : " + name + " already exists", 400);  //  bad request
		   }
		}
		catch (JSONException e) {
			name =null;
		}
		
		// change the address
		try {
			address = (String) jsonObject.get(JSON_ADDRESS);
			device.setAddress(address);
			logger.info("address = " + address);
		}
		catch (JSONException e) {
			address =null;
		}
		
		if ((name==null) && (address==null)) {
			throw new LBaaSException("missing 'name' and 'address' ", 400);  //  bad request
		}
		
		
		deviceModel.setDevice(device);
										
	}

		
}
