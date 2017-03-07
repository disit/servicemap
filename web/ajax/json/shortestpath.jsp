<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@include file= "/include/parameters.jsp" %>
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
  
  request.setCharacterEncoding("UTF-8");
  response.setContentType("application/json; charset=UTF-8");
  ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

  String source = request.getParameter("source");
  String destination = request.getParameter("destination");
  if(source==null || destination==null) {
    response.sendError(404, "missing source or destination parameters");
    return;
  }
 
  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  
  String srcLatLng[] = ServiceMap.parsePosition(source);
  String dstLatLng[] = ServiceMap.parsePosition(destination);

  if(srcLatLng==null || dstLatLng==null) {
    response.sendError(404, "wrong source or destination parameters");
    return;
  }
  
  RepositoryConnection con = ServiceMap.getSparqlConnection();

  serviceMapApi.makeShortestPath(out, con, srcLatLng, dstLatLng, null, null, null);
  
  logAccess(ip, null, ua, null, null, source+";"+destination, "ui-shortestpath", null, null, null, "", null, null, null);
%>