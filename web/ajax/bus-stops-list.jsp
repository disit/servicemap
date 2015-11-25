<%@page import="org.disit.servicemap.ServiceMap"%>
<%@ page trimDirectiveWhitespaces="true" %>
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

    //String nomeLinea = request.getParameter("nomeLinea");
    String codeRoute = request.getParameter("codeRoute");
    //System.out.println(codeRoute);

    out.println("<option value=\"\"> - Select a Bus Stop - </option>");
    out.println("<option value=\"all\">SHOW ENTIRE ROUTE</option>");
/*
    String filtroLinee = "";
    if (!nomeLinea.equals("all")) {
      filtroLinee = "?tpll dcterms:identifier \"" + nomeLinea + "\"^^xsd:string.";
    }
    
    String queryString
            = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource/>"
            + "PREFIX schema:<http://schema.org/#> "
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> 	 "
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> 	 "
            + "PREFIX vcard:<http://www.w3.org/2006/vcard/ns#> 	 "
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> 	 "
            + "PREFIX dcterms:<http://purl.org/dc/terms/>  "
            + "SELECT DISTINCT ?nomeFermata WHERE {"
            + " ?tpll rdf:type km4c:PublicTransportLine ."
            + filtroLinee
            + " ?tpll km4c:hasRoute ?route."
            + " ?route km4c:hasSection ?rs."
            + " ?rs km4c:endsAtStop ?bs1."
            + " ?rs km4c:startsAtStop ?bs2."
            + " { ?bs1 foaf:name ?nomeFermata. }"
            + " UNION "
            + " { ?bs2 foaf:name ?nomeFermata. }"
            + "} "
            + "ORDER BY ?nomeFermata";
*/

    String queryString = ServiceMap.busLineQuery(codeRoute);
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(filterQuery(queryString),"get-bus-stops-list","any",codeRoute);
    try {
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String valueOfNomeFermata = bindingSet.getValue("nomeFermata").stringValue();
        out.println("<option value=\"" + valueOfNomeFermata + "\">" + valueOfNomeFermata + "</option>");
      }
      con.close();
    } catch (Exception e) {

      out.println(e.getMessage());
    }
%>