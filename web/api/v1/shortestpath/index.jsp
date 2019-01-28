<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
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

  Configuration conf = Configuration.getInstance();
  if(!conf.get("enablePathSearch", "true").equalsIgnoreCase("true")){
    ServiceMap.logError(request, response, 400, "service not available, try later");
    return;
  }
  String source = request.getParameter("source");
  String destination = request.getParameter("destination");
  if(source==null || destination==null) {
    ServiceMap.logError(request, response, 400, "missing source or destination parameters");
    return;
  }
  
  String routeType = request.getParameter("routeType");
  if(routeType!=null && !"foot_shortest".equals(routeType) && 
          !"foot_quiet".equals(routeType) && 
          !"car".equals(routeType) && 
          !"public_transport".equals(routeType) && 
          !"feet".equals(routeType)) {
    ServiceMap.logError(request, response, 400, "invalid routeType parameter (foot_shortest, foot_quiet, car, public_transport) ");    
    return;
  }
  String maxFeetKM = request.getParameter("maxFeetKM");
  String startDatetime = request.getParameter("startDatetime");

  String uid = request.getParameter("uid");
  if(uid!=null && !ServiceMap.validateUID(uid)) {
    ServiceMap.logError(request, response, 404, "invalid uid");
    return;
  }
  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  String reqFrom = request.getParameter("requestFrom");
  if(! ServiceMap.checkIP(ip, "api")) {
    ServiceMap.logError(request, response, 403,"API calls daily limit reached");
    return;
  }      

  if ("html".equals(request.getParameter("format"))) {%>
<jsp:include page="../../../mappa.jsp" > <jsp:param name="mode" value="query"/> <jsp:param name="api" value="shortestpath"/> </jsp:include>
<%
    response.setContentType("text/html; charset=UTF-8");
    ServiceMap.logAccess(request, null, null, null, source+";"+destination+";"+startDatetime+";"+routeType+";"+maxFeetKM, "api-shortestpath", null, null, null, null, "html", uid, reqFrom);
  } else { //format json
    request.setCharacterEncoding("UTF-8");
    response.setContentType("application/json; charset=UTF-8");
    response.addHeader("Access-Control-Allow-Origin", "*");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

    String srcLatLng[] = ServiceMap.parsePosition(source);
    String dstLatLng[] = ServiceMap.parsePosition(destination);
    //check if it could be an OSM node id
    if(srcLatLng==null || dstLatLng==null) {
      Pattern pattern = Pattern.compile("(OS0*)?([1-9]+[0-9]*)(NO)?");
      Matcher matcher = pattern.matcher(source);
      if (matcher.find())
      {
        srcLatLng = new String[]{matcher.group(2)};
      }
      matcher = pattern.matcher(destination);
      if (matcher.find())
      {
        dstLatLng = new String[]{matcher.group(2)};
      }

      if(srcLatLng==null || dstLatLng==null) {
        ServiceMap.logError(request, response, 400, "wrong source or destination parameters");
        return;
      }
    }
    RepositoryConnection con = ServiceMap.getSparqlConnection();

    try {
      serviceMapApi.makeShortestPath(out, con, srcLatLng, dstLatLng, startDatetime, routeType, maxFeetKM);
    }catch(IllegalArgumentException e) {
      ServiceMap.logError(request, response, 400, e.getMessage());
    }
    finally {
      con.close();
    }

    ServiceMap.logAccess(request, null, null, null, source+";"+destination+";"+startDatetime+";"+routeType+";"+maxFeetKM, "api-shortestpath", null, null, null, null, "json", uid, reqFrom);
  }  
%>