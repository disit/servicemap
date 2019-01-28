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

    String position = request.getParameter("position");
    String search = request.getParameter("search");
    String searchMode = request.getParameter("searchMode");
    String maxResults = request.getParameter("maxResults");
    if(maxResults==null)
      maxResults="10";
    double wktDist = 0.0004;
    String maxDists = request.getParameter("maxDists");
    if(maxDists!=null)
      wktDist = Double.parseDouble(maxDists);
    String sortByDist = request.getParameter("sortByDistance");
    if(sortByDist==null)
      sortByDist = "false";
    
    String excludePOI = request.getParameter("excludePOI");
    if(excludePOI==null)
      excludePOI="false";
    String categories = request.getParameter("categories");
    
    String intersectGeom = request.getParameter("intersectGeom");
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

    if(position == null && search==null) {
      ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"missing 'position' or 'search' parameters");
    }
    else {
      if(search!=null) {
        int results = serviceMapApi.queryLocationSearch(out, search, searchMode, position, maxDists, excludePOI.equalsIgnoreCase("true"), categories, maxResults, sortByDist.equalsIgnoreCase("true"));
        ServiceMap.updateResultsPerIP(ip, "api", results);
        ServiceMap.logAccess(request, null, excludePOI+";"+position, null, null, "api-location-search", null, null, null, search, "json", uid, reqFrom);
      } else {
        String c[] = position.split(";");
        if(c.length>=2) {
          String lat=c[0];
          String lng=c[1];
          RepositoryConnection con = ServiceMap.getSparqlConnection();
          int results = serviceMapApi.queryLocation(out, con, lat, lng, intersectGeom, wktDist);
          ServiceMap.updateResultsPerIP(ip, "api", results);
          ServiceMap.logAccess(request, null, position, null, null, "api-location", null, null, null, null, "json", uid, reqFrom);
          con.close();
        }
        else
          ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"invalid 'position' parameter (missing lat;long)");
      }
    }
%>
