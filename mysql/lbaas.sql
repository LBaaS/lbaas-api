# LBaaS Database schema
# pemellquist@gmail.com

DROP DATABASE IF EXISTS lbaas;
CREATE DATABASE lbaas;
USE lbaas;

# versions, used to define overall version for schema
# major version differences are not backward compatibile
create TABLE versions (
   major     INT                       NOT NULL,
   minor     INT                       NOT NULL,
   PRIMARY KEY (major) 
);
INSERT INTO versions values (2,0);

# loadbalancers
CREATE TABLE loadbalancers ( 
    id        BIGINT                   NOT NULL AUTO_INCREMENT,  # unique id for this loadbalancer, generated by DB when record is created
    name      VARCHAR(128)             NOT NULL,                 # tenant assigned load balancer name
    tenantid  VARCHAR(128)             NOT NULL,                 # tenant id who owns this loadbalancer
    protocol  VARCHAR(128)             NOT NULL,                 # loadbalancer protocol used, can be 'HTTP', 'TCP' or 'HTTPS' 
    port      INT                      NOT NULL,                 # TCP port number associated with protocol and used by loadbalancer northbound interface
    status    VARCHAR(50)              NOT NULL,                 # current status, see ATLAS API 1.1 for all possible values
    algorithm VARCHAR(80)              NOT NULL,                 # LB Algorithm in use e.g. ROUND_ROBIN, see ATLAS API 1.1 for all possible values
    created   TIMESTAMP                NOT NULL,                 # timestamp of when LB was created
    updated   TIMESTAMP                NOT NULL,                 # timestamp of when LB was last updated
    device    BIGINT                   NOT NULL,                 # reference to associated device OR '0' for unassigned
    errmsg    VARCHAR(128),                                      # optional error message which can describe details regarding LBs state, can be blank if no error state exists             
    PRIMARY KEY (id)                                             # ids are unique accross all LBs
 ) DEFAULT CHARSET utf8 DEFAULT COLLATE utf8_general_ci;
 
 #nodes
 CREATE TABLE nodes (
    id             BIGINT                NOT NULL AUTO_INCREMENT,   # unique id for this node, generated by DB when record is created
    lbid           BIGINT                NOT NULL,                  # Loadbalancer who owns this node
    address        VARCHAR(128)          NOT NULL,                  # IPV4 or IPV6 address for this node
    port           INT                   NOT NULL,                  # TCP port number associated with this node and used from LB to node
    weight         INT                   NOT NULL,                  # Node weight if applicable to algorithm used
    enabled        BOOLEAN               NOT NULL,                  # is node enabled or not
    status         VARCHAR(128)          NOT NULL,                  # status of node 'OFFLINE', 'ONLINE', 'ERROR', this value is reported by the device
    PRIMARY KEY (id)                                                # ids are unique accross all Nodes
 ) DEFAULT CHARSET utf8 DEFAULT COLLATE utf8_general_ci;

 
 # devices
CREATE TABLE devices ( 
    id             BIGINT                NOT NULL AUTO_INCREMENT,   # unique id for this device, generated by DB when record is created
    name           VARCHAR(128)          NOT NULL,                  # admin assigned device name, this is the unique gearman worker function name
    floatingIpAddr VARCHAR(128)          NOT NULL,                  # IPV4 or IPV6 address of device for floating IP
    publicIpAddr   VARCHAR(128)          NOT NULL,                  # IPV4 or IPV6 address of device for floating IP
    loadbalancers  VARCHAR(128)          NOT NULL,                  # Reference to loadbalancers using this device ( JSON array )
    az             INT                   NOT NULL,                  # availability zone in which this device exists
    type           VARCHAR(128)          NOT NULL,                  # text description of type of device, e.g. 'HAProxy'
    created        TIMESTAMP             NOT NULL,                  # timestamp of when device was created
    updated        TIMESTAMP             NOT NULL,                  # timestamp of when device was last updated
    status         VARCHAR(128)          NOT NULL,                  # status of device 'OFFLINE', 'ONLINE', 'ERROR', this value is reported by the device
    PRIMARY KEY (id)
) DEFAULT CHARSET utf8 DEFAULT COLLATE utf8_general_ci;



 
