package org.pem.lbaas.messaging;

import org.apache.log4j.Logger;
import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventCallback;
import org.gearman.GearmanJoin;
import org.gearman.GearmanServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pem.lbaas.Lbaas;
import org.pem.lbaas.datamodel.Device;
import org.pem.lbaas.datamodel.LoadBalancer;
import org.pem.lbaas.handlers.tenant.LBaaSException;
import org.pem.lbaas.handlers.tenant.LbaasHandler;
import org.pem.lbaas.persistency.DeviceDataModel;
import org.pem.lbaas.persistency.DeviceModelAccessException;
import org.pem.lbaas.persistency.LoadBalancerDataModel;

public class LBaaSTaskManager implements GearmanJobEventCallback<String> {
	
	// TODO: make models static for better perf
	
	private static Logger logger = Logger.getLogger(LBaaSTaskManager.class);
	
	Gearman gearman=null;
	GearmanClient gearmanClient=null;
	GearmanServer gearmanServer=null;
	
	public LBaaSTaskManager() {
	   gearman = Gearman.createGearman();
       gearmanClient = gearman.createGearmanClient();
       gearmanServer = gearman.createGearmanServer( Lbaas.lbaasConfig.gearmanServerAddr, Lbaas.lbaasConfig.gearmanServerPort);
       gearmanClient.addServer(gearmanServer);
       logger.info("LBaaSTaskManager constructor");
	}
	
	public boolean sendJob( String workerName, String message ) throws InterruptedException {
       logger.info("gearman client submitting job to :" + workerName);	
       logger.info("gearman client job request msg : " + message);
       GearmanJoin<String> join = gearmanClient.submitJob( workerName, message.getBytes(), workerName, this);            
       join.join();		
       logger.info("gearman job submitted");
       return true;
	}
	
	public boolean sendJob( Integer deviceId, String job) {
		DeviceDataModel deviceModel = new DeviceDataModel();
		Device device = null;
		try {
		   device = deviceModel.getDevice(deviceId);
		}
		catch (DeviceModelAccessException dme) {
            logger.error(dme.message);
        }
		try {
		    return this.sendJob( device.getName() ,job);
		}
		catch ( InterruptedException ie)
		{
			logger.error("sendjob interrupted : " + ie);
		}		
		return true;
	}
	
	protected void finalize ()  {
       logger.info("gearman shutting down .. "); 
       gearman.shutdown();
	}
	
	public void processLoadBalancerSuccess( String message) {
		logger.info("gearman job sucess");
		LoadBalancerDataModel lbModel = new LoadBalancerDataModel();
		DeviceDataModel devModel = new DeviceDataModel();
		try {
		    JSONObject jsonObject=new JSONObject(message);
			   
		   
		    Integer deviceId = (Integer) jsonObject.getInt(LbaasHandler.HPCS_DEVICE);
		    String action = (String) jsonObject.get(LbaasHandler.HPCS_ACTION);
		    Integer requestId = (Integer) jsonObject.getInt(LbaasHandler.HPCS_REQUESTID);
		    String response = (String) jsonObject.get(LbaasHandler.HPCS_RESPONSE);
		    
		    // temp hack, just grabs the first one for now
		    JSONArray jsonLbs = (JSONArray) jsonObject.get("loadbalancers");
		    JSONObject jsonlb = jsonLbs.getJSONObject(0);
		    String lbname = (String) jsonlb.get("name");
		    Integer id = (Integer) jsonlb.getInt("id");
		    
		    
		    logger.info("LB        : " + lbname);
		    logger.info("LB ID     : " + id );
		    logger.info("Device ID : " + deviceId);
		    logger.info("requestID : " + requestId);
		    logger.info("action    : " + action);
		    logger.info("response  : " + response);
		    
		    if ( response.equalsIgnoreCase(LbaasHandler.HPCS_RESPONSE_PASS)) {
		    	logger.info("worker response : PASS");
			    if ( action.equalsIgnoreCase(LbaasHandler.ACTION_CREATE) ||  action.equalsIgnoreCase(LbaasHandler.ACTION_UPDATE)) {	
			    	// move lb to active state
			    	try {
			    	   lbModel.setStatus(LoadBalancer.STATUS_ACTIVE, id);
			    	}
			    	catch (DeviceModelAccessException dme) {
			             logger.error(dme.message);
		            }
			    	
			    	// move device status to online
			    	try {
			    	   devModel.setStatus(Device.STATUS_ONLINE, deviceId);
			    	}
			    	catch (DeviceModelAccessException dme) {
			             logger.error(dme.message);
		           }
			    }
			    else
			    if ( action.equalsIgnoreCase(LbaasHandler.ACTION_DELETE)) {
			    	// update Device status to free, putting it back in the pool as free
			    	// LB has already been deleted in API thread	
			    	try {
			    	   devModel.markAsFree(deviceId);			
			    	   devModel.setStatus(Device.STATUS_OFFLINE, deviceId);
			    	}
			    	catch (DeviceModelAccessException dme) {
			             logger.error(dme.message);
		           }
			    }
		    }
		    else {
		    	logger.info("worker response not PASS value : " + response + " marking LB and device as ERROR");
		    	logger.info("message : " + message);
		    	
		    	// move lb to error state
		    	if ( !action.equalsIgnoreCase(LbaasHandler.ACTION_DELETE)) {
		    	   try {			    	
		    	      lbModel.setStatus(LoadBalancer.STATUS_ERROR, id);
		    	   }
		    	   catch (DeviceModelAccessException dme) {
			             logger.error(dme.message);
		           }
		    	}
		    	
		    	// move device to error state
		    	try {
		    	   devModel.setStatus(Device.STATUS_ERROR, deviceId);
		    	}
		    	catch (DeviceModelAccessException dme) {
		             logger.error(dme.message);
	           }
		    }		    			    
		}
		catch (JSONException e) {
			logger.error("JSON error in gearman job response : " + e.toString());
		}
	}
	
    public void processLoadBalancerSubmitFail( String message) {
    	logger.error("gearman job submit failure message :" + message);
    	
	}
    
    public void processLoadBalancerWorkerFail( String message) {
    	logger.error("gearman job worker failure message :" + message);    	    	
	}
	
				          
     @Override
     public void onEvent(String attachment, GearmanJobEvent event) {
             switch (event.getEventType()) {
             case GEARMAN_JOB_SUCCESS: 
                 processLoadBalancerSuccess( new String(event.getData()));
                 break;
             case GEARMAN_SUBMIT_FAIL: 
            	 processLoadBalancerSubmitFail( new String(event.getData()));
            	 break;
             case GEARMAN_JOB_FAIL: 
            	 processLoadBalancerWorkerFail( new String(event.getData()));
                 break;
             default:
            	 logger.info("gearback callback : " + event.getEventType() + " data :" + new String(event.getData()));
             }

     }
}
