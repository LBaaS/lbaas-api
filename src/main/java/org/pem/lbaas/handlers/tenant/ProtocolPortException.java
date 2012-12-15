package org.pem.lbaas.handlers.tenant;

import org.apache.log4j.Logger;


public class ProtocolPortException  extends Exception{
	private static Logger logger = Logger.getLogger(ProtocolPortException.class);
	public String message=null;
	
	public 	ProtocolPortException( String msg) {
	   logger.error(msg);	
	   message = new String( msg);
	}
}