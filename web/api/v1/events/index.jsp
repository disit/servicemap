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
    try {
      String selection = request.getParameter("selection");
      String range = request.getParameter("range");
      if(range==null)
        range = "day";
      if(!range.equals("day") && !range.equals("week") && !range.equals("month"))
        ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"invalid 'range' parameter value (day,week,month)");
      String maxDists = request.getParameter("maxDists");
      String maxResults = request.getParameter("maxResults");    
      String uid = request.getParameter("uid");
      String lang = request.getParameter("lang");
      if(uid!=null && !ServiceMap.validateUID(uid)) {
        ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST, "invalid uid");
        return;
      }
      String text = request.getParameter("text");
      String ip = ServiceMap.getClientIpAddress(request);
      String ua = request.getHeader("User-Agent");
      String reqFrom = request.getParameter("requestFrom");

      String coords[] = null;
      if(selection!=null) {
        if(!selection.startsWith("wkt:") && ! selection.startsWith("geo:")) {
          coords = selection.split(";");
          if(coords.length!=2 && coords.length!=4) {
            ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"invalid 'selection' parameter (lat;long[;lat;long] | wkt:... | geo: ...)");
            return;
          }
          if(maxDists==null)
            maxDists = "1";
          try {
            for(int i=0;i<coords.length;i++)
              Float.parseFloat(coords[i]);
          } catch(NumberFormatException e) {
            ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"invalid 'selection' parameter (lat or long not float numbers)");
            return;
          }
          try {
            if(coords.length==2)
              Float.parseFloat(maxDists);
          } catch(NumberFormatException e) {
            ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST,"invalid 'maxDists' parameter (not float number)");
            return;
          }
        }
        else {
          coords = new String[]{selection};
          maxDists = "";
        }
      }
      if(! ServiceMap.checkIP(ip, "api")) {
        ServiceMap.logError(request, response, 403,"API calls daily limit reached");
        return;
      }      
      int results = serviceMapApi.queryEventList(out, con, range, coords, maxDists, maxResults, text, lang, reqFrom!=null);
      ServiceMap.updateResultsPerIP(ip, "api", results);
      ServiceMap.logAccess(request, null, selection, null, null, "api-events-"+range, maxResults, maxDists, null, null, "json", uid, reqFrom);
    } finally {
      con.close();
    }
%>
