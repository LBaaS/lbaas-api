package org.pem.lbaas;

/**
 * pemellquist@gmail.com
 */


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

public class LbaasConfig {
	private static Logger logger = Logger.getLogger(LbaasConfig.class);
	
	public static String API_PORT       = "api-port";
	public static String ADMIN_PORT     = "admin-port";
	public static String KEYSTORE       = "keystore";
	public static String KEYSTOREPWD    = "keystorepwd";
	public static String DB_PATH        = "db-path";
	public static String DB_DRIVER      = "db-driver";
	public static String DB_USER        = "db-user";
	public static String DB_PWD         = "db-pwd";
	public static String DB_VALID_TO    = "db-valid-to";
	public static String GEARMAN_JOB_SERVER1_ADDR = "gearman-job-server1-addr";
	public static String GEARMAN_JOB_SERVER2_ADDR = "gearman-job-server2-addr";
	public static String GEARMAN_JOB_SERVER_PORT = "gearman-job-server-port";
	public static String KEYSTONE_SERVICE_ID     = "keystone-service-id";
	public static String KEYSTONE_SERVER_VIP     = "keystone-server-vip";	
	public static String KEYSTONE_KEYSTORE       = "keystone-keystore";
	public static String KEYSTONE_KEYSTOREPWD    = "keystone-keystorepwd";
	public static String KEYSTONE_TRUSTSTORE     = "keystone-truststore";
	public static String KEYSTONE_TRUSTSTOREPWD  = "keystone-truststorepwd";
	public static String KEYSTONE_SERVER_PORT    = "keystone-server-port"; 
	public static String LIMITS_MAX_LBS          = "max-lbs"; 
	public static String LIMITS_MAX_VIPS_PER_LB  = "max-vips-per-lb";
	public static String LIMITS_MAX_NODES_PER_LB = "max-nodes-per-lb";
	public static String PAGE_LIMIT              = "page-limit";
	public static String OBJECT_STORE_ENDPOINT   = "object-store-endpoint";
	public static String OBJECT_STORE_TYPE       = "object-store-type";
	public static String OBJECT_STORE_LOG_PATH   = "object-store-log-base-path";
	
		
	
	public int apiPort;
	public int adminPort;
	public String keystore;
	public String keystorePwd;
	public String dbPath;
	public String dbDriver;
	public String dbUser;
	public String dbPwd;
	public int dbValidTimeOut;
	public String gearmanServer1Addr;
	public String gearmanServer2Addr;
	public int gearmanServerPort;
	public String keystoneServiceId;
	public String keystoneServerVIP;	
	public String keystoneServerPort;
	public String keystoneKeystore;
	public String keystoneKeystorePwd;
	public String keystoneTruststore;
	public String keystoneTruststorePwd;
	public int maxLbs;
	public int maxVipsPerLb;
	public int maxNodesperLb;
	public int pageLimit;
	public String objectStoreType;
	public String objectStoreEndpoint;
	public String objectStoreLogBasePath;
	
	
	public boolean load(String filename) {		
		try {
	           XMLConfiguration serviceConfig = new XMLConfiguration(filename);
	           
	           apiPort = serviceConfig.getInt(API_PORT);
	           adminPort = serviceConfig.getInt(ADMIN_PORT);
	           keystore = serviceConfig.getString(KEYSTORE);
	           keystorePwd = serviceConfig.getString(KEYSTOREPWD);
	           
	           dbPath = serviceConfig.getString(DB_PATH);
	           dbDriver = serviceConfig.getString(DB_DRIVER);
	           dbUser = serviceConfig.getString(DB_USER);
	           dbPwd = serviceConfig.getString(DB_PWD);
	           dbValidTimeOut = serviceConfig.getInt(DB_VALID_TO);
	           
	           gearmanServer1Addr = serviceConfig.getString(GEARMAN_JOB_SERVER1_ADDR);
	           gearmanServer2Addr = serviceConfig.getString(GEARMAN_JOB_SERVER2_ADDR);
	           gearmanServerPort = serviceConfig.getInt(GEARMAN_JOB_SERVER_PORT);
	           
	           keystoneServiceId = serviceConfig.getString(KEYSTONE_SERVICE_ID);
	           keystoneServerVIP = serviceConfig.getString(KEYSTONE_SERVER_VIP);
	           keystoneServerPort = serviceConfig.getString(KEYSTONE_SERVER_PORT);
	           keystoneKeystore = serviceConfig.getString(KEYSTONE_KEYSTORE);
        	   keystoneKeystorePwd = serviceConfig.getString(KEYSTONE_KEYSTOREPWD);
        	   keystoneTruststore = serviceConfig.getString(KEYSTONE_TRUSTSTORE);
       		   keystoneTruststorePwd = serviceConfig.getString(KEYSTONE_TRUSTSTOREPWD);
       		   
       		   maxLbs = serviceConfig.getInt(LIMITS_MAX_LBS);
       		   maxVipsPerLb = serviceConfig.getInt(LIMITS_MAX_VIPS_PER_LB);
       		   maxNodesperLb = serviceConfig.getInt(LIMITS_MAX_NODES_PER_LB);
       		   
       		   pageLimit = serviceConfig.getInt(PAGE_LIMIT);
       		   
       		   objectStoreType = serviceConfig.getString(OBJECT_STORE_TYPE);
       		   objectStoreEndpoint = serviceConfig.getString(OBJECT_STORE_ENDPOINT);
       		   objectStoreLogBasePath = serviceConfig.getString(OBJECT_STORE_LOG_PATH);
	        	   
		}  
	    catch(ConfigurationException cex) {
	       logger.error(cex + "failure to open:" + filename);
	       return false;
	    }
	    	       
	    return true;
	}
	           
	public void log() {
	   
	   logger.info("API port                : " + apiPort);
	   logger.info("ADMIN port              : " + adminPort);
	   logger.info("Keystore                : " + keystore);
	   logger.info("DB Path                 : " + dbPath);
	   logger.info("DB Driver               : " + dbDriver);
	   logger.info("DB Valid Time Out       : " + dbValidTimeOut);
	   logger.info("Gearman Server1 Address : " + gearmanServer1Addr);
	   if (( Lbaas.lbaasConfig.gearmanServer2Addr!=null) && (!Lbaas.lbaasConfig.gearmanServer2Addr.isEmpty()))
	      logger.info("Gearman Server2 Address : " + gearmanServer2Addr);
	   else
		  logger.info("Gearman Server2 Address : Not Defined");
	   logger.info("Gearman Server Port     : " + gearmanServerPort);
	   logger.info("Keystone Service Id     : " + keystoneServiceId);
	   logger.info("Keystone Server VIP     : " + keystoneServerVIP);
	   logger.info("Keystone Server Port    : " + keystoneServerPort);
	   logger.info("Keystone Keystore       : " + keystoneKeystore);
	   logger.info("Keystone Truststore     : " + keystoneTruststore);
	   logger.info("Max LBs Per Tenant      : " + maxLbs);
	   logger.info("Max VIPs Per LB         : " + maxVipsPerLb);
	   logger.info("Max Nodes Per LB        : " + maxNodesperLb);
	   logger.info("Pagination Limit        : " + pageLimit);
	   logger.info("Object Store Type       : " + objectStoreType);
	   logger.info("Object Store Endpoint   : " + objectStoreEndpoint);
	   logger.info("Object Store Log Path   : " + objectStoreLogBasePath);   
	   
	}


}
