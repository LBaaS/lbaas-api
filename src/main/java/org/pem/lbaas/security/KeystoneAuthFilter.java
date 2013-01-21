package org.pem.lbaas.security;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.pem.lbaas.LbaasConfig;

import com.hp.csbu.cc.middleware.TokenAuth;

public class KeystoneAuthFilter {
	
	private static String KEYSTONE_SERVICE_ID     = "ServiceIds";
	private static String KEYSTONE_SERVER_VIP     = "ServerVIP";
	private static String KEYSTONE_SERVER_PORT    = "ServerPort";
	private static String KEYSTONE_SSL_AUTH       = "ConnSSLClientAuth";
	private static String KEYSTONE_KEYSTORE       = "Keystore";
	private static String KEYSTONE_KEYSTORE_PWD   = "KeystorePass";
	private static String KEYSTONE_TRUSTSTORE     = "Truststore";
	private static String KEYSTONE_TRUSTSTORE_PWD = "TruststorePass";
	
	private static String KEYSTONE_TENANT_ID      = "X-TENANT-ID";
	private static String KEYSTONE_TENANT_NAME    = "X-TENANT-NAME";
	private static String KEYSTONE_STATUS         = "X-IDENTITY-STATUS";
	private static String KEYSTONE_USER_ID        = "X-USER-ID";
	private static String KEYSTONE_USER_NAME      = "X-USER-NAME";
	private static String KEYSTONE_ROLES          = "X-ROLES";
	private static String KEYSTONE_AUTH_CONFIRMED = "Confirmed";
    private static String LBAAS_ADMIN             = "lbaas-admin";
	private static String LBAAS_USER              = "lbaas-user";
		
	
	public static void setupAuthFilter( ServletContextHandler context, LbaasConfig lbaasConfig) {
		FilterHolder filterHolder = new FilterHolder(TokenAuth.class); 
		filterHolder.setInitParameter(KEYSTONE_SERVICE_ID, lbaasConfig.keystoneServiceId);
		filterHolder.setInitParameter(KEYSTONE_SERVER_VIP, lbaasConfig.keystoneServerVIP); 
		filterHolder.setInitParameter(KEYSTONE_SSL_AUTH, "True"); 
		filterHolder.setInitParameter(KEYSTONE_KEYSTORE, lbaasConfig.keystoneKeystore); 
		filterHolder.setInitParameter(KEYSTONE_KEYSTORE_PWD, lbaasConfig.keystoneKeystorePwd); 
		filterHolder.setInitParameter(KEYSTONE_TRUSTSTORE, lbaasConfig.keystoneTruststore); 
		filterHolder.setInitParameter(KEYSTONE_TRUSTSTORE_PWD, lbaasConfig.keystoneTruststorePwd); 
		filterHolder.setInitParameter(KEYSTONE_SERVER_PORT, lbaasConfig.keystoneServerPort);
		filterHolder.setInitParameter("ConnTimeout", "500");			
		filterHolder.setInitParameter("ConnPoolMaxActive", "3");
		filterHolder.setInitParameter("ConnPoolMaxIdle", "3");
		filterHolder.setInitParameter("ConnPoolEvictPeriod", "600000");
		filterHolder.setInitParameter("ConnPoolMinIdleTime", "600000");	
		EnumSet<DispatcherType> all = EnumSet.of(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST);
		context.addFilter(filterHolder, "/*", all);
	}
	
	public static boolean authenticated(HttpServletRequest request) {
		return getStatus(request).equalsIgnoreCase(KEYSTONE_AUTH_CONFIRMED);
	}
	
	public static String toString(HttpServletRequest request) {
		String string = "Status: " + getStatus(request) + " TenantId: " + getTenantId(request) + " TenantName: " 
		              + getTenantName(request) + " UserId: " + getUserId(request)
		              + " UserName: " + getUserName(request) + " Roles: " + getRoles(request);
		return string;
	}
	
	public static String getTenantId(HttpServletRequest request) {
		String tenantId = (String) request.getAttribute(KEYSTONE_TENANT_ID);
		return tenantId;
	}
	
    public static String getTenantName(HttpServletRequest request) {
    	String tenantName = (String) request.getAttribute(KEYSTONE_TENANT_NAME);
		return tenantName;
	}
    
    public static String getStatus(HttpServletRequest request) {
    	String status = (String) request.getAttribute(KEYSTONE_STATUS);
    	return status;
	}
    
    public static String getUserName(HttpServletRequest request) {
    	String userName = (String) request.getAttribute(KEYSTONE_USER_NAME);
		return userName;
	}
    
    public static String getUserId(HttpServletRequest request) {
    	String userId = (String) request.getAttribute(KEYSTONE_USER_ID);
		return userId;
	}
    
    public static String getRoles(HttpServletRequest request) {
    	String roles = (String) request.getAttribute(KEYSTONE_ROLES);
		return roles;
	}
    
    public static boolean isAdmin(HttpServletRequest request) {
    	if ( getRoles(request).contains(LBAAS_ADMIN))
    		return true;
    	else
    		return false;
    }
		

}
