<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file= "/include/parameters.jsp" %>
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

    response.setContentType("application/json; charset=UTF-8");
    response.addHeader("Access-Control-Allow-Origin", "*");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String line = request.getParameter("line");
    String codRoute = request.getParameter("route");
    String geometry = request.getParameter("geometry");
    String uid = request.getParameter("uid");
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    String reqFrom = request.getParameter("requestFrom");

    if(line == null && codRoute == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing 'line' or 'route' parameters");
    }
    else {
      serviceMapApi.queryBusStopsOfLine(out, con, line, codRoute, geometry==null || "true".equalsIgnoreCase(geometry));
      logAccess(ip, null, ua, line+";"+codRoute, null, null, "api-tpl-bus-stops", null, null, null, null, "json", uid, reqFrom);
    }
%>
