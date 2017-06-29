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

  String uid = request.getParameter("uid");
  if(uid!=null && !ServiceMap.validateUID(uid)) {
    response.sendError(404, "invalid uid");
    return;
  }
  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  String reqFrom = request.getParameter("requestFrom");
if ("html".equals(request.getParameter("format"))) {%>
<jsp:include page="../../../../mappa.jsp" > <jsp:param name="mode" value="bus-position"/> </jsp:include>
<%
    response.setContentType("text/html; charset=UTF-8");
    logAccess(ip, null, ua, "", null, null, "api-tpl-bus-stops", null, null, null, null, "html", uid, reqFrom);
} else { //format JSON
    response.setContentType("application/json; charset=UTF-8");
    response.addHeader("Access-Control-Allow-Origin", "*");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

    RepositoryConnection con = ServiceMap.getSparqlConnection();
    serviceMapApi.queryBusesLastPosition(out, con);
    con.close();
    logAccess(ip, null, ua, "", null, null, "api-tpl-bus-stops", null, null, null, null, "json", uid, reqFrom);
}
%>