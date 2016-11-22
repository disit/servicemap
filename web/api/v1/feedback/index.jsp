<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@include file= "/include/parameters.jsp" %>
<%
/* ServiceMap.
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
  request.setCharacterEncoding("UTF-8");
  response.setContentType("application/json; charset=UTF-8");
  response.addHeader("Access-Control-Allow-Origin", "*");
  ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

  RepositoryConnection con = ServiceMap.getSparqlConnection();

  String serviceUri = request.getParameter("serviceUri");
  if(serviceUri==null) {
    response.sendError(404, "missing serviceUri");
    return;
  }
 
  String uid = request.getParameter("uid");
  if(uid==null) {
    response.sendError(404, "missing uid");
    return;
  }
  
  int stars = 0;
  if(request.getParameter("stars")!=null) {
    stars=Integer.parseInt(request.getParameter("stars"));
    if(stars<1 || stars>5) {
      response.sendError(404, "invalid number of stars (1-5)");
      return;
    }
  }
  
  String serviceName = ServiceMap.getServiceName(con, serviceUri);
  if(serviceName == null)
    serviceName = ServiceMap.getServiceIdentifier(con, serviceUri);
  if(serviceName==null) {
    response.sendError(404,"invalid serviceUri (no name/id found)");
    System.out.println("request invalid serviceUri "+serviceUri);
    return;
  }

  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  String reqFrom = request.getParameter("requestFrom");


  if(stars>0) {
    serviceMapApi.setStarsToService(uid, serviceUri, stars);
    logAccess(ip, null, ua, null, null, serviceUri, "api-service-stars", null, null, null, ""+stars, null, uid, reqFrom);
  }
  
  String comment =  request.getParameter("comment");
  if(comment!=null) {
    comment = java.net.URLDecoder.decode(comment, "UTF-8");
    //comment = new String(comment.getBytes("iso-8859-1"), "UTF-8"); //workaround! when utf8 data is sent via GET
    serviceMapApi.addCommentToService(uid, serviceUri, serviceName, comment);
    logAccess(ip, null, ua, null, null, serviceUri, "api-service-comment", null, null, null, null, null, uid, reqFrom);
  }
  else if(stars==0) {
    response.sendError(404,"stars or comment parameter should be provided");
    return;
  }
  out.println("\"OK\"");
%>