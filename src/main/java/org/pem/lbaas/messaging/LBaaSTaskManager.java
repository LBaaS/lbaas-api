package org.pem.lbaas.messaging;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;


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
public class LBaaSTaskManager implements GearmanJobEventCallback<String>, Runnable {	
	private static Logger logger = Logger.getLogger(LBaaSTaskManager.class);
	private static DeviceDataModel deviceModel = new DeviceDataModel();
	private static LoadBalancerDataModel loadbalancerModel = new LoadBalancerDataModel();
	private static Semaphore semaphore = new Semaphore(1);
	private static HashMap requestMap = new HashMap();
	private static long life=0;
	private static Thread runner=null;
	
	public static long TASK_MGR_SLEEP_TOV = 1000;
	public static long TASK_MGR_MAX_LIFE = 10;
	
	// gearman
	Gearman gearman=null;
	GearmanClient gearmanClient=null;
	GearmanServer gearmanServer1=null;
	GearmanServer gearmanServer2=null;
	
	/**
	 * Constructor sets up gearman for client behavior
	 */
	public LBaaSTaskManager() {
	   gearman = Gearman.createGearman();
       gearmanClient = gearman.createGearmanClient();
       gearmanServer1 = gearman.createGearmanServer( Lbaas.lbaasConfig.gearmanServer1Addr, Lbaas.lbaasConfig.gearmanServerPort);
       gearmanClient.addServer(gearmanServer1);
       
       // HA Gearman setup
       if (( Lbaas.lbaasConfig.gearmanServer2Addr!=null) && (!Lbaas.lbaasConfig.gearmanServer2Addr.isEmpty())) {
    	   gearmanServer2 = gearman.createGearmanServer( Lbaas.lbaasConfig.gearmanServer2Addr, Lbaas.lbaasConfig.gearmanServerPort);
           gearmanClient.addServer(gearmanServer2);
       }
       
       logger.info("LBaaSTaskManager constructor");
       if (runner==null) {
          runner = new Thread(this);
          runner.start();
       }
	}
	
	/**
	 *  get rid of old jobs 
	 *  */
	private void ageOutDeadJobs() {
		try {			
			semaphore.acquire();
			Iterator it = requestMap.entrySet().iterator();
		    while (it.hasNext()) {
		    	Map.Entry entry = (Map.Entry) it.next();
		    	String key = (String)entry.getKey();
		    	LBaaSTrackedJob job = (LBaaSTrackedJob)entry.getValue();
		    	if ( job.lifeInSecs > TASK_MGR_MAX_LIFE) {
		    		logger.info("timing out job " + key);
		    		jobCompletedFail(job.trackedJob);
		    		requestMap.remove(key);
		    	}
		    	else {
		    		logger.info("job life :" + job.lifeInSecs);
		    		job.lifeInSecs++;
		    	}
		    }	
		}
		catch (InterruptedException ie) {
			logger.info("job depth InterruptedException");
		}
		finally {
			semaphore.release();
		}
		
	}
	
	/**
	 * Task manager background thread for timing out dead jobs
	 */
	public void run() {
		for (;;) {
			life++;
			try {
				Thread.sleep(TASK_MGR_SLEEP_TOV);
				ageOutDeadJobs();
			} catch (InterruptedException e) {
			   logger.info("Task Manager run InterruptedException");
			}
		}
	}
	
	/**
	 *  return current gearman servercount
	 * @return number of current gearman job servers
	 */
	public int serverCount() {
		return gearmanClient.getServerCount();	
	}
	
	/**
	 * return the current depth of pending jobs
	 * @return
	 */
	public int jobDepth() {
		int size=0;
		try {
			semaphore.acquire();
			size = requestMap.size();			
		}
		catch (InterruptedException ie) {
			logger.info("job depth InterruptedException");
		}
		finally {
			semaphore.release();
		}
		
		return size;
	}
	
	/**
	 * extract the job request id from the job
	 * @param message
	 * @return
	 */
	public String getJobRequestId( String message) {
		try {
			   JSONObject jsonObject=new JSONObject(message);
			   long id = jsonObject.getLong(LbaasHandler.HPCS_REQUESTID);
			   return Long.toString(id);
		}
		catch (JSONException e) {
			logger.error("could not find " + LbaasHandler.HPCS_REQUESTID + " in job submitted!" );
			return null;
		}
	}
	
	/**
	 * Stop tracking a job since it was completed with success
	 * @param message
	 */
	public void jobCompletedSuccess( String message) {
		logger.info("jobCompletedSuccess");
		try {
		   semaphore.acquire();
		   String jobRequestId = getJobRequestId(message);
		   if (jobRequestId!=null)
		      requestMap.remove(jobRequestId);
		   else
			  logger.error("could not mark job completed!");
		}
		catch (InterruptedException ie) {
			logger.info("jobCompletedSuccess InterruptedException");
		}
		finally {
			semaphore.release();
		}
	}
	
	/**
	 * Stop tracking a job due to a failure or timeout
	 * @param message
	 */
	public synchronized void jobCompletedFail( String message) {
		logger.info("jobCompletedFail");
		
		// mark device in error
		try {
		    JSONObject jsonObject=new JSONObject(message);			   
		   
		    Long deviceId = (Long) jsonObject.getLong(LbaasHandler.HPCS_DEVICE);
		    Long requestId = (Long) jsonObject.getLong(LbaasHandler.HPCS_REQUESTID);
		    String errorMsg = null;
		    if (jsonObject.has(LbaasHandler.HPCS_ERROR)) 
		    	errorMsg = (String) jsonObject.get(LbaasHandler.HPCS_ERROR);
		    
		    logger.info("Device ID : " + deviceId);
		    logger.info("requestID : " + requestId);
		    logger.info("errorMsg  : " + errorMsg);
		    
		    
		    // put device in error state
	    	try {
	    		deviceModel.setStatus(Device.STATUS_ERROR, deviceId);
	    	}
	    	catch (DeviceModelAccessException dme) {
	             logger.error(dme.message);
	    	}
	    	
	    	// put LBs in error state
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
		        
		        // move lb to error state
		    	try {
		    		loadbalancerModel.setStatus(LoadBalancer.STATUS_ERROR, lbId,tenantId);
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
       gearmanClient.submitJob( workerName, message.getBytes(), workerName, this);  
       String jobRequestId = getJobRequestId(message);
       if (jobRequestId!=null ) {
    	   try {
    		  semaphore.acquire();
    	      requestMap.put(jobRequestId, new LBaaSTrackedJob(message));
    	   }
    	   catch (InterruptedException ie) {
   			   logger.info("sendjob InterruptedException");
   		   }
   		   finally {
   			   semaphore.release();
   		   }
       }
       else
    	   logger.error("could not get job id to track job!");
       logger.info("gearman job submitted");
	}
	
	/**
	 * Send a gearman job using the device ID as the destination which is resolved to actual gearman worker name
	 * @param deviceId to send to
	 * @param job to send is the LBaaS job encoded in JSON
	 * @return
	 */
	public synchronized void sendJob( Long deviceId, String job) throws DeviceModelAccessException, InterruptedException {
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
	 * is extracted and used to update database for loadbalancer and device.
	 * @param message
	 */
	public synchronized void processLoadBalancerSuccess( String message) {
		logger.info("gearman job sucess");		
		try {
		    JSONObject jsonObject=new JSONObject(message);			   
		   
		    Long deviceId = (Long) jsonObject.getLong(LbaasHandler.HPCS_DEVICE);
		    String action = (String) jsonObject.get(LbaasHandler.HPCS_ACTION);
		    Long requestId = (Long) jsonObject.getLong(LbaasHandler.HPCS_REQUESTID);
		    String response = (String) jsonObject.get(LbaasHandler.HPCS_RESPONSE);
		    String errorMsg = null;
		    if (jsonObject.has(LbaasHandler.HPCS_ERROR)) 
		    	errorMsg = (String) jsonObject.get(LbaasHandler.HPCS_ERROR);
		    
		    logger.info("Device ID : " + deviceId);
		    logger.info("requestID : " + requestId);
		    logger.info("action    : " + action);
		    logger.info("response  : " + response);
		    logger.info("errorMsg  : " + errorMsg);
		    
		    // stop tracking this job
		    jobCompletedSuccess(message);
		    
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
				   else if ( action.equalsIgnoreCase(LbaasHandler.ACTION_ARCHIVE)) {
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
		        }
		        else {
			    	logger.info("worker response not PASS value : " + response + " marking LB and device as ERROR");
			    	logger.info("message : " + message);
			    				    	
			    	if ( action.equalsIgnoreCase(LbaasHandler.ACTION_UPDATE)) {
			    		
			    	   // check for log archive failure, which is treated as a non failure but we save error message			    		
			    	   try {			    	
			    		   loadbalancerModel.setStatus(LoadBalancer.STATUS_ERROR, lbId, tenantId);
			    	   }
			    	   catch (DeviceModelAccessException dme) {
				             logger.error(dme.message);
			           }
			    	 			    	
			    	   // move device to error state
			    	   try {
			    	      deviceModel.setStatus(Device.STATUS_ERROR, deviceId);
			    	   }
			    	   catch (DeviceModelAccessException dme) {
			              logger.error(dme.message);
		               }
			    	}
			    	else if( action.equalsIgnoreCase(LbaasHandler.ACTION_DELETE)) {
			    		   // move device to error state
				    	   try {
				    	      deviceModel.setStatus(Device.STATUS_ERROR, deviceId);
				    	   }
				    	   catch (DeviceModelAccessException dme) {
				              logger.error(dme.message);
			               }
			    	}
                    else if( action.equalsIgnoreCase(LbaasHandler.ACTION_ARCHIVE)) {
                    	try {		
                    	   loadbalancerModel.setStatus(LoadBalancer.STATUS_ACTIVE, lbId,tenantId);	
 			    		   loadbalancerModel.setErrMsg(errorMsg, lbId, tenantId);
 			    	   }
 			    	   catch (DeviceModelAccessException dme) {
 				             logger.error(dme.message);
 			           }
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
    public synchronized void processLoadBalancerSubmitFail( String message) {
    	logger.error("gearman job submit failure message :" + message);
    	jobCompletedFail(message);    	
    	
    	try {
			semaphore.acquire();
			String jobRequestId = getJobRequestId(message);
			if (jobRequestId!=null)
			   requestMap.remove(jobRequestId);		
		}
		catch (InterruptedException ie) {
			logger.info("job depth InterruptedException");
		}
		finally {
			semaphore.release();
		}
		
	}
    
    /**
     * process a worker failure, not much to do other than log it out
     * @param message
     */
    public synchronized void processLoadBalancerWorkerFail( String message) {
    	logger.error("gearman job worker failure message :" + message);    	
    	jobCompletedFail(message);
    	
    	try {
			semaphore.acquire();
			String jobRequestId = getJobRequestId(message);
			if (jobRequestId!=null)
			   requestMap.remove(jobRequestId);		
		}
		catch (InterruptedException ie) {
			logger.info("job depth InterruptedException");
		}
		finally {
			semaphore.release();
		}
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
