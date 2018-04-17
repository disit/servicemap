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

    RepositoryConnection con = ServiceMap.getSparqlConnection();

    String nomeLinea = request.getParameter("nomeLinea");
    String codRoute = request.getParameter("codRoute");
    
    ServiceMapApiV1 api = new ServiceMapApiV1();
    try {
      api.queryBusStopsOfLine(out, con, nomeLinea, codRoute, false);
    } finally {
      con.close();
    }
/*    
   if("vuoto".equals(codRoute)){
    
   if (!"all".equals(nomeLinea)) {
      String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
               + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
               + "PREFIX schema:<http://schema.org/#>\n"
               + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
               + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
               + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
               + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
               + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
               + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
               + "SELECT DISTINCT ?bs ?bslat ?bslong ?nomeFermata ?x WHERE {\n"
               + " ?tpll rdf:type km4c:PublicTransportLine.\n"
               + " ?tpll dcterms:identifier \"" + nomeLinea + "\"^^xsd:string.\n"
               + " ?tpll km4c:hasRoute ?route.\n"
               + " ?route km4c:hasSection ?rs.\n"
               //  +   " ?rs km4c:endsAtStop ?bs1 ."
               + " ?rs km4c:startsAtStop ?bs.\n"
               // +   " { ?bs1 foaf:name ?nomeFermata ."
               // +   "   ?bs1 geo:lat ?bslat .   "
               // +   "   ?bs1 geo:long ?bslong . "
               // +   " } "
               // +   " UNION "
               + " ?bs foaf:name ?nomeFermata.\n"
               + " ?bs geo:lat ?bslat.\n"
               + " ?bs geo:long ?bslong.\n"
               + "} ORDER BY ?nomeFermata";
        
       
      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
      TupleQueryResult result = tupleQuery.evaluate();
      logQuery(filterQuery(queryString),"get-bus-stops-of-line","any",nomeLinea);
      out.println("{ "
               + "\"type\": \"FeatureCollection\", "
               + "\"features\": [ ");

      try {
        int i = 0;
        while (result.hasNext()) {
          BindingSet bindingSet = result.next();
          String valueOfBS = bindingSet.getValue("bs").stringValue();
          String valueOfNomeFermata = bindingSet.getValue("nomeFermata").stringValue();
          String valueOfBSLat = bindingSet.getValue("bslat").stringValue();
          String valueOfBSLong = bindingSet.getValue("bslong").stringValue();
          if (i != 0) {
            out.println(", ");
          }
          out.println("{ "
                   + " \"geometry\": {  "
                   + "     \"type\": \"Point\",  "
                   + "    \"coordinates\": [  "
                   + "       " + valueOfBSLong + ",  "
                   + "      " + valueOfBSLat + "  "
                   + " ]  "
                   + "},  "
                   + "\"type\": \"Feature\",  "
                   + "\"properties\": {  "
                   + "    \"popupContent\": \"" + valueOfNomeFermata + "\", "
                   + "    \"nome\": \"" + valueOfNomeFermata + "\", "
                   + "    \"serviceUri\": \"" + valueOfBS + "\", "
                   + "    \"tipo\": \"fermata\", "
                   + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\" "
                   // **********************************************
                   + "}, "
                   + "\"id\": " + Integer.toString(i + 1) + " "
                   + "}");
          i++;
        }
      } catch (Exception e) {
       out.println(e.getMessage());
      }
    } else {
       String queryAllBusStops = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
               + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
               + "PREFIX schema:<http://schema.org/#> "
               + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
               + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
               + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
               + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#> "
               + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
               + "SELECT DISTINCT ?nomeFermata ?lat ?long ?bs WHERE {"
               + " ?tpll rdf:type km4c:PublicTransportLine;"
               + "  km4c:hasRoute ?route."
               + " ?route km4c:hasSection ?rs."
               + " ?rs km4c:startsAtStop ?bs."
               + " ?bs foaf:name ?nomeFermata;"
               + "  geo:lat ?lat;"
               + "  geo:long ?long."
               + " FILTER ( datatype(?lat ) = xsd:float )"
               + " FILTER ( datatype(?long ) = xsd:float )"
               + "}";
       
      TupleQuery tupleQueryAllBusStops = con.prepareTupleQuery(QueryLanguage.SPARQL, queryAllBusStops);
      TupleQueryResult resultAllBusStop = tupleQueryAllBusStops.evaluate();
      //ServiceMap.println(queryAllBusStops);
      out.println("{ "
               + "\"type\": \"FeatureCollection\", "
               + "\"features\": [ ");
      try {
          int i = 0;
          while (resultAllBusStop.hasNext()) {
            BindingSet bindingSet = resultAllBusStop.next();
            String valueOfBS = bindingSet.getValue("bs").stringValue();
            String valueOfBSLat = bindingSet.getValue("lat").stringValue();
            String valueOfBSLong = bindingSet.getValue("long").stringValue();
            String nome = bindingSet.getValue("nomeFermata").stringValue();

            if (i != 0) {
              out.println(", ");
            }
            out.println("{ "
                   + " \"geometry\": {  "
                   + "     \"type\": \"Point\",  "
                   + "    \"coordinates\": [  "
                   + "       " + valueOfBSLong + ",  "
                   + "      " + valueOfBSLat + "  "
                   + " ]  "
                   + "},  "
                   + "\"type\": \"Feature\",  "
                   + "\"properties\": {  "
                   + "    \"popupContent\": \"" + nome + "\", "
                   + "    \"nome\": \"" + nome + "\", "
                   + "    \"serviceUri\": \"" + valueOfBS + "\", "
                   + "    \"tipo\": \"fermata\", "
                    + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\" "
                   + "}, "
                   + "\"id\": " + Integer.toString(i + 1) + " "
                   + "}");
            i++;
          }
      } catch (Exception e) {
          out.println(e.getMessage());
      } finally {
          con.close();
      }
    }
   }else{
       String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
               + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
               + "PREFIX schema:<http://schema.org/#>\n"
               + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
               + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
               + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
               + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
               + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
               + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
               + "SELECT DISTINCT ?bs ?bslat ?bslong ?nomeFermata ?x WHERE {\n"
               + " ?tpll rdf:type km4c:PublicTransportLine.\n"
               + " ?tpll dcterms:identifier \"" + nomeLinea + "\"^^xsd:string.\n"
               + " ?tpll km4c:hasRoute ?route.\n"
               + " ?route km4c:hasFirstStop ?bs1.\n"
               + " ?route km4c:hasSection ?rs.\n"
               + " ?route dcterms:identifier \"" + codRoute + "\".\n"
               + " ?rs km4c:endsAtStop ?bs2.\n"
               + " ?rs km4c:distance ?dist.\n"
               +   " { ?bs1 foaf:name ?nomeFermata .\n"
               +   " BIND(?bs1 AS ?bs).\n"
               +   " } "
               +   " UNION "
               + " { ?bs2 foaf:name ?nomeFermata.\n"
               +   " BIND(?bs2 AS ?bs).\n"
               +   " }\n"
               + " ?bs geo:lat ?bslat.\n"
               + " ?bs geo:long ?bslong.\n"
               + "} ORDER BY ASC(?dist)";

      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
      TupleQueryResult result = tupleQuery.evaluate();
      logQuery(filterQuery(queryString),"get-bus-stops-of-line","any",nomeLinea);
      out.println("{ "
               + "\"type\": \"FeatureCollection\", "
               + "\"features\": [ ");
      try {
        int i = 0;
        while (result.hasNext()) {
          BindingSet bindingSet = result.next();
          String valueOfBS = bindingSet.getValue("bs").stringValue();
          String valueOfNomeFermata = bindingSet.getValue("nomeFermata").stringValue();
          String valueOfBSLat = bindingSet.getValue("bslat").stringValue();
          String valueOfBSLong = bindingSet.getValue("bslong").stringValue();
          if (i != 0) {
            out.println(", ");
          }
          out.println("{ "
                   + " \"geometry\": {  "
                   + "     \"type\": \"Point\",  "
                   + "    \"coordinates\": [  "
                   + "       " + valueOfBSLong + ",  "
                   + "      " + valueOfBSLat + "  "
                   + " ]  "
                   + "},  "
                   + "\"type\": \"Feature\",  "
                   + "\"properties\": {  "
                   + "    \"popupContent\": \"" + valueOfNomeFermata + "\", "
                   + "    \"nome\": \"" + valueOfNomeFermata + "\", "
                   + "    \"serviceUri\": \"" + valueOfBS + "\", "
                   + "    \"tipo\": \"fermata\", "
                   + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\" "
                   + "}, "
                   + "\"id\": " + Integer.toString(i + 1) + " "
                   + "}");
          i++;
        }
      } catch (Exception e) {
       out.println(e.getMessage());
      }
   }
    out.println("] }");
*/        
%>