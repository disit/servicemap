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

    Repository repo = new SPARQLRepository(sparqlEndpoint);
    repo.initialize();
    RepositoryConnection con = repo.getConnection();
    String selection = request.getParameter("selection");
    String range = request.getParameter("range");
    if(range==null)
      range = "day";
    if(!range.equals("day") && !range.equals("week") && !range.equals("month"))
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,"invalid 'range' parameter value (day,week,month)");
    String maxDists = request.getParameter("maxDists");
    String maxResults = request.getParameter("maxResults");    
    String uid = request.getParameter("uid");
    String text = request.getParameter("text");
    String ip = request.getRemoteAddr();
    String ua = request.getHeader("User-Agent");

    String coords[] = null;
    if(selection!=null) {
      coords = selection.split(";");
      if(coords.length<2) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,"invalid 'selection' parameter (missing lat;long)");
        return;
      }
    }
    serviceMapApi.queryEventList(out, con, range, coords, maxDists, maxResults, text);
    logAccess(ip, null, ua, selection, null, null, "api-events-"+range, maxResults, maxDists, null, null, "json", uid);
%>
