---
layout: page
permalink: /api/lbaas/
title: HP Cloud My Service API
description: "HP Cloud Load Balancer Service API Specification"
keywords: "lbaas,loadbalancer"
product: LoadBalancer
published: false
author:pemellquist@gmail.com

---

# HP Cloud Load Balancer as a Service API Specification

**Date:** February 8, 2013

**Document Version:** 0.1


## 1. Overview

This guide is intended for software developers who want to create applications using the HP Cloud Load Balancer API. It assumes the reader has a general understanding of load balancing concepts and is familiar with RESTful web services, HTTP/1.1 conventions and JSON serialization formats.


### 1.1 API Maturity Level

This API definition represents the HP Cloud Load Balancer as a Service in Beta release form. Functionality represented within this specification is supported.


**Maturity Level**: *Experimental*

**Version API Status**: *BETA*


## 2. Architecture View


### 2.1 Overview
The HP Cloud Load Balancer as a Service (LBaaS) set of APIs provide a RESTful interface for the creation and management of load balancers in the cloud. These can be used for a variety of purposes including load balancers for your external services as well as internal load balancing needs. The load balancing solution is meant to provide both load balancing and high availability. The LBaaS APIs are integrated within the HP Cloud ecosystem including integration with the HP Cloud identity management system and billing systems.

### 2.2 Conceptual/Logical Architecture View
To use OpenStack Load Balancers API effectively, you should understand several key concepts.

#### 2.2.1 Load Balancer
A load balancer is a logical device. It is used to distribute workloads between multiple back-end systems or services called nodes, based on the criteria defined as part of its configuration.

#### 2.2.2 Virtual IP
A virtual IP is an Internet Protocol (IP) address configured on the load balancer for use by clients connecting to a service that is load balanced. Incoming connections and requests are distributed to back-end nodes based on the configuration of the load balancer.

#### 2.2.3 Node
A node is a back-end device providing a service on a specified IP and port.

### 2.3 Infrastructure Architecture View
LBaaS fits into the HP Cloud ecosystem of APIs by utilizing the common authentication mechanisms as other HP cloud services. In order to use LBaaS, a users account must be enabled to do so and all API calls will require a valid HP Cloud authentication token.

## 3. Account-level View 
Each HP Cloud account wishing to use LBaaS must have the account activated for LBaaS usage. Once the account is activated, the HP Cloud LBaaS service will show up in the service catelog returned during user login. In addition, LBaaS endpoints to be used will also be presented. Availability zone information may vary based on region.



### 3.1 Service Catalog
Once logged in the service catalog should list the availability of the LBaaS service, roles available and endpoints for the region you have logged into.

*The following is an example of LBaaS information within the service catalog:* 

	 "user": {
	    "id": "59267322167978",
	    "name": "lbaas_user",
	    "roles": [
	      {
	        "id": "83241756956007",
	        "serviceId": "220",
	        "name": "lbaas-user",
	        "tenantId": "22994259061625"
	      },
	      {
	        "id": "00000000004024",
	        "serviceId": "140",
	        "name": "user",
	        "tenantId": "22994259061625"
	      },
	      {
	        "id": "00000000004013",
	        "serviceId": "130",
	        "name": "block-admin",
	        "tenantId": "22994259061625"
	      }
	    ]
	  },
	  "serviceCatalog": [
	    {
	      "name": "Identity",
	      "type": "identity",
	      "endpoints": [{
	        "publicURL": "https:\/\/usa.region-b.geo-1.identity.hpcloudsvc.com:35357\/v2.0\/",
	        "region": "region-b.geo-1",
	        "versionId": "2.0",
	        "versionInfo": "https:\/\/usa.region-b.geo-1.identity-internal.hpcloudsvc.com:35357\/v2.0\/"
	      }]
	    },
	    {
	      "name": "Load Balancer",
	      "type": "hpext:lbaas",
	      "endpoints": [{
	        "tenantId": "22994259061625",
	        "publicURL": "https:\/\/usa.region-b.geo-1.lbaas.hpcloudsvc.com\/v1.1",
	        "publicURL2": "",
	        "region": "region-b.geo-1",
	        "versionId": "1.1",
	        "versionInfo": "https:\/\/usa.region-b.geo-1.lbaas.hpcloudsvc.com\/v1.1",
	        "versionList": "https:\/\/usa.region-b.geo-1.lbaas.hpcloudsvc.com"
	      }]
	    }
	]


## 4. General API Information 

This section describes operations and guidelines that are common to all LBaaS APIs.

### 4.1 Authentication
The LBaaS API uses the standard defined by OpenStack Keystone project for authentication. Please refer to the HP Cloud identity management system for more details on all authentication methods currently supported.

### 4.2 Service Access/Endpoints
As shown in the example above, logging into your region will provide you with the appropriate LBaaS endpoints to use. In addition, all supported versions are published within the service catalog. A client may chose to use any LBaaS API version listed.

### 4.3 Request/Response Types
The LBaaS API currently only supports JSON data serialization formats for request and response bodies. The request format is specified using the Content-Type header and is required for operations that have a request body. The response format should be specified in requests using the Accept header. If no response format is specified, JSON is the default.

### 4.4 Persistent Connections
By default, the API supports persistent connections via HTTP/1.1 keep-alives. All connections will be kept alive unless the connection header is set to close. In adherence with the IETF HTTP RFCs, the server may close the connection at any time and clients should not rely on this behavior.

### 4.5 Paginated Collections
Some LBaaS APIs have the capability to return collections of many resources. To reduce load on the service, list operations will return a maximum of 100 items at a time. To navigate the collection, Openstack style 'limit' and 'marker' query parameters are utilized. For example, ?limit=50&marker=1 can be set in the URI. If a marker beyond the end of a list is given, an empty list is returned.

### 4.6 Absolute Limits
Absolute limits are limits which prohibit a user from creating too many LBaaS resources. For example, maxNodesPerLoadbalancer identifies the total number of nodes that may be associated with a given load balancer. Limits for a specific tenant may be queried for using the 'GET /limits' API. This will return the limit values which apply to the tenant who made the request.

|Limited Resource          |Description                                              |
|:-------------------------|:--------------------------------------------------------|
|maxLoadBalancers          |Maximum number of load balancers allowed for this tenant |
|maxNodesPerLoadBalancer   |Maximum number of nodes allowed for each load balancer   |
|maxLoadBalancerNameLength |Maximum length allowed for a load balancer name          |
|maxVIPsPerLoadBalancer    |Maximum number of Virtual IPs for each load balancer     |



### 4.7 Faults
When issuing a LBaaS API request it is possible that an error can occur. In these cases, the system will return an HTTP error response code denoting the type of error and a response body with additional details regarding the error.(specific HTTP status codes possible are listed in each API definition)

*The following represents the JSON response body used for all faults:*

	{
	   "message":"Description of fault",
	   "details":"Details of fault",
	   "code": HTTP standard error status
	}


### 4.8 Specifying Tenant IDs
Tenant identifiers with LBaaS API URIs are not required. The tenant identifier is derived from the Openstack Keystone authentication token provided which each call. This simplifies the REST URIs to only include the base URI and the resource. For example, to retrieve a list of load balancers the request would be 'GET https://<endpoint>/loadbalancers'. All LBaaS calls will behave in this manner.



## 5. LBaaS API Resources and Methods 
The following is a summary of all supported LBaaS API resources and methods. Each resource and method is defined in detail in the subsequent sections. 

**Derived resource identifiers:**

**{baseURI}** is the endpoint URI returned in the service catalog upon logging in including the protocol, endpoint and base URI.

**{ver}** is the specific version URI returned as part of the service catalog.

**{loadbalancerId}**  is the unique identifier for a load balancer returned by the LBaaS service.

**{nodeId}** is the unique identifier for a load balancer node returned by the LBaaS service.

### 5.1 LBaaS API Summary Table

|Resource            |Operation                                 |Method |Path                                                          |
|:-------------------|:-----------------------------------------|:------|:-------------------------------------------------------------|
|versions            |Get list of all API versions              |GET    |{baseURI}/                                                    | 
|versions            |Get specific API version                  |GET    |{baseURI}/{ver}                                               |
|limits              |Get list of LBaaS limits                  |GET    |{baseURI}/{ver}/limits                                        |
|protocols           |Get list of supported protocols           |GET    |{baseURI}/{ver}/protocols                                     |
|algorithms          |Get list of supported algorithms          |GET    |{baseURI}/{ver}/algorithms                                    |
|load balancer       |Get list of all load balancers            |GET    |{baseURI}/{ver}/loadbalancers                                 | 
|load balancer       |Get a specific load balancer              |GET    |{baseURI}/{ver}/loadbalancers/{loadbalancerId}                |
|load balancer       |Create a new load balancer                |POST   |{baseURI}/{ver}/loadbalancers                                 |
|load balancer       |Update an existing load balancer          |PUT    |{baseURI}/{ver}/loadbalancers/{loadbalancerId}                | 
|load balancer       |Delete an existing load balancer          |DELETE |{baseURI}/{ver}/loadbalancers/{loadbalancerId}                | 
|node                |Get list of load balancer nodes           |GET    |{baseURI}/{ver}/loadbalancers/{loadbalancerId}/nodes          |
|node                |Get a specific load balancer node         |GET    |{baseURI}/{ver}/loadbalancers/{loadbalancerId}/nodes/{nodeId} |
|node                |Create a new load balancer node           |POST   |{baseURI}/{ver}/loadbalancers/{loadbalancerId}/nodes          |
|node                |Update a load balancer node               |PUT    |{baseURI}/{ver}/loadbalancers/{loadbalancerId}/nodes/{nodeId} |
|node                |Delete a load balancer node               |DELETE |{baseURI}/{ver}/loadbalancers/{loadbalancerId}/nodes/{nodeId} |
|virtual IP          |Get list of virtual IPs                   |GET    |{baseURI}/{ver}/loadbalancers/{loadbalancerId}virtualips      |


### 5.2 Common Request Headers 

*HTTP standard request headers*

**Accept** - Internet media types that are acceptable in the response. HP Cloud LBaaS supports the media type application/json.

**Content-Length** - The length of the request body in octets (8-bit bytes).

**Content-Type** - The Internet media type of the request body. Used with POST and PUT requests. 

*Non-standard request headers*

**X-Auth-Token** - HP Cloud authorization token.

*Example*

	GET /v1.0/loadbalancers HTTP/1.1
	Host: system.hpcloudsvc.com
	Content-Type: application/json
	Accept: application/json
	X-Auth-Token: HPAuth_2895c13b1118e23d977f6a21aa176fd2bd8a10e04b74bd8e353216072968832a
	Content-Length: 85

### 5.3 Common Response Headers 

*HTTP standard response headers*

**Content-Type** - Internet media type of the response body.

**Date** - The date and time that the response was sent.

*Example*

	HTTP/1.1 200 OK
	Content-Length: 1135
	Content-Type: application/json; charset=UTF-8
	Date: Tue, 30 Oct 2012 16:22:35 GMT



## 6. Get a List of All API Versions

### 6.1 Operation 
|Resource            |Operation                                 |Method |Path                                                          |
|:-------------------|:-----------------------------------------|:------|:-------------------------------------------------------------|
|versions            |Get list of all API versions              |GET    |{baseURI}/                                                    |

### 6.2 Description 
This method allows querying the LBaaS service for all supported versions it supports. This method is also advertised within the Keystone service catalog which is presented upon user login.

### 6.3 Request Data
None required.

### 6.4 Query Parameters Supported
None required.

### 6.5 Required HTTP Header Values
**X-Auth-Token**

### 6.6 Request Body
None required.

### 6.7 Normal Response Code 
|HTTP Status Code  |Description          |
|:-----------------|:--------------------|
|200               |OK                   |

### 6.8 Response Body
The response body contains a list of all supported versions of LBaaS.

### 6.9 Error Response Codes 
|HTTP Status Code  |Description          |
|:-----------------|:--------------------|
|401               |Unauthorized         |
|404               |Not Found            |
|405               |Not Allowed          |


### 6.10 Example
	curl -H "X-Auth-Token:HPAuth_d17a1fb4c6b5375050e80c800ef55a4987b9d" https://uswest.region-b.geo-1.lbaas.hpcloudsvc.com | python -mjson.tool

	{
    	"versions": [
        	{
            	"id": "v1.1", 
            	"links": [
                	{
                    	"href": "http://api-docs.hpcloud.com", 
                    	"rel": "self"
                	}
            	], 
            	"status": "CURRENT", 
            	"updated": "2012-12-18T18:30:02.25Z"
        	}
    	]
	}




## 7. Get Specific API Version Information

### 7.1 Operation
|Resource            |Operation                                 |Method |Path                                                          |
|:-------------------|:-----------------------------------------|:------|:-------------------------------------------------------------|
|versions            |Get specific API version                  |GET    |{baseURI}/{ver}                                               |

### 7.2 Description
This method allows querying the LBaaS service for information regarding a specific version of the LBaaS API. This method is also advertised within the Keystone service catalog which is presented upon user login.

### 7.3 Request Data
None required.

### 7.4 Query Parameters Supported
None required.

### 7.5 Required HTTP Header Values
**X-Auth-Token**

### 7.6 Request Body
None required.

### 7.7 Normal Response Code
|HTTP Status Code  |Description          |
|:-----------------|:--------------------|
|200               |OK                   |

### 7.8 Response Body
The response body contains information regarding a specific LBaaS API version.

### 7.9 Error Response Codes
|HTTP Status Code  |Description          |
|:-----------------|:--------------------|
|401               |Unauthorized         |
|404               |Not Found            |
|405               |Not Allowed          |

### 7.10 Example

	curl -H "X-Auth-Token:HPAuth_d17a1fb4e100ef55a4987b9d" https://uswest.region-b.geo-1.lbaas.hpcloudsvc.com/v1.1 | python -mjson.tool

	{
    		"version": {
        	"id": "v1.1", 
        	"links": [
            	{
                	"href": "http://api-docs.hpcloud.com", 
                	"rel": "self"
            	}
        	], 
        	"media-types": [
            	{
                	"base": "application/json"
            	}
        	], 
        	"status": "CURRENT", 
        	"updated": "2012-12-18T18:30:02.25Z"
    		}
	}



## 8. Get List of LBaaS Limits 

### 8.1 Operation
|Resource            |Operation                                 |Method |Path                                                          |
|:-------------------|:-----------------------------------------|:------|:-------------------------------------------------------------|
|limits              |Get list of LBaaS limits                  |GET    |{baseURI}/{ver}/limits                                        |

### 8.2 Description
This method allows querying the LBaaS service for a list of API limits which apply on a tenant basis. Each tenant may not utilize LBaaS API resources exceeded these limits. 

### 8.3 Request Data
None required.

### 8.4 Query Parameters Supported
None required.

### 8.5 Required HTTP Header Values
**X-Auth-Token**

### 8.6 Request Body
None required.

### 8.7 Normal Response Code
|HTTP Status Code  |Description          |
|:-----------------|:--------------------|
|200               |OK                   |

### 8.8 Response Body
The response body contains information regarding limits imposed for the tenant making the request.

### 8.9 Error Response Codes
|HTTP Status Code  |Description          |
|:-----------------|:--------------------|
|401               |Unauthorized         |
|404               |Not Found            |
|405               |Not Allowed          |

### 8.10 Example

	curl -H "X-Auth-Token:HPAuth_d17a1fb4e1e0b8857b9d" https://ntt.region-b.geo-1.lbaas.hpcloudsvc.com/v1.1/limits | python -mjson.tool

	{
	"limits": {
        	"absolute": {
            		"values": {
                		"maxLoadBalancerNameLength": 128, 
                		"maxLoadBalancers": 20, 
                		"maxNodesPerLoadBalancer": 5, 
                		"maxVIPsPerLoadBalancer": 1
            			}
        		}
    		}
	}


## 9. Get List Of Supported Protocols 

### 9.1 Operation
|Resource            |Operation                                 |Method |Path                                                          |
|:-------------------|:-----------------------------------------|:------|:-------------------------------------------------------------|
|protocols           |Get list of supported protocols           |GET    |{baseURI}/{ver}/protocols                                     |


### 9.2 Description
All load balancers must be configured with the protocol of the service which is being load balanced. The protocol selection should be based on the protocol of the back-end nodes. The current specification supports HTTP, HTTPS and TCP services.

When configuring an HTTP or HTTPS load balancer, the default port for the given protocol will be selected unless otherwise specified. For TCP load balancers, the port attribute must be provided.

### 9.3 Request Data
None required.

### 9.4 Query Parameters Supported
None required.

### 9.5 Required HTTP Header Values
**X-Auth-Token**

### 9.6 Request Body
None required.

### 9.7 Normal Response Code
|HTTP Status Code  |Description          |
|:-----------------|:--------------------|
|200               |OK                   |

### 9.8 Response Body
The response body contains the currently supported protocols and port numbers.

### 9.9 Error Response Codes
|HTTP Status Code  |Description          |
|:-----------------|:--------------------|
|401               |Unauthorized         |
|404               |Not Found            |
|405               |Not Allowed          |

### 9.10 Example

	curl -H "X-Auth-Token:HPAuth_d17a1a4987b9d" https://uswest.region-b.geo-1.lbaas.hpcloudsvc.com/v1.1/protocols | python -mjson.tool

	{
		"protocols": [
		{	
            		"name": "HTTP", 
            		"port": 80
        	}, 
        	{
            		"name": "TCP", 
            		"port": 443
        	}
    		]
	}



## 10. Get List Of Supported Algorithms 

### 10.1 Operation
|Resource            |Operation                                 |Method |Path                                                          |
|:-------------------|:-----------------------------------------|:------|:-------------------------------------------------------------|
|algorithms          |Get list of supported algorithms          |GET    |{baseURI}/{ver}/algorithms                                    |

### 10.2 Description
All load balancers utilize an algorithm that defines how traffic should be directed between back- end nodes. The default algorithm for newly created load balancers is ROUND_ROBIN, which can be overridden at creation time or changed after the load balancer has been initially provisioned.

The algorithm name is to be constant within a major revision of the load balancing API, though new algorithms may be created with a unique algorithm name within a given major revision of this API.

|Name               |Description                                                           |
|:------------------|:---------------------------------------------------------------------|
|LEAST_CONNECTIONS  |The node with the lowest number of connections will receive requests. |
|ROUND_ROBIN        |Connections are routed to each of the back-end servers in turn.       | 


### 10.3 Request Data
None required.

### 10.4 Query Parameters Supported
None required.

### 10.5 Required HTTP Header Values
**X-Auth-Token**

### 10.6 Request Body
None required.

### 10.7 Normal Response Code
| HTTP Status Code | Description         |
|:-----------------|:--------------------|
|200               |OK                   |

### 10.8 Response Body
The response body contains the currently supported algorithms.

### 10.9 Error Response Codes
| HTTP Status Code | Description         |
|:-----------------|:--------------------|
|401               |Unauthorized         |
|404               |Not Found            |
|405               |Not Allowed          |

### 10.10 Example





## Known Issues 

