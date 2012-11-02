package org.pem.lbaas.persistency;

import org.apache.log4j.Logger;

public class DeviceModelAccessException  extends Exception{
	private static Logger logger = Logger.getLogger(DeviceModelAccessException.class);
	public String message=null;
	
	public 	DeviceModelAccessException( String msg) {
	   logger.error(msg);	
	   message = new String( msg);
	}
}