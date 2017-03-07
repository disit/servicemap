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

    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String selection = request.getParameter("selection");
    String uid = request.getParameter("uid");
    if(uid!=null && !ServiceMap.validateUID(uid)) {
      response.sendError(404, "invalid uid");
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

    if(selection == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing 'selection' parameter");
    }
    else {
      String c[] = selection.split(";");
      if(c.length>=2) {
        serviceMapApi.queryTplLatLng(out, con, c, maxDist, agency, maxResults, "true".equalsIgnoreCase(geometry));
        logAccess(ip, null, ua, selection, null, null, "api-tpl-latlng", null, null, null, null, "json", uid, reqFrom);
      }
      else
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,"invalid 'position' parameter (missing lat;long)");
    }
%>
