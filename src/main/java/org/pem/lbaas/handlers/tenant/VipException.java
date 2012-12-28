package org.pem.lbaas.handlers.tenant;

import org.apache.log4j.Logger;


public class VipException  extends Exception{
	private static Logger logger = Logger.getLogger(VipException.class);
	public String message=null;
	
	public 	VipException( String msg) {
	   logger.error(msg);	
	   message = new String( msg);
	}
}
