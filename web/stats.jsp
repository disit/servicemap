<%/* ServiceMap.
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
      Repository repo = new SPARQLRepository(sparqlEndpoint);
      repo.initialize();
      RepositoryConnection con = repo.getConnection();
      int nn=0;

      String queryLastAVM = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "?x a km4c:AVMRecord."
              + "?x dcterms:created ?d."
              + "}  order by desc(?d) limit 1";
      
      String queryAVM = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?yy ?mm ?dd (count(*) as ?c) where {"
              + "graph ?g {"
              + "?x a km4c:AVMRecord."
              + "?x dcterms:created ?d."
              + "}"
              + "bind( year(?d) as ?yy)"
              + "bind( month(?d) as ?mm)"
              + "bind( day(?d) as ?dd)"
              + "}  group by ?yy ?mm ?dd order by desc(?yy) desc(?mm) desc(?dd)";
      
      nn+=formatStat(out, con, queryLastAVM, queryAVM, "AVM status", "avm");

      String queryLastMeteo = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "?x a km4c:WeatherReport."
              + "?x km4c:updateTime/<http://schema.org/value> ?d."
              + "}  order by desc(?d) limit 1";

      String queryMeteo = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?yy ?mm ?dd (count(*) as ?c) where {"
              + "graph ?g {"
              + "?x a km4c:WeatherReport."
              + "?x km4c:updateTime/<http://schema.org/value> ?d."
              + "}"
              + "bind( year(?d) as ?yy)"
              + "bind( month(?d) as ?mm)"
              + "bind( day(?d) as ?dd)"
              + "}  group by ?yy ?mm ?dd order by desc(?yy) desc(?mm) desc(?dd)";
      nn+=formatStat(out, con, queryLastMeteo, queryMeteo, "Meteo status", "meteo");

      String queryLastParking = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "   ?x a km4c:SituationRecord."
              + "   ?x km4c:observationTime/dcterms:identifier ?d."
              + "}  order by desc(?d) limit 1";

      String queryParking = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?yy ?mm ?dd (count(*) as ?c) where {"
              + (sparqlType.equals("virtuoso") ? "{ select distinct * where { " : "")
              + "   ?x a km4c:SituationRecord."
              + "   ?x km4c:observationTime/dcterms:identifier ?d."
              + (sparqlType.equals("virtuoso") ? "}} " : "")
              + "bind( year(?d) as ?yy)"
              + "bind( month(?d) as ?mm)"
              + "bind( day(?d) as ?dd)"
              + "}  group by ?yy ?mm ?dd order by desc(?yy) desc(?mm) desc(?dd)";
      nn+=formatStat(out, con, queryLastParking, queryParking, "Parking status", "park");

      String queryLastSensor = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "   ?x a km4c:Observation."
              + "   ?x km4c:measuredTime/dcterms:identifier ?d."
              + "}  order by desc(?d) limit 1";

      String querySensor = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?yy ?mm ?dd (count(*) as ?c) where {"
              + (sparqlType.equals("virtuoso") ? "{ select distinct * where { " : "")
              + "   ?x a km4c:Observation."
              + "   ?x km4c:measuredTime/dcterms:identifier ?d."
              + (sparqlType.equals("virtuoso") ? "}} " : "")
              + "bind( year(?d) as ?yy)"
              + "bind( month(?d) as ?mm)"
              + "bind( day(?d) as ?dd)"
              + "}  group by ?yy ?mm ?dd order by desc(?yy) desc(?mm) desc(?dd)";
      nn+=formatStat(out, con, queryLastSensor, querySensor, "Sensor status", "sensor");
      out.println("<div style='clear:both;padding-top:20px;'><b>total records: </b>"+nn+"</div>");
      %>
  </body>
</html>
<%!
    private int formatStat(JspWriter out, RepositoryConnection con, final String queryLast, String query, String heading, String id) throws Exception {
      TupleQuery tupleQuery;
      TupleQueryResult result;
      int n;
      String lst="";

      tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryLast);
      result = tupleQuery.evaluate();
      if(result.hasNext()) {
        BindingSet bindingSet = result.next();
        lst = bindingSet.getValue("d").stringValue().substring(0, 19);   
      }
      
      long start = System.nanoTime();
      tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
      result = tupleQuery.evaluate();
      logQuery(filterQuery(query), "stat-"+id, "any", "", System.nanoTime() - start);
      out.println("<div id='"+id+"' style='float:left;margin-right:20px'><h2>"+heading+"</h2><small>last: "+lst+"</small>");
      out.println("<table><tr><th>y</th><th>m</th><th>d</th><th>count</th></tr>");
      n=0;
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String year = bindingSet.getValue("yy").stringValue();
        String month = bindingSet.getValue("mm").stringValue();
        String day = bindingSet.getValue("dd").stringValue();
        String count = bindingSet.getValue("c").stringValue();
        n+=Integer.parseInt(count);
        out.println("<tr><td align='right'>" + year + "</td><td align='right'>" + month + "</td><td align='right'>" + day + "</td><td align='right'>" + count + "</td></tr>");
      }
      out.println("<tr><td colspan='3' align='right'><b>total</b></td><td align='right'>"+n+"</td></tr>");
      out.println("</table></div>");
      return n;
    }
%>