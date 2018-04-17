<%/* ServiceMap.
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
%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file= "/include/parameters.jsp" %>
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Statistics</title>
  </head>
  <body>
    <h1>Realtime statistics</h1>
    <%
      RepositoryConnection con = ServiceMap.getSparqlConnection();
      int nn=0;

      String queryLastAVM = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "?x a km4c:AVMRecord."
              + "?x dcterms:created ?d."
              + "}  order by desc(?d) limit 1";
      
      String queryAVM = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?dd (count(*) as ?c) where {"
              + "graph ?g {"
              + "?x a km4c:AVMRecord."
              + "?x dcterms:created ?d."
              + "}"
              + "bind( xsd:date(?d) as ?dd)"
              + "}  group by ?dd order by desc(?dd)";
      
      nn+=formatStat(out, con, queryLastAVM, queryAVM, "AVM status", "avm");

      String queryLastMeteo = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "?x a km4c:WeatherReport."
              + "?x km4c:updateTime/<http://schema.org/value> ?d."
              + "}  order by desc(?d) limit 1";

      String queryMeteo = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?dd (count(*) as ?c) where {"
              + "graph ?g {"
              + "?x a km4c:WeatherReport."
              + "?x km4c:updateTime/<http://schema.org/value> ?d."
              + "}"
              + "bind( xsd:date(?d) as ?dd)"
              + "}  group by ?dd order by desc(?dd)";
      nn+=formatStat(out, con, queryLastMeteo, queryMeteo, "Meteo status", "meteo");

      String queryLastParking = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "   ?x a km4c:SituationRecord."
              + "   ?x km4c:observationTime/dcterms:identifier ?d."
              + "}  order by desc(?d) limit 1";

      String queryParking = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?dd (count(*) as ?c) where {"
              + (sparqlType.equals("virtuoso") ? "{ select distinct * where { " : "")
              + "   ?x a km4c:SituationRecord."
              + "   ?x km4c:observationTime/dcterms:identifier ?d."
              + (sparqlType.equals("virtuoso") ? "}} " : "")
              + "bind( xsd:date(?d) as ?dd)"
              + "}  group by ?dd order by desc(?dd)";
      nn+=formatStat(out, con, queryLastParking, queryParking, "Parking status", "park");

      String queryLastSensor = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "   ?x a km4c:Observation."
              + "   ?x km4c:measuredTime/dcterms:identifier ?d."
              + "}  order by desc(?d) limit 1";

      String querySensor = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?dd (count(*) as ?c) where {"
              + (sparqlType.equals("virtuoso") ? "{ select distinct * where { " : "")
              + "   ?x a km4c:Observation."
              + "   ?x km4c:measuredTime/dcterms:identifier ?d."
              + (sparqlType.equals("virtuoso") ? "}} " : "")
              + "bind( xsd:date(?d) as ?dd)"
              + "}  group by ?dd order by desc(?dd)";
      nn+=formatStat(out, con, queryLastSensor, querySensor, "Sensor status", "sensor");
      out.println("<div style='clear:both;padding-top:20px;'><b>total records: </b>"+nn+"</div>");
      %>
  </body>
</html>
<%!
    private int formatStat(JspWriter out, RepositoryConnection con, final String queryLast, String query, String heading, String id) throws Exception {
      TupleQuery tupleQuery;
      TupleQueryResult result;
      int n = 0;
      String lst="";

      out.println("<div id='"+id+"' style='float:left;margin-right:20px'><h2>"+heading+"</h2>");
      try {
        tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryLast);
        result = tupleQuery.evaluate();
        if(result.hasNext()) {
          BindingSet bindingSet = result.next();
          lst = bindingSet.getValue("d").stringValue().substring(0, 19);   
        }
        out.println("<small>last: "+lst+"</small>");
      } catch(Exception e) {
        ServiceMap.notifyException(e);
      }
      out.println("<table><tr><th>date</th><th>count</th></tr>");
      try {
        long start = System.nanoTime();
        tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
        result = tupleQuery.evaluate();
        logQuery(filterQuery(query), "stat-"+id, "any", "", System.nanoTime() - start);
        n=0;
        while (result.hasNext()) {
          BindingSet bindingSet = result.next();
          String day = (bindingSet.getValue("dd")!=null ? bindingSet.getValue("dd").stringValue().substring(0,10) : "");
          String count = (bindingSet.getValue("c")!=null ? bindingSet.getValue("c").stringValue() : "");
          n+=Integer.parseInt(count);
          out.println("<tr><td align='right'>" + day + "</td><td align='right'>" + count + "</td></tr>");
        }
        out.println("<tr><td colspan='1' align='right'><b>total</b></td><td align='right'>"+n+"</td></tr>");
      } catch(Exception e) {
        ServiceMap.notifyException(e);
      }
      out.println("</table>");
      out.println("</div>");
      return n;
    }
%>