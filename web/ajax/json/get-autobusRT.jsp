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
<%@ page import="java.text.Normalizer"%>
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

  String queryText = "PREFIX luc: <http://www.ontotext.com/owlim/lucene#>\n"
          + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
          + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
          + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
          + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
          + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
          + "PREFIX schema:<http://schema.org/>\n"
          + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
          + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
          + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
          + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
          + "SELECT * WHERE {\n"
          + "{ \n"
          + "select ?v (max(?d) as ?md) (count(*) as ?c) where {\n"
          + "?s a km4c:AVMRecord.\n"
          + "?s dcterms:created ?d.\n"
          + "?s km4c:vehicle ?v\n"
          + "}\n"
          + "group by ?v\n"
          + "}\n"
          + "?s a km4c:AVMRecord;\n"
          + "km4c:vehicle ?v;\n"
          + "dcterms:created ?md;\n"
          + "geo:lat ?lat; geo:long ?long.\n"
          + "?ride km4c:hasAVMRecord ?s.\n"
          + "?ride km4c:onRoute ?route.\n"
          + "?route km4c:hasLastStop ?bse.\n"
          + "?bse foaf:name ?bsLast.\n"
          + "?route km4c:hasFirstStop ?bss.\n"
          + "?bss foaf:name ?bsFirst.\n"
          + "?route foaf:name ?rName.\n"
          + "bind(bif:dateDiff('minute',?md,now()) as ?delay)\n"
          + "filter(?delay<20)\n"
          + "}order by desc(?md) \n";  
       
    queryText += " LIMIT 15";
  
  TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
  long startTime = System.nanoTime();
  TupleQueryResult result = tupleQuery.evaluate();
  logQuery(queryText, "AutobusRT", sparqlType,"AutobusRT;20", System.nanoTime() - startTime);
  int i = 0;
  out.println("{ "
          + "\"type\": \"FeatureCollection\", "
          + "\"features\": [ ");
  while (result.hasNext()) {
    if (i != 0) {
      out.println(", ");
    }
    BindingSet bindingSet = result.next();
    
    //String serviceUri = bindingSet.getValue("ser").stringValue();
    String serviceLat = "";
    if (bindingSet.getValue("lat") != null) {
      serviceLat = bindingSet.getValue("lat").stringValue();
    }
    String serviceLong = "";
    if (bindingSet.getValue("long") != null) {
      serviceLong = bindingSet.getValue("long").stringValue();
    }

    String nomeLinea = "";
    if (bindingSet.getValue("rName") != null) {
      nomeLinea = bindingSet.getValue("rName").stringValue();
    }

    String bsFirst = "";
    if (bindingSet.getValue("bsFirst") != null) {
      bsFirst = bindingSet.getValue("bsFirst").stringValue();
    }
    
    String bsLast = "";
    if (bindingSet.getValue("bsLast") != null) {
      bsLast = bindingSet.getValue("bsLast").stringValue();
    }

    String busNumber = "";
    if (bindingSet.getValue("v") != null) {
      busNumber = bindingSet.getValue("v").stringValue();
    } 
    
    String delay = "";
    if (bindingSet.getValue("delay") != null) {
      delay = bindingSet.getValue("delay").stringValue();
    } 
    
    out.println("{ "
            + " \"geometry\": {  "
            + "     \"type\": \"Point\",  "
            + "    \"coordinates\": [  "
            + "      " + serviceLong + ",  "
            + "      " + serviceLat + "  "
            + " ]  "
            + "},  "
            + "\"type\": \"Feature\",  "
            + "\"properties\": {  "
            + "\"vehicleNum\": \""+busNumber+"\", "
            + "\"line\": \""+nomeLinea+"\", "
            + "\"direction\": \""+bsFirst+" &#10132; "+bsLast+"\", "
            + "\"tipo\": \"RealTimeInfo\", "
            + "\"serviceUri\": \"busCode"+busNumber+"\", "
            + "\"detectionTime\": \""+delay+"\", "
            // *** INSERIMENTO serviceType
            + "\"serviceType\": \"bus_real_time\" "
            // **********************************************
            + "}, "
            + "\"id\": " + Integer.toString(i + 1) + "  "
            + "}\n");
    i++;
  }
  out.println("] "
          + "}");
  con.close();
%>