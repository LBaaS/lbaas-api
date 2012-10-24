package org.pem.lbaas.datamodel;

import java.io.Serializable;


public class Device implements Serializable {

	 private final static long serialVersionUID = 532511515L;
	 protected Integer id;
	 protected String name;
	 protected String address;
	 protected String status;
	 
	 // PEM Added
	 protected String lbType;  // type of LB, e.g. "HAProxy"
	 protected Integer lbId;   // reference to loadbalancer or 0 means not yet assigned
	 public static String STATUS_OFFLINE    = "OFFLINE";           // device is offline not functioning as an LB
	 public static String STATUS_ONLINE     = "ONLINE";            // device is online and functional as defined by associated LB
	 public static String STATUS_ERROR      = "ERROR";             // device is in an error state and not functional
	 
	 public Integer getId() {
	    return id;
	 }
	   
     public void setId(Integer value) {
	    this.id = value;
	 }
	      
     public String getName() {
         return name;
     }
    
     public void setName(String value) {
         this.name = value;
     }
     
     public String getAddress() {
         return address;
     }
    
     public void setAddress(String value) {
         this.address = value;
     }
     
     // PEM added     
     public void setStatus( String deviceStatus) {
    	 status = deviceStatus;
     }
     
     public String getStatus() {
    	 return status;
     }
     
     public void setLbType( String lbtype) {
    	 lbType = lbtype;
     }
     
     public String getLbType() {
    	 return lbType;
     }
     
     public void setLbId( Integer lbid) {
    	 lbId = lbid;
     }
     
     public Integer getLbId() {
    	 return lbId;
     }
     
     
}
