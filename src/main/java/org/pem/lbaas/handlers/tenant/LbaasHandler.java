package org.pem.lbaas.handlers.tenant;


import java.util.ArrayList;
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
import org.pem.lbaas.datamodel.IpVersion;
import org.pem.lbaas.datamodel.LoadBalancer;
import org.pem.lbaas.datamodel.Node;
import org.pem.lbaas.datamodel.Nodes;
import org.pem.lbaas.datamodel.VipType;
import org.pem.lbaas.datamodel.VirtualIp;
import org.pem.lbaas.datamodel.VirtualIps;
import org.pem.lbaas.messaging.LBaaSTaskManager;
import org.pem.lbaas.persistency.DeviceDataModel;
import org.pem.lbaas.persistency.DeviceModelAccessException;
import org.pem.lbaas.persistency.LoadBalancerDataModel;

/**
 * LbaasHandler JAX-RS REST handler for Load Balancers
 * @author Peter Mellquist pemellquist@hp.com
 *
 */

@Path("/loadbalancers")
public class LbaasHandler {
	private static Logger logger = Logger.getLogger(LbaasHandler.class);
	private static LoadBalancerDataModel lbModel = new LoadBalancerDataModel();
	private static DeviceDataModel deviceModel = new DeviceDataModel();
    private static LBaaSTaskManager lbaasTaskManager = new LBaaSTaskManager();
    private static long requestId=0;
    
    // actions and HPCS private JSON names
    public static String HPCS_ACTION         = "hpcs_action";
    public static String HPCS_REQUESTID      = "hpcs_requestid";
    public static String HPCS_RESPONSE       = "hpcs_response";
    public static String HPCS_DEVICE         = "hpcs_device";
    public static String HPCS_RESPONSE_PASS  = "PASS";
    public static String HPCS_RESPONSE_FAIL  = "FAIL";
    public static String ACTION_UPDATE       = "UPDATE";
    public static String ACTION_SUSPEND      = "SUSPEND";
    public static String ACTION_ENABLE       = "ENABLE";
    public static String ACTION_DELETE       = "DELETE";
    
    // JSON names
    public    static String JSON_NAME        = "name";
    public    static String JSON_ID          = "id";	
    protected static String JSON_PROTOCOL    = "protocol";
    protected static String JSON_PORT        = "port";
    protected static String JSON_ALGORITHM   = "algorithm";
    protected static String JSON_STATUS      = "status";
    protected static String JSON_CREATED     = "created";
    protected static String JSON_UPDATED     = "updated";
    protected static String JSON_ADDRESS     = "address";
    protected static String JSON_TYPE        = "type";
    protected static String JSON_IPVER       = "ipVersion";
    protected static String JSON_IPVER4      = "IPV4";
    protected static String JSON_PUBLIC      = "public";
    protected static String JSON_VIPS        = "virtualIps";
    protected static String JSON_NODES       = "nodes";
    public    static String JSON_LBS         = "loadbalancers";
    
    // node info
    public static String    NODE_ONLINE      = "online";

    
	/**
	 * Convert a LoadBalancer to JSON format
	 * @param LoadBalancer
	 * @return JSON encoded LoadBalancer
	 * @throws JSONException
	 */
	protected String LbToJson(LoadBalancer lb) throws JSONException{
		
		JSONObject jsonResponseObject=new JSONObject();
		try {	
		   jsonResponseObject.put(JSON_NAME,lb.getName());	
		   jsonResponseObject.put(JSON_ID,lb.getId());
		   jsonResponseObject.put(JSON_PROTOCOL,lb.getProtocol());
		   jsonResponseObject.put(JSON_PORT, lb.getPort());
		   jsonResponseObject.put(JSON_ALGORITHM,lb.getAlgorithm());
		   jsonResponseObject.put(JSON_STATUS,lb.getStatus());
		   jsonResponseObject.put(JSON_CREATED,lb.getCreated());
		   jsonResponseObject.put(JSON_UPDATED,lb.getUpdated());
		   
		   // vips
		   JSONArray jsonVipArray = new JSONArray();		   
		   VirtualIps vips = lb.getVirtualIps();
		   if ( vips != null) {
			   List<VirtualIp> vipslist = vips.getVirtualIps();
			   if ( vipslist!=null)
				   for ( int x=0;x<vipslist.size();x++) {
					   JSONObject jsonVIP=new JSONObject();
					   jsonVIP.put(JSON_ADDRESS, vipslist.get(x).getAddress());
					   jsonVIP.put(JSON_ID, vipslist.get(x).getId());
					   jsonVIP.put(JSON_TYPE, vipslist.get(x).getType());
					   jsonVIP.put(JSON_IPVER, vipslist.get(x).getIpVersion());
					   jsonVipArray.put(jsonVIP);
				   }
		   }
		   jsonResponseObject.put(JSON_VIPS, jsonVipArray);
		   
		   // nodes
		   JSONArray jsonNodeArray = new JSONArray();
		   Nodes nodes = lb.getNodes();
		   if (nodes != null) {
			   List<Node> nodeList = nodes.getNodes();
			   for ( int y=0;y<nodeList.size();y++) {
				   JSONObject jsonNode=new JSONObject();
				   jsonNode.put(JSON_ADDRESS, nodeList.get(y).getAddress());
				   jsonNode.put(JSON_ID,nodeList.get(y).getId()); 			   
				   jsonNode.put(JSON_PORT ,nodeList.get(y).getPort());
				   jsonNode.put(JSON_STATUS, nodeList.get(y).getStatus());
				   jsonNodeArray.put(jsonNode);
			   }		   		   
		   }		   
		   jsonResponseObject.put(JSON_NODES, jsonNodeArray);
		   		   
		   return jsonResponseObject.toString();
		}
		catch ( JSONException jsone) {
			throw jsone;
		}
	}
	
	
	
    /**
     * Create a JSON array of LoadBalancers to be sent to gearman worker
     * @param lbs
     * @param action
     * @return JSON encoded array
     * @throws JSONException
     */
	protected String LbToJsonArray(List<LoadBalancer> lbs, String action) throws JSONException {
		
		JSONObject jsonObject=new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		try {
		   for (int x=0;x<lbs.size();x++) {
			   JSONObject jsonLb=new JSONObject();
			   jsonLb.put(JSON_NAME,lbs.get(x).getName());	
			   jsonLb.put(JSON_ID,lbs.get(x).getId());
			   jsonLb.put(JSON_PROTOCOL,lbs.get(x).getProtocol());
			   jsonLb.put(JSON_PORT, lbs.get(x).getPort());
			   jsonLb.put(JSON_ALGORITHM,lbs.get(x).getAlgorithm());
			   jsonLb.put(JSON_STATUS, lbs.get(x).getStatus());
			   jsonLb.put(JSON_CREATED,lbs.get(x).getCreated());
			   jsonLb.put(JSON_UPDATED,lbs.get(x).getUpdated());
			   
			   JSONArray jsonVipArray = new JSONArray();		   
			   VirtualIps vips = lbs.get(x).getVirtualIps();
			   if ( vips != null) {
				   List<VirtualIp> vipslist = vips.getVirtualIps();
				   if ( vipslist!=null)
					   for ( int y=0;y<vipslist.size();y++) {
						   JSONObject jsonVIP=new JSONObject();
						   jsonVIP.put(JSON_ADDRESS, vipslist.get(y).getAddress());
						   jsonVIP.put(JSON_ID, vipslist.get(y).getId());
						   jsonVIP.put(JSON_TYPE, vipslist.get(y).getType());
						   jsonVIP.put(JSON_IPVER, vipslist.get(y).getIpVersion());
						   jsonVipArray.put(jsonVIP);
					   }
			   }
			   jsonLb.put(JSON_VIPS, jsonVipArray);
			   			
			   JSONArray jsonNodeArray = new JSONArray();
			   Nodes nodes = lbs.get(x).getNodes();
			   if (nodes != null) {
				   List<Node> nodeList = nodes.getNodes();
				   for ( int y=0;y<nodeList.size();y++) {
					   JSONObject jsonNode=new JSONObject();
					   jsonNode.put(JSON_ADDRESS, nodeList.get(y).getAddress());
					   jsonNode.put(JSON_ID,nodeList.get(y).getId()); 			   
					   jsonNode.put(JSON_PORT ,nodeList.get(y).getPort());
					   jsonNode.put(JSON_STATUS, nodeList.get(y).getStatus());
					   jsonNodeArray.put(jsonNode);
				   }		   		   
			   }		   
			   jsonLb.put(JSON_NODES, jsonNodeArray);
			   
			   jsonArray.put(jsonLb);
		   }
		   jsonObject.put(HPCS_REQUESTID,++requestId);
		   jsonObject.put(HPCS_ACTION, action);
		   jsonObject.put(HPCS_DEVICE,lbs.get(0).getDevice()); 
		   jsonObject.put(JSON_LBS,jsonArray);		   				   	   		   
		   return jsonObject.toString();
		}
		catch ( JSONException jsone) {
			throw jsone;
		}
		
	}		
	
	
	/**
	 * Convert a JSONObject to Nodes
	 * @param jsonNodesArray
	 * @return Nodes
	 */
	protected Nodes jsonToNodes(JSONObject jsonObject) throws JSONException {
	   if ( jsonObject.has(JSON_NODES) ) {			   
		   Nodes nodes = new Nodes();
		   try {
			   JSONArray jsonNodesArray = (JSONArray) jsonObject.get(JSON_NODES);
			   for ( int x=0;x<jsonNodesArray.length();x++) {
				   Node node = new Node();
				   JSONObject jsonNode = jsonNodesArray.getJSONObject(x);
				   String address = (String) jsonNode.get(JSON_ADDRESS);
				   node.setAddress(address);
				   String port = (String) jsonNode.get(JSON_PORT);
				   node.setPort(Integer.valueOf(port));
				   node.setStatus(NODE_ONLINE);			   
				   node.setId(new Integer(x+1));
				   nodes.getNodes().add(node);			   
			   }
			   return nodes;
		   }
		   catch(JSONException jse) {
			   logger.warn(jse.toString());
			   throw jse;
		   }			   
	   }
	   else {
		   throw new LBaaSException("JSON 'nodes' not in request body", 400);
	   }
	}
	
	/**
	 * Convert a JSONObject to VirtualIps
	 * @param jsonObject
	 * @return
	 * @throws JSONException
	 */
	protected VirtualIps jsonToVips(JSONObject jsonObject) throws JSONException {		
	   if ( jsonObject.has(JSON_VIPS) ) {
		   VirtualIps virtualIps = new VirtualIps();
		   try {
			   JSONArray jsonVIPArray = (JSONArray) jsonObject.get(JSON_VIPS);
			   for ( int x=0;x<jsonVIPArray.length();x++) {
				   VirtualIp virtualIp = new VirtualIp();				   
				   JSONObject jsonVip = jsonVIPArray.getJSONObject(x);				   
				   String address = (String) jsonVip.get(JSON_ADDRESS);
				   virtualIp.setAddress(address);				   
				   if (jsonVip.get(JSON_IPVER).toString().equalsIgnoreCase(JSON_IPVER4))	
				      virtualIp.setIpVersion(IpVersion.IPV_4);
				   else
					   virtualIp.setIpVersion(IpVersion.IPV_6);				   
				   if ( jsonVip.get(JSON_TYPE).toString().equalsIgnoreCase(JSON_PUBLIC))
				      virtualIp.setType(VipType.PUBLIC);
				   else
					   virtualIp.setType(VipType.PRIVATE);				   
				   virtualIp.setId(new Integer(x+1));
				   virtualIps.getVirtualIps().add(virtualIp);
			   }
		       return virtualIps;
		   }
		   catch(JSONException jse) {
			   logger.warn(jse.toString());
			   throw jse;
		   }
	   }
	   else {
		   return null;
	   }
	}
	
	/**
	 * Create VirtualIps from a Device
	 * @param device
	 * @return VirtualIps
	 */
	protected VirtualIps deviceToVips( Device device) {
		 VirtualIps virtualIps = new VirtualIps();
		 VirtualIp virtualIp = new VirtualIp();
		 virtualIp.setAddress(device.getAddress());
		 virtualIp.setIpVersion(IpVersion.IPV_4);
		 virtualIp.setType(VipType.PUBLIC);
		 virtualIp.setId(new Integer(1));
		 virtualIps.getVirtualIps().add(virtualIp);
		 return virtualIps;
	}
		
	
	
    /**
     * Get all Loadbalancers
     * @return JSON encoded list of LoadBalancers in abbreviated format per Atlas specification
     */
	@GET
	@Produces("application/json")
	public String getAll() {
		logger.info("GET loadbalancers");
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();		
		List<LoadBalancer> lbs=null;
		
		try {
		   lbs = lbModel.getLoadBalancers(null);
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);                                   
	    }
		
		try {	
		   for (int x=0;x<lbs.size();x++) {
			   JSONObject jsonLb=new JSONObject();
			   jsonLb.put(JSON_NAME, lbs.get(x).getName());
			   jsonLb.put(JSON_ID, lbs.get(x).getId());
			   jsonLb.put(JSON_PROTOCOL,lbs.get(x).getProtocol());
			   jsonLb.put(JSON_PORT,lbs.get(x).getPort());
			   jsonLb.put(JSON_ALGORITHM, lbs.get(x).getAlgorithm());
			   jsonLb.put(JSON_STATUS,lbs.get(x).getStatus());
			   jsonLb.put(JSON_CREATED, lbs.get(x).getCreated());
			   jsonLb.put(JSON_UPDATED, lbs.get(x).getUpdated());
			   jsonArray.put(jsonLb);
		   }
			
		   jsonObject.put(JSON_LBS,jsonArray);		   				   	   		   
		   return jsonObject.toString();
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		}
	}
	
	
	
	/**
	 * Get a specific LoadBalancer base on its id
	 * @param id
	 * @return
	 */
	@GET
	@Path("/{id}")
	@Produces("application/json")
	public String getLb(@PathParam("id") String id) 
	{
		logger.info("GET loadbalancer : " + id);
		LoadBalancer lb = null;
	
		Long lbId = new Long(id);
		try {
		   lb = lbModel.getLoadBalancer(lbId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + id + " not found", 404);  
		}
		
		try {
			return LbToJson(lb);
		}
		catch (JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
	}
	
	
	/**
	 * Update a specific LoadBalancer
	 * @param id of LoadBalancer
	 * @param content JSON put body
	 */
	@PUT
	@Path("/{id}")
	@Consumes("application/json")
	@Produces("application/json")
	public String updateLb(@PathParam("id") String id, String content) 
	{
		logger.info("PUT loadbalancer : " + id);
		LoadBalancer lb=null;
		
		// attempt to read lb to be updated
		Long lbId = new Long(id);		
		try {
		   lb = lbModel.getLoadBalancer(lbId);
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);
	    }		
		if ( lb == null) {
			throw new LBaaSException("could not find id : " + id, 404);    //  not found			
		}
		
		String name, algorithm;
		JSONObject jsonObject=null;
		
		// decode JSON
		try {
		  jsonObject=new JSONObject(content);
		}
		catch (JSONException e) {
			throw new LBaaSException("bad json request", 400);    //  bad request	
		}
			
		// look for name
		try {
		   name = (String) jsonObject.get("name");
		   lb.setName(name);
		   logger.info("   name = " + name);
		}
		catch (JSONException e) {
			name =null;
		}
		
		// look for algorithm
		try {
			   algorithm = (String) jsonObject.get("algorithm");
			   lb.setName(name);
			   logger.info("   algorithm = " + algorithm);
		}
		catch (JSONException e) {
			algorithm =null;
		}
		
		// must have one of these fields
		if ((name==null) && (algorithm==null)) {
			throw new LBaaSException("name or algorithm must be specified", 400);    //  bad request				
		}
				
		if (name !=null)
			lb.setName(name);
		
		if (algorithm != null)
			lb.setAlgorithm(algorithm);
		
		// mark as change pending
		lb.setStatus(LoadBalancer.STATUS_PENDING_UPDATE);		
		
		// write changes to DB
		try {
			lbModel.setLoadBalancer(lb);
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);
	    }
		
		// have the device process the job 
		try {
		   List<LoadBalancer> lbs = lbModel.getLoadBalancersWithDevice(lb.getDevice());
		   lbaasTaskManager.sendJob( new Long(lb.getDevice()), LbToJsonArray(lbs, ACTION_UPDATE ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error JSON exception :" + ie.toString(), 500);  //  internal error
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error JSON exception :" + dme.toString(), 500);  //  internal error
		}
								
		//respond with JSON
		try {
		   return LbToJson(lb);
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
	}
	
	
	/**
	 * Delete a LoadBalancer
	 * @param id
	 */
	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	public void deleteLb(@PathParam("id") String id) 
	{
		// delete the LB, send worker delete, finally clear device when worker responds ( not here )		
		logger.info("DELETE loadbalancer : " + id);
		LoadBalancer lb=null;
		
		Long lbId = new Long(id);
		try {
		   lb = lbModel.getLoadBalancer(lbId);
		}
		catch ( DeviceModelAccessException dme) {
	          throw new LBaaSException(dme.message, 500);
	    }		
		if ( lb == null) {
			throw new LBaaSException("could not find loadbalancer id : " + id, 404);              //  not found	
		}
		
		int deleteCount=0;
		try {
		   deleteCount = lbModel.deleteLoadBalancer(lbId);
		}
		catch ( DeviceModelAccessException dme) {
	          throw new LBaaSException(dme.message, 500);
	    }
		
		if (deleteCount==0) {
			throw new LBaaSException("could not find loadbalancer id on delete: " + id, 404);    //  not found	
		}
		
		// have the device process the job 
		// if there are remaining LBs on this device, send it as an Update with the remaining LB
		// if there are no more LBs on this device, send it as a Delete with the deleted device
		try {
		   List<LoadBalancer> lbs = lbModel.getLoadBalancersWithDevice(lb.getDevice());
		   String action=null;
		   if ( lbs.size() == 0) {
			   action = ACTION_DELETE;
			   lbs.clear();
			   lbs.add(lb);      // the deleted lb
		   }
		   else
			   action = ACTION_UPDATE;
		   lbaasTaskManager.sendJob( new Long(lb.getDevice()), LbToJsonArray(lbs, action ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error JSON exception :" + ie.toString(), 500);  //  internal error
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error JSON exception :" + dme.toString(), 500);  //  internal error
		}
	}
	
	
	
	@GET
	@Path("/{loadbalancerId}/nodes")
	@Produces("application/json")
	public String getLbNodes(@PathParam("loadbalancerId") String loadbalancerId) 
	{
		logger.info("GET loadbalancer nodes : " + loadbalancerId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
	
	@GET
	@Path("/{loadbalancerId}/nodes/{nodeId}")
	@Produces("application/json")
	public String getLbNode(@PathParam("loadbalancerId") String loadbalancerId, @PathParam("nodeId") String nodeId) 
	{
		logger.info("GET loadbalancer node : " + loadbalancerId + ":" + nodeId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
	
	@POST
	@Path("/{loadbalancerId}/nodes")
	@Consumes("application/json")
	@Produces("application/json")
	public String addLbNodes(@PathParam("loadbalancerId") String loadbalancerId) 
	{
		logger.info("POST loadbalancer nodes : " + loadbalancerId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
	
	@PUT
	@Path("/{loadbalancerId}/nodes/{nodeId}")
	@Consumes("application/json")
	@Produces("application/json")
	public String modifyLbNode(@PathParam("loadbalancerId") String loadbalancerId, @PathParam("nodeId") String nodeId) 
	{
		logger.info("PUT loadbalancer node : " + loadbalancerId + ":" + nodeId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
		
	
    /**
     * create a new LoadBalancer
     * @param content, JSON encoded LoadBalancer for creation
     * @return JSON encoded LoadBalancer which was created including new id
     */
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public String post(String content) {
		logger.info("POST loadbalancers");
		
		Device device=null;
		List<LoadBalancer> lbs = new  ArrayList<LoadBalancer>();
		
		// process POST'ed body
		LoadBalancer lb = new LoadBalancer();
		Long lbId=new Long(0);
		try {
		   JSONObject jsonObject=new JSONObject(content);
		   
		   // name
	       if ( jsonObject.has(JSON_NAME)) {		   
		      String name = (String) jsonObject.get(JSON_NAME);
		      lb.setName(name);
		      logger.info("   name = " + name);
	       }
	       else {
	            throw new LBaaSException("POST requires 'name' in request body", 400);   
	       }
		   		   
		   
		   // check to ensure protocol is supported
		   if (jsonObject.has(JSON_PROTOCOL) ) {
			   String protocol = (String) jsonObject.get(JSON_PROTOCOL);
			   if (! ProtocolHandler.exists(protocol)) {
				   throw new LBaaSException("protocol specified not supported : " + protocol, 400); 					   
			   }
			   else {			   
				   lb.setProtocol(protocol);
				   lb.setPort(ProtocolHandler.getPort(protocol));				   
			   }
		   }
		   else {
			   lb.setProtocol(ProtocolHandler.DEFAULT_PROTOCOL);
			   lb.setPort(ProtocolHandler.DEFAULT_PORT);
		   }
		   logger.info("   protocol = " + lb.getProtocol());
		   logger.info("   port     = " + lb.getPort());
		   
		   // check to see if algorithm us supported
		   if (jsonObject.has(JSON_ALGORITHM) ) {
			   String algorithm = (String) jsonObject.get(JSON_ALGORITHM);
			   if (! AlgorithmsHandler.exists(algorithm)) {
				   throw new LBaaSException("algorithm specified not supported : " + algorithm, 400); 					   
			   }
			   else {			   
				   lb.setAlgorithm(algorithm);				   
			   }
		   }
		   else {
			   lb.setAlgorithm(AlgorithmsHandler.DEFAULT_ALGO);
		   }
		   logger.info("   algorithm = " + lb.getAlgorithm());
		   
		   
		   
		   //  nodes
		   try {
			   lb.setNodes(jsonToNodes(jsonObject));			   
		   }
		   catch ( JSONException jsone) {
				throw new LBaaSException( jsone.toString(), 400);  
		   } 
		   		   		   		   
		   
		   // vips
		   // vips control if an existing device is to be used or a new device is to be used
		   // no vip = new device
		   // yes vip = find and use but only for a new protocol
		   
		   VirtualIps virtualIps = null;
		   try {
		      virtualIps = jsonToVips(jsonObject);		      		      		      
		   }
		   catch ( JSONException jsone) {
				throw new LBaaSException( jsone.toString(), 400);  
		   } 
		   if ( virtualIps != null) {
			    
			   // check that only one vip can be specified for now
			   List<VirtualIp> vipList = virtualIps.getVirtualIps();
			   if ( vipList.size()!=1) {
				   throw new LBaaSException("only one VIP can be specified" , 400);    //  not available
			   }			   
			   // check that vip is the same address as existing device and this is a new protocol not an existing one
			   String address = vipList.get(0).getAddress();
			   List<Device> devices = deviceModel.getDevicesWithAddr(address);
			   if (devices.size()==0) {
				   throw new LBaaSException("VIP specified does not exist" , 400);    //  not available
			   }
			   
			    
			   // use this device
			   device = devices.get(0);
			   
			   // set the vip into the newly created lb
			   lb.setVirtualIps(virtualIps);
			   
			    
			   // get the other LBs used by this device to submit with the request
			   //device.lbIds
			   for (int z=0;z<device.lbIds.size();z++) {
				   LoadBalancer existingLb = lbModel.getLoadBalancer(device.lbIds.get(z));
				   lbs.add(existingLb);
			   }
			   
			    
			   lbs.add(lb);     // add this LB as as well			   
		   }
		   else {
			   // find free device to use
			   try {
			      device = deviceModel.findFreeDevice();
			   }
			   catch (DeviceModelAccessException dme) {
		             throw new LBaaSException(dme.message, 500);
	           }
			   		   
			   if ( device == null) {
				   throw new LBaaSException("cannot find free device available" , 503);    //  not available
			   }
			   
			   logger.info("found free device at id : " + device.getId().toString());
			   
			   virtualIps = deviceToVips( device);
			   lb.setVirtualIps(virtualIps);
			   lbs.add(lb);     // only a single LB in this update
		   }
		   		   		   		   
		     	   
		   // mark lb as using found device
		   lb.setDevice( new Long(device.getId()));              
		   
		   // write it to datamodel
		   lbId = lbModel.createLoadBalancer(lb);	       	   	
		   
		   	
		   
		   // set device lb and write it back to data model
		   device.lbIds.add(lbId);
		   try {
		      deviceModel.setDevice(device);
		   }
		   catch (DeviceModelAccessException dme) {
	             throw new LBaaSException(dme.message, 500);
         }
		   
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);
	      }
		catch (JSONException e) {
			return e.toString();
		}
		 							
		// read LB back from data model, it will now have valid id
		LoadBalancer lbResponse=null;
		try {
		   lbResponse = lbModel.getLoadBalancer(lbId);
		}
		catch (DeviceModelAccessException dme) {
            throw new LBaaSException(dme.message, 500);
        }
 		
		// have the device process the request
		try {           			   
		   lbaasTaskManager.sendJob( lbResponse.getDevice(), LbToJsonArray(lbs, ACTION_UPDATE ));		    	
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error JSON exception :" + ie.toString(), 500);  //  internal error
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error JSON exception :" + dme.toString(), 500);  //  internal error
		}
		
		//respond with JSON
		try {
		   return LbToJson(lbResponse);
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
		
	}	

	
}
