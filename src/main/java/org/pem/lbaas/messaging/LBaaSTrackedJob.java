package org.pem.lbaas.messaging;

public class LBaaSTrackedJob {
     public long lifeInSecs;
     public String trackedJob;
     
     public LBaaSTrackedJob( String job ) {
    	 lifeInSecs=0;
    	 trackedJob=job;
     }
     
}
