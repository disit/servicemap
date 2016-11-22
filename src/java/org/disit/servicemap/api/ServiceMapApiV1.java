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
package org.disit.servicemap.api;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.jsp.JspWriter;
import org.disit.servicemap.Configuration;
import org.disit.servicemap.ServiceMap;
import static org.disit.servicemap.ServiceMap.escapeJSON;
import static org.disit.servicemap.ServiceMap.logQuery;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.disit.servicemap.ConnectionPool;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

/**
 *
 * @author bellini
 */
public class ServiceMapApiV1 extends ServiceMapApi {

  public void queryBusStop(JspWriter out, RepositoryConnection con, String idService, String lang, String realtime, String uid) throws Exception {
    queryTplStop(out, con, idService, "BusStop", lang, realtime, uid);
  }
  
  public void queryTplStop(JspWriter out, RepositoryConnection con, String idService, String tplclass, String lang, String realtime, String uid) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String nomeFermata = "";
    String type = tplclass;
    if (lang.equals("it")) {
      if(tplclass.equals("BusStop"))
        type = "Fermata Bus";
      else if(tplclass.equals("Tram_stops"))
        type = "Fermata Tram";
      else if(tplclass.equals("Train_station"))
        type = "Stazione treno";
    }
    
    String queryStringStop = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
            + "PREFIX schema:<http://schema.org/#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n"
            + "SELECT distinct ?nomeFermata ?bslat ?bslong ?address ?ag ?agname WHERE {\n"
            + "	<" + idService + "> rdf:type km4c:"+tplclass+"."
            + "	<" + idService + "> foaf:name ?nomeFermata."
            + " OPTIONAL {<" + idService + "> km4c:isInRoad ?road."
            + "     ?road km4c:extendName ?address}."
            + "	<" + idService + "> geo:lat ?bslat."
            + "	<" + idService + "> geo:long ?bslong."
            + " OPTIONAL {"
            + "  {?st gtfs:stop <"+idService+">.}UNION{?st gtfs:stop [owl:sameAs <"+idService+">]}" 
            + "  ?st gtfs:trip ?t."
            +"   ?t gtfs:route/gtfs:agency ?ag."
            +"   ?ag foaf:name ?agname."
            + " }"
            + "}LIMIT 1";

    TupleQuery tupleQueryBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringStop);
    long ts = System.nanoTime();
    TupleQueryResult busStopResult = tupleQueryBusStop.evaluate();
    logQuery(queryStringStop, "API-busstop-info", sparqlType, idService, System.nanoTime() - ts);
    out.println("{\"BusStop\": ");
    out.println("{ "
            + "\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");

    int i = 0;

    try {
      while (busStopResult.hasNext()) {
        BindingSet bindingSetBusStop = busStopResult.next();

        String valueOfBSLat = bindingSetBusStop.getValue("bslat").stringValue();
        String valueOfBSLong = bindingSetBusStop.getValue("bslong").stringValue();
        String valueOfRoad = "";
        String valueOfAgName = "";
        String valueOfAgUri = "";
        if (bindingSetBusStop.getValue("address") != null) {
          valueOfRoad = bindingSetBusStop.getValue("address").stringValue();
        }
        nomeFermata = bindingSetBusStop.getValue("nomeFermata").stringValue();
        if(bindingSetBusStop.getValue("agname") != null)
          valueOfAgName = bindingSetBusStop.getValue("agname").stringValue();
        if(bindingSetBusStop.getValue("ag") != null)
          valueOfAgUri = bindingSetBusStop.getValue("ag").stringValue();
        if (i != 0) {
          out.println(", ");
        }
        float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);
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
                + "    \"name\": \"" + nomeFermata + "\", "
                + "    \"serviceUri\": \"" + idService + "\", "
                + "    \"typeLabel\": \"" + type + "\", "
                + "    \"address\": \"" + valueOfRoad + "\", "
                + "    \"agency\": \"" + valueOfAgName + "\", "
                + "    \"agencyUri\": \"" + valueOfAgUri + "\", "
                + "    \"serviceType\": \"TransferServiceAndRenting_" + tplclass + "\",\n"
                + "    \"photos\": " + ServiceMap.getServicePhotos(idService) + ",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(idService,"thumbs") + ",\n"
                + "    \"photoOrigs\": " + ServiceMap.getServicePhotos(idService,"originals") + ",\n"
                + "    \"avgStars\": " + avgServiceStars[0] + ",\n"
                + "    \"starsCount\": " + (int) avgServiceStars[1] + ",\n"
                + (uid != null ? "    \"userStars\": " + ServiceMap.getServiceStarsByUid(idService, uid) + ",\n" : "")
                + "    \"comments\": " + ServiceMap.getServiceComments(idService)
                + "}, "
                + "\"id\": " + Integer.toString(i + 1) + " "
                + "}");
        i++;
      }
    } catch (Exception e) {
      out.println(e.getMessage());
    }

    out.println("] "
            + "},");

    TupleQueryResult resultLines = queryBusLines(idService, con);

    if (resultLines != null) {
      try {
        out.println("\"busLines\":");
        out.println("{ \"head\": {"
                + "\"busStop\": "
                + "\"" + nomeFermata + "\""
                + ","
                + "\"vars\": ["
                + "\"busLine\",\"lineUri\",\"lineDesc\"]"
                + "},");
        out.println("\"results\": {");
        out.println("\"bindings\": [");

        int j = 0;
        while (resultLines.hasNext()) {
          BindingSet bindingSetLines = resultLines.next();
          String idLine = "";
          if(bindingSetLines.getValue("id")!=null)
            idLine = JSONObject.escape(bindingSetLines.getValue("id").stringValue());
          String lineUri = bindingSetLines.getValue("line").stringValue();
          String lineDesc = JSONObject.escape(bindingSetLines.getValue("desc").stringValue());
          if (j != 0) {
            out.println(", ");
          }
          out.println("{"
                  + "\"busLine\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + idLine + "\" "
                  + " },"
                  + "\"lineUri\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + lineUri + "\" "
                  + " },"
                  + "\"lineDesc\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + lineDesc + "\" "
                  + " }"
                  + " }");
          j++;
        }
        out.println("]}}");

      } catch (Exception e) {
        out.println(e.getMessage());
      }
    }
    out.println(","+queryTplTimeTable(con,idService));
    if ("true".equalsIgnoreCase(realtime)) {
      String queryStringAVM = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>  "
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
              + "PREFIX schema:<http://schema.org/#> "
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>  "
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
              + "PREFIX time:<http://www.w3.org/2006/time#>  "
              + "SELECT ?ride  (MAX(?avmr) AS ?avmrNew)   "
              + "WHERE{  "
              //+ " <" + idService + "> rdf:type km4c:BusStop."
              + " <" + idService + "> km4c:hasForecast ?bsf."
              + " <" + idService + ">  km4c:hasForecast ?bsf.\n"
              + " ?avmr km4c:includeForecast ?bsf.\n"
              + " OPTIONAL {?rs km4c:endsAtStop <" + idService + "> }.\n"
              + " OPTIONAL {?rs km4c:startsAtStop <" + idService + "> }.\n"
              + " ?route km4c:hasSection ?rs.\n"
              + " ?avmr km4c:onRoute ?route.\n"
              //+ " ?avmr km4c:concernLine ?tpll."
              + " ?ride km4c:hasAVMRecord ?avmr."
              + " ?avmr km4c:hasLastStopTime ?time."
              + " ?time <http://schema.org/value> ?timeInstant."
              + "}GROUP BY ?ride ORDER BY DESC (?avmrNew) LIMIT 10";

      TupleQuery tupleQueryAVM = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringAVM);
      ts = System.nanoTime();
      TupleQueryResult resultAVM = tupleQueryAVM.evaluate();
      logQuery(queryStringAVM, "API-AVM1-info", sparqlType, idService, System.nanoTime() - ts);
      String filtroSecondaQuery = "";
      try {
        i = 0;
        while (resultAVM.hasNext()) {
          BindingSet bindingSetAVM = resultAVM.next();
          String valueOfAVMR = bindingSetAVM.getValue("avmrNew").stringValue();
          if (i > 0) {
            filtroSecondaQuery += "UNION";
          }
          filtroSecondaQuery += " { ?ride km4c:hasAVMRecord <" + valueOfAVMR + ">.";
          filtroSecondaQuery += "  ?ride dcterms:identifier ?idRide.";
          filtroSecondaQuery += "  <" + valueOfAVMR + "> km4c:includeForecast ?previsione;";
          filtroSecondaQuery += "   km4c:onRoute ?route;\n";
          filtroSecondaQuery += "   km4c:rideState ?stato.} ";
          i++;
        }
        if (i == 0) {
          out.println(",\"realtime\": {}");
          out.println("}");
          return;
        }
      } catch (Exception e) {
        out.println(e.getMessage());
      }
      String queryStringAVM2 = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX time:<http://www.w3.org/2006/time#>\n"
              + "SELECT DISTINCT ?arrivoPrevistoIstante ?linea ?stato ?idRide ?bsLast ?rName ?bsFirst WHERE {\n"
              + "	?fermata rdf:type km4c:BusStop.\n"
              + "	?fermata foaf:name \"" + nomeFermata + "\".\n"
              + "	?fermata km4c:hasForecast ?previsione\n."
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
              + "	}\n"
              + "	ORDER BY ASC (?arrivoPrevistoIstante)	LIMIT 6";

      TupleQuery tupleQueryAVM2 = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringAVM2);
      String idRide = "";
      ts = System.nanoTime();
      TupleQueryResult resultAVM2 = tupleQueryAVM2.evaluate();
      logQuery(queryStringAVM2, "API-AVM2-info", sparqlType, idService, System.nanoTime() - ts);
      out.println(",\"realtime\":");
      if (resultAVM2.hasNext()) {
        out.println("{ \"head\": {"
                + "\"busStop\":[ "
                + "\"" + nomeFermata + "\""
                + "],"
                + "\"vars\":[ "
                + "\"arrivalTime\", "
                + "\"busLine\","
                + "\"status\","
                + "\"direction\","
                + "\"ride\""
                + "]"
                + "},");
        out.println("\"results\": {");
        out.println("\"bindings\": [");
        int k = 0;
        while (resultAVM2.hasNext()) {
          BindingSet bindingSet2 = resultAVM2.next();
          String valueOfArrivoPrevistoIstante = bindingSet2.getValue("arrivoPrevistoIstante").stringValue();
          String valueOfLinea = "";
          String bsLast = "";
          String bsFirst = "";
          String nomeLinea = "";
          if (bindingSet2.getValue("linea") != null) {
            valueOfLinea = bindingSet2.getValue("linea").stringValue();
          }
          String valueOfStato = "";
          if (bindingSet2.getValue("stato") != null) {
            valueOfStato = bindingSet2.getValue("stato").stringValue();
          }
          if (bindingSet2.getValue("idRide") != null) {
            idRide = bindingSet2.getValue("idRide").stringValue();
          }
          if (bindingSet2.getValue("bsLast") != null) {
            bsLast = bindingSet2.getValue("bsLast").stringValue();
          }
          if (bindingSet2.getValue("bsFirst") != null) {
            bsFirst = bindingSet2.getValue("bsFirst").stringValue();
          }
          String direction = bsFirst + " &#10132; " + bsLast;
          if (bindingSet2.getValue("rName") != null) {
            nomeLinea = bindingSet2.getValue("rName").stringValue();
          }

          valueOfLinea = valueOfLinea.replace("http://www.disit.org/km4city/resource/", "");
          valueOfArrivoPrevistoIstante = valueOfArrivoPrevistoIstante.substring(11, 19);

          if (k != 0) {
            out.println(", ");
          }

          out.println("{"
                  + "\"arrivalTime\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfArrivoPrevistoIstante + "\" "
                  + " },"
                  + "\"busLine\": { "
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + nomeLinea + "\" "
                  + " },"
                  + "\"status\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfStato + "\" "
                  + " },"
                  + "\"direction\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + direction + "\" "
                  + " },"
                  + "\"ride\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + idRide + "\" "
                  + " }");
          out.println(" }");
          k++;
        }
        out.println("]}}");
      } else {
        out.println("{}");
      }
    }
    out.println("}");
  }
  
  public String queryTplTimeTable(RepositoryConnection con, String stopUri) throws Exception {
    String s = "\"timetable\":";
    /*String queryString = "prefix dcterms:<http://purl.org/dc/terms/>\n" +
        "select distinct ?at ?dt ?routeName ?lineName ?lineDesc ?now where {\n" +
        "{\n" +
        "?s km4c:hasBusStop [owl:sameAs <"+stopUri+">]\n" +
        "} UNION {\n" +
        "?s km4c:hasBusStop <"+stopUri+">\n" +
        "}\n" +
        "?r km4c:hasSection ?s.\n" +
        "?r km4c:hasTplServiceDate ?sd.\n" +
        "?r dcterms:description ?routeName.\n" +
        "?sd dcterms:date ?d.\n" +
        "?s km4c:arrivalTime ?at.\n" +
        "?s km4c:departureTime ?dt.\n" +
        "bind(xsd:time(now()) as ?now)\n" +
        "filter(xsd:date(?d)=xsd:date(now()) && xsd:time(?at)>?now)\n" +
        "?r km4c:hasPublicTransportLine ?l.\n" +
        "optional{?l foaf:name ?lineName.}\n" +
        "?l dcterms:description ?lineDesc.\n" +
        "} \n" +
        "order by ?d ?at ";*/
    String queryString = "prefix dcterms:<http://purl.org/dc/terms/>\n" +
        "prefix gtfs:<http://vocab.gtfs.org/terms#>\n" +
        "select distinct ?at ?dt ?routeName ?lineName ?lineDesc ?trip ?d ?now (xsd:date(?d)=xsd:date(now()) as ?today) where {\n" +
        "{\n" +
        "?st gtfs:stop [owl:sameAs <"+stopUri+">]\n" +
        "} UNION {\n" +
        "?st gtfs:stop <"+stopUri+">\n" +
        "}\n" +
        "?st gtfs:trip ?trip.\n" +
        "?trip gtfs:service ?sd.\n" +
        "?sd dcterms:date ?d.\n" +
        "bind(xsd:time(now()) as ?now)\n" +
        "filter((xsd:date(?d)=xsd:date(now()) && str(?at)>str(xsd:time(now())))||(xsd:date(?d)=bif:dateadd(\"day\",1,xsd:date(now())) && str(?at)<str(xsd:time(now()))))\n" +
        "optional{?trip gtfs:headsign ?routeName.}\n" +
        "?st gtfs:arrivalTime ?at.\n" +
        "?st gtfs:departureTime ?dt.\n" +
        "?trip gtfs:route ?route.\n" +
        "optional{?route gtfs:shortName ?lineName.}\n" +
        "?route gtfs:longName ?lineDesc.\n" +
        "} \n" +
        "order by ?d ?at ";

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    long ts = System.nanoTime();
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(queryString, "API-busstop-timetable", "", stopUri, System.nanoTime() - ts);
    System.out.println(queryString);
    try {
      s += "{ \"head\": {"
                + "\"vars\":[ "
                + "\"date\", "
                + "\"arrivalTime\", "
                + "\"lineName\","
                + "\"lineDesc\","
                + "\"routeName\","
                + "\"trip\""
                + "]"
                + "},"
                + "\"results\": {"
                + "\"bindings\": [";
      int i=0;
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();

        String valueOfArrivalTime = bindingSet.getValue("at").stringValue();
        String valueOfDepartureTime = bindingSet.getValue("dt").stringValue();
        String valueOfRouteName = "";
        if(bindingSet.getValue("routeName")!=null)
          valueOfRouteName = JSONObject.escape(bindingSet.getValue("routeName").stringValue());
        String valueOfLineName = "";
        if(bindingSet.getValue("lineName")!=null)
          valueOfLineName = JSONObject.escape(bindingSet.getValue("lineName").stringValue());
        String valueOfLineDesc = JSONObject.escape(bindingSet.getValue("lineDesc").stringValue());
        String valueOfTrip = bindingSet.getValue("trip").stringValue();
        String now = bindingSet.getValue("now").stringValue();
        String today = bindingSet.getValue("today").stringValue();
        String valueOfDate = bindingSet.getValue("d").stringValue();

        String[] dta = fixTPLDateTime(valueOfDate, valueOfArrivalTime);
        String[] dtd = fixTPLDateTime(valueOfDate, valueOfDepartureTime);
        if(dta!=null) {
          valueOfDate = dta[0];
          valueOfArrivalTime = dta[1];
        }
        if(dtd!=null) {
          valueOfDepartureTime = dtd[1];
        }
        //System.out.println("--- "+valueOfArrivalTime+" "+now);
        //per bug virtuoso verifica che effetivamente l'ora sia successiva alla data attuale
        //System.out.println("date: "+valueOfDate+" today:"+today);
        //if(today.equals("0") || valueOfArrivalTime.compareTo(now)>0) {
          if(i>0)
            s += ",";
          s += "{"
                  + "\"date\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfDate + "\" "
                  + " },"
                  + "\"arrivalTime\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfArrivalTime + "\" "
                  + " },"
                  + "\"departureTime\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfDepartureTime + "\" "
                  + " },"
                  + "\"lineName\": { "
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfLineName + "\" "
                  + " },"
                  + "\"lineDesc\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfLineDesc + "\" "
                  + " },"
                  + "\"routeName\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfRouteName + "\" "
                  + " },"
                  + "\"trip\": {"
                  + "\"type\": \"uri\","
                  + "\"value\": \"" + valueOfTrip + "\" "
                  + "}}";
          i++;
        //}
      }
      s+="]}}";
      if(i>0)
        return s;
    } catch (Exception e) {
      e.printStackTrace();
    }    
    return "\"timetable\":{}";
  }
  
  // fix the 24:30:12 as 00:30:12 and a day in advance
  public static String[] fixTPLDateTime(String date, String time) throws Exception {
    String[] t = time.split(":");
    int hour = Integer.parseInt(t[0]);
    if(hour>23) {
      String[] r = new String[2];
      hour -= 24;
      r[1]=String.format("%02d", hour)+":"+t[1]+":"+t[2];
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Calendar c = Calendar.getInstance();
      c.setTime(sdf.parse(date));
      c.add(Calendar.DATE, 1);  // number of days to add
      r[0] = sdf.format(c.getTime());  // dt is now the new date
      return r;
    }
    return null;
  }
  
  public void queryBusRoutes(JspWriter out, RepositoryConnection con, String agency, String line, String stopName, boolean getGeometry) {
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    final String km4cVersion = conf.get("km4cVersion", "new");

    /*String queryForLine = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
            + "PREFIX schema:<http://schema.org/>"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>"
            + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
            + "SELECT DISTINCT ?line ?routeId ?routeName ?bss ?bse ?polyline WHERE {"
            + " ?tpll rdf:type km4c:PublicTransportLine."
            + (line!=null ? " BIND( \"" + line + "\" AS ?line)." : "")
            + " ?tpll dcterms:identifier ?line."
            + " ?tpll km4c:hasRoute ?route."
            + " ?route dcterms:identifier ?routeId.\n"
            + " ?route foaf:name ?routeName.\n"
            + " ?route km4c:hasFirstStop [foaf:name ?bss].\n"
            + " ?route km4c:hasLastStop [foaf:name ?bse]."
            + (getGeometry ? " ?route opengis:hasGeometry [opengis:asWKT ?polyline].\n" : "")
            + (busStopName != null ? " ?route km4c:hasSection ?rs."
                    + " ?rs km4c:endsAtStop ?bs1."
                    + " ?rs km4c:startsAtStop ?bs2."
                    + " { ?bs1 foaf:name \"" + busStopName + "\"."
                    + " }UNION "
                    + " {?bs2 foaf:name \"" + busStopName + "\" . "
                    + " } " : "")
            + "} ORDER BY ?line ";*/
    String stopFilter = "";
    if(stopName!=null) {
      if(stopName.startsWith("http://")) {
        stopFilter = "{?stx gtfs:stop <"+stopName+">}UNION{?stx gtfs:stop [ owl:sameAs <"+stopName+">]}.\n";
      } else {
        stopFilter = "?stx gtfs:stop/foaf:name \""+stopName+"\".\n";
      }
      stopFilter += "?stx gtfs:trip ?trip.";
    }
    String lineFilter = "";
    if(line != null) {
      if(line.startsWith("http://"))
        lineFilter = "?trip gtfs:route <"+line+">.\n";
      else {
        lineFilter =
              "?ln gtfs:shortName \""+line+"\".\n" +
              "?trip gtfs:route ?ln.\n";
        if(agency==null)
          agency = "Ataf&Linea";
        if(agency.startsWith("http://"))
          lineFilter += "?ln gtfs:agency <"+agency+">.";
        else
          lineFilter += "?ln gtfs:agency/foaf:name \""+agency+"\".";
      }
    } else if(stopName==null) {
        lineFilter =
              "?trip gtfs:route ?ln.\n";
        if(agency==null)
          agency = "Ataf&Linea";
        if(agency.startsWith("http://"))
          lineFilter += "?ln gtfs:agency <"+agency+">.";
        else
          lineFilter += "?ln gtfs:agency/foaf:name \""+agency+"\".";
    }
              
    String queryForLine = 
            "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n" +
            "PREFIX dcterms:<http://purl.org/dc/terms/>\n" +
            "PREFIX ogis:<http://www.opengis.net/ont/geosparql#>\n" +
            "SELECT DISTINCT ?line ?shape ?bss ?bse (min(?trip) as ?routeId) ?polyline {{\n" +
            "SELECT ?line ?trip (MAX(?st) as ?mx) (MIN(?st) as ?mn) {\n" +
            stopFilter +
            "?st gtfs:trip ?trip.\n" +
            "?trip gtfs:service/dcterms:date ?d.\n" +
            "filter(xsd:date(?d)=xsd:date(now()))\n" +
            lineFilter +
            "OPTIONAL{?trip gtfs:route/gtfs:shortName ?line1.}\n" +
            "?trip gtfs:route/gtfs:longName ?line2.\n" +
            "BIND(if(?line1,?line1,?line2) as ?line)" +
            "} GROUP BY ?line ?trip\n" +
            "}\n" +
            "?trip ogis:hasGeometry ?shape.\n" +
            (getGeometry ? "?shape ogis:asWKT ?polyline.\n" : "" ) +
            "?mx gtfs:stop/foaf:name ?bse.\n" +
            "?mn gtfs:stop/foaf:name ?bss.\n" +
            "} group by ?line ?shape ?bss ?bse ?polyline order by ?bss ?bse";

    TupleQuery tupleQueryForLine;
    try {
      System.out.println(queryForLine);
      tupleQueryForLine = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForLine);
      long ts = System.nanoTime();
      TupleQueryResult result = tupleQueryForLine.evaluate();
      ServiceMap.logQuery(queryForLine, "API-busroutes", "", stopName, System.nanoTime() - ts);
      out.println("{\"BusRoutes\": [");
      int n =0;
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String valueOfLine = bindingSet.getValue("line").stringValue();
        String valueOfRouteId = bindingSet.getValue("routeId").stringValue();
        String valueOfRouteName = ""; //bindingSet.getValue("routeName").stringValue();
        String valueOfPolyline = null;
        if(bindingSet.getValue("polyline")!=null)
          valueOfPolyline = bindingSet.getValue("polyline").stringValue();
        String valueOfBss = bindingSet.getValue("bss").stringValue();
        String valueOfBse = bindingSet.getValue("bse").stringValue();

        if(n!=0)
          out.println(",");
        out.print("{\n"
                + " \"line\":\""+valueOfLine+"\",\n"
                + " \"route\":\""+valueOfRouteId+"\",\n"
                + " \"routeName\":\""+valueOfRouteName+"\",\n"
                + (valueOfPolyline!=null ? " \"wktGeometry\":\""+ServiceMap.fixWKT(valueOfPolyline)+"\",\n" : "")
                + " \"firstBusStop\":\""+valueOfBss+"\",\n"
                + " \"lastBusStop\":\""+valueOfBse+"\"\n"
                + "}");
        n++;
      }
      out.println("]}");
      return;
    } catch (RepositoryException | MalformedQueryException | QueryEvaluationException | IOException ex) {
      Logger.getLogger(ServiceMapApi.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

public void queryAllBusLines(JspWriter out, RepositoryConnection con, String agency) {
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    final String km4cVersion = conf.get("km4cVersion", "new");

    String queryForLines;
    queryForLines = "SELECT DISTINCT ?sname ?lname ?r WHERE {\n"
              + " ?r rdf:type gtfs:Route.\n"
              + " ?r gtfs:agency <"+agency+">.\n"
              + " OPTIONAL {?r gtfs:shortName ?sname.}\n"
              + " ?r gtfs:longName ?lname.\n"
              + " BIND (xsd:integer(?sname) as ?line)\n"
              + "} ORDER BY ?line";
      
    TupleQuery tupleQuery;
    try {
      tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForLines);
      long ts = System.nanoTime();
      TupleQueryResult result = tupleQuery.evaluate();
      ServiceMap.logQuery(queryForLines, "API-all-buslines", "", "", System.nanoTime() - ts);
      out.println("{\"BusLines\": [");
      int n =0;
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String valueOfShortName = "";
        if(bindingSet.getValue("sname")!=null)
          valueOfShortName = JSONObject.escape(bindingSet.getValue("sname").stringValue());
        String valueOfLongName = JSONObject.escape(bindingSet.getValue("lname").stringValue());
        String valueOfUri = JSONObject.escape(bindingSet.getValue("r").stringValue());

        if(n!=0)
          out.println(",");
        out.print("{\n"
                + " \"agency\":\""+agency+"\",\n"
                + " \"shortName\":\""+valueOfShortName+"\",\n"
                + " \"longName\":\""+valueOfLongName+"\",\n"
                + " \"uri\":\""+valueOfUri+"\"\n"
                + "}");
        n++;
      }
      out.println("]}");
      return;
    } catch (RepositoryException | MalformedQueryException | QueryEvaluationException | IOException ex) {
      Logger.getLogger(ServiceMapApi.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public void queryLatLngServices(JspWriter out, RepositoryConnection con, final String[] coords, String categorie, final String textToSearch, final String raggioBus, String raggioSensori, final String raggioServizi, String risultatiBus, String risultatiSensori, String risultatiServizi, final String language, String cat_servizi, final boolean getGeometry, final boolean inside, boolean photos) throws Exception {
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    final String km4cVersion = conf.get("km4cVersion", "new");
    String lang_tmp = "";
    if (language == null) {
      lang_tmp = "en";
    } else {
      lang_tmp = language;
    }
    final String lang = lang_tmp;

    if (cat_servizi == null) {
      cat_servizi = "categorie";
    }

    List<String> listaCategorieServizi = new ArrayList<String>();
    boolean searchBusStop = true;
    boolean searchSensorSite = true;
    boolean searchServices = true;
    if (categorie != null) {
      String[] arrayCategorie = categorie.split(";");
      // GESTIONE CATEGORIE
      listaCategorieServizi = new ArrayList(Arrays.asList(arrayCategorie));
      searchBusStop = listaCategorieServizi.contains("BusStop");
      listaCategorieServizi.remove("BusStop");
      searchSensorSite = listaCategorieServizi.contains("SensorSite");
      listaCategorieServizi.remove("SensorSite");
      listaCategorieServizi.remove("Event");
      listaCategorieServizi.remove("PublicTransportLine");
      if(listaCategorieServizi.isEmpty())
        searchServices=false;
    }
    String fcc = "";
    try {
      fcc = ServiceMap.filterServices(listaCategorieServizi);
    } catch (Exception e) {
    }
    final String fc = fcc;
    int i = 0;
    int numeroBus = 0;
    int resBusStop = 0;
    int resSensori = 0;
    int resServizi = 0;
    String args = "";
    for (String c : coords) {
      args += c + ";";
    }
    args += categorie + ";" + textToSearch + ";" + raggioBus;
    out.println("{");
    if (searchBusStop && !inside) {
      String type;
      if (lang.equals("it")) {
        type = "Fermata";
      } else {
        type = "BusStop";
      }
      SparqlQuery queryNearBusStop = new SparqlQuery() {
        @Override
        public String query(String type) throws Exception {
          return "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                  + "PREFIX schema:<http://schema.org/#>\n"
                  + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                  + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                  + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                  + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
                  + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
                  + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
                  + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n"
                  + ("count".equals(type) ? "SELECT (COUNT(*) AS ?count) WHERE {\n" : "")
                  + "SELECT DISTINCT ?bs (STR(?nome) AS ?nomeFermata) ?bslat ?bslong ?ag ?agname ?sameas ?x WHERE {\n"
                  + " ?bs geo:lat ?bslat.\n"
                  + " ?bs geo:long ?bslong.\n"
                  + ServiceMap.geoSearchQueryFragment("?bs", coords, raggioBus)
                  + " ?bs rdf:type km4c:BusStop.\n"
                  + " ?bs foaf:name ?nome.\n"
                  + ServiceMap.textSearchQueryFragment("?bs", "foaf:name", textToSearch)
                  + (km4cVersion.equals("old") ? " FILTER ( datatype(?bslat ) = xsd:float )\n"
                          + " FILTER ( datatype(?bslong ) = xsd:float )\n" : "")
                  + " ?st gtfs:stop ?bs.\n"
                  + " ?st gtfs:trip/gtfs:route/gtfs:agency ?ag.\n"
                  + " ?ag foaf:name ?agname.\n"
                  + " OPTIONAL {?bs owl:sameAs ?sameas }"
                  //+ " FILTER NOT EXISTS {?bs owl:sameAs ?bb }\n" //eliminate duplicate
                  + "}"
                  + (type != null ? "}" : " ORDER BY ?dist");
        }
      };
      String queryStringNearBusStop = queryNearBusStop.query(null);
      System.out.println(queryNearBusStop);
      int fullCount = -1;
      if (!risultatiBus.equals("0")) {
        //resBusStop = ((Integer.parseInt(risultatiServizi))/10*3);
        //queryStringNearBusStop += " LIMIT " + resBusStop;
        //fullCount = ServiceMap.countQuery(con, queryNearBusStop.query("count"));

        if (cat_servizi.equals("categorie")) {
          resBusStop = ((Integer.parseInt(risultatiServizi)) / 10 * 3);
          queryStringNearBusStop += " LIMIT " + resBusStop;
          fullCount = ServiceMap.countQuery(con, queryNearBusStop.query("count"));
        } else {
          if (cat_servizi.contains(":")) {
            String parts[] = cat_servizi.split(":");
            if (!parts[1].equals("0")) {
              resBusStop = ((Integer.parseInt(parts[1])) / 10 * 3);
              queryStringNearBusStop += " LIMIT " + resBusStop;
              fullCount = ServiceMap.countQuery(con, queryNearBusStop.query("count"));
            }
          } else {
            queryStringNearBusStop += " LIMIT " + risultatiServizi;
            fullCount = ServiceMap.countQuery(con, queryNearBusStop.query("count"));
          }
        }

      } else {
        fullCount = ServiceMap.countQuery(con, queryNearBusStop.query("count"));
      }

      TupleQuery tupleQueryNearBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringNearBusStop);
      long ts = System.nanoTime();
      TupleQueryResult resultNearBS = tupleQueryNearBusStop.evaluate();
      logQuery(queryStringNearBusStop, "API-nearbusstop", sparqlType, args, System.nanoTime() - ts);
      out.println("\"BusStops\": {");
      if (fullCount >= 0) {
        out.println(" \"fullCount\": " + fullCount + ",");
      }
      out.println(" \"type\": \"FeatureCollection\",\n"
              + " \"features\":[");
      int s = 0;
      while (resultNearBS.hasNext()) {
        BindingSet bindingSetNearBS = resultNearBS.next();
        String valueOfBS = bindingSetNearBS.getValue("bs").stringValue();
        String valueOfNomeFermata = JSONObject.escape(bindingSetNearBS.getValue("nomeFermata").stringValue());
        String valueOfBSLat = bindingSetNearBS.getValue("bslat").stringValue();
        String valueOfBSLong = bindingSetNearBS.getValue("bslong").stringValue();
        String valueOfAgUri = bindingSetNearBS.getValue("ag").stringValue();
        String valueOfAgName = bindingSetNearBS.getValue("agname").stringValue();
        String uriSameAs = null;
        if(bindingSetNearBS.getValue("sameas")!=null)
          uriSameAs = bindingSetNearBS.getValue("sameas").stringValue();

        TupleQueryResult resultLinee = queryBusLines(valueOfBS, con);
        String valueOfLinee = "";
        if (resultLinee != null) {
          while (resultLinee.hasNext()) {
            BindingSet bindingSetLinee = resultLinee.next();
            if(bindingSetLinee.getValue("id")!=null) {
              String idLinee = bindingSetLinee.getValue("id").stringValue();
              valueOfLinee = valueOfLinee + " - " + idLinee;
            }
          }
        }

        if (!valueOfLinee.equals("")) {
          valueOfLinee = JSONObject.escape(valueOfLinee.substring(3));
        }

        if (s != 0) {
          out.println(", ");
        }

        out.print(" {\n"
                + " \"geometry\": {\n"
                + "  \"type\": \"Point\",\n"
                + "  \"coordinates\": [" + valueOfBSLong + ", " + valueOfBSLat + "]\n"
                + " },\n"
                + " \"type\": \"Feature\",\n"
                + " \"properties\": {\n"
                + "  \"name\": \"" + valueOfNomeFermata + "\",\n"
                //+ "  \"nome\": \"" + valueOfNomeFermata + "\",\n"
                + "  \"typeLabel\": \"" + type + "\",\n"
                + "  \"tipo\": \"fermata\",\n"
                + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\",\n"
                + "    \"busLines\": \"" + valueOfLinee + "\",\n"
                + "    \"serviceUri\": \"" + valueOfBS + "\",\n"
                + "    \"agency\": \"" + valueOfAgName + "\",\n"
                + "    \"agencyUri\": \"" + valueOfAgUri + "\",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfBS,"thumbs") + "\n"
                + " },\n"
                + " \"id\": " + Integer.toString(s + 1) + "\n"
                + "}");
        s++;
        numeroBus++;
      }
      out.println("]}");
    }

    int numeroSensori = 0;
    if (searchSensorSite && !inside) {
      String type;
      if (lang.equals("it")) {
        type = "Sensore";
      } else {
        type = "Sensor";
      }

      SparqlQuery queryNearSensors = new SparqlQuery() {
        @Override
        public String query(String type) throws Exception {
          return "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                  + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                  + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                  + "PREFIX schema:<http://schema.org/>\n"
                  + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
                  + ("count".equals(type) ? "SELECT (COUNT(*) AS ?count) WHERE {\n" : "")
                  + "SELECT DISTINCT ?sensor ?idSensore  ?lat ?long ?address ?x WHERE {\n"
                  + " ?sensor rdf:type km4c:SensorSite.\n"
                  + " ?sensor geo:lat ?lat.\n"
                  + " ?sensor geo:long ?long.\n"
                  + " ?sensor dcterms:identifier ?idSensore.\n"
                  + ServiceMap.textSearchQueryFragment("?sensor", "?p", textToSearch)
                  + ServiceMap.geoSearchQueryFragment("?sensor", coords, raggioBus)
                  + " ?sensor schema:streetAddress ?address.\n"
                  + "} "
                  + (type != null ? "}" : "ORDER BY ?dist");
        }
      };

      String queryStringNearSensori = queryNearSensors.query(null);
      int fullCount = -1;
      if (!risultatiSensori.equals("0")) {
        //resSensori = ((Integer.parseInt(risultatiServizi))/10*2);
        //queryStringNearSensori += " LIMIT " + resSensori;
        //fullCount = ServiceMap.countQuery(con, queryNearSensors.query("count"));
        if (cat_servizi.equals("categorie")) {
          resSensori = ((Integer.parseInt(risultatiServizi)) / 10 * 2);
          queryStringNearSensori += " LIMIT " + resSensori;
          fullCount = ServiceMap.countQuery(con, queryNearSensors.query("count"));
        } else {
          if (cat_servizi.contains(":")) {
            String parts[] = cat_servizi.split(":");
            if (!parts[1].equals("0")) {
              resSensori = ((Integer.parseInt(parts[1])) / 10 * 2);
              queryStringNearSensori += " LIMIT " + resSensori;
              fullCount = ServiceMap.countQuery(con, queryNearSensors.query("count"));
            }
          } else {
            queryStringNearSensori += " LIMIT " + risultatiServizi;
            fullCount = ServiceMap.countQuery(con, queryNearSensors.query("count"));
          }
        }
      } else {
        fullCount = ServiceMap.countQuery(con, queryNearSensors.query("count"));
      }

      TupleQuery tupleQuerySensori = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringNearSensori);
      long ts = System.nanoTime();
      TupleQueryResult resultNearSensori = tupleQuerySensori.evaluate();
      logQuery(queryStringNearSensori, "API-nearsensori", sparqlType, args, System.nanoTime() - ts);

      if (!searchBusStop) {
        out.println("\"SensorSites\": {");
      } else {
        out.println(", \"SensorSites\": {");
      }
      if (fullCount >= 0) {
        out.println(" \"fullCount\": " + fullCount + ",");
      }
      out.println(" \"type\": \"FeatureCollection\",\n"
              + " \"features\": [");

      while (resultNearSensori.hasNext()) {
        BindingSet bindingSetNearSensori = resultNearSensori.next();
        String valueOfId = bindingSetNearSensori.getValue("idSensore").stringValue();
        String valueOfIdService = bindingSetNearSensori.getValue("sensor").stringValue();
        String valueOfLat = bindingSetNearSensori.getValue("lat").stringValue();
        String valueOfLong = bindingSetNearSensori.getValue("long").stringValue();

        if (i != 0) {
          out.println(",");
        }
        out.print(" {\n"
                + "  \"geometry\": {\n"
                + "    \"type\": \"Point\",\n"
                + "    \"coordinates\":[" + valueOfLong + "," + valueOfLat + "]\n"
                + "  },\n"
                + "  \"type\": \"Feature\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": \"" + valueOfId + "\",\n"
                //+ "    \"nome\": \"" + valueOfId + "\",\n"
                + "    \"tipo\": \"sensore\",\n"
                + "    \"typeLabel\": \"" + type + "\",\n"
                + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\",\n"
                + "    \"serviceUri\": \"" + valueOfIdService + "\",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfIdService,"thumbs") + "\n"
                + "  },\n"
                + "  \"id\": " + Integer.toString(i + 1) + "\n"
                + " }");

        i++;
        numeroSensori++;
      }
      out.println("]}");
    }

    int numeroServizi = 0;
    //if (!categorie.equals("BusStop") && !categorie.equals("SensorSite") && !categorie.equals("SensorSite;BusStop") && !categorie.equals("BusStop;SensorSite")) {
    if (searchServices) {

      /*String filtroDL_tmp = "";
      if (cat_servizi.equals("categorie_t")) {
        if (categorie.contains("Fresh_place")) {
          filtroDL_tmp = " OPTIONAL {?ser a ?sTypeDL. FILTER(?sTypeDL=km4c:DigitalLocation)}";
        } else {
          filtroDL_tmp = " ?ser a ?sTypeDL. FILTER(?sTypeDL=km4c:DigitalLocation)";
        }
      }*/
      final String filtroDL = ""; //filtroDL_tmp; //ELIMINATO virtuoso genera un errore "VECSL Error VECSL: Geo chekc ro id is to be a vec ssl.  For support." non si capisce a cosa serva

      SparqlQuery queryNearServices = new SparqlQuery() {
        @Override
        public String query(String type) throws Exception {
          return "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                  + "PREFIX schema:<http://schema.org/>\n"
                  + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                  + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                  + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                  + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
                  + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
                  + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
                  + "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n"
                  + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
                  + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n"
                  + ("count".equals(type) ? "SELECT (COUNT(*) AS ?count) WHERE {\n" : "")
                  + "SELECT DISTINCT ?ser ?serAddress ?elat ?elong ?sType ?sCategory ?sTypeIta (IF(?sName1,?sName1,?sName2) as ?sName) ?email ?note ?multimedia ?description (!STRSTARTS(?wktGeometry,\"POINT\") as ?hasGeometry) ?geo ?ag ?agname ?x WHERE {\n"
                  + " ?ser rdf:type km4c:Service" + (sparqlType.equals("virtuoso") ? " OPTION (inference \"urn:ontology\")" : "") + ".\n"
                  + " OPTIONAL {?ser <http://schema.org/name> ?sName1}\n"
                  + " OPTIONAL {?ser foaf:name ?sName2}\n"
                  + ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch)
                  + (inside ? 
                    "?ser opengis:hasGeometry [geo:geometry ?geo].\n"
                    + "  ?ser geo:lat ?elat.\n"
                    + "  ?ser geo:long ?elong.\n"
                    + "filter(bif:st_contains(?geo,bif:st_point("+coords[1]+","+coords[0]+"),0.0000001))"
                  : " {\n"
                    + "  ?ser km4c:hasAccess ?entry.\n"
                    + "  ?entry geo:lat ?elat.\n"
                    + "  ?entry geo:long ?elong.\n"
                    + ServiceMap.geoSearchQueryFragment("?entry", coords, raggioServizi)
                    + " } UNION {\n"
                    + "  ?ser geo:lat ?elat.\n"
                    + "  ?ser geo:long ?elong.\n"
                    + ServiceMap.geoSearchQueryFragment("?ser", coords, raggioServizi)
                    + " }\n")
                  + fc
                  + (!km4cVersion.equals("old")
                          ? " graph ?g {?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:BusStop && ?sType!=km4c:SensorSite)}\n"
                          + filtroDL
                          + " ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory != <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>) "
                          + " ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"" + lang + "\")\n" : "")
                  + (getGeometry || inside ? " OPTIONAL {?ser opengis:hasGeometry [opengis:asWKT ?wktGeometry].}\n" : "")
                  + "   OPTIONAL {?ser km4c:multimediaResource ?multimedia}.\n"
                  + "   OPTIONAL {?st gtfs:stop ?ser.\n"
                  + "     ?st gtfs:trip/gtfs:route/gtfs:agency ?ag.\n"
                  + "     ?ag foaf:name ?agname.}\n"
                  + "}"
                  + (type != null ? "}" : " ORDER BY ?dist");
        }
      };

      String queryStringServiceNear = queryNearServices.query(null);
      System.out.println(queryStringServiceNear);
      int fullCount = -1;
      if (!risultatiServizi.equals("0")) {
        //resServizi = ((Integer.parseInt(risultatiServizi))-(numeroBus + numeroSensori));
        //queryStringServiceNear += " LIMIT " + resServizi;
        //fullCount = ServiceMap.countQuery(con, queryNearServices.query("count"));
        if (!cat_servizi.equals("categorie_t")) {
          resServizi = ((Integer.parseInt(risultatiServizi)) - (numeroBus + numeroSensori));
          queryStringServiceNear += " LIMIT " + resServizi;
          fullCount = ServiceMap.countQuery(con, queryNearServices.query("count"));
        } else {

          queryStringServiceNear += " LIMIT " + risultatiServizi;
          fullCount = ServiceMap.countQuery(con, queryNearServices.query("count"));
        }
      } else {
        fullCount = ServiceMap.countQuery(con, queryNearServices.query("count"));
      }
      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringServiceNear);
      long ts = System.nanoTime();
      TupleQueryResult result = tupleQuery.evaluate();
      logQuery(queryStringServiceNear, "API-nearservices", sparqlType, args, System.nanoTime() - ts);
      if (!searchBusStop && !searchSensorSite || inside) {
        out.println("\"Services\":{");
      } else {
        out.println(", \"Services\":{");
      }
      if (fullCount >= 0) {
        out.println(" \"fullCount\": " + fullCount + ",");
      }
      out.println(" \"type\": \"FeatureCollection\",\n"
              + " \"features\": [");
      int w = 0;
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String valueOfSer = bindingSet.getValue("ser").stringValue();

        String valueOfSType = "";
        if (bindingSet.getValue("sType") != null) {
          valueOfSType = bindingSet.getValue("sType").stringValue();
        }
        String valueOfSTypeIta = "";
        if (bindingSet.getValue("sTypeIta") != null) {
          valueOfSTypeIta = bindingSet.getValue("sTypeIta").stringValue();
        }

        String valueOfTipo = "";
        valueOfTipo = valueOfSTypeIta;
        valueOfTipo = valueOfTipo.replace(" ", "_");
        valueOfTipo = valueOfTipo.replaceAll("[^\\P{Punct}_]+", "");

        // DICHIARAZIONE VARIABILI serviceType e serviceCategory per ICONA
        String subCategory = "";
        if (bindingSet.getValue("sType") != null) {
          subCategory = bindingSet.getValue("sType").stringValue();
          subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
        }

        String category = "";
        if (bindingSet.getValue("sCategory") != null) {
          category = bindingSet.getValue("sCategory").stringValue();
          category = category.replace("http://www.disit.org/km4city/schema#", "");
        }

        String serviceType = category + "_" + subCategory;

        String valueOfSName = "";
        if (bindingSet.getValue("sName") != null) {
          valueOfSName = bindingSet.getValue("sName").stringValue();
        } else {
          valueOfSName = subCategory.replace("_", " ").toUpperCase();
        }
        String valueOfELat = bindingSet.getValue("elat").stringValue();
        String valueOfELong = bindingSet.getValue("elong").stringValue();      
        String valueOfHasGeometry = "false";
        if(bindingSet.getValue("hasGeometry")!=null)
          valueOfHasGeometry = "1".equals(bindingSet.getValue("hasGeometry").stringValue()) ? "true" : "false";
        String valueOfMultimedia = "";
        if(bindingSet.getValue("multimedia")!=null)
          valueOfMultimedia = bindingSet.getValue("multimedia").stringValue();
        String valueOfAgUri = null, valueOfAgName = null;
        if(bindingSet.getValue("ag")!=null)
          valueOfAgUri = bindingSet.getValue("ag").stringValue();
        if(bindingSet.getValue("agname")!=null)
          valueOfAgName = bindingSet.getValue("agname").stringValue();

        if(inside && bindingSet.getValue("geo")!=null) {
          //per bug di virtuoso controlla se geometria effettivamente contiene il punto richiesto
          WKTReader wktReader=new WKTReader();
          Geometry position=wktReader.read("POINT("+coords[1]+" "+coords[0]+")").buffer(0.0001);
          try {
            Geometry g=wktReader.read(bindingSet.getValue("geo").stringValue());
            if(!g.intersects(position)) {
              System.out.println("SKIP! "+valueOfSName);
              continue;
            } else {
              System.out.println("OK "+valueOfSName);
            }
          }catch(Exception e) {
            e.printStackTrace();
          }
        }
        
        if (w != 0) {
          out.println(",");
        }

        out.print(" {\n"
                + "  \"geometry\": {\n"
                + "   \"type\": \"Point\",\n"
                + "   \"coordinates\": [" + valueOfELong + "," + valueOfELat + "]\n"
                + "  },\n"
                + "  \"type\": \"Feature\",\n"
                + "  \"properties\": {\n"
                + "   \"name\": \"" + escapeJSON(valueOfSName) + "\",\n"
                //+ "   \"nome\": \"" + escapeJSON(valueOfSName) + "\",\n"
                + "   \"tipo\": \"" + escapeJSON(valueOfTipo) + "\",\n"
                + "   \"typeLabel\": \"" + escapeJSON(valueOfSTypeIta) + "\",\n"
                + "   \"serviceType\": \"" + escapeJSON(serviceType) + "\",\n"
                + (getGeometry ? "   \"hasGeometry\": " + escapeJSON(valueOfHasGeometry) + ",\n" : "")
                + "   \"serviceUri\": \"" + escapeJSON(valueOfSer) + "\",\n"
                + (photos ? "   \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfSer,"thumbs") + ",\n" : "")
                + (valueOfAgName!=null ? "   \"agency\": \"" + valueOfAgName + "\",\n" : "")
                + (valueOfAgUri!=null ? "   \"agencyUri\": \"" + valueOfAgUri + "\",\n" : "")
                + "   \"multimedia\": \"" + valueOfMultimedia + "\"\n"
                + "  },\n"
                + "  \"id\": " + Integer.toString(w + 1) + "\n"
                + " }");
        w++;
        numeroServizi++;
      }
      out.println("]}");
    } 
    out.println("}");
  }
  
  public void queryFulltext(JspWriter out, RepositoryConnection con, final String textToSearch, String selection, final String dist, String limit, final String lang, final boolean getGeometry) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    final String km4cVersion = conf.get("km4cVersion", "new");
    String[] _coords = null;
    String type;
    if (lang.equals("it")) {
      type = "Servizio";
    } else {
      type = "Service";
    }
    String _freeText = "";

    if (selection != null && selection.contains(";")) {
      _coords = selection.split(";");
    } else {
      _freeText = "} UNION {\n"
              + "  ?ser km4c:hasStreetNumber ?sn.\n"
              + "  ?sn km4c:hasExternalAccess ?entry.\n"
              + "  ?sn km4c:number ?civic.\n"
              + "  ?entry geo:lat ?lat.\n"
              + "  ?entry geo:long ?long.\n";
    }
    final String[] coords = _coords;
    final String freeText = _freeText;

    SparqlQuery query = new SparqlQuery() {
      @Override
      public String query(String type) throws Exception {
        return "PREFIX luc: <http://www.ontotext.com/owlim/lucene#>\n"
                + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
                + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX schema:<http://schema.org/>\n"
                + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
                + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
                + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n"
                + ("count".equals(type) ? "SELECT (COUNT(*) as ?count) WHERE {\n" : "")
                + "SELECT DISTINCT ?ser ?long ?lat ?sType ?sTypeIta ?sCategory ?sName ?txt ?civic (!STRSTARTS(?wktGeometry,\"POINT\") as ?hasGeometry) ?multimedia ?ag ?agname ?x WHERE {\n"
                + ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch)
                + (km4cVersion.equals("old")
                        ? "  OPTIONAL { ?ser km4c:hasServiceCategory ?cat.\n"
                        + "  ?cat rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"" + lang + "\") }\n"
                        : " {\n"
                        + "  ?ser a ?sType. FILTER(?sType!=km4c:Event && ?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService)\n"
                        + "  OPTIONAL { ?sType rdfs:subClassOf ?sCategory. FILTER(STRSTARTS(STR(?sCategory),\"http://www.disit.org/km4city/schema#\"))}\n"
                        + "  ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"" + lang + "\")\n"
                        + " }\n")
                + " OPTIONAL {{?ser rdfs:label ?sName.}UNION{?ser schema:name ?sName.}UNION{?ser foaf:name ?sName.}UNION{?ser km4c:extendName ?sName.}}\n"
                + " {\n"
                + "  ?ser geo:lat ?lat .\n"
                + "  ?ser geo:long ?long .\n"
                + ServiceMap.geoSearchQueryFragment("?ser", coords, dist)
                + " } UNION {\n"
                + "  ?ser km4c:hasAccess ?entry.\n"
                + "	 ?entry geo:lat ?lat.\n"
                + "	 ?entry geo:long ?long.\n"
                + ServiceMap.geoSearchQueryFragment("?entry", coords, dist)
                + freeText
                /*+ "} UNION {"
                 + " ?ser km4c:hasStreetNumber/km4c:hasExternalAccess ?entry."
                 + " ?entry geo:lat ?lat."
                 + " ?entry geo:long ?long." 
                 + ServiceMap.geoSearchQueryFragment("?entry", coords, dist)*/
                + " }\n"
                + (getGeometry ? " OPTIONAL {?ser opengis:hasGeometry [opengis:asWKT ?wktGeometry].}\n" : "")
                + " OPTIONAL {?ser km4c:multimediaResource ?multimedia}.\n"
                + " OPTIONAL {\n"
                + "  {?st gtfs:stop ?ser.}UNION{?st gtfs:stop [owl:sameAs ?ser]}\n" 
                + "  ?st gtfs:trip ?t.\n"
                +"   ?t gtfs:route/gtfs:agency ?ag.\n"
                +"   ?ag foaf:name ?agname.\n"
                + " }\n"
                + "} "
                + (type != null ? "}" : "ORDER BY DESC(?sc)");
      }
    };
    int fullCount = -1;
    String queryText = query.query(null);
    System.out.println(queryText);
    if (!"0".equals(limit)) {
      queryText += " LIMIT " + limit;
      fullCount = ServiceMap.countQuery(con, query.query("count"));
    } else {
      fullCount = ServiceMap.countQuery(con, query.query("count"));
    }
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
    long start = System.nanoTime();
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(queryText, "API-free-text-search", sparqlType, textToSearch + ";" + limit, System.nanoTime() - start);
    int i = 0;
    out.println("{");
    if (fullCount >= 0) {
      out.println(" \"fullCount\": " + fullCount + ",");
    }
    out.println(" \"type\": \"FeatureCollection\",\n"
            + " \"features\": [");
    while (result.hasNext()) {
      if (i != 0) {
        out.print(", ");
      }
      BindingSet bindingSet = result.next();

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
        //label = label.replace(" ", "_");
        //label = label.replace("'", "");
      }
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

      String sName = "";
      if (bindingSet.getValue("sName") != null) {
        sName = bindingSet.getValue("sName").stringValue();
      } else {
        sName = subCategory.replace("_", " ").toUpperCase();
      }

      // Controllo categoria SensorSite e BusStop per ricerca testuale.
      String serviceType = "";
      String valueOfLinee = "";

      serviceType = category + "_" + subCategory;
      if ("Road".equals(subCategory)) {
        serviceType = "";
      }
      if (subCategory.equals("BusStop")) {
        TupleQueryResult resultLinee = queryBusLines(serviceUri, con);
        if (resultLinee != null) {
          while (resultLinee.hasNext()) {
            BindingSet bindingSetLinee = resultLinee.next();
            if(bindingSetLinee.getValue("id")!=null) {
              String idLinee = bindingSetLinee.getValue("id").stringValue();
              valueOfLinee = valueOfLinee + " - " + idLinee;
            }
          }
          if (valueOfLinee.length() > 3) {
            valueOfLinee = valueOfLinee.substring(3);
          }
        }        
      }

      String number = "";
      if (bindingSet.getValue("civic") != null) {
        number = bindingSet.getValue("civic").stringValue();
      }

      String txt = "";
      if (bindingSet.getValue("txt") != null) {
        txt = bindingSet.getValue("txt").stringValue();
      }

      String hasGeometry = "false";
      if (bindingSet.getValue("hasGeometry") != null) {
        hasGeometry = "1".equals(bindingSet.getValue("hasGeometry").stringValue()) ? "true" : "false";
      }

      String multimedia = "";
      if (bindingSet.getValue("multimedia") != null) {
        multimedia = bindingSet.getValue("multimedia").stringValue();
      }
      
      String agencyUri = null, agency = null;
      if(bindingSet.getValue("ag") != null) {
        agencyUri = bindingSet.getValue("ag").stringValue();
        agency = bindingSet.getValue("agname").stringValue();
      }
      
      out.println(" {\n"
              + "  \"geometry\":{\n"
              + "   \"type\": \"Point\",\n"
              + "   \"coordinates\": [" + serviceLong + "," + serviceLat + "]\n"
              + "  },\n"
              + "  \"type\": \"Feature\",\n"
              + "  \"properties\": {\n"
              + "  \"serviceUri\": \"" + serviceUri + "\",\n"
              + "  \"name\": \"" + ServiceMap.escapeJSON(sName) + "\",\n"
              + "  \"tipo\": \"servizio\",\n"
              + "  \"photoThumbs\": " + ServiceMap.getServicePhotos(serviceUri,"thumbs") + ",\n"
              + "  \"multimedia\": \"" + multimedia + "\",\n"
              + "  \"civic\": \"" + escapeJSON(number) + "\"");
      out.println("  ,\"serviceType\": \"" + escapeJSON(serviceType) + "\"");
      if (!"".equals(label)) {
        out.println("  ,\"typeLabel\": \"" + ServiceMap.escapeJSON(label) + "\"");
      } else {
        out.println("  ,\"typeLabel\": \"" + type + "\",");
      }
      if (getGeometry) {
        out.println("  ,\"hasGeometry\": " + escapeJSON(hasGeometry) + "");
      }
      if ("BusStop".equals(subCategory)) {
        out.println("  ,\"busLines\": \"" + escapeJSON(valueOfLinee) + "\"");
      }
      if (agencyUri!=null) {
        out.println("  ,\"agency\": \"" + escapeJSON(agency) + "\"");
        out.println("  ,\"agencyUri\": \"" + agencyUri + "\"");
      }      
      out.println("  },\n"
              + " \"id\": " + Integer.toString(i + 1) + "\n"
              + " }");
      i++;
    }
    out.println("]}");
  }

  public void queryMunicipalityServices(JspWriter out, RepositoryConnection con, String selection, String categorie, String textToSearch, String risultatiBus, String risultatiSensori, String risultatiServizi, String lang, final boolean getGeometry) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    List<String> listaCategorie = new ArrayList<>();
    if (categorie != null && !"".equals(categorie)) {
      String[] arrayCategorie = categorie.split(";");
      // GESTIONE CATEGORIE
      listaCategorie = Arrays.asList(arrayCategorie);
    }
    String nomeComune = selection.substring(selection.indexOf("COMUNE di") + 10);
    String filtroLocalita = "";
    filtroLocalita += "{ ?ser km4c:hasAccess ?entry . ";
    filtroLocalita += "	?entry geo:lat ?elat . ";
    filtroLocalita += "	 FILTER (?elat>40) ";
    filtroLocalita += "	 ?entry geo:long ?elong . ";
    filtroLocalita += "	 FILTER (?elong>10) ";
    filtroLocalita += "   ?nc km4c:hasExternalAccess ?entry . ";
    filtroLocalita += "  ?nc km4c:belongToRoad ?road . ";
    filtroLocalita += "  ?road km4c:inMunicipalityOf ?mun . ";
    filtroLocalita += "?mun foaf:name \"" + nomeComune + "\"^^xsd:string . }";
    filtroLocalita += "UNION";
    filtroLocalita += "{";
    filtroLocalita += "?ser km4c:isInRoad ?road . ";
    filtroLocalita += "	?ser geo:lat ?elat . ";
    filtroLocalita += "	 FILTER (?elat>40) ";
    filtroLocalita += "	 ?ser geo:long ?elong . ";
    filtroLocalita += "	 FILTER (?elong>10) ";
    filtroLocalita += "?road km4c:inMunicipalityOf ?mun . ";
    filtroLocalita += "?mun foaf:name \"" + nomeComune + "\"^^xsd:string . ";
    filtroLocalita += "}";

    String fc = "";
    try {
      fc = ServiceMap.filterServices(listaCategorie);
    } catch (Exception e) {
      e.printStackTrace();
    }
    int b = 0;
    int numeroBus = 0;
    if (listaCategorie.contains("BusStop")) {
      String type = "";
      if (type.equals("it")) {
        type = "Fermata";
      } else {
        type = "BusStop";
      }
      String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "SELECT DISTINCT ?bs ?nomeFermata ?bslat ?bslong ?address ?x WHERE {\n"
              + " ?bs rdf:type km4c:BusStop.\n"
              + " ?bs foaf:name ?nomeFermata.\n"
              + ServiceMap.textSearchQueryFragment("?bs", "foaf:name", textToSearch)
              + " ?bs geo:lat ?bslat.\n"
              + " ?bs geo:long ?bslong.\n"
              + " ?bs km4c:isInMunicipality ?com.\n"
              + " ?com foaf:name \"" + nomeComune + "\"^^xsd:string.\n"
              + "}";
      if (!risultatiBus.equals("0")) {
        queryString += " LIMIT " + risultatiBus;
      }
      TupleQuery tupleQueryBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString, km4cVersion));
      long ts = System.nanoTime();
      TupleQueryResult resultBS = tupleQueryBusStop.evaluate();
      ServiceMap.logQuery(queryString, "API-fermate", sparqlType, nomeComune + ";" + textToSearch, System.nanoTime() - ts);

      out.println("{\"BusStops\": ");
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      while (resultBS.hasNext()) {
        BindingSet bindingSetBS = resultBS.next();
        String valueOfBS = bindingSetBS.getValue("bs").stringValue();
        String valueOfNomeFermata = bindingSetBS.getValue("nomeFermata").stringValue();
        String valueOfBSLat = bindingSetBS.getValue("bslat").stringValue();
        String valueOfBSLong = bindingSetBS.getValue("bslong").stringValue();
        if (b != 0) {
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
                + "    \"name\": \"" + escapeJSON(valueOfNomeFermata) + "\", "
                + "    \"typeLabel\": \"" + escapeJSON(type) + "\", "
                + "    \"serviceUri\": \"" + valueOfBS + "\", "
                + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\", "
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfBS,"thumbs") + ",\n"
                + "}, "
                + "\"id\": " + Integer.toString(b + 1) + "  "
                + "}");
        b++;
        numeroBus++;
      }
      out.println("]}");
      if (categorie.equals("BusStop")) {
        out.println("}");
      }
    }
    int numeroSensori = 0;
    if (listaCategorie.contains("SensorSite")) {
      String type = "";
      if (type.equals("it")) {
        type = "Sensore";
      } else {
        type = "Sensor";
      }
      String queryStringSensori = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
              + "PREFIX schema:<http://schema.org/#>"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX dct:<http://purl.org/dc/terms/#>"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#> "
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
              + "select distinct ?sensor ?idSensore  ?lat ?long ?address ?x where{"
              + "?sensor rdf:type km4c:SensorSite ."
              + ServiceMap.textSearchQueryFragment("?sensor", "?p", textToSearch)
              + "?sensor geo:lat ?lat ."
              //+ " FILTER regex(str(?lat), \"^4\") ."
              + "?sensor geo:long ?long ."
              + "?sensor <http://purl.org/dc/terms/identifier> ?idSensore ."
              + "?sensor km4c:placedOnRoad ?road ."
              + "?road km4c:inMunicipalityOf ?mun ."
              + "?mun foaf:name \"" + nomeComune + "\"^^xsd:string ."
              + "}";
      if (!risultatiSensori.equals("0")) {

        queryStringSensori += " LIMIT " + risultatiSensori;
      }
      TupleQuery tupleQuerySensori = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringSensori, km4cVersion));
      long ts = System.nanoTime();
      TupleQueryResult resultSensori = tupleQuerySensori.evaluate();
      ServiceMap.logQuery(queryStringSensori, "API-sensori", sparqlType, nomeComune + ";" + textToSearch, System.nanoTime() - ts);
      if (!listaCategorie.contains("BusStop")) {
        out.println("{\"SensorSites\": ");
      } else {
        out.println(",\"SensorSites\":");
      }
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      int s = 0;
      while (resultSensori.hasNext()) {
        BindingSet bindingSetSensori = resultSensori.next();
        String valueOfId = bindingSetSensori.getValue("idSensore").stringValue();
        String valueOfIdService = bindingSetSensori.getValue("sensor").stringValue();
        String valueOfLat = bindingSetSensori.getValue("lat").stringValue();

        String valueOfLong = bindingSetSensori.getValue("long").stringValue();

        if (s != 0) {
          out.println(", ");
        }

        out.println("{ "
                + " \"geometry\": {  "
                + "     \"type\": \"Point\",  "
                + "    \"coordinates\": [  "
                + "       " + valueOfLong + ",  "
                + "      " + valueOfLat + "  "
                + " ]  "
                + "},  "
                + "\"type\": \"Feature\",  "
                + "\"properties\": {  "
                + "    \"name\": \"" + valueOfId + "\", "
                + "    \"typeLabel\": \"" + type + "\", "
                + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\", "
                + "    \"serviceUri\": \"" + escapeJSON(valueOfIdService) + "\" "
                + "},  "
                + "\"id\": " + Integer.toString(s + 1) + "  "
                + "}");

        s++;
        numeroSensori++;

      }
      out.println("]}");
      if (categorie.equals("SensorSite") || categorie.equals("SensorSite;BusStop")) {
        out.println("}");
      }
    }
    int numeroServizi = 0;
    if (!categorie.equals("BusStop") && !categorie.equals("SensorSite") && !categorie.equals("SensorSite;BusStop") && !categorie.equals("BusStop;SensorSite")) {
      String type = "";
      if (type.equals("it")) {
        type = "Servizio";
      } else {
        type = "Service";
      }
      String queryStringServices
              = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
              + "SELECT distinct ?ser ?serAddress ?serNumber ?elat ?elong ?sName ?sType ?email ?note ?labelIta ?multimedia ?description ?identifier ?sCategory (!STRSTARTS(?wktGeometry,\"POINT\") as ?hasGeometry) WHERE {\n"
              + " ?ser rdf:type km4c:Service" + (sparqlType.equals("virtuoso") ? " OPTION (inference \"urn:ontology\")" : "") + ".\n"
              + " OPTIONAL{?ser schema:name ?sName. }\n"
              //+ " ?ser schema:streetAddress ?serAddress.\n"
              //+ " OPTIONAL {?ser km4c:houseNumber ?serNumber}.\n"
              //+ " OPTIONAL {?ser dc:description ?description FILTER(LANG(?description) = \""+lang+"\")}\n"
              //+ " OPTIONAL {?ser km4c:multimediaResource ?multimedia }\n"
              + " OPTIONAL { ?ser dcterms:identifier ?identifier }\n"
              //+ " OPTIONAL {?ser skos:note ?note }\n"
              //+ " OPTIONAL {?ser schema:email ?email }\n"
              + ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch)
              + filtroLocalita
              + fc
              + (!km4cVersion.equals("old")
                      ? " graph ?g {?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:SensorSite && ?sType!=km4c:BusStop)}\n"
                      + " ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory != <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>) "
                      + " ?sType rdfs:label ?labelIta. FILTER(LANG(?labelIta)=\"" + lang + "\")\n" : "")
              + (getGeometry ? " OPTIONAL {?ser opengis:hasGeometry [opengis:asWKT ?wktGeometry].}\n" : "")
              + "}";
      if (!risultatiServizi.equals("0")) {
        queryStringServices += " LIMIT " + risultatiServizi;
      }
      TupleQuery tupleQueryServices = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringServices, km4cVersion));
      long ts = System.nanoTime();
      TupleQueryResult resultServices = tupleQueryServices.evaluate();
      ServiceMap.logQuery(queryStringServices, "API-servizi", sparqlType, nomeComune + ";" + textToSearch + ";" + categorie, System.nanoTime() - ts);

      if (!listaCategorie.contains("BusStop") && !listaCategorie.contains("SensorSite")) {
        out.println("{\"Services\": ");
      } else {
        out.println(", \"Services\": ");
      }
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");

      int t = 0;
      while (resultServices.hasNext()) {
        BindingSet bindingSetServices = resultServices.next();
        String valueOfSer = bindingSetServices.getValue("ser").stringValue();
        String valueOfSName = "";
        if (bindingSetServices.getValue("sName") != null) {
          valueOfSName = bindingSetServices.getValue("sName").stringValue();
        }
        /*String valueOfSerAddress = bindingSetServices.getValue("serAddress").stringValue();
         String valueOfSerNumber = "";
         if (bindingSetServices.getValue("serNumber") != null) {
         valueOfSerNumber = bindingSetServices.getValue("serNumber").stringValue();
         }*/
        String valueOfSType = bindingSetServices.getValue("sType").stringValue();
        String valueOfSTypeIta = "";
        if (bindingSetServices.getValue("labelIta") != null) {
          valueOfSTypeIta = bindingSetServices.getValue("labelIta").stringValue();
        }
        String valueOfELat = bindingSetServices.getValue("elat").stringValue();
        String valueOfELong = bindingSetServices.getValue("elong").stringValue();
        /*String valueOfNote = "";
         if (bindingSetServices.getValue("note") != null) {
         valueOfNote = bindingSetServices.getValue("note").stringValue();
         }

         String valueOfEmail = "";
         if (bindingSetServices.getValue("email") != null) {
         valueOfEmail = bindingSetServices.getValue("email").stringValue();
         }*/

        String subCategory = "";
        if (bindingSetServices.getValue("sType") != null) {
          subCategory = bindingSetServices.getValue("sType").stringValue();
          subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
        }

        String category = "";
        if (bindingSetServices.getValue("sCategory") != null) {
          category = bindingSetServices.getValue("sCategory").stringValue();
          category = category.replace("http://www.disit.org/km4city/schema#", "");
        }

        String serviceType = category + "_" + subCategory;

        String hasGeometry = "false";
        if (bindingSetServices.getValue("hasGeometry") != null) {
          hasGeometry = "1".equals(bindingSetServices.getValue("hasGeometry").stringValue()) ? "true" : "false";
        }
        
        if (t != 0) {
          out.println(", ");
        }
        out.println("{ "
                + " \"geometry\": {  "
                + "     \"type\": \"Point\",  "
                + "    \"coordinates\": [  "
                + "       " + valueOfELong + ",  "
                + "      " + valueOfELat + "  "
                + " ]  "
                + "},  "
                + "\"type\": \"Feature\",  "
                + "\"properties\": {  "
                + "    \"name\": \"" + escapeJSON(valueOfSName) + "\", "
                + "    \"typeLabel\": \"" + escapeJSON(valueOfSTypeIta) + "\", "
                + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\", "
                + (getGeometry ? "    \"hasGeometry\": " + hasGeometry + ", " : "")
                + "    \"serviceUri\": \"" + valueOfSer + "\" "
                + "}, "
                + "\"id\": " + Integer.toString(t + 1) + "  "
                + "}");
        t++;
        numeroServizi++;
      }
      out.println("]}}");
    }
  }

  public void queryMeteo(JspWriter out, RepositoryConnection con, String idService, String lang) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String queryForComune = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
            + "PREFIX dcterms:<http://purl.org/dc/terms/> "
            + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX schema:<http://schema.org/#> "
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#> "
            + "PREFIX time:<http://www.w3.org/2006/time#> "
            + "SELECT ?nomeComune WHERE {"
            + " {"
            + "  <" + idService + "> km4c:refersToMunicipality ?mun."
            + "  ?mun foaf:name ?nomeComune."
            + " }UNION{"
            + "  <" + idService + "> rdf:type km4c:Municipality;"
            + "   foaf:name ?nomeComune."
            + " }"
            + "}";
    TupleQuery tupleQueryComuneMeteo = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForComune);
    long ts = System.nanoTime();
    TupleQueryResult resultComuneMeteo = tupleQueryComuneMeteo.evaluate();
    logQuery(queryForComune, "API-meteo-comune", sparqlType, idService, System.nanoTime() - ts);
    String nomeComune = "";
    int i = 0;
    try {
      if (resultComuneMeteo.hasNext()) {
        BindingSet bindingSetComuneMeteo = resultComuneMeteo.next();
        nomeComune = bindingSetComuneMeteo.getValue("nomeComune").stringValue();
      } else {
        out.println("{\"ERROR\":\"Invalid service url\"}");
      }
    } catch (Exception e) {
      out.println(e.getMessage());
    }
    //System.out.println("comune: " + nomeComune);
    String queryStringMeteo1 = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>"
            + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
            + "PREFIX schema:<http://schema.org/#>"
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>"
            + "PREFIX time:<http://www.w3.org/2006/time#> "
            + "SELECT distinct ?wRep ?instantDateTime WHERE{"
            + " ?comune rdf:type km4c:Municipality."
            + " ?comune foaf:name \"" + nomeComune + "\"^^xsd:string."
            + " ?comune km4c:hasWeatherReport ?wRep."
            + " ?wRep km4c:updateTime ?instant."
            + " ?instant <http://schema.org/value> ?instantDateTime."
            + "} ORDER BY DESC (?instantDateTime) LIMIT 1 ";
    TupleQuery tupleQuery1 = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringMeteo1, km4cVersion));
    ts = System.nanoTime();
    TupleQueryResult result1 = tupleQuery1.evaluate();
    logQuery(queryStringMeteo1, "API-meteo-1", sparqlType, nomeComune, System.nanoTime() - ts);

    if (result1.hasNext()) {
      BindingSet bindingSet1 = result1.next();
      String valueOfInstantDateTime = bindingSet1.getValue("instantDateTime").stringValue();
      String valueOfWRep = bindingSet1.getValue("wRep").stringValue();

      String wPred = "";
      String queryMeteo = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
              + "PREFIX dcterms:<http://purl.org/dc/terms/> "
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
              + "PREFIX schema:<http://schema.org/#> "
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#> "
              + "PREFIX time:<http://www.w3.org/2006/time#> "
              + "SELECT distinct ?giorno ?descrizione ?minTemp ?maxTemp ?instantDateTime ?wPred WHERE{"
              + " <" + valueOfWRep + "> km4c:hasPrediction ?wPred."
              + " ?wPred dcterms:description ?descrizione."
              + " ?wPred km4c:day ?giorno."
              + " ?wPred km4c:hour ?g. FILTER(STR(?g)=\"giorno\")\n"
              + " OPTIONAL { ?wPred km4c:minTemp ?minTemp.}"
              + " OPTIONAL { ?wPred km4c:maxTemp ?maxTemp.}"
              + "}";

      TupleQuery tupleQueryMeteo = con.prepareTupleQuery(QueryLanguage.SPARQL, queryMeteo); //tolto filterQuery per problema temporaneo da rimettere
      ts = System.nanoTime();
      TupleQueryResult resultMeteo = tupleQueryMeteo.evaluate();
      logQuery(queryMeteo, "API-meteo-2", sparqlType, nomeComune, System.nanoTime() - ts);
      out.println("{ \"head\": {"
              + "\"location\": "
              + "\"" + nomeComune + "\""
              + ","
              + "\"vars\":[ "
              + "\"day\", "
              + "\"description\","
              + "\"minTemp\","
              + "\"maxTemp\","
              + "\"instantDateTime\""
              + "]"
              + "},");
      out.println("\"results\": {");
      out.println("\"bindings\": [");
      try {
        i = 0;
        while (resultMeteo.hasNext()) {
          BindingSet bindingSetMeteo = resultMeteo.next();
          wPred = bindingSetMeteo.getValue("wPred").stringValue();
          String valueOfGiorno = bindingSetMeteo.getValue("giorno").stringValue();
          String valueOfDescrizione = bindingSetMeteo.getValue("descrizione").stringValue();
          String valueOfMinTemp = "";
          if (bindingSetMeteo.getValue("minTemp") != null) {
            valueOfMinTemp = bindingSetMeteo.getValue("minTemp").stringValue();
          }
          String valueOfMaxTemp = "";
          if (bindingSetMeteo.getValue("maxTemp") != null) {
            valueOfMaxTemp = bindingSetMeteo.getValue("maxTemp").stringValue();
          }
          if (i != 0) {
            out.println(", ");
          }

          out.println("{"
                  + "\"day\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfGiorno + "\" "
                  + " },"
                  + "\"description\": { "
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfDescrizione + "\" "
                  + " },"
                  + "\"minTemp\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfMinTemp + "\" "
                  + " },"
                  + "\"maxTemp\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfMaxTemp + "\" "
                  + " },"
                  + "\"instantDateTime\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + valueOfInstantDateTime + "\" "
                  + " }"
                  + "}");

          i++;
        }
        out.println("]}}");
      } catch (Exception e) {
        out.println(e.getMessage());
      }
    } else {
      out.println("{\"ERROR\":\"No forecast found\"}");
    }
  }

  public void querySensor(JspWriter out, RepositoryConnection con, String idService, String lang, String realtime, String uid) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String type = "";
    if (lang.equals("it")) {
      type = "Sensore";
    } else {
      type = "Sensor";
    }
    String querySensore = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX schema:<http://schema.org/>"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>"
            + "PREFIX dct:<http://purl.org/dc/terms/#>"
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#> "
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>"
            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
            + "select distinct ?idSensore ?lat ?long ?address ?nomeComune where{"
            + " <" + idService + "> rdf:type km4c:SensorSite;"
            + "  geo:lat ?lat;"
            + "  geo:long ?long;"
            + "  dcterms:identifier ?idSensore;"
            + "  schema:streetAddress ?address;"
            + "  km4c:placedOnRoad ?road."
            + " ?road km4c:inMunicipalityOf ?mun."
            + " ?mun foaf:name ?nomeComune."
            //+ " FILTER regex(str(?lat), \"^4\") ."
            + "}"
            + "LIMIT 1";
    String nomeSensore = "";
    TupleQuery tupleQuerySensor = con.prepareTupleQuery(QueryLanguage.SPARQL, querySensore);
    long ts = System.nanoTime();
    TupleQueryResult resultSensor = tupleQuerySensor.evaluate();
    logQuery(querySensore, "API-sensor-info", sparqlType, idService, System.nanoTime() - ts);
    out.println("{\"Sensor\": ");
    out.println("{ "
            + "\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    try {
      int s = 0;

      while (resultSensor.hasNext()) {
        BindingSet bindingSetSensor = resultSensor.next();
        String valueOfId = bindingSetSensor.getValue("idSensore").stringValue();
        String nomeComune = bindingSetSensor.getValue("nomeComune").stringValue();
        String valueOfLat = bindingSetSensor.getValue("lat").stringValue();
        String valueOfLong = bindingSetSensor.getValue("long").stringValue();
        String valueOfAddress = bindingSetSensor.getValue("address").stringValue();

        nomeSensore = valueOfId;
        if (s != 0) {
          out.println(", ");
        }
        float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);
        out.println("{ "
                + " \"geometry\": {  "
                + "     \"type\": \"Point\",  "
                + "    \"coordinates\": [  "
                + "       " + valueOfLong + ",  "
                + "      " + valueOfLat + "  "
                + " ]  "
                + "},  "
                + "\"type\": \"Feature\",  "
                + "\"properties\": {  "
                + "    \"name\": \"" + escapeJSON(valueOfId) + "\", "
                + "    \"typeLabel\": \"" + escapeJSON(type) + "\", "
                + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\", "
                + "    \"serviceUri\": \"" + idService + "\", "
                + "    \"municipality\": \"" + escapeJSON(nomeComune) + "\", "
                + "    \"address\": \"" + escapeJSON(valueOfAddress) + "\",\n"
                + "    \"photos\": " + ServiceMap.getServicePhotos(idService) + ",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(idService,"thumbs") + ",\n"
                + "    \"photoOrigs\": " + ServiceMap.getServicePhotos(idService,"originals") + ",\n"
                + "    \"avgStars\": " + avgServiceStars[0] + ",\n"
                + "    \"starsCount\": " + (int) avgServiceStars[1] + ",\n"
                + (uid != null ? "    \"userStars\": " + ServiceMap.getServiceStarsByUid(idService, uid) + ",\n" : "")
                + "    \"comments\": " + ServiceMap.getServiceComments(idService)
                + "},  "
                + "\"id\": " + Integer.toString(s + 1) + "  "
                + "}");
        s++;
      }
      out.println("]}");
    } catch (Exception e) {
      out.println(e.getMessage());
    }
    if ("true".equalsIgnoreCase(realtime)) {
      String querySensorData = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
              + "PREFIX schema:<http://schema.org/#>"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX dct:<http://purl.org/dc/terms/#>"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#> "
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
              + "select ?avgDistance ?avgTime ?occupancy ?concentration ?vehicleFlow ?averageSpeed ?thresholdPerc ?speedPercentile ?timeInstant where{"
              + " <" + idService + "> rdf:type km4c:SensorSite;"
              + "  dcterms:identifier \"" + nomeSensore + "\"^^xsd:string;"
              + "  km4c:hasObservation ?obs."
              + " ?obs dcterms:date ?timeInstant."
              + " optional {?obs km4c:averageDistance ?avgDistance}."
              + " optional {?obs km4c:averageTime ?avgTime}."
              + " optional {?obs km4c:occupancy ?occupancy}."
              + " optional {?obs km4c:concentration ?concentration}."
              + " optional {?obs km4c:vehicleFlow ?vehicleFlow}."
              + " optional {?obs km4c:averageSpeed ?averageSpeed}."
              + " optional {?obs km4c:thresholdPerc ?thresholdPerc}."
              + " optional {?obs km4c:speedPrecentile ?speedPercentile}."
              + "} ORDER BY DESC (?timeInstant)"
              + "LIMIT 1";
      TupleQuery tupleQuerySensorData = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(querySensorData, km4cVersion));
      TupleQueryResult resultSensorData = tupleQuerySensorData.evaluate();
      logQuery(querySensorData, "API-sensor-rt-info", sparqlType, idService, System.nanoTime() - ts);

      String valueOfInstantDateTime = "";
      try {
        int t = 0;

        out.println(",\"realtime\":");
        if (resultSensorData.hasNext()) {
          out.println("{ \"head\": {"
                  + "\"sensor\":[ "
                  + "\"" + nomeSensore + "\""
                  + "],"
                  + "\"vars\":[ "
                  + "\"avgDistance\", "
                  + "\"avgTime\","
                  + "\"occupancy\","
                  + "\"concentration\","
                  + "\"vehicleFlow\","
                  + "\"averageSpeed\","
                  + "\"thresholdPerc\","
                  + "\"speedPercentile\","
                  + "\"instantTime\""
                  + "]"
                  + "},");
          out.println("\"results\": {");
          out.println("\"bindings\": [");
          while (resultSensorData.hasNext()) {
            BindingSet bindingSetSensorData = resultSensorData.next();
            valueOfInstantDateTime = bindingSetSensorData.getValue("timeInstant").stringValue();
            String valueOfAvgDistance = "Not Available";
            if (bindingSetSensorData.getValue("avgDistance") != null) {
              valueOfAvgDistance = bindingSetSensorData.getValue("avgDistance").stringValue();
            }
            String valueOfAvgTime = "Not Available";
            if (bindingSetSensorData.getValue("avgTime") != null) {
              valueOfAvgTime = bindingSetSensorData.getValue("avgTime").stringValue();
            }
            String valueOfOccupancy = "Not Available";
            if (bindingSetSensorData.getValue("occupancy") != null) {
              valueOfOccupancy = bindingSetSensorData.getValue("occupancy").stringValue();
            }
            String valueOfConcentration = "Not Available";
            if (bindingSetSensorData.getValue("concentration") != null) {
              valueOfConcentration = bindingSetSensorData.getValue("concentration").stringValue();
            }
            String valueOfVehicleFlow = "Not Available";
            if (bindingSetSensorData.getValue("vehicleFlow") != null) {
              valueOfVehicleFlow = bindingSetSensorData.getValue("vehicleFlow").stringValue();
            }
            String valueOfAverageSpeed = "Not Available";
            if (bindingSetSensorData.getValue("averageSpeed") != null) {
              valueOfAverageSpeed = bindingSetSensorData.getValue("averageSpeed").stringValue();
            }
            String valueOfThresholdPerc = "Not Available";
            if (bindingSetSensorData.getValue("thresholdPerc") != null) {
              valueOfThresholdPerc = bindingSetSensorData.getValue("thresholdPerc").stringValue();
            }
            String valueOfSpeedPercentile = "Not Available";
            if (bindingSetSensorData.getValue("speedPercentile") != null) {
              valueOfSpeedPercentile = bindingSetSensorData.getValue("speedPercentile").stringValue();
            }

            if (t != 0) {
              out.println(", ");
            }

            out.println("{");
            out.println("\"avgDistance\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfAvgDistance + "\" "
                    + " },");
            out.println("\"avgTime\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfAvgTime + "\" "
                    + " },");
            out.println("\"occupancy\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfOccupancy + "\" "
                    + " },");
            out.println("\"concentration\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfConcentration + "\" "
                    + " },");
            out.println("\"vehicleFlow\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfVehicleFlow + "\" "
                    + " },");
            out.println("\"averageSpeed\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfAverageSpeed + "\" "
                    + " },");
            out.println("\"thresholdPerc\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfThresholdPerc + "\" "
                    + " },");
            out.println("\"speedPercentile\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfSpeedPercentile + "\" "
                    + " },");
            out.println("\"instantTime\": {"
                    + "\"type\": \"literal\","
                    + "\"value\": \"" + valueOfInstantDateTime + "\" "
                    + " }");
            out.println("}");
            t++;
          }
          out.println("]}}");

        } else {
          out.println("{}");
        }
      } catch (Exception e) {
        out.println(e.getMessage());
      }
    }
    out.println("}");
  }

  public String queryService(JspWriter out, RepositoryConnection con, String idService, String lang, String realtime, String uid) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String r = "";
    int i = 0;
    String queryService = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX schema:<http://schema.org/>\n"
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
            + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
            + "SELECT ?serAddress ?serNumber ?elat ?elong ?sName ?sType ?type ?sCategory ?sTypeIta ?email ?note ?multimedia ?descriptionEng ?descriptionIta ?phone ?fax ?website ?prov ?city ?cap ?coordList WHERE{\n"
            + " {\n"
            + "  <" + idService + "> km4c:hasAccess ?entry.\n"
            + "  ?entry geo:lat ?elat.\n"
            + "  ?entry geo:long ?elong.\n"
            + " }UNION{\n"
            + "  <" + idService + "> km4c:isInRoad ?road.\n"
            + "  <" + idService + "> geo:lat ?elat.\n"
            + "  <" + idService + "> geo:long ?elong.\n"
            + " }UNION{\n"
            + "  <" + idService + "> geo:lat ?elat.\n"
            + "  <" + idService + "> geo:long ?elong.\n"
            + " }\n"
            + "   OPTIONAL {<" + idService + "> schema:name ?sName.}\n"
            + "   OPTIONAL { <" + idService + "> schema:streetAddress ?serAddress.}\n"
            + "   OPTIONAL {<" + idService + "> opengis:hasGeometry ?geometry .\n"
            + "     ?geometry opengis:asWKT ?coordList .}\n"
            + (km4cVersion.equals("old")
                    ? " <" + idService + "> km4c:hasServiceCategory ?cat .\n"
                    + " ?cat rdfs:label ?nome.\n"
                    + " BIND (?nome  AS ?sType).\n"
                    + " BIND (?nome  AS ?sTypeIta).\n"
                    + " FILTER(LANG(?nome) = \"it\").\n"
                    + " OPTIONAL {<" + idService + "> <http://purl.org/dc/elements/1.1/description> ?description.\n"
                    + " FILTER(LANG(?description) = \"it\")}.\n"
                : " <" + idService + "> a ?type . FILTER(?type!=km4c:RegularService && ?type!=km4c:Service && ?type!=km4c:DigitalLocation)\n"
                    + " ?type rdfs:label ?nome.\n"
                    + " ?type rdfs:subClassOf ?sCategory.\n"
                    + " BIND (?nome  AS ?sType).\n"
                    + " BIND (?nome  AS ?sTypeIta).\n"
                    + " FILTER(LANG(?nome) = \"" + lang + "\").\n")
            + "   OPTIONAL {<" + idService + "> km4c:houseNumber ?serNumber}.\n"
            + "   OPTIONAL {<" + idService + "> dcterms:description ?descriptionIta\n"
            //+ " FILTER(LANG(?descriptionEng) = \"en\")"
            + "}\n"
            + "   OPTIONAL {<" + idService + "> dcterms:description ?descriptionEng FILTER(?descriptionEng!=?descriptionIta)\n"
            + "}"
            + "   OPTIONAL {<" + idService + "> km4c:multimediaResource ?multimedia}.\n"
            + "   OPTIONAL {<" + idService + "> skos:note ?note}.\n"
            + "   OPTIONAL {<" + idService + "> schema:email ?email }.\n"
            + "   OPTIONAL {<" + idService + "> schema:faxNumber ?fax}\n"
            + "   OPTIONAL {<" + idService + "> schema:telephone ?phone}\n"
            + "   OPTIONAL {<" + idService + "> schema:addressRegion ?prov}\n"
            + "   OPTIONAL {<" + idService + "> schema:addressLocality ?city}\n"
            + "   OPTIONAL {<" + idService + "> schema:postalCode ?cap}\n"
            + "   OPTIONAL {<" + idService + "> schema:url ?website}\n"
            + "} LIMIT 1";
    // out.println("count = "+count);
    String queryDBpedia = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX cito:<http://purl.org/spar/cito/>\n"
            + "SELECT ?linkDBpedia WHERE{\n"
            + " OPTIONAL {<" + idService + "> km4c:isInRoad ?road.\n"
            + "?road cito:cites ?linkDBpedia.}\n"
            + "}";

    out.println("{ \"Service\":\n"
            + "{\"type\": \"FeatureCollection\",\n"
            + "\"features\": [\n");
    TupleQuery tupleQueryService = con.prepareTupleQuery(QueryLanguage.SPARQL, queryService);
    long ts = System.nanoTime();
    TupleQueryResult resultService = tupleQueryService.evaluate();
    logQuery(queryService, "API-service-info", sparqlType, idService, System.nanoTime() - ts);
    String TOS = "";
    String NOS = "";
    TupleQuery tupleQueryDBpedia = con.prepareTupleQuery(QueryLanguage.SPARQL, queryDBpedia);
    ts = System.nanoTime();
    TupleQueryResult resultDBpedia = tupleQueryDBpedia.evaluate();
    logQuery(queryService, "API-service-dbpedia-info", sparqlType, idService, System.nanoTime() - ts);
    String valueOfDBpedia = "[";

    while (resultService.hasNext()) {
      BindingSet bindingSetService = resultService.next();
      while (resultDBpedia.hasNext()) {

        BindingSet bindingSetDBpedia = resultDBpedia.next();
        if (bindingSetDBpedia.getValue("linkDBpedia") != null) {
          if (!("[".equals(valueOfDBpedia))) {
            valueOfDBpedia = valueOfDBpedia + ", \"" + bindingSetDBpedia.getValue("linkDBpedia").stringValue() + "\"";
          } else {
            valueOfDBpedia = valueOfDBpedia + "\"" + bindingSetDBpedia.getValue("linkDBpedia").stringValue() + "\"";
          }
        }
      }
      valueOfDBpedia = valueOfDBpedia + "]";
      String valueOfSerAddress = "";
      if (bindingSetService.getValue("serAddress") != null) {
        valueOfSerAddress = bindingSetService.getValue("serAddress").stringValue();
      }
      String valueOfSerNumber = "";
      if (bindingSetService.getValue("serNumber") != null) {
        valueOfSerNumber = bindingSetService.getValue("serNumber").stringValue();
      }

      String valueOfSType = bindingSetService.getValue("sType").stringValue();
      String valueOfSTypeIta = "";
      if (bindingSetService.getValue("sTypeIta") != null) {
        valueOfSTypeIta = bindingSetService.getValue("sTypeIta").stringValue();
      }

      // DICHIARAZIONE VARIABILI serviceType e serviceCategory per ICONA
      String subCategory = "";
      if (bindingSetService.getValue("type") != null) {
        subCategory = bindingSetService.getValue("type").stringValue();
        subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
        //subCategory = Character.toLowerCase(subCategory.charAt(0)) + subCategory.substring(1);
        //subCategory = subCategory.replace(" ", "_");
      }

      String category = "";
      if (bindingSetService.getValue("sCategory") != null) {
        category = bindingSetService.getValue("sCategory").stringValue();
        category = category.replace("http://www.disit.org/km4city/schema#", "");
        //category = Character.toLowerCase(category.charAt(0)) + category.substring(1);
        //category = category.replace(" ", "_");
      }

      String serviceType = category + "_" + subCategory;
      r = category + ";" + subCategory; // return value

      // controllo del Nome per i Geolocated Object
      String valueOfSName = "";
      if (bindingSetService.getValue("sName") != null) {
        valueOfSName = bindingSetService.getValue("sName").stringValue();
      } else {
        valueOfSName = subCategory.replace("_", " ").toUpperCase();
      }
      String valueOfELat = bindingSetService.getValue("elat").stringValue();
      String valueOfELong = bindingSetService.getValue("elong").stringValue();
      String valueOfNote = "";
      if (bindingSetService.getValue("note") != null) {
        valueOfNote = bindingSetService.getValue("note").stringValue();
      }

      String valueOfEmail = "";
      if (bindingSetService.getValue("email") != null) {
        valueOfEmail = bindingSetService.getValue("email").stringValue();
      }
      String valueOfMultimediaResource = "";
      if (bindingSetService.getValue("multimedia") != null) {
        valueOfMultimediaResource = bindingSetService.getValue("multimedia").stringValue();
      }
      String valueOfDescriptionIta = "";
      if (bindingSetService.getValue("descriptionIta") != null) {
        valueOfDescriptionIta = bindingSetService.getValue("descriptionIta").stringValue();
      }
      String valueOfDescriptionEng = "";
      if (bindingSetService.getValue("descriptionEng") != null) {
        valueOfDescriptionEng = bindingSetService.getValue("descriptionEng").stringValue();
      }
      //AGGIUNTA CAMPI DA VISUALIZZARE SU SCHEDA
      String valueOfFax = "";
      if (bindingSetService.getValue("fax") != null) {
        valueOfFax = bindingSetService.getValue("fax").stringValue();
      }
      String valueOfPhone = "";
      if (bindingSetService.getValue("phone") != null) {
        valueOfPhone = bindingSetService.getValue("phone").stringValue();
      }
      String valueOfProv = "";
      if (bindingSetService.getValue("prov") != null) {
        valueOfProv = bindingSetService.getValue("prov").stringValue();
      }
      String valueOfCity = "";
      if (bindingSetService.getValue("city") != null) {
        valueOfCity = bindingSetService.getValue("city").stringValue();
      }
      String valueOfUrl = "";
      if (bindingSetService.getValue("website") != null) {
        valueOfUrl = bindingSetService.getValue("website").stringValue();
      }
      String valueOfCap = "";
      if (bindingSetService.getValue("cap") != null) {
        valueOfCap = bindingSetService.getValue("cap").stringValue();
      }
      String valueOfCoordList = "";
      if (bindingSetService.getValue("coordList") != null) {
        valueOfCoordList = bindingSetService.getValue("coordList").stringValue();
      }
      NOS = valueOfSName;

      valueOfSTypeIta = valueOfSTypeIta.replace("@it", "");
      TOS = valueOfSTypeIta;

      Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
      valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 \\.:;,]+", "");

      float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);

      if (i != 0) {
        out.println(", ");
      }

      out.println("{ "
              + " \"geometry\": {\n"
              + "     \"type\": \"Point\",\n"
              + "    \"coordinates\": [ " + valueOfELong + ", " + valueOfELat + " ]\n"
              + "},\n"
              + "\"type\": \"Feature\",\n"
              + "\"properties\": {\n"
              + "    \"name\": \"" + escapeJSON(valueOfSName) + "\",\n"
              + "    \"typeLabel\": \"" + TOS + "\",\n"
              + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\",\n"
              + "    \"phone\": \"" + escapeJSON(valueOfPhone) + "\",\n"
              + "    \"fax\": \"" + escapeJSON(valueOfFax) + "\",\n"
              + "    \"website\": \"" + escapeJSON(valueOfUrl) + "\",\n"
              + "    \"province\": \"" + escapeJSON(valueOfProv) + "\",\n"
              + "    \"city\": \"" + escapeJSON(valueOfCity) + "\",\n"
              + "    \"cap\": \"" + escapeJSON(valueOfCap) + "\",\n"
              + "    \"email\": \"" + escapeJSON(valueOfEmail) + "\",\n"
              + "    \"linkDBpedia\": " + valueOfDBpedia + ",\n"
              + "    \"note\": \"" + escapeJSON(valueOfNote) + "\",\n"
              + "    \"description\": \"" + escapeJSON(valueOfDescriptionIta) + "\",\n"
              + "    \"description2\": \"" + escapeJSON(valueOfDescriptionEng) + "\",\n"
              + "    \"multimedia\": \"" + valueOfMultimediaResource + "\",\n"
              + "    \"serviceUri\": \"" + idService + "\",\n"
              + "    \"address\": \"" + escapeJSON(valueOfSerAddress) + "\", \"civic\": \"" + escapeJSON(valueOfSerNumber) + "\",\n"
              + "    \"wktGeometry\": \"" + escapeJSON(ServiceMap.fixWKT(valueOfCoordList)) + "\",\n"
              + "    \"photos\": " + ServiceMap.getServicePhotos(idService) + ",\n"
              + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(idService,"thumbs") + ",\n"
              + "    \"photoOrigs\": " + ServiceMap.getServicePhotos(idService,"originals") + ",\n"
              + "    \"avgStars\": " + avgServiceStars[0] + ",\n"
              + "    \"starsCount\": " + (int) avgServiceStars[1] + ",\n"
              + (uid != null ? "    \"userStars\": " + ServiceMap.getServiceStarsByUid(idService, uid) + ",\n" : "")
              + "    \"comments\": " + ServiceMap.getServiceComments(idService)
              + "},\n"
              + "\"id\": " + Integer.toString(i + 1) + "\n"
              + "}");
      i++;
    }
    out.println("] }");
    String labelPark = "Car park";
    if (lang.equals("it")) {
      labelPark = "Parcheggio auto";
    }
    if (TOS.equals(labelPark) && "true".equalsIgnoreCase(realtime)) {
      String queryStringParkingStatus = "  PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX time:<http://www.w3.org/2006/time#>\n"
              + "SELECT distinct ?situationRecord ?instantDateTime ?occupancy ?free ?occupied ?capacity ?cpStatus WHERE { \n"
              + "	?cps km4c:observeCarPark <" + idService + ">.\n"
              + "	?cps km4c:capacity ?capacity.\n"
              + "	?situationRecord km4c:relatedToSensor ?cps.\n"
              + "	?situationRecord km4c:observationTime ?time.\n"
              + "	?time <http://purl.org/dc/terms/identifier> ?instantDateTime.\n"
              + "	OPTIONAL{?situationRecord km4c:parkOccupancy ?occupancy.}\n"
              + "	?situationRecord km4c:free ?free.\n"
              + "	OPTIONAL{?situationRecord km4c:carParkStatus ?cpStatus.}\n"
              + "	OPTIONAL{?situationRecord km4c:occupied ?occupied.}\n"
              + "} ORDER BY DESC (?instantDateTime) LIMIT 1";
      TupleQuery tupleQueryParking = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringParkingStatus);
      ts = System.nanoTime();
      TupleQueryResult resultParkingStatus = tupleQueryParking.evaluate();
      logQuery(queryService, "API-service-park-info", sparqlType, idService, System.nanoTime() - ts);
      out.println(",\"realtime\": ");
      if (resultParkingStatus.hasNext()) {
        out.println("{ \"head\": {"
                + "\"parkingArea\":[ "
                + "\"" + NOS + "\""
                + "],"
                + "\"vars\":[ "
                + "\"capacity\", "
                + "\"freeParkingLots\","
                + "\"occupiedParkingLots\","
                + "\"occupancy\","
                + "\"updating\""
                + "]"
                + "},");
        out.println("\"results\": {");
        out.println("\"bindings\": [");

        int p = 0;
        while (resultParkingStatus.hasNext()) {
          BindingSet bindingSetParking = resultParkingStatus.next();

          String valueOfInstantDateTime = "";
          if (bindingSetParking.getValue("instantDateTime") != null) {
            valueOfInstantDateTime = bindingSetParking.getValue("instantDateTime").stringValue();
          }
          String valueOfOccupancy = "";
          if (bindingSetParking.getValue("occupancy") != null) {
            valueOfOccupancy = bindingSetParking.getValue("occupancy").stringValue();
          }
          String valueOfFree = "";
          if (bindingSetParking.getValue("free") != null) {
            valueOfFree = bindingSetParking.getValue("free").stringValue();
          }
          String valueOfOccupied = "";
          if (bindingSetParking.getValue("occupied") != null) {
            valueOfOccupied = bindingSetParking.getValue("occupied").stringValue();
          }
          String valueOfCapacity = "";
          if (bindingSetParking.getValue("capacity") != null) {
            valueOfCapacity = bindingSetParking.getValue("capacity").stringValue();
          }
          String valueOfcpStatus = "";
          if (bindingSetParking.getValue("cpStatus") != null) {
            valueOfcpStatus = bindingSetParking.getValue("cpStatus").stringValue();
          }

          if (p != 0) {
            out.println(", ");
          }

          out.println("{"
                  + "\"capacity\": {"
                  + "\"value\": \"" + valueOfCapacity + "\" "
                  + " },"
                  + "\"freeParkingLots\": { "
                  + "\"value\": \"" + valueOfFree + "\" "
                  + " },"
                  + "\"occupiedParkingLots\": {"
                  + "\"value\": \"" + valueOfOccupied + "\" "
                  + " },"
                  + "\"occupancy\": {"
                  + "\"value\": \"" + valueOfOccupancy + "\" "
                  + " },"
                  + "\"status\": {"
                  + "\"value\": \"" + valueOfcpStatus + "\" "
                  + " },"
                  + "\"updating\": {"
                  + "\"value\": \"" + valueOfInstantDateTime + "\" "
                  + " }");
          out.println("}");
          p++;
        }
        out.println("]}}}");
      } else {
        out.println("{}}");
      }
    } else {
      out.println("}");
    }
    return r;
  }

  public void queryEvent(JspWriter out, RepositoryConnection con, String idService, String lang, String uid) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    int i = 0;
    String queryService = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX schema:<http://schema.org/>"
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>"
            + "SELECT ?elat ?elong ?name1 ?name2 ?note ?description1 ?description2 ?website ?address ?number ?prov ?city ?startDate ?endDate ?startTime ?categ1 ?categ2 WHERE{"
            + " {"
            + "  <" + idService + "> km4c:hasAccess ?entry ."
            + "  ?entry geo:lat ?elat;"
            + "   geo:long ?elong."
            + " }UNION{"
            + "  <" + idService + "> km4c:isInRoad ?road;"
            + "   geo:lat ?elat;"
            + "   geo:long ?elong."
            + " }UNION{"
            + "  <" + idService + "> geo:lat ?elat;"
            + "   geo:long ?elong."
            + " }"
            + " OPTIONAL {<" + idService + "> schema:name ?name1"
            + "  OPTIONAL {<" + idService + "> schema:name ?name2 FILTER(?name2!=?name1)}}"
            + " OPTIONAL { <" + idService + "> schema:streetAddress ?address}"
            + " OPTIONAL {<" + idService + "> km4c:houseNumber ?number}"
            + " OPTIONAL {<" + idService + "> schema:addressRegion ?prov }"
            + " OPTIONAL {<" + idService + "> schema:addressLocality ?city }"
            + " OPTIONAL {<" + idService + "> schema:url ?website }"
            + " OPTIONAL {<" + idService + "> schema:description ?description1"
            + "  OPTIONAL {<" + idService + "> schema:description ?description2 FILTER(?description1!=?description2)}}"
            + " OPTIONAL {<" + idService + "> skos:note ?note}"
            + " OPTIONAL {<" + idService + "> schema:startDate ?startDate}"
            + " OPTIONAL {<" + idService + "> schema:endDate ?endDate}"
            + " OPTIONAL {<" + idService + "> km4c:eventTime ?startTime}"
            + " OPTIONAL {<" + idService + "> km4c:freeEvent ?freeEvent}"
            + " OPTIONAL {<" + idService + "> schema:price ?price}"
            + " OPTIONAL {<" + idService + "> km4c:eventCategory ?categ1"
            + "  OPTIONAL {<" + idService + "> km4c:eventCategory ?categ2 FILTER(?categ2!=?categ1)}}"
            //+ "   OPTIONAL {<" + idService + "> schema:email ?email } . "
            + "}LIMIT 1";

    out.println("{ \"Event\":"
            + "{\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    TupleQuery tupleQueryService = con.prepareTupleQuery(QueryLanguage.SPARQL, queryService);
    long ts = System.nanoTime();
    TupleQueryResult resultService = tupleQueryService.evaluate();
    logQuery(queryService, "API-event-info", sparqlType, idService, System.nanoTime() - ts);
    String TOS = "";
    String NOS = "";

    while (resultService.hasNext()) {
      BindingSet bindingSetService = resultService.next();
      String valueOfName = "";
      if (bindingSetService.getValue("name1") != null) {
        valueOfName = bindingSetService.getValue("name1").stringValue();
      }
      String valueOfName2 = "";
      if (bindingSetService.getValue("name2") != null) {
        valueOfName2 = bindingSetService.getValue("name2").stringValue();
      }
      String valueOfELat = bindingSetService.getValue("elat").stringValue();
      String valueOfELong = bindingSetService.getValue("elong").stringValue();
      String valueOfNote = "";
      if (bindingSetService.getValue("note") != null) {
        valueOfNote = bindingSetService.getValue("note").stringValue();
      }
      String valueOfDescriptionIta = "";
      if (bindingSetService.getValue("description1") != null) {
        valueOfDescriptionIta = bindingSetService.getValue("description1").stringValue();
      }
      String valueOfDescriptionEng = "";
      if (bindingSetService.getValue("description2") != null) {
        valueOfDescriptionEng = bindingSetService.getValue("description2").stringValue();
      }
      String valueOfAddress = "";
      if (bindingSetService.getValue("address") != null) {
        valueOfAddress = bindingSetService.getValue("address").stringValue();
      }
      String valueOfNumber = "";
      if (bindingSetService.getValue("number") != null) {
        valueOfNumber = bindingSetService.getValue("number").stringValue();
      }
      String valueOfProv = "";
      if (bindingSetService.getValue("prov") != null) {
        valueOfProv = bindingSetService.getValue("prov").stringValue();
      }
      String valueOfCity = "";
      if (bindingSetService.getValue("city") != null) {
        valueOfCity = bindingSetService.getValue("city").stringValue();
      }
      String valueOfUrl = "";
      if (bindingSetService.getValue("website") != null) {
        valueOfUrl = bindingSetService.getValue("website").stringValue();
      }
      String valueOfStartDate = "";
      if (bindingSetService.getValue("startDate") != null) {
        valueOfStartDate = bindingSetService.getValue("startDate").stringValue();
      }
      String valueOfStartTime = "";
      if (bindingSetService.getValue("startTime") != null) {
        valueOfStartTime = bindingSetService.getValue("startTime").stringValue();
      }
      String valueOfEndDate = "";
      if (bindingSetService.getValue("endDate") != null) {
        valueOfEndDate = bindingSetService.getValue("endDate").stringValue();
      }
      String valueOfCateg1 = "";
      if (bindingSetService.getValue("categ1") != null) {
        valueOfCateg1 = bindingSetService.getValue("categ1").stringValue();
      }
      String valueOfCateg2 = "";
      if (bindingSetService.getValue("categ2") != null) {
        valueOfCateg2 = bindingSetService.getValue("categ2").stringValue();
      }

      NOS = valueOfName;

      Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
      valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 \\.,:;]+", "");

      if (i != 0) {
        out.println(", ");
      }

      float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);

      out.println("{ "
              + " \"geometry\": {  "
              + "     \"type\": \"Point\",  "
              + "    \"coordinates\": [  "
              + "       " + valueOfELong + ",  "
              + "      " + valueOfELat + "  "
              + " ]  "
              + "},"
              + "\"type\": \"Feature\",  "
              + "\"properties\": {  "
              + "    \"serviceUri\": \"" + idService + "\", "
              + "    \"name\": \"" + escapeJSON(valueOfName) + "\", "
              + "    \"name2\": \"" + escapeJSON(valueOfName2) + "\", "
              + "    \"website\": \"" + escapeJSON(valueOfUrl) + "\", "
              + "    \"address\": \"" + escapeJSON(valueOfAddress) + "\","
              + "    \"number\": \"" + escapeJSON(valueOfNumber) + "\", "
              + "    \"province\": \"" + escapeJSON(valueOfProv) + "\", "
              + "    \"city\": \"" + escapeJSON(valueOfCity) + "\", "
              + "    \"note\": \"" + escapeJSON(valueOfNote) + "\", "
              + "    \"description\": \"" + escapeJSON(valueOfDescriptionIta) + "\", "
              + "    \"description2\": \"" + escapeJSON(valueOfDescriptionEng) + "\", "
              + "    \"startDate\": \"" + escapeJSON(valueOfStartDate) + "\", "
              + "    \"startTime\": \"" + escapeJSON(valueOfStartTime) + "\", "
              + "    \"endDate\": \"" + escapeJSON(valueOfEndDate) + "\", "
              + "    \"eventCategory\": \"" + escapeJSON(valueOfCateg1) + "\", "
              + "    \"eventCategory2\": \"" + escapeJSON(valueOfCateg2) + "\", "
              + "    \"photos\": " + ServiceMap.getServicePhotos(idService) + ",\n"
              + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(idService,"thumbs") + ",\n"
              + "    \"photoOrigs\": " + ServiceMap.getServicePhotos(idService,"originals") + ",\n"
              + "    \"avgStars\": " + avgServiceStars[0] + ",\n"
              + "    \"starsCount\": " + (int) avgServiceStars[1] + ",\n"
              + (uid != null ? "    \"userStars\": " + ServiceMap.getServiceStarsByUid(idService, uid) + ",\n" : "")
              + "    \"comments\": " + ServiceMap.getServiceComments(idService)
              + "}, "
              + "\"id\": " + Integer.toString(i + 1) + "  "
              + "}");
      i++;
    }
    out.println("] }}");
  }

  public void queryEventList(JspWriter out, RepositoryConnection con, String range, String[] coords, String dist, String numEv, String textFilter, boolean photos) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String eventRange = range;
    String numEventi = numEv;
    int i = 0;
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    Date data_attuale = new Date();
    String data_inizio = df.format(data_attuale);
    String data_fine = df.format(data_attuale);
    if (eventRange.equals("week")) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(data_attuale);
      calendar.add(Calendar.DAY_OF_YEAR, 7);
      data_inizio = df.format(calendar.getTime());
    } else if (eventRange.equals("month")) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(data_attuale);
      calendar.add(Calendar.DAY_OF_YEAR, 30);
      data_inizio = df.format(calendar.getTime());
    }
    String args = "";
    if (coords != null) {
      for (String c : coords) {
        args += c + ";";
      }
    }
    args += range + ";" + dist + ";" + numEv + ";" + textFilter;

    String queryEvList = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> \n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
            + "PREFIX schema:<http://schema.org/> \n"
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/> \n"
            + "SELECT DISTINCT ?ev ?elat ?elong  ?nameIta ?place (substr(str(?sDate), 1,10) AS ?startDate)  (substr(str(?eDate), 1,10) AS ?endDate) ?time ?cost ?identifier ?catIta ?civic ?address ?price ?website ?phone ?descIta ?x WHERE{\n"
            /*+ " {"
             + "  <" + idService + "> km4c:hasAccess ?entry ."
             + "  ?entry geo:lat ?elat;"
             + "   geo:long ?elong."
             + " }UNION{"
             + "  <" + idService + "> km4c:isInRoad ?road;"
             + "   geo:lat ?elat;"
             + "   geo:long ?elong."
             + " }UNION{"*/
            + " ?ev rdf:type km4c:Event. \n"
            + " ?ev dcterms:identifier ?identifier. \n"
            + ServiceMap.textSearchQueryFragment("?ev", "?p", textFilter)
            + "  OPTIONAL {?ev geo:lat ?elat; \n"
            + "   geo:long ?elong.} \n"
            + " ?ev schema:name ?nameIta. FILTER(LANG(?nameIta)= \"it\") \n"
            + " ?ev km4c:placeName ?place. \n"
            + " ?ev schema:startDate ?sDate. FILTER (?sDate <= \"" + data_inizio + "\"^^xsd:date). \n"
            + " OPTIONAL {?ev schema:endDate ?eDate. FILTER (xsd:date(?eDate) >= \"" + data_fine + "\"^^xsd:date).} \n"
            //+ " OPTIONAL {?ev schema:endDate ?eDate.} \n"
            + " OPTIONAL {?ev km4c:eventTime ?time.} \n"
            + " OPTIONAL {?ev km4c:freeEvent ?cost.} \n"
            + " OPTIONAL {?ev km4c:eventCategory ?catIta. FILTER(LANG(?catIta)= \"it\")} \n"
            + " OPTIONAL {?ev km4c:houseNumber ?civic.} \n"
            + " OPTIONAL {?ev schema:streetAddress ?address.} \n"
            + " OPTIONAL {?ev schema:price ?price.} \n"
            + ServiceMap.geoSearchQueryFragment("?ev", coords, dist)
            + " OPTIONAL {?ev schema:description ?descIta. FILTER(LANG(?descIta)= \"it\")} \n"
            + " OPTIONAL {?ev schema:url ?website.} \n"
            + " OPTIONAL {?ev schema:telephone ?phone.} \n"
            + "} ORDER BY asc(?sDate)";

    if (numEventi != null && !numEventi.equals("0")) {
      queryEvList += " LIMIT " + numEventi;
    }

    //System.out.println(queryEvList);
    out.println("{ \"Event\":"
            + "{\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    TupleQuery tupleQueryEvList = con.prepareTupleQuery(QueryLanguage.SPARQL, queryEvList);
    TupleQueryResult resultEvList = tupleQueryEvList.evaluate();
    long ts = System.nanoTime();
    logQuery(queryEvList, "API-event-list", sparqlType, args, System.nanoTime() - ts);

    while (resultEvList.hasNext()) {
      BindingSet bindingSetEvList = resultEvList.next();
      String valueOfName = "";
      if (bindingSetEvList.getValue("nameIta") != null) {
        valueOfName = bindingSetEvList.getValue("nameIta").stringValue();
      }

      //if(i == 1){
      String valueOfELat = bindingSetEvList.getValue("elat").stringValue();
      String valueOfELong = bindingSetEvList.getValue("elong").stringValue();
      /*}
       else{
          
       }*/

      String valueOfPlace = "";
      if (bindingSetEvList.getValue("place") != null) {
        valueOfPlace = bindingSetEvList.getValue("place").stringValue();
      }

      String valueOfIdentifier = "";
      if (bindingSetEvList.getValue("ev") != null) {
        valueOfIdentifier = bindingSetEvList.getValue("ev").stringValue();
      }

      String valueOfSDate = "";
      if (bindingSetEvList.getValue("startDate") != null) {
        valueOfSDate = bindingSetEvList.getValue("startDate").stringValue();
        //valueOfSDate = valueOfSDate.replace("T00:00:00+01:00", "");
        //valueOfSDate = valueOfSDate.replace("T00:00:00+02:00", "");

      }
      String valueOfEDate = "";
      if (bindingSetEvList.getValue("endDate") != null) {
        valueOfEDate = bindingSetEvList.getValue("endDate").stringValue();
        //valueOfEDate = valueOfEDate.replace("T00:00:00+01:00", "");
        //valueOfEDate = valueOfEDate.replace("T00:00:00+02:00", "");
      }
      String valueOfTime = "";
      if (bindingSetEvList.getValue("time") != null && !bindingSetEvList.getValue("time").stringValue().equals("-/-")) {
        valueOfTime = bindingSetEvList.getValue("time").stringValue();
      }
      String valueOfCost = "";
      if (bindingSetEvList.getValue("cost") != null) {
        valueOfCost = bindingSetEvList.getValue("cost").stringValue();
      }

      String valueOfAddress = "";
      if (bindingSetEvList.getValue("address") != null) {
        valueOfAddress = bindingSetEvList.getValue("address").stringValue();
      }

      String valueOfCivic = "";
      if (bindingSetEvList.getValue("civic") != null) {
        valueOfCivic = bindingSetEvList.getValue("civic").stringValue();
      }

      String valueOfCatIta = "";
      if (bindingSetEvList.getValue("catIta") != null) {
        valueOfCatIta = bindingSetEvList.getValue("catIta").stringValue();
      }

      String valueOfPrice = "";
      if (bindingSetEvList.getValue("price") != null) {
        valueOfPrice = bindingSetEvList.getValue("price").stringValue();
      }

      String valueOfDescIta = "";
      if (bindingSetEvList.getValue("descIta") != null) {
        valueOfDescIta = bindingSetEvList.getValue("descIta").stringValue();
      }

      String valueOfWebsite = "";
      if (bindingSetEvList.getValue("website") != null) {
        valueOfWebsite = bindingSetEvList.getValue("website").stringValue();
      }

      String valueOfPhone = "";
      if (bindingSetEvList.getValue("phone") != null) {
        valueOfPhone = bindingSetEvList.getValue("phone").stringValue();
      }

      String serviceType = "Event";

      /*Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
       valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 \\.,:;]+", "");*/
      if ((!valueOfEDate.equals("")) || (valueOfEDate.equals("") && (valueOfSDate.equals(data_inizio)))) {
        if (i != 0) {
          out.println(", ");
        }

        out.println("{ "
                + " \"geometry\": {  "
                + "     \"type\": \"Point\",  "
                + "    \"coordinates\": [  "
                + "       " + valueOfELong + ",  "
                + "      " + valueOfELat + "  "
                + " ]  "
                + "},"
                + "\"type\": \"Feature\",  "
                + "\"properties\": {  "
                + "    \"serviceUri\": \"" + escapeJSON(valueOfIdentifier) + "\", "
                //+ "    \"nome\": \"" + escapeJSON(valueOfName) + "\", "
                + "    \"name\": \"" + escapeJSON(valueOfName) + "\", "
              + "    \"tipo\": \"event\", "
                + "    \"place\": \"" + escapeJSON(valueOfPlace) + "\", "
                + "    \"startDate\": \"" + escapeJSON(valueOfSDate) + "\", "
                + "    \"startTime\": \"" + escapeJSON(valueOfTime) + "\", "
                + "    \"endDate\": \"" + escapeJSON(valueOfEDate) + "\", "
                + "    \"freeEvent\": \"" + escapeJSON(valueOfCost) + "\", "
                + "    \"address\": \"" + escapeJSON(valueOfAddress) + "\", "
                + "    \"civic\": \"" + escapeJSON(valueOfCivic) + "\", "
                + "    \"categoryIT\": \"" + escapeJSON(valueOfCatIta) + "\", "
                + "    \"price\": \"" + escapeJSON(valueOfPrice) + "\", "
                + "    \"phone\": \"" + escapeJSON(valueOfPhone) + "\", "
                + "    \"descriptionIT\": \"" + escapeJSON(valueOfDescIta) + "\", "
                + "    \"website\": \"" + escapeJSON(valueOfWebsite) + "\", "
                + (photos ? "    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfIdentifier,"thumbs") + ",\n" : "")
                + "    \"serviceType\": \"" + serviceType + "\" "
                + "}, "
                + "\"id\": " + Integer.toString(i + 1) + "  "
                + "}");
        i++;
      }
    }
    out.println("] }}");
  }

  public void queryTplLatLng(JspWriter out, RepositoryConnection con, String[] coords, String dist, String agency, String numLinee, boolean getPolyline) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");

    String args = "";
    if (coords != null) {
      for (String c : coords) {
        args += c + ";";
      }
    }
    args += dist + ";" + numLinee;

    int i = 0;

    String queryNearTPL = 
            "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n" +
            "PREFIX dcterms:<http://purl.org/dc/terms/>\n" +
            "PREFIX ogis:<http://www.opengis.net/ont/geosparql#>\n" +
            "SELECT DISTINCT ?line ?shape ?bss ?bse (min(?trip) as ?routeId) ?polyline ?ag ?agname {{\n" +
            "SELECT ?line ?trip (MAX(?st) as ?mx) (MIN(?st) as ?mn) {\n" +
            "?bs a gtfs:Stop.\n" +
            ServiceMap.geoSearchQueryFragment("?bs", coords, dist) +
            "?stx gtfs:stop ?bs.\n" +
            "?stx gtfs:trip ?trip.\n" +
            "?st gtfs:trip ?trip.\n" +
            "?trip gtfs:service/dcterms:date ?d.\n" +
            "filter(xsd:date(?d)=xsd:date(now()))\n" +
            "?trip gtfs:route ?route.\n" +
            "OPTIONAL{ ?route gtfs:shortName ?line1.}\n" +
            "?route gtfs:longName ?line2.\n" +
            "BIND(if(?line1,?line1,?line2) as ?line)" +
            "} GROUP BY ?line ?trip\n" +
            "}\n" +
            "?trip ogis:hasGeometry ?shape.\n" +
            (getPolyline ? "?shape ogis:asWKT ?polyline.\n" : "" ) +
            "?mx gtfs:stop/foaf:name ?bse.\n" +
            "?mn gtfs:stop/foaf:name ?bss.\n" +
            "?trip gtfs:route/gtfs:agency ?ag.\n" +
            "?ag foaf:name ?agname.\n" +
            "} group by ?line ?shape ?bss ?bse ?polyline ?ag ?agname order by ?agname ?bss ?bse";

    if (numLinee != null && !numLinee.equals("0")) {
      queryNearTPL += " LIMIT " + numLinee;
    }
    System.out.println(queryNearTPL);
    out.println("{ \"PublicTransportLine\":"
            + "{\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    TupleQuery tupleQueryNearTPL = con.prepareTupleQuery(QueryLanguage.SPARQL, queryNearTPL);
    long ts = System.nanoTime();
    TupleQueryResult resultNearTPL = tupleQueryNearTPL.evaluate();
    logQuery(queryNearTPL, "API-neartpl", sparqlType, args, System.nanoTime() - ts);

    while (resultNearTPL.hasNext()) {
      BindingSet bindingSetNearTPL = resultNearTPL.next();

      String valueOfLineNumber = "";
      if (bindingSetNearTPL.getValue("line") != null) {
        valueOfLineNumber = ServiceMap.escapeJSON(bindingSetNearTPL.getValue("line").stringValue());
      }

      String valueOfLineName = "";
      /*if (bindingSetNearTPL.getValue("nomeLinea") != null) {
        valueOfLineName = bindingSetNearTPL.getValue("nomeLinea").stringValue();
      }*/

      String valueOfRoute = "";
      if (bindingSetNearTPL.getValue("routeId") != null) {
        valueOfRoute = bindingSetNearTPL.getValue("routeId").stringValue();
      }

      String valueOfFirstBS = "";
      if (bindingSetNearTPL.getValue("bss") != null) {
        valueOfFirstBS = ServiceMap.escapeJSON(bindingSetNearTPL.getValue("bss").stringValue());
      }

      String valueOfLastBS = "";
      if (bindingSetNearTPL.getValue("bse") != null) {
        valueOfLastBS = ServiceMap.escapeJSON(bindingSetNearTPL.getValue("bse").stringValue());
      }

      String valueOfPolyline = "";
      if (bindingSetNearTPL.getValue("polyline") != null) {
        valueOfPolyline = bindingSetNearTPL.getValue("polyline").stringValue();
      }

      String valueOfAgency = "";
      if (bindingSetNearTPL.getValue("ag") != null) {
        valueOfAgency = bindingSetNearTPL.getValue("ag").stringValue();
      }
      
      String valueOfAgencyName = "";
      if (bindingSetNearTPL.getValue("agname") != null) {
        valueOfAgencyName = ServiceMap.escapeJSON(bindingSetNearTPL.getValue("agname").stringValue());
      }
      
      String serviceType = "PublicTransportLine";
      String direction = valueOfFirstBS + " \u2794 " + valueOfLastBS;
      String valueOfRouteId = ""; //valueOfRoute.replace("http://www.disit.org/km4city/resource/", "");

      if (i != 0) {
        out.println(", ");
      }

      out.print("{ "
              + "\"type\": \"Feature\",  "
              + "\"properties\": {  "
              + "\"lineNumber\": \"" + escapeJSON(valueOfLineNumber) + "\", "
              + "\"lineName\": \"" + escapeJSON(valueOfLineName) + "\", "
              + "\"route\": \"" + escapeJSON(valueOfRouteId) + "\", "
              + "\"routeUri\": \"" + escapeJSON(valueOfRoute) + "\", "
              + "\"direction\": \"" + escapeJSON(direction) + "\", "
              + "\"agency\": \"" + escapeJSON(valueOfAgencyName) + "\", "
              + "\"agencyUri\": \"" + escapeJSON(valueOfAgency) + "\", "
              + (getPolyline ? "\"polyline\": \"" + ServiceMap.fixWKT(valueOfPolyline) + "\", " : "")
              + "\"serviceType\": \"" + serviceType + "\" "
              + "}, "
              + "\"id\": " + Integer.toString(i + 1) + "  "
              + "}");
      i++;
    }
    out.println("] }}");
  }

  public void queryBusStopsOfLine(JspWriter out, RepositoryConnection con, String nomeLinea, String codRoute, boolean getGeometry) throws Exception {

    if(codRoute!=null && codRoute.startsWith("http://")) {
      out.println("{");
      String routeQuery = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
              + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n"
              + "SELECT DISTINCT ?line ?polyline ?nomeLinea ?ag ?agname WHERE {\n"
              + " <"+codRoute+"> opengis:hasGeometry ?shape;\n"
              + " gtfs:route ?r.\n"
              + " OPTIONAL {?r gtfs:shortName ?line.}\n"
              + " ?r gtfs:longName ?nomeLinea;\n"
              + "  gtfs:agency ?ag.\n"
              + " ?ag foaf:name ?agname.\n"
              + (getGeometry ? " ?shape opengis:asWKT ?polyline.\n" : "")
              + "} LIMIT 1";
      System.out.println(routeQuery);
      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, routeQuery);
      long ts = System.nanoTime();
      TupleQueryResult result = tupleQuery.evaluate();
      logQuery(routeQuery, "get-bus-stops-of-line-route", "any", codRoute, System.nanoTime() - ts);
      out.println("\"Route\":{");
      String valueOfAgUri = "", valueOfAgName = "";
      try {
        if (result.hasNext()) {
          BindingSet bindingSet = result.next();
          String valueOfGeometry = "";
          if (bindingSet.getValue("polyline") != null) {
            valueOfGeometry = bindingSet.getValue("polyline").stringValue();
          }
          String valueOfLineName = JSONObject.escape(bindingSet.getValue("nomeLinea").stringValue());
          String valueOfLineNumber = "";
          if(bindingSet.getValue("line")!=null)
            valueOfLineNumber = JSONObject.escape(bindingSet.getValue("line").stringValue());
          if(bindingSet.getValue("ag")!=null) {
            valueOfAgUri = bindingSet.getValue("ag").stringValue();
            valueOfAgName = JSONObject.escape(bindingSet.getValue("agname").stringValue());
          }
          out.println("\"lineNumber\":\"" + valueOfLineNumber + "\",\"lineName\":\"" + valueOfLineName + "\""
                  + (getGeometry ? ",\"wktGeometry\":\"" + ServiceMap.fixWKT(valueOfGeometry) + "\"" : ""));
        }
      } catch (Exception e) {
        out.println(e.getMessage());
        e.printStackTrace();
      }
      out.println("},");
      String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n"
              + "SELECT DISTINCT ?bs ?bslat ?bslong ?nomeFermata ?type ?x WHERE {\n"
              + " ?st gtfs:trip <"+codRoute+">.\n"
              + " ?st gtfs:stop ?bs.\n"
              + " ?bs a ?type. FILTER(?type!=gtfs:Stop)"
              + " ?st gtfs:stopSequence ?ss.\n"
              + " ?bs geo:lat ?bslat.\n"
              + " ?bs geo:long ?bslong.\n"
              + " ?bs foaf:name ?nomeFermata.\n"
              + "} ORDER BY ASC(?ss)";
      System.out.println(queryString);

      tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      ts = System.nanoTime();
      result = tupleQuery.evaluate();
      logQuery(queryString, "get-bus-stops-of-line-3", "any", nomeLinea, System.nanoTime() - ts);
      out.println("\"BusStops\":{\n"
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      try {
        int i = 0;
        while (result.hasNext()) {
          BindingSet bindingSet = result.next();
          String valueOfBS = bindingSet.getValue("bs").stringValue();
          String valueOfNomeFermata = JSONObject.escape(bindingSet.getValue("nomeFermata").stringValue());
          String valueOfBSLat = bindingSet.getValue("bslat").stringValue();
          String valueOfBSLong = bindingSet.getValue("bslong").stringValue();
          String valueOfType = bindingSet.getValue("type").stringValue();
          valueOfType = valueOfType.substring(valueOfType.lastIndexOf("#")+1);
                  
          if (i != 0) {
            out.println(",");
          }
          out.print("{"
                  + "\"geometry\":{"
                  + "\"type\":\"Point\","
                  + "\"coordinates\":["
                  + valueOfBSLong + ","
                  + valueOfBSLat
                  + "]},"
                  + "\"type\":\"Feature\","
                  + "\"properties\":{"
                  + "\"popupContent\": \"" + valueOfNomeFermata + "\","
                  //+ "\"nome\": \"" + valueOfNomeFermata + "\","
                  + "\"name\": \"" + valueOfNomeFermata + "\","
                  + "\"serviceUri\": \"" + valueOfBS + "\","
                  + "\"tipo\": \"fermata\", "
                  + "\"agency\": \"" + valueOfAgName + "\","
                  + "\"agencyUri\": \"" + valueOfAgUri + "\","
                  + "\"serviceType\": \"TransferServiceAndRenting_"+valueOfType+"\" "
                  + "}, "
                  + "\"id\": " + Integer.toString(i + 1) + " "
                  + "}");
          i++;
        }
      } catch (Exception e) {
        out.println(e.getMessage());
      }

      out.println("]}}");
      return;
    }
    else {
      //throw new IllegalArgumentException("codRoute needed");
    
    if (codRoute == null || "vuoto".equals(codRoute) || "".equals(codRoute)) {
      if (!"all".equals(nomeLinea)) {
        String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
                + "PREFIX schema:<http://schema.org/>\n"
                + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
                + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
                + "SELECT DISTINCT ?bs ?bslat ?bslong ?nomeFermata ?x WHERE {\n"
                + " ?tpll rdf:type km4c:PublicTransportLine.\n"
                + " ?tpll dcterms:identifier \"" + nomeLinea + "\".\n"
                + " ?tpll km4c:hasRoute ?route.\n"
                + " ?route km4c:hasSection ?rs.\n"
                + " ?rs km4c:startsAtStop ?bs.\n"
                + " ?bs foaf:name ?nomeFermata.\n"
                + " ?bs geo:lat ?bslat.\n"
                + " ?bs geo:long ?bslong.\n"
                + "} ORDER BY ?nomeFermata";

        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
        long ts = System.nanoTime();
        TupleQueryResult result = tupleQuery.evaluate();
        logQuery(queryString, "get-bus-stops-of-line-1", "any", nomeLinea, System.nanoTime() - ts);
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
            out.print("{"
                    + "\"geometry\":{"
                    + "\"type\": \"Point\","
                    + "\"coordinates\":["
                    + valueOfBSLong + ","
                    + valueOfBSLat
                    + "]},"
                    + "\"type\":\"Feature\","
                    + "\"properties\":{"
                    + "\"popupContent\":\"" + valueOfNomeFermata + "\","
                    + "\"name\":\"" + valueOfNomeFermata + "\","
                    + "\"serviceUri\":\"" + valueOfBS + "\","
                    + "\"tipo\": \"fermata\", "
                    + "\"serviceType\": \"TransferServiceAndRenting_BusStop\""
                    + "},"
                    + "\"id\": " + Integer.toString(i + 1)
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
                + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
                + "SELECT DISTINCT ?nomeFermata ?lat ?long ?bs ?x WHERE {"
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
        long ts = System.nanoTime();
        TupleQueryResult resultAllBusStop = tupleQueryAllBusStops.evaluate();
        logQuery(queryAllBusStops, "get-bus-stops-of-line-2", "any", nomeLinea, System.nanoTime() - ts);
        //System.out.println(queryAllBusStops);
        out.println("{"
                + "\"type\": \"FeatureCollection\","
                + "\"features\": [");
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
            out.print("{"
                    + "\"geometry\":{"
                    + "\"type\": \"Point\","
                    + "\"coordinates\":["
                    + valueOfBSLong + ","
                    + valueOfBSLat
                    + "]},"
                    + "\"type\":\"Feature\","
                    + "\"properties\":{"
                    + "\"popupContent\":\"" + nome + "\","
                    + "\"name\":\"" + nome + "\","
                    + "\"serviceUri\":\"" + valueOfBS + "\","
                    + "\"tipo\": \"fermata\","
                    + "\"serviceType\": \"TransferServiceAndRenting_BusStop\""
                    + "},"
                    + "\"id\":" + Integer.toString(i + 1)
                    + "}");
            i++;
          }
        } catch (Exception e) {
          out.println(e.getMessage());
        } finally {
          con.close();
        }
      }
    } else {
      out.println("{");
      String routeQuery = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
              + "SELECT DISTINCT ?line ?polyline ?nomeLinea WHERE {\n"
              + " ?tpll rdf:type km4c:PublicTransportLine.\n"
              + " ?tpll dcterms:identifier ?line.\n"
              + " ?tpll km4c:hasRoute ?route.\n"
              + " ?route dcterms:identifier \"" + codRoute + "\".\n"
              + " ?route foaf:name ?nomeLinea. \n"
              + (getGeometry ? " ?route opengis:hasGeometry [opengis:asWKT ?polyline].\n" : "")
              + "} LIMIT 1";
      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, routeQuery);
      long ts = System.nanoTime();
      TupleQueryResult result = tupleQuery.evaluate();
      logQuery(routeQuery, "get-bus-stops-of-line-route", "any", nomeLinea + ";" + codRoute, System.nanoTime() - ts);
      out.println("\"Route\":{");
      try {
        if (result.hasNext()) {
          BindingSet bindingSet = result.next();
          String valueOfGeometry = "";
          if (bindingSet.getValue("polyline") != null) {
            valueOfGeometry = bindingSet.getValue("polyline").stringValue();
          }
          String valueOfLineName = bindingSet.getValue("nomeLinea").stringValue();
          String valueOfLineNumber = bindingSet.getValue("line").stringValue();
          out.println("\"lineNumber\":\"" + valueOfLineNumber + "\",\"lineName\":\"" + valueOfLineName + "\""
                  + (getGeometry ? ",\"wktGeometry\":\"" + ServiceMap.fixWKT(valueOfGeometry) + "\"" : ""));
        }
      } catch (Exception e) {
        out.println(e.getMessage());
        e.printStackTrace();
      }
      out.println("},");
      String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "SELECT DISTINCT ?bs ?bslat ?bslong ?nomeFermata ?x WHERE {\n"
              + " ?route dcterms:identifier \"" + codRoute + "\".\n"
              + " ?route km4c:hasFirstStop ?bs1.\n"
              + " ?route km4c:hasSection ?rs.\n"
              + " ?rs km4c:endsAtStop ?bs2.\n"
              + " ?rs km4c:distance ?dist.\n"
              + " { ?bs1 foaf:name ?nomeFermata .\n"
              + " BIND(?bs1 AS ?bs).\n"
              + " } "
              + " UNION "
              + " { ?bs2 foaf:name ?nomeFermata.\n"
              + " BIND(?bs2 AS ?bs).\n"
              + " }\n"
              + " ?bs geo:lat ?bslat.\n"
              + " ?bs geo:long ?bslong.\n"
              + "} ORDER BY ASC(?dist)";

      tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      ts = System.nanoTime();
      result = tupleQuery.evaluate();
      logQuery(queryString, "get-bus-stops-of-line-3", "any", nomeLinea, System.nanoTime() - ts);
      out.println("\"BusStops\":{\n"
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
            out.println(",");
          }
          out.print("{"
                  + "\"geometry\":{"
                  + "\"type\":\"Point\","
                  + "\"coordinates\":["
                  + valueOfBSLong + ","
                  + valueOfBSLat
                  + "]},"
                  + "\"type\":\"Feature\","
                  + "\"properties\":{"
                  + "\"popupContent\": \"" + valueOfNomeFermata + "\","
                  //+ "\"nome\": \"" + valueOfNomeFermata + "\","
                  + "\"name\": \"" + valueOfNomeFermata + "\","
                  + "\"serviceUri\": \"" + valueOfBS + "\","
                  + "\"tipo\": \"fermata\", "
                  + "\"serviceType\": \"TransferServiceAndRenting_BusStop\""
                  + "}, "
                  + "\"id\": " + Integer.toString(i + 1) + " "
                  + "}");
          i++;
        }
      } catch (Exception e) {
        out.println(e.getMessage());
      }
    }
    out.println("]}}");
  }
  }

  public void queryBusesLastPosition(JspWriter out, RepositoryConnection con) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String queryText = null;
    if(conf.get("busLastPositionFrom", "timetable").equals("timetable")) {
      queryText = "select ?lat ?long ?rName ?stopName (substr(?busid,25) as ?v) ?bsFirst ?bsLast ?prev ?delay {\n" +
              "{select ?t max(?at2) as ?prev {\n" +
              "?x1 a gtfs:StopTime.\n" +
              "?x1 gtfs:arrivalTime ?at1.\n" +
              "?x1 gtfs:trip ?t.\n" +
              "?x2 a gtfs:StopTime.\n" +
              "?x2 gtfs:arrivalTime ?at2.\n" +
              "?x2 gtfs:trip ?t.\n" +
              "?t gtfs:route ?r.\n" +
              "?r gtfs:agency/foaf:name \"Ataf&Linea\".\n" +
              //"?r gtfs:shortName \"11\".\n" +
              "?t gtfs:service/dcterms:date ?d.\n" +
              "filter(xsd:date(?d)=xsd:date(now()) && str(?at1)>str(xsd:time(now())) && str(?at2)<str(xsd:time(now())))\n" +
              "} group by ?t\n" +
              "}\n" +
              "?x a gtfs:StopTime.\n" +
              "?x gtfs:trip ?t.\n" +
              "?x gtfs:arrivalTime ?prev.\n" +
              "?x gtfs:stop ?stop.\n" +
              "?stop geo:lat ?lat.\n" +
              "?stop geo:long ?long.\n" +
              "?stop foaf:name ?stopName.\n" +
              "?t gtfs:route ?r.\n" +
              "?t gtfs:headsign ?bsLast.\n" +
              "?t dcterms:identifier ?busid.\n" +
              "?r gtfs:shortName ?rName.\n" +
              "?fs a gtfs:StopTime.\n" +
              "?fs gtfs:trip ?t.\n" +
              "?fs gtfs:stopSequence \"01\".\n" +
              "?fs gtfs:stop/foaf:name ?bsFirst.\n" +
              "bind(bif:dateDiff('minute',xsd:time(?prev),xsd:time(now())) as ?delay)" +
              "} order by ?v";
    }
    else { //realtime
      queryText = "PREFIX luc: <http://www.ontotext.com/owlim/lucene#>\n"
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
            + "SELECT DISTINCT * WHERE {\n"
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
    //queryText += " LIMIT 15";
    }

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
    long startTime = System.nanoTime();
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(queryText, "AutobusRT", sparqlType, "AutobusRT;20", System.nanoTime() - startTime);
    int i = 0;
    out.println("{ "
            + "\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    String prevBusNumber = "";
    while (result.hasNext()) {
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
        nomeLinea = ServiceMap.escapeJSON(bindingSet.getValue("rName").stringValue());
      }

      String bsFirst = "";
      if (bindingSet.getValue("bsFirst") != null) {
        bsFirst = bindingSet.getValue("bsFirst").stringValue();
      }

      String bsLast = "";
      if (bindingSet.getValue("bsLast") != null) {
        bsLast = ServiceMap.escapeJSON(bindingSet.getValue("bsLast").stringValue());
      }

      String busNumber = "";
      if (bindingSet.getValue("v") != null) {
        busNumber = bindingSet.getValue("v").stringValue();
      }

      String delay = "";
      if (bindingSet.getValue("delay") != null) {
        delay = bindingSet.getValue("delay").stringValue();
      }

      if(!busNumber.equals(prevBusNumber)) {
        if (i != 0) {
          out.println(", ");
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
                + "\"vehicleNum\": \"" + busNumber + "\", "
                + "\"line\": \"" + nomeLinea + "\", "
                + "\"direction\": \"" + bsFirst + " &#10132; " + bsLast + "\", "
                + "\"tipo\": \"RealTimeInfo\", "
                + "\"serviceUri\": \"busCode" + busNumber + "\", "
                + "\"detectionTime\": \"" + delay + "\", "
                + "\"serviceType\": \"bus_real_time\" "
                + "}, "
                + "\"id\": " + Integer.toString(i + 1) + "  "
                + "}\n");
        i++;
      }
      prevBusNumber = busNumber;
    }
    out.println("] "
            + "}");
  }

  public void addCommentToService(String uid, String serviceUri, String serviceName, String comment) throws Exception {
    Configuration conf = Configuration.getInstance();
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("INSERT INTO ServiceComment(uid,serviceUri,serviceName,comment) VALUES (?,?,?,?)");
      st.setString(1, uid);
      st.setString(2, serviceUri);
      st.setString(3, serviceName);
      st.setString(4, comment);

      st.executeUpdate();
      st.close();
      String baseApiUrl = conf.get("baseApiUrl", "");
      ServiceMap.sendEmail(conf.get("validationEmail", "pierfrancesco.bellini@unifi.it"), "KM4CITY new comment for " + serviceName,
              "new comment added for:\n\n"
              + serviceName + "\n"
              + baseApiUrl + "v1/?format=html&serviceUri=" + serviceUri + "\n\n"
              + "uid: "+uid+"\n"
              + "comment:\n"
              + comment + "\n\n"
              + "Validate it on " + baseApiUrl.replace("/api/", "/") + "comments.jsp");
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    finally {
      connection.close();      
    }
  }

  public void setStarsToService(String uid, String serviceUri, int stars) throws Exception {
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("REPLACE INTO ServiceStars(uid,serviceUri,stars) VALUES (?,?,?)");
      st.setString(1, uid);
      st.setString(2, serviceUri);
      st.setInt(3, stars);

      st.executeUpdate();
      st.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    finally {
      connection.close();      
    }
  }
  
  public void queryLastContribution(JspWriter out, int n, String lang) throws Exception {
    String baseApiUrl = Configuration.getInstance().get("baseApiUrl", "");
    Connection connection = ConnectionPool.getConnection();
    out.println("{\n"
            + "\"LastPhotos\": [");
    try {
      PreparedStatement st = connection.prepareStatement("SELECT serviceUri,serviceName,file,timestamp FROM ServicePhoto WHERE status='validated' ORDER BY id DESC LIMIT "+n);
      ResultSet r = st.executeQuery();
      while(r.next()) {
        String uri = r.getString("serviceUri");
        String name = r.getString("serviceName");
        String timestamp = r.getString("timestamp");
        String file = r.getString("file");
        if(!r.isFirst())
          out.println(",");
        out.print("  {\"serviceUri\":\""+uri+"\", "+ServiceMap.map2Json(ServiceMap.getServiceInfo(uri,lang))+" \"photo\":\""+baseApiUrl+"v1/photo/"+file+"\", \"photoThumb\":\""+baseApiUrl+"v1/photo/thumbs/"+file+"\", \"photoOrig\":\""+baseApiUrl+"v1/photo/originals/"+file+"\",\"timestamp\":\""+timestamp+"\"}");
      }
      st.close();
      out.println("],\n"
              + "\"LastComments\":[");
      st = connection.prepareStatement("SELECT serviceUri,serviceName,comment,timestamp FROM ServiceComment WHERE status='validated' ORDER BY id DESC LIMIT "+n);
      r = st.executeQuery();
      while(r.next()) {
        String uri = r.getString("serviceUri");
        String name = r.getString("serviceName");
        String comment = r.getString("comment");
        String timestamp = r.getString("timestamp");
        if(!r.isFirst())
          out.println(",");
        out.print("  {\"serviceUri\":\""+uri+"\", "+ServiceMap.map2Json(ServiceMap.getServiceInfo(uri,lang))+" \"comment\":\""+escapeJSON(comment)+"\", \"timestamp\":\""+timestamp+"\"}");
      }
      st.close();
      out.println("],\n"
              + "\"LastStars\":[");
      st = connection.prepareStatement("SELECT serviceUri,stars,timestamp FROM ServiceStars ORDER BY timestamp DESC LIMIT "+n);
      r = st.executeQuery();
      while(r.next()) {
        String uri = r.getString("serviceUri");
        String stars = r.getString("stars");
        String timestamp = r.getString("timestamp");
        String name = ServiceMap.getServiceName(uri);
        if(name==null)
          name = uri.substring(uri.lastIndexOf("/")+1);
        if(!r.isFirst())
          out.println(",");
        out.print("  {\"serviceUri\":\""+uri+"\", "+ServiceMap.map2Json(ServiceMap.getServiceInfo(uri,lang))+" \"stars\":"+stars+", \"timestamp\":\""+timestamp+"\"}");
      }
      st.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    finally {
      connection.close();      
    }
    out.println("]}");
  }

  public TupleQueryResult queryBusLines(String busStop, RepositoryConnection con){
      String queryForLine = "";
      if(busStop.startsWith("http://"))
       queryForLine = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>"
              + "select distinct ?id ?line ?desc ?ag ?agname where {\n"
              + "{?st gtfs:stop [owl:sameAs <"+busStop+">]\n"
              + "} UNION {\n"
              + "?st gtfs:stop <"+busStop+">\n"
              + "}\n"
              + "?st gtfs:trip ?trip.\n"
              + "?trip gtfs:route ?line.\n"
              + "?line gtfs:longName ?desc.\n"
              + "OPTIONAL {?line gtfs:shortName ?id}.\n"
              + "?line gtfs:agency ?ag.\n"
              + "?ag foaf:name ?agname.\n"
              + "} ORDER BY ?id ";
      else
       queryForLine = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>"
              + "select distinct ?id where {\n"
              + "?st gtfs:stop [ foaf:name \""+busStop+"\" ].\n"
              + "?st gtfs:trip ?trip.\n"
              + "?line gtfs:longName ?desc.\n"
              + "OPTIONAL {?line gtfs:shortName ?id}.\n"
              + "} ORDER BY ?id ";

      TupleQuery tupleQueryForLine;
      try {
          tupleQueryForLine = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForLine);
          System.out.println(queryForLine);
          long ts = System.nanoTime();
          TupleQueryResult result = tupleQueryForLine.evaluate();
          ServiceMap.logQuery(queryForLine,"API-buslines","",busStop,System.nanoTime()-ts);
          return result;
      } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
          Logger.getLogger(ServiceMapApi.class.getName()).log(Level.SEVERE, null, ex);
      }
  return null;    
  }
  
  public void queryTplAgencyList(JspWriter out, RepositoryConnection con) throws Exception {
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    final String km4cVersion = conf.get("km4cVersion", "new");

    String queryForAgencies = "SELECT DISTINCT ?ag ?name WHERE {"
            + " ?ag a gtfs:Agency."
            + " ?ag foaf:name ?name."
            + "} ORDER BY ?name";

    TupleQuery tupleQuery;
    tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForAgencies);
    long ts = System.nanoTime();
    TupleQueryResult result = tupleQuery.evaluate();
    ServiceMap.logQuery(queryForAgencies, "API-all-agencies", "", "", System.nanoTime() - ts);
    out.println("{\"Agencies\": [");
    int n =0;
    while (result.hasNext()) {
      BindingSet bindingSet = result.next();
      String valueOfAg = JSONObject.escape(bindingSet.getValue("ag").stringValue());
      String valueOfName = JSONObject.escape(bindingSet.getValue("name").stringValue());

      if(n!=0)
        out.println(",");
      out.print("{\n"
              + " \"agency\":\""+valueOfAg+"\",\n"
              + " \"name\":\""+valueOfName+"\"\n"
              + "}");
      n++;
    }
    out.println("]}");
  }
}
