<%@page import="java.io.IOException"%>
<%@page import="org.openrdf.model.Value"%>
<%@ page import="java.util.*"%>
<%@ page import="org.openrdf.repository.Repository"%>
<%@ page import="org.openrdf.repository.sparql.SPARQLRepository"%>
<%@ page import="java.sql.*"%>
<%@ page import="java.util.List"%>
<%@ page import="org.openrdf.query.BooleanQuery"%>
<%@ page import="org.openrdf.OpenRDFException"%>
<%@ page import="org.openrdf.repository.RepositoryConnection"%>
<%@ page import="org.openrdf.query.TupleQuery"%>
<%@ page import="org.openrdf.query.TupleQueryResult"%>
<%@ page import="org.openrdf.query.BindingSet"%>
<%@ page import="org.openrdf.query.QueryLanguage"%>
<%@ page import="java.io.File"%>
<%@ page import="java.net.URL"%>
<%@ page import="org.openrdf.rio.RDFFormat"%>
<%@include file= "/include/parameters.jsp" %>
<%@page import="org.json.simple.JSONObject"%>
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
    String selection = request.getParameter("serviceUri");
    String latitudine = "";
    String longitudine = "";
    String queryForCoordinates = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "SELECT ?lat ?long WHERE{\n"
              + " { <" + selection + "> km4c:hasAccess ?entry.\n"
              + "  ?entry geo:lat ?lat.\n"
              //+ "  FILTER (?lat>40)\n"
              + "  ?entry geo:long ?long.\n"
              //+ "  FILTER (?long>10)\n"
              + " } UNION {\n"
              + "  <" + selection + "> km4c:isInRoad ?road.\n"
              + "  <" + selection + "> geo:lat ?lat.\n"
              //+ "  FILTER (?lat>40)\n"
              + "  <" + selection + "> geo:long ?long.\n"
              //+ "  FILTER (?long>10)\n"
              + " } UNION {\n"
              + "  <" + selection + "> geo:lat ?lat.\n"
              //+ "  FILTER (?lat>40)\n"
              + "  <" + selection + "> geo:long ?long.\n"
              //+ "  FILTER (?long>10)\n"
              + " }\n"
              + "}LIMIT 1";

    TupleQuery tupleQueryForCoordinates = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForCoordinates);
    TupleQueryResult resultCoord = tupleQueryForCoordinates.evaluate();
    logQuery(filterQuery(queryForCoordinates),"get-coodinates","any",selection);
    
    while (resultCoord.hasNext()) {
      BindingSet bindingSetCoord = resultCoord.next();
      String latitude = bindingSetCoord.getValue("lat").stringValue();
      String longitude = bindingSetCoord.getValue("long").stringValue();
      latitudine = latitude;
      longitudine = longitude;
    }
    JSONObject obj = new JSONObject();
    obj.put("latitudine", latitudine);
    obj.put("longitudine", longitudine);
    out.println(obj);
    con.close();
%>