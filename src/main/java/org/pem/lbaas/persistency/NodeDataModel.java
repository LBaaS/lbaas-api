package org.pem.lbaas.persistency;


/**
 * Persistency class for LBaaS Nodes
 * @author Peter Mellquist pemellquist@gmail.com
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.sql.*;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pem.lbaas.Lbaas;
import org.pem.lbaas.datamodel.Device;
import org.pem.lbaas.datamodel.LoadBalancer;
import org.pem.lbaas.datamodel.Node;
import org.pem.lbaas.datamodel.Nodes;
import org.pem.lbaas.datamodel.VirtualIp;
import org.pem.lbaas.handlers.tenant.LbaasHandler;

public class NodeDataModel {
   private static Logger logger       = Logger.getLogger(NodeDataModel.class);
   protected Connection  dbConnection = null;
   
   protected final static String SQL_ID             = "id";
   protected final static String SQL_LBID           = "lbid";
   protected final static String SQL_ADDRESS        = "address";
   protected final static String SQL_PORT           = "port";
   protected final static String SQL_WEIGHT         = "weight";
   protected final static String SQL_ENABLED        = "enabled";
   protected final static String SQL_STATUS         = "status";
   
	
   /**
    * Get a connection to database
    * 
    * Obtain a connection to database which tries to maintain a single connection and only re-open when needed.
    * 
    * @return Connection
    * @throws DeviceModelAccessException
    */
   protected Connection dbConnect() throws NodeModelAccessException {
      if ( dbConnection==null ) {
	     try {           
            logger.info("not open, opening db connection");
	        Class.forName (Lbaas.lbaasConfig.dbDriver).newInstance ();
	        dbConnection = DriverManager.getConnection (Lbaas.lbaasConfig.dbPath, Lbaas.lbaasConfig.dbUser, Lbaas.lbaasConfig.dbPwd);
	        return dbConnection;
	      }
	      catch (Exception e) {
	         throw new NodeModelAccessException("Cannot connect to database server "+ e);
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
                   throw new NodeModelAccessException("Cannot connect to database server "+ e);
		        }      
            }
			else
			   return dbConnection;
         }
		 catch (SQLException sqe) {
            throw new NodeModelAccessException("Cannot connect to database server "+ sqe);				
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
    * Convert SQL result set to a Node object
    * 
    * @param rs
    * @return Node
    * @throws SQLException
    */
   protected Node rsToNode(ResultSet rs) throws SQLException {
      Node node = new Node();
      try {
    	  node.setId( rs.getLong(SQL_ID));
    	  node.setLbId(rs.getLong(SQL_LBID));    	  
    	  node.setAddress(rs.getString(SQL_ADDRESS));
    	  node.setPort(rs.getInt(SQL_PORT));
    	  node.setWeight(rs.getInt(SQL_WEIGHT));
    	  node.setEnabled(rs.getBoolean(SQL_ENABLED));
    	  node.setStatus(rs.getString(SQL_STATUS));    	    	      	     	  
	  }
	  catch (SQLException sqle) {                                              
         logger.error( "SQL Exception : " + sqle); 
	     throw sqle;
	  }
      return node;   
	}
	
	
   /**
    * get all Nodes for a specific condition
    * 
    * @return List of Node objects
    * @throws NodeModelAccessException if internal database error
    */
   public  synchronized List<Node> getNodes( String condition) throws NodeModelAccessException {
      List<Node> nodes = new  ArrayList<Node>();		
	  Connection conn = dbConnect();
	  Statement stmt=null;
	  String query = "SELECT * FROM nodes";
	  if (condition!=null) {
		  query = query + " WHERE " + condition;
	  }
	  try {
         stmt=conn.createStatement();
		 ResultSet rs=stmt.executeQuery(query);
		 while (rs.next()) {
		    Node node = rsToNode(rs);
			nodes.add(node);
		 }
		 rs.close();
		 stmt.close();			   
	  }
	  catch (SQLException s) {                                              
         throw new NodeModelAccessException("SQL Exception : " + s);
      }	  
      return nodes;
   }
   
   
   /**
    * get all nodes for the specific LB
    * @param lbid
    * @return List of Nodes
    */
   public  synchronized List<Node> getNodesForLb( long lbid) throws NodeModelAccessException {
	   String condition = " lbid =  " + lbid;
	   return getNodes(condition); 
   }
	
	
   /**
    * Get a Node 
    * 
    * @param  id
    * @return Node or null if not found
    * @throws DeviceModelAccessException if internal database error
    */
   public synchronized Node getNode(long nodeId) throws NodeModelAccessException {
	  logger.info("getNode"); 
      Connection conn = dbConnect();
      Statement stmt=null;	
      String query = "SELECT * FROM nodes WHERE id=" + nodeId;
      try {
         stmt=conn.createStatement();
		 ResultSet rs=stmt.executeQuery(query);
		 if ( rs.next()) {
		    Node node = rsToNode(rs);
		    rs.close();
		    stmt.close();
		    return node;
		 }
		 else {
            rs.close();
			stmt.close(); 
            return null;            // not found
		 }
      }
      catch (SQLException s) {                                              
    	  throw new NodeModelAccessException("SQL Exception : " + s);
      }		
     
	}
	
	
	
   /**
    * Delete a node based on its id
    * 
    * Deletes node and returns delete count
    * 
    * @param nodeid
    * @return 1 if deleted or 0 if not ( delete count )
    * @throws NodeModelAccessException if internal database error
    */
   public synchronized int deleteNode(long nodeId) throws NodeModelAccessException {
	  logger.info("deleteNode");  
      Connection conn = dbConnect();
      Statement stmt=null;		
      String query = "DELETE FROM nodes WHERE id=" + nodeId;
      try {
         stmt=conn.createStatement();
         int deleteCount = stmt.executeUpdate(query);
         logger.info("deleted " + deleteCount + " records");	    	  
         stmt.close();		      
         return deleteCount;
      }
      catch (SQLException s) {                                              
         throw new NodeModelAccessException("SQL Exception : " + s);
      }		
   }
   
   
   /**
    * Delete all nodes for the specified lbid
    * @param lbid
    * @return
    * @throws NodeModelAccessException
    */
   public synchronized int deleteNodes(long lbid) throws NodeModelAccessException {
	      logger.info("deleteNodes"); 
	      Connection conn = dbConnect();
	      Statement stmt=null;		
	      String query = "DELETE FROM nodes WHERE lbid=" + lbid;
	      try {
	         stmt=conn.createStatement();
	         int deleteCount = stmt.executeUpdate(query);
	         logger.info("deleted " + deleteCount + " records");	    	  
	         stmt.close();		      
	         return deleteCount;
	      }
	      catch (SQLException s) {                                              
	         throw new NodeModelAccessException("SQL Exception : " + s);
	      }		
	   }
   	

   /**
    * Create a new Node
    * 
    * @param node
    * @return new node id
    * @throws DeviceModelAccessException if internal database error
    */
   public synchronized long createNode(Node node) throws NodeModelAccessException {		
	    logger.info("createNode"); 
		long val=0;				
		Connection conn = dbConnect();
		try {
			String query = "insert into nodes (lbid,address,port,weight,enabled,status) values(?, ?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);	
			statement.setLong(1,node.getLbId());			
			statement.setString(2,node.getAddress());			
			statement.setInt(3, node.getPort());	
			statement.setInt(4, node.getWeight());
			statement.setBoolean(5, node.isEnabled());
			statement.setString(6,node.getStatus());
						
			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
	            throw new NodeModelAccessException("Creating node failed, no rows affected.");
	        }

	        ResultSet generatedKeys = statement.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            val = generatedKeys.getInt(1);
	        } else {
	            throw new NodeModelAccessException("Creating node failed, no generated key obtained.");
	        }
						
	       node.setId(val);
	    }
	    catch (SQLException s) {
	    	throw new NodeModelAccessException("SQL Exception : " + s);   
		}
	    				
		return val;	
	}
   
   
   /**
    * Change the 'enabled' condition on a node
    * @param boolean
    * @param nodeid
    * @throws NodeModelAccessException
    */
   public synchronized boolean enable( boolean value, long nodeid) throws NodeModelAccessException {
	   logger.info("enable nodeid :" + nodeid + " enabled: " + value); 
	   
		  if ( this.getNode(nodeid)==null)
			  return false;                            // id not found
		  
	      Connection conn = dbConnect();
	      try {
				String query = "UPDATE nodes SET enabled = ?, status = ? WHERE id = ?";
				PreparedStatement statement = conn.prepareStatement(query);
				statement.setBoolean(1, value);	
				statement.setString(2, value ? LbaasHandler.NODE_ONLINE : LbaasHandler.NODE_OFFLINE);
				statement.setLong(3,nodeid);
				statement.executeUpdate();
				statement.close();
				return true;
	      }
	      catch (SQLException s) {
		    	throw new NodeModelAccessException("SQL Exception : " + s);   
		  }
	      	      
   }
   
   /**
    * Create a set of nodes
    * @param nodes
    * @param lbid 
    * @throws NodeModelAccessException
    */
   public synchronized void createNodes( Nodes nodes, long lbid) throws NodeModelAccessException {
	   logger.info("createNodes"); 
	   List<Node> nodeList = nodes.getNodes();
	   for ( int z=0;z<nodeList.size();z++) {
		   Node node = nodeList.get(z);
		   node.setLbId(lbid);
		   createNode(node);
	   }		   	   
   }
		
	
}

