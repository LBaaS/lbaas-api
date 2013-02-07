package org.pem.lbaas.persistency;

/**
 * Persistency class for LBaaS Devices
 * @author Peter Mellquist pemellquist@gmail.com
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.pem.lbaas.datamodel.VirtualIp;

public class DeviceDataModel {
   private static Logger logger       = Logger.getLogger(DeviceDataModel.class);
   protected Connection  dbConnection = null;
   
   protected final static String SQL_ID             = "id";
   protected final static String SQL_NAME           = "name";
   protected final static String SQL_FLOAT_ADDRESS  = "floatingIpAddr";
   protected final static String SQL_PUBLIC_ADDRESS = "publicIpAddr";
   protected final static String SQL_LOADBALANCERS  = "loadbalancers";
   protected final static String SQL_TYPE           = "type";
   protected final static String SQL_STATUS         = "status";
   protected final static String SQL_CREATED        = "created";
   protected final static String SQL_UPDATED        = "updated";
   protected final static String SQL_AZ             = "az";
   protected final static String LBIDS              = "ids";
   protected final static String LBID               = "id";
   protected final static String EMPTY_LBIDS        = "'{\"ids\":[]}'";
	
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
    * Generate JSON array of loadbalancer references
    * @param lbIds
    * @return String format of JSON loadbalancer Ids
    */
   public static JSONObject lbIdsToJson( ArrayList<Long> lbIds) throws JSONException{
	   JSONObject jsonObject = new JSONObject();
	   JSONArray jsonArray = new JSONArray();
	   for (int x=0;x<lbIds.size();x++) {
           JSONObject jsonLBid=new JSONObject();
           jsonLBid.put(LBID, lbIds.get(x));
           jsonArray.put(jsonLBid);
	   }	   
	   jsonObject.put(LBIDS,jsonArray);	
	   return jsonObject;
   }
   
   /**
    * Generate array of Longs from JSON 
    * @param lbIds
    * @return ArrayList<Long>
    */
   protected ArrayList<Long> jsonToLbIds( String lbIds) throws JSONException {
	  ArrayList<Long> values = new ArrayList<Long>();
      JSONObject jsonObject=new JSONObject(lbIds);
      JSONArray lbidArray = (JSONArray) jsonObject.get(LBIDS);
      for ( int x=0;x<lbidArray.length();x++) {				   
		   JSONObject jsonlbid = lbidArray.getJSONObject(x);
		   values.add( new Long(jsonlbid.get(LBID).toString()));
      }
	  
      return values;
   }

   /**
    * Convert SQL result set to a Device object
    * 
    * @param rs
    * @return Device
    * @throws SQLException
    */
   protected Device rsToDevice(ResultSet rs) throws SQLException, JSONException {
      Device device = new Device();
      try {
         device.setId(new Long(rs.getLong(SQL_ID)));
         device.setName(rs.getString(SQL_NAME));
         device.setAddress(rs.getString(SQL_FLOAT_ADDRESS));
         device.setAz(rs.getInt(SQL_AZ));
         device.setPublicIP(rs.getString(SQL_PUBLIC_ADDRESS));
         device.lbIds = jsonToLbIds( rs.getString(SQL_LOADBALANCERS));
         device.setLbType(rs.getString(SQL_TYPE));
         device.setStatus(rs.getString(SQL_STATUS));
         SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm'Z'");
         
         Timestamp created = rs.getTimestamp(SQL_CREATED);
         Date createdDate = new Date(created.getTime());                  
         device.setCreated( ft.format(createdDate));
         
         Timestamp updated = rs.getTimestamp(SQL_UPDATED);
         Date updatedDate = new Date(updated.getTime()); 
         device.setUpdated( ft.format(updatedDate));
	  }
	  catch (SQLException sqle) {                                              
         logger.error( "SQL Exception : " + sqle); 
	     throw sqle;
	  }
      return device;   
	}
	
	
   /**
    * get all devices with specified condition
    * 
    * @return List of Device objects
    * @throws DeviceModelAccessException if internal database error
    */
   public  List<Device> getDevices( String condition) throws DeviceModelAccessException {
      List<Device> devices = new  ArrayList<Device>();		
	  Connection conn = dbConnect();
	  Statement stmt=null;
	  String query = "SELECT * FROM devices";
	  if (condition!=null) {
		  query = query + " WHERE " + condition;
	  }
	  try {
         stmt=conn.createStatement();
		 ResultSet rs=stmt.executeQuery(query);
		 while (rs.next()) {
		    Device dev = rsToDevice(rs);
			devices.add(dev);
		 }
		 rs.close();
		 stmt.close();			   
	  }
	  catch (SQLException s) {                                              
         throw new DeviceModelAccessException("SQL Exception : " + s);
      }
	  catch (JSONException jse) {
    	  throw new DeviceModelAccessException("JSON Exception : " + jse);
      }
      return devices;
   }
   
   /**
    * get all devices with pagination using marker and limit
    * 
    * @return List of Device objects
    * @throws DeviceModelAccessException if internal database error
    */
   public  List<Device> getDevicesMarkerAndLimit(long marker, long limit) throws DeviceModelAccessException {
      List<Device> devices = new  ArrayList<Device>();		
	  Connection conn = dbConnect();
	  Statement stmt=null;
	  String query = "SELECT * FROM devices LIMIT " + limit + " OFFSET " + marker;
	  try {
         stmt=conn.createStatement();
		 ResultSet rs=stmt.executeQuery(query);
		 while (rs.next()) {
		    Device dev = rsToDevice(rs);
			devices.add(dev);
		 }
		 rs.close();
		 stmt.close();			   
	  }
	  catch (SQLException s) {                                              
         throw new DeviceModelAccessException("SQL Exception : " + s);
      }
	  catch (JSONException jse) {
    	  throw new DeviceModelAccessException("JSON Exception : " + jse);
      }
      return devices;
   }
   
   /**
    * get all devices with a specified address
    * @param address
    * @return List of Devices
    */
   public  List<Device> getDevicesWithAddr( String address) throws DeviceModelAccessException {
	   String condition = " floatingIpAddr =  '" + address + "'";
	   return getDevices(condition); 
   }
	
	
   /**
    * Get a Device based on some condition
    * 
    * @param  id
    * @return Device or null if not found
    * @throws DeviceModelAccessException if internal database error
    */
   public Device getDevice(long id) throws DeviceModelAccessException {
      Connection conn = dbConnect();
      Statement stmt=null;	
      String query = "SELECT * FROM devices WHERE id=" + id;
      try {
         stmt=conn.createStatement();
		 ResultSet rs=stmt.executeQuery(query);
		 if ( rs.next()) {
		    Device device = rsToDevice(rs);
		    rs.close();
		    stmt.close();
		    return device;
		 }
		 else {
            rs.close();
			stmt.close(); 
            return null;            // not found
		 }
      }
      catch (SQLException s) {                                              
    	  throw new DeviceModelAccessException("SQL Exception : " + s);
      }		
      catch (JSONException jse) {
    	  throw new DeviceModelAccessException("JSON Exception : " + jse);
      }
	}
	
   
   
   
	
   /**
    * Find a free Device 
    * 
    * Finds a free device to be use when building a loadbalancer OR null if none found
    * 
    * @return Device or null if none found
    * @throws DeviceModelAccessException if internal database error
    */
   public Device findFreeDevice() throws DeviceModelAccessException {		
      Connection conn = dbConnect();
      Statement stmt=null;		
      String query = "SELECT * FROM devices WHERE loadbalancers = " + EMPTY_LBIDS;
      try {
         stmt=conn.createStatement();
         ResultSet rs=stmt.executeQuery(query);
		 if ( rs.next()) {
		    Device device = rsToDevice(rs);
		    rs.close();
		    stmt.close();
		    return device;
		 }
		 else {
            rs.close();
			stmt.close();
			return null;             // no free devices found
		 }
      }
      catch (SQLException s) {                                              
         throw new DeviceModelAccessException("SQL Exception : " + s);
      }		
      catch (JSONException jse) {
    	  throw new DeviceModelAccessException("JSON Exception : " + jse);
      }
   }
	

   /**
    * Determine if a Device name already exists
    * 
    * Devices names must be unique, this method checks to see if name already exists
    * 
    * @param name
    * @return boolean
    * @throws DeviceModelAccessException if internal database error
    */
   public boolean existsName(String name) throws DeviceModelAccessException {
      Connection conn = dbConnect();
      Statement stmt=null;
      String query = "SELECT * FROM devices WHERE name = '" + name + "'";
      try {
         stmt=conn.createStatement();
         ResultSet rs=stmt.executeQuery(query);
         if ( rs.next())
            return true;
         else
            return false;
      }
      catch (SQLException s) {                                              
         throw new DeviceModelAccessException("SQL Exception : " + s);
      }
   }
	
	
   /**
    * Delete a device based on its id
    * 
    * Deletes device and returns delete count
    * 
    * @param id
    * @return 1 if deleted or 0 if not ( delete count )
    * @throws DeviceModelAccessException if internal database error
    */
   public int deleteDevice(long id) throws DeviceModelAccessException {
      Connection conn = dbConnect();
      Statement stmt=null;		
      String query = "DELETE FROM devices WHERE id=" + id;
      try {
         stmt=conn.createStatement();
         int deleteCount = stmt.executeUpdate(query);
         logger.info("deleted " + deleteCount + " records");	    	  
         stmt.close();		      
         return deleteCount;
      }
      catch (SQLException s) {                                              
         throw new DeviceModelAccessException("SQL Exception : " + s);
      }		
   }
   
   
   /**
    * Change the status on a Device
    * 
    * Changes device status returns true if changed or false if Device not found
    * 
    * @param status
    * @param id
    * @return boolean
    * @throws DeviceModelAccessException if internal database error
    */
   public boolean setStatus(String status, long id) throws DeviceModelAccessException {	
      Device device = this.getDevice(id);
      if ( device == null)
    	  return false;                           // id not found
      device.setStatus( status);
      this.setDevice(device);   
      return true;	
    }
   

   /**
    * Change a Device based on its ID
    * 
    * Change a Device based on its id, only allows change of name, address, loadbalancer and status
    * 
    * @param device
    * @return boolean
    * @throws DeviceModelAccessException if internal database error
    */
   public boolean setDevice(Device device) throws DeviceModelAccessException {
	     
	   
	  if ( this.getDevice(device.getId())==null)
		  return false;                            // id not found
	  
      Connection conn = dbConnect();
      try {
			String query = "UPDATE devices SET name = ?, floatingIpAddr = ?, loadbalancers = ?, status = ? , updated = ? WHERE id = ?";
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setString(1, device.getName());
			statement.setString(2, device.getAddress());
			statement.setString(3, lbIdsToJson( device.lbIds).toString());
			statement.setString(4, device.getStatus());
			Calendar calendar = Calendar.getInstance();
			Date dNow = calendar.getTime();
			statement.setTimestamp( 5,new java.sql.Timestamp(dNow.getTime()));	
			statement.setInt(6,device.getId().intValue());
			statement.executeUpdate();
			statement.close();
			return true;
      }
      catch (SQLException s) {
	    	throw new DeviceModelAccessException("SQL Exception : " + s);   
	  }
      catch (JSONException jse) {
    	  throw new DeviceModelAccessException("JSON Exception : " + jse);
      }
			
   }
   
	
   /**
    * Mark a Device as no longer being used by a Loadbalancer
    * 
    * remove the specified loadbalancer id from the device list
    * 
    * @param id
    * @return boolean
    * @throws DeviceModelAccessException if internal database error
    */
   public boolean markAsFree(long deviceId, long lbId) throws DeviceModelAccessException {
      Device device = this.getDevice(deviceId);
      if (device==null)
    	  return false;                        // id not found
      
      // remove referenced LB
      for (int x=0;x<device.lbIds.size();x++) 
         if (device.lbIds.get(x) == lbId)
        	 device.lbIds.remove(x);
      
      this.setDevice(device);
      return true;
   }
	
   /**
    * Get summary of device usage information
    * 
    * @return DeviceUsage
    */
   public DeviceUsage getUsage() throws DeviceModelAccessException{
      Connection conn = dbConnect();
      Statement stmt=null;
      long count=0;
      long free=0;
      String queryCount = "SELECT COUNT(*) FROM devices";
      String queryFree = "SELECT COUNT(*) FROM devices WHERE loadbalancers = " + EMPTY_LBIDS;
	  try {
		  stmt=conn.createStatement();
		  ResultSet res = stmt.executeQuery(queryCount);
		  while (res.next()){
			  count = res.getInt(1);
		  }
		  
		  res = stmt.executeQuery(queryFree);
		  while (res.next()){
			  free = res.getInt(1);
		  }
		  
	  }
	  catch (SQLException s) {
	    	throw new DeviceModelAccessException("SQL Exception : " + s);   
		}
      DeviceUsage usage = new DeviceUsage();
      usage.total = count;
      usage.free  = free;
      usage.taken = count - free;
      return usage;
   }
	

   /**
    * Create a new device
    * 
    * @param device
    * @return new device id
    * @throws DeviceModelAccessException if internal database error
    */
   public long createDevice(Device device) throws DeviceModelAccessException {		
		
		long val=0;				
		Connection conn = dbConnect();
		try {
			String query = "insert into devices (name,floatingIpAddr,publicIpAddr,loadbalancers,type,created, updated, status, az) values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);	
			statement.setString(1,device.getName() );
			statement.setString(2, device.getAddress());
			statement.setString(3, device.getPublicIP());			
			statement.setString(4,lbIdsToJson( device.lbIds).toString());
			statement.setString(5,device.getLbType());
			Calendar calendar = Calendar.getInstance();
			Date dNow = calendar.getTime();
			statement.setTimestamp(6,new java.sql.Timestamp(dNow.getTime()));	
			statement.setTimestamp(7,new java.sql.Timestamp(dNow.getTime()));	
			statement.setString(8,device.getStatus());	
			statement.setInt(9,device.getAz());
			
			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
	            throw new DeviceModelAccessException("Creating device failed, no rows affected.");
	        }

	        ResultSet generatedKeys = statement.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            val = generatedKeys.getLong(1);
	        } else {
	            throw new DeviceModelAccessException("Creating device failed, no generated key obtained.");
	        }
			
			
	       device.setId(val);
	    }
	    catch (SQLException s) {
	    	throw new DeviceModelAccessException("SQL Exception : " + s);   
		}
	    catch (JSONException jse) {
	    	  throw new DeviceModelAccessException("JSON Exception : " + jse);
	    }
				
		return val;
	
	}
	
	
	
}

