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

    Repository repo = new SPARQLRepository(sparqlEndpoint);
    repo.initialize();
    RepositoryConnection con = repo.getConnection();

    String latitudine = request.getParameter("lat");
    String longitudine = request.getParameter("lng");
    String ip = request.getRemoteAddr();
    String ua = request.getHeader("User-Agent");

    logAccess(ip, null, ua, latitudine+";"+longitudine, null, null, "ui-location", null, null, null, null, null, null);
    ServiceMapApiV1 api = new ServiceMapApiV1();
    JSONObject obj = api.queryLocation(con, latitudine, longitudine);
    if(obj!=null) {
      String address = (String)obj.get("address");
      String number = (String)obj.get("number");
      if(address!=null)
        address = address + ", " + number + ", ";
      else
        address = "";
      out.println("Address: " + address + obj.get("municipality"));
    }
    con.close();
/*
    String queryString = ServiceMap.latLngToAddressQuery(latitudine, longitudine, sparqlType);

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    if(sparqlType.equals("owlim"))
      tupleQuery.setMaxQueryTime(maxTime);
    
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(filterQuery(queryString),"get-address",sparqlType,latitudine+";"+longitudine);

    try {
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            String valueOfVia = bindingSet.getValue("via").stringValue();
            String valueOfNumero = bindingSet.getValue("numero").stringValue();
            String valueOfComune = bindingSet.getValue("comune").stringValue();

            out.println("Indirizzo Approssimativo: " + valueOfVia + ", " + valueOfNumero + ", " + valueOfComune);
        }
        //	}
    } catch (Exception e) {
        out.println(e.getMessage());
    }finally{con.close() ;}
*/        
%>