<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
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
    response.addHeader("Access-Control-Allow-Origin", "*");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

    String selection = request.getParameter("selection");
    String uid = request.getParameter("uid");
    if(uid!=null && !ServiceMap.validateUID(uid)) {
      ServiceMap.logError(request, response, 404, "invalid uid");
      return;
    }    
    String maxDist = request.getParameter("maxDist");
    if(maxDist==null) {
      maxDist = request.getParameter("maxDists");
      if(maxDist==null)
        maxDist = "0.1";
    }
    String maxResults = request.getParameter("maxResults");
    if(maxResults==null)
      maxResults = "100";
    
    String agency = request.getParameter("agency");
    
    String geometry = request.getParameter("geometry");
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    String reqFrom = request.getParameter("requestFrom");
    if(! ServiceMap.checkIP(ip, "api")) {
      ServiceMap.logError(request, response, 403,"API calls daily limit reached");
      return;
    }      

    if(selection == null) {
      ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"missing 'selection' parameter");
    }
    else {
      String c[] = selection.split(";");
      if(c.length==2 || c.length==4 || (c.length==1 && (c[0].startsWith("wkt:") || c[0].startsWith("geo:")))) {
        RepositoryConnection con = ServiceMap.getSparqlConnection();
        int results = serviceMapApi.queryTplLatLng(out, con, c, maxDist, agency, maxResults, "true".equalsIgnoreCase(geometry));
        ServiceMap.updateResultsPerIP(ip, "api", results);    
        con.close();
        ServiceMap.logAccess(request, null, selection, null, null, "api-tpl-latlng", null, null, null, null, "json", uid, reqFrom);
      }
      else
        ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"invalid 'selection' parameter (accepted 'lat;long', 'lat1;long1;lat2;long2', 'wkt:...' or 'geo:...' )");
    }
%>
