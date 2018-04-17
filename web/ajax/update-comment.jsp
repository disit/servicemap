<%@page import="org.disit.servicemap.ServiceMap"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page import="java.sql.Connection"%>
<%@page import="org.disit.servicemap.ConnectionPool"%>
<%@page trimDirectiveWhitespaces="true" %>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
/* ServiceMap.
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.

   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */

  String ipAddress = ServiceMap.getClientIpAddress(request);  
  
  Configuration conf = Configuration.getInstance();
  if(!ipAddress.startsWith(conf.get("internalNetworkIpPrefix", "192.168.0.")) && !ipAddress.equals("127.0.0.1")) {
    response.sendError(403, "unaccessible from "+ipAddress);
    return;
  }
  
  String id = request.getParameter("id");
  String status = request.getParameter("status");
  if (id != null) {
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("UPDATE ServiceComment SET status=? WHERE id=?");
      st.setString(1, status);
      st.setString(2, id);
      int n = st.executeUpdate();
      out.println(n);
      st.close();      
    } catch (SQLException ex) {
      ServiceMap.notifyException(ex);
    } finally {
      connection.close();
    }
  }
%>