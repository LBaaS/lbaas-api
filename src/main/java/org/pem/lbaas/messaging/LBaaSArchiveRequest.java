package org.pem.lbaas.messaging;

import org.pem.lbaas.Lbaas;
import org.pem.lbaas.security.KeystoneAuthFilter;

public class LBaaSArchiveRequest {
	public String objectStoreType;
	public String objectStoreEndpoint;
	public String objectStoreBasePath;
	public String objectStoreToken;
	
	public LBaaSArchiveRequest() {
		objectStoreType=null;
		objectStoreEndpoint=null;
		objectStoreBasePath=null;
		objectStoreToken=null;
	}
}
