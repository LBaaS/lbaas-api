package org.pem.lbaas.persistency;

/**
 * pemellquist@gmail.com
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
	
   
   protected void finalize ()  {
      dbClose();
   }

   
   protected Device rsToDevice(ResultSet rs) throws SQLException {
      Device device = new Device();
      try {
         device.setId(new Integer(rs.getInt("id")));
         device.setName(rs.getString("name"));
         device.setAddress(rs.getString("address"));
         device.setLbId(new Integer (rs.getInt("loadbalancer")));
         device.setLbType(rs.getString("type"));
         device.setStatus(rs.getString("status"));	
         device.setCreated(rs.getString("created"));
         device.setUpdated(rs.getString("updated"));
	  }
	  catch (SQLException sqle) {                                              
         logger.error( "SQL Exception : " + sqle); 
	     throw sqle;
	  }
      return device;   
	}
	
	
   
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
	
   
   public boolean setStatus(String status, Integer id) throws DeviceModelAccessException {	
      Device device = this.getDevice(id);
      if ( device == null)
    	  return false;                           // id not found
      device.setStatus( status);
      this.setDevice(device);   
      return true;	
    }
   
	
   public boolean setDevice(Device device) throws DeviceModelAccessException {
	   
	  if ( this.getDevice(device.getId())==null)
		  return false;                            // id not found
	  
      Connection conn = dbConnect();
      Statement stmt=null;
      int id = device.getId().intValue();
      String name = device.getName();
      String address = device.getAddress();
      int lbid = device.getLbId().intValue();		
      String update = "UPDATE devices SET name = '" + name + "' , address = '" + address + "' , loadbalancer = " + lbid + " WHERE id = " + id;
      try {
         stmt=conn.createStatement();
         stmt.execute(update);		      
         return true;
      }
      catch (SQLException s) {                                              
         throw new DeviceModelAccessException("SQL Exception : " + s);             
      }
   }
   
	
   public boolean markAsFree(int id) throws DeviceModelAccessException {
      Device device = this.getDevice(id);
      if (device==null)
    	  return false;                        // id not found
      device.setLbId(0);
      this.setDevice(device);
      return true;
   }
	
   public DeviceUsage getUsage() {
      DeviceUsage usage = new DeviceUsage();
      return usage;
   }
	

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

