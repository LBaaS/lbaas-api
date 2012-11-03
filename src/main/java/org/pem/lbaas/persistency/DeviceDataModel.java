package org.pem.lbaas.persistency;

/**
 * Persistency class for LBaaS Devices
 * @author Peter Mellquist pemellquist@gmail.com
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.sql.*;

import org.apache.log4j.Logger;
import org.pem.lbaas.Lbaas;
import org.pem.lbaas.datamodel.Device;
import org.pem.lbaas.datamodel.LoadBalancer;

public class DeviceDataModel {
   private static Logger logger       = Logger.getLogger(DeviceDataModel.class);
   protected Connection  dbConnection = null;
   
   public final static String SQL_ID            = "id";
   public final static String SQL_NAME          = "name";
   public final static String SQL_ADDRESS       = "address";
   public final static String SQL_LOADBALANCER  = "loadbalancer";
   public final static String SQL_TYPE          = "type";
   public final static String SQL_STATUS        = "status";
   public final static String SQL_CREATED       = "created";
   public final static String SQL_UPDATED       = "updated";
	
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
    * Convert SQL result set to a Device object
    * 
    * @param rs
    * @return Device
    * @throws SQLException
    */
   protected Device rsToDevice(ResultSet rs) throws SQLException {
      Device device = new Device();
      try {
         device.setId(new Integer(rs.getInt(SQL_ID)));
         device.setName(rs.getString(SQL_NAME));
         device.setAddress(rs.getString(SQL_ADDRESS));
         device.setLbId(new Integer (rs.getInt(SQL_LOADBALANCER)));
         device.setLbType(rs.getString(SQL_TYPE));
         device.setStatus(rs.getString(SQL_STATUS));	
         device.setCreated(rs.getString(SQL_CREATED));
         device.setUpdated(rs.getString(SQL_UPDATED));
	  }
	  catch (SQLException sqle) {                                              
         logger.error( "SQL Exception : " + sqle); 
	     throw sqle;
	  }
      return device;   
	}
	
	
   /**
    * get all devices
    * 
    * @return List of Device objects
    * @throws DeviceModelAccessException if internal database error
    */
   public  List<Device> getDevices() throws DeviceModelAccessException {
      List<Device> devices = new  ArrayList<Device>();		
	  Connection conn = dbConnect();
	  Statement stmt=null;
	  String query = "SELECT * FROM devices";
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
      return devices;
   }
	
	
   /**
    * Get a Device based on its id
    * 
    * @param id
    * @return Device or null if not found
    * @throws DeviceModelAccessException if internal database error
    */
   public Device getDevice(Integer id) throws DeviceModelAccessException {
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
      String query = "SELECT * FROM devices WHERE loadbalancer = 0";
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
   public int deleteDevice(Integer id) throws DeviceModelAccessException {
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
   public boolean setStatus(String status, Integer id) throws DeviceModelAccessException {	
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
			String query = "UPDATE devices SET name = ?, address = ?, loadbalancer = ?, status = ? WHERE id = ?";
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setString(1, device.getName());
			statement.setString(2, device.getAddress());
			statement.setInt(3, device.getLbId().intValue());
			statement.setString(4, device.getStatus());
			statement.setInt(5,device.getId().intValue());
			statement.executeUpdate();
			statement.close();
			return true;
      }
      catch (SQLException s) {
	    	throw new DeviceModelAccessException("SQL Exception : " + s);   
	  }
			
   }
   
	
   /**
    * Mark a Device as no longer being used by a Loadbalancer
    * 
    * sets the loadbalancer reference value to zero meaning no longer in use.
    * 
    * @param id
    * @return boolean
    * @throws DeviceModelAccessException if internal database error
    */
   public boolean markAsFree(int id) throws DeviceModelAccessException {
      Device device = this.getDevice(id);
      if (device==null)
    	  return false;                        // id not found
      device.setLbId(0);
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
      String queryFree = "SELECT COUNT(*) FROM devices WHERE loadbalancer = 0";
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
   public Integer createDevice(Device device) throws DeviceModelAccessException {		
		
		int val=0;				
		Connection conn = dbConnect();
		try {
			String query = "insert into devices (name,address,loadbalancer,type,created, updated, status) values(?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);	
			statement.setString(1,device.getName() );
			statement.setString(2, device.getAddress());
			statement.setInt(3,device.getLbId().intValue());
			statement.setString(4,device.getLbType());
			Date dNow = new Date();
		    SimpleDateFormat ft = new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
			statement.setString(5,ft.format(dNow));	
			statement.setString(6,ft.format(dNow));	
			statement.setString(7,device.getStatus());			
			
			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
	            throw new DeviceModelAccessException("Creating device failed, no rows affected.");
	        }

	        ResultSet generatedKeys = statement.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            val = generatedKeys.getInt(1);
	        } else {
	            throw new DeviceModelAccessException("Creating device failed, no generated key obtained.");
	        }
			
			
	       device.setId(new Integer(val));
	    }
	    catch (SQLException s) {
	    	throw new DeviceModelAccessException("SQL Exception : " + s);   
		}
				
		return new Integer(val);
	
	}
	
	
	
}

