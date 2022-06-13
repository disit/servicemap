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

  String lang = request.getParameter("lang");
  if(lang==null || (!lang.equals("it") && !lang.equals("en")))
    lang = "en";
  String maxResults = request.getParameter("maxResults");
  if(maxResults==null)
    maxResults = "5";
  int n = 5;
  try {
    n = Integer.parseInt(maxResults);
  }catch(NumberFormatException ex) {
    //continue with 5
  }
  
  String uid = request.getParameter("uid");
  if(uid!=null && !ServiceMap.validateUID(uid)) {
    ServiceMap.logError(request, response, 404, "invalid uid");
    return;
  }
  
  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  String reqFrom = request.getParameter("requestFrom");
  String apikey = request.getParameter("apikey");
  if(! ServiceMap.checkIP(ip, "api")) {
    ServiceMap.logError(request, response, 403,"API calls daily limit reached");
    return;
  }      

  serviceMapApi.queryLastContribution(out, n, lang, apikey);
  ServiceMap.logAccess(request, null, null, null, "", "api-last-feedback", null, null, null, "", null, uid, reqFrom);
%>