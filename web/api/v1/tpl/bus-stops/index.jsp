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
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

    String line = request.getParameter("line");
    String codRoute = request.getParameter("route");
    String geometry = request.getParameter("geometry");
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

    if(line == null && codRoute == null) {
      ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"missing 'line' or 'route' parameters");
    }
    else {
      RepositoryConnection con = ServiceMap.getSparqlConnection();
      int results = serviceMapApi.queryBusStopsOfLine(out, con, line, codRoute, geometry==null || "true".equalsIgnoreCase(geometry));
      ServiceMap.updateResultsPerIP(ip, "api", results);
      con.close();
      ServiceMap.logAccess(request, null, line+";"+codRoute, null, null, "api-tpl-bus-stops", null, null, null, null, "json", uid, reqFrom);
    }
%>
