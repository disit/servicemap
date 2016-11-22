<%@page trimDirectiveWhitespaces="true" %>
<%@page  import="java.io.IOException"%>
<%@page  import="org.openrdf.model.Value"%>
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

  String filtroSecondaQuery = "";
  String nomeFermata = request.getParameter("nomeFermata");
  String idRide = "";

  String queryStringAVM = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
          + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
          + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
          + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
          + "PREFIX schema:<http://schema.org/#>\n"
          + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
          + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
          + "PREFIX time:<http://www.w3.org/2006/time#>\n"
          + "SELECT ?ride  (MAX(?avmr) AS ?avmrNew)\n"
          + "WHERE{\n"
          + " ?bs rdf:type km4c:BusStop.\n"
          + " ?bs foaf:name \"" + nomeFermata + "\".\n"
          + " ?bs km4c:hasForecast ?bsf.\n"
          + " ?avmr km4c:includeForecast ?bsf.\n"
          + " OPTIONAL {?rs km4c:endsAtStop ?bs}.\n"
          + " OPTIONAL {?rs km4c:startsAtStop ?bs}.\n"
          + " ?route km4c:hasSection ?rs.\n"
          //+ " ?avmr km4c:concernLine ?tpll.\n"
          + " ?avmr km4c:onRoute ?route.\n"
          + " ?ride km4c:hasAVMRecord ?avmr.\n"
          + " ?avmr km4c:hasLastStopTime ?time.\n"
          + " ?time <http://schema.org/value> ?timeInstant.\n"
          + "}\n"
          + "GROUP BY ?ride ORDER BY DESC (?avmrNew) LIMIT 10";
  TupleQuery tupleQueryAVM = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringAVM);
 System.out.println(queryStringAVM);
  long start = System.nanoTime();
  TupleQueryResult resultAVM = tupleQueryAVM.evaluate();
  logQuery(filterQuery(queryStringAVM), "get-avm-1", "any", nomeFermata, System.nanoTime() - start);

  try {
    int i = 0;
    while (resultAVM.hasNext()) {
      BindingSet bindingSet = resultAVM.next();
      String valueOfAVMR = bindingSet.getValue("avmrNew").toString();
      if (i > 0) {
        filtroSecondaQuery += "UNION";
      }
      filtroSecondaQuery += "{ <" + valueOfAVMR + "> km4c:includeForecast ?previsione.\n";
      //filtroSecondaQuery += "  <" + valueOfAVMR + "> km4c:concernLine ?linea.\n";
      filtroSecondaQuery += "  <" + valueOfAVMR + "> km4c:onRoute ?route.\n";
      filtroSecondaQuery += "  <" + valueOfAVMR + "> km4c:rideState ?stato.\n";
      filtroSecondaQuery += "  ?ride dcterms:identifier ?idRide.\n";
      filtroSecondaQuery += "  ?ride km4c:hasAVMRecord <" + valueOfAVMR + ">.}\n";
      i++;
    }
    if(i==0) {
      out.println("<b><span name=\"lbl\" caption=\"msg_real_time\">Real-time data currently not available</span></b>");
      return;
    }
  } catch (Exception e) {
    //out.println(e.getMessage());
    //out.println("");
  }
  String queryStringAVM2 = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
          + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
          + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
          + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
          + "PREFIX schema:<http://schema.org/>\n"
          + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
          + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
          + "PREFIX time:<http://www.w3.org/2006/time#>\n"
          + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
          + "SELECT DISTINCT ?arrivoPrevistoIstante ?route ?linea ?stato ?idRide ?bsLast ?rName ?bsFirst WHERE {\n"
          + "	?fermata rdf:type km4c:BusStop.\n"
          + "	?fermata foaf:name \"" + nomeFermata + "\".\n"
          + "	?fermata km4c:hasForecast ?previsione.\n"
          + filtroSecondaQuery
          + "	?previsione km4c:hasExpectedTime ?arrivoPrevisto.\n"
          + "	?arrivoPrevisto schema:value ?arrivoPrevistoIstante.\n"
          + "	FILTER (?arrivoPrevistoIstante >= now()).\n"
          + "   ?ride km4c:onRoute ?route.\n"
          + "   ?route km4c:hasLastStop ?bse.\n"
          + "   ?bse foaf:name ?bsLast.\n"
          + "   ?route km4c:hasFirstStop ?bss.\n"
          + "   ?bss foaf:name ?bsFirst.\n"
          + " ?route foaf:name ?rName.\n"
          + " ?route opengis:hasGeometry ?geometry .\n"
          + " ?geometry opengis:asWKT ?polyline .\n"
          + " ?linea km4c:hasRoute ?route.\n"
          /*+ " ?ride km4c:onRoute ?route.\n"
          + " ?route km4c:direction ?dir.\n"
          + " ?route km4c:hasSection ?rs.\n"
          + " ?rs km4c:distance ?mxdist.\n"
          + " ?rs km4c:endsAtStop ?bse.\n"
          + " ?bse foaf:name ?bsLast.\n"
          + " {\n"
          + "   SELECT ?route ?dir (max(?dist) as ?mxdist) WHERE {\n"
          + "     ?route km4c:hasSection ?rs.\n"
          + "     ?route km4c:direction ?dir.\n"
          + "     ?rs km4c:distance ?dist.\n"
          + "   } group by ?route ?dir\n"
          + " }"*/
          + "	}\n"
          + "	ORDER BY ASC (?arrivoPrevistoIstante)	LIMIT 6";

  /*String queryStringAVM2 = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
   + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
   + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
   + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
   + "PREFIX schema:<http://schema.org/>\n"
   + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
   + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
   + "PREFIX time:<http://www.w3.org/2006/time#>\n"
   + "SELECT distinct ?linea ?stato ?arrivoPrevistoIstante ?idRide WHERE {\n"
   + "{\n"
   + "SELECT ?ride (MAX(?avmr) AS ?avmrLast) WHERE{\n"
   + "?bs rdf:type km4c:BusStop.\n"
   + "?bs foaf:name \"" + nomeFermata + "\".\n"
   + "?bs km4c:hasForecast ?bsf.\n"
   + "?avmr km4c:includeForecast ?bsf.\n"
   //+ "?avmr km4c:concernLine ?tpll."
   + "?ride km4c:hasAVMRecord ?avmr.\n"
   //+ "?avmr km4c:hasLastStopTime/schema:value ?timeInstant."
   + "}\n"
   + "GROUP BY ?ride \n"
   + "ORDER BY DESC (?avmrLast)\n"
   + "LIMIT 15\n"
   + "}\n"
   + "?bs rdf:type km4c:BusStop.\n"
   + "?bs foaf:name \"" + nomeFermata + "\".\n"
   + "?bs km4c:hasForecast ?previsione.\n"
   + "?avmrLast km4c:includeForecast ?previsione.\n"
   + "?previsione km4c:expectedTime ?arrivoPrevistoIstante.\n"
   + "?avmrLast km4c:concernLine ?linea.\n"
   + "?avmrLast km4c:rideState ?stato.\n"
   + "?ride km4c:hasAVMRecord ?avmrLast.\n"
   + "?ride dcterms:identifier ?idRide.\n"
   + "FILTER(?arrivoPrevistoIstante>=now())\n"
   + "}\n"
   + "order by ?arrivoPrevistoIstante\n";*/
  start = System.nanoTime();
  TupleQuery tupleQueryAVM2 = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringAVM2);
  TupleQueryResult resultAVM2 = tupleQueryAVM2.evaluate();
  logQuery(filterQuery(queryStringAVM2), "get-avm-2", "any", nomeFermata, System.nanoTime() - start);
  System.out.println(queryStringAVM2);
  if (resultAVM2.hasNext()) {
    out.println("<div class=\"avm\"><b><span name=\"lbl\" caption=\"next_transits\">Next transits</span>:</b> <br />");
    out.println("<table>");
    out.println("<tr>");
    out.println("<td><b>Time</b></td>");
    out.println("<td><b>Line</b></td>");
    out.println("<td><b>Status</b></td>");
    //out.println("<td><b>Corsa</b></td>");
    out.println("<td><b>Direction</b></td>");
    out.println("</tr>");

    while (resultAVM2.hasNext()) {
      BindingSet bindingSet2 = resultAVM2.next();
      String valueOfArrivoPrevistoIstante = bindingSet2.getValue("arrivoPrevistoIstante").stringValue();
      String valueOfLinea = "";
      String bsLast = "";
      String bsFirst = "";
      String valueOfStato = "";
      String route = "";
      String nomeLinea = "";
      if (bindingSet2.getValue("linea") != null) {
        valueOfLinea = bindingSet2.getValue("linea").stringValue();
      }
      if (bindingSet2.getValue("stato") != null) {
        valueOfStato = bindingSet2.getValue("stato").stringValue();
      }
      if (bindingSet2.getValue("idRide") != null) {
        idRide = bindingSet2.getValue("idRide").stringValue();
      }
      if (bindingSet2.getValue("route") != null) {
        route = bindingSet2.getValue("route").stringValue();
      }
      if (bindingSet2.getValue("bsLast") != null) {
        bsLast = bindingSet2.getValue("bsLast").stringValue();
      }
      if (bindingSet2.getValue("bsFirst") != null) {
        bsFirst = bindingSet2.getValue("bsFirst").stringValue();
      }
      String direction = bsFirst+" &#10132; "+bsLast;
      route = route.replace("http://www.disit.org/km4city/resource/", "");
      
      /*Class.forName("com.mysql.jdbc.Driver");
        conMySQL = ConnectionPool.getConnection(); //DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
        String query = "SELECT routeLetter FROM Codici_route_ataf where codRoute = '"+route+"'";
        // create the java statement
        st = conMySQL.createStatement();
        // execute the query, and get a java resultset
        rs = st.executeQuery(query);
        String letter = "";
        // iterate through the java resultset
        while (rs.next()) {
            if(!(rs.getString("routeLetter")).equals("_")){
            letter = rs.getString("routeLetter");
            }
        }*/
      valueOfLinea = valueOfLinea.replace("http://www.disit.org/km4city/resource/", "");
      if (bindingSet2.getValue("rName") != null) {
        nomeLinea = bindingSet2.getValue("rName").stringValue();
      }
      
      valueOfArrivoPrevistoIstante = valueOfArrivoPrevistoIstante.substring(11, 19); //solo hh:mm:ss
      out.println("<tr>");
      // out.println("<td>" + idRide + "</td>");
      out.println("<td>" + valueOfArrivoPrevistoIstante + "</td>");
      out.println("<td>" + nomeLinea + "</td>");
      if (valueOfStato.equals("Ritardo")) {
        out.println("<td style=\"color:red;\">" + valueOfStato + "</td>");
      } else {
        if (valueOfStato.equals("Anticipo")) {
          out.println("<td style=\"color:green;\">" + valueOfStato + "</td>");
        } else {
          out.println("<td>" + valueOfStato + "</td>");
        }
      }
      out.println("<td class='percorso' onclick='showLinea(\""+ valueOfLinea +"\",\""+route+"\",\""+direction+"\",\""+nomeLinea+"\")'>" + direction + "</td>");
      out.println("</tr>");
      
    }
    out.println("</table>");
  } else {
    out.println("<b><span name=\"lbl\" caption=\"msg_real_time\">Real-time data currently not available</span></b>");
  }
  con.close();
%>