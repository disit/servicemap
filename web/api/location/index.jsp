<%@page import="org.disit.servicemap.api.ServiceMapApi"%>
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
    ServiceMapApi serviceMapApi = new ServiceMapApi();

    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String position = request.getParameter("position");
    String uid = request.getParameter("uid");
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    String reqFrom = request.getParameter("requestFrom");

    if(position == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing 'position' parameter");
    }
    else {
      String c[] = position.split(";");
      if(c.length>=2) {
        String lat=c[0];
        String lng=c[1];
        serviceMapApi.queryLocation(out, con, lat, lng, "false", 0.0004);
        ServiceMap.logAccess(request, null, position, null, null, "api-location", null, null, null, null, "json", uid, reqFrom);
      }
      else
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,"invalid 'position' parameter (missing lat;long)");
    }
%>
