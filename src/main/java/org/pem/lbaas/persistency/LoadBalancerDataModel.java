package org.pem.lbaas.persistency;

/**
 * Persistency class for LBaaS LoadBalancers
 * @author Peter Mellquist pemellquist@gmail.com
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Date;
import java.util.StringTokenizer;
import java.sql.*;

import org.apache.log4j.Logger;
import org.pem.lbaas.Lbaas;
import org.pem.lbaas.datamodel.Device;
import org.pem.lbaas.datamodel.IpVersion;
import org.pem.lbaas.datamodel.LoadBalancer;
import org.pem.lbaas.datamodel.Node;
import org.pem.lbaas.datamodel.Nodes;
import org.pem.lbaas.datamodel.VipType;
import org.pem.lbaas.datamodel.VirtualIp;
import org.pem.lbaas.datamodel.VirtualIps;
import org.pem.lbaas.handlers.tenant.LimitsHandler;
import org.pem.lbaas.handlers.tenant.ProtocolHandler;

public class LoadBalancerDataModel {
	
	private static Logger logger = Logger.getLogger(LoadBalancerDataModel.class);
	private static NodeDataModel nodeModel = new NodeDataModel();
	private static DeviceDataModel deviceModel = new DeviceDataModel();
	protected Connection  dbConnection = null;
	private static Calendar calendar = Calendar.getInstance();
	
	protected final static String SQL_ID            = "id";
	protected final static String SQL_NAME          = "name";	
	protected final static String SQL_PROTOCOL      = "protocol";	
	public    final static String SQL_TENANTID      = "tenantid";
	protected final static String SQL_PORT          = "port";
	protected final static String SQL_STATUS        = "status";	
	protected final static String SQL_ALGORITHM     = "algorithm";	
	protected final static String SQL_CREATED       = "created";
	protected final static String SQL_UPDATED       = "updated";
	protected final static String SQL_DEVICE        = "device";
	protected final static String SQL_ERRMSG        = "errmsg";
	
	static final protected String DEFAULT_ALGO      = "ROUND_ROBIN";
	
   /**
    * Get a connection to database
    * 
    * Obtain a connection to database which tries to maintain a single connection and only re-open when needed.
    * 
    * @return Connection
    * @throws DeviceModelAccessException
    */
   protected Connection dbConnect() throws DeviceModelAccessException {
      if ( dbConnection==null ) {
	     try {           
            logger.info("not open, opening db connection");
	        Class.forName (Lbaas.lbaasConfig.dbDriver).newInstance ();
	        dbConnection = DriverManager.getConnection (Lbaas.lbaasConfig.dbPath, Lbaas.lbaasConfig.dbUser, Lbaas.lbaasConfig.dbPwd);
	        return dbConnection;
	      }
	      catch (Exception e) {
	         throw new DeviceModelAccessException("Cannot connect to database server "+ e);
	      }       
      }
      else {
         try {
            if (( dbConnection.isClosed()) || ( !dbConnection.isValid(Lbaas.lbaasConfig.dbValidTimeOut))) {
               try {          
                  logger.info("closed, re-opening db connection");
			      Class.forName (Lbaas.lbaasConfig.dbDriver).newInstance ();
			      dbConnection = DriverManager.getConnection (Lbaas.lbaasConfig.dbPath, Lbaas.lbaasConfig.dbUser, Lbaas.lbaasConfig.dbPwd);
			      return dbConnection;
			    }
			    catch (Exception e) {
                   throw new DeviceModelAccessException("Cannot connect to database server "+ e);
		        }      
            }
			else
			   return dbConnection;
         }
		 catch (SQLException sqe) {
            throw new DeviceModelAccessException("Cannot connect to database server "+ sqe);				
         }
      }
   }
      
   /**
    * Close the database connection
    */
   protected void dbClose() {
      if (dbConnection != null)
      {
         try {
            logger.info("closing db connection");
            dbConnection.close ();
          }
          catch (Exception e) { 
            logger.error("Cannot close Database Connection " + e);
          }
       }
   }
	
   
   /**
    * finalize'r closes database 
    */
   protected void finalize ()  {
      dbClose();
   }
	
	
	/**
	 * Convert a result set into a Loadbalancer object
	 * @param rs
	 * @return a LoadBalancer object
	 * @throws SQLException
	 */
	public LoadBalancer rsToLb( ResultSet rs ) throws SQLException {
	   LoadBalancer lb = new LoadBalancer();
	   try {
		   lb.setId(rs.getLong(SQL_ID));
		   lb.setName(rs.getString(SQL_NAME));
		   lb.setTenantId(rs.getString(SQL_TENANTID));
		   lb.setProtocol(rs.getString(SQL_PROTOCOL));
		   lb.setPort(rs.getInt(SQL_PORT));
		   lb.setStatus(rs.getString(SQL_STATUS));
		   lb.setAlgorithm(rs.getString(SQL_ALGORITHM));
		   		   
		   SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm'Z'");
		   
		   Timestamp created = rs.getTimestamp(SQL_CREATED);
	       Date createdDate = new Date(created.getTime());                  
	       lb.setCreated( ft.format(createdDate));
		   
		   Timestamp updated = rs.getTimestamp(SQL_UPDATED);
	       Date updatedDate = new Date(updated.getTime());                  
	       lb.setUpdated( ft.format(updatedDate));
		   
		   lb.setDevice(new Long(rs.getInt(SQL_DEVICE)));
		   
		   lb.setErrorMsg(rs.getString(SQL_ERRMSG));
		  		   		   		   		   
	   }
	   catch (SQLException sqle){                                              
           logger.error( "SQL Exception : " + sqle); 
           throw sqle;
	   }
	
	   return lb;   
	}
	
	 /**
	    * Change the status on a Loadbalancer
	    * 
	    * Changes device status returns true if changed or false if Device not found
	    * 
	    * @param status
	    * @param id
	    * @return boolean
	    * @throws DeviceModelAccessException if internal database error
	    */
	public synchronized boolean setStatus( String status, long id, String tenantId) throws DeviceModelAccessException {	
	   logger.info("setStatus : " + status + "lbID: " + id + " tenantId: " + tenantId);
	   LoadBalancer lb = this.getLoadBalancer(id,tenantId);
	   if (lb == null) {
		   throw new DeviceModelAccessException("Could not find loadbalancer id: " + id + " tenantid: " +  tenantId);
	   }
	   lb.setStatus( status);
	   // clear error msg if LB is OK
	   if ( status.equalsIgnoreCase(LoadBalancer.STATUS_ACTIVE))
		   lb.setErrorMsg(null);
	   
	   this.setLoadBalancer(lb);	
	   return true;	
	}
	
	 /**
	    * Change the errmsg on a Loadbalancer
	    * 
	    * Changes device status returns true if changed or false if Device not found
	    * 
	    * @param message
	    * @param id
	    * @return boolean
	    * @throws DeviceModelAccessException if internal database error
	    */
	public synchronized boolean setErrMsg( String msg, long id, String tenantId) throws DeviceModelAccessException {
		
	   // trim the message if its too long 
	   if ( msg.length()> LimitsHandler.LIMIT_MAX_NAME_SIZE)
		   msg = msg.substring(0,LimitsHandler.LIMIT_MAX_NAME_SIZE-1);
		
	   logger.info("setErrMsg : " + msg + "lbID: " + id + " tenantId: " + tenantId);
	   LoadBalancer lb = this.getLoadBalancer(id,tenantId);
	   if (lb == null) {
		   throw new DeviceModelAccessException("Could not find loadbalancer id: " + id + " tenantid: " +  tenantId);
	   }
	   lb.setErrorMsg(msg);
	   this.setLoadBalancer(lb);	
	   return true;	
	}
		
	
	  /**
	    * Get a Loadbalancer based on its id
	    * 
	    * @param id
	    * @return Device or null if not found
	    * @throws DeviceModelAccessException if internal database error
	    */
	public synchronized LoadBalancer getLoadBalancer( long lbId, String tenantId) throws DeviceModelAccessException {
		logger.info("getLoadBalancer");
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "SELECT * FROM loadbalancers WHERE " + SQL_ID + "=" + lbId +" AND " + SQL_TENANTID + "=\'" + tenantId + "\'";
		   try {
		      stmt=conn.createStatement();
		      ResultSet rs=stmt.executeQuery(query);
		      if (rs.next()) {
			      LoadBalancer lb = rsToLb(rs);
			      rs.close();
			      stmt.close();
			      
			      // get the nodes
			      try {
			        List<Node> listOfNodes = nodeModel.getNodesForLb(lb.getId().longValue());			      
			        Nodes nodes = new Nodes();
			  		for ( int z=0;z<listOfNodes.size();z++)
			  			nodes.getNodes().add(listOfNodes.get(z));
			        lb.setNodes(nodes);			      
			        			      
			      }
			      catch ( NodeModelAccessException nme) {
			    	  throw new DeviceModelAccessException("SQL Exception : " + nme);
			      }
			      
			      // load the VIP from the device info
				  VirtualIps virtualIps = new VirtualIps();				 						     
				  VirtualIp virtualIp = new VirtualIp();
				  // use floating ip from device
				  Device device = deviceModel.getDevice(lb.getDevice());
				  virtualIp.setAddress(device.getAddress());
				  // public IP
				  virtualIp.setType(VipType.PUBLIC);
				  //IPV4 only supported for now
                  virtualIp.setIpVersion(IpVersion.IPV_4);
                  // vip id is device id
                  virtualIp.setId(lb.getDevice());						     
                  virtualIps.getVirtualIps().add(virtualIp);				   					     	   
				  lb.setVirtualIps(virtualIps);		
				  
				  return lb;
			      
		      }
		      else {
		         rs.close();
				 stmt.close(); 
		         return null;            // not found
			  }
		   }
		   catch (SQLException s){                                              
			   throw new DeviceModelAccessException("SQL Exception : " + s);
           }
		}
		return null;
	}
	
	/**
	    * Change a LoadBalancer based on its ID
	    * 
	    * Change a LoadBalancer based on its id, only allows change of name, algorithm and status
	    * 
	    * @param device
	    * @return boolean
	    * @throws DeviceModelAccessException if internal database error
	    */
	public synchronized boolean setLoadBalancer(LoadBalancer lb)  throws DeviceModelAccessException {
		
		logger.info("setLoadBalancer name: " + lb.getName() + " alogorithm: " + lb.getAlgorithm() + " status: " + lb.getStatus() + " for lbid: " + lb.getId() + " tenantid: " + lb.getTenantId());
		Connection conn = dbConnect();
		
		try {
			String query = "UPDATE loadbalancers SET name = ?, algorithm = ?, status = ?, updated = ?, errmsg = ? WHERE id = ? AND tenantid = ?";			
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setString(1,lb.getName());
			statement.setString(2,lb.getAlgorithm());
			statement.setString(3,lb.getStatus());
			
			
			Date dNow = calendar.getTime();						
			statement.setTimestamp(4,new java.sql.Timestamp(dNow.getTime()));
			
			statement.setString(5,lb.getErrorMsg());
			
			statement.setLong(6,lb.getId());
			statement.setString(7,lb.getTenantId());
			statement.executeUpdate();
			statement.close();
			return true;
      }
      catch (SQLException s) {
	    	throw new DeviceModelAccessException("SQL Exception : " + s);   
	  }		
		
	}
	
	
	/**
	 * Create a LoadBalancer
	 * @param LoadBalancer
	 * @return new id for LoadBalancer
	 * @throws DeviceModelAccessException
	 */
	public synchronized Long createLoadBalancer( LoadBalancer lb) throws DeviceModelAccessException {	
				
		logger.info("createLoadBalancer");
		long val=0;
		
		// status
		lb.setStatus(LoadBalancer.STATUS_BUILD);
				
		Connection conn = dbConnect();
		try {
			String query = "insert into loadbalancers (name,tenantid, protocol,port,status,algorithm,created,updated,device,errmsg ) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);	
			statement.setString(1,lb.getName() );
			statement.setString(2,lb.getTenantId());
			statement.setString(3,lb.getProtocol());
			statement.setInt(4,lb.getPort());
			statement.setString(5,lb.getStatus());
			statement.setString(6,lb.getAlgorithm());  
			
			Date dNow = calendar.getTime();
			statement.setTimestamp(7,new java.sql.Timestamp(dNow.getTime()));	
			statement.setTimestamp(8,new java.sql.Timestamp(dNow.getTime()));	
			statement.setLong(9,lb.getDevice());         
			statement.setString( 10, lb.getErrorMsg());
			
			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
	            throw new DeviceModelAccessException("Creating loadbalancer failed, no rows affected.");
	        }

	        ResultSet generatedKeys = statement.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            val = generatedKeys.getInt(1);
	        } else {
	            throw new DeviceModelAccessException("Creating loadbalancer failed, no generated key obtained.");
	        }
	        	        
	        lb.setId(val);
	        
	        // create the nodes
	        try {
	        	nodeModel.createNodes( lb.getNodes(), lb.getId());	
	        }
	        catch(NodeModelAccessException nme) {
	        	throw new DeviceModelAccessException("SQL Exception : " + nme);  
	        }
						
		   
	    }
	    catch (SQLException s){
	    	throw new DeviceModelAccessException("SQL Exception : " + s);   
		}
				
		return new Long(val);
	
	}
	
	/**
	 * Get a list of LoadBalancers
	 * @return List of LoadBalancer
	 * @throws DeviceModelAccessException
	 */
	public synchronized List<LoadBalancer> getLoadBalancers(String condition) throws DeviceModelAccessException {
		
		logger.info("getLoadBalancers");
		List<LoadBalancer> lbs = new  ArrayList<LoadBalancer>();
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "SELECT * FROM loadbalancers";
		   if ( condition !=null)
			   query = query + " WHERE " + condition;
		   try {
		      stmt=conn.createStatement();
		      ResultSet rs=stmt.executeQuery(query);
		      while (rs.next()) {
		         LoadBalancer lb = rsToLb(rs);
		         
		         // get the nodes		      
			     try {
			        List<Node> listOfNodes = nodeModel.getNodesForLb(lb.getId().longValue());			      
			        Nodes nodes = new Nodes();
			  		for ( int z=0;z<listOfNodes.size();z++)
			  		   nodes.getNodes().add(listOfNodes.get(z));
			        lb.setNodes(nodes);			      			      
			      }
			      catch ( NodeModelAccessException nme) {
			    	  throw new DeviceModelAccessException("SQL Exception : " + nme);
			      }
		         
		         lbs.add(lb);
		      }
		      rs.close();
		      stmt.close();
		   }
		   catch (SQLException s){                                              
			   throw new DeviceModelAccessException("SQL Exception : " + s);
           }
		}
		return lbs;
	}
	
	/**
	 * Get a list of LoadBalancers which are using the same device
	 * @param deviceId
	 * @return
	 * @throws DeviceModelAccessException
	 */
	public  synchronized List<LoadBalancer> getLoadBalancersWithDevice(Long deviceId) throws DeviceModelAccessException {
		logger.info("getLoadBalancersWithDevice");
		String condition = SQL_DEVICE + "=" + deviceId.toString();
		return getLoadBalancers(condition);
	}

	
	/**
	 * Delete a LoadBalancer
	 * @param id of LoadBalancer to delete
	 * @return number deleted, should be 1 or 0 if not found
	 * @throws DeviceModelAccessException
	 */
	public synchronized int deleteLoadBalancer( long lbId, String tenantId) throws DeviceModelAccessException {
		logger.info("deleteLoadBalancer");
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "DELETE FROM loadbalancers WHERE " + SQL_ID + "=" + lbId +" AND " + SQL_TENANTID + "=\'" + tenantId + "\'";
		   try {
		      stmt=conn.createStatement();
		      int deleteCount = stmt.executeUpdate(query);
		      logger.info("deleted " + deleteCount + " records");		    	  
		      stmt.close();		      
		      
		      try {
		         nodeModel.deleteNodes(lbId);
		      }
		      catch ( NodeModelAccessException nme) {
		    	  throw new DeviceModelAccessException("SQL Exception : " + nme);
		      }
		      
		      
		      return deleteCount;
		   }
		   catch (SQLException s){                                              
			   throw new DeviceModelAccessException("SQL Exception : " + s);
           }
		}
		return 0;
	}
		
	
}
