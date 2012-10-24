package org.pem.lbaas.persistency;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;

import org.apache.log4j.Logger;
import org.pem.lbaas.Lbaas;
import org.pem.lbaas.datamodel.Device;
import org.pem.lbaas.datamodel.LoadBalancer;


public class DeviceDataModel {
	
	private static Logger logger = Logger.getLogger(DeviceDataModel.class);
	
	public Connection dbConnect() {
		Connection conn = null;
        try
        {           
            Class.forName (Lbaas.lbaasConfig.dbDriver).newInstance ();
            conn = DriverManager.getConnection (Lbaas.lbaasConfig.dbPath, Lbaas.lbaasConfig.dbUser, Lbaas.lbaasConfig.dbPwd);
            return conn;
        }
        catch (Exception e)
        {
        	logger.error("Cannot connect to database server "+ e);
        	return null;
        }       
	}
	
	public void dbClose(Connection conn) {
		 if (conn != null)
         {
             try
             {
                 conn.close ();
             }
             catch (Exception e) { 
            	 logger.error("Cannot close Database Connection " + e);
             }
         }
	}
	

	public Device rsToDevice( ResultSet rs ) throws SQLException {
		   Device device = new Device();
		   try {
			   device.setId(new Integer(rs.getInt("id")));
			   device.setName(rs.getString("name"));
			   device.setAddress(rs.getString("address"));
			   device.setLbId(new Integer (rs.getInt("loadbalancer")));
			   device.setLbType(rs.getString("type"));
			   device.setStatus(rs.getString("status"));			  			   
		   }
		   catch (SQLException sqle){                                              
	           logger.error( "SQL Exception : " + sqle); 
	           throw sqle;
		   }
		
		   return device;   
	}
	
	
	public  List<Device> getDevices() {
		List<Device> devices = new  ArrayList<Device>();
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
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
		      dbClose(conn);
		   }
		   catch (SQLException s){                                              
               logger.error( "SQL Exception : " + s);
               dbClose(conn);
           }
		}
		return devices;
	}
	
	
	public Device getDevice( Integer id) {
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "SELECT * FROM devices WHERE id=" + id;
		   try {
		      stmt=conn.createStatement();
		      ResultSet rs=stmt.executeQuery(query);
		      rs.next();
		      Device device = rsToDevice(rs);
		      rs.close();
		      stmt.close();
		      dbClose(conn);
		      return device;
		   }
		   catch (SQLException s){                                              
               logger.error( "SQL Exception : " + s);
               dbClose(conn);
           }
		}
		return null;
	}
	
	
	public Device findFreeDevice() {
		
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "SELECT * FROM devices WHERE loadbalancer = 0";
		   try {
		      stmt=conn.createStatement();
		      ResultSet rs=stmt.executeQuery(query);
		      rs.next();
		      Device device = rsToDevice(rs);
		      rs.close();
		      stmt.close();
		      dbClose(conn);
		      return device;
		   }
		   catch (SQLException s){                                              
               logger.error( "SQL Exception : " + s);
               dbClose(conn);
           }
		}
		return null;
	}
	
	public boolean existsName( String name) {
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "SELECT * FROM devices WHERE name = '" + name + "'";
		   try {
		      stmt=conn.createStatement();
		      ResultSet rs=stmt.executeQuery(query);
		      if ( rs.next())
		         return true;
		   }
		   catch (SQLException s){                                              
               logger.error( "SQL Exception : " + s);
               dbClose(conn);
           }
		}
		return false;
	}
	
	
	public int deleteDevice( Integer deviceId) {
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "DELETE FROM devices WHERE id=" + deviceId;
		   try {
		      stmt=conn.createStatement();
		      int deleteCount = stmt.executeUpdate(query);
		      logger.info("deleted " + deleteCount + " records");
		    	  
		      stmt.close();
		      dbClose(conn);
		      
		      return deleteCount;
		   }
		   catch (SQLException s){                                              
               logger.error( "SQL Exception : " + s);
               dbClose(conn);
           }
		}
		return 0;
	}
	
	public boolean setStatus( String status, Integer id) {	
		   Device device = this.getDevice(id);
		   device.setStatus( status);
		   this.setDevice(device);   
		   return true;	
		}
	
	public boolean setDevice( Device device) {	
		Connection conn = dbConnect();
		Statement stmt=null;
		int id = device.getId().intValue();
		String name = device.getName();
		String status = device.getStatus();
		int lbid = device.getLbId().intValue();
		if (conn!=null) {
		   String update = "UPDATE devices SET name = '" + name + "' , status = '" + status + "' , loadbalancer = " + lbid + " WHERE id = " + id;
		   try {
		      stmt=conn.createStatement();
		      stmt.execute(update);
		      
		      return true;
		   }
		   catch (SQLException s){                                              
               logger.error( "SQL Exception : " + s);
               dbClose(conn);
           }
		}
		return false;		 

	}
	
	public boolean markAsFree( int id) {
		Device device = this.getDevice(id);
		device.setLbId(0);
		this.setDevice(device);
		return true;
	}
	
	public Integer createDevice( Device device) {		
		
		int val=0;				
		Connection conn = dbConnect();
		try {
			String query = "insert into devices (name,address,loadbalancer,type,status) values(?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);	
			statement.setString(1,device.getName() );
			statement.setString(2, device.getAddress());
			statement.setInt(3,device.getLbId().intValue());
			statement.setString(4,device.getLbType());
			statement.setString(5,device.getStatus());			
			
			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
	            throw new SQLException("Creating device failed, no rows affected.");
	        }

	        ResultSet generatedKeys = statement.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            val = generatedKeys.getInt(1);
	        } else {
	            throw new SQLException("Creating device failed, no generated key obtained.");
	        }
			
			
	       device.setId(new Integer(val));
		   dbClose(conn);
	    }
	    catch (SQLException s){
			  logger.error( "SQL Exception : " + s);
              dbClose(conn);
		}
				
		return new Integer(val);
	
	}
	
	
	
}

