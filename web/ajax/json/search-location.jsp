<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
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
  
  request.setCharacterEncoding("UTF-8");
  response.setContentType("application/json; charset=UTF-8");
  ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();

  String search = request.getParameter("search");
  if(search==null) {
    response.sendError(404, "missing search terms");
    return;
  }
  String searchMode = request.getParameter("searchMode");
  String position = request.getParameter("position");
  String excludePOI = request.getParameter("excludePOI");
  if(excludePOI==null)
    excludePOI="false";
  String categories = request.getParameter("categories");
  String maxResults = request.getParameter("maxResults");
  if(maxResults==null)
    maxResults="10";
  String maxDists = request.getParameter("maxDists");
  String sortByDist = request.getParameter("sortByDistance");
  
  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  
  serviceMapApi.queryLocationSearch(out, search, searchMode, position, maxDists, excludePOI.equalsIgnoreCase("true"), categories, maxResults, sortByDist.equalsIgnoreCase("true"));
  ServiceMap.logAccess(request, null, excludePOI+";"+position, null, null, "ui-location-search", maxResults, maxDists, null, search, "json", null, null);
%>