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
  response.addHeader("Access-Control-Allow-Origin", "*");
  ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

  String serviceUri = request.getParameter("serviceUri");
  if(serviceUri==null) {
    response.sendError(404, "missing serviceUri");
    return;
  }
 
  String uid = request.getParameter("uid");
  if(uid==null || uid.trim().isEmpty() || uid.equals("null")) {
    response.sendError(404, "missing uid");
    return;
  }
  if(!ServiceMap.validateUID(uid)) {
    response.sendError(404, "invalid uid");
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
  
  RepositoryConnection con = ServiceMap.getSparqlConnection();
  try {
    String serviceName = ServiceMap.getServiceName(con, serviceUri);
    if(serviceName == null)
      serviceName = ServiceMap.getServiceIdentifier(con, serviceUri);
    if(serviceName==null) {
      response.sendError(404,"invalid serviceUri (no name/id found)");
      ServiceMap.println("request invalid serviceUri "+serviceUri);
      return;
    }

    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    String reqFrom = request.getParameter("requestFrom");

    if(! ServiceMap.checkIP(ip, "api")) {
      response.sendError(403,"API calls daily limit reached");
      return;
    }      

    if(stars>0) {
      serviceMapApi.setStarsToService(uid, serviceUri, stars);
      ServiceMap.logAccess(request, null, null, null, serviceUri, "api-service-stars", null, null, null, ""+stars, null, uid, reqFrom);
    }

    String comment =  request.getParameter("comment");
    if(comment!=null) {
      comment = java.net.URLDecoder.decode(comment, "UTF-8");
      //comment = new String(comment.getBytes("iso-8859-1"), "UTF-8"); //workaround! when utf8 data is sent via GET
      serviceMapApi.addCommentToService(uid, serviceUri, serviceName, comment);
      ServiceMap.logAccess(request, null, null, null, serviceUri, "api-service-comment", null, null, null, null, null, uid, reqFrom);
    }
    else if(stars==0) {
      response.sendError(404,"stars or comment parameter should be provided");
      return;
    }
    out.println("\"OK\"");
  } finally {
    con.close();
  }
%>