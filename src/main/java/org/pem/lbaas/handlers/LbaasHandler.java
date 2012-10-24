package org.pem.lbaas.handlers;


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
import org.pem.lbaas.persistency.LoadBalancerDataModel;
import javax.ws.rs.WebApplicationException;

@Path("/loadbalancers")
public class LbaasHandler {
	private static Logger logger = Logger.getLogger(LbaasHandler.class);	
    private static LBaaSTaskManager lbaasTaskManager = new LBaaSTaskManager();
    private static long requestId=0;
    public static String HPCS_ACTION         = "hpcs_action";
    public static String HPCS_REQUESTID      = "hpcs_requestid";
    public static String HPCS_RESPONSE       = "hpcs_response";
    public static String HPCS_DEVICE         = "hpcs_device";
    public static String HPCS_RESPONSE_PASS  = "PASS";
    public static String HPCS_RESPONSE_FAIL  = "FAIL";
    public static String ACTION_CREATE       = "CREATE";
    public static String ACTION_UPDATE       = "UPDATE";
    public static String ACTION_SUSPEND      = "SUSPEND";
    public static String ACTION_ENABLE       = "ENABLE";
    public static String ACTION_DELETE       = "DELETE";
    
		    
	/*
	 * Utility method for JSON'izing a single LB
	 */
	protected String LbToJson(LoadBalancer lb, String action) throws JSONException{
		
		JSONObject jsonResponseObject=new JSONObject();
		try {	
		   // internal fields for worker IPC only	
		   if ( action != null) {
			   jsonResponseObject.put(HPCS_REQUESTID,++requestId);
			   jsonResponseObject.put(HPCS_ACTION,action);
			   jsonResponseObject.put(HPCS_DEVICE, lb.getDevice());
		   }
		   jsonResponseObject.put("name",lb.getName());	
		   jsonResponseObject.put("id",lb.getId());
		   jsonResponseObject.put("protocol",lb.getProtocol());
		   jsonResponseObject.put("port", lb.getPort());
		   jsonResponseObject.put("algorithm", lb.getAlgorithm());
		   jsonResponseObject.put("status", lb.getStatus());
		   jsonResponseObject.put("created",  lb.getCreated());
		   jsonResponseObject.put("updated",  lb.getUpdated());
		   
		   // vips
		   JSONArray jsonVipArray = new JSONArray();		   
		   VirtualIps vips = lb.getVirtualIps();
		   if ( vips != null) {
			   List<VirtualIp> vipslist = vips.getVirtualIps();
			   if ( vipslist!=null)
				   for ( int x=0;x<vipslist.size();x++) {
					   JSONObject jsonVIP=new JSONObject();
					   jsonVIP.put("address", vipslist.get(x).getAddress());
					   jsonVIP.put("id", vipslist.get(x).getId());
					   jsonVIP.put("type", vipslist.get(x).getType());
					   jsonVIP.put("ipVersion", vipslist.get(x).getIpVersion());
					   jsonVipArray.put(jsonVIP);
				   }
		   }
		   jsonResponseObject.put("virtualIps", jsonVipArray);
		   
		   // nodes
		   JSONArray jsonNodeArray = new JSONArray();
		   Nodes nodes = lb.getNodes();
		   if (nodes != null) {
			   List<Node> nodeList = nodes.getNodes();
			   for ( int y=0;y<nodeList.size();y++) {
				   JSONObject jsonNode=new JSONObject();
				   jsonNode.put("address", nodeList.get(y).getAddress());
				   jsonNode.put("id",nodeList.get(y).getId()); 			   
				   jsonNode.put("port" ,nodeList.get(y).getPort());
				   jsonNode.put("status", nodeList.get(y).getStatus());
				   jsonNodeArray.put(jsonNode);
			   }		   		   
		   }		   
		   jsonResponseObject.put("nodes", jsonNodeArray);
		   		   
		   return jsonResponseObject.toString();
		}
		catch ( JSONException jsone) {
			throw jsone;
		}
	}
	
	
	/*
	 * return list of all LBs
	 */
	@GET
	@Produces("application/json")
	public String getAll() {
		logger.info("GET loadbalancers");
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		LoadBalancerDataModel model = new LoadBalancerDataModel();
		List<LoadBalancer> lbs = model.getLoadBalancers();
		try {	
		   for (int x=0;x<lbs.size();x++) {
			   JSONObject jsonLb=new JSONObject();
			   jsonLb.put("name", lbs.get(x).getName());
			   jsonLb.put("id", lbs.get(x).getId());
			   jsonLb.put("protocol",lbs.get(x).getProtocol());
			   jsonLb.put("port",lbs.get(x).getPort());
			   jsonLb.put("algorithm", lbs.get(x).getAlgorithm());
			   jsonLb.put("status",lbs.get(x).getStatus());
			   jsonLb.put("created", lbs.get(x).getCreated());
			   jsonLb.put("updated", lbs.get(x).getUpdated());
			   jsonArray.put(jsonLb);
		   }
			
		   jsonObject.put("loadbalancers",jsonArray);		   				   	   		   
		   return jsonObject.toString();
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		}
	}
	
	/*
	 * return a specific LB
	 */
	@GET
	@Path("/{id}")
	@Produces("application/json")
	public String getLb(@PathParam("id") String id) 
	{
		logger.info("GET loadbalancer : " + id);
		LoadBalancerDataModel model = new LoadBalancerDataModel();
		Integer lbId = new Integer(id);
		LoadBalancer lb = model.getLoadBalancer(lbId);
		if ( lb == null) {
			throw new LBaaSException("could not find id : " + id, 404);  //  not found
		}
		
		try {
			return LbToJson(lb,null);
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
	}
	
	/*
	 * Update a specific LB
	 * only allows changing name or algorithm
	 */
	@PUT
	@Path("/{id}")
	@Consumes("application/json")
	public void updateLb(@PathParam("id") String id, String content) 
	{
		logger.info("PUT loadbalancer : " + id);
		LoadBalancerDataModel model = new LoadBalancerDataModel();
		
		// attempt to read lb top be updated
		Integer lbId = new Integer(id);
		LoadBalancer lb = model.getLoadBalancer(lbId);
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
			throw new LBaaSException("name and algorithm missing", 400);    //  bad request				
		}
				
		if (name !=null)
			lb.setName(name);
		
		if (algorithm != null)
			lb.setAlgorithm(algorithm);
		
		// mark as change pending
		lb.setStatus(LoadBalancer.STATUS_PENDING_UPDATE);		
		
		// write changes to DB
		model.setLoadBalancer(lb);
		
		// have the device process the job 
		try {
		   lbaasTaskManager.sendJob( lb.getDevice(), LbToJson(lb, ACTION_UPDATE ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
										
	}
	
	/*
	 * Delete an LB
	 */
	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	public void deleteLb(@PathParam("id") String id) 
	{
		// delete the LB, send worker delete, finally clear device when worker responds ( not here )
		
		logger.info("DELETE loadbalancer : " + id);
		LoadBalancerDataModel model = new LoadBalancerDataModel();
		Integer lbId = new Integer(id);
		LoadBalancer lb = model.getLoadBalancer(lbId);
		if ( lb == null) {
			throw new LBaaSException("could not find id : " + id, 404);              //  not found	
		}
		int deleteCount = model.deleteLoadBalancer(lbId);
		if (deleteCount==0) {
			throw new LBaaSException("could not find id on delete: " + id, 404);    //  not found	
		}
		
		// have the device process the job 
		try {
		   lbaasTaskManager.sendJob( lb.getDevice(), LbToJson(lb, ACTION_DELETE ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
	}
	
	/*
	 * return Nodes for a specific LB 
	 */
	@GET
	@Path("/{loadbalancerId}/nodes")
	@Produces("application/json")
	public String getLbNodes(@PathParam("loadbalancerId") String loadbalancerId) 
	{
		logger.info("GET loadbalancer nodes : " + loadbalancerId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
	/*
	 * Get specific node for an LB 
	 */
	@GET
	@Path("/{loadbalancerId}/nodes/{nodeId}")
	@Produces("application/json")
	public String getLbNode(@PathParam("loadbalancerId") String loadbalancerId, @PathParam("nodeId") String nodeId) 
	{
		logger.info("GET loadbalancer node : " + loadbalancerId + ":" + nodeId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
	/*
	 * Add a new node to the LB.
	 */
	@POST
	@Path("/{loadbalancerId}/nodes")
	@Consumes("application/json")
	@Produces("application/json")
	public String addLbNodes(@PathParam("loadbalancerId") String loadbalancerId) 
	{
		logger.info("POST loadbalancer nodes : " + loadbalancerId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
	/*
	 * modify an LBs Node
	 */
	@PUT
	@Path("/{loadbalancerId}/nodes/{nodeId}")
	@Consumes("application/json")
	@Produces("application/json")
	public String modifyLbNode(@PathParam("loadbalancerId") String loadbalancerId, @PathParam("nodeId") String nodeId) 
	{
		logger.info("PUT loadbalancer node : " + loadbalancerId + ":" + nodeId);
		
		throw new LBaaSException("not supported" , 501);  //  not implemented
	}
	
		
	/*
	 * Create a new LB
	 */
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public String post(String content) {
		logger.info("POST loadbalancers");
		
		Device device=null;
		
		// process POSTed body
		LoadBalancerDataModel lbModel = new LoadBalancerDataModel();
		LoadBalancer lb = new LoadBalancer();
		Integer lbId=0;
		int x=0;
		try {
		   JSONObject jsonObject=new JSONObject(content);
		   String name = (String) jsonObject.get("name");
		   lb.setName(name);
		   logger.info("   name = " + name);
		   
		   // minimally request needs nodes
		   if ( !jsonObject.has("nodes") ) {
			   throw new LBaaSException("nodes are required", 400);    //  bad request				   
		   }
		   
		   // check to ensure protocol is supported
		   if (jsonObject.has("protocol") ) {
			   String protocol = (String) jsonObject.get("protocol");
			   if (! ProtocolHandler.exists(protocol)) {
				   throw new LBaaSException("protocol specified not supported : " + protocol, 400);    //  bad request					   
			   }
			   else {
				   logger.info("   protocol = " + protocol);				   
				   lb.setProtocol(protocol);
				   lb.setPort(ProtocolHandler.getPort(protocol));
				   logger.info("   port = " + lb.getPort());
			   }
		   }
		   
		   // nodes
		   Nodes nodes = new Nodes();		   
		   JSONArray jsonNodesArray = (JSONArray) jsonObject.get("nodes");
		   for ( x=0;x<jsonNodesArray.length();x++) {
			   Node node = new Node();
			   //logger.info("node["+x+"] = "+ jsonNodesArray.getJSONObject(x));
			   JSONObject jsonNode = jsonNodesArray.getJSONObject(x);
			   String address = (String) jsonNode.get("address");
			   node.setAddress(address);
			   String port = (String) jsonNode.get("port");
			   node.setPort(Integer.valueOf(port));
			   node.setStatus("ONLINE");			   
			   node.setId(new Integer(x+1));
			   logger.info("   Node["+x+"]");
			   logger.info("      address = " + address);
			   logger.info("      port = " + port);
			   logger.info("      status = " + node.getStatus());
			   nodes.getNodes().add(node);			   
		   }
		   lb.setNodes(nodes);		 
		   
		   // vips
		   VirtualIps virtualIps = new VirtualIps();
		   if ( jsonObject.has("virtualIps") ) {
			   JSONArray jsonVIPArray = (JSONArray) jsonObject.get("virtualIps");
			   for ( x=0;x<jsonVIPArray.length();x++) {
				   VirtualIp virtualIp = new VirtualIp();
				   //logger.info("vip["+x+"] = "+ jsonVIPArray.getJSONObject(x));
				   JSONObject jsonVip = jsonVIPArray.getJSONObject(x);
				   
				   String address = (String) jsonVip.get("address");
				   virtualIp.setAddress(address);
				   
				   if (jsonVip.get("ipVersion").toString().equalsIgnoreCase("IPV4"))	
				      virtualIp.setIpVersion(IpVersion.IPV_4);
				   else
					   virtualIp.setIpVersion(IpVersion.IPV_6);
				   
				   if ( jsonVip.get("type").toString().equalsIgnoreCase("public"))
				      virtualIp.setType(VipType.PUBLIC);
				   else
					   virtualIp.setType(VipType.PRIVATE);
				   
				   virtualIp.setId(new Integer(x+1));
				   
				   
				   logger.info("   VIP["+x+"]");
				   logger.info("      address = " + virtualIp.getAddress());
				   logger.info("      ipversion = " + virtualIp.getIpVersion().toString());
				   logger.info("      type = " + virtualIp.getType().toString());
				   virtualIps.getVirtualIps().add(virtualIp);
			   }
			   lb.setVirtualIps(virtualIps);
		   }
		   
		   
		   // find free device to use
		   DeviceDataModel deviceModel = new DeviceDataModel();
		   device = deviceModel.findFreeDevice();
		   if ( device == null) {
			   throw new LBaaSException("cannot find free device available" , 503);    //  not available
		   }
		   
		   logger.info("found free device at id : " + device.getId().toString());
		   		   		   
		   // create new LB
		   lb.setDevice( device.getId());              // set lb device id
		   lbId = lbModel.createLoadBalancer(lb);	   // write it to datamodel	   	
		   
		   // set device lb and write it back to data model
		   device.setLbId(lbId);
		   deviceModel.setDevice(device);
		   
		}
		catch (JSONException e) {
			return e.toString();
		}
						
		// read LB back from data model, it will now have valid id
		LoadBalancer lbResponse = lbModel.getLoadBalancer(lbId);
 		
		// have the device process the request
		try {
		   lbaasTaskManager.sendJob( lbResponse.getDevice(), LbToJson(lb, ACTION_CREATE ));
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
		
		//respond with JSON
		try {
		   return LbToJson(lbResponse,null);
		}
		catch ( JSONException jsone) {
			throw new LBaaSException("internal server error JSON exception :" + jsone.toString(), 500);  //  internal error
		} 
		
	}	

	
}
