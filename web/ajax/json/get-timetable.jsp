<%@page import="org.json.simple.parser.JSONParser"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
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

 String TPLStopUri = request.getParameter("TPLStopUri");//urifermata
 
 ServiceMapApiV1 api = new ServiceMapApiV1();
 RepositoryConnection conn = ServiceMap.getSparqlConnection();
 
 try {
  String timetable = api.queryTplTimeTable(conn, TPLStopUri, "", "");
 //JSONObject obj = new JSONObject(JSON.parse("{"+timetable+"}"));
 //JSONObject obj = new JSONObject(); 
 //JSONParser parser = new JSONParser();
 //Reader r =  new StringReader("{"+timetable+"}");
 //Object obj = parser.parse(r);
 //JSONObject jsonObject = (JSONObject) obj;
 //String name = (String) jsonObject.get("timetable");
 //out.println(name);

  out.println("{"+timetable+"}");
 } catch(IllegalArgumentException e) {
      response.sendError(400);
 } finally {
  conn.close();
 }
%>