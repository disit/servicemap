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

  String source = request.getParameter("source");
  String destination = request.getParameter("destination");
  if(source==null || destination==null) {
    response.sendError(400, "missing source or destination parameters");
    return;
  }
  
  String routeType = request.getParameter("routeType");
  if(routeType!=null && !"foot_shortest".equals(routeType) && !"foot_quiet".equals(routeType) && !"car".equals(routeType) && !"feet".equals(routeType)) {
    response.sendError(400, "invalid routeType parameter (foot_shortest, foot_quiet, car) ");    
    return;
  }
  String maxFeetKM = request.getParameter("maxFeetKM");
  String startDatetime = request.getParameter("startDatetime");

  String uid = request.getParameter("uid");
  if(uid!=null && !ServiceMap.validateUID(uid)) {
    response.sendError(404, "invalid uid");
    return;
  }
  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  String reqFrom = request.getParameter("requestFrom");

  if ("html".equals(request.getParameter("format"))) {%>
<jsp:include page="../../../mappa.jsp" > <jsp:param name="mode" value="query"/> <jsp:param name="api" value="shortestpath"/> </jsp:include>
<%
    response.setContentType("text/html; charset=UTF-8");
    logAccess(ip, null, ua, null, null, source+";"+destination+";"+startDatetime+";"+routeType+";"+maxFeetKM, "api-shortestpath", null, null, null, null, "html", uid, reqFrom);
  } else { //format json
    request.setCharacterEncoding("UTF-8");
    response.setContentType("application/json; charset=UTF-8");
    response.addHeader("Access-Control-Allow-Origin", "*");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

    String srcLatLng[] = ServiceMap.parsePosition(source);
    String dstLatLng[] = ServiceMap.parsePosition(destination);

    if(srcLatLng==null || dstLatLng==null) {
      response.sendError(400, "wrong source or destination parameters");
      return;
    }
    RepositoryConnection con = ServiceMap.getSparqlConnection();

    try {
      serviceMapApi.makeShortestPath(out, con, srcLatLng, dstLatLng, startDatetime, routeType, maxFeetKM);
    }catch(IllegalArgumentException e) {
      response.sendError(400, e.getMessage());
    }

    logAccess(ip, null, ua, null, null, source+";"+destination+";"+startDatetime+";"+routeType+";"+maxFeetKM, "api-shortestpath", null, null, null, null, "json", uid, reqFrom);
    con.close();
  }  
%>