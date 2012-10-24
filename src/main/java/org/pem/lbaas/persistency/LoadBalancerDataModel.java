package org.pem.lbaas.persistency;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.StringTokenizer;
import java.sql.*;

import org.apache.log4j.Logger;
import org.pem.lbaas.Lbaas;
import org.pem.lbaas.datamodel.IpVersion;
import org.pem.lbaas.datamodel.LoadBalancer;
import org.pem.lbaas.datamodel.Node;
import org.pem.lbaas.datamodel.Nodes;
import org.pem.lbaas.datamodel.VipType;
import org.pem.lbaas.datamodel.VirtualIp;
import org.pem.lbaas.datamodel.VirtualIps;

public class LoadBalancerDataModel {
	
	private static Logger logger = Logger.getLogger(LoadBalancerDataModel.class);

	static final protected String DEFAULT_PROTOCOL_HTTP    = "HTTP";
	static final protected int    DEFAULT_PORT_HTTP        = 80;	
	static final protected String DEFAULT_ALGO             = "ROUND_ROBIN";
	
	public Connection dbConnect()  {
		Connection connection = null;				
        try
        {           
            Class.forName (Lbaas.lbaasConfig.dbDriver).newInstance ();
            connection = DriverManager.getConnection (Lbaas.lbaasConfig.dbPath, Lbaas.lbaasConfig.dbUser, Lbaas.lbaasConfig.dbPwd);
            return connection;
        }
        catch (Exception e)
        {
        	logger.error("Cannot connect to database server exception :"+ e);
        	return null;
        }       
	}
	
	public void dbClose(Connection connection) {
		 if (connection != null)
         {
             try
             {
            	 connection.close ();
             }
             catch (Exception e) { 
            	 logger.error("Cannot close Database Connection exception :" + e);            	 
             }
         }
	}
	
	public String encodeNodeDBFields( LoadBalancer lb) {
		String nodesString = new String();
		Nodes nodes = lb.getNodes();
		   if (nodes != null) {
			   List<Node> nodeList = nodes.getNodes();
			   for ( int y=0;y<nodeList.size();y++) {
				   nodesString += nodeList.get(y).getAddress();
				   nodesString += ":";
				   nodesString += nodeList.get(y).getPort().toString();
				   nodesString += ":";
				   nodesString += nodeList.get(y).getStatus();
				   nodesString += ":";
				   nodesString += nodeList.get(y).getId().toString();
				   nodesString += ",";
				   
			   }	
			   nodesString = nodesString.substring(0, nodesString.length()-1);
		   }		   		
		
		return nodesString;
	}
	
	public String encodeVIPDBFields( LoadBalancer lb) {
		String vipString = new String();
		VirtualIps vips = lb.getVirtualIps();
		   if (vips != null) {
			   List<VirtualIp> vipList = vips.getVirtualIps();
			   for ( int y=0;y<vipList.size();y++) {
				   vipString += vipList.get(y).getAddress();
				   vipString += ":";
				   vipString += vipList.get(y).getType().toString();
				   vipString += ":";
				   vipString += vipList.get(y).getIpVersion().toString();
				   vipString += ":";
				   vipString += vipList.get(y).getId().toString();
				   vipString += ",";
				   
			   }	
			   vipString = vipString.substring(0, vipString.length()-1);
		   }		   		
		  
		return vipString;
	}
	
	public LoadBalancer rsToLb( ResultSet rs ) throws SQLException {
	   LoadBalancer lb = new LoadBalancer();
	   try {
		   lb.setId(new Integer(rs.getInt("id")));
		   lb.setName(rs.getString("name"));
		   lb.setProtocol(rs.getString("protocol"));
		   lb.setPort(rs.getInt("port"));
		   lb.setStatus(rs.getString("status"));
		   lb.setAlgorithm(rs.getString("algorithm"));
		   lb.setCreated(rs.getString("created"));
		   lb.setUpdated(rs.getString("updated"));
		   lb.setDevice(new Integer(rs.getInt("device")));
		  
		   // nodes
		   Nodes nodes = new Nodes();
		   String nodeString = rs.getString("nodes");
		   StringTokenizer stNodes = new StringTokenizer(nodeString, ",");
		   while(stNodes.hasMoreTokens()) { 
		      String fields = stNodes.nextToken(); 
		      //logger.info("node fields :" + fields);
		      StringTokenizer stNodeFields = new StringTokenizer(fields, ":");
		      while(stNodeFields.hasMoreTokens()) { 
			     String address = stNodeFields.nextToken();
			     String port = stNodeFields.nextToken();
			     String status = stNodeFields.nextToken();
			     String id = stNodeFields.nextToken();			     
			     Node node = new Node();
			     node.setAddress(address);
			     node.setPort(Integer.parseInt(port));
			     node.setStatus(status);
			     node.setId(new Integer(id));
			     nodes.getNodes().add(node);
		      }
		   }
		   lb.setNodes(nodes);	
		   
		   // vips
		   VirtualIps virtualIps = new VirtualIps();
		   String vipString = rs.getString("vips");
		   StringTokenizer stVips = new StringTokenizer(vipString, ",");
		   while(stVips.hasMoreTokens()) { 
			      String fields = stVips.nextToken(); 
			      //logger.info("vip fields :" + fields);
			      StringTokenizer stVipFields = new StringTokenizer(fields, ":");
			      while(stVipFields.hasMoreTokens()) { 
				     String address = stVipFields.nextToken();
				     String type = stVipFields.nextToken();
				     String version = stVipFields.nextToken();
				     String id = stVipFields.nextToken();	
				     
				     VirtualIp virtualIp = new VirtualIp();
				     
				     virtualIp.setAddress(address);
				     
				     if (type.equalsIgnoreCase(VipType.PUBLIC.toString()))
				         virtualIp.setType(VipType.PUBLIC);
				     else
				    	 virtualIp.setType(VipType.PRIVATE);
				     
				     if (version.equalsIgnoreCase(IpVersion.IPV_4.toString()))
				        virtualIp.setIpVersion(IpVersion.IPV_4);
				     else
				    	 virtualIp.setIpVersion(IpVersion.IPV_6);
				     
				     virtualIp.setId(new Integer(id));
				     
				     virtualIps.getVirtualIps().add(virtualIp);				   
			      }
			   }
		   
		   
		   lb.setVirtualIps(virtualIps);

		   
	   }
	   catch (SQLException sqle){                                              
           logger.error( "SQL Exception : " + sqle); 
           throw sqle;
	   }
	
	   return lb;   
	}
	
	
	public boolean setStatus( String status, Integer id) {			
	   LoadBalancer lb = this.getLoadBalancer(id);
	   lb.setStatus( status);
	   this.setLoadBalancer(lb);	
	   return true;	
	}
		
	
	public LoadBalancer getLoadBalancer( Integer lbId) {
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "SELECT * FROM loadbalancers WHERE id=" + lbId;
		   try {
		      stmt=conn.createStatement();
		      ResultSet rs=stmt.executeQuery(query);
		      rs.next();
		      LoadBalancer lb = rsToLb(rs);
		      rs.close();
		      stmt.close();
		      dbClose(conn);
		      return lb;
		   }
		   catch (SQLException s){                                              
               logger.error( "SQL Exception : " + s);
               dbClose(conn);
           }
		}
		return null;
	}
	
	public boolean setLoadBalancer( LoadBalancer lb) {	
		Connection conn = dbConnect();
		Statement stmt=null;
		int id = lb.getId().intValue();
		String name = lb.getName();
		String status = lb.getStatus();
		String algorithm = lb.getAlgorithm();
		if (conn!=null) {
		   String update = "UPDATE loadbalancers SET name = '" + name + "' , algorithm = '" + algorithm + "' , status = '" + status  + "'  WHERE id = " + id;
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
	
	public Integer createLoadBalancer( LoadBalancer lb) {	
		
		logger.info("createLoadBalancer");
				
		int val=0;
		
		// protocol
		if (lb.getProtocol() == null)
			lb.setProtocol(DEFAULT_PROTOCOL_HTTP);
		
		// port
		if (lb.getPort() == null)
			if ( lb.getProtocol().equalsIgnoreCase(DEFAULT_PROTOCOL_HTTP))
			   lb.setPort(new Integer(DEFAULT_PORT_HTTP));
		
		// algo
		if ( lb.getAlgorithm() == null)
			lb.setAlgorithm(DEFAULT_ALGO);
		
		// status
		lb.setStatus(LoadBalancer.STATUS_BUILD);
		
		// created and updated
		Date dNow = new Date();
	    SimpleDateFormat ft = new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
		lb.setCreated(ft.format(dNow));
		lb.setUpdated(ft.format(dNow));
		//logger.info("create time : " + lb.getCreated());
		
		Connection conn = dbConnect();
		try {
			String query = "insert into loadbalancers (name,tenantid, protocol,port,status,algorithm,vips,nodes,created,updated,device ) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);	
			statement.setString(1,lb.getName() );
			statement.setString(2,"0");
			statement.setString(3,lb.getProtocol());
			statement.setInt(4,lb.getPort());
			statement.setString(5,lb.getStatus());
			statement.setString(6,lb.getAlgorithm());
			statement.setString(7,encodeVIPDBFields(lb));   
			statement.setString(8, encodeNodeDBFields(lb));
			statement.setString(9,lb.getCreated());
			statement.setString(10,lb.getUpdated());
			statement.setInt(11,lb.getDevice());         
			
			int affectedRows = statement.executeUpdate();
			if (affectedRows == 0) {
	            throw new SQLException("Creating loadbalancer failed, no rows affected.");
	        }

	        ResultSet generatedKeys = statement.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            val = generatedKeys.getInt(1);
	        } else {
	            throw new SQLException("Creating loadbalancer failed, no generated key obtained.");
	        }
			
			
		   lb.setId(new Integer(val));
		   dbClose(conn);
	    }
	    catch (SQLException s){
			  logger.error( "SQL Exception : " + s);
              dbClose(conn);
		}
				
		return new Integer(val);
	
	}
	
	
	public  List<LoadBalancer> getLoadBalancers() {
		List<LoadBalancer> lbs = new  ArrayList<LoadBalancer>();
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "SELECT * FROM loadbalancers";
		   try {
		      stmt=conn.createStatement();
		      ResultSet rs=stmt.executeQuery(query);
		      while (rs.next()) {
		         LoadBalancer lb = rsToLb(rs);
		         lbs.add(lb);
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
		return lbs;
	}

	
	public int deleteLoadBalancer( Integer lbId) {
		Connection conn = dbConnect();
		Statement stmt=null;
		if (conn!=null) {
		   String query = "DELETE FROM loadbalancers WHERE id=" + lbId;
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
		
	
}
