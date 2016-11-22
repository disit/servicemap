<%@page import="org.disit.servicemap.PhotoUploadServlet"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page import="java.sql.Connection"%>
<%@page import="org.disit.servicemap.ConnectionPool"%>
<%@page trimDirectiveWhitespaces="true" %>
<%@include file="/include/parameters.jsp" %>
<%@page contentType="text/plain" pageEncoding="UTF-8"%>
<%
    //is client behind something?
    String ipAddress = request.getHeader("X-Forwarded-For");  
    if (ipAddress == null) {  
      ipAddress = ServiceMap.getClientIpAddress(request);  
    }

    if(!ipAddress.startsWith("192.168.0.") && !ipAddress.equals("127.0.0.1")) {
      response.sendError(403, "unaccessible from "+ipAddress);
      return;
    }

    Configuration conf = Configuration.getInstance();
    String uploadPath = conf.get("photoUploadPath", "/tmp/servicemap");
    
    File uploads = new File(uploadPath);
    File originals = new File(uploads,"originals");

    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("SELECT * FROM ServicePhoto ORDER BY timestamp DESC");
      ResultSet rs = st.executeQuery();
      while(rs.next()) {
        String file = rs.getString("file");
        out.println("file: "+file);

        File f=new File(originals, file);
        if(f.exists()) {
          try {
            PhotoUploadServlet.produceImages(f, uploads);
            out.println("  OK");
          }
          catch(Exception e) {
            out.println("  EXCEPTION "+e.getMessage());
            e.printStackTrace();
          }
        } else {
          out.println("  NOT EXISTS "+f.getAbsolutePath());
        }
      }
      st.close();
      connection.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }%>
