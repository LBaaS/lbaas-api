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
import org.pem.lbaas.handlers.tenant.LBaaSException;;

import javax.ws.rs.WebApplicationException;

@Path("/devices")
public class DeviceHandler {

	private static Logger logger = Logger.getLogger(DeviceHandler.class);
	public final String DEFAULT_TYPE = "HAProxy";
	
    protected String deviceToJson(Device device) throws JSONException {		
	   JSONObject jsonDevice=new JSONObject();
	   try {		  				
	      jsonDevice.put("id", device.getId());
		  jsonDevice.put("name", device.getName());
		  jsonDevice.put("address", device.getAddress());
		  jsonDevice.put("loadbalancer", device.getLbId());
		  jsonDevice.put("type", device.getLbType());
		  jsonDevice.put("status", device.getStatus());	
			  			   			   
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
			   jsonDevice.put("id", devices.get(x).getId());
			   jsonDevice.put("name", devices.get(x).getName());
			   jsonDevice.put("address", devices.get(x).getAddress());
			   jsonDevice.put("loadbalancer", devices.get(x).getLbId());
			   jsonDevice.put("type", devices.get(x).getLbType());
			   jsonDevice.put("status", devices.get(x).getStatus());			   
			   
			   jsonArray.put(jsonDevice);
		   }
			
		   jsonObject.put("devices",jsonArray);		   				   	   		   
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
		   
		   if ( jsonObject.has("name")) {
			   String name = (String) jsonObject.get("name");
			   device.setName(name);
			   logger.info("   name = " + name);
			   if ( deviceModel.existsName(name)) {
				   throw new LBaaSException("device name already exists", 400);  //  bad request
			   }
		   }
		   else {
			   throw new LBaaSException("Missing 'name' in resource request", 400);  //  bad request
		   }
		   
		   if ( jsonObject.has("address")) {
			   String address = (String) jsonObject.get("address");
			   device.setAddress(address);
			   logger.info("   address = " + address); 
		   }
		   else {
			   throw new LBaaSException("Missing 'address' in resource request", 400);  //  bad request
		   }
		   
		  	 
		   // default loadbalancer to 0 which means unassigned
		   device.setLbId(new Integer(0 ));		   
		   
		   if ( jsonObject.has("type")) {
			   String type = (String) jsonObject.get("type");
			   device.setLbType(type);
			   logger.info("   type = " + type);
		   }
		   else
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
			throw new LBaaSException("could not find id on delete : " + id, 404);           // not found			
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
			throw new LBaaSException("could not find id on put : " + id, 404);           // not found
		}
		
		String name, status;
		JSONObject jsonObject=null;
		
		try {
		  jsonObject=new JSONObject(content);
		}
		catch (JSONException jsone) {
			throw new LBaaSException("Submitted JSON Exception : " + jsone.toString(), 400);  //  bad request
		}
				
		try {
		   name = (String) jsonObject.get("name");
		   device.setName(name);
		   logger.info("name = " + name);
		   if ( deviceModel.existsName(name)) {
			   throw new LBaaSException("device name already exists", 400);  //  bad request
		   }
		}
		catch (JSONException e) {
			name =null;
		}
		
		try {
			   status = (String) jsonObject.get("status");
			   device.setStatus(status);
			   logger.info("status = " + status);
		}
		catch (JSONException e) {
			status =null;
		}
		
		if ((name==null) && (status==null)) {
			throw new LBaaSException("missing 'name' and 'status' ", 400);  //  bad request
		}
		
		if (name !=null)
			device.setName(name);
		
		if (status != null)
			device.setStatus(status);
		
		deviceModel.setDevice(device);
										
	}

		
}
