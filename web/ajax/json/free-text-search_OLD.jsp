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
<%@ page import="org.disit.servicemap.Configuration"%>
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
  String textToSearch = request.getParameter("search");
  String limit = request.getParameter("limit");
  textToSearch = unescapeUri(textToSearch);
  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");

  logAccess(ip, null, ua, null, null, null, "ui-text-search", limit, null, null, textToSearch, null, null);

  String textSearchQuery = ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch);

  if (textSearchQuery.equals("")) {
    response.sendError(400, "Invalid search");
    //out.println("{ "
    //        + "\"type\": \"FeatureCollection\", "
    //        + "\"features\": [ ] }");
    return;
  }

  String queryText = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
          + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
          + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
          + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
          + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
          + "PREFIX schema:<http://schema.org/>\n"
          + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
          + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
          + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
          + "SELECT DISTINCT ?ser ?long ?lat ?sTypeIta ?sName ?sType ?sCategory ?identifier ?civic ?x WHERE {\n"
          //+ " {\n"
          //+ "  ?ser rdf:type ?t.\n"
          //+ "  ?t rdfs:label ?txt.\n"
          //+ "  ?txt bif:contains \"" + textToSearch + "\" OPTION (score ?sc).\n"
          //+ " }UNION{\n"
          //+ "  ?ser a ?t.\n"
          //+ "  ?t rdfs:subClassOf ?c.\n"
          //+ "  ?c rdfs:label ?txt.\n"
          //+ "  ?txt bif:contains \"" + textToSearch + "\" OPTION (score ?sc).\n"
          //+ " }UNION{\n"
          //+ "  ?ser ?p ?txt.\n"
          //+ (sparqlType.equals("owlim") ? 
          //    " ?txt luc:myIndex \"" + textToSearch + "\".\n"
          //  + " ?txt luc:score ?sc.\n" :
          //    " ?txt bif:contains \"" + textToSearch + "\" OPTION (score ?sc).\n")
          //+ " }\n"
          + textSearchQuery
          + (km4cVersion.equals("old")
                  ? "  OPTIONAL { ?ser km4c:hasServiceCategory ?cat.\n"
                  + "  ?cat rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\") }\n"
                  :
                    "  ?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:Event)\n"
                  + "  OPTIONAL { ?sType rdfs:subClassOf ?sCategory. FILTER(STRSTARTS(STR(?sCategory),\"http://www.disit.org/km4city/schema#\")) }\n"
                  + "  ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\")\n" )
          + " OPTIONAL {{?ser rdfs:label ?sName.}UNION{?ser schema:name ?sName.}UNION{?ser foaf:name ?sName.}UNION{?ser km4c:extendName ?sName.}}\n"
          + " {\n"
          + "  ?ser geo:lat ?lat .\n"
          + "  ?ser geo:long ?long .\n"
          //+ "  ?ser geo:geometry ?geo .\n"
          + "} UNION {\n"
          + "  ?ser km4c:hasAccess ?entry.\n"
          + "	 ?entry geo:lat ?lat.\n"
          + "	 ?entry geo:long ?long.\n"
          //+ "	 ?entry geo:geometry ?geo.\n"
          + (enable_road_ftsearch.equals("true") ? 
             "} UNION {\n"
           + "  ?ser km4c:hasStreetNumber ?sn.\n"
           + "  ?sn km4c:hasExternalAccess ?entry.\n"
           + "  ?sn km4c:number ?civic.\n"
           + "	?entry geo:lat ?lat.\n"
           + "	?entry geo:long ?long.\n" : "")
          + " }\n"
          + " ?ser dcterms:identifier ?identifier.\n"
          + "} ORDER BY DESC(?sc)";
  if (!"0".equals(limit)) {
    queryText += " LIMIT " + limit;
  }
  TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
  logQuery(queryText, "free-text-search", sparqlType, textToSearch + ";" + limit);
  TupleQueryResult result = tupleQuery.evaluate();
  int i = 0;
  out.println("{ "
          + "\"type\": \"FeatureCollection\", "
          + "\"features\": [ ");
  while (result.hasNext()) {
    if (i != 0) {
      out.println(", ");
    }
    BindingSet bindingSet = result.next();
    //String nameService = "";

    String serviceUri = bindingSet.getValue("ser").stringValue();
    String serviceLat = "";
    if (bindingSet.getValue("lat") != null) {
      serviceLat = bindingSet.getValue("lat").stringValue();
    }
    String serviceLong = "";
    if (bindingSet.getValue("long") != null) {
      serviceLong = bindingSet.getValue("long").stringValue();
    }
    String label = "";
    if (bindingSet.getValue("sTypeIta") != null) {
      label = bindingSet.getValue("sTypeIta").stringValue();
      //label = Character.toLowerCase(label.charAt(0)) + label.substring(1);
      label = label.replace(" ", "_");
      label = label.replaceAll("[^\\P{Punct}_]+", "");
    }
    /*String txt = "";
     if (bindingSet.getValue("txt") != null) {
     txt = bindingSet.getValue("txt").stringValue();
     }*/
    // DICHIARAZIONE VARIABILI serviceType e serviceCategory per ICONA
    String subCategory = "";
    if (bindingSet.getValue("sType") != null) {
      subCategory = bindingSet.getValue("sType").stringValue();
      subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
      //subCategory = Character.toLowerCase(subCategory.charAt(0)) + subCategory.substring(1);
      //subCategory = subCategory.replace(" ", "_");
    }

    String category = "";
    if (bindingSet.getValue("sCategory") != null) {
      category = bindingSet.getValue("sCategory").stringValue();
      category = category.replace("http://www.disit.org/km4city/schema#", "");
      //category = Character.toLowerCase(category.charAt(0)) + category.substring(1);
      //category = category.replace(" ", "_");
    }
    String serviceType = "";
    if(!category.equals(""))
      serviceType = category + "_" + subCategory;

    String identifier = "";
    if (bindingSet.getValue("identifier") != null) {
      identifier = bindingSet.getValue("identifier").stringValue();
    }
    
    String number = "";
    if (bindingSet.getValue("civic") != null) {
      number = bindingSet.getValue("civic").stringValue();
    }

    String sName = "";
    if (bindingSet.getValue("sName") != null) {
      sName = bindingSet.getValue("sName").stringValue();
    } else {
      sName = identifier;
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
            + "    \"serviceUri\": \"" + serviceUri + "\", "
            + "\"nome\": \"" + escapeJSON(sName) + "\", "
            + "\"tipo\": \"servizio\", "
            + "\"number\": \"" + escapeJSON(number) + "\", "
            // *** INSERIMENTO serviceType
            + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\", "
            // **********************************************
            + "\"type\": \"" + escapeJSON(label) + "\" "
            + "}, "
            + "\"id\": " + Integer.toString(i + 1) + "  "
            // + "\"query\": \"" + queryString + "\" "
            + "}");
    i++;
  }
  out.println("] "
          + "}");
  con.close();
%>