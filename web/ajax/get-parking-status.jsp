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

    String nomeParcheggio = request.getParameter("nomeParcheggio");
    String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX schema:<http://schema.org/>\n"
            + "PREFIX time:<http://www.w3.org/2006/time#>\n"
            + "SELECT distinct  ?situationRecord ?instantDateTime ?occupancy ?free ?occupied ?cpStatus ?capacity WHERE {\n"
            + "	?park rdf:type km4c:Car_park.\n"
            //+ "	?park schema:name \"" + nomeParcheggio + "\" ^^xsd:string .  "
            + "	{?park schema:name \"" + nomeParcheggio + "\".} union { ?park schema:name \"" + nomeParcheggio + "\" ^^xsd:string.}\n"
            + "	?cps km4c:observeCarPark ?park.\n"
            + "	?cps km4c:capacity ?capacity.\n"
            + "	?situationRecord km4c:relatedToSensor ?cps.\n"
            + "	?situationRecord km4c:observationTime ?time.\n"
            + "	?time <http://purl.org/dc/terms/identifier> ?instantDateTime.\n"
            + "	?situationRecord km4c:parkOccupancy ?occupancy.\n"
            + "	?situationRecord km4c:carParkStatus ?cpStatus.\n"
            + "	?situationRecord km4c:free ?free.\n"
            + "	?situationRecord km4c:occupied ?occupied.\n"
            + "} "
            + "ORDER BY DESC (?instantDateTime) "
            + "LIMIT 1";
    //out.println(queryString);

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(filterQuery(queryString),"get-parking-status","any",nomeParcheggio);
    
    if (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String valueOfcpStatus= "";
        if(bindingSet.getValue("cpStatus") != null){
            valueOfcpStatus = bindingSet.getValue("cpStatus").stringValue();
        }
        if (valueOfcpStatus.equals("carParkClosed")){
            out.println("<span style='border:1px solid #000000; color:#C03639; padding:2px;'><b><span name=\"lbl\" caption=\"park_close\">Parking Closed</span></b></span><br />");
        }else{
            out.println("<span style='border:1px solid #000000; color:#128E4E; padding:2px;'><b><span name=\"lbl\" caption=\"park_open\">Parking Open</span></b></span><br />");
        }
        out.println("<div class=\"park\"><span name=\"lbl\" caption=\"parking_data\">Occupation data of the parking</span>: <b>" + nomeParcheggio + "</b> <br />");
        out.println("<table>");
        //out.println("<th>Dati di occupazione del parcheggio <b>"+nomeParcheggio+"</b></th>");		 
        out.println("<tr>");
        out.println("<td><b><span>Total Capacity</span></b></td>");
        out.println("<td><b>Free Spaces</b></td>");
        out.println("<td><b>Occupied Spaces</b></td>");
        out.println("</tr>");
        String valueOfInstantDateTime = "";
        try {
            int i = 0;
            //while (result.hasNext()) {
                //BindingSet bindingSet = result.next();
                
                valueOfInstantDateTime = bindingSet.getValue("instantDateTime").stringValue();
                String valueOfOccupancy = bindingSet.getValue("occupancy").stringValue();
                String valueOfFree = bindingSet.getValue("free").stringValue();
                String valueOfOccupied = bindingSet.getValue("occupied").stringValue();
                String valueOfCapacity = bindingSet.getValue("capacity").stringValue();

                out.println("<tr>");
                out.println("<td>" + valueOfCapacity + "</td>");
                out.println("<td>" + valueOfFree + "</td>");
                out.println("<td>" + valueOfOccupied + "</td>");
                out.println("</tr>");

                i++;
            //}
            out.println("</table>");
            out.println("<div class=\"clearer\"></div>");
            out.println("<div class=\"aggiornamento\"><span name=\"lbl\" caption=\"Last_Update\">Latest Update</span>: " + valueOfInstantDateTime + "</div>");

            out.println("</div>");
        } catch (Exception e) {
            out.println(e.getMessage());
        }
    } else {
        out.println("<span name=\"lbl\" caption=\"msg_real_time\">Real-time data currently not available");
    }
    con.close();
%>