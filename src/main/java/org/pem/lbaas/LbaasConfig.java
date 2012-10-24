package org.pem.lbaas;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

public class LbaasConfig {
	private static Logger logger = Logger.getLogger(LbaasConfig.class);
	
	public static String API_PORT       = "api-port";
	public static String DB_PATH        = "db-path";
	public static String DB_DRIVER      = "db-driver";
	public static String DB_USER        = "db-user";
	public static String DB_PWD         = "db-pwd";
	public static String GEARMAN_JOB_SERVER_ADDR = "gearman-job-server-addr";
	public static String GEARMAN_JOB_SERVER_PORT = "gearman-job-server-port";
		
	
	public int apiPort;
	public String dbPath;
	public String dbDriver;
	public String dbUser;
	public String dbPwd;
	public String gearmanServerAddr;
	public int gearmanServerPort;
	
	
	public boolean load(String filename) {		
		try {
	           XMLConfiguration serviceConfig = new XMLConfiguration(filename);
	           
	           apiPort = serviceConfig.getInt(API_PORT);
	           
	           dbPath = serviceConfig.getString(DB_PATH);
	           dbDriver = serviceConfig.getString(DB_DRIVER);
	           dbUser = serviceConfig.getString(DB_USER);
	           dbPwd = serviceConfig.getString(DB_PWD);
	           
	           gearmanServerAddr = serviceConfig.getString(GEARMAN_JOB_SERVER_ADDR);
	           gearmanServerPort = serviceConfig.getInt(GEARMAN_JOB_SERVER_PORT);
		}  
	    catch(ConfigurationException cex) {
	       logger.error(cex + "failure to open:" + filename);
	       return false;
	    }
	    	       
	    return true;
	}
	           
	public void log() {
	   
	   logger.info("API port               : " + apiPort);
	   logger.info("DB Path                : " + dbPath);
	   logger.info("DB Driver              : " + dbDriver);
	   logger.info("Gearman Server Address : " + gearmanServerAddr);
	   logger.info("Gearman Server Port    : " + gearmanServerPort);
	      
	}


}
