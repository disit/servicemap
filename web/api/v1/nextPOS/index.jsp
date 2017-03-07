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
    String range = request.getParameter("range");
    if(range==null)
      range = "day";
    if(!range.equals("day") && !range.equals("week") && !range.equals("month"))
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,"invalid 'range' parameter value (day,week,month)");
    String categories = request.getParameter("categories");
    if(categories==null)
      categories = "Archaeological_site;Botanical_and_zoological_gardens;Churches;Cultural_sites;Historical_buildings;Monument_location;Museum;Squares";

    String maxDists = request.getParameter("maxDists");
    if(maxDists == null)
      maxDists = "8";
    String uid = request.getParameter("uid");
    if(uid!=null && !ServiceMap.validateUID(uid)) {
      response.sendError(404, "invalid uid");
      return;
    }
    String text = request.getParameter("text");
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    String reqFrom = request.getParameter("requestFrom");

    String coords[] = {"43.771155796865166","11.254205703735352"};
    
    serviceMapApi.queryNextPOS(out, con, range, coords, categories, maxDists, text, reqFrom!=null);
    logAccess(ip, null, ua, null, null, null, "api-nextpos-"+range, null, maxDists, null, null, "json", uid, reqFrom);
%>
