package org.pem.lbaas.handlers.tenant;


import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pem.lbaas.Lbaas;
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
import org.pem.lbaas.persistency.NodeDataModel;
import org.pem.lbaas.persistency.NodeModelAccessException;
import org.pem.lbaas.security.KeystoneAuthFilter;

/**
 * LbaasHandler JAX-RS REST handler for Load Balancers
 * @author Peter Mellquist pemellquist@hp.com
 *
 */

@Path("/v1.1/loadbalancers")
public class LbaasHandler {
	private static Logger logger = Logger.getLogger(LbaasHandler.class);
	private static LoadBalancerDataModel lbModel = new LoadBalancerDataModel();
	private static DeviceDataModel deviceModel = new DeviceDataModel();
	private static NodeDataModel nodeModel = new NodeDataModel();
    private static LBaaSTaskManager lbaasTaskManager = new LBaaSTaskManager();
    private static long requestId=0;
    
    // actions and HPCS private JSON names
    public static String HPCS_ACTION         = "hpcs_action";
    public static String HPCS_REQUESTID      = "hpcs_requestid";
    public static String HPCS_RESPONSE       = "hpcs_response";
    public static String HPCS_DEVICE         = "hpcs_device";
    public static String HPCS_TENANTID       = "hpcs_tenantid";
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
    protected static String JSON_CONDITION   = "condition";
    protected static String JSON_TYPE        = "type";
    protected static String JSON_IPVER       = "ipVersion";
    protected static String JSON_IPVER4      = "IPV4";
    protected static String JSON_PUBLIC      = "public";
    protected static String JSON_VIPS        = "virtualIps";
    protected static String JSON_NODES       = "nodes";
    public    static String JSON_LBS         = "loadBalancers";
    
    // node info
    public static String    NODE_ONLINE      = "ONLINE";
    public static String    NODE_OFFLINE     = "OFFLINE";
    public static String    NODE_ENABLED     = "ENABLED";
    public static String    NODE_DISABLED    = "DISABLED";

    
	/**
	 * Convert a LoadBalancer to JSON format
	 * @param LoadBalancer
	 * @return JSON encoded LoadBalancer
	 * @throws JSONException
	 */
	protected String LbToJson(LoadBalancer lb) throws JSONException{
		
		JSONObject jsonResponseObject=new JSONObject();
		try {	
		   // base fields
		   jsonResponseObject.put(JSON_NAME,lb.getName());	
		   jsonResponseObject.put(JSON_ID,lb.getId().toString());
		   jsonResponseObject.put(JSON_PROTOCOL,lb.getProtocol());
		   jsonResponseObject.put(JSON_PORT, lb.getPort().toString());
		   jsonResponseObject.put(JSON_ALGORITHM,lb.getAlgorithm());
		   jsonResponseObject.put(JSON_STATUS,lb.getStatus());
		   jsonResponseObject.put(JSON_CREATED,lb.getCreated());
		   jsonResponseObject.put(JSON_UPDATED,lb.getUpdated());
		   
		   // vips
		   jsonResponseObject.put(JSON_VIPS, vipsToJSON(lb.getVirtualIps()));

		   // nodes
		   jsonResponseObject.put(JSON_NODES, nodesToJSON(lb.getNodes()));
		   		   
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
			   // base fields
			   jsonLb.put(JSON_NAME,lbs.get(x).getName());	
			   jsonLb.put(JSON_ID,lbs.get(x).getId().toString());
			   jsonLb.put(JSON_PROTOCOL,lbs.get(x).getProtocol());
			   jsonLb.put(JSON_PORT, lbs.get(x).getPort().toString());
			   jsonLb.put(JSON_ALGORITHM,lbs.get(x).getAlgorithm());
			   jsonLb.put(JSON_STATUS, lbs.get(x).getStatus());
			   jsonLb.put(JSON_CREATED,lbs.get(x).getCreated());
			   jsonLb.put(JSON_UPDATED,lbs.get(x).getUpdated());
			   jsonLb.put(HPCS_TENANTID,lbs.get(x).getTenantId());
			   

			   // vips
			   jsonLb.put(JSON_VIPS,vipsToJSON(lbs.get(x).getVirtualIps()));
	   
			   // nodes
			   jsonLb.put(JSON_NODES, nodesToJSON(lbs.get(x).getNodes()));
			   
			   jsonArray.put(jsonLb);
		   }
		   
		   // HPCS specific fields
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
	protected Nodes jsonToNodes(JSONObject jsonObject) throws JSONException, ProtocolPortException, ProtocolAddressException {
	   if ( jsonObject.has(JSON_NODES) ) {			   
		   Nodes nodes = new Nodes();
		   try {
			   JSONArray jsonNodesArray = (JSONArray) jsonObject.get(JSON_NODES);
			   for ( int x=0;x<jsonNodesArray.length();x++) {				   				   				   
				   Node node = new Node();
				   JSONObject jsonNode = jsonNodesArray.getJSONObject(x);
				   
				   // address
				   if (!jsonNode.has(JSON_ADDRESS))
					   throw new ProtocolAddressException("node definition requires address");				   				   				  
				   String address = (String) jsonNode.get(JSON_ADDRESS);				  				   
				   if ( ! ProtocolHandler.validateIPv4Address(address)) {
					   throw new ProtocolAddressException("not a valid IPV4 address : " + address + " for node definition");
				   }				   
				   node.setAddress(address);
				   
				   // port
				   if (!jsonNode.has(JSON_PORT))
					   throw new ProtocolAddressException("node definition requires port");				  				   
				   String strPort = (String) jsonNode.get(JSON_PORT);
				   int port = ProtocolHandler.toPort(strPort);
				   if(port <0) {
					   throw new ProtocolPortException("illegal port: " + strPort + " defined for node address: " + address);
				   }
				   node.setPort(Integer.valueOf(port));
				   
				   node.setWeight(0);                // currently not used				   
				   node.setStatus(NODE_ONLINE);	     // currently only support ONLINE state
				   node.setEnabled(true);            // currently only support ENABLED state
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
		   throw new JSONException("JSON 'nodes' not in request body");
	   }
	}
	
	/**
	 * Convert Nodes to a JSONArray
	 * @param nodes
	 * @return
	 * @throws JSONException
	 */
	protected JSONArray nodesToJSON( Nodes nodes) throws JSONException {		
       JSONArray jsonNodeArray = new JSONArray();
	   if (nodes != null) {
		   List<Node> nodeList = nodes.getNodes();
		   for ( int x=0;x<nodeList.size();x++) 
			   jsonNodeArray.put(nodeToJSON(nodeList.get(x)));		   		   		   
	   }
	   return jsonNodeArray;
	}
	
	/**
	 * Convert a Node to a JSON Object
	 * @param node
	 * @return
	 * @throws JSONException
	 */
	protected JSONObject nodeToJSON( Node node) throws JSONException {
	   JSONObject jsonNode=new JSONObject();
	   jsonNode.put(JSON_ADDRESS, node.getAddress());
	   jsonNode.put(JSON_ID,node.getId().toString()); 			   
	   jsonNode.put(JSON_PORT ,node.getPort().toString());
	   jsonNode.put(JSON_STATUS, node.getStatus());	   
       jsonNode.put(JSON_CONDITION, NODE_ENABLED);

	   return jsonNode;
	}
	
	/**
	 * return a node based on its id from a loadbalancer or null if not found
	 * @param lb
	 * @param nodeId
	 * @return
	 */
	protected Node getNodeInLb(LoadBalancer lb, long nodeId) {				
		Nodes nodes = lb.getNodes();
		List<Node> nodeList = nodes.getNodes();
		for (int x=0;x<nodeList.size();x++) {
			if ( nodeList.get(x).getId() == nodeId)
				return nodeList.get(x);
		}		
		return null;          // not found
	}
	
	/** 
	 * Convert VirtualIps to JSONArray
	 * @param vips
	 * @return
	 * @throws JSONException
	 */
	protected JSONArray vipsToJSON(VirtualIps vips ) throws JSONException {
	   JSONArray jsonVipArray = new JSONArray();		   
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
	   return jsonVipArray;
	}
	
	
	/**
	 * Convert a JSONObject to VirtualIps
	 * @param jsonObject
	 * @return
	 * @throws JSONException
	 */
	protected VirtualIps jsonToVips(JSONObject jsonObject) throws JSONException, ProtocolAddressException {		
	   if ( jsonObject.has(JSON_VIPS) ) {
		   VirtualIps virtualIps = new VirtualIps();
		   try {
			   JSONArray jsonVIPArray = (JSONArray) jsonObject.get(JSON_VIPS);
			   for ( int x=0;x<jsonVIPArray.length();x++) {
				   VirtualIp virtualIp = new VirtualIp();				   
				   JSONObject jsonVip = jsonVIPArray.getJSONObject(x);				   
				   String address = (String) jsonVip.get(JSON_ADDRESS);
				   if ( address.length()>LimitsHandler.LIMIT_MAX_ADDR_SIZE) {
					   throw new ProtocolAddressException("address too long : " + address + "for VIP definition");
				   }
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
	public String getAll(@Context HttpServletRequest request) {
		
		 if (!KeystoneAuthFilter.authenticated(request)) {
		    	throw new LBaaSException("Get /loadbalancers request cannot be authenticated", 401);  //  bad auth
		 }
		    
		logger.info("Get /loadbalancers " + KeystoneAuthFilter.toString(request));
		
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();		
		List<LoadBalancer> lbs=null;
		
		String tenantId = KeystoneAuthFilter.getTenantId(request);
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);  //  bad auth
		}
		
		
		// get all load balancers
		try {
		   lbs = lbModel.getLoadBalancers(LoadBalancerDataModel.SQL_TENANTID + "= \'" + tenantId + "\'");
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);                                   
	    }
		
		// format JSON response
		try {	
		   for (int x=0;x<lbs.size();x++) {
			   JSONObject jsonLb=new JSONObject();
			   jsonLb.put(JSON_NAME, lbs.get(x).getName());
			   jsonLb.put(JSON_ID, lbs.get(x).getId().toString());
			   jsonLb.put(JSON_PROTOCOL,lbs.get(x).getProtocol());
			   jsonLb.put(JSON_PORT,lbs.get(x).getPort().toString());
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
	public String getLb(@Context HttpServletRequest request, @PathParam("id") String id) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /loadbalancers/{id} request cannot be authenticated", 401);  //  bad auth
	    }
	    
	    logger.info("Get /loadbalancers/" + id + " " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
	    
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);  //  bad auth
		}
				
		LoadBalancer lb = null;
	
		// read LB
		Long lbId = new Long(id);
		try {
		   lb = lbModel.getLoadBalancer(lbId, tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + id + " not found for tenant :" + tenantId, 404);  
		}
		
		// return JSON formatted response
		try {
			return LbToJson(lb);
		}
		catch (JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
	}
	
	
	/**
	 * Update a specific LoadBalancer, only allows change of name or algorithm
	 * @param id of LoadBalancer
	 * @param content JSON put body
	 */
	@PUT
	@Path("/{id}")
	@Consumes("application/json")
	@Produces("application/json")
	public String updateLb(@Context HttpServletRequest request, @PathParam("id") String id, String content) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Put /loadbalancers/{id} request cannot be authenticated", 401);  //  bad auth
	    }
	    
	    logger.info("Put /loadbalancers/" + id + " " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
				
		LoadBalancer lb=null;
		
		// attempt to read lb to be updated
		Long lbId = new Long(id);		
		try {
		   lb = lbModel.getLoadBalancer(lbId,tenantId);
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);
	    }		
		if ( lb == null) {
			throw new LBaaSException("could not find id : " + id + " for tenant :" + tenantId, 404);    //  not found			
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
		   if ( name.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
	         	throw new LBaaSException("'name' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400); 
		   lb.setName(name);
		   logger.info("   name = " + name);
		}
		catch (JSONException e) {
			name =null;
		}
		
		// look for algorithm
		try {
			   algorithm = (String) jsonObject.get("algorithm");
			   if ( algorithm.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
		         	throw new LBaaSException("'algorithm' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400); 
			   if (! AlgorithmsHandler.exists(algorithm)) 
				   throw new LBaaSException("algorithm specified not supported : " + algorithm, 400); 	
			   lb.setAlgorithm(algorithm);
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
			throw new LBaaSException("internal server error exception :" + ie.toString(), 500);  //  internal error
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error exception :" + dme.toString(), 500);  //  internal error
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
	public void deleteLb(@Context HttpServletRequest request, @PathParam("id") String id) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Delete /loadbalancers/{id} request cannot be authenticated", 401);  //  bad auth
	    }
	    
	    logger.info("Delete /loadbalancers/" + id + " " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);  //  bad auth
		}
		
		// delete the LB, send worker delete, finally clear device when worker responds ( not here )				
		LoadBalancer lb=null;
		
		// need the device object from the LB to be deleted, so get it
		Long lbId = new Long(id);
		try {
		   lb = lbModel.getLoadBalancer(lbId, tenantId);
		}
		catch ( DeviceModelAccessException dme) {
	          throw new LBaaSException(dme.message, 500);
	    }		
		if ( lb == null) {
			throw new LBaaSException("could not find loadbalancer id : " + id + " for tenant :" + tenantId, 404);              //  not found	
		}
		
		// delete LB
		int deleteCount=0;
		try {
		   deleteCount = lbModel.deleteLoadBalancer(lbId, tenantId);    // delete the lb
		   deviceModel.markAsFree(lb.getDevice(),lbId);	      // delete the reference from the device
		}
		catch ( DeviceModelAccessException dme) {
	          throw new LBaaSException(dme.message, 500);
	    }		
		if (deleteCount==0) {
			throw new LBaaSException("could not find loadbalancer id on delete: " + id + " for tenant:"+ tenantId, 404);    //  not found	
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
	@Path("/{id}/nodes")
	@Produces("application/json")
	public String getLbNodes(@Context HttpServletRequest request,@PathParam("id") String id) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /loadbalancers/{id}/nodes request cannot be authenticated", 401);  //  bad auth
	    }
	    
	    logger.info("Get /loadbalancers/" + id + "/nodes " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);  //  bad auth
		}
				
		
		LoadBalancer lb = null;
		
		// read LB
		Long lbId = new Long(id);
		try {
		   lb = lbModel.getLoadBalancer(lbId,tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + id + " not found for tenant :" + tenantId, 404);  
		}
		
		// return JSON formatted response
		try {
			JSONObject nodes = new JSONObject();
			nodes.put(JSON_NODES, nodesToJSON(lb.getNodes()));
			return nodes.toString();
		}
		catch (JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
	}
	
	
	@GET
	@Path("/{id}/nodes/{nodeId}")
	@Produces("application/json")
	public String getLbNode(@Context HttpServletRequest request, @PathParam("id") String id, @PathParam("nodeId") String nodeId) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /loadbalancers/{id}/nodes/{nodeid} request cannot be authenticated", 401);  //  bad auth
	    }
	    
	    logger.info("Get /loadbalancers/" + id + "/nodes" + nodeId + " " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);  //  bad auth
		}
				
        LoadBalancer lb = null;
		
		// read LB
		Long lbId = new Long(id);
		try {
		   lb = lbModel.getLoadBalancer(lbId, tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + id + " not found for tenant :" + tenantId, 404);   
		}
				
		// find the node
		Long longNodeId = new Long(nodeId);
		Node node = getNodeInLb( lb, longNodeId);		
		if (node == null) {
			throw new LBaaSException("node id: " + nodeId + " not found", 404);  
		}
		
		// return JSON formatted response
		try {			
			return nodeToJSON(node).toString();
		}
		catch (JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
	}
	
	
	@POST
	@Path("/{lbid}/nodes")
	@Consumes("application/json")
	@Produces("application/json")
	public String addLbNodes(@Context HttpServletRequest request, @PathParam("lbid") String lbid, String content) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("POST /loadbalancers/{id}/nodes request cannot be authenticated", 401);  //  bad auth
	    }
		
		logger.info("POST loadbalancer nodes lbid: " + lbid );
        String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);  //  bad auth
		}
		
		LoadBalancer lb = null;
		
		// read LB and check total # of nodes present
		Long lbId = new Long(lbid);
		try {
		   lb = lbModel.getLoadBalancer(lbId,tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);  
		}
		
		int currentNodeSize = lb.getNodes().getNodes().size();
						
	    //  extract nodes from content, check for errors and over limits
		Nodes nodes = null;
		try {
		   JSONObject jsonObject=new JSONObject(content);	
		   nodes = jsonToNodes(jsonObject);
		   if ( (nodes.getNodes().size() + currentNodeSize) > Lbaas.lbaasConfig.maxNodesperLb)  {
			   logger.warn("attempt to create update an LB with more than " + Lbaas.lbaasConfig.maxNodesperLb + " nodes");
	           throw new LBaaSException( "attempt to update an LB with more than " + Lbaas.lbaasConfig.maxNodesperLb + " nodes. LB already has " + currentNodeSize + " nodes", 413); 
		   }
			  		   
		}         
		catch ( JSONException jsone) {
		   throw new LBaaSException( jsone.toString(), 400);  
        } 
		catch ( ProtocolPortException ppe) {
		   throw new LBaaSException( ppe.message, 400);  
        } 	
		catch ( ProtocolAddressException pae) {
		   throw new LBaaSException( pae.message, 400);  
        } 
		
		// update Node model with new nodes
		try {
		   nodeModel.createNodes(nodes, lbId.longValue());
		   }
		catch (NodeModelAccessException nme) {
			throw new LBaaSException(nme.getMessage(),500);
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
			throw new LBaaSException("internal server error exception :" + ie.toString(), 500);  //  internal error
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error exception :" + dme.toString(), 500);  //  internal error
		}
		
		return content;
		
	}
	
	
	@PUT
	@Path("/{loadbalancerId}/nodes/{nodeId}")
	@Consumes("application/json")
	@Produces("application/json")
	public String modifyLbNode(@Context HttpServletRequest request, @PathParam("loadbalancerId") String loadbalancerId, @PathParam("nodeId") String nodeId) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /loadbalancers/{id}/nodes/{nodeid} request cannot be authenticated", 401);  //  bad auth
	    }
		
		logger.info("PUT loadbalancer node : " + loadbalancerId + ":" + nodeId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
	
	// delete a node
	
		
	
    /**
     * create a new LoadBalancer
     * @param content, JSON encoded LoadBalancer for creation
     * @return JSON encoded LoadBalancer which was created including new id
     */
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public String post(@Context HttpServletRequest request, String content) {
		
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Post /loadbalancers request cannot be authenticated", 401);  //  bad auth
	    }
	    
	    logger.info("Post /loadbalancers " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);  //  bad auth
		}
		
		// check if tenant has not exceeded their limit
		try {
			List<LoadBalancer> currentLbs = lbModel.getLoadBalancers(LoadBalancerDataModel.SQL_TENANTID + "= \'" + tenantId + "\'");
			if (currentLbs.size() > Lbaas.lbaasConfig.maxLbs) {
				throw new LBaaSException( "maximum number of allowed loadbalancers for this tenant has been reached : " +  Lbaas.lbaasConfig.maxLbs, 413); 
			}
		}
		catch ( DeviceModelAccessException dme) {
		    throw new LBaaSException(dme.message, 500);                                   
		}
		
		
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
		   
		   // if port is specified should match what protocol supports
		   if (jsonObject.has(JSON_PORT) ) {
			   String strPort = (String) jsonObject.get(JSON_PORT);			   
			   int port = ProtocolHandler.toPort(strPort);
			   if (port <0 ) {
				   throw new LBaaSException("illegal port value specified for load balancer : " + strPort, 400); 
			   }
			   
			   if ( !ProtocolHandler.supports( lb.getProtocol(), port )) {
				   throw new LBaaSException("port number :" + port + " is not supported for protocol : " + lb.getProtocol() + " see /protocols for supported protocold and ports", 400); 
			   }
			   lb.setPort(port);
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
			   Nodes nodes = jsonToNodes(jsonObject);
			   if (nodes.getNodes().size() > Lbaas.lbaasConfig.maxNodesperLb) {
				   logger.warn("attempt to create an LB with more than " + Lbaas.lbaasConfig.maxNodesperLb + " nodes");
				   throw new LBaaSException( "attempt to create an LB with more than " + Lbaas.lbaasConfig.maxNodesperLb + " nodes", 413); 
			   }
			   lb.setNodes(nodes);			   
		   }
		   catch ( JSONException jsone) {
				throw new LBaaSException( jsone.toString(), 400);  
		   } 
		   catch ( ProtocolPortException ppe) {
				throw new LBaaSException( ppe.message, 400);  
		   } 	
		   catch ( ProtocolAddressException pae) {
				throw new LBaaSException( pae.message, 400);  
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
		   catch ( ProtocolAddressException pae) {
				throw new LBaaSException( pae.message, 400);  
		   } 
		   
		   
		   
		   if ( virtualIps != null) {
			    
			   // check that only one vip can be specified for now
			   List<VirtualIp> vipList = virtualIps.getVirtualIps();
			   if ( vipList.size() > Lbaas.lbaasConfig.maxVipsPerLb) {
				   throw new LBaaSException("maximum number of VIPs allowed is " + Lbaas.lbaasConfig.maxVipsPerLb , 400);    //  not available
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
				   LoadBalancer existingLb = lbModel.getLoadBalancer(device.lbIds.get(z),tenantId);
				   
				   if (existingLb.getProtocol().equalsIgnoreCase(lb.getProtocol())) {
					   throw new LBaaSException("VIP already has loadbalancer with this protoocol for tenant "+ tenantId , 400);    //  not available
				   }
				   
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
		   
		   // tenant who created this
		   lb.setTenantId(tenantId);
		   
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
		   lbResponse = lbModel.getLoadBalancer(lbId,tenantId);
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
