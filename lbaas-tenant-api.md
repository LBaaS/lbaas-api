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

*This guide is intended for software developers who want to create applications using the HP Cloud Load Balancer API. It assumes the reader has a general understanding of load balancing concepts and is familiar with ReSTful web services, HTTP/1.1 conventions JSON serialization formats.*


### 1.1 API Maturity Level

*This API definition represents the HP Cloud Load Balancer as a Service in Beta release form.*


**Maturity Level**: *Experimental*

**Version API Status**: *BETA*


## 2. Architecture View


### 2.1 Overview
*The HP Cloud Load Balancer as a Service (LBaaS) set of APIs provide a RESTful interface for the creation and management of load balancers in the cloud which can be used for a variety of purposes including load balancers for your external services and internal load balancing needs. The load balancing solution is meant to provide both load balancing and high availability for your services. The LBaaS APIs are integrated within the HP Cloud ecosystem including integration with the HP Cloud identity management system and billing.*

### 2.2 Conceptual/Logical Architecture View
*To use OpenStack Load Balancers API effectively, you should understand several key concepts.*

#### 2.2.1 Load Balancer
*A load balancer is a logical device. It is used to distribute workloads between multiple back-end systems or services called nodes, based on the criteria defined as part of its configuration.*

#### 2.2.2 Virtual IP
*A virtual IP is an Internet Protocol (IP) address configured on the load balancer for use by clients connecting to a service that is load balanced. Incoming connections and requests are distributed to back-end nodes based on the configuration of the load balancer.*

#### 2.2.3 Node
*A node is a back-end device providing a service on a specified IP and port.*

### 2.3 Infrastructure Architecture View
*LBaaS fits into the HP Cloud ecosystem of APIs by utilizing the common authentication mechanisms as other HP cloud services. In order to use LBaaS a users account must be enabled to do so and all API calls require a valid HP Cloud authentication token.*

## 3. Account-level View ( STOP )
*Describe the relationship of the API with respect to the accounts, groups, tenants, regions, availability zones etc.*


### 3.1 Accounts
*Describe the structure of the user accounts, groups and tenants. Currently this might be described separately in context of Control Services, but in future each service API needs to state their usage. In future CS might support complex group hierarchies, enterprise account structures while there maybe a phased adoption by individual service APIs*

### 3.2 Regions and Availability Zones
*Describe the availability of the service API in different regions and availability zones. State plans for future expansion as well.*

**Region(s)**: region-a

**Availability Zone(s)**: az-1, az-2, az-3 

**Future Expansion**: region-b


### 3.3 Service Catalog
*Describe if the service API is exposed via the service catalog. Reference the fragment of the service catalog showing the structure.*

The service is exposed in the service catalog, as shown in the following fragment:


	{
	   service catalog fragment here
	}



## 4. REST API Specifications
*Describe the API specifications, namely the API operations, and its details, documenting the naming conventions, request and response formats, media type support, status codes, error conditions, rate limits, quota limits, and specific business rules.*

### 4.1 Service API Operations


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



