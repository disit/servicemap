<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="org.json.simple.JSONArray"%>
<%@page import="org.json.simple.JSONObject"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@page import="org.disit.servicemap.ServiceMap"%>
<%@page import="java.io.IOException"%>
<%@page import="org.openrdf.model.Value"%>
<%@page import="java.util.*"%>
<%@page import="org.openrdf.repository.Repository"%>
<%@page import="org.openrdf.repository.sparql.SPARQLRepository"%>
<%@page import="java.sql.*"%>
<%@page import="java.util.List"%>
<%@page import="org.openrdf.query.BooleanQuery"%>
<%@page import="org.openrdf.OpenRDFException"%>
<%@page import="org.openrdf.repository.RepositoryConnection"%>
<%@page import="org.openrdf.query.TupleQuery"%>
<%@page import="org.openrdf.query.TupleQueryResult"%>
<%@page import="org.openrdf.query.BindingSet"%>
<%@page import="org.openrdf.query.QueryLanguage"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URL"%>
<%@page import="org.openrdf.rio.RDFFormat"%>
<%@page import="java.text.Normalizer"%>
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

    Configuration conf=Configuration.getInstance();
    RepositoryConnection con = ServiceMap.getSparqlConnection();

    String latitudine = request.getParameter("lat");
    String longitudine = request.getParameter("lng");
    String findGeometry = request.getParameter("findGeometry");

    try {
      ServiceMap.logAccess(request, null, latitudine+";"+longitudine, null, null, "ui-location", null, null, null, null, null, null, "user");
      ServiceMapApiV1 api = new ServiceMapApiV1();
      JSONObject obj = api.queryLocation(con, latitudine, longitudine, findGeometry, 0.0004);
      if(obj!=null) {
        String address = (String)obj.get("address");
        String number = (String)obj.get("number");
        String uri;
        if(address!=null) {
          address = address + ", " + (number!=null ? number + ", " : "");
          uri=(String)obj.get("addressUri");
        }
        else {
          address = "";
          uri=(String)obj.get("municipalityUri");
        }
        out.println("<small>Address:</small> <span id='actualAddress'><a href=\""+logEndPoint+uri+"\" target=\"_blank\">" + address + obj.get("municipality")+"</a></span>");

        if(conf.get("enablePathSearch","true").equals("true") /*&& !address.equals("")*/)
          out.println("<br><button style='margin:10px 10px 10px 0px' id='startpathsearch' onclick='setStartSearchPath("+latitudine+","+longitudine+")'>Path from here</button><button style='margin:10px 10px 10px 0px' id='endpathsearch' onclick='setEndSearchPath("+latitudine+","+longitudine+")'>Path to here</button>");
        out.println("<button style='background-color: #c3caf9;' onclick='mapLatLngClick(L.latLng("+latitudine+", "+longitudine+"),false,true)'>Search geometry</button><br>");

        JSONArray a=(JSONArray)obj.get("intersect");
        if(a!=null) {
          out.println("<div id='intersect' style='max-height:64px;overflow:auto;'>");
          for(int i=0;i<a.size();i++) {
            JSONObject area=(JSONObject)a.get(i);
            String name = (String)area.get("name");
            if(area.get("agency")!=null) 
              name += " - "+(String)area.get("agency");

            if(area.get("agency")==null)
              out.println("<small>"+area.get("class").toString().replace("http://www.disit.org/km4city/schema#","").replace("http://vocab.gtfs.org/terms#", "") +":</small> <a href=\""+logEndPoint+area.get("uri")+"\" target=\"_blank\">"+name+"</a>"+" (dist:"+String.format("%.4f",area.get("distance"))+")<br>");
            else
              out.println("<small>"+area.get("class").toString().replace("http://www.disit.org/km4city/schema#","").replace("http://vocab.gtfs.org/terms#", "") +":</small> <a href=\"#\" onclick=\"showLinea('','"+area.get("uri")+"','"+area.get("direction")+"','"+area.get("name")+"')\">"+name+"</a>"+" (dist:"+String.format("%.4f",area.get("distance"))+")<br>");
          }
          out.println("</div>");
        }
      }
    } catch (IllegalArgumentException e) {
      response.sendError(400);
    } finally {
      con.close();
    }
%>