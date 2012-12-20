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

/**
 * LBaaSTaskManager manages asynchronous tasks from the API server to the device workers using gearman.
 * @author peter
 *
 */
public class LBaaSTaskManager implements GearmanJobEventCallback<String> {	
	private static Logger logger = Logger.getLogger(LBaaSTaskManager.class);
	private static DeviceDataModel deviceModel = new DeviceDataModel();
	private static LoadBalancerDataModel loadbalancerModel = new LoadBalancerDataModel();
	
	// gearman
	Gearman gearman=null;
	GearmanClient gearmanClient=null;
	GearmanServer gearmanServer=null;
	
	/**
	 * Constructor sets up gearman for client behavior
	 */
	public LBaaSTaskManager() {
	   gearman = Gearman.createGearman();
       gearmanClient = gearman.createGearmanClient();
       gearmanServer = gearman.createGearmanServer( Lbaas.lbaasConfig.gearmanServerAddr, Lbaas.lbaasConfig.gearmanServerPort);
       gearmanClient.addServer(gearmanServer);
       logger.info("LBaaSTaskManager constructor");
	}
	
	public int serverCount() {
		return gearmanClient.getServerCount();	
	}
	
	/**
	 * Send a gearman job to a worker, response is asynchronous to callback
	 * @param workerName is the unique name for the worker
	 * @param message to send is the LBaaS job encoded in JSON
	 * @return
	 * @throws InterruptedException
	 */
	public void sendJob( String workerName, String message ) throws InterruptedException {
       logger.info("gearman client submitting job to :" + workerName);	
       logger.info("gearman client job request msg : " + message);
       GearmanJoin<String> join = gearmanClient.submitJob( workerName, message.getBytes(), workerName, this);            
       join.join();		
       logger.info("gearman job submitted");
	}
	
	/**
	 * Send a gearman job using the device ID as the destination which is resolved to actual gearman worker name
	 * @param deviceId to send to
	 * @param job to send is the LBaaS job encoded in JSON
	 * @return
	 */
	public void sendJob( Long deviceId, String job) throws DeviceModelAccessException, InterruptedException {
		DeviceDataModel deviceModel = new DeviceDataModel();
		Device device = null;
		try {
		   device = deviceModel.getDevice(deviceId);
		}
		catch (DeviceModelAccessException dme) {
            throw dme;
        }
		try {
		    this.sendJob( device.getName() ,job);
		}
		catch ( InterruptedException ie)
		{
			logger.error("sendjob interrupted : " + ie);
			throw ie;
		}		
	}
	
	/**
	 * finalize'r cleans everything up
	 */
	protected void finalize ()  {
       logger.info("gearman shutting down .. "); 
       gearman.shutdown();
	}
	
	/**
	 * Process Gearman job success. Response from worker has status, loadbalancer and device information which
	 * is extracted and used to update database for loadlancer and device.
	 * @param message
	 */
	public void processLoadBalancerSuccess( String message) {
		logger.info("gearman job sucess");		
		try {
		    JSONObject jsonObject=new JSONObject(message);			   
		   
		    Long deviceId = (Long) jsonObject.getLong(LbaasHandler.HPCS_DEVICE);
		    String action = (String) jsonObject.get(LbaasHandler.HPCS_ACTION);
		    Long requestId = (Long) jsonObject.getLong(LbaasHandler.HPCS_REQUESTID);
		    String response = (String) jsonObject.get(LbaasHandler.HPCS_RESPONSE);
		    logger.info("Device ID : " + deviceId);
		    logger.info("requestID : " + requestId);
		    logger.info("action    : " + action);
		    logger.info("response  : " + response);
		    
		    // must have array of lbs
		    if ( !jsonObject.has(LbaasHandler.JSON_LBS) ) {
		    	logger.error("gearman worker response does not have array of " + LbaasHandler.JSON_LBS + " unable to process response!");
		    	return;
		    }
		    
		    // loop through all LBs
		    JSONArray jsonLbArray = (JSONArray) jsonObject.get(LbaasHandler.JSON_LBS);
			for ( int x=0;x<jsonLbArray.length();x++) {
				JSONObject jsonLb = jsonLbArray.getJSONObject(x);						    		    
		        String lbName = (String) jsonLb.get(LbaasHandler.JSON_NAME);
		        Long lbId = (Long) jsonLb.getLong(LbaasHandler.JSON_ID);
		        String tenantId = (String) jsonLb.get(LbaasHandler.HPCS_TENANTID);
		        logger.info("Loadbalancer : " + lbName + " (" + lbId + ")" + "tenant id :" + tenantId);
		        
		        if ( response.equalsIgnoreCase(LbaasHandler.HPCS_RESPONSE_PASS)) {
		    	   logger.info("worker response : PASS");
			       if ( action.equalsIgnoreCase(LbaasHandler.ACTION_UPDATE)) {	
				    	// move lb to active state
				    	try {
				    		loadbalancerModel.setStatus(LoadBalancer.STATUS_ACTIVE, lbId,tenantId);
				    	}
				    	catch (DeviceModelAccessException dme) {
				             logger.error(dme.message);
			            }
				    	
				    	// move device status to online
				    	try {
				    		deviceModel.setStatus(Device.STATUS_ONLINE, deviceId);
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
					    	deviceModel.setStatus(Device.STATUS_OFFLINE, deviceId);
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
			    		   loadbalancerModel.setStatus(LoadBalancer.STATUS_ERROR, lbId, tenantId);
			    	   }
			    	   catch (DeviceModelAccessException dme) {
				             logger.error(dme.message);
			           }
			    	}
			    	
			    	// move device to error state
			    	try {
			    		deviceModel.setStatus(Device.STATUS_ERROR, deviceId);
			    	}
			    	catch (DeviceModelAccessException dme) {
			             logger.error(dme.message);
		           }
		        }	
			}
		}
		catch (JSONException e) {
			logger.error("JSON error in gearman job response : " + e.toString());
		}
	}
	
	/**
     * process a worker submit failure, not much to do other than log it out
     * @param message
     */
    public void processLoadBalancerSubmitFail( String message) {
    	logger.error("gearman job submit failure message :" + message);
    	
	}
    
    /**
     * process a worker failure, not much to do other than log it out
     * @param message
     */
    public void processLoadBalancerWorkerFail( String message) {
    	logger.error("gearman job worker failure message :" + message);    	    	
	}
	
	
    /**
     * Gearman asynch callback for processing worker responses
     */
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
