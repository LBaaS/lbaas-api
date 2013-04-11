package org.pem.lbaas.handlers.tenant;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.eclipse.jetty.util.log.Log;
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
import org.pem.lbaas.messaging.LBaaSArchiveRequest;
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
    private static Semaphore semaphore = new Semaphore(1);
    private static long requestId=0;
    
    // actions and HPCS private JSON names
    public static String HPCS_ACTION         = "hpcs_action";
    public static String HPCS_REQUESTID      = "hpcs_requestid";
    public static String HPCS_RESPONSE       = "hpcs_response";
    public static String HPCS_DEVICE         = "hpcs_device";
    public static String HPCS_TENANTID       = "hpcs_tenantid";
    public static String HPCS_ERROR          = "hpcs_error";
    public static String HPCS_OBJECT_STORE_TYPE     = "hpcs_object_store_type";
    public static String HPCS_OBJECT_STORE_ENDPOINT = "hpcs_object_store_endpoint";
    public static String HPCS_OBJECT_STORE_BASEPATH = "hpcs_object_store_basepath";
    public static String HPCS_OBJECT_STORE_TOKEN    = "hpcs_object_store_token";
    public static String HPCS_RESPONSE_PASS  = "PASS";
    public static String HPCS_RESPONSE_FAIL  = "FAIL";
    public static String ACTION_UPDATE       = "UPDATE";
    public static String ACTION_SUSPEND      = "SUSPEND";
    public static String ACTION_ENABLE       = "ENABLE";
    public static String ACTION_DELETE       = "DELETE";
    public static String ACTION_ARCHIVE      = "ARCHIVE";
    
    // JSON names
    public    static String JSON_NAME        = "name";
    public    static String JSON_ID          = "id";	
    protected static String JSON_PROTOCOL    = "protocol";
    protected static String JSON_PORT        = "port";
    protected static String JSON_ALGORITHM   = "algorithm";
    protected static String JSON_STATUS      = "status";
    protected static String JSON_STATUSDESCR = "statusDescription";
    protected static String JSON_CREATED     = "created";
    protected static String JSON_UPDATED     = "updated";
    protected static String JSON_ADDRESS     = "address";
    protected static String JSON_CONDITION   = "condition";
    protected static String JSON_WEIGHT      = "weight";
    protected static String JSON_TYPE        = "type";
    protected static String JSON_IPVER       = "ipVersion";
    protected static String JSON_IPVER4      = "IPV4";
    protected static String JSON_PUBLIC      = "public";
    protected static String JSON_VIPS        = "virtualIps";
    protected static String JSON_NODES       = "nodes";
    public    static String JSON_LBS         = "loadBalancers";
    public    static String JSON_AUTH_TOKEN  = "authToken";
    public    static String JSON_OBJ_ENDPNT  = "objectStoreEndpoint";
    public    static String JSON_OBJ_PATH    = "objectStoreBasePath";
    
    // node info
    public static String    NODE_ONLINE      = "ONLINE";
    public static String    NODE_OFFLINE     = "OFFLINE";
    public static String    NODE_ENABLED     = "ENABLED";
    public static String    NODE_DISABLED    = "DISABLED";
    
    // query parm
    public static String    PARAM_TENANTID   = "tenantid";

    
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
		   
		   if (lb.getErrorMsg()!=null)
			   jsonResponseObject.put(JSON_STATUSDESCR, lb.getErrorMsg());
		   
		   // vips
		   jsonResponseObject.put(JSON_VIPS, vipsToJSON(lb.getVirtualIps()));

		   // nodes
		   jsonResponseObject.put(JSON_NODES, nodesToJSON(lb.getNodes(),false));
		   		   
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
	protected String LbToJsonArray(List<LoadBalancer> lbs, LBaaSArchiveRequest lbaaSArchiveRequest, String action) throws JSONException {
		
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
			   
			   if (lbs.get(x).getErrorMsg()!=null)
				   jsonLb.put(JSON_STATUSDESCR, lbs.get(x).getErrorMsg());
			   
			   // vips
			   jsonLb.put(JSON_VIPS,vipsToJSON(lbs.get(x).getVirtualIps()));
	   
			   // nodes
			   jsonLb.put(JSON_NODES, nodesToJSON(lbs.get(x).getNodes(),true));
			   
			   jsonArray.put(jsonLb);
		   }
		   
		   // HPCS specific fields
		   jsonObject.put(HPCS_REQUESTID,++requestId);
		   jsonObject.put(HPCS_ACTION, action);
		   jsonObject.put(HPCS_DEVICE,lbs.get(0).getDevice()); 
		   jsonObject.put(JSON_LBS,jsonArray);		 
		   
		   // archive request if present
		   if (action.equalsIgnoreCase(ACTION_ARCHIVE)) {
			   
			   // trim trailing slash from endpoint if present
			   if (lbaaSArchiveRequest.objectStoreEndpoint.endsWith("/"))
				   lbaaSArchiveRequest.objectStoreEndpoint = lbaaSArchiveRequest.objectStoreEndpoint.substring(0, lbaaSArchiveRequest.objectStoreEndpoint.length()-1);
			   
			   // trim leading slash from basepath if present
			   if (lbaaSArchiveRequest.objectStoreBasePath.startsWith("/"))
				   lbaaSArchiveRequest.objectStoreBasePath = lbaaSArchiveRequest.objectStoreBasePath.substring(1, lbaaSArchiveRequest.objectStoreBasePath.length());
			   
			   if (lbaaSArchiveRequest != null) {
				   jsonObject.put(HPCS_OBJECT_STORE_TYPE, lbaaSArchiveRequest.objectStoreType);
				   jsonObject.put(HPCS_OBJECT_STORE_ENDPOINT,lbaaSArchiveRequest.objectStoreEndpoint);
				   jsonObject.put(HPCS_OBJECT_STORE_BASEPATH, lbaaSArchiveRequest.objectStoreBasePath);
				   jsonObject.put(HPCS_OBJECT_STORE_TOKEN, lbaaSArchiveRequest.objectStoreToken);
			   }
			   else
				   logger.error("archive request requires LBaaSArchiveRequest instance!");
		   }
		   
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
					   throw new ProtocolAddressException("node definition requires 'port'");				  				   
				   String strPort = (String) jsonNode.get(JSON_PORT);
				   int port = ProtocolHandler.toPort(strPort);
				   if(port <0) {
					   throw new ProtocolPortException("illegal port: " + strPort + " defined for node address: " + address);
				   }
				   node.setPort(Integer.valueOf(port));
				   
				   if (jsonNode.has(JSON_ID))
					   throw new JSONException("node 'id' may not be defined on creation");
				   
				   if (jsonNode.has(JSON_WEIGHT))
					   throw new JSONException("node 'weight' is not supported");
				   node.setWeight(0);                	
				   
				   if (jsonNode.has(JSON_STATUS))
					   throw new JSONException("node 'status' may not be defined on creation");	
				   node.setStatus(NODE_ONLINE);	     
				   
				   if (jsonNode.has(JSON_CONDITION))
					   throw new JSONException("node 'condition' may not be defined on creation");
				   node.setEnabled(true);            
				   
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
	protected JSONArray nodesToJSON( Nodes nodes, boolean hideDisabledNodes) throws JSONException {		
       JSONArray jsonNodeArray = new JSONArray();
	   if (nodes != null) {
		   List<Node> nodeList = nodes.getNodes();
		   for ( int x=0;x<nodeList.size();x++) 
			   if ( !hideDisabledNodes)
			     jsonNodeArray.put(nodeToJSON(nodeList.get(x)));
			   else
				   if ( nodeList.get(x).isEnabled())
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
	protected JSONObject nodeToJSON(Node node) throws JSONException {
	   JSONObject jsonNode=new JSONObject();
	   jsonNode.put(JSON_ADDRESS, node.getAddress());
	   jsonNode.put(JSON_ID,node.getId().toString()); 			   
	   jsonNode.put(JSON_PORT ,node.getPort().toString());
	   jsonNode.put(JSON_STATUS, node.getStatus());	   
       jsonNode.put(JSON_CONDITION, node.isEnabled()?NODE_ENABLED:NODE_DISABLED);

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
			   jsonVIP.put(JSON_ID, vipslist.get(y).getId().toString());
			   jsonVIP.put(JSON_TYPE, vipslist.get(y).getType());
			   jsonVIP.put(JSON_IPVER, vipslist.get(y).getIpVersion().value());
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
	protected VirtualIps jsonToVips(JSONObject jsonObject, String tenantId) throws JSONException,VipException {		
	   if ( jsonObject.has(JSON_VIPS) ) {
		   VirtualIps virtualIps = new VirtualIps();
		   try {
			   JSONArray jsonVIPArray = (JSONArray) jsonObject.get(JSON_VIPS);
			   
			   // read the vip id, find the device, ensure it is already used by the same tenant
			   // use the VIP info from the device ...
			   JSONObject jsonVip = jsonVIPArray.getJSONObject(0);
			   
			   if (!jsonVip.has(JSON_ID))
				   throw new VipException("VIP 'id' not specified");
			   
			   String id = jsonVip.getString(JSON_ID);
			   long longId=0;
			   try {
				   longId= Long.parseLong(id);
			         
			   } catch (NumberFormatException nfe) {
				   throw new VipException("VIP id : " + id + " is not a valid id");
			   }
			   
			   Device device=null;
			   try { 
			      device = deviceModel.getDevice(longId);
			   }
			   catch ( DeviceModelAccessException dme) {
				   throw new VipException("VIP id : " + id + " is not a valid id");
			   }
			   
			   if ( device == null)
				   throw new VipException("VIP id : " + id + " is not a valid id");
			   
			   // check that device is used by the same tenant
			   // VIPs cannot be shared across tenants
			   
			   // do not allow using an unassigned device
			   if (device.lbIds.size() == 0)
			      throw new VipException("VIP id : " + id + " is not a valid id");
			   
			   // get lb owner
			   long lbOwnerId = device.lbIds.get(0);
			   LoadBalancer lb = null;
			   
			   // try to read this LB
			   try {
                  lb = lbModel.getLoadBalancer(lbOwnerId, tenantId);
               }
			   catch ( DeviceModelAccessException dme) {
			      throw new LBaaSException(dme.message, 500);                                     
               }		
			   if (lb == null) {
			      throw new VipException("VIP id : " + id + " is not a valid id"); 
			   }
			   
			   
			   VirtualIp virtualIp = new VirtualIp();
			   virtualIp.setAddress(device.getAddress());
			   virtualIp.setIpVersion(IpVersion.IPV_4);
			   virtualIp.setType(VipType.PUBLIC);
			   virtualIp.setId(longId);
			   virtualIps.getVirtualIps().add(virtualIp);
			   
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
		 virtualIp.setId(device.getId());
		 virtualIps.getVirtualIps().add(virtualIp);
		 return virtualIps;
	}
	
	/**
	 * look for admin role and extract admin role tenantid query param which allow cross tenant access
	 * @param request
	 * @param info
	 * @param tenantId
	 * @return
	 */
	protected String getAdminTenantId( HttpServletRequest request, UriInfo info, String tenantId)
	{
	   String returnedTenantId = tenantId;
	   if ( KeystoneAuthFilter.isAdmin(request)) {
		   String adminTenantId = info.getQueryParameters().getFirst(PARAM_TENANTID);
		   if (adminTenantId != null) 	
			   returnedTenantId = adminTenantId;
		   logger.info("admin role tenantId: " + returnedTenantId);
	   }
	   return returnedTenantId;
	}
		
	
	
    /**
     * Get all Loadbalancers
     * @return JSON encoded list of LoadBalancers in abbreviated format per Atlas specification
     */
	@GET
	@Produces("application/json")
	public String getAll(@Context HttpServletRequest request, @Context UriInfo info) {
		
		 if (!KeystoneAuthFilter.authenticated(request)) {
		    	throw new LBaaSException("GET /loadbalancers request cannot be authenticated", 401);   
		 }		 		 
		    
		logger.info("GET /loadbalancers " + KeystoneAuthFilter.toString(request));
		
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();		
		List<LoadBalancer> lbs=null;
		
		String tenantId = KeystoneAuthFilter.getTenantId(request);
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
								
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
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		}
	}
	
	
	
	/**
	 * Get a specific LoadBalancer base on its id
	 * @param id
	 * @return
	 */
	@GET
	@Path("/{lbid}")
	@Produces("application/json")
	public String getLb(@Context HttpServletRequest request, @PathParam("lbid") String lbid, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /loadbalancers/{id} request cannot be authenticated", 401);   
	    }
	    
		logger.info("Get /loadbalancers/" + lbid + " " + KeystoneAuthFilter.toString(request));
		
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
	    
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
			    				
		LoadBalancer lb = null;
	
		// read LB
		long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalancer id : " + lbid + " is not a valid id",404);
		}
		   
		try {
		   lb = lbModel.getLoadBalancer(longId, tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);  
		}
		
		// return JSON formatted response
		try {
			return LbToJson(lb);
		}
		catch (JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
	}
	
	
	/**
	 * Update a specific LoadBalancer, only allows change of name or algorithm
	 * @param id of LoadBalancer
	 * @param content JSON put body
	 */
	@PUT
	@Path("/{lbid}")
	@Consumes("application/json")
	@Produces("application/json")
	public String updateLb(@Context HttpServletRequest request, @PathParam("lbid") String lbid, String content, @Context UriInfo info) 
	{		
		
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("PUT /loadbalancers/{id} request cannot be authenticated", 401);   
	    }
		
		logger.info("PUT /loadbalancers/" + lbid + " " + KeystoneAuthFilter.toString(request));
	    	    
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
	    
	    // check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);		
				
		LoadBalancer lb=null;
		
		// attempt to read lb to be updated
		long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalancer id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId,tenantId);
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);
	    }		
		if ( lb == null) {
			throw new LBaaSException("could not find loadbalancer id : " + lbid + " for tenant :" + tenantId, 404);     			
		}
		
		// PUTs not allowed while LB is in BUILD or PENDING-UPDATE state
		if ((lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_BUILD)) || (lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_PENDING_UPDATE))) {
			throw new LBaaSException("operation not allowed while load balancer status : " + lb.getStatus() + " for tenant :" + tenantId, 422);  
		}
		
		String name, algorithm;
		JSONObject jsonObject=null;
		
		// decode JSON
		try {
		  jsonObject=new JSONObject(content);
		}
		catch (JSONException e) {
			throw new LBaaSException("bad json request", 400);     	
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
			throw new LBaaSException("name or algorithm must be specified", 400);     				
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
		   lbaasTaskManager.sendJob( new Long(lb.getDevice()), LbToJsonArray(lbs, null, ACTION_UPDATE ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error exception :" + ie.toString(), 500);   
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error exception :" + dme.toString(), 500);   
		}
								
		//respond with JSON
		try {
		   return LbToJson(lb);
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
	}
	
	
	/**
	 * Delete a LoadBalancer
	 * @param id
	 */
	@DELETE
	@Path("/{lbid}")
	@Produces("application/json")
	public Response deleteLb(@Context HttpServletRequest request, @PathParam("lbid") String lbid, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("DELETE /loadbalancers/{id} request cannot be authenticated", 401);   
	    }
		
		logger.info("DELETE /loadbalancers/" + lbid + " " + KeystoneAuthFilter.toString(request));
	    	    
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
	    
	    // check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);	
						
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// delete the LB, send worker delete, finally clear device when worker responds ( not here )				
		LoadBalancer lb=null;
		
		// need the device object from the LB to be deleted, so get it
		long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalancer id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId, tenantId);
		}
		catch ( DeviceModelAccessException dme) {
	          throw new LBaaSException(dme.message, 500);
	    }		
		if ( lb == null) {
			throw new LBaaSException("could not find loadbalancer id : " + lbid + " for tenant :" + tenantId, 404);               	
		}
		
		
		// DELETEs not allowed while LB is in BUILD or PENDING-UPDATE state
		if ((lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_BUILD)) || (lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_PENDING_UPDATE))) {
			throw new LBaaSException("operation not allowed while load balancer status : " + lb.getStatus() + " for tenant :" + tenantId, 422);  
		}
		
		// delete LB
		int deleteCount=0;
		try {
		   deleteCount = lbModel.deleteLoadBalancer(longId, tenantId);    // delete the lb
		   deviceModel.markAsFree(lb.getDevice(),longId);	      // delete the reference from the device
		}
		catch ( DeviceModelAccessException dme) {
	          throw new LBaaSException(dme.message, 500);
	    }		
		if (deleteCount==0) {
			throw new LBaaSException("could not find loadbalancer id on delete: " + lbid + " for tenant:"+ tenantId, 404);     	
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
		   lbaasTaskManager.sendJob( new Long(lb.getDevice()), LbToJsonArray(lbs, null, action ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error JSON exception :" + ie.toString(), 500);   
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error JSON exception :" + dme.toString(), 500);   
		}
		
		return Response.status(202).build();
	}
	
	
	@GET
	@Path("/{lbid}/virtualips")
	@Produces("application/json")
	public String getLbVips(@Context HttpServletRequest request,@PathParam("lbid") String lbid, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("GET /loadbalancers/{id}/virtualips request cannot be authenticated", 401);   
	    }
	    
	    logger.info("GET /loadbalancers/" + lbid + "/virtualips " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
				
		
		LoadBalancer lb = null;
		
		// read LB
		long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalancer id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId,tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);  
		}
		
		JSONObject jsonResponseObject=new JSONObject();
		try {	
		   jsonResponseObject.put(JSON_VIPS, vipsToJSON(lb.getVirtualIps()));
		   return jsonResponseObject.toString();
		}
		catch ( JSONException jsone) {
			throw new LBaaSException( jsone.toString() ,500);
		}
		
		
		
	}
	
	
	/**
	 * Get all the nodes for a loadbalancer
	 * @param request
	 * @param id
	 * @return JSON array of nodes
	 */
	@GET
	@Path("/{lbid}/nodes")
	@Produces("application/json")
	public String getLbNodes(@Context HttpServletRequest request,@PathParam("lbid") String lbid, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("GET /loadbalancers/{id}/nodes request cannot be authenticated", 401);   
	    }
	    
	    logger.info("GET /loadbalancers/" + lbid + "/nodes " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
				
		
		LoadBalancer lb = null;
		
		// read LB
		long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalancer id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId,tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);  
		}
		
		// return JSON formatted response
		try {
			JSONObject nodes = new JSONObject();
			nodes.put(JSON_NODES, nodesToJSON(lb.getNodes(), false));
			return nodes.toString();
		}
		catch (JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
	}
	
	
	/**
	 * get a node definition for a loadbalancer
	 * @param request
	 * @param id
	 * @param nodeId
	 * @return
	 */
	@GET
	@Path("/{lbid}/nodes/{nodeid}")
	@Produces("application/json")
	public String getLbNode(@Context HttpServletRequest request, @PathParam("lbid") String lbid, @PathParam("nodeid") String nodeId, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("Get /loadbalancers/{id}/nodes/{nodeid} request cannot be authenticated", 401);   
	    }
	    
	    logger.info("GET /loadbalancers/" + lbid + "/nodes" + nodeId + " " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
				
        LoadBalancer lb = null;
		
		// read LB
        long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalaner id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId, tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);   
		}
				
		// find the node
		long longNodeId=0;
		try {
			longNodeId= Long.parseLong(nodeId);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("nodeId : " + nodeId + " is not a valid id",404);
		}	
		Node node = getNodeInLb( lb, longNodeId);		
		if (node == null) {
			throw new LBaaSException("node id: " + nodeId + " not found", 404);  
		}
		
		// return JSON formatted response
		try {			
			return nodeToJSON(node).toString();
		}
		catch (JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
	}
	
	
	/**
	 * Add a node to a loadbalancer
	 * @param request
	 * @param lbid
	 * @param content
	 * @return
	 */
	@POST
	@Path("/{lbid}/nodes")
	@Consumes("application/json")
	@Produces("application/json")
	public String addLbNodes(@Context HttpServletRequest request, @PathParam("lbid") String lbid, String content, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("POST /loadbalancers/{id}/nodes request cannot be authenticated", 401);   
	    }
		
		logger.info("POST loadbalancer nodes lbid: " + lbid );
        String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
		
		LoadBalancer lb = null;
		
		// read LB and check total # of nodes present
		long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalaner id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId,tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);  
		}
		
		// POSTs not allowed while LB is in BUILD or PENDING-UPDATE state
		if ((lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_BUILD)) || (lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_PENDING_UPDATE))) {
			throw new LBaaSException("operation not allowed while load balancer status : " + lb.getStatus() + " for tenant :" + tenantId, 422);  
		}
		
		int currentNodeSize = lb.getNodes().getNodes().size();
						
	    //  extract nodes from content, check for errors and over limits
		Nodes nodes = null;
		try {
		   JSONObject jsonObject=new JSONObject(content);	
		   nodes = jsonToNodes(jsonObject);
		   if ( (nodes.getNodes().size() + currentNodeSize) > Lbaas.lbaasConfig.maxNodesperLb)  {
			   logger.warn("attempt to create update an LB with more than " + Lbaas.lbaasConfig.maxNodesperLb + " nodes");
	           throw new LBaaSException( "attempt to update an LB with more than " + Lbaas.lbaasConfig.maxNodesperLb + " nodes ", 413); 
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
		
		// mark as change pending
		lb.setStatus(LoadBalancer.STATUS_PENDING_UPDATE);		
		
		// write changes to DB
		try {
			lbModel.setLoadBalancer(lb);
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);
	    }
		
		
		// update Node model with new nodes
		try {
		   nodeModel.createNodes(nodes, longId);
		   }
		catch (NodeModelAccessException nme) {
			throw new LBaaSException(nme.getMessage(),500);
		}
		
		
				
		// have the device process the job 
		try {
		   List<LoadBalancer> lbs = lbModel.getLoadBalancersWithDevice(lb.getDevice());
		   lbaasTaskManager.sendJob( new Long(lb.getDevice()), LbToJsonArray(lbs, null, ACTION_UPDATE ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error exception :" + ie.toString(), 500);   
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error exception :" + dme.toString(), 500);   
		}
		
		return content;
		
	}
	
	
	@DELETE
	@Path("/{lbid}/nodes/{nodeid}")
	@Consumes("application/json")
	@Produces("application/json")
	public void deleteLbNode(@Context HttpServletRequest request, @PathParam("lbid") String lbid, @PathParam("nodeid") String nodeid, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("DELETE /loadbalancers/{id}/nodes/{nodeid} request cannot be authenticated", 401);   
	    }
	    
	    logger.info("DELETE /loadbalancers/" + lbid + "/nodes" + nodeid + " " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
				
        LoadBalancer lb = null;
		
		// read LB
        long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalaner id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId, tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);   
		}
		
		// node DELETEs not allowed while LB is in BUILD or PENDING-UPDATE state
		if ((lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_BUILD)) || (lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_PENDING_UPDATE))) {
			throw new LBaaSException("operation not allowed while load balancer status : " + lb.getStatus() + " for tenant :" + tenantId, 422);  
		}
				
		// find the node
		long longNodeId=0;
		try {
			longNodeId= Long.parseLong(nodeid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("node id : " + nodeid + " is not a valid id",404);
		}	
		
		Node node = getNodeInLb( lb, longNodeId);		
		if (node == null) {
			throw new LBaaSException("node id: " + nodeid + " not found", 404);  
		}
		
		// mark as change pending
		lb.setStatus(LoadBalancer.STATUS_PENDING_UPDATE);		
		
		// write changes to DB
		try {
			lbModel.setLoadBalancer(lb);
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);
	    }
		
		// update Node model with new nodes
		try {
		   nodeModel.deleteNode(longNodeId);
		   }
		catch (NodeModelAccessException nme) {
			throw new LBaaSException(nme.getMessage(),500);
		}
				
		// have the device process the job 
		try {
		   List<LoadBalancer> lbs = lbModel.getLoadBalancersWithDevice(lb.getDevice());
		   lbaasTaskManager.sendJob( new Long(lb.getDevice()), LbToJsonArray(lbs, null, ACTION_UPDATE ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error exception :" + ie.toString(), 500);   
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error exception :" + dme.toString(), 500);   
		}
		
		
	}
	
	
	/**
	 * Update a loadbalancers node
	 * @param request
	 * @param id
	 * @param nodeId
	 * @param content
	 */
	@PUT
	@Path("/{lbid}/nodes/{nodeid}")
	@Consumes("application/json")
	@Produces("application/json")
	public void putLbNode(@Context HttpServletRequest request, @PathParam("lbid") String lbid, @PathParam("nodeid") String nodeid, String content, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("PUT /loadbalancers/{id}/nodes/{nodeid} request cannot be authenticated", 401);   
	    }
	    
	    logger.info("PUT /loadbalancers/" + lbid + "/nodes" + nodeid + " " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
				
        LoadBalancer lb = null;
		
		// read LB
        long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalaner id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId, tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);   
		}
		
		// node DELETEs not allowed while LB is in BUILD or PENDING-UPDATE state
		if ((lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_BUILD)) || (lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_PENDING_UPDATE))) {
			throw new LBaaSException("operation not allowed while load balancer status : " + lb.getStatus() + " for tenant :" + tenantId, 422);  
		}
				
		// find the node
		long longNodeId=0;
		try {
			longNodeId= Long.parseLong(nodeid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("node id : " + nodeid + " is not a valid id",404);
		}	
		Node node = getNodeInLb( lb, longNodeId);		
		if (node == null) {
			throw new LBaaSException("node id: " + nodeid + " not found", 404);  
		}
		
		JSONObject jsonObject=null;
		// process the JSON body
		try {
			  jsonObject=new JSONObject(content);
				
		      if (jsonObject.has(JSON_ID))
			     throw new LBaaSException("json node request may not have 'id'", 400);        
		
		      if (jsonObject.has(JSON_ADDRESS))
			     throw new LBaaSException("json node request may not have 'address'", 400);    
		
		      if (jsonObject.has(JSON_PORT))
			     throw new LBaaSException("json node request may not have 'port'", 400);       
		
		      if (jsonObject.has(JSON_WEIGHT))
			     throw new LBaaSException("json node request may not have 'weight'", 400);     
		
		      if (jsonObject.has(JSON_STATUS))
			     throw new LBaaSException("json node request may not have 'status'", 400);     
		
		      if (!jsonObject.has(JSON_CONDITION))
			     throw new LBaaSException("json node request must have 'condition'", 400);     
		
		      String condition = jsonObject.getString(JSON_CONDITION);
		      
		      if ( !condition.equalsIgnoreCase(NODE_ENABLED) && !condition.equalsIgnoreCase(NODE_DISABLED))
		    	  throw new LBaaSException("'condition' value may only be 'ENABLED' or 'DISABLED'", 400);     
		      
		      // change the condition on the node
		      // note, disabling a node will leave it in the local data model but will not be part of the
		      boolean value = false;
		      if ( condition.equalsIgnoreCase(NODE_ENABLED))
		    	  value = true;
		      try {
				   nodeModel.enable(value,longNodeId);
				   }
				catch (NodeModelAccessException nme) {
					throw new LBaaSException(nme.getMessage(),500);
				}
				
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
				   lbaasTaskManager.sendJob( new Long(lb.getDevice()), LbToJsonArray(lbs, null, ACTION_UPDATE ));
				}
				catch ( JSONException jsone) {
					throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
				} 
				catch ( InterruptedException ie) {
					throw new LBaaSException("internal server error exception :" + ie.toString(), 500);   
				}
				catch ( DeviceModelAccessException dme) {
					throw new LBaaSException("internal server error exception :" + dme.toString(), 500);   
				}
		
		}
		catch (JSONException e) {
			throw new LBaaSException("bad json request", 400);     	
		}
		
		
	}
	
    /**
     * create a new LoadBalancer
     * @param content, JSON encoded LoadBalancer for creation
     * @return JSON encoded LoadBalancer which was created including new id
     */
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Response post(@Context HttpServletRequest request, String content, @Context UriInfo info) {
		
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("POST /loadbalancers request cannot be authenticated", 401);   
	    }
	    
	    logger.info("POST /loadbalancers " + KeystoneAuthFilter.toString(request));
	    String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
		
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
		      if ( name.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
		         	throw new LBaaSException("'name' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400);
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
				   throw new LBaaSException("port number: " + port + " is not supported for protocol: " + lb.getProtocol() + ".  See /protocols for supported protocols and ports", 400); 
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
		      virtualIps = jsonToVips(jsonObject,tenantId);		      		      		      
		   }
		   catch ( JSONException jsone) {
				throw new LBaaSException( jsone.toString(), 400);  
		   } 
		   catch ( VipException ve) {
				throw new LBaaSException( ve.message, 400);  
		   } 
		   
		   // tenant who created this
		   lb.setTenantId(tenantId);
		   		   
		   
		   if ( virtualIps != null) {
			    
			   // check that only one vip can be specified for now
			   List<VirtualIp> vipList = virtualIps.getVirtualIps();
			   if ( vipList.size() > Lbaas.lbaasConfig.maxVipsPerLb) {
				   throw new LBaaSException("maximum number of VIPs allowed is " + Lbaas.lbaasConfig.maxVipsPerLb , 400);    //  not available
			   }			   
			   // check that vip is the same address as existing device and this is a new protocol not an existing one
			   String address = vipList.get(0).getAddress();
			   
			   // find existing VIP
			   try {
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
						   throw new LBaaSException("VIP already has loadbalancer with this protoocol for tenant "+ tenantId , 400);     
					   }
					   
					   lbs.add(existingLb);
				   }
			   }
			   catch ( DeviceModelAccessException dme) {
			      throw new LBaaSException(dme.message, 500);
			   }
			    
			   lbs.add(lb);     // add this LB as as well
			   
			   // mark lb as using found device
			   lb.setDevice( new Long(device.getId()));     
			   		   
			   // write it to datamodel
			   try {
			      lbId = lbModel.createLoadBalancer(lb);
			      
			      // set device lb and write it back to data model
				  device.lbIds.add(lbId);
				  
				  // update device with new lb info
				  deviceModel.setDevice(device);
				  
				  logger.info("device taken :" + device.getId().toString());
				   
			   }
			   catch ( DeviceModelAccessException dme) {
			      throw new LBaaSException(dme.message, 500);
			   }
			   			   			   
		   }
		   else {
			   
			   // find free device to use
			   try {
				   
				  logger.info("semaphore available permits : " + semaphore.availablePermits()); 
				  semaphore.acquire(); 
				   
			      device = deviceModel.findFreeDevice();
			   			   		   
			      if ( device == null) { 
				     throw new LBaaSException("no available devices found" , 503);     
			      }
			   
			      logger.info("found free device at id : " + device.getId().toString());
			   
			      virtualIps = deviceToVips( device);
			      lb.setVirtualIps(virtualIps);
			      lbs.add(lb);     // only a single LB in this update
			   			   
			      // mark lb as using found device
			      lb.setDevice( new Long(device.getId()));     
			   		          	   			   		   				   			     
			    
			      // create the load balancer
			      lbId = lbModel.createLoadBalancer(lb);
			      
			      // set device lb and write it back to data model
			      device.lbIds.add(lbId);
			      
			      // update device with lb reference
			      deviceModel.setDevice(device);
			      
			      logger.info("device taken :" + device.getId().toString());
			      
			   }
			   catch (DeviceModelAccessException dme) {
		             throw new LBaaSException(dme.message, 500);
	           }
			   catch (InterruptedException ie) {
					logger.info("job depth InterruptedException");
			   }
			   finally {
			      semaphore.release();
			   }
			   
		   }
		   		   		   		   		     	   		   		   		   
		}
		catch (JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);
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
		   lbaasTaskManager.sendJob( lbResponse.getDevice(), LbToJsonArray(lbs, null, ACTION_UPDATE ));		    	
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error JSON exception :" + ie.toString(), 500);   
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error JSON exception :" + dme.toString(), 500);   
		}
		
		//respond with JSON
		try {		   
		   return Response.status(202).entity( LbToJson(lbResponse)).build();
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
		
	}	
	
	
	
	/**
	 * Create a connection log on the specified Object store
	 * @param request
	 * @param lbid
	 * @param content
	 * @return
	 */
	@POST
	@Path("/{lbid}/logs")
	@Consumes("application/json")
	@Produces("application/json")
	public void createLog(@Context HttpServletRequest request, @PathParam("lbid") String lbid, String content, @Context UriInfo info) 
	{
		if (!KeystoneAuthFilter.authenticated(request)) {
	    	throw new LBaaSException("POST /loadbalancers/{id}/logs request cannot be authenticated", 401);   
	    }
		
		logger.info("POST loadbalancer logs lbid: " + lbid );
        String tenantId = KeystoneAuthFilter.getTenantId(request);
		
		// must have tenant id
		if ((tenantId == null) || ( tenantId.isEmpty())) {
			throw new LBaaSException("token and/or tenant id was not specified", 401);   
		}
		
		// check for admin role tenantId
		tenantId = getAdminTenantId( request, info, tenantId);
		
		LoadBalancer lb = null;
		
		// read LB 
		long longId=0;
		try {
		   longId= Long.parseLong(lbid);		         
        } catch (NumberFormatException nfe) 
        {
		   throw new LBaaSException("loadbalaner id : " + lbid + " is not a valid id",404);
		}	
		try {
		   lb = lbModel.getLoadBalancer(longId,tenantId);
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException(dme.message, 500);                                     
		}		
		if (lb == null) {
			throw new LBaaSException("loadbalancer id:" + lbid + " not found for tenant :" + tenantId, 404);  
		}
		
		// POSTs not allowed while LB is in BUILD or PENDING-UPDATE state
		if ((lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_BUILD)) || (lb.getStatus().equalsIgnoreCase(LoadBalancer.STATUS_PENDING_UPDATE))) {
			throw new LBaaSException("operation not allowed while load balancer status : " + lb.getStatus() + " for tenant :" + tenantId, 422);  
		}
		
		// defaults for archive request
		LBaaSArchiveRequest lbaaSArchiveRequest = new LBaaSArchiveRequest();
		lbaaSArchiveRequest.objectStoreType = Lbaas.lbaasConfig.objectStoreType;
		lbaaSArchiveRequest.objectStoreEndpoint = Lbaas.lbaasConfig.objectStoreEndpoint;
		lbaaSArchiveRequest.objectStoreBasePath = Lbaas.lbaasConfig.objectStoreLogBasePath;
		lbaaSArchiveRequest.objectStoreToken =  request.getHeader(KeystoneAuthFilter.KEYSTONE_AUTH_TOKEN);
		
		// look for JSON body and extract and override values 
		if ( !content.isEmpty()) {
			try {
				JSONObject jsonObject=new JSONObject(content);
				
				// auth token
				if ( jsonObject.has(JSON_AUTH_TOKEN)) {
				   String token = (String) jsonObject.get(JSON_AUTH_TOKEN);
				   if ( token.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
		              throw new LBaaSException("'token' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400); 
				   lbaaSArchiveRequest.objectStoreToken = token;
				   logger.info("over riding token value with : " + token);
				}
				
				// object endpoint				
				if ( jsonObject.has(JSON_OBJ_ENDPNT)) {
					   String endpoint = (String) jsonObject.get(JSON_OBJ_ENDPNT);
					   if ( endpoint.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
			              throw new LBaaSException("'objectStoreEndpoint' is over max allowed length of : " + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400); 
					   lbaaSArchiveRequest.objectStoreEndpoint = endpoint;
					   logger.info("over riding endpoint value with : " + endpoint);
				}
				
				// object store base path				
				if ( jsonObject.has(JSON_OBJ_PATH)) {
					   String path = (String) jsonObject.get(JSON_OBJ_PATH);
					   if ( path.length() > LimitsHandler.LIMIT_MAX_NAME_SIZE)
			              throw new LBaaSException("'objectStoreBasePath' name is over max allowed length of : " + LimitsHandler.LIMIT_MAX_NAME_SIZE, 400); 
					   lbaaSArchiveRequest.objectStoreBasePath=path;
					   logger.info("over riding base path value with : " + path);
				}
				
			}
			catch ( JSONException jsone) {
				throw new LBaaSException( jsone.toString(), 400);    
			} 
			
		}
		
		// mark as change pending
		lb.setStatus(LoadBalancer.STATUS_PENDING_UPDATE);		
		
		// write changes to DB
		try {
			lbModel.setLoadBalancer(lb);
		}
		catch ( DeviceModelAccessException dme) {
	         throw new LBaaSException(dme.message, 500);
	    }
	    
		// make worker request
		List<LoadBalancer> lbs = new  ArrayList<LoadBalancer>();
		
		// only a single LB log is generated at a time
		lbs.add(lb);
		
		// have the device process the request
		try {           			   
		   lbaasTaskManager.sendJob( lb.getDevice(), LbToJsonArray(lbs, lbaaSArchiveRequest, ACTION_ARCHIVE ));		    	
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);   
		} 
		catch ( InterruptedException ie) {
			throw new LBaaSException("internal server error JSON exception :" + ie.toString(), 500);   
		}
		catch ( DeviceModelAccessException dme) {
			throw new LBaaSException("internal server error JSON exception :" + dme.toString(), 500);   
		}
		
		
	}

	
}
