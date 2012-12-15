package org.pem.lbaas.handlers.tenant;

import org.apache.log4j.Logger;


public class ProtocolAddressException  extends Exception{
	private static Logger logger = Logger.getLogger(ProtocolAddressException.class);
	public String message=null;
	
	public 	ProtocolAddressException( String msg) {
	   logger.error(msg);	
	   message = new String( msg);
	}
}