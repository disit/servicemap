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
<%@page import="java.text.ParseException"%>
<%@page import="java.text.SimpleDateFormat"%>
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

    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource/>\n"
            + "PREFIX schema:<http://schema.org/#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX vcard:<http://www.w3.org/2006/vcard/ns#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
            + "SELECT DISTINCT ?id WHERE {\n"
            + " ?tpll rdf:type km4c:PublicTransportLine.\n"
            + " ?tpll <http://purl.org/dc/terms/identifier> ?id.\n"
            + " ?tpll km4c:hasRoute ?r.\n"
            + " BIND (xsd:integer(?id) as ?line)\n"
            + "} ORDER BY ?line";
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(filterQuery(queryString),"get-lines","any");
    
    String idLine = "";
    try {
        if (result.hasNext()) {
            out.println("<option value=\"\"> - Select a Bus Line - </option>");
            out.println("<option value=\"all\">ALL THE BUS LINES</option>");
        }
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            if (bindingSet.getValue("id") != null) {
                idLine = bindingSet.getValue("id").stringValue();
                out.println("<option value=\"" + idLine + "\">Line " + idLine + "</option>");
            }
        }
    } catch (Exception e) {
        out.println(result);
    }finally{con.close();}
%>