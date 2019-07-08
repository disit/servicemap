<%@page import="org.disit.servicemap.api.ServiceMapApi"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@page  import="java.io.IOException"%>
<%@page  import="org.openrdf.model.Value"%>
<%@ page import="java.util.*"%>
<%@ page import="org.openrdf.repository.Repository"%>
<%@ page import="org.openrdf.repository.sparql.SPARQLRepository"%>
<%@ page import="java.sql.*"%>
<%@ page import="java.util.List"%>
<%@ page import="org.openrdf.query.BooleanQuery"%>
<%@ page import="org.openrdf.OpenRDFException"%>
<%@ page import="org.openrdf.repository.RepositoryConnection"%>
<%@ page import="org.openrdf.query.TupleQuery"%>
<%@ page import="java.text.ParseException"%>
<%@ page import="java.text.SimpleDateFormat"%>

<%@ page import="org.openrdf.query.TupleQueryResult"%>
<%@ page import="org.openrdf.query.BindingSet"%>
<%@ page import="org.openrdf.query.QueryLanguage"%>
<%@ page import="java.io.File"%>
<%@ page import="java.net.URL"%>
<%@ page import="org.openrdf.rio.RDFFormat"%>
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
  
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String uriFermata = request.getParameter("uriFermata");
    String divRoute = request.getParameter("divRoute");

    ServiceMapApi api= new ServiceMapApiV1();
    
    TupleQueryResult result = api.queryBusLines(uriFermata, con);
    if(result==null)
      return;
    try{
        out.println("<div class=\"infoLinee\">");
        if(result.hasNext()){//scrivo solo per ATAF - temporaneamente
            out.println("<b>Lines:</b>");
            out.println("<table>");
            out.println("<tr>");
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String Line="";
                if(bindingSet.getValue("id")!=null)
                    Line = bindingSet.getValue("id").stringValue();
                else
                    Line = bindingSet.getValue("desc").stringValue();
                
                String LineUri = bindingSet.getValue("line").stringValue();
                String ag = bindingSet.getValue("ag").stringValue();
                //out.println("<td onclick='showLinea(\""+ idLine +"\")'>" + idLine + "</td>");
                out.println("<td onclick='showRoute(\""+ ag +"\",\""+ LineUri +"\",\"" + ServiceMap.stringEncode(uriFermata) + "\",\"" + ServiceMap.stringEncode(divRoute) + "\")'>" + Line + "</td>");
            }
            out.println("</tr>");
            out.println("</table>");
        }
        out.println("</div>"); 
    }catch (Exception e) {
        ServiceMap.notifyException(e);
    }finally{
      con.close();
    }
%>