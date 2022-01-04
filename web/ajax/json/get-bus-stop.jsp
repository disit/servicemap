<%@page contentType="application/json" pageEncoding="UTF-8"%>
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

    String nomeFermata = request.getParameter("nomeFermata");
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
            + "PREFIX schema:<http://schema.org/#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
            + "SELECT distinct ?bs ?nomeFermata ?bslat ?bslong WHERE {\n"
            + "	?bs rdf:type km4c:BusStop.\n"
            + "	?bs foaf:name \"" + ServiceMap.stringEncode(nomeFermata) + "\"^^xsd:string.\n"
            + "	?bs geo:lat ?bslat.\n"
            + "	?bs geo:long ?bslong.\n"
            + "} LIMIT 1";
    //out.println(queryString);

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(filterQuery(queryString),"get-bus-stop","any",nomeFermata);

    out.println("{ "
            + "\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");

    try {
        int i = 0;
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();

            String valueOfBS = bindingSet.getValue("bs").stringValue();
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
                    + "    \"popupContent\": \"" + nomeFermata + "\", "
                    + "    \"name\": \"" + nomeFermata + "\", "
                    + "    \"serviceUri\": \"" + valueOfBS + "\", "
                    + "    \"tipo\": \"fermata\", "
                    + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\" "
                    + "}, "
                    + "\"id\": " + Integer.toString(i + 1) + " "
                    + "}");
            i++;
        }
    } finally{con.close();}
    out.println("] }");
%>