<%@page import="org.disit.servicemap.ServiceMapping"%>
<%/* ServiceMap.
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
%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file= "/include/parameters.jsp" %>
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Configuration</title>
  </head>
  <body>
    <h1>Configuration</h1>
    <%
      //is client behind something?
      String ipAddress = request.getHeader("X-Forwarded-For");  
      if (ipAddress == null) {  
        ipAddress = ServiceMap.getClientIpAddress(request);  
      }
      
      if(ipAddress.startsWith("192.168.0.") || ipAddress.equals("127.0.0.1")) {
        Configuration conf=Configuration.getInstance();
        if(request.getParameter("reset")!=null) {
          conf.load();
          ServiceMapping.reset();
        }
        out.println(conf.asHtml());
        out.println("<ul>");
        out.println("<li>sparqlEndpoint="+sparqlEndpoint+"</li>");
        out.println("<li>sparqlType="+sparqlType+"</li>");
        out.println("<li>km4cVersion="+km4cVersion+"</li>");
        out.println("</ul>");
        out.println("<small>from: "+ipAddress+"</small>");
      }
      else {
        out.println("not accessible from "+ipAddress);
      }
    %>
  </body>
</html>
