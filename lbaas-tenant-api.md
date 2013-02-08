---
layout: page
permalink: /api/lbaas/
title: HP Cloud My Service API
description: "HP Cloud Load Balancer Service API Specification"
keywords: "lbaas,loadbalancer"
product: LoadBalancer
published: false

---

# HP Cloud Load Balancer as a Service API Specification

**Date:** February 8, 2013

**Document Version:** 0.1


## 1. Overview

*This guide is intended for software developers who want to create applications using the HP Cloud Load Balancer API. It assumes the reader has a general understanding of load balancing concepts and is familiar with RESTful web services, HTTP/1.1 conventions and JSON serialization formats.*


### 1.1 API Maturity Level

*This API definition represents the HP Cloud Load Balancer as a Service in Beta release form. Functionality represented within this specification is supported.*


**Maturity Level**: *Experimental*

**Version API Status**: *BETA*


## 2. Architecture View


### 2.1 Overview
*The HP Cloud Load Balancer as a Service (LBaaS) set of APIs provide a RESTful interface for the creation and management of load balancers in the cloud. These can be used for a variety of purposes including load balancers for your external services as well as internal load balancing needs. The load balancing solution is meant to provide both load balancing and high availability. The LBaaS APIs are integrated within the HP Cloud ecosystem including integration with the HP Cloud identity management system and billing systems.*

### 2.2 Conceptual/Logical Architecture View
*To use OpenStack Load Balancers API effectively, you should understand several key concepts.*

#### 2.2.1 Load Balancer
*A load balancer is a logical device. It is used to distribute workloads between multiple back-end systems or services called nodes, based on the criteria defined as part of its configuration.*

#### 2.2.2 Virtual IP
*A virtual IP is an Internet Protocol (IP) address configured on the load balancer for use by clients connecting to a service that is load balanced. Incoming connections and requests are distributed to back-end nodes based on the configuration of the load balancer.*

#### 2.2.3 Node
*A node is a back-end device providing a service on a specified IP and port.*

### 2.3 Infrastructure Architecture View
*LBaaS fits into the HP Cloud ecosystem of APIs by utilizing the common authentication mechanisms as other HP cloud services. In order to use LBaaS, a users account must be enabled to do so and all API calls will require a valid HP Cloud authentication token.*

## 3. Account-level View 
*Each HP Cloud account wishing to use LBaaS must have the account activated for LBaaS usage. Once the account is activated, the HP Cloud LBaaS service will show up in the service catelog returned during user login. In addition, LBaaS endpoints to be used will also be presented. Availability zone information may vary based on region.*



### 3.1 Service Catalog
*Once logged in the service catalog should list the availability of the LBaaS service, roles available and endpoints for the region you have logged into.*

The following is an example of LBaaS information within the service catalog: 

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


## 4. REST API Specification

### 4.1 General API Information
*This section describes operations and guidelines that are common to all LBaaS APIs.*

#### 4.1.1 Authentication
*The LBaaS API uses the standard defined by OpenStack Keystone project for authentication. Please refer to the HP Cloud identity management system for more details on all authentication methods currently supported.*

#### 4.1.2 Service Access/Endpoints
*As shown in the example above, logging into your region will provide you with the appropriate LBaaS endpoints to use. In addition, all supported versions are published within the service catalog. A client may chose to use any LBaaS API version listed.* 

#### 4.1.3 Request/Response Types
*The LBaaS API currently only supports JSON data serialization formats for request and response bodies. The request format is specified using the Content-Type header and is required for operations that have a request body. The response format should be specified in requests using the Accept header. If no response format is specified, JSON is the default.*

#### 4.1.4 Persistent Connections
*By default, the API supports persistent connections via HTTP/1.1 keep-alives. All connections will be kept alive unless the connection header is set to close. In adherence with the IETF HTTP RFCs, the server may close the connection at any time and clients should not rely on this behavior.*

#### 4.1.5 Paginated Collections
*Some LBaaS APIs have the capability to return collections of many resources. To reduce load on the service, list operations will return a maximum of 100 items at a time. To navigate the collection, Openstack style 'limit' and 'marker' query parameters are utilized. For example, ?limit=50&marker=1 can be set in the URI. If a marker beyond the end of a list is given, an empty list is returned.*

#### 4.1.6 Absolute Limits
*Absolute limits are limits which prohibit a user from creating too many LBaaS resources. For example, maxNodesPerLoadbalancer identifies the total number of nodes that may be associated with a given load balancer. Limits for a specific tenant may be queried for using the 'GET /limits' API. This will return the limit values which apply to the tenant who made the request.*

| Limited Resource     | Description                   |
|:---------------------|:------------------------------|
| blah                 |blah                           |



#### 4.1.7 Faults

#### 4.1.8 Specifing Tenant IDs

### 4.2 LBaaS API Summary Table



## 5. STOP

**Host**: https://az-1.region-a.geo-1.compute.hpcloudsvc.com

**BaseUri**: {Host}/v1.1/{tenant_id}

**Admin URI**: N/A

| Resource | Operation            | HTTP Method | Path                   | JSON/XML Support? | Privilege Level |
| :------- | :------------------- | :---------- | :--------------------- | :---------------- | :-------------: |
| R1       | [Short desc. of the call](#anchor_link) | {GET/POST/DELETE/PUT} | {BaseURI}/{path} | {Y/N}    |                 |


### 4.2 Common Request Headers
*List the common response headers i.e. X-Auth-Token, Content-Type, Content-Length, Date etc.*

### 4.3 Common Response Headers
*List the common response headers i.e. Content-Type, Content-Length, Connection, Date, ETag, Server, etc.*

### 4.4 Service API Operation Details
*The following section, enumerates each resource and describes each of its API calls as listed in the Service API Operations section, documenting the naming conventions, request and response formats, status codes, error conditions, rate limits, quota limits, and specific business rules.*

#### 4.4.1 {Resource}
*Describe the resource and what information they provide. Then enumerate all the API method calls below.*

**Status Lifecycle**

N/A

**Rate Limits**

N/A

**Quota Limits**

N/A

**Business Rules**

None.

##### 4.4.1.1 {Short description of the method call}
**{HTTP Verb: GET, POST, DELETE, PUT} {path only, no root path}**

*Description about the method call*

**Request Data**

*Specify all the required/optional url and data parameters for the given method call.*

**URL Parameters**

*Pagination concepts can be described here, i.e. marker, limit, count etc. Filtering concepts can be described as well i.e. prefix, delimiter etc.*

* *name_of_attribute* - {data type} - {description of the attribute}
* *name_of_attribute* - {data type} - {description of the attribute}
* *name_of_attribute* (Optional) - {data type} - {description of the attribute}

**Data Parameters**

*List all the attributes that comprises the data structure*

* *name_of_attribute* - {data type} - {description of the attribute}
* *name_of_attribute* - {data type} - {description of the attribute}
* *name_of_attribute* (Optional) - {data type} - {description of the attribute}

*Either put 'This call does not require a request body' or include JSON/XML request data structure*

JSON


	{json data structure here}


XML


	<xml data structure here>


**Success Response**

*Specify the status code and any content that is returned.*

**Status Code**

200 - OK

**Response Data**

*Either put 'This call does not require a request body' or include JSON/XML response data structure*

JSON


	{json data structure here}


XML


	<xml data structure here>


**Error Response**

*Enumerate all the possible error status codes and any content that is returned.*

**Status Code**

500 - Internal Server Error

**Response Data**

JSON


	{"cloudServersFault": {"message": "Server Error, please try again later.", "code": 500}}


XML


	<xml data structure here>


**Curl Example**


	curl -i -H "X-Auth-Token: <Auth_Token>" {BaseUri}/{path}


**Additional Notes**

*Specify any inconsistencies, ambiguities, issues, commentary or discussion relevant to the call.*

## 5. Changes from Cloud 1.0 API to Cloud 1.1 API

*Put down a list of things that has changed from the 1.0 specs to 1.1 specs. If your service did not have a 1.0 version, please remove this section.*

## 6. Glossary

*Put down definitions of terms and items that need explanation.*



