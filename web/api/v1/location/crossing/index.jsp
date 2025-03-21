<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@page import="org.disit.servicemap.api.CheckParameters"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file= "/include/parameters.jsp" %>
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

    response.setContentType("application/json; charset=UTF-8");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

    String msg;
    String position = request.getParameter("position");
    if(position!=null && (msg=CheckParameters.checkLatLng(position))!=null) {
      ServiceMap.logError(request, response, 400, "invalid position: "+msg);
      return;      
    }
    String maxResults = request.getParameter("maxResults");
    if(maxResults==null)
      maxResults="10";
      
    double maxDist = 0.01;
    String maxDists = request.getParameter("maxDists");
    if(maxDists!=null)
      maxDist = Double.parseDouble(maxDists);
      
    String sortByDist = request.getParameter("sortByDistance");
    if(sortByDist==null)
      sortByDist = "true";
    
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

    try {
      if(position == null) {
        ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"missing 'position' parameter");
      }
      else {
          String c[] = position.split(";");
          if(c.length>=2) {
            String lat=c[0];
            String lng=c[1];
            RepositoryConnection con = ServiceMap.getSparqlConnection();
            int results = serviceMapApi.queryCrossing(out, con, lat, lng, maxDist);
            ServiceMap.updateResultsPerIP(ip, "api", results);
            ServiceMap.logAccess(request, null, position, null, null, "api-location", null, null, null, null, "json", uid, reqFrom);
            con.close();
          }
          else
            ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"invalid 'position' parameter (missing lat;long)");
      }
    } catch(IllegalArgumentException e) {
      ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
%>
