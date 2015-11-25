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
<%@page contentType="text/html" pageEncoding="UTF-8"%>
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

  String nomeSensore = request.getParameter("nomeSensore");
  String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX schema:<http://schema.org/#>\n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
            + "PREFIX dct:<http://purl.org/dc/terms/#>\n"
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n "
            + "SELECT  ?avgDistance ?avgTime ?occupancy ?concentration ?vehicleFlow ?averageSpeed ?thresholdPerc ?speedPercentile ?timeInstant WHERE{\n"
            + " ?sensor rdf:type km4c:SensorSite.\n"
            + " ?sensor <http://purl.org/dc/terms/identifier> \"" + nomeSensore + "\"^^xsd:string.\n"
            + " ?sensor km4c:hasObservation ?obs.\n"
            + " ?obs km4c:measuredTime ?time.\n"
            + " ?time <http://purl.org/dc/terms/identifier> ?timeInstant. FILTER(?timeInstant >= xsd:date(now()))\n"
            + " OPTIONAL {?obs km4c:averageDistance ?avgDistance}\n"
            + " OPTIONAL {?obs km4c:averageTime ?avgTime}\n"
            + " OPTIONAL {?obs km4c:occupancy ?occupancy}\n"
            + " OPTIONAL {?obs km4c:concentration ?concentration}\n"
            + " OPTIONAL {?obs km4c:vehicleFlow ?vehicleFlow}\n"
            + " OPTIONAL {?obs km4c:averageSpeed ?averageSpeed}\n"
            + " OPTIONAL {?obs km4c:thresholdPerc ?thresholdPerc}\n"
            + " OPTIONAL {?obs km4c:speedPrecentile ?speedPercentile}\n"
            + "} "
            + "ORDER BY DESC (?timeInstant)"
            + " LIMIT 1";
  TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
  TupleQueryResult result = tupleQuery.evaluate();
  logQuery(filterQuery(queryString),"get-sensor-data","any",nomeSensore);
	  
  String valueOfInstantDateTime = "";
  try {
    int i = 0;
    if (result.hasNext()) {
      out.println("<div class=\"sensori\"><span name=\"lbl\" caption=\"sensor_data\">Data from sensor</span>: <b>" + nomeSensore + "</b> <br />");
      out.println("<table>");
      out.println("<tr>");
      out.println("<td><b>Avg Distance(m)</b></td>");
      out.println("<td><b>Avg Time (sec)</b></td>");
      out.println("<td><b>Occupancy (%)</b></td>");
      out.println("<td><b>Concentration (car/km)</b></td>");
      out.println("</tr>");
    }
    else
      out.println("<b><span name=\"lbl\" caption=\"msg_real_time\">Real-time data currently not available</span></b>");
    while (result.hasNext()) {
      BindingSet bindingSet = result.next();
      valueOfInstantDateTime = bindingSet.getValue("timeInstant").toString();
      String valueOfAvgDistance = "";
      if (bindingSet.getValue("avgDistance") != null) {
        valueOfAvgDistance = bindingSet.getValue("avgDistance").toString();
      }
      String valueOfAvgTime = "";
      if (bindingSet.getValue("avgTime") != null) {
        valueOfAvgTime = bindingSet.getValue("avgTime").toString();
      }
      String valueOfOccupancy = "";
      if (bindingSet.getValue("occupancy") != null) {
        valueOfOccupancy = bindingSet.getValue("occupancy").toString();
      }
      String valueOfConcentration = "";
      if (bindingSet.getValue("concentration") != null) {
        valueOfConcentration = bindingSet.getValue("concentration").toString();
      }
      String valueOfVehicleFlow = "";
      if (bindingSet.getValue("vehicleFlow") != null) {
        valueOfVehicleFlow = bindingSet.getValue("vehicleFlow").toString();
      }
      String valueOfAverageSpeed = "";
      if (bindingSet.getValue("averageSpeed") != null) {
        valueOfAverageSpeed = bindingSet.getValue("averageSpeed").toString();
      }
      String valueOfThresholdPerc = "";
      if (bindingSet.getValue("thresholdPerc") != null) {
        valueOfThresholdPerc = bindingSet.getValue("thresholdPerc").toString();
      }
      String valueOfSpeedPercentile = "";
      if (bindingSet.getValue("speedPercentile") != null) {
        valueOfSpeedPercentile = bindingSet.getValue("speedPercentile").toString();
      }

      valueOfAvgDistance = valueOfAvgDistance.replace("\"^^<http://www.w3.org/2001/XMLSchema#float>", "");
      valueOfAvgDistance = valueOfAvgDistance.replace("\"", "");
      valueOfAvgTime = valueOfAvgTime.replace("\"^^<http://www.w3.org/2001/XMLSchema#float>", "");
      valueOfAvgTime = valueOfAvgTime.replace("\"", "");
      valueOfOccupancy = valueOfOccupancy.replace("\"^^<http://www.w3.org/2001/XMLSchema#float>", "");
      valueOfOccupancy = valueOfOccupancy.replace("\"", "");
      valueOfConcentration = valueOfConcentration.replace("\"^^<http://www.w3.org/2001/XMLSchema#float>", "");
      valueOfConcentration = valueOfConcentration.replace("\"", "");
      valueOfVehicleFlow = valueOfVehicleFlow.replace("\"^^<http://www.w3.org/2001/XMLSchema#float>", "");
      valueOfVehicleFlow = valueOfVehicleFlow.replace("\"", "");
      valueOfAverageSpeed = valueOfAverageSpeed.replace("\"^^<http://www.w3.org/2001/XMLSchema#float>", "");
      valueOfAverageSpeed = valueOfAverageSpeed.replace("\"", "");
      valueOfThresholdPerc = valueOfThresholdPerc.replace("\"^^<http://www.w3.org/2001/XMLSchema#float>", "");
      valueOfThresholdPerc = valueOfThresholdPerc.replace("\"", "");
      valueOfSpeedPercentile = valueOfSpeedPercentile.replace("\"^^<http://www.w3.org/2001/XMLSchema#float>", "");
      valueOfSpeedPercentile = valueOfSpeedPercentile.replace("\"", "");
      valueOfInstantDateTime=valueOfInstantDateTime.replace("^^","");

      out.println("<tr>");

      if (valueOfAvgDistance != null && valueOfAvgDistance != "") {
        out.println("<td>" + valueOfAvgDistance + "</td>");
      }
      else
        out.println ("<td> N.A. </td>");
      if (valueOfAvgTime != null && valueOfAvgTime != "") {
        out.println("<td>" + valueOfAvgTime+ "</td>");
      }
      else
        out.println ("<td> N.A. </td>");
      if (valueOfOccupancy != null && valueOfOccupancy != "") {
        out.println("<td>" + valueOfOccupancy+ "</td>");
      }
      else
        out.println ("<td> N.A. </td>");

      if (valueOfConcentration != null && valueOfConcentration != "") {
        out.println("<td>" + valueOfConcentration+ "</td>");
      }
      else
        out.println ("<td> N.A. </td>");
      out.println("</tr>");
      out.println("<tr>");
      out.println("<td><b>Vehicle Flow (car/h)</b></td>");
      out.println("<td><b>Avg Speed (Km/h)</b></td>");
      out.println("<td><b>Threshold Perc (%)</b></td>");
      out.println("<td><b>Speed Perc (%)</b></td>");
      out.println("</tr>");
      out.println("<tr>");
      if (valueOfVehicleFlow != null && valueOfVehicleFlow != "") {
        out.println("<td>" + valueOfVehicleFlow+ "</td>");
      }
      else
        out.println ("<td> N.A. </td>");
      if (valueOfAverageSpeed != null && valueOfAverageSpeed != "") {
        out.println("<td>" + valueOfAverageSpeed+ "</td>");
      }
      else
        out.println ("<td> N.A. </td>");
      if (valueOfThresholdPerc != null && valueOfThresholdPerc != "") {
        out.println("<td>" + valueOfThresholdPerc+ "</td>");
      }
      else
        out.println ("<td> N.A. </td>");
      if (valueOfSpeedPercentile != null && valueOfSpeedPercentile != "") {
        out.println("<td>" + valueOfSpeedPercentile+ "</td>");
      }
      else
        out.println ("<td> N.A. </td>");
      out.println("</tr>");

      out.println("<div class=\"clearer\"></div>");
      out.println("<div class=\"aggiornamento\"><span name=\"lbl\" caption=\"last_update\" >Latest Update</span>: " + valueOfInstantDateTime + "</div>");
                       
    }
    out.println("</table>");
    out.println("</div>");                      
  } catch (Exception e) {
    out.println(e.getMessage());
  }finally{con.close();}
  
%>