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

package org.disit.servicemap.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.util.EntityUtils;
import org.apache.phoenix.exception.PhoenixIOException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.disit.servicemap.ConnectionPool;
import org.disit.servicemap.ServiceMapping;
import org.disit.servicemap.ServiceValuesServlet;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.AggregatorFactory;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortOrder;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.openrdf.query.Binding;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

/**
 *
 * @author bellini
 */
public class ServiceMapApiV1 extends ServiceMapApi {

  public void queryBusStop(JspWriter out, RepositoryConnection con, String idService, String lang, String realtime, String uid, String toTime) throws Exception {
    queryTplStop(out, con, idService, "BusStop", lang, realtime, uid, toTime);
  }
  
  public void queryTplStop(JspWriter out, RepositoryConnection con, String idService, String tplclass, String lang, String realtime, String uid, String toTime) throws Exception {
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
    if(CheckParameters.checkUri(idService)!=null)
      throw new IllegalArgumentException("invalid URI");
    
    idService = ServiceMap.urlEncode(idService);
    String queryStringStop = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
            + "PREFIX schema:<http://schema.org/#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n"
            + "SELECT distinct ?nomeFermata ?bslat ?bslong ?address ?ag ?agname ?code WHERE {\n"
            + "	<" + idService + "> rdf:type km4c:"+tplclass+"."
            + "	<" + idService + "> foaf:name ?nomeFermata."
            + " OPTIONAL {<" + idService + "> km4c:isInRoad ?road."
            + "     ?road km4c:extendName ?address}."
            + "	<" + idService + "> geo:lat ?bslat."
            + "	<" + idService + "> geo:long ?bslong."
            + " OPTIONAL {"
            //+ "  {?st gtfs:stop <"+idService+">.}UNION{?st gtfs:stop [owl:sameAs <"+idService+">]}" 
            //+ "  ?st gtfs:trip ?t."
            //+"   ?t gtfs:route/gtfs:agency ?ag."
            + "  <"+ idService + "> gtfs:agency ?ag.\n"
            + "  <"+ idService + "> gtfs:code ?code.\n"
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

    String valueOfCode = "";
    String valueOfAgName = "";
    
    try {
      while (busStopResult.hasNext()) {
        BindingSet bindingSetBusStop = busStopResult.next();

        String valueOfBSLat = bindingSetBusStop.getValue("bslat").stringValue();
        String valueOfBSLong = bindingSetBusStop.getValue("bslong").stringValue();
        String valueOfRoad = "";
        String valueOfAgUri = "";
        if (bindingSetBusStop.getValue("address") != null) {
          valueOfRoad = bindingSetBusStop.getValue("address").stringValue();
        }
        nomeFermata = bindingSetBusStop.getValue("nomeFermata").stringValue();
        if(bindingSetBusStop.getValue("agname") != null)
          valueOfAgName = bindingSetBusStop.getValue("agname").stringValue();
        if(bindingSetBusStop.getValue("ag") != null)
          valueOfAgUri = bindingSetBusStop.getValue("ag").stringValue();
        if(bindingSetBusStop.getValue("code") != null)
          valueOfCode = bindingSetBusStop.getValue("code").stringValue();
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
                + "    \"code\": \"" + valueOfCode + "\", "
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
    out.println(","+queryTplTimeTable(con, idService, valueOfAgName, valueOfCode, toTime));
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
  
  public String queryTplTimeTable(RepositoryConnection con, String stopUri, String agencyName, String code, String toTime) throws Exception {
    Map<String,String> rtPassages = ServiceMap.getTplRealPassageTimes(agencyName, code);
    Configuration conf=Configuration.getInstance();
    
    if(CheckParameters.checkUri(stopUri)!=null)
      throw new IllegalArgumentException("invalid URI "+stopUri);
    
    String s = "\"timetable\":";
    
    Date now;
    if(toTime==null)
      now = new Date();
    else {
      DateFormat df = new SimpleDateFormat(ServiceMap.dateFormatT);
      now = df.parse(toTime);
    }
    
    String timeZoneId = "Europe/Rome";
    JsonObject agency = getTplAgencyOfStop(con, stopUri);
    if(agency!=null && agency.has("timeZone"))
      timeZoneId = agency.get("timeZone").getAsString();
    
    TimeZone tz = TimeZone.getTimeZone(timeZoneId);
    DateFormat dateFormatterDate = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat dateFormatterTime = new SimpleDateFormat("HH:mm:ss");
    dateFormatterDate.setTimeZone(tz);
    dateFormatterTime.setTimeZone(tz);

    String nowDate = dateFormatterDate.format(now);
    String nowTime = dateFormatterTime.format(now);
    if(agencyName==null || agencyName.isEmpty()) {
      agencyName = "none";
      if(agency!=null && agency.has("agencyName"))
        agencyName = agency.get("agencyName").getAsString();
    }
    
    ServiceMap.println(agencyName + " now@"+timeZoneId+": "+nowDate+" "+nowTime);
    Map<String,String> gtfsrtPassages = null;
    if(conf.get("gtfsRealtime","Helsingin seudun liikenne").contains(agencyName)) {
      Connection rtcon = ServiceMap.getRTConnection2();
      if(rtcon==null) {
        throw new Exception("missing hbase phoenix connection");
      }
      //find passage
      gtfsrtPassages = new HashMap<>();
      Statement stmt = rtcon.createStatement();
      stmt.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
      String gtfsrtTable = conf.get("gtfsRealTimeTable", "gtfsrTripupdate_hel");
      long ts = System.currentTimeMillis();
      ResultSet rs = stmt.executeQuery("SELECT tripUri, convert_tz(arrivalTime,'UTC','"+timeZoneId+"') FROM " + gtfsrtTable + " WHERE stopUri='" + stopUri + "' AND arrivalTime>=NOW()");
      while(rs.next()) {
        String gtfsTripUri = rs.getString(1);
        String gtfsRtArrivalTime = rs.getString(2);
        ServiceMap.println(stopUri+" "+gtfsTripUri+" --> "+gtfsRtArrivalTime);
        gtfsrtPassages.put(gtfsTripUri, gtfsRtArrivalTime);
      }
      ServiceMap.performance("GTFSRT checked stop "+stopUri+" time:"+(System.currentTimeMillis()-ts)+"ms");
      stmt.close();
      rtcon.close();
    }
    Map<String,String> gtfsrtAlerts = null;
    if(conf.get("gtfsAlerts","Helsingin seudun liikenne").contains(agencyName)) {
      long ts = System.currentTimeMillis();
      Connection rtcon = ServiceMap.getRTConnection2();
      //find active alerts
      gtfsrtAlerts = new HashMap<>();
      Statement stmt = rtcon.createStatement();
      stmt.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
      String table = conf.get("gtfsAlertsTable", "Bus_hsl_yaml_RT_temp");
      ResultSet rs = stmt.executeQuery("SELECT tripuri,effect,descren FROM " + table + " where alertstart<=NOW() AND ALERTEND>=NOW()");
      int na=0;
      while(rs.next()) {
        String tripUri = rs.getString(1);
        String gtfsEffect = rs.getString(2);
        String gtfsDescrEn = rs.getString(3);
        gtfsrtAlerts.put(tripUri, "{\"effect\":\""+gtfsEffect+"\",\"description\":\""+gtfsDescrEn+"\"}");
        na++;
      }
      stmt.close();
      rtcon.close();
      ServiceMap.performance("GTFSALERT nalert:"+na+" time:"+(System.currentTimeMillis()-ts)+"ms");
    }

    String queryString = "prefix dcterms:<http://purl.org/dc/terms/>\n" +
        "prefix gtfs:<http://vocab.gtfs.org/terms#>\n" +
        "select distinct ?at ?dt ?routeName ?lineName ?lineDesc ?trip ?d where {\n" +
        "{\n" +
        "?st gtfs:stop [owl:sameAs <"+ServiceMap.urlEncode(stopUri)+">]\n" +
        "} UNION {\n" +
        "?st gtfs:stop <"+ServiceMap.urlEncode(stopUri)+">\n" +
        "}\n" +
        "?st gtfs:trip ?trip.\n" +
        "?trip gtfs:service ?sd.\n" +
        "?sd dcterms:date ?d.\n" +
        "filter((xsd:date(?d)=xsd:date(\""+nowDate+"\") && str(?at)>str(xsd:time(\""+nowTime+"\")))||(xsd:date(?d)=bif:dateadd(\"day\",1,xsd:date(\""+nowDate+"\")) && str(?at)<str(xsd:time(\""+nowTime+"\"))))\n" +
        "optional{?trip gtfs:headsign ?routeName.}\n" +
        "?st gtfs:arrivalTime ?at.\n" +
        "optional{?st gtfs:departureTime ?dt.}\n" +
        "?trip gtfs:route ?route.\n" +
        "optional{?route gtfs:shortName ?lineName.}\n" +
        "?route gtfs:longName ?lineDesc.\n" +
        "} \n" +
        "order by ?d ?at ";

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    long ts = System.nanoTime();
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(queryString, "API-busstop-timetable", "", stopUri, System.nanoTime() - ts);
    ServiceMap.println(queryString);
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
        String valueOfDepartureTime = valueOfArrivalTime;
        if(bindingSet.getValue("dt")!=null)
          valueOfDepartureTime = bindingSet.getValue("dt").stringValue();
        String valueOfRouteName = "";
        if(bindingSet.getValue("routeName")!=null)
          valueOfRouteName = JSONObject.escape(bindingSet.getValue("routeName").stringValue());
        String valueOfLineName = "";
        if(bindingSet.getValue("lineName")!=null)
          valueOfLineName = JSONObject.escape(bindingSet.getValue("lineName").stringValue());
        String valueOfLineDesc = JSONObject.escape(bindingSet.getValue("lineDesc").stringValue());
        String valueOfTrip = bindingSet.getValue("trip").stringValue();
        String valueOfDate = bindingSet.getValue("d").stringValue();
        
        if(!valueOfDepartureTime.contains(":")) {
          valueOfDepartureTime = valueOfArrivalTime;
        }

        String[] dta = fixTPLDateTime(valueOfDate, valueOfArrivalTime);
        String[] dtd = fixTPLDateTime(valueOfDate, valueOfDepartureTime);
        if(dta!=null) {
          valueOfDate = dta[0];
          valueOfArrivalTime = dta[1];
        }
        if(dtd!=null) {
          valueOfDepartureTime = dtd[1];
        }
        if(rtPassages!=null) {
          //TODO modificare arrivaltime sulla base dei passaggi real time
          String realTime = rtPassages.get(valueOfLineName+"/"+valueOfDate+" "+valueOfArrivalTime);
          if(realTime!=null)
            valueOfArrivalTime = realTime;
        }
        String plannedDate = null;
        String plannedArrivalTime = null;
        String plannedDepartureTime = null;
        if(gtfsrtPassages!=null) {
          String realArrivalTime = gtfsrtPassages.get(valueOfTrip);
          if(realArrivalTime!=null) {
            String[] at = realArrivalTime.split(" ");
            if(at.length == 2) {
              ServiceMap.println("GTFSRT stop "+stopUri+" "+valueOfTrip+" --> "+realArrivalTime+"/"+valueOfDate+" "+valueOfArrivalTime);
              plannedDate = valueOfDate;
              plannedArrivalTime = valueOfArrivalTime;
              valueOfDate = at[0];
              valueOfArrivalTime = at[1].replace(".000", "");
              if(valueOfDepartureTime.equals(plannedArrivalTime)) {
                plannedDepartureTime = plannedArrivalTime;
                valueOfDepartureTime = valueOfArrivalTime;
              }
            }
          }
        }
        String alert = null;
        if(gtfsrtAlerts!=null) {
          alert = gtfsrtAlerts.get(valueOfTrip);
          if(alert==null)
            alert = "{}";
        }
        //ServiceMap.println("--- "+valueOfArrivalTime+" "+now);
        //per bug virtuoso verifica che effetivamente l'ora sia successiva alla data attuale
        //ServiceMap.println("date: "+valueOfDate+" today:"+today);
        //if(today.equals("0") || valueOfArrivalTime.compareTo(now)>0) {
          if(i>0)
            s += ",";
          s += "{"
                  + "\"date\":{"
                  + "\"type\":\"literal\","
                  + "\"value\":\"" + valueOfDate + "\""
                  + (plannedDate!=null ? ",\"planned\":\""+plannedDate+"\"":"")
                  + "},"
                  + "\"arrivalTime\":{"
                  + "\"type\":\"literal\","
                  + "\"value\":\"" + valueOfArrivalTime + "\""
                  + (plannedArrivalTime!=null ? ",\"planned\": \""+plannedArrivalTime+"\"":"")
                  + "},"
                  + "\"departureTime\":{"
                  + "\"type\":\"literal\","
                  + "\"value\":\"" + valueOfDepartureTime + "\""
                  + (plannedDepartureTime!=null ? ",\"planned\": \""+plannedDepartureTime+"\"":"")
                  + "},"
                  + "\"lineName\":{ "
                  + "\"type\":\"literal\","
                  + "\"value\":\"" + valueOfLineName + "\" "
                  + "},"
                  + "\"lineDesc\":{"
                  + "\"type\":\"literal\","
                  + "\"value\":\"" + valueOfLineDesc + "\" "
                  + "},"
                  + "\"routeName\":{"
                  + "\"type\":\"literal\","
                  + "\"value\":\"" + valueOfRouteName + "\" "
                  + "},"
                  + "\"trip\":{"
                  + "\"type\":\"uri\","
                  + "\"value\":\"" + valueOfTrip + "\" "
                  + "}"
                  + (alert!=null ? ",\"alert\":"+alert+"" : "")
                  + "}";
          i++;
        //}
      }
      s+="]}}";
      if(i>0)
        return s;
    } catch (Exception e) {
      ServiceMap.notifyException(e);
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
  
  public JsonObject getTplAgencyOfStop(RepositoryConnection con, String stopUri) throws Exception {
    if(CheckParameters.checkUri(stopUri)!=null)
      throw new IllegalArgumentException("invalid URI "+stopUri);

    String queryString = "prefix dcterms:<http://purl.org/dc/terms/>\n" +
        "prefix gtfs:<http://vocab.gtfs.org/terms#>\n" +
        "select distinct ?agencyUri ?agencyName ?timeZone ?fareUrl where {\n" +
        " <"+ServiceMap.urlEncode(stopUri)+"> gtfs:agency ?agencyUri.\n" +
        " ?agencyUri foaf:name ?agencyName. \n"+
        " optional { ?agencyUri gtfs:timeZone ?timeZone. }\n"+
        " optional { ?agencyUri gtfs:fareUrl ?fareUrl.} \n" +
        "}";

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    long ts = System.nanoTime();
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(queryString, "API-busstop-agency", "", stopUri, System.nanoTime() - ts);
    ServiceMap.println(queryString);
    if (result.hasNext()) {
      BindingSet bindingSet = result.next();

      JsonObject a=new JsonObject();
      for(Binding b : bindingSet) {
        a.addProperty(b.getName(),b.getValue().stringValue());
      }
      return a;
    }
    return null;
  }
  
  public int queryBusRoutes(JspWriter out, RepositoryConnection con, String agency, String line, String stopName, boolean getGeometry) {
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
        if(CheckParameters.checkUri(stopName)!=null)
          throw new IllegalArgumentException("invalid URI "+stopName);
        stopFilter = "{?stx gtfs:stop <"+ServiceMap.urlEncode(stopName)+">}UNION{?stx gtfs:stop [ owl:sameAs <"+ServiceMap.urlEncode(stopName)+">]}.\n";
      } else {
        stopFilter = "?stx gtfs:stop/foaf:name \""+ServiceMap.stringEncode(stopName)+"\".\n";
      }
      stopFilter += "?stx gtfs:trip ?trip.";
    }
    String lineFilter = "";
    if(line != null) {
      if(line.startsWith("http://")) {
        if(CheckParameters.checkUri(line)!=null)
          throw new IllegalArgumentException("invalid URI "+line);
        lineFilter = "?trip gtfs:route <"+ServiceMap.urlEncode(line)+">.\n";
      } else {
        lineFilter =
              "?ln gtfs:shortName \""+ServiceMap.stringEncode(line)+"\".\n" +
              "?trip gtfs:route ?ln.\n";
        if(agency==null)
          agency = "Ataf&Linea";
        if(agency.startsWith("http://")) {
          if(CheckParameters.checkUri(agency)!=null)
            throw new IllegalArgumentException("invalid URI "+agency);
          lineFilter += "?ln gtfs:agency <"+ServiceMap.urlEncode(agency)+">.";
        } else
          lineFilter += "?ln gtfs:agency/foaf:name \""+ServiceMap.stringEncode(agency)+"\".";
      }
    } else if(stopName==null) {
        lineFilter =
              "?trip gtfs:route ?ln.\n";
        if(agency==null)
          agency = "Ataf&Linea";
        if(agency.startsWith("http://")) {
          if(CheckParameters.checkUri(agency)!=null)
            throw new IllegalArgumentException("invalid URI");
          lineFilter += "?ln gtfs:agency <"+ServiceMap.urlEncode(agency)+">.";
        } else
          lineFilter += "?ln gtfs:agency/foaf:name \""+ServiceMap.stringEncode(agency)+"\".";
    }
              
    String queryForLine = 
            "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n" +
            "PREFIX dcterms:<http://purl.org/dc/terms/>\n" +
            "PREFIX ogis:<http://www.opengis.net/ont/geosparql#>\n" +
            "SELECT DISTINCT ?line ?shape ?bss ?bse (min(?trip) as ?routeId) ?polyline {{\n" +
            "SELECT ?line ?trip (MAX(?st) as ?mx) (MIN(?st) as ?mn) {\n" +
            stopFilter +
            "?st gtfs:trip ?trip.\n" +
            //"?trip gtfs:service/dcterms:date ?d.\n" +
            //"filter(xsd:date(?d)=xsd:date(now()))\n" +
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
      //ServiceMap.println(queryForLine);
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
                + " \"line\":\""+JSONObject.escape(valueOfLine)+"\",\n"
                + " \"route\":\""+valueOfRouteId+"\",\n"
                + " \"routeName\":\""+JSONObject.escape(valueOfRouteName)+"\",\n"
                + (valueOfPolyline!=null ? " \"wktGeometry\":\""+ServiceMap.fixWKT(valueOfPolyline)+"\",\n" : "")
                + " \"firstBusStop\":\""+JSONObject.escape(valueOfBss)+"\",\n"
                + " \"lastBusStop\":\""+JSONObject.escape(valueOfBse)+"\"\n"
                + "}");
        n++;
      }
      out.println("]}");
      return n;
    } catch (RepositoryException | MalformedQueryException | QueryEvaluationException | IOException ex) {
      Logger.getLogger(ServiceMapApi.class.getName()).log(Level.SEVERE, null, ex);
    }
    return 0;
  }

public int queryAllBusLines(JspWriter out, RepositoryConnection con, String agency) {
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    final String km4cVersion = conf.get("km4cVersion", "new");

    if(CheckParameters.checkUri(agency)!=null)
      throw new IllegalArgumentException("invalid URI "+agency);
    
    String queryForLines;
    queryForLines = "SELECT DISTINCT ?sname ?lname ?r WHERE {\n"
              + " ?r rdf:type gtfs:Route.\n"
              + " ?r gtfs:agency <"+ServiceMap.urlEncode(agency)+">.\n"
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
      return n;
    } catch (RepositoryException | MalformedQueryException | QueryEvaluationException | IOException ex) {
      Logger.getLogger(ServiceMapApi.class.getName()).log(Level.SEVERE, null, ex);
    }
    return 0;
  }

  public int queryLatLngServices(JspWriter out, RepositoryConnection con, final String[] coords, String categorie, final String textToSearch, final String raggioBus, String raggioSensori, final String raggioServizi, String risultatiBus, String risultatiSensori, String risultatiServizi, final String language, String cat_servizi, final boolean getGeometry, final boolean inside, boolean photos, final String makeFullCount, final String value_type, final String graphUri, String valueName, final String apiKey, String model) throws Exception {
    ServiceMap.println("API-LatLngServices START");
    long tss = System.currentTimeMillis();
    
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    final String km4cVersion = conf.get("km4cVersion", "new");
    final int MAX_RESULTS = Integer.parseInt(conf.get("maxResults", "50000"));
    
    String fromTime = null;
    if(valueName!=null && conf.get("fromTimeLimitDays", null) != null) {
      DateFormat dateFormatterT = new SimpleDateFormat(ServiceMap.dateFormatT);
      Date fTime = new Date(new Date().getTime() - Integer.parseInt(conf.get("fromTimeLimitDays", "1"))*24*60*60*1000);
      
      fromTime = dateFormatterT.format(fTime);
    }
    
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
    int percBus=3;
    int percSensor = 2;
    if(searchBusStop && !searchSensorSite && !searchServices) {
      percBus = 10;
    } else if(!searchBusStop && searchSensorSite && !searchServices) {
      percSensor = 10;
    } else if(searchBusStop && searchSensorSite && !searchServices) {
      percBus = 8;
      percSensor = 2;
    }
    String fcc = "";
    try {
      fcc = ServiceMap.filterServices(listaCategorieServizi);
    } catch (Exception e) {
    }
    final String fc = fcc;
    int i = 0;
    int numeroBus = 0;
    int numeroSensori = 0;
    int numeroServizi = 0;
    
    int resBusStop = 0;
    int resSensori = 0;
    int resServizi = 0;
    String args = "";
    for (String c : coords) {
      args += c + ";";
    }
    args += categorie + ";" + textToSearch + ";" + raggioBus;
    out.println("{");
    try {
      if (searchBusStop && !inside) {
        String type;
        if (lang.equals("it")) {
          type = "Fermata";
        } else {
          type = "BusStop";
        }
        int fullCount = -1;
        final String limitNearBusStop;
        if (!risultatiServizi.equals("0")) {
          if (cat_servizi.equals("categorie")) {
            resBusStop = ((Integer.parseInt(risultatiServizi)) * percBus / 10);
            limitNearBusStop = " LIMIT " + resBusStop;
          } else {
            if (cat_servizi.contains(":")) {
              String parts[] = cat_servizi.split(":");
              if (!parts[1].equals("0")) {
                resBusStop = ((Integer.parseInt(parts[1])) * percBus / 10);
                limitNearBusStop = " LIMIT " + resBusStop;
              } else {
                limitNearBusStop = "";
              }
            } else {
              resBusStop = Integer.parseInt(risultatiServizi);
              limitNearBusStop = " LIMIT " + risultatiServizi;
            }
          }
        } else {
          resBusStop = ( MAX_RESULTS * percBus) / 10;
          limitNearBusStop = "";
        }
        SparqlQuery queryNearBusStop = new SparqlQuery() {
          @Override
          public String query(String type, String geoMode) throws Exception {
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
                    + "SELECT DISTINCT ?bs (STR(?nome) AS ?nomeFermata) ?bslat ?bslong ?ag ?agname ?sameas ?dist ?x WHERE {\n"
                    + " ?bs geo:lat ?bslat.\n"
                    + " ?bs geo:long ?bslong.\n"
                    + ServiceMap.modelSearchQueryFragment("?bs", model)
                    + ServiceMap.graphSearchQueryFragment("?bs", graphUri)
                    + ServiceMap.geoSearchQueryFragment("?bs", coords, raggioBus, geoMode)
                    + " ?bs rdf:type km4c:BusStop.\n"
                    + " ?bs foaf:name ?nome.\n"
                    + ServiceMap.textSearchQueryFragment("?bs", "foaf:name", textToSearch)
                    + ServiceMap.valueTypeSearchQueryFragment("?bs", value_type)
                    + (km4cVersion.equals("old") ? " FILTER ( datatype(?bslat ) = xsd:float )\n"
                            + " FILTER ( datatype(?bslong ) = xsd:float )\n" : "")
                    //+ " ?st gtfs:stop ?bs.\n"
                    //+ " ?st gtfs:trip/gtfs:route/gtfs:agency ?ag.\n"
                    + " ?bs gtfs:agency ?ag.\n"
                    + " ?ag foaf:name ?agname.\n"
                    + " OPTIONAL {?bs owl:sameAs ?sameas }"
                    //+ " FILTER NOT EXISTS {?bs owl:sameAs ?bb }\n" //eliminate duplicate
                    + "}"
                    + (type != null ? "}" : " ORDER BY ?dist"+limitNearBusStop);
          }
        };
        if("true".equals(makeFullCount))
          fullCount = ServiceMap.countQuery(con, queryNearBusStop);

        TupleQueryResult resultNearBS = ServiceMap.geoSparqlQuery(con, queryNearBusStop, null,"API-nearbusstop", args);
        out.println("\"BusStops\": {");
        if (fullCount >= 0) {
          out.println("\"fullCount\": " + fullCount + ",");
        }
        out.println("\"type\": \"FeatureCollection\",\n"
                + "\"features\":[");
        int s = 0;
        while (resultNearBS.hasNext() && s < resBusStop) {
          BindingSet bindingSetNearBS = resultNearBS.next();
          String valueOfBS = bindingSetNearBS.getValue("bs").stringValue();
          String valueOfNomeFermata = JSONObject.escape(bindingSetNearBS.getValue("nomeFermata").stringValue());
          String valueOfBSLat = bindingSetNearBS.getValue("bslat").stringValue();
          String valueOfBSLong = bindingSetNearBS.getValue("bslong").stringValue();
          String valueOfDist = bindingSetNearBS.getValue("dist").stringValue();      
          String valueOfAgUri = bindingSetNearBS.getValue("ag").stringValue();
          String valueOfAgName = bindingSetNearBS.getValue("agname").stringValue();
          String uriSameAs = null;
          if(bindingSetNearBS.getValue("sameas")!=null)
            uriSameAs = bindingSetNearBS.getValue("sameas").stringValue();

          String valueOfLinee = "";
          if(fullCount<100 || !risultatiServizi.equals("0")) {
            TupleQueryResult resultLinee = queryBusLines(valueOfBS, con);
            if (resultLinee != null) {
              while (resultLinee.hasNext()) {
                BindingSet bindingSetLinee = resultLinee.next();
                if(bindingSetLinee.getValue("id")!=null) {
                  String idLinee = bindingSetLinee.getValue("id").stringValue();
                  valueOfLinee = valueOfLinee + " - " + idLinee;
                }
              }
            }
          }

          if (!valueOfLinee.equals("")) {
            valueOfLinee = JSONObject.escape(valueOfLinee.substring(3));
          }

          if (s != 0) {
            out.println(", ");
          }

          out.print("{\n"
                  + "\"geometry\":{\"type\": \"Point\",\"coordinates\":[" + valueOfBSLong + ", " + valueOfBSLat + "]},\n"
                  + "\"type\": \"Feature\",\n"
                  + "\"properties\": {\n"
                  + "\"name\": \"" + valueOfNomeFermata + "\",\n"
                  + "\"typeLabel\": \"" + type + "\",\n"
                  + "\"tipo\": \"fermata\",\n"
                  + "\"serviceType\": \"TransferServiceAndRenting_BusStop\",\n"
                  + "\"busLines\": \"" + valueOfLinee + "\",\n"
                  + "\"serviceUri\": \"" + valueOfBS + "\",\n"
                  + "\"distance\": \"" + valueOfDist + "\",\n"
                  + "\"agency\": \"" + valueOfAgName + "\",\n"
                  + "\"agencyUri\": \"" + valueOfAgUri + "\",\n"
                  + "\"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfBS,"thumbs") + "\n"
                  + "},\n"
                  + "\"id\": " + Integer.toString(s + 1) + "\n"
                  + "}");
          s++;
          numeroBus++;
        }
        out.println("]}");
      }

      if (searchSensorSite && !inside) {
        String type;
        if (lang.equals("it")) {
          type = "Sensore";
        } else {
          type = "Sensor";
        }

        int fullCount = -1;
        final String limitNearSensori;
        if (!risultatiServizi.equals("0")) {
          if (cat_servizi.equals("categorie")) {
            resSensori = ((Integer.parseInt(risultatiServizi)) * percSensor / 10);
            limitNearSensori = " LIMIT " + resSensori;
          } else {
            if (cat_servizi.contains(":")) {
              String parts[] = cat_servizi.split(":");
              if (!parts[1].equals("0")) {
                resSensori = ((Integer.parseInt(parts[1])) * percSensor / 10);
                limitNearSensori = " LIMIT " + resSensori;
              } else {
                limitNearSensori = "";
              }
            } else {
              resSensori = Integer.parseInt(risultatiServizi);
              limitNearSensori = " LIMIT " + risultatiServizi;
            }
          }
        } else {
          resSensori = (MAX_RESULTS * percSensor / 10);
          limitNearSensori = "";
        }
        
        SparqlQuery queryNearSensors = new SparqlQuery() {
          @Override
          public String query(String type, String geoMode) throws Exception {
            return "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                    + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                    + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX schema:<http://schema.org/>\n"
                    + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
                    + ("count".equals(type) ? "SELECT (COUNT(*) AS ?count) WHERE {\n" : "")
                    + "SELECT DISTINCT ?sensor ?idSensore ?name ?lat ?long ?address ?dist ?x \n"
                    + ServiceMap.graphAccessQueryFragment2(apiKey)
                    + "WHERE {\n"
                    + " ?sensor rdf:type km4c:SensorSite.\n"
                    + " ?sensor geo:lat ?lat.\n"
                    + " ?sensor geo:long ?long.\n"
                    + " OPTIONAL {?sensor dcterms:identifier ?idSensore.}\n"
                    + " OPTIONAL {?sensor schema:name ?name.}\n"
                    + ServiceMap.textSearchQueryFragment("?sensor", "?p", textToSearch)
                    + ServiceMap.geoSearchQueryFragment("?sensor", coords, raggioBus, geoMode)
                    + ServiceMap.valueTypeSearchQueryFragment("?sensor", value_type)
                    + ServiceMap.graphSearchQueryFragment("?sensor", graphUri)
                    + ServiceMap.modelSearchQueryFragment("?sensor", model)
                    + ServiceMap.graphAccessQueryFragment("?sensor", apiKey)
                    + "} "
                    + (type != null ? "}" : "ORDER BY ?dist" + limitNearSensori);
          }
        };

        if("true".equals(makeFullCount))
          fullCount = ServiceMap.countQuery(con, queryNearSensors);

        TupleQueryResult resultNearSensori = ServiceMap.geoSparqlQuery(con, queryNearSensors, null, "API-nearsensori", args);

        if (!searchBusStop) {
          out.println("\"SensorSites\": {");
        } else {
          out.println(",\"SensorSites\": {");
        }
        if (fullCount >= 0) {
          out.println("\"fullCount\": " + fullCount + ",");
        }
        out.println("\"type\": \"FeatureCollection\",\n"
              + "\"features\": [");

        while (resultNearSensori.hasNext() && numeroSensori < resSensori) {
          BindingSet bindingSetNearSensori = resultNearSensori.next();
          String valueOfId = "unknown";
          if(bindingSetNearSensori.getValue("idSensore")!=null)
            valueOfId = bindingSetNearSensori.getValue("idSensore").stringValue();
          String valueOfName = valueOfId;
          if(bindingSetNearSensori.getValue("name")!=null)
            valueOfName = bindingSetNearSensori.getValue("name").stringValue();
          String valueOfIdService = bindingSetNearSensori.getValue("sensor").stringValue();
          String valueOfLat = bindingSetNearSensori.getValue("lat").stringValue();
          String valueOfLong = bindingSetNearSensori.getValue("long").stringValue();
          String valueOfDist = bindingSetNearSensori.getValue("dist").stringValue();      

          if (i != 0) {
            out.println(",");
          }
          out.print("{\n"
                  + "\"geometry\":{\"type\": \"Point\",\"coordinates\":[" + valueOfLong + "," + valueOfLat + "]},\n"
                  + "\"type\": \"Feature\",\n"
                  + "\"properties\": {\n"
                  + "\"name\": \"" + valueOfName + "\",\n"
                  + "\"tipo\": \"sensore\",\n"
                  + "\"typeLabel\": \"" + type + "\",\n"
                  + "\"serviceType\": \"TransferServiceAndRenting_SensorSite\",\n"
                  + "\"serviceUri\": \"" + valueOfIdService + "\",\n"
                  + "\"distance\": \"" + valueOfDist + "\",\n"
                  + (valueName!=null ? "\"lastValue\": " + lastRealTimeValue(valueOfIdService, valueName, fromTime, apiKey, con) + "," : "")
                  + "\"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfIdService,"thumbs") + "\n"
                  + "},\n"
                  + "\"id\": " + Integer.toString(i + 1) + "\n"
                  + "}");

          i++;
          numeroSensori++;
        }
        out.println("]}");
      }

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
        final boolean excludeSameAs = conf.get("excludeSameAs", "true").equalsIgnoreCase("true");
        
        int fullCount = -1;
        String limitServiceNear;
        if (!risultatiServizi.equals("0")) {
          if (!cat_servizi.equals("categorie_t")) {
            resServizi = ((Integer.parseInt(risultatiServizi)) - (numeroBus + numeroSensori));
            limitServiceNear = " LIMIT " + resServizi;
          } else {
            resServizi = Integer.parseInt(risultatiServizi);
            limitServiceNear = " LIMIT " + risultatiServizi;
          }
        } else {
          resServizi = MAX_RESULTS - numeroBus - numeroSensori;
          limitServiceNear = "";
        }

        SparqlQuery queryNearServices = new SparqlQuery() {
          @Override
          public String query(String type, String geoMode) throws Exception {
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
                    + "SELECT DISTINCT * {"
                    + "SELECT DISTINCT ?ser (STR(?lat) AS ?elat) (STR(?long) AS ?elong) ?sType ?sCategory ?sTypeLang (IF(?sName1,?sName1,?sName2) as ?sName) ?multimedia ?hasGeometry (STR(?geo) AS ?sgeo) ?ag ?agname (xsd:int(?dist*10000)/10000.0 AS ?dst) \n"
                    + ServiceMap.graphAccessQueryFragment2(apiKey)
                    + "WHERE {\n"
                    + " ?ser rdf:type km4c:Service" + (sparqlType.equals("virtuoso") ? " OPTION (inference \"urn:ontology\")" : "") + ".\n"
                    + ( excludeSameAs ?" FILTER NOT EXISTS {?ser owl:sameAs ?xxx}\n" : "")
                    + ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch)
                    + (inside ? 
                      "?ser opengis:hasGeometry [geo:geometry ?geo].\n"
                      + "  ?ser geo:lat ?lat.\n"
                      + "  ?ser geo:long ?long.\n"
                      + "filter(bif:st_contains(?geo,bif:st_point("+coords[1]+","+coords[0]+"),0.0000001))"
                    : /*" {\n"
                      + "  ?ser km4c:hasAccess ?entry.\n"
                      + "  ?entry geo:lat ?elat.\n"
                      + "  ?entry geo:long ?elong.\n"
                      + ServiceMap.geoSearchQueryFragment("?entry", coords, raggioServizi)
                      + " } UNION {\n"
                      + */ "  ?ser geo:lat ?lat.\n"
                      + "  ?ser geo:long ?long.\n"
                      + ServiceMap.geoSearchQueryFragment("?ser", coords, raggioServizi, geoMode)
                      + ServiceMap.valueTypeSearchQueryFragment("?ser", value_type)
                      + ServiceMap.graphSearchQueryFragment("?ser", graphUri)
                      + ServiceMap.modelSearchQueryFragment("?ser", model)
                      + ServiceMap.graphAccessQueryFragment("?ser", apiKey)
                      /*+ " }\n"*/)
                    + fc
                    + " OPTIONAL {?ser <http://schema.org/name> ?sName1}\n"
                    + " OPTIONAL {?ser foaf:name ?sName2}\n"
                    + (!km4cVersion.equals("old")
                            ? " ?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:BusStop && ?sType!=km4c:SensorSite)\n"
                            + filtroDL
                            + " ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory != <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>)\n"
                            + " ?sCategory rdfs:subClassOf km4c:Service.\n"
                            + " ?sType rdfs:label ?sTypeLang. FILTER(LANG(?sTypeLang)=\"" + lang + "\")\n" : "")
                    + (getGeometry || inside ? " OPTIONAL {?ser opengis:hasGeometry [opengis:asWKT ?wktGeometry]. BIND(!STRSTARTS(?wktGeometry,\"POINT\") as ?hasGeometry)}\n" : "")
                    + "   OPTIONAL {?ser km4c:multimediaResource ?multimedia}.\n"
                    //+ "   OPTIONAL {?st gtfs:stop ?ser.\n"
                    //+ "     ?st gtfs:trip/gtfs:route/gtfs:agency ?ag.\n"
                    //+ "     ?ag foaf:name ?agname.}\n"
                    + "   OPTIONAL {?ser gtfs:agency ?ag.\n"
                    + "     ?ag foaf:name ?agname.}\n"
                    + "}\n"
                    + "}"
                    + (type != null ? "}" : " ORDER BY ?dst");
          }
        };

        if("true".equals(makeFullCount))
          fullCount = ServiceMap.countQuery(con, queryNearServices);

        TupleQueryResult result = ServiceMap.geoSparqlQuery(con, queryNearServices, null, "API-nearservices", args);
        if (!searchBusStop && !searchSensorSite || inside) {
          out.println("\"Services\":{");
        } else {
          out.println(", \"Services\":{");
        }
        if (fullCount >= 0) {
          out.println("\"fullCount\": " + fullCount + ",");
        }
        out.println("\"type\": \"FeatureCollection\",\n"
                + "\"features\": [");
        int w = 0;
        while (result.hasNext() && numeroServizi < resServizi ) {
          BindingSet bindingSet = result.next();
          
          String valueOfSer = bindingSet.getValue("ser").stringValue();
          // check if is private or public and if user can see it
          if(!IoTChecker.checkIoTService(valueOfSer, apiKey)) {
              continue;
          }

          String valueOfSType = "";
          if (bindingSet.getValue("sType") != null) {
            valueOfSType = bindingSet.getValue("sType").stringValue();
          }
          String valueOfSTypeLang = "";
          if (bindingSet.getValue("sTypeLang") != null) {
            valueOfSTypeLang = bindingSet.getValue("sTypeLang").stringValue();
          }

          String valueOfTipo = "";
          valueOfTipo = valueOfSTypeLang;
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
          String valueOfDist = "";
          if(bindingSet.getValue("dst")!=null)
            valueOfDist = bindingSet.getValue("dst").stringValue();      
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

          if(inside && bindingSet.getValue("sgeo")!=null) {
            //per bug di virtuoso controlla se geometria effettivamente contiene il punto richiesto
            WKTReader wktReader=new WKTReader();
            Geometry position=wktReader.read("POINT("+coords[1]+" "+coords[0]+")").buffer(0.0001);
            try {
              Geometry g=wktReader.read(bindingSet.getValue("sgeo").stringValue());
              if(!g.intersects(position)) {
                ServiceMap.println("SKIP! "+valueOfSName);
                continue;
              } else {
                ServiceMap.println("OK "+valueOfSName);
              }
            }catch(Exception e) {
              ServiceMap.notifyException(e);
            }
          }

          if (w != 0) {
            out.println(",");
          }

          out.print("{\n"
                  + "\"geometry\": {\"type\": \"Point\",\"coordinates\":[" + valueOfELong + "," + valueOfELat + "]},\n"
                  + "\"type\": \"Feature\",\n"
                  + "\"properties\": {\n"
                  + "\"name\": \"" + escapeJSON(valueOfSName) + "\",\n"
                  + "\"tipo\": \"" + escapeJSON(valueOfTipo) + "\",\n"
                  + "\"typeLabel\": \"" + escapeJSON(valueOfSTypeLang) + "\",\n"
                  + "\"serviceType\": \"" + escapeJSON(serviceType) + "\",\n"
                  + (getGeometry ? "\"hasGeometry\": " + escapeJSON(valueOfHasGeometry) + ",\n" : "")
                  + "\"distance\": \"" + escapeJSON(valueOfDist) + "\",\n"
                  + "\"serviceUri\": \"" + valueOfSer + "\",\n"
                  + (photos ? "   \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfSer,"thumbs") + ",\n" : "")
                  + (valueOfAgName!=null ? "   \"agency\": \"" + valueOfAgName + "\",\n" : "")
                  + (valueOfAgUri!=null ? "   \"agencyUri\": \"" + valueOfAgUri + "\",\n" : "")
                  + "\"multimedia\": \"" + valueOfMultimedia + "\"\n"
                  + (valueName!=null ? ",\"lastValue\": " + lastRealTimeValue(valueOfSer, valueName, fromTime, apiKey, con) : "")
                  + "},\n"
                  + "\"id\": " + Integer.toString(w + 1) + "\n"
                  + "}");
          w++;
          numeroServizi++;
        }
        out.println("]}");
      }
    } finally {
      out.println("}");
    }
    ServiceMap.performance("API-LatLngServices END elapsed: "+(System.currentTimeMillis()-tss)+"ms");
    return numeroBus + numeroSensori + numeroServizi;
  }
  
  public int queryFulltext(JspWriter out, RepositoryConnection con, final String textToSearch, String selection, final String dist, String limit, final String lang, final boolean getGeometry, final String apikey) throws Exception {
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
      public String query(String type, String geoMode) throws Exception {
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
                + "SELECT DISTINCT ?ser ?long ?lat ?sType ?sTypeIta ?sCategory ?sName ?txt ?civic (!STRSTARTS(?wktGeometry,\"POINT\") as ?hasGeometry) ?multimedia ?ag ?agname ?x \n"
                + ServiceMap.graphAccessQueryFragment2(apikey)
                + "WHERE {\n"
                + ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch)
                + (km4cVersion.equals("old")
                        ? "  OPTIONAL { ?ser km4c:hasServiceCategory ?cat.\n"
                        + "  ?cat rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"" + lang + "\") }\n"
                        : " {\n"
                        + "  ?ser a ?sType. FILTER(?sType!=km4c:Event && ?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService)\n"
                        + "  OPTIONAL { ?sType rdfs:subClassOf* ?sCategory. ?sCategory rdfs:subClassOf km4c:Service. FILTER(STRSTARTS(STR(?sCategory),\"http://www.disit.org/km4city/schema#\"))}\n"
                        + "  ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"" + lang + "\")\n"
                        + " }\n")
                + " OPTIONAL {{?ser rdfs:label ?sName.}UNION{?ser schema:name ?sName.}UNION{?ser foaf:name ?sName.}UNION{?ser km4c:extendName ?sName.}}\n"
                + " {\n"
                + "  ?ser geo:lat ?lat .\n"
                + "  ?ser geo:long ?long .\n"
                + ServiceMap.geoSearchQueryFragment("?ser", coords, dist, geoMode)
                + " } UNION {\n"
                + "  ?ser km4c:hasAccess ?entry.\n"
                + "	 ?entry geo:lat ?lat.\n"
                + "	 ?entry geo:long ?long.\n"
                + ServiceMap.geoSearchQueryFragment("?entry", coords, dist, geoMode)
                + freeText
                /*+ "} UNION {"
                 + " ?ser km4c:hasStreetNumber/km4c:hasExternalAccess ?entry."
                 + " ?entry geo:lat ?lat."
                 + " ?entry geo:long ?long." 
                 + ServiceMap.geoSearchQueryFragment("?entry", coords, dist)*/
                + " }\n"
                + ServiceMap.graphAccessQueryFragment("?ser", apikey)
                + (getGeometry ? " OPTIONAL {?ser opengis:hasGeometry [opengis:asWKT ?wktGeometry].}\n" : "")
                + " OPTIONAL {?ser km4c:multimediaResource ?multimedia}.\n"
                + " OPTIONAL {\n"
                //+ "  {?st gtfs:stop ?ser.}UNION{?st gtfs:stop [owl:sameAs ?ser]}\n" 
                //+ "  ?st gtfs:trip ?t.\n"
                //+"   ?t gtfs:route/gtfs:agency ?ag.\n"
                + "   {?ser gtfs:agency ?ag.}UNION{?ser ^owl:sameAs/gtfs:agency ?ag}\n"
                + "   ?ag foaf:name ?agname.\n"
                + " }\n"
                + "} "
                + (type != null ? "}" : "ORDER BY DESC(?sc)");
      }
    };
    int fullCount = -1;
    String queryText = query.query(null, null);
    //ServiceMap.println(queryText);
    if (!"0".equals(limit)) {
      queryText += " LIMIT " + Integer.parseInt(limit);
      fullCount = ServiceMap.countQuery(con, query.query("count", null));
    } else {
      fullCount = ServiceMap.countQuery(con, query.query("count", null));
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
      BindingSet bindingSet = result.next();

      String serviceUri = bindingSet.getValue("ser").stringValue();
      if(!IoTChecker.checkIoTService(serviceUri, apikey)) {
        continue;
      }
      
      if (i != 0) {
        out.print(", ");
      }
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
    return i;
  }

  public int queryMunicipalityServices(JspWriter out, RepositoryConnection con, String selection, String categorie, String textToSearch, String risultatiBus, String risultatiSensori, String risultatiServizi, String lang, final boolean getGeometry, String makeFullCount, String apiKey) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    List<String> listaCategorie = new ArrayList<>();
    if (categorie != null && !"".equals(categorie)) {
      String[] arrayCategorie = categorie.split(";");
      // GESTIONE CATEGORIE
      listaCategorie = Arrays.asList(arrayCategorie);
    }
    String nomeComune = selection;
    if(nomeComune.toLowerCase().contains("comune di "))
      nomeComune = selection.substring(selection.toLowerCase().indexOf("comune di ") + 10);
    String filtroLocalita = "";
    filtroLocalita += "{ ?ser km4c:hasAccess ?entry . ";
    filtroLocalita += "	?entry geo:lat ?elat . ";
    //filtroLocalita += "	 FILTER (?elat>40) ";
    filtroLocalita += "	 ?entry geo:long ?elong . ";
    //filtroLocalita += "	 FILTER (?elong>10) ";
    filtroLocalita += "   ?nc km4c:hasExternalAccess ?entry . ";
    filtroLocalita += "  ?nc km4c:belongToRoad ?road . ";
    filtroLocalita += "  ?road km4c:inMunicipalityOf ?mun . ";
    filtroLocalita += "?mun foaf:name \"" + nomeComune + "\"^^xsd:string . }";
    filtroLocalita += "UNION {";
    filtroLocalita += " ?ser km4c:isInRoad ?road . ";
    filtroLocalita += "	?ser geo:lat ?elat . ";
    //filtroLocalita += "	 FILTER (?elat>40) ";
    filtroLocalita += "	?ser geo:long ?elong . ";
    //filtroLocalita += "	 FILTER (?elong>10) ";
    filtroLocalita += " ?road km4c:inMunicipalityOf ?mun . ";
    filtroLocalita += " ?mun foaf:name \"" + nomeComune + "\"^^xsd:string . ";
    filtroLocalita += "}";
    filtroLocalita += "UNION {";
    filtroLocalita += " ?ser km4c:inMunicipalityOf ?mun .";
    filtroLocalita += " ?mun foaf:name \"" + nomeComune + "\"^^xsd:string . ";
    filtroLocalita += "}";

    String fc = "";
    try {
      fc = ServiceMap.filterServices(listaCategorie);
    } catch (Exception e) {
      ServiceMap.notifyException(e);
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
        queryString += " LIMIT " + Integer.parseInt(risultatiBus);
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

        out.println("{\n"
                + " \"geometry\": {\n"
                + "     \"type\": \"Point\",\n"
                + "    \"coordinates\": [\n"
                + "       " + valueOfBSLong + "," + valueOfBSLat + "\n"
                + " ]\n"
                + "},\n"
                + "\"type\": \"Feature\",\n"
                + "\"properties\": {\n"
                + "    \"name\": \"" + escapeJSON(valueOfNomeFermata) + "\",\n"
                + "    \"typeLabel\": \"" + escapeJSON(type) + "\",\n"
                + "    \"serviceUri\": \"" + valueOfBS + "\",\n"
                + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfBS,"thumbs") + "\n"
                + "},\n"
                + "\"id\": " + Integer.toString(b + 1) + "\n"
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
        queryStringSensori += " LIMIT " + Integer.parseInt(risultatiSensori);
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

        out.println("{\n"
                + " \"geometry\": {\n"
                + "     \"type\": \"Point\",\n"
                + "    \"coordinates\": [\n"
                + "       " + valueOfLong + "," + valueOfLat + "\n"
                + " ]\n"
                + "},\n"
                + "\"type\": \"Feature\",\n"
                + "\"properties\": {\n"
                + "    \"name\": \"" + valueOfId + "\",\n"
                + "    \"typeLabel\": \"" + type + "\",\n"
                + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\",\n"
                + "    \"serviceUri\": \"" + escapeJSON(valueOfIdService) + "\",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfIdService,"thumbs") + "\n"
                + "},\n"
                + "\"id\": " + Integer.toString(s + 1) + "\n"
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
              + "SELECT distinct ?ser ?serAddress ?serNumber ?elat ?elong (IF(?sName1,?sName1,?sName2) as ?sName) ?sType ?email ?note ?labelIta ?multimedia ?description ?identifier ?sCategory (!STRSTARTS(?wktGeometry,\"POINT\") as ?hasGeometry) WHERE {\n"
              + " ?ser rdf:type km4c:Service" + (sparqlType.equals("virtuoso") ? " OPTION (inference \"urn:ontology\")" : "") + ".\n"
              + " OPTIONAL {?ser <http://schema.org/name> ?sName1}\n"
              + " OPTIONAL {?ser foaf:name ?sName2}\n"
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
                      + " ?sType rdfs:subClassOf* ?sCategory. FILTER(?sCategory != <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>) "
                      + " ?sCategory rdfs:subClassOf km4c:Service."
                      + " ?sType rdfs:label ?labelIta. FILTER(LANG(?labelIta)=\"" + lang + "\")\n" : "")
              + (getGeometry ? " OPTIONAL {?ser opengis:hasGeometry [opengis:asWKT ?wktGeometry].}\n" : "")
              + "}";
      if (!risultatiServizi.equals("0")) {
        queryStringServices += " LIMIT " + Integer.parseInt(risultatiServizi);
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
        if(!IoTChecker.checkIoTService(valueOfSer, apiKey)) {
          continue;
        }
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
        out.println("{\n"
                + " \"geometry\": {\n"
                + "     \"type\": \"Point\",\n"
                + "    \"coordinates\": [\n"
                + "       " + valueOfELong + "," + valueOfELat + "\n"
                + " ]\n"
                + "},\n"
                + "\"type\": \"Feature\",\n"
                + "\"properties\": {\n"
                + "    \"name\": \"" + escapeJSON(valueOfSName) + "\",\n"
                + "    \"typeLabel\": \"" + escapeJSON(valueOfSTypeIta) + "\",\n"
                + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\",\n"
                + (getGeometry ? "    \"hasGeometry\": " + hasGeometry + ",\n" : "")
                + "    \"serviceUri\": \"" + valueOfSer + "\",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfSer,"thumbs") + "\n"
                + "},\n"
                + "\"id\": " + Integer.toString(t + 1) + "\n"
                + "}");
        t++;
        numeroServizi++;
      }
      out.println("]}}");
    }
    return numeroBus + numeroSensori + numeroServizi;
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
            + "  <" + ServiceMap.urlEncode(idService) + "> km4c:refersToMunicipality ?mun."
            + "  ?mun foaf:name ?nomeComune."
            + " }UNION{"
            + "  <" + ServiceMap.urlEncode(idService) + "> rdf:type km4c:Municipality;"
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
    //ServiceMap.println("comune: " + nomeComune);
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
              + " <" + ServiceMap.urlEncode(valueOfWRep) + "> km4c:hasPrediction ?wPred."
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

  public void querySensor(JspWriter out, RepositoryConnection con, String idService, String lang, String realtime, String checkHealtiness, String uid, String fromTime) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String type = "";
    JsonArray rtData = new JsonArray();
    JsonObject rtAttrs = null;
    if (lang.equals("it")) {
      type = "Sensore";
    } else {
      type = "Sensor";
    }
    
    ServiceMapping.MappingData md = ServiceMapping.getInstance().getMappingForServiceType(1, "SensorSite");
    
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
            + "select distinct ?idSensore ?lat ?long ?address ?nomeComune ?period where{"
            + " <" + ServiceMap.urlEncode(idService) + "> rdf:type km4c:SensorSite;"
            + "  geo:lat ?lat;"
            + "  geo:long ?long;"
            + "  dcterms:identifier ?idSensore;"
            + "  schema:streetAddress ?address;"
            + "  km4c:placedOnRoad ?road."
            + " ?road km4c:inMunicipalityOf ?mun."
            + " ?mun foaf:name ?nomeComune."
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
                + "    \"address\": \"" + escapeJSON(valueOfAddress) + "\",");
                if(md!=null)
                  rtAttrs = md.printServiceAttributes(out, con, idService);
                out.println("    \"photos\": " + ServiceMap.getServicePhotos(idService) + ",\n"
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
      int limit = 1;
      if(checkHealtiness.equals("true") && rtAttrs != null) {
        limit = ServiceMap.getHealthCount(rtAttrs);
        if(limit == 0)
          limit = 1;
        else {
          fromTime = null;
        }
      }
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
              + "select distinct ?avgDistance ?avgTime ?occupancy ?concentration ?vehicleFlow ?averageSpeed ?thresholdPerc ?speedPercentile ?timeInstant where{"
              + " <" + ServiceMap.urlEncode(idService) + "> rdf:type km4c:SensorSite;"
              //+ "  dcterms:identifier \"" + nomeSensore + "\"^^xsd:string;"
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
              + (fromTime!=null ? 
                " FILTER(?timeInstant>=\""+fromTime+ServiceMap.getCurrentTimezoneOffset()+"\"^^xsd:dateTime)"
                      : "")
              + "} ORDER BY DESC (?timeInstant)"
              + (fromTime==null ? "LIMIT " + limit : "LIMIT " + conf.get("fromTimeLimitSparql",conf.get("fromTimeLimit","1500")));
      ServiceMap.println(querySensorData);
      TupleQuery tupleQuerySensorData = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(querySensorData, km4cVersion));
      TupleQueryResult resultSensorData = tupleQuerySensorData.evaluate();
      logQuery(querySensorData, "API-sensor-rt-info", sparqlType, idService, System.nanoTime() - ts);

      String valueOfInstantDateTime = "";
      try {
        int t = 0;

        out.println(",\"realtime\":");
        if(resultSensorData.hasNext()) {
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
            JsonObject rt = new JsonObject();
            
            valueOfInstantDateTime = bindingSetSensorData.getValue("timeInstant").stringValue();
            rt.addProperty("measuredTime", valueOfInstantDateTime);
            
            String valueOfAvgDistance = "Not Available";
            if (bindingSetSensorData.getValue("avgDistance") != null) {
              valueOfAvgDistance = bindingSetSensorData.getValue("avgDistance").stringValue();
              rt.addProperty("avgDistance", valueOfAvgDistance);
            }
            String valueOfAvgTime = "Not Available";
            if (bindingSetSensorData.getValue("avgTime") != null) {
              valueOfAvgTime = bindingSetSensorData.getValue("avgTime").stringValue();
              rt.addProperty("avgTime", valueOfAvgTime);
            }
            String valueOfOccupancy = "Not Available";
            if (bindingSetSensorData.getValue("occupancy") != null) {
              valueOfOccupancy = bindingSetSensorData.getValue("occupancy").stringValue();
              rt.addProperty("occupancy", valueOfOccupancy);
            }
            String valueOfConcentration = "Not Available";
            if (bindingSetSensorData.getValue("concentration") != null) {
              valueOfConcentration = bindingSetSensorData.getValue("concentration").stringValue();
              rt.addProperty("concentration", valueOfConcentration);
            }
            String valueOfVehicleFlow = "Not Available";
            if (bindingSetSensorData.getValue("vehicleFlow") != null) {
              valueOfVehicleFlow = bindingSetSensorData.getValue("vehicleFlow").stringValue();
              rt.addProperty("vehicleFlow", valueOfVehicleFlow);
            }
            String valueOfAverageSpeed = "Not Available";
            if (bindingSetSensorData.getValue("averageSpeed") != null) {
              valueOfAverageSpeed = bindingSetSensorData.getValue("averageSpeed").stringValue();
              rt.addProperty("averageSpeed", valueOfAverageSpeed);
            }
            String valueOfThresholdPerc = "Not Available";
            if (bindingSetSensorData.getValue("thresholdPerc") != null) {
              valueOfThresholdPerc = bindingSetSensorData.getValue("thresholdPerc").stringValue();
              rt.addProperty("thresholdPerc", valueOfThresholdPerc);
            }
            String valueOfSpeedPercentile = "Not Available";
            if (bindingSetSensorData.getValue("speedPercentile") != null) {
              valueOfSpeedPercentile = bindingSetSensorData.getValue("speedPercentile").stringValue();
              rt.addProperty("speedPercentile", valueOfSpeedPercentile);
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
            rtData.add(rt);
            t++;
          }
          out.println("]}}");

        } else {
          out.println("{}");
        }
        if(checkHealtiness.equals("true") && rtAttrs!=null)
              out.println(", \"healthiness\": "+ServiceMap.computeHealthiness(rtData, rtAttrs));

      } catch (Exception e) {
        out.println(e.getMessage());
      }
    }
    out.println("}");
  }

  public String queryService(JspWriter out, RepositoryConnection con, String serviceUri, String lang, String realtime, String valueName, String fromTime, String toTime, String checkHealthiness, String uid, List<String> serviceTypes, String format) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String subCategory = "";
    String category = "";
    String TOS = "";
    String NOS = "";
    String r = "";
    JsonObject rtAttributes = null;
    serviceUri = ServiceMap.urlEncode(serviceUri);
    int i = 0;
    int limit = 1;
    if(fromTime!=null && fromTime.startsWith("last-")) {
      limit = Integer.parseInt(fromTime.split("-")[1]);
      fromTime = null;
    }
    ServiceMapping.MappingData md = ServiceMapping.getInstance().getMappingForServiceType(1, serviceTypes);
    if(md==null || (md.detailsQuery==null)) {
      ServiceMap.println("querySerice: no mapping or no details query");
      String queryService = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
              + "SELECT ?serAddress ?serNumber ?elat ?elong (IF(?sName1,?sName1,?sName2) as ?sName) ?sType ?type ?sCategory ?sTypeIta ?email ?note ?multimedia ?description1 ?description2 ?phone ?fax ?website ?prov ?city ?cap ?coordList WHERE{\n"
              + " {\n"
              + "  <" + serviceUri + "> km4c:hasAccess ?entry.\n"
              + "  ?entry geo:lat ?elat.\n"
              + "  ?entry geo:long ?elong.\n"
              //+ " }UNION{\n"
              //+ "  <" + idService + "> km4c:isInRoad ?road.\n"
              //+ "  <" + idService + "> geo:lat ?elat.\n"
              //+ "  <" + idService + "> geo:long ?elong.\n"
              + " }UNION{\n"
              + "  <" + serviceUri + "> geo:lat ?elat ; geo:long ?elong.\n"
              + " }\n"
              + "   OPTIONAL {<" + serviceUri + "> schema:name ?sName1.}\n"
              + "   OPTIONAL {<" + serviceUri + "> foaf:name ?sName2.}\n"
              + "   OPTIONAL { <" + serviceUri + "> schema:streetAddress ?serAddress.}\n"
              + "   OPTIONAL {<" + serviceUri + "> opengis:hasGeometry ?geometry .\n"
              + "     ?geometry opengis:asWKT ?coordList .}\n"
              + (km4cVersion.equals("old")
                      ? " <" + serviceUri + "> km4c:hasServiceCategory ?cat .\n"
                      + " ?cat rdfs:label ?nome.\n"
                      + " BIND (?nome  AS ?sType).\n"
                      + " BIND (?nome  AS ?sTypeIta).\n"
                      + " FILTER(LANG(?nome) = \"it\").\n"
                      + " OPTIONAL {<" + serviceUri + "> <http://purl.org/dc/elements/1.1/description> ?description.\n"
                      + " FILTER(LANG(?description) = \"it\")}.\n"
                  : " <" + serviceUri + "> a ?type . FILTER(?type!=km4c:RegularService && ?type!=km4c:Service && ?type!=km4c:DigitalLocation)\n"
                      + " ?type rdfs:label ?nome.\n"
                      + " ?type rdfs:subClassOf* ?sCategory.\n"
                      + " ?sCategory rdfs:subClassOf km4c:Service.\n"
                      + " BIND (?nome  AS ?sType).\n"
                      + " BIND (?nome  AS ?sTypeIta).\n"
                      + " FILTER(LANG(?nome) = \"" + lang + "\").\n")
              + "   OPTIONAL {<" + serviceUri + "> km4c:houseNumber ?serNumber}.\n"
              + "   OPTIONAL {<" + serviceUri + "> dcterms:description ?description1\n"
              //+ " FILTER(LANG(?descriptionEng) = \"en\")"
              + "}\n"
              + "   OPTIONAL {<" + serviceUri + "> dcterms:description ?description2 FILTER(?description2!=?description1)\n"
              + "}"
              + "   OPTIONAL {<" + serviceUri + "> km4c:multimediaResource ?multimedia}.\n"
              + "   OPTIONAL {<" + serviceUri + "> skos:note ?note}.\n"
              + "   OPTIONAL {<" + serviceUri + "> schema:email ?email }.\n"
              + "   OPTIONAL {<" + serviceUri + "> schema:faxNumber ?fax}\n"
              + "   OPTIONAL {<" + serviceUri + "> schema:telephone ?phone}\n"
              + "   OPTIONAL {<" + serviceUri + "> schema:addressRegion ?prov}\n"
              + "   OPTIONAL {<" + serviceUri + "> schema:addressLocality ?city}\n"
              + "   OPTIONAL {<" + serviceUri + "> schema:postalCode ?cap}\n"
              + "   OPTIONAL {<" + serviceUri + "> schema:url ?website}\n"
              + "} LIMIT 1";
      // out.println("count = "+count);
      String queryDBpedia = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX cito:<http://purl.org/spar/cito/>\n"
              + "SELECT ?linkDBpedia WHERE{\n"
              + " {<" + serviceUri + "> km4c:isInRoad/cito:cites ?linkDBpedia.}\n"
              + " UNION { <" + serviceUri + "> rdfs:seeAlso ?linkDBpedia.}\n"
              + "}";

      if(!"geojson".equals(format))
        out.println("{ \"Service\":");
      out.println("{\"type\": \"FeatureCollection\",\n"
              + "\"features\": [\n");
      //ServiceMap.println(queryService);
      TupleQuery tupleQueryService = con.prepareTupleQuery(QueryLanguage.SPARQL, queryService);
      long tss = System.nanoTime();
      TupleQueryResult resultService = tupleQueryService.evaluate();
      logQuery(queryService, "API-service-info", sparqlType, serviceUri, System.nanoTime() - tss);
      TupleQuery tupleQueryDBpedia = con.prepareTupleQuery(QueryLanguage.SPARQL, queryDBpedia);
      tss = System.nanoTime();
      TupleQueryResult resultDBpedia = tupleQueryDBpedia.evaluate();
      logQuery(queryDBpedia, "API-service-dbpedia-info", sparqlType, serviceUri, System.nanoTime() - tss);
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
        if (bindingSetService.getValue("type") != null) {
          subCategory = bindingSetService.getValue("type").stringValue();
          subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
          //subCategory = Character.toLowerCase(subCategory.charAt(0)) + subCategory.substring(1);
          //subCategory = subCategory.replace(" ", "_");
        }

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
        String valueOfDescription1 = "";
        if (bindingSetService.getValue("description1") != null) {
          valueOfDescription1 = ServiceMap.replaceHTMLEntities(bindingSetService.getValue("description1").stringValue());
        }
        String valueOfDescription2 = "";
        if (bindingSetService.getValue("description2") != null) {
          valueOfDescription2 = ServiceMap.replaceHTMLEntities(bindingSetService.getValue("description2").stringValue());
        }
        if(valueOfDescription1.isEmpty() || 
                (!valueOfDescription2.isEmpty() && !valueOfDescription1.contains(" the ") && lang.equals("en")) ||
                (!valueOfDescription2.isEmpty() && valueOfDescription1.contains(" the ") && !lang.equals("en"))) {
          String tmp=valueOfDescription2;
          valueOfDescription2 = valueOfDescription1;
          valueOfDescription1 = tmp;        
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

        if(conf.get("normalizeNotes", "false").equals("true")) {
          Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
          valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 \\.:;,]+", "");
        }

        float[] avgServiceStars = ServiceMap.getAvgServiceStars(serviceUri);

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
                + "    \"description\": \"" + escapeJSON(valueOfDescription1) + "\",\n"
                + "    \"description2\": \"" + escapeJSON(valueOfDescription2) + "\",\n"
                + "    \"multimedia\": \"" + valueOfMultimediaResource + "\",\n"
                + "    \"serviceUri\": \"" + serviceUri + "\",\n"
                + "    \"address\": \"" + escapeJSON(valueOfSerAddress) + "\", \"civic\": \"" + escapeJSON(valueOfSerNumber) + "\",\n"
                + "    \"wktGeometry\": \"" + escapeJSON(ServiceMap.fixWKT(valueOfCoordList)) + "\",");
        if(md!=null)
          rtAttributes = md.printServiceAttributes(out, con, serviceUri);
        out.println(
                  "    \"photos\": " + ServiceMap.getServicePhotos(serviceUri) + ",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(serviceUri,"thumbs") + ",\n"
                + "    \"photoOrigs\": " + ServiceMap.getServicePhotos(serviceUri,"originals") + ",\n"
                + "    \"avgStars\": " + avgServiceStars[0] + ",\n"
                + "    \"starsCount\": " + (int) avgServiceStars[1] + ",\n"
                + (uid != null ? "    \"userStars\": " + ServiceMap.getServiceStarsByUid(serviceUri, uid) + ",\n" : "")
                + "    \"comments\": " + ServiceMap.getServiceComments(serviceUri)
                + "},\n"
                + "\"id\": " + Integer.toString(i + 1) + "\n"
                + "}");
        i++;
      }
      out.println("] }");
    } else {
        ServiceMap.println("queryService: using mapping "+md.serviceType);
        String detailsQuery = md.detailsQuery;
        if(detailsQuery==null) {
          throw new Exception("Missing details query for "+serviceUri);
        }
        if(!"geojson".equals(format))
          out.println("{ \"" + md.section + "\":");
        out.println("{\"type\": \"FeatureCollection\",\n"
                + "\"features\": [\n");

        detailsQuery = detailsQuery.replace("%SERVICE_URI",ServiceMap.urlEncode(serviceUri));
        detailsQuery = detailsQuery.replace("%LANG",lang);
        ServiceMap.println(detailsQuery);
        TupleQuery tupleQueryDetails = con.prepareTupleQuery(QueryLanguage.SPARQL, detailsQuery);
        long ts = System.nanoTime();
        TupleQueryResult resultDetails = tupleQueryDetails.evaluate();
        logQuery(detailsQuery, "API-service-info", sparqlType, serviceUri, System.nanoTime() - ts);
        ServiceMap.println(detailsQuery);
        
        if (resultDetails.hasNext()) {
          String queryDBpedia = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX cito:<http://purl.org/spar/cito/>\n"
              + "SELECT ?linkDBpedia WHERE{\n"
              + " {<" + serviceUri + "> km4c:isInRoad/cito:cites ?linkDBpedia.}\n"
              + " UNION { <" + serviceUri + "> rdfs:seeAlso ?linkDBpedia.}\n"
              + "}";
          TupleQuery tupleQueryDBpedia = con.prepareTupleQuery(QueryLanguage.SPARQL, queryDBpedia);
          ts = System.nanoTime();
          TupleQueryResult resultDBpedia = tupleQueryDBpedia.evaluate();
          logQuery(queryDBpedia, "API-service-dbpedia-info", sparqlType, serviceUri, System.nanoTime() - ts);
          String valueOfDBpedia = "[";
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
          
          List<String> vars = resultDetails.getBindingNames();

          int p = 0;
          while (resultDetails.hasNext()) {
            BindingSet bindingSet = resultDetails.next();

            if (p != 0) {
              out.println(" ,");
            }

            String valueOfELong = null;
            if(bindingSet.getValue("_elong")!=null)
              valueOfELong = bindingSet.getValue("_elong").stringValue();
            String valueOfELat = null;
            if(bindingSet.getValue("_elat")!=null)
              valueOfELat = bindingSet.getValue("_elat").stringValue();
            String valueOfWktGeometry = "";
            if(bindingSet.getValue("_wktGeometry")!=null)
              valueOfWktGeometry = bindingSet.getValue("_wktGeometry").stringValue();
            if (bindingSet.getValue("_type") != null) {
              subCategory = bindingSet.getValue("_type").stringValue();
              subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
            }
            if (bindingSet.getValue("_category") != null) {
              category = bindingSet.getValue("_category").stringValue();
              category = category.replace("http://www.disit.org/km4city/schema#", "");
            }

            String serviceType = category;
            if(!serviceType.isEmpty())
             serviceType += "_";
            serviceType += subCategory;
            r = category + ";" + subCategory; // return value
            
            out.println("{");
            if(valueOfELong!=null && valueOfELat!=null)
                out.println(" \"geometry\": {\n"
                + "     \"type\": \"Point\",\n"
                + "    \"coordinates\": [ " + valueOfELong + ", " + valueOfELat + " ]\n"
                + " },");
            out.println(" \"type\": \"Feature\",\n"
                + " \"properties\": {");

            out.println("    \"serviceUri\": \"" + serviceUri + "\",");
            out.println("    \"serviceType\": \"" + serviceType + "\",");
            
            for(String v : vars) {
              if(!v.startsWith("_")) {
                String value = "";
                if (bindingSet.getValue(v) != null) {
                  value = bindingSet.getValue(v).stringValue();
                }
                out.println("    \""+v+"\": \""+escapeJSON(value)+"\",");
              }
            }

            rtAttributes = md.printServiceAttributes(out, con, serviceUri);
            
            float[] avgServiceStars = ServiceMap.getAvgServiceStars(serviceUri);
            
            out.println("    \"linkDBpedia\": " + valueOfDBpedia + ",");
            out.println("    \"wktGeometry\": \"" + escapeJSON(ServiceMap.fixWKT(valueOfWktGeometry)) + "\",\n"
                + "    \"photos\": " + ServiceMap.getServicePhotos(serviceUri) + ",\n"
                + "    \"photoThumbs\": " + ServiceMap.getServicePhotos(serviceUri,"thumbs") + ",\n"
                + "    \"photoOrigs\": " + ServiceMap.getServicePhotos(serviceUri,"originals") + ",\n"
                + "    \"avgStars\": " + avgServiceStars[0] + ",\n"
                + "    \"starsCount\": " + (int) avgServiceStars[1] + ",\n"
                + (uid != null ? "    \"userStars\": " + ServiceMap.getServiceStarsByUid(serviceUri, uid) + ",\n" : "")
                + "    \"comments\": " + ServiceMap.getServiceComments(serviceUri));
            if(!"geojson".equals(format))
                out.println("}\n}");
            p++;
          }
        }
        if(!"geojson".equals(format))
          out.println("]}");
    }
   
    serviceUri = ServiceMapping.getInstance().getServiceUriAlias(serviceUri);
        
    JsonArray rtData = new JsonArray();
    String labelPark = "Car park";
    if (lang.equals("it")) {
      labelPark = "Parcheggio auto";
    }
    if (md==null && TOS.equals(labelPark) && "true".equalsIgnoreCase(realtime)) {
      String queryStringParkingStatus = "  PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX time:<http://www.w3.org/2006/time#>\n"
              + "SELECT distinct ?situationRecord ?timeInstant ?occupancy ?free ?occupied ?capacity ?cpStatus WHERE { \n"
              + "	?cps km4c:observeCarPark <" + serviceUri + ">.\n"
              + "	?cps km4c:capacity ?capacity.\n"
              + "	?situationRecord km4c:relatedToSensor ?cps.\n"
              + "	?situationRecord km4c:observationTime ?time.\n"
              + "	?time <http://purl.org/dc/terms/identifier> ?timeInstant.\n"
              + "	OPTIONAL{?situationRecord km4c:parkOccupancy ?occupancy.}\n"
              + "	?situationRecord km4c:free ?free.\n"
              + "	OPTIONAL{?situationRecord km4c:carParkStatus ?cpStatus.}\n"
              + "	OPTIONAL{?situationRecord km4c:occupied ?occupied.}\n"
              + (fromTime!=null ? 
                " FILTER(?timeInstant>=\""+fromTime+ServiceMap.getCurrentTimezoneOffset()+"\"^^xsd:dateTime)"
                      : "")
              + "} ORDER BY DESC (?timeInstant)"
              + (fromTime==null ? "LIMIT "+limit : "LIMIT " + conf.get("fromTimeLimitSparql",conf.get("fromTimeLimit","1500")));
      TupleQuery tupleQueryParking = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringParkingStatus);
      long ts = System.nanoTime();
      TupleQueryResult resultParkingStatus = tupleQueryParking.evaluate();
      logQuery(queryStringParkingStatus, "API-service-park-info", sparqlType, serviceUri, System.nanoTime() - ts);
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

          JsonObject rt = new JsonObject(); 
          String valueOfInstantDateTime = "";
          if (bindingSetParking.getValue("timeInstant") != null) {
            valueOfInstantDateTime = bindingSetParking.getValue("timeInstant").stringValue();
            rt.addProperty("measuredTime", valueOfInstantDateTime);
          }
          String valueOfOccupancy = "";
          if (bindingSetParking.getValue("occupancy") != null) {
            valueOfOccupancy = bindingSetParking.getValue("occupancy").stringValue();
            rt.addProperty("occupancy", valueOfOccupancy);
          }
          String valueOfFree = "";
          if (bindingSetParking.getValue("free") != null) {
            valueOfFree = bindingSetParking.getValue("free").stringValue();
            rt.addProperty("occupancy", valueOfOccupancy);
          }
          String valueOfOccupied = null;
          if (bindingSetParking.getValue("occupied") != null) {
            valueOfOccupied = bindingSetParking.getValue("occupied").stringValue();
            rt.addProperty("occupancy", valueOfOccupancy);
          }
          String valueOfCapacity = "";
          if (bindingSetParking.getValue("capacity") != null) {
            valueOfCapacity = bindingSetParking.getValue("capacity").stringValue();
            rt.addProperty("occupancy", valueOfOccupancy);
          }
          String valueOfcpStatus = "";
          if (bindingSetParking.getValue("cpStatus") != null) {
            valueOfcpStatus = bindingSetParking.getValue("cpStatus").stringValue();
            rt.addProperty("occupancy", valueOfOccupancy);
          }
          
          //calcola il valore di occupied in base a capacity e free
          if(valueOfOccupied==null) {
            int occupied = Integer.parseInt(valueOfCapacity)-Integer.parseInt(valueOfFree);
            if(occupied<0)
              occupied=0;
            valueOfOccupied=""+occupied;
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
        out.println("]}}");
      } else {
        out.println("{}");
      }
    } else if("true".equalsIgnoreCase(realtime)) {
      if(checkHealthiness.equals("true") && rtAttributes != null) {
        limit = ServiceMap.getHealthCount(rtAttributes);
        if(limit == 0)
          limit = 1;
        else {
          fromTime = null;
        }
      }
      if(valueName!=null && rtAttributes.has(valueName) && 
              rtAttributes.getAsJsonObject(valueName).get("attr_type")!=null &&
              rtAttributes.getAsJsonObject(valueName).get("attr_type").getAsString().equals("CustomAttribute")) {
        // is stored with values
        Connection rtCon= ServiceMap.getRTConnection2();
        if(rtCon!=null) {
          ServiceValuesServlet.getValues(rtCon, conf, fromTime, toTime, (fromTime==null ? ""+limit : null), serviceUri, valueName, rtData, "sparql");
          out.println(",\"realtime\":{\"head\": {\n" +
              " \"vars\":[\""+valueName+"\"]},\n" +
              " \"results\": {\n" +
              " \"bindings\":"+rtData+"}}");
          rtCon.close();
        }
      } else {
        ArrayList<String> customAttrs = new ArrayList<>();
        if(fromTime==null && rtAttributes!=null) {
          for(Map.Entry<String,JsonElement> e:rtAttributes.entrySet()) {
            if(e.getValue().getAsJsonObject().has("attr_type") && e.getValue().getAsJsonObject().get("attr_type").getAsString().equals("CustomAttribute") )
              customAttrs.add(e.getKey());
          }
          ServiceMap.println("custom:"+customAttrs);
        }

        if(md!=null && md.realTimeSqlQuery!=null && md.realTimeSolrQuery==null) {
          realTimeSqlQuery(md, rtAttributes, customAttrs, serviceUri, valueName, fromTime, toTime, limit, conf, serviceTypes, out, rtData);
        } else if(md!=null && md.realTimeSparqlQuery!=null && md.realTimeSolrQuery==null) {
          realTimeSparqlQuery(md, rtAttributes, customAttrs, serviceUri, valueName, fromTime, toTime, limit, conf, con, sparqlType, out, rtData);
        } else if(md!=null && md.realTimeSolrQuery!=null) {
          if(md.realTimeSolrQuery.equals("true") || md.realTimeSolrQuery.equals("solr"))
            realTimeSolrQuery(conf, rtAttributes, customAttrs, serviceUri, valueName, fromTime, toTime, limit, out, rtData);
          else if(md.realTimeSolrQuery.equals("elasticsearch"))
            realTimeElasticSearchQuery(conf, rtAttributes, customAttrs, serviceUri, valueName, fromTime, toTime, limit, out, rtData);
        }
      }
    }
    if(checkHealthiness.equals("true"))
      out.println(", \"healthiness\": "+ServiceMap.computeHealthiness(rtData, rtAttributes, toTime));
    Connection rtCon = null;
    if(md!=null && md.predictionSqlQuery!=null) {
      rtCon = predictionQuery(md, serviceUri, out, rtCon, conf);
    }
    if(md!=null && md.trendSqlQuery!=null) {
      rtCon = timeTrendQuery(md, serviceUri, out, rtCon, conf);
    }
    if(rtCon!=null)
      rtCon.close();
    if("json".equals(format))
      out.println("}");
    else
      out.println("}}]}");
    return r;
  }

  private void realTimeSolrQuery(Configuration conf, JsonObject rtAttributes,  ArrayList<String> customAttrs, String serviceUri, String valueName, String fromTime, String toTime, int limit, JspWriter out, JsonArray rtData) throws SolrServerException, IOException {
    //get RT data from SOLR
    String urlString = conf.get("solrIoTIndexUrl", "http://192.168.0.12:8983/solr/sensors-ETL-IOTv3");
    SolrClient solr = new HttpSolrClient(urlString);
    String tilde = "~";
    ServiceMap.println("query solr "+serviceUri);
    String q = "serviceUri:\""+serviceUri+"\"";
    ServiceMap.println(q);
    SolrQuery query = new SolrQuery(q);
    if(fromTime!=null || toTime!=null) {
      String fq = null;
      String tz = "Z";
      if(fromTime!=null && toTime==null)
        fq = "date_time:["+fromTime+tz+" TO *]";
      else if(fromTime==null && toTime!=null)
        fq = "date_time:[* TO "+toTime+tz+"]";
      else
        fq = "date_time:["+fromTime+tz+" TO "+toTime+tz+"]";
      query.addFilterQuery(fq);
      ServiceMap.println(fq);
    }
    
    query.setRows(fromTime == null ? (rtAttributes.entrySet().size()+1) * limit : 10000);
    query.addField("id");
    query.addField("value_name");
    query.addField("value");
    query.addField("value_str");
    query.addField("value_unit");
    query.addField("date_time");
    query.addSort("date_time", ORDER.desc);
    query.addSort("value_name", ORDER.asc);
    query.addFacetField("value_name");
    
    long ts = System.currentTimeMillis();
    QueryResponse qr=solr.query(query);
    SolrDocumentList sdl=qr.getResults();
    long nfound=sdl.getNumFound();
    ServiceMap.performance("solr query "+serviceUri+" from:"+fromTime+" : "+(System.currentTimeMillis()-ts)+"ms");
    
    out.println(",\"realtime\": ");
    if(nfound==0) {
      out.println("{}");
    } else {
      out.print("{ \"head\": {\n"
              + " \"vars\":[ \"measuredTime\"");
      for(FacetField ff : qr.getFacetFields()) {
        for(Count count: ff.getValues()) {
          if(count.getCount()>0) {
            out.print(",\""+count.getName()+"\"");
          }
        }
      }
      out.println("]},");
      
      out.println(" \"results\": {");
      out.println(" \"bindings\": [");
      
      int c=0;
      Date cdt = null;
      JsonObject rt = new JsonObject();
      DateFormat dateFormatterTZ = new SimpleDateFormat(ServiceMap.dateFormatTZ);
      
      for(SolrDocument d: sdl) {
        Date dt = (Date)d.getFieldValue("date_time");
        Object mt = d.getFieldValue("value_name");
        Object value = d.getFieldValue("value");
        ServiceMap.println("value:"+value);
        if(value==null) {
          value = d.getFieldValue("value_str");
          ServiceMap.println("value_str:"+value);
        }
        String u = (String)d.getFieldValue("value_unit");
        
        //su solr l'ora è memorizzata come se fosse GMT quindi va tolto l'offset da GMT
        int offset=TimeZone.getDefault().getOffset(dt.getTime());
        dt.setTime(dt.getTime()-offset);
        
        //ServiceMap.println(ServiceMap.dateFormatterTZ.format(dt)+" "+mt+" "+value+" "+u);
        if(cdt==null || cdt.equals(dt)) {
          if(c==0) {
            String measuredTime = dateFormatterTZ.format(dt);
            out.print("  {\n  \"measuredTime\":{\"value\":\""+measuredTime+"\"},");
            rt.addProperty("measuredTime", measuredTime);
          }
          else
            out.print(",");
          c++;
          cdt=dt;
        } else {
          if(fromTime==null && rtData.size()>=limit-1) //prende i primi valori
            break;
          cdt = dt;
          rtData.add(rt);
          rt = new JsonObject();
          String measuredTime = dateFormatterTZ.format(dt);
          out.print("},  {\n  \"measuredTime\":{\"value\":\""+measuredTime+"\"},");
          rt.addProperty("measuredTime", measuredTime);
          c=1;
        }
        out.print("  \""+mt+"\":{\"value\":\""+value+"\"}");
        rt.addProperty(mt.toString(),value.toString());
      }
      rtData.add(rt);
      if(c!=0)
        out.println(" }");
      out.println("]}}");
    }
    solr.shutdown();
  }
  
  private void realTimeElasticSearchQuery(Configuration conf, JsonObject rtAttributes,  ArrayList<String> customAttrs, String serviceUri, String valueName, String fromTime, String toTime, int limit, JspWriter out, JsonArray rtData) throws Exception {
    RestHighLevelClient client = ServiceMap.createElasticSearchClient(conf);
    String[] index = conf.get("elasticSearchIndex", "sensorindex").split(";");

    try {
      String q = "serviceUri.keyword:\""+serviceUri+"\"";

      DateFormat dateFormatterT = new SimpleDateFormat(ServiceMap.dateFormatT);
      Date tTime = new Date();
      if(toTime!=null) {
        tTime = dateFormatterT.parse(toTime);
      }
      Date fTime = new Date();
      if(fromTime!=null) {
        fTime = dateFormatterT.parse(fromTime);
      } 
      if(fromTime!=null || toTime!=null) {
        DateFormat dateFormatterGMT = new SimpleDateFormat(ServiceMap.dateFormatGMT);
        String fq = null;
        String tz = "Z";
        String toTimeZ = null;
        String fromTimeZ = null;
        if(fromTime!=null) {
          fromTimeZ = dateFormatterGMT.format(fTime);
        }
        if(toTime!=null) {
          toTimeZ = dateFormatterGMT.format(tTime);
        }
        if(fromTime!=null && toTime==null)
          fq = "date_time:["+fromTimeZ+" TO *]";
        else if(fromTime==null && toTime!=null)
          fq = "date_time:[* TO "+toTimeZ+"]";
        else
          fq = "date_time:["+fromTimeZ+" TO "+toTimeZ+"]";
        q += " AND "+fq;
      }
      if(valueName!=null) {
        q += " AND value_name.keyword:\"" + valueName + "\"";
      }
      if(conf.get("elasticSearchFilterEmptyValue","false").equals("true")) {
        q += " AND NOT value_str.keyword:\"\"";
      }
      int resultSize = Integer.parseInt(conf.get("elasticSearchMaxSize", "10000"));
      if(fromTime == null) {
        if(limit<0)
          limit=-limit;
        if(valueName!=null) {
          resultSize = limit;
        } else {
          if(rtAttributes!=null)
            resultSize = (rtAttributes.entrySet().size()+10) * limit;
          else
            resultSize = 10 * limit;
        }
      } else if(limit<0)
        resultSize=-limit;
      SearchRequest sr = new SearchRequest();
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
      searchSourceBuilder.query(QueryBuilders.boolQuery().must(
              QueryBuilders.queryStringQuery(q)
      ));
      //aggregate by UUID to be used if more records are stored with the same timestamp
      if(conf.get("elasticSearchAggrByUUID", "false").equals("true")) {
        searchSourceBuilder.sort("date_time", SortOrder.DESC)
                .sort("uuid.keyword", SortOrder.ASC)
                .sort("value_name.keyword", SortOrder.ASC);
      } else {
        if(valueName != null)
          searchSourceBuilder.sort("date_time", SortOrder.DESC);
        else
          searchSourceBuilder.sort("date_time", SortOrder.DESC)
                .sort("value_name.keyword", SortOrder.ASC);        
      }
      searchSourceBuilder.size(resultSize);
      if(valueName==null) {
        searchSourceBuilder.aggregation(AggregationBuilders.terms("value_name").field("value_name.keyword").size(Integer.parseInt(conf.get("elasticSearchMaxAggVName", "100"))).order(BucketOrder.key(true)));
      }
      boolean fromAggregation = false;
      if(valueName!=null && fromTime!=null) {
        if((tTime.getTime() - fTime.getTime())/1000>=Integer.parseInt(conf.get("elasticSearchAggDays1", "100"))*24*60*60L) {
          searchSourceBuilder.aggregation(
                  AggregationBuilders.dateHistogram("date_time")
                          .field(conf.get("elasticSearchAggField", "date_time"))
                          .dateHistogramInterval(DateHistogramInterval.hours(Integer.parseInt(conf.get("elasticSearchAvgHours", "6"))))
                          .subAggregation(AggregationBuilders.avg("avg").field("value"))).size(0);
          fromAggregation = true;
        } else if((tTime.getTime() - fTime.getTime())/1000>=Integer.parseInt(conf.get("elasticSearchAggDays", "20"))*24*60*60L) {
          searchSourceBuilder.aggregation(
                  AggregationBuilders.dateHistogram("date_time")
                          .field(conf.get("elasticSearchAggField", "date_time"))
                          .dateHistogramInterval(DateHistogramInterval.minutes(Integer.parseInt(conf.get("elasticSearchAvgMins", "15"))))
                          .subAggregation(AggregationBuilders.avg("avg").field("value"))).size(0);
          fromAggregation = true;
        }
      }

      sr.source(searchSourceBuilder);
      if(conf.get("elasticSearchScrollSearch","false").equals("true"))
        sr.scroll(TimeValue.timeValueMinutes(1));
      sr.indices(index);

      long ts = System.currentTimeMillis();
      SearchResponse r = client.search(sr, RequestOptions.DEFAULT);
      SearchHit[] hits = r.getHits().getHits();
      long nfound = hits.length;
      if(nfound==0 && fromAggregation) {
          Histogram agg = r.getAggregations().get("date_time");
          if(!agg.getBuckets().isEmpty())
            nfound = 1; //we don't need exact number
      }
      String jsonQuery = "NA";
      if(conf.get("elasticSearchDebugQuery", "false").equals("true")) {
        try {
          jsonQuery = searchSourceBuilder.toString();
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
      ServiceMap.performance("elasticsearch query "+serviceUri+" from:"+fromTime+" to:"+toTime+" : "+(System.currentTimeMillis()-ts)+"ms nfound:"+nfound+" aggr:"+fromAggregation+" query:"+jsonQuery);


      if(out!=null)
        out.println(",\"realtime\": ");
      if(nfound==0 && out!=null) {
        out.println("{}");
      } else {
        if(out!=null) {
          out.print("{ \"head\": {\n"
                  + " \"vars\":[ \"measuredTime\"");
          if(valueName != null) {
            out.print(",\""+valueName+"\"");                      
          } else {
            Terms agg = r.getAggregations().get("value_name");

            for (Terms.Bucket entry : agg.getBuckets()) {
              //System.out.println(" "+entry.getKey()+" "+entry.getDocCount());
              if(entry.getDocCount()>0)
                out.print(",\""+entry.getKey()+"\"");
            }
            for(String a:customAttrs) {
                out.print(",\""+a+"\"");              
            }
          }
          out.println("]},");

          out.println(" \"results\": {");
          out.println(" \"bindings\": [");
        }
        
        int c=0;
        Date cdt = null;
        String cuuid = null;
        JsonObject rt = new JsonObject();
        DateFormat dateFormatterTZ[] = { 
          new SimpleDateFormat(ServiceMap.dateFormatTZ), new SimpleDateFormat(ServiceMap.dateFormatTZ2),
          new SimpleDateFormat(ServiceMap.dateFormatTZ3), new SimpleDateFormat(ServiceMap.dateFormatTZ4),
          new SimpleDateFormat(ServiceMap.dateFormatTZ5), new SimpleDateFormat(ServiceMap.dateFormatTZ6)};
        Gson gson = new Gson();                
        if(!fromAggregation) {
          do {
            for(SearchHit h:hits) { 
              Map<String, Object> d = h.getSourceAsMap();

              String dts = (String)d.get("date_time");
              Date dt = null;
              ParseException exc = null;
              for(DateFormat dtf: dateFormatterTZ) {
                try {
                  dt = dtf.parse(dts);
                } catch(ParseException e) {
                  exc = e;
                  continue;
                }
                break;
              }
              if(dt == null) {
                    ServiceMap.notifyException(exc, "date: "+dts+" suri:"+serviceUri);
                    throw exc;
              }
              
              String uuid = (String)d.get("uuid");

              /*int offset=TimeZone.getDefault().getOffset(dt.getTime());
              dt.setTime(dt.getTime()-offset);*/

              Object vn = d.get("value_name");

              if(conf.get("elasticSearchCheckDuplicates", "false").equals("true") && rt.has(vn.toString()) && dt.equals(cdt)) {
                //ServiceMap.notifyException(null, "WARNING ESearch "+serviceUri+" duplicate value name "+vn+" @ "+dts);
                ServiceMap.println("WARNING ESearch "+serviceUri+" duplicate value name "+vn+" @ "+dts);
                continue;
              }

              Object value = d.get("value");
              Object valueOut = value;
              //ServiceMap.println("value:"+value);
              if(value==null) {
                value = d.get("value_str");
                if(value!=null)
                  valueOut = "\""+JSONObject.escape(value.toString())+"\"";
                else {
                  value = d.get("value_obj");
                  if(value!=null) {
                    if(conf.get("elasticSearchValueObjAsString", "false").equals("true"))
                      valueOut = value = gson.toJson(value);
                    else {
                      value = gson.toJson(value);
                      valueOut = "\""+JSONObject.escape(value.toString())+"\"";
                    }
                  }
                  else {
                    valueOut = "\"\"";
                    value = "";
                  }
                }
              } else {
                valueOut = "\""+value+"\"";
              }
              //ServiceMap.println(dts+" "+vn+":"+value);

              ServiceMap.println(dateFormatterTZ[0].format(dt)+" "+uuid+" "+vn+" "+value);
              
              if(cdt==null || ( cdt.equals(dt) &&
                      ( conf.get("elasticSearchAggrByUUID","false").equals("false") || cuuid.equals(uuid) ) ) ) {
                if(c==0) {
                  String measuredTime = dateFormatterTZ[0].format(dt);
                  if(out!=null)
                    out.print("  {\n  \"measuredTime\":{\"value\":\""+measuredTime+"\"},");
                  rt.addProperty("measuredTime", measuredTime);
                }
                else
                  if(out!=null) 
                    out.print(",");
                c++;
                cdt = dt;
                cuuid = uuid;
              } else {
                if(fromTime==null && rtData.size()>=limit-1) //prende i primi valori
                  break;
                cdt = dt;
                cuuid = uuid;
                rtData.add(rt);
                rt = new JsonObject();
                String measuredTime = dateFormatterTZ[0].format(dt);
                if(out!=null)
                  out.print("},  {\n  \"measuredTime\":{\"value\":\""+measuredTime+"\"},");
                rt.addProperty("measuredTime", measuredTime);
                c=1;
              }
              if(out!=null)
                out.print("  \""+vn+"\":{\"value\":"+valueOut+"}");
              rt.addProperty(vn.toString(),value.toString());
            }
            if(conf.get("elasticSearchScrollSearch","false").equals("true")) {
              SearchScrollRequest scrollRequest = new SearchScrollRequest(r.getScrollId()); 
              scrollRequest.scroll(TimeValue.timeValueMinutes(1));
              ServiceMap.println("elaticSearch scroll");
              r = client.scroll(scrollRequest, RequestOptions.DEFAULT);
              hits = r.getHits().getHits();
              ServiceMap.println("elaticSearch scroll found "+hits.length+" hits");
            } else 
              break;
          } while(hits.length > 0);
          rtData.add(rt);
        } else {
          Histogram agg = r.getAggregations().get("date_time");

          List<? extends Histogram.Bucket> l = agg.getBuckets();
          ListIterator<? extends Histogram.Bucket> li = l.listIterator(l.size());
          while(li.hasPrevious()) {
            Histogram.Bucket entry = li.previous();
            org.elasticsearch.search.aggregations.metrics.avg.Avg avg = entry.getAggregations().get("avg");
            //System.out.println(" "+entry.getKey()+" "+entry.getDocCount()+" "+avg.getValueAsString());
            String value = avg.getValueAsString();
            if(!value.equals("Infinity")) {
              if(c!=0 && out!=null)
                out.println("},");
              rt = new JsonObject();
              Date measuredTime = dateFormatterTZ[0].parse(entry.getKeyAsString());
              if(out!=null) out.print("{\n\"measuredTime\":{\"value\":\""+dateFormatterTZ[0].format(measuredTime)+"\"},");
              rt.addProperty("measuredTime", dateFormatterTZ[0].format(measuredTime));
              if(out!=null) out.print(" \""+valueName+"\":{\"value\":\""+value+"\"}");
              rt.addProperty(valueName, value);
              rtData.add(rt);
              c++;
            }
          }
          /*for (Histogram.Bucket entry : agg.getBuckets()) {
            org.elasticsearch.search.aggregations.metrics.avg.Avg avg = entry.getAggregations().get("avg");
            System.out.println(" "+entry.getKey()+" "+entry.getDocCount()+" "+avg.getValueAsString());
          }*/       
        }
        String customAttrLastValues = "";
        if(fromTime == null) {
          try(Connection rtCon = ServiceMap.getRTConnection()) {
            if(rtCon!=null) {
              for(String a:customAttrs) {
                if(valueName==null || a.equals(valueName)) {
                  JsonArray cdata = new JsonArray();
                  ServiceValuesServlet.getValues(rtCon, conf, null, toTime, "1", serviceUri, a, cdata, null);
                  if(cdata.size()>0) {
                    customAttrLastValues += ",\""+a+"\":"+cdata.get(0);
                    if(rtData.size()>0)
                      rtData.get(0).getAsJsonObject().add(a, cdata.get(0));
                  }
                }
              }
              if(out!=null)
                out.print(customAttrLastValues);
            }
          }
        }
        if(c!=0 && out!=null)
          out.println(" }");
        if(out!=null)
          out.println("]}}");
      }
    } finally {
      client.close();
    }
  }
  
  private Connection timeTrendQuery(ServiceMapping.MappingData md, String serviceUri, JspWriter out, Connection rtCon, Configuration conf) throws NumberFormatException, Exception, IOException, SQLException {
    long ts = System.currentTimeMillis();
    String query = md.trendSqlQuery;
    query = query.replace("%SERVICE_URI", ServiceMap.urlEncode(serviceUri));
    ServiceMap.println("trend query: "+query);
    out.println(", \"trends\": [");
    if(rtCon==null)
      rtCon = ServiceMap.getRTConnection();
    if(rtCon==null)
      return null;
    Statement s = rtCon.createStatement();
    s.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
    ResultSet rs = s.executeQuery(query);
    int nCols = rs.getMetaData().getColumnCount();
    int n=0;
    while(rs.next()) {
      out.println(n>0?",{":"{");
      for(int c=1; c<=nCols; c++) {
        String label = rs.getMetaData().getColumnLabel(c);
        String value = rs.getString(c);
        //ServiceMap.println(label+": "+value);
        out.println((c>1 ? "," : "")+"\""+label+"\":\""+value+"\"");
      }
      out.println("}");
      n++;
    }
    s.close();
    out.println("]");
    ServiceMap.performance("phoenix time trend: "+(System.currentTimeMillis()-ts)+"ms "+serviceUri);
    return rtCon;
  }

  private Connection predictionQuery(ServiceMapping.MappingData md, String serviceUri, JspWriter out, Connection rtCon, Configuration conf) throws IOException, SQLException, Exception, NumberFormatException {
    long ts = System.currentTimeMillis();
    String query = md.predictionSqlQuery;
    query = query.replace("%SERVICE_URI", ServiceMap.urlEncode(serviceUri));
    ServiceMap.println("prediction query: "+query);
    out.println(", \"predictions\": [");
    rtCon = ServiceMap.getRTConnection();
    if(rtCon==null)
      return null;
    Statement s = rtCon.createStatement();
    s.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
    ResultSet rs = s.executeQuery(query);
    int nCols = rs.getMetaData().getColumnCount();
    int n=0;
    while(rs.next()) {
      out.println(n>0?",{":"{");
      for(int c=1; c<=nCols; c++) {
        String label = rs.getMetaData().getColumnLabel(c);
        String value = rs.getString(c);
        //ServiceMap.println(label+": "+value);
        out.println((c>1 ? "," : "")+"\""+label+"\":\""+value+"\"");
      }
      out.println("}");
      n++;
    }
    s.close();
    out.println("]");
    ServiceMap.performance("phoenix time prediction: "+(System.currentTimeMillis()-ts)+"ms "+serviceUri);
    return rtCon;
  }

  private void realTimeSparqlQuery(ServiceMapping.MappingData md, JsonObject rtAttributes,  ArrayList<String> customAttrs, String serviceUri, String valueName, String fromTime, String toTime, int limit, Configuration conf, RepositoryConnection con, String sparqlType, JspWriter out, JsonArray rtData) throws RepositoryException, QueryEvaluationException, MalformedQueryException, IOException, NumberFormatException {
    //get RT data from SPARQL
    String query = md.realTimeSparqlQuery;
    query = query.replace("%SERVICE_URI", ServiceMap.urlEncode(serviceUri));
    String frmTime = "", tTime = "";
    if(toTime!=null) {
      tTime = " FILTER(?measuredTime<=\""+toTime+ServiceMap.getCurrentTimezoneOffset()+"\"^^xsd:dateTime) ";
    }
    if(fromTime!=null) {
      frmTime = " FILTER(?measuredTime>=\""+fromTime+ServiceMap.getCurrentTimezoneOffset()+"\"^^xsd:dateTime) ";
      limit = Integer.parseInt(conf.get("fromTimeLimitSparql",conf.get("fromTimeLimit","1500")));
    }
    query = query.replace("%FROM_TIME", frmTime+tTime).replace("%LIMIT", ""+limit);
    TupleQuery tupleQueryRT = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
    long ts = System.nanoTime();
    long tss = System.currentTimeMillis();
    TupleQueryResult resultStatus = tupleQueryRT.evaluate();
    logQuery(query, "API-service-rt-info", sparqlType, serviceUri, System.nanoTime() - ts);
    //ServiceMap.println(query);
    out.println(",\"realtime\": ");
    if (resultStatus.hasNext()) {    
      List<String> vars = resultStatus.getBindingNames();
      out.print("{ \"head\": {\n"
              + " \"vars\":[ ");
      int c = 0;
      for(String v:vars) {
        if(!v.startsWith("_")) {
          if(c!=0)
            out.print(",");
          out.print("\""+v+"\"");
          c++;
        }
      }
      out.println("]},");
      out.println(" \"results\": {");
      out.println(" \"bindings\": [");
      
      int p = 0;
      while (resultStatus.hasNext()) {
        BindingSet bindingSet = resultStatus.next();
        
        if (p != 0) {
          out.println(",");
        }
        
        out.println("  {");
        c=0;
        JsonObject rt = new JsonObject();
        for(String v : vars) {
          if(!v.startsWith("_")) {
            String value = "";
            if (bindingSet.getValue(v) != null) {
              value = bindingSet.getValue(v).stringValue();
              //ServiceMap.println("v:"+v+" -->"+value);
            }
            if(c!=0)
              out.println(",");
            out.print("  \""+v+"\":{\"value\":\""+JSONObject.escape(value)+"\"}");
            rt.addProperty(v,value);
            c++;
          }
        }
        if(fromTime == null) {
          try(Connection rtCon = ServiceMap.getRTConnection()) {
            if(rtCon!=null) {
              String customAttrLastValues = "";
              for(String a:customAttrs) {
                if(valueName==null || a.equals(valueName)) {
                  JsonArray cdata = new JsonArray();            
                  ServiceValuesServlet.getValues(rtCon, conf, null, toTime, "1", serviceUri, a, cdata, null);
                  if(cdata.size()>0) 
                    customAttrLastValues += ",\""+a+"\":"+cdata.get(0);
                }
              }
              out.print(customAttrLastValues);
            }
          } catch (Exception ex) {
            ServiceMap.notifyException(ex);
          }
        }
        out.println(" }");
        
        p++;
        rtData.add(rt);
      }
      out.println("]}}");
    }
    else
      out.println("{}");
    ServiceMap.performance("sparql time realtime: "+(System.currentTimeMillis()-tss)+"ms "+serviceUri+" from:"+fromTime+" to:"+toTime);
  }

  private void realTimeSqlQuery(ServiceMapping.MappingData md, JsonObject rtAttributes, ArrayList<String> customAttrs, String serviceUri, String valueName, String fromTime, String toTime, int limit, Configuration conf, List<String> serviceTypes, JspWriter out, JsonArray rtData) throws NumberFormatException, IOException {    
    // get RT data from phoenix
    boolean isWeatherSensor = false;
    String query = md.realTimeSqlQuery;
    long ts = System.currentTimeMillis();
    query = query.replace("%SERVICE_URI", ServiceMap.urlEncode(serviceUri));
    String serviceId = serviceUri.substring(serviceUri.lastIndexOf("/")+1);
    query = query.replace("%SERVICE_ID", serviceId);
    String frmTime = "";
    String tTime = "";
    if(toTime!=null) {
      tTime = " AND observationTime<=to_date('"+toTime.replace("T"," ")+"',null,'CET') ";
    }
    if(fromTime!=null) {
      frmTime = " AND observationTime>=to_date('"+fromTime.replace("T"," ")+"',null,'CET') ";
      limit = Integer.parseInt(conf.get("fromTimeLimitSql",conf.get("fromTimeLimit","1500")));
    } else if(serviceTypes.contains("Weather_sensor")) {
      if(limit==1)
        limit = 2;
      isWeatherSensor = true;
    }
    query = query.replace("%FROM_TIME", frmTime+tTime).replace("%LIMIT", ""+limit);
    ServiceMap.println("realtime query: "+query);
    if(out!=null)
      out.println(", \"realtime\": ");
    if(conf.get("disablePhoenixQuery", "false").equals("true")) {
      if(out!=null)
        out.println("{\"error\":\"problems with phoenix\"}");
      ServiceMap.notifyException(null,"ERROR PHOENIX DISABLED "+serviceUri);
      return;
    }
    try {
      Connection rtCon = ServiceMap.getRTConnection();
      if(rtCon==null) {
        throw new Exception("missing hbase phoenix connection");
      }
      //Connection rtCon2 = ServiceMap.getRTConnection2();
      Statement s = rtCon.createStatement();
      s.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
      ResultSet rs = s.executeQuery(query);
      int nCols = rs.getMetaData().getColumnCount();
      int p = 0;
      String customAttrLastValues = "";
      JsonObject customAttrLastValuesObj = new JsonObject();
      if(fromTime == null) {
        for(String a:customAttrs) {
          if(valueName==null || a.equals(valueName)) {
            JsonArray cdata = new JsonArray();            
            ServiceValuesServlet.getValues(rtCon, conf, null, toTime, "1", serviceUri, a, cdata, null);
            if(cdata.size()>0) {
              customAttrLastValues += ",\""+a+"\":"+cdata.get(0);
              customAttrLastValuesObj.add(a, cdata.get(0));
            }
          }
        }
      }
      if(!isWeatherSensor) {
        while (rs.next()) {
          if (p == 0 && out!=null) {
            out.print("{ \"head\": {\n"
                    + " \"vars\":[ ");
            int cc = 0;
            for(int c=1; c<=nCols; c++) {
              String v = rs.getMetaData().getColumnLabel(c);
              if(!v.startsWith("_") && (valueName == null || v.equals(valueName) || v.equals("measuredTime"))) {
                if(cc!=0)
                  out.print(",");
                out.print("\""+v+"\"");
                cc++;
              }
            }
            for(String a:customAttrs) {
              if(valueName == null || a.equals(valueName)) {
                if(cc!=0)
                  out.print(",");
                out.print("\""+a+"\"");
                cc++;
              }
            }
            out.println("]},");
            out.println(" \"results\": {");
            out.println(" \"bindings\": [");
          } else if(out!=null) {
            out.println(",");
          }
          if(out!=null)
            out.println("  {");
          int cc = 0;
          JsonObject rt = new JsonObject();
          for(int c=1; c<=nCols; c++) {
            String v = rs.getMetaData().getColumnLabel(c);
            if(!v.startsWith("_") && (valueName == null || v.equals(valueName) || v.equals("measuredTime"))) {
              String value = rs.getString(c);
              if(value!=null && rs.getMetaData().getColumnType(c)==java.sql.Types.DATE ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIMESTAMP ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIME)
                value=value.replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
              if(value==null)
                value = "";
              //ServiceMap.println("v:"+v+" -->"+value);
              if(out!=null) {
                if(cc!=0)
                  out.println(",");
                out.print("  \""+v+"\":{\"value\":\""+JSONObject.escape(value)+"\"}");
              }
              rt.addProperty(v, value);
              cc++;
            }
          }
          if(out!=null) {
            out.println(customAttrLastValues);
            out.println(" }");
          }
          p++;
          rtData.add(rt);
        }
      } else {
        //FIX per problema con weather sensor
        String[] row = new String[nCols];
        while (rs.next()) {
          if (p == 0 && out!=null) {
            out.print("{ \"head\": {\n"
                    + " \"vars\":[ ");
            int cc = 0;
            for(int c=1; c<=nCols; c++) {
              String v = rs.getMetaData().getColumnLabel(c);
              if(!v.startsWith("_") && (valueName==null || v.equals(valueName) || v.equals("measuredTime"))) {
                if(cc!=0)
                  out.print(",");
                out.print("\""+v+"\"");
                cc++;
              }
            }
            for(String a:customAttrs) {
              if(valueName == null || a.equals(valueName)) {
                if(cc!=0)
                  out.print(",");
                out.print("\""+a+"\"");
                cc++;
              }
            }
            out.println("]},");
            out.println(" \"results\": {");
            out.println(" \"bindings\": [");
          }
          for(int c=1; c<=nCols; c++) {
            String v = rs.getMetaData().getColumnLabel(c);
            if(!v.startsWith("_")) {
              String value = rs.getString(c);
              if(value!=null && rs.getMetaData().getColumnType(c)==java.sql.Types.DATE ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIMESTAMP ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIME)
                value=value.replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
              if(row[c-1]==null)
                row[c-1] = value;
            }
          }
          p++;
        }
        if(p>0) {
          int cc=0;
          JsonObject rt = new JsonObject();
          if(out!=null)
            out.println(" {");
          for(int c=1; c<=nCols; c++) {
            String v = rs.getMetaData().getColumnLabel(c);
            if(!v.startsWith("_") && (valueName==null || v.equals(valueName))) {
              String value=row[c-1];
              if(value==null)
                value="";
              if(out!=null) {
                if(cc!=0)
                  out.println(",");
                out.print("  \""+v+"\":{\"value\":\""+JSONObject.escape(value)+"\"}");
              }
              rt.addProperty(v,value);
              cc++;
            }
          }
          if(out!=null) {
            out.println(customAttrLastValues);
            out.println(" }");
          }
          rtData.add(rt);
        }
      }
      if(out!=null) {
        if(p>0)
          out.println("]}}");
        else
          out.println("{}");
      }
      rtCon.close();      
      //rtCon2.close();
      
      //add custom values to rtData[0]
      if(rtData.size()>0) {
        for(Map.Entry<String, JsonElement> x:customAttrLastValuesObj.entrySet()) {
          rtData.get(0).getAsJsonObject().add(x.getKey(), x.getValue());
        }
      }
      ServiceMap.performance("phoenix time realtime: "+(System.currentTimeMillis()-ts)+"ms "+serviceUri+" from:"+fromTime+" to:"+toTime);
    } catch(PhoenixIOException e) {
      if(out!=null) out.println("{\"error\":\"problems with phoenix\"}");
      ServiceMap.notifyException(e);
      conf.getSet("disablePhoenixQuery", "true");
    }catch(Exception e) {
      if(out!=null) out.println("{}");
      ServiceMap.notifyException(e);
    }
  }

  private String lastRealTimeValue(String serviceUri, String valueName, String fromTime, String apiKey, RepositoryConnection con) {
    try {
      ArrayList<String> serviceTypes = ServiceMap.getTypes(con, serviceUri, true, apiKey);
      ServiceMapping.MappingData md = ServiceMapping.getInstance().getMappingForServiceType(1, serviceTypes);
      if (md == null) {
        return "{\"error\":\"mapping not found\"}";
      }
      Configuration conf = Configuration.getInstance();

      serviceUri = ServiceMapping.getInstance().getServiceUriAlias(serviceUri);
      JsonObject rtAttributes = md.getServiceAttributes(con, serviceUri);
      JsonArray rtData = new JsonArray();
      if (valueName != null && rtAttributes.has(valueName)
              && rtAttributes.getAsJsonObject(valueName).get("attr_type") != null
              && rtAttributes.getAsJsonObject(valueName).get("attr_type").getAsString().equals("CustomAttribute")) {
        // is stored with values
        Connection rtCon = ServiceMap.getRTConnection2();
        if (rtCon != null) {
          ServiceValuesServlet.getValues(rtCon, null, null, null, "1", serviceUri, valueName, rtData, "sparql");
          rtCon.close();
        }
      } else {
        ArrayList<String> customAttrs = new ArrayList<>();
        if(rtAttributes!=null) {
          for(Map.Entry<String,JsonElement> e:rtAttributes.entrySet()) {
            if(e.getValue().getAsJsonObject().has("attr_type") && e.getValue().getAsJsonObject().get("attr_type").getAsString().equals("CustomAttribute") )
              customAttrs.add(e.getKey());
          }
        }
        if (md.realTimeSqlQuery != null && md.realTimeSolrQuery == null) {
          realTimeSqlQuery(md, rtAttributes, customAttrs, serviceUri, valueName, fromTime, null, 1, conf, serviceTypes, null, rtData);
        } else if (md.realTimeSparqlQuery != null && md.realTimeSolrQuery == null) {
          realTimeSparqlQuery(md, rtAttributes, customAttrs, serviceUri, valueName, fromTime, null, 1, conf, con, conf.get("sparqlType", "virtuoso"), null, rtData);
        } else if (md.realTimeSolrQuery != null) {
          if (md.realTimeSolrQuery.equals("true") || md.realTimeSolrQuery.equals("solr")) {
            realTimeSolrQuery(conf, rtAttributes, customAttrs, serviceUri, valueName, fromTime, null, 1, null, rtData);
          } else if (md.realTimeSolrQuery.equals("elasticsearch")) {
            realTimeElasticSearchQuery(conf, rtAttributes, customAttrs, serviceUri, valueName, fromTime, null, -1, null, rtData);
          }
        }
      }
      if(rtData.size()>=1) {
        return rtData.get(0).toString();
      }
      return "{}";
    } catch(Exception e) {
      ServiceMap.notifyException(e);
      return "{\"error\":\"exception\"}";
    }
  }
  
  public void queryEvent(JspWriter out, RepositoryConnection con, String idService, String lang, String uid) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    idService = ServiceMap.urlEncode(idService);
    int i = 0;
    String queryService = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX schema:<http://schema.org/>"
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>"
            + "SELECT ?elat ?elong ?name1 ?name2 ?note ?description1 ?description2 ?website ?address ?number ?prov ?city ?startDate ?endDate ?startTime ?categ1 ?categ2 ?multimedia WHERE{"
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
            + " OPTIONAL {<" + idService + "> km4c:multimediaResource ?multimedia}"
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
      String valueOfMultimedia = "";
      if (bindingSetService.getValue("multimedia") != null) {
        valueOfMultimedia = bindingSetService.getValue("multimedia").stringValue();
      }

      NOS = valueOfName;

      if(conf.get("normalizeNotes", "false").equals("true")) {
        Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 \\.,:;|]+", "");
      }

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
              + "    \"multimedia\": \"" + escapeJSON(valueOfMultimedia) + "\", "
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

  public int queryEventList(JspWriter out, RepositoryConnection con, final String range, final String[] coords, final String dist, String numEv, final String textFilter, String lang, boolean photos) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String eventRange = range;
    String numEventi = numEv;
    int i = 0;
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    Date data_attuale = new Date();
    final String data_inizio;
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
    } else {
      data_inizio = df.format(data_attuale);
    }
    String args = "";
    if (coords != null) {
      for (String c : coords) {
        args += c + ";";
      }
    }
    args += range + ";" + dist + ";" + numEv + ";" + textFilter;

    if(lang==null)
      lang = conf.get("eventsLang", "it");
    if(lang.equals("any"))
      lang = null;
    final String flang = ServiceMap.stringEncode(lang);
    final String limit;
    if (numEventi != null && !numEventi.equals("0")) {      
      limit = " LIMIT " + Integer.parseInt(numEventi);
    } else {
      limit = "";
    }

    SparqlQuery queryEvList = new SparqlQuery() {
      @Override
      public String query(String type, String geoMode) throws Exception {
        return "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> \n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
            + "PREFIX schema:<http://schema.org/> \n"
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/> \n"
            + "SELECT DISTINCT ?ev ?elat ?elong  ?nameLang ?place (substr(str(?sDate), 1,10) AS ?startDate)  (substr(str(?eDate), 1,10) AS ?endDate) ?time ?cost ?identifier ?catLang ?catEng ?civic ?address ?price ?website ?phone ?descLang ?descEng ?multimedia ?x WHERE{\n"
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
            + ServiceMap.geoSearchQueryFragment("?ev", coords, dist, geoMode)
            + " ?ev geo:lat ?elat; \n"
            + "   geo:long ?elong. \n"
            + " ?ev schema:name ?nameLang. "+ (flang!=null ? "FILTER(LANG(?nameLang)= \""+flang+"\")\n" : "\n")
            + " ?ev km4c:placeName ?place. \n"
            + " ?ev schema:startDate ?sDate. FILTER (?sDate <= \"" + data_inizio + "\"^^xsd:date). \n"
            //+ " OPTIONAL {"
            + " ?ev schema:endDate ?eDate. FILTER (xsd:date(?eDate) >= \"" + data_fine + "\"^^xsd:date).\n"
            //+ " } \n"
            + " OPTIONAL {?ev km4c:eventTime ?time.} \n"
            + " OPTIONAL {?ev km4c:freeEvent ?cost.} \n"
            + " OPTIONAL {?ev km4c:eventCategory ?catLang. "+(flang!=null ? "FILTER(LANG(?catLang)= \""+flang+"\")" : "") + "}\n"
            + " OPTIONAL {?ev km4c:eventCategory ?catEng. FILTER(LANG(?catEng)= \"en\")}\n"
            + " OPTIONAL {?ev km4c:houseNumber ?civic.} \n"
            + " OPTIONAL {?ev schema:streetAddress ?address.} \n"
            + " OPTIONAL {?ev schema:price ?price.} \n"
            + " OPTIONAL {?ev schema:description ?descLang. "+ (flang!=null ? "FILTER(LANG(?descLang)= \""+flang+"\")" : "") + "}\n"
            + " OPTIONAL {?ev schema:description ?descEng. FILTER(LANG(?descEng)= \"en\")}\n"
            + " OPTIONAL {?ev schema:url ?website.} \n"
            + " OPTIONAL {?ev schema:telephone ?phone.} \n"
            + " OPTIONAL {?ev km4c:multimediaResource ?multimedia.} \n"
            + "} ORDER BY asc(?sDate)"
            + limit;
      }
    };
           
    //ServiceMap.println(queryEvList);
    out.println("{ \"Event\":"
            + "{\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    
    TupleQueryResult resultEvList = ServiceMap.geoSparqlQuery(con, queryEvList, null, "API-event-list", args);

    Map<String,String> processed=new HashMap<String,String>();
    while (resultEvList.hasNext()) {
      BindingSet bindingSetEvList = resultEvList.next();
      String valueOfName = "";
      if (bindingSetEvList.getValue("nameLang") != null) {
        valueOfName = bindingSetEvList.getValue("nameLang").stringValue();
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

      String valueOfCatLang = "";
      if (bindingSetEvList.getValue("catLang") != null) {
        valueOfCatLang = bindingSetEvList.getValue("catLang").stringValue();
      }

      String valueOfCatEng = "";
      if (bindingSetEvList.getValue("catEng") != null) {
        valueOfCatEng = bindingSetEvList.getValue("catEng").stringValue();
      }
      
      String valueOfPrice = "";
      if (bindingSetEvList.getValue("price") != null) {
        valueOfPrice = bindingSetEvList.getValue("price").stringValue();
      }

      String valueOfDescLang = "";
      if (bindingSetEvList.getValue("descLang") != null) {
        valueOfDescLang = bindingSetEvList.getValue("descLang").stringValue();
      }

      String valueOfDescEng = "";
      if (bindingSetEvList.getValue("descEng") != null) {
        valueOfDescEng = bindingSetEvList.getValue("descEng").stringValue();
      }
      
      String valueOfWebsite = "";
      if (bindingSetEvList.getValue("website") != null) {
        valueOfWebsite = bindingSetEvList.getValue("website").stringValue();
      }

      String valueOfPhone = "";
      if (bindingSetEvList.getValue("phone") != null) {
        valueOfPhone = bindingSetEvList.getValue("phone").stringValue();
      }

      String valueOfMultimedia = "";
      if (bindingSetEvList.getValue("multimedia") != null) {
        valueOfMultimedia = bindingSetEvList.getValue("multimedia").stringValue();
      }
      
      String serviceType = "Event";
      String llang = "";
      if(lang!=null) {
        llang=lang.toUpperCase();
      }

      /*Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
       valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 \\.,:;]+", "");*/
      if ((!valueOfEDate.equals("")) || (valueOfEDate.equals("") && (valueOfSDate.equals(data_inizio)))) {
        if(processed.containsKey(valueOfName))
          continue;
        if (i != 0) {
          out.println(", ");
        }
        String tt = "";
        /*if(parseTime) {
          String[] times = ServiceMap.findTime(valueOfTime);
          if(times[0]!=null) {
            tt = "    \"startsAtTime\": \""+times[0]+"\",\n";
          }
          if(times[1]!=null) {
            tt += "    \"endsAtTime\": \""+times[1]+"\",\n";
          }
        }*/

        out.println("{ "
                + " \"geometry\": {\n"
                + "     \"type\": \"Point\",\n"
                + "    \"coordinates\": [ " + valueOfELong + "," + valueOfELat + "]\n"
                + " },\n"
                + " \"type\": \"Feature\",\n"
                + " \"properties\": {\n"
                + "    \"serviceUri\": \"" + escapeJSON(valueOfIdentifier) + "\",\n"
                + "    \"name\": \"" + escapeJSON(valueOfName) + "\",\n"
                + "    \"tipo\": \"event\",\n"
                + "    \"place\": \"" + escapeJSON(valueOfPlace) + "\",\n"
                + "    \"startDate\": \"" + escapeJSON(valueOfSDate) + "\",\n"
                + "    \"startTime\": \"" + escapeJSON(valueOfTime) + "\",\n"
                + tt
                + "    \"endDate\": \"" + escapeJSON(valueOfEDate) + "\",\n"
                + "    \"freeEvent\": \"" + escapeJSON(valueOfCost) + "\",\n"
                + "    \"address\": \"" + escapeJSON(valueOfAddress) + "\",\n"
                + "    \"civic\": \"" + escapeJSON(valueOfCivic) + "\",\n"
                + "    \"categoryEN\": \"" + escapeJSON(valueOfCatEng) + "\",\n"
                + (!llang.equals("EN") ? "    \"category"+llang+"\": \"" + escapeJSON(valueOfCatLang) + "\",\n" : "")
                + "    \"price\": \"" + escapeJSON(valueOfPrice) + "\",\n"
                + "    \"phone\": \"" + escapeJSON(valueOfPhone) + "\",\n"
                + "    \"descriptionEN\": \"" + escapeJSON(valueOfDescEng) + "\",\n"
                + (!llang.equals("EN") ? "    \"description"+llang+"\": \"" + escapeJSON(valueOfDescLang) + "\",\n" : "")
                + "    \"website\": \"" + escapeJSON(valueOfWebsite) + "\",\n"
                + "    \"multimedia\": \"" + escapeJSON(valueOfMultimedia) + "\",\n"
                + (photos ? "    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfIdentifier,"thumbs") + ",\n" : "")
                + "    \"serviceType\": \"" + serviceType + "\"\n"
                + "}, "
                + "\"id\": " + Integer.toString(i + 1) + "  "
                + "}");
        i++;
        processed.put(valueOfName, valueOfIdentifier);
      }
    }
    out.println("] }}");
    return i;
  }

  public int queryTplLatLng(JspWriter out, RepositoryConnection con, String[] coords, String dist, String agency, String numLinee, boolean getPolyline) throws Exception {
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
      queryNearTPL += " LIMIT " + Integer.parseInt(numLinee);
    }
    //ServiceMap.println(queryNearTPL);
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
    return i;
  }

  public int queryBusStopsOfLine(JspWriter out, RepositoryConnection con, String nomeLinea, String codRoute, boolean getGeometry) throws Exception {
    int i=0;
    if (codRoute != null && codRoute.startsWith("http://")) {
      if(CheckParameters.checkUri(codRoute)!=null)
        throw new IllegalArgumentException("invalid URI");
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
              + " <" + ServiceMap.urlEncode(codRoute) + "> opengis:hasGeometry ?shape;\n"
              + " gtfs:route ?r.\n"
              + " OPTIONAL {?r gtfs:shortName ?line.}\n"
              + " ?r gtfs:longName ?nomeLinea;\n"
              + "  gtfs:agency ?ag.\n"
              + " ?ag foaf:name ?agname.\n"
              + (getGeometry ? " ?shape opengis:asWKT ?polyline.\n" : "")
              + "} LIMIT 1";
      //ServiceMap.println(routeQuery);
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
          if (bindingSet.getValue("line") != null) {
            valueOfLineNumber = JSONObject.escape(bindingSet.getValue("line").stringValue());
          }
          if (bindingSet.getValue("ag") != null) {
            valueOfAgUri = bindingSet.getValue("ag").stringValue();
            valueOfAgName = JSONObject.escape(bindingSet.getValue("agname").stringValue());
          }
          out.println("\"lineNumber\":\"" + valueOfLineNumber + "\",\"lineName\":\"" + valueOfLineName + "\""
                  + (getGeometry ? ",\"wktGeometry\":\"" + ServiceMap.fixWKT(valueOfGeometry) + "\"" : ""));
        }
      } catch (Exception e) {
        ServiceMap.notifyException(e);
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
              + " ?st gtfs:trip <" + ServiceMap.urlEncode(codRoute) + ">.\n"
              + " ?st gtfs:stop ?bs.\n"
              + " ?bs a ?type. FILTER(?type!=gtfs:Stop)"
              + " ?st gtfs:stopSequence ?ss.\n"
              + " ?bs geo:lat ?bslat.\n"
              + " ?bs geo:long ?bslong.\n"
              + " ?bs foaf:name ?nomeFermata.\n"
              + "} ORDER BY ASC(xsd:int(?ss))";
      //ServiceMap.println(queryString);

      tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      ts = System.nanoTime();
      result = tupleQuery.evaluate();
      logQuery(queryString, "get-bus-stops-of-line-3", "any", nomeLinea, System.nanoTime() - ts);
      out.println("\"BusStops\":{\n"
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      try {
        while (result.hasNext()) {
          BindingSet bindingSet = result.next();
          String valueOfBS = bindingSet.getValue("bs").stringValue();
          String valueOfNomeFermata = JSONObject.escape(bindingSet.getValue("nomeFermata").stringValue());
          String valueOfBSLat = bindingSet.getValue("bslat").stringValue();
          String valueOfBSLong = bindingSet.getValue("bslong").stringValue();
          String valueOfType = bindingSet.getValue("type").stringValue();
          valueOfType = valueOfType.substring(valueOfType.lastIndexOf("#") + 1);

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
                  + "\"serviceType\": \"TransferServiceAndRenting_" + valueOfType + "\" "
                  + "}, "
                  + "\"id\": " + Integer.toString(i + 1) + " "
                  + "}");
          i++;
        }
      } catch (Exception e) {
        out.println(e.getMessage());
      }

      out.println("]}}");
      return i;
    } else {
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
                  + " ?tpll dcterms:identifier \"" + ServiceMap.stringEncode(nomeLinea) + "\".\n"
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
          //ServiceMap.println(queryAllBusStops);
          out.println("{"
                  + "\"type\": \"FeatureCollection\","
                  + "\"features\": [");
          try {
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
                + " ?route dcterms:identifier \"" + ServiceMap.stringEncode(codRoute) + "\".\n"
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
          ServiceMap.notifyException(e);
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
                + " ?route dcterms:identifier \"" + ServiceMap.stringEncode(codRoute) + "\".\n"
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
    return i;
  }

  public int queryTplLastPosition(JspWriter out, String agency, String line, RepositoryConnection con) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String agencyUri = null;
    if(agency == null || agency.trim().isEmpty()) {
      agencyUri = conf.get("defaultTplAgencyUri", "http://www.disit.org/km4city/resource/Bus_ataflinea_Agency_777-08");
      agency = null;
    }
    else if(agency.startsWith("http://")) {
      if(CheckParameters.checkUri(agency)!=null)
        throw new IllegalArgumentException("agency invalid URI");
      agencyUri = agency;
      agency = null;
    }
    String lineUri = null;
    if(line!=null && line.startsWith("http://")) {
      if(CheckParameters.checkUri(line)!=null)
        throw new IllegalArgumentException("line invalid URI");
      lineUri = line;
      line = null;
      agencyUri = null;
      agency = null;
    } else if(line!=null && line.trim().isEmpty()) {
      line = null;
    }
    
    boolean predictPosition = false;
    if(conf.get("enableRtTplLinePosition","true").equalsIgnoreCase("true") && (lineUri!=null || line!=null) )
      predictPosition = true;
    if(conf.get("enableRtTplAgPosition","false").equalsIgnoreCase("true") && (lineUri==null && line==null) )
      predictPosition = true;
    
    String  queryText = "select ?trip ?rName ?lat ?long ?stopName (?busid as ?v) ?bsFirst ?bsLast ?prev ?delay (str(xsd:time(now())) as ?time) ?agName {\n" +
              "{select ?trip max(?at2) as ?prev {\n" +
              "?x1 a gtfs:StopTime.\n" +
              "?x1 gtfs:arrivalTime ?at1.\n" +
              "?x1 gtfs:trip ?trip.\n" +
              "?x2 a gtfs:StopTime.\n" +
              "?x2 gtfs:arrivalTime ?at2.\n" +
              "?x2 gtfs:trip ?trip.\n" +              
              (agencyUri!=null ? "?trip gtfs:route ?r.\n?r gtfs:agency <"+ServiceMap.urlEncode(agencyUri)+">.\n" : "")+
              (agency!=null ? "?trip gtfs:route ?r.\n?r gtfs:agency/foaf:name \""+ServiceMap.stringEncode(agency)+"\".\n" : "")+
              ((agencyUri!=null || agency!=null) && line!=null ? "?r gtfs:shortName \""+ServiceMap.stringEncode(line)+"\".\n" : "")+
              (lineUri!=null ? "?trip gtfs:route <"+ServiceMap.urlEncode(lineUri)+">.\n" : "")+
              "?trip gtfs:service/dcterms:date ?d.\n" +
              "filter(?d=SUBSTR(STR(xsd:date(now())),1,10) && str(?at1)>str(xsd:time(now())) && str(?at2)<str(xsd:time(now())))\n" +
              "} group by ?trip\n" +
              "}\n" +
              "?trip gtfs:route ?r.\n" +
              "?trip gtfs:headsign ?bsLast.\n" +
              "?trip dcterms:identifier ?busid.\n" +
              //"?x a gtfs:StopTime.\n" +
              "?x gtfs:trip ?trip.\n" +
              "?x gtfs:arrivalTime ?prev.\n" +
              "?x gtfs:stop ?stop.\n" +
              "?x gtfs:stop ?stop.\n" +
              "?stop geo:lat ?lat.\n" +
              "?stop geo:long ?long.\n" +
              "?stop foaf:name ?stopName.\n" +
              "?r gtfs:shortName ?rName.\n" +
              "?r gtfs:agency/foaf:name ?agName.\n" +
              //"?fs a gtfs:StopTime.\n" +
              "?fs gtfs:trip ?trip.\n" +
              "?fs gtfs:stopSequence ?ss. FILTER(xsd:int(?ss)=1)\n" +
              "?fs gtfs:stop/foaf:name ?bsFirst.\n" +
              "bind(bif:dateDiff('minute',xsd:time(?prev),xsd:time(now())) as ?delay)" +
              "} order by ?v";

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
    tupleQuery.setMaxQueryTime(40000);
    long startTime = System.nanoTime();
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(queryText, "TPLRT", sparqlType, agencyUri+";"+line, System.nanoTime() - startTime);
    int i = 0;
    out.println("{ "
            + "\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    String prevBusNumber = "";
    while (result.hasNext()) {
      BindingSet bindingSet = result.next();
      //String serviceUri = bindingSet.getValue("ser").stringValue();
      String trip = "";
      if (bindingSet.getValue("trip") != null) {
        trip = bindingSet.getValue("trip").stringValue();
      }

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
        busNumber = busNumber.substring(busNumber.lastIndexOf('_')+1);
      }

      String delay = "";
      if (bindingSet.getValue("delay") != null) {
        delay = bindingSet.getValue("delay").stringValue();
      }
      
      String time = "";
      if (bindingSet.getValue("time") != null) {
        time = bindingSet.getValue("time").stringValue();
        time = time.split("\\+")[0]; //remove timezone
      }
      
      String agencyName = "";
      if (bindingSet.getValue("agName") != null) {
        agencyName = bindingSet.getValue("agName").stringValue();
      }

      if(!busNumber.equals(prevBusNumber)) {
        if (i != 0) {
          out.println(", ");
        }
        if(predictPosition) {
          try {
            long sst= System.nanoTime();
            TplLocation loc = new TplLocation(trip, con);
            Coordinate pos = loc.currentLoc(time);
            long sse = System.nanoTime();
            ServiceMap.performance("tpllocation "+trip+" "+time+" time: "+(sse-sst)/1000000+"ms");
            serviceLat = pos.y+"";
            serviceLong = pos.x+"";
          } catch(Exception e) {
            ServiceMap.notifyException(e,"TplLocation: "+trip+" "+time);
          }
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
                + "\"agency\": \"" + JSONObject.escape(agencyName) + "\", "
                + "\"direction\": \"" + JSONObject.escape(bsFirst) + " &#10132; " + JSONObject.escape(bsLast) + "\", "
                + "\"tipo\": \"RealTimeInfo\", "
                + "\"serviceUri\": \"busCode" + JSONObject.escape(busNumber) + "\", "
                + "\"detectionTime\": \"" + delay + "\", "
                + "\"serviceType\": \"bus_real_time\", "
                + "\"tripUri\": \""+trip+"\", "
                + "\"time\": \""+time+"\" "
                + "}, "
                + "\"id\": " + Integer.toString(i + 1) + "  "
                + "}\n");
        i++;
      }
      prevBusNumber = busNumber;
    }
    out.println("] "
            + "}");
    return i;
  }
  
  public int queryBusesLastPosition(JspWriter out, RepositoryConnection con) throws Exception {
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
              "filter(?d=SUBSTR(STR(xsd:date(now())),1,10) && str(?at1)>str(xsd:time(now())) && str(?at2)<str(xsd:time(now())))\n" +
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
    tupleQuery.setMaxQueryTime(40000);
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
    return i;
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
              + "Validate it on " + baseApiUrl.replace("/api/", "/") + "comments.jsp", null);
    } catch (SQLException ex) {
      ServiceMap.notifyException(ex);
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
      ServiceMap.notifyException(ex);
    } finally {
      connection.close();      
    }
  }
  
  public void queryLastContribution(JspWriter out, int n, String lang, String apikey) throws Exception {
    String baseApiUrl = Configuration.getInstance().get("baseApiUrl", "");
    out.println("{\n"
            + "\"LastPhotos\": [");
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("SELECT serviceUri,serviceName,file,timestamp FROM ServicePhoto WHERE status='validated' ORDER BY id DESC LIMIT "+n);
      ResultSet r = st.executeQuery();
      int c=0;
      while(r.next()) {
        String uri = r.getString("serviceUri");
        String name = r.getString("serviceName");
        String timestamp = r.getString("timestamp");
        String file = r.getString("file");
        Map<String, String> sinfo = ServiceMap.getServiceInfo(uri, lang, apikey);
        if(!sinfo.isEmpty()) {
          if(c++ >= 1)
            out.println(",");
          out.print("  {\"serviceUri\":\""+uri+"\", "+ServiceMap.map2Json(sinfo)+" \"photo\":\""+baseApiUrl+"v1/photo/"+file+"\", \"photoThumb\":\""+baseApiUrl+"v1/photo/thumbs/"+file+"\", \"photoOrig\":\""+baseApiUrl+"v1/photo/originals/"+file+"\",\"timestamp\":\""+timestamp+"\"}");
        }
      }
      st.close();
      out.println("],\n"
              + "\"LastComments\":[");
      st = connection.prepareStatement("SELECT serviceUri,serviceName,comment,timestamp FROM ServiceComment WHERE status='validated' ORDER BY id DESC LIMIT "+n);
      r = st.executeQuery();
      c = 0;
      while(r.next()) {
        String uri = r.getString("serviceUri");
        String name = r.getString("serviceName");
        String comment = r.getString("comment");
        String timestamp = r.getString("timestamp");
        Map<String, String> sinfo = ServiceMap.getServiceInfo(uri, lang, apikey);
        if(!sinfo.isEmpty()) {
          if(c++ >= 1)
            out.println(",");
          out.print("  {\"serviceUri\":\""+uri+"\", "+ServiceMap.map2Json(sinfo)+" \"comment\":\""+escapeJSON(comment)+"\", \"timestamp\":\""+timestamp+"\"}");
        }
      }
      st.close();
      out.println("],\n"
              + "\"LastStars\":[");
      st = connection.prepareStatement("SELECT serviceUri,stars,timestamp FROM ServiceStars ORDER BY timestamp DESC LIMIT "+n);
      r = st.executeQuery();
      c = 0;
      while(r.next()) {
        String uri = r.getString("serviceUri");
        String stars = r.getString("stars");
        String timestamp = r.getString("timestamp");
        Map<String, String> sinfo = ServiceMap.getServiceInfo(uri, lang, apikey);
        if(!sinfo.isEmpty()) {
          if(c++ >= 1)
            out.println(",");
          out.print("  {\"serviceUri\":\""+uri+"\", "+ServiceMap.map2Json(sinfo)+" \"stars\":"+stars+", \"timestamp\":\""+timestamp+"\"}");
        }
      }
      st.close();
    } catch (SQLException ex) {
      ServiceMap.notifyException(ex);
    } finally {
      connection.close();      
    }
    out.println("]}");
  }

  public TupleQueryResult queryBusLines(String busStop, RepositoryConnection con) {
      String queryForLine = "";
      if(busStop!=null && busStop.startsWith("http://")) {
        if(CheckParameters.checkUri(busStop)!=null) 
          throw new IllegalArgumentException("invalid URI");
       queryForLine = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>"
              + "select distinct ?id ?line ?desc ?ag ?agname where {\n"
              + "{?st gtfs:stop [owl:sameAs <"+ServiceMap.urlEncode(busStop)+">]\n"
              + "} UNION {\n"
              + "?st gtfs:stop <"+ServiceMap.urlEncode(busStop)+">\n"
              + "}\n"
              + "?st gtfs:trip ?trip.\n"
              + "?trip gtfs:route ?line.\n"
              + "?line gtfs:longName ?desc.\n"
              + "OPTIONAL {?line gtfs:shortName ?id}.\n"
              + "?line gtfs:agency ?ag.\n"
              + "?ag foaf:name ?agname.\n"
              + "} ORDER BY ?id ";
      } else
       queryForLine = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>"
              + "select distinct ?id where {\n"
              + "?st gtfs:stop [ foaf:name \""+ServiceMap.stringEncode(busStop)+"\" ].\n"
              + "?st gtfs:trip ?trip.\n"
              + "?line gtfs:longName ?desc.\n"
              + "OPTIONAL {?line gtfs:shortName ?id}.\n"
              + "} ORDER BY ?id ";

      TupleQuery tupleQueryForLine;
      try {
          tupleQueryForLine = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForLine);
          //ServiceMap.println(queryForLine);
          //long ts = System.nanoTime();
          TupleQueryResult result = tupleQueryForLine.evaluate();
          //ServiceMap.logQuery(queryForLine,"API-buslines","",busStop,System.nanoTime()-ts);
          return result;
      } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
          Logger.getLogger(ServiceMapApi.class.getName()).log(Level.SEVERE, null, ex);
      }
  return null;    
  }
  
  public int queryTplAgencyList(JspWriter out, RepositoryConnection con, String selection) throws Exception {
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    final String km4cVersion = conf.get("km4cVersion", "new");

    String areas = conf.get("tplAgenciesAreas", null);
    if(areas!=null && selection!=null) {
      String[] aa = areas.trim().split(",");
      String[] latLng = selection.split(";");
      double lat = Double.parseDouble(latLng[0]);
      double lng = Double.parseDouble(latLng[1]);

      for(String a: aa) {
        String[] coords = a.trim().split(";");
        if(coords.length>=4) {
          double lat1=Double.parseDouble(coords[0]);
          double lng1=Double.parseDouble(coords[1]);
          double lat2=Double.parseDouble(coords[2]);
          double lng2=Double.parseDouble(coords[3]);
          if(lat>=lat1 && lat<=lat2 && lng>=lng1 && lng<=lng2) {
            selection=null; //return all agencies on the store
            break;
          }
        }
      }
    }
    String queryForAgencies;
    if(selection == null)
      queryForAgencies= "SELECT DISTINCT ?ag ?name WHERE {"
            + " ?ag a gtfs:Agency."
            + " ?ag foaf:name ?name."
            + "} ORDER BY ?name";
    else {
      String[] latLng = selection.split(";");
      queryForAgencies = "select distinct ?ag ?name {\n" +
            "{\n" +
            "select ?ag (str(max(?lat)) as ?mxlat) (str(min(?lat)) as ?mnlat) (str(max(?lng)) as ?mxlng) (str(min(?lng)) as ?mnlng) {\n" +
            "?s a gtfs:Stop.\n" +
            "?s gtfs:agency ?ag.\n" +
            "?s geo:lat ?lat.\n" +
            "?s geo:long ?lng.\n" +
            "} group by ?ag\n" +
            "}\n" +
            "?ag foaf:name ?name.\n" +
//            "bind(bif:st_geomfromtext(concat(\"POLYGON((\",?mnlng,\" \",?mnlat,\",\",?mnlng,\" \",?mxlat,\",\",?mxlng,\" \",?mxlat,\",\",?mxlng,\" \",?mnlat,\",\",?mnlng,\" \",?mnlat,\"))\")) as ?geo)\n" +
            "filter(bif:st_intersects(bif:st_point("+latLng[1]+","+latLng[0]+"),bif:st_geomfromtext(concat(\"POLYGON((\",?mnlng,\" \",?mnlat,\",\",?mnlng,\" \",?mxlat,\",\",?mxlng,\" \",?mxlat,\",\",?mxlng,\" \",?mnlat,\",\",?mnlng,\" \",?mnlat,\"))\")),1))\n" +
            "} ORDER BY ?name";
    }
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
    return n;
  }
  
  public void makeShortestPath(JspWriter out, RepositoryConnection con, String[] srcLatLng, String[] dstLatLng, String datetime, String routeType, String maxFeet) throws Exception { 
    Configuration conf = Configuration.getInstance();
    if(datetime==null) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      datetime = sdf.format(new Date());
    }
    String transport = "any";
    if(routeType==null || routeType.equals("feet") || routeType.equals("foot_shortest"))
      routeType = "shortest_foot_optimization";
    else if(routeType.equals("foot_quiet"))
      routeType = "quietest_foot_optimization";
    else if(routeType.equals("car")) {
      routeType = "fastest_car_optimization";
      transport = "car";
    }
    else if(routeType.equals("public_transport"))
      routeType = "public_transit_optimization";
    else
      throw new IllegalArgumentException("invalid route type parameter");
    if(maxFeet==null)
      maxFeet = "0.1";
    
    String srcnode="",dstnode="", nid;
    if(conf.get("useInternalOsm","true").equals("true")) {
      if(srcLatLng.length>=2) {
        nid = ServiceMap.getOsmNodeId(srcLatLng[0], srcLatLng[1], transport);
        if(nid!=null)
          srcnode = "\"lat\":"+srcLatLng[0]+",\"lon\":"+srcLatLng[1]+",\"node_id\":\""+nid+"\"";
      } else
        srcnode = "\"lat\":0,\"lon\":0,\"node_id\":\""+srcLatLng[0]+"\"";
      if(dstLatLng.length>=2) {
        nid = ServiceMap.getOsmNodeId(dstLatLng[0], dstLatLng[1], transport);
        if(nid!=null)
          dstnode = "\"lat\":"+dstLatLng[0]+",\"lon\":"+dstLatLng[1]+",\"node_id\":\""+nid+"\"";      
      } else {
          dstnode = "\"lat\":0,\"lon\":0,\"node_id\":\""+dstLatLng[0]+"\"";              
      }
    } else {
      srcnode = "\"lat\":"+srcLatLng[0]+",\"lon\":"+srcLatLng[1];      
      dstnode = "\"lat\":"+dstLatLng[0]+",\"lon\":"+dstLatLng[1];      
    }
    String JSON="{\"message_version\":\"1.0\",\n"+
      "\"journey\":{\n"+
      "\"search_route_type\": \""+routeType+"\",\n"+
      "\"search_max_feet_km\":"+maxFeet+",\n"+
      "\"start_datetime\":\""+datetime+"\",\n"+
      "\"source_node\":{"+srcnode+"},\n"+
      "\"destination_node\":{"+dstnode+"}\n"+
      "}}";
    
    HttpClient httpclient = HttpClients.createDefault();
    HttpPost httppost = null;
    HttpResponse response = null;
    
    String passengerEndpoint = conf.get("passengerEndpoint", "http://192.168.0.24:8080/api/shortest_path");

    //ServiceMap.println("POST to "+theReqUrl+" query="+query);
    try {
      long start = System.currentTimeMillis();
      httppost = new HttpPost(passengerEndpoint);
      httppost.setHeader("Content-Type", "application/json");

      HttpEntity postParams = new StringEntity(JSON,ContentType.create("application/json", "UTF-8"));
      httppost.setEntity(postParams);

      // Create a response handler
      response = httpclient.execute(httppost);
      
      int statusCode = response.getStatusLine().getStatusCode();
      if(statusCode == 200) {
        String resultJson = EntityUtils.toString(response.getEntity());
        JSONParser parser = new JSONParser();
        JSONObject r = (JSONObject)parser.parse(new StringReader(resultJson));
        JSONObject resp = (JSONObject) r.get("response");
        if(resp.get("error_code").equals("0")) { //route found
          JSONObject journey = (JSONObject) r.get("journey");
          JSONArray routes= (JSONArray)journey.get("routes");
          if(routes.size()>0 && routeType.equals("public_transit_optimization")) {
            if(conf.get("shortestPathMultimodalRemoveOnlyOnFoot","false").equals("true"))
              routes.remove(0); //remove the foot only route
            else if(routes.size()>1) {
              routes.add(routes.remove(0)); //put as last choice
            }
          }
          for(int i=0; i<routes.size(); i++) {
            JSONObject route=(JSONObject)routes.get(i);
            JSONArray arcs=(JSONArray)route.get("arc");
            String wkt="LINESTRING(";
            String startTime="", endTime="";
            for(int j=0;j<arcs.size();j++) {
              JSONObject arc=(JSONObject) arcs.get(j);
              JSONObject src = (JSONObject)arc.get("source_node");
              wkt += src.get("lon") + " " + src.get("lat") + ",";
              if(j==0)
                startTime = (String)arc.get("start_datetime");
              else if(j==arcs.size()-1)
                endTime = (String)arc.get("start_datetime");
              if(arc.get("transport").equals("public transport")) {
                String agency_id = (String)arc.get("transport_provider");                                
                JSONObject dst = (JSONObject)arc.get("destination_node");
                                
                Pattern p = Pattern.compile("([^:]+):([^/]+)/([^\\[]+)\\[([^\\]]+)\\]\\[([^\\]]+)\\]\\[([^\\]]+)\\]:([^,]+),(.+)");
                Matcher m = p.matcher((String) arc.get("desc"));
                if(!m.matches()) {
                  throw new Exception("invalid desc \""+arc.get("desc")+"\"");
                }
                String routeShortName = m.group(1).trim();
                String headsign = m.group(3).trim();
                String time = (String) arc.get("start_datetime");
                String date = datetime.split("T")[0];
                String tripId =  m.group(4).trim();
                String srcStopId =  m.group(7).trim();
                String dstStopId =  m.group(8).trim();
                
                arc.put("desc",routeShortName+" : "+headsign);
                arc.put("trip_id", tripId);
                
                ServiceMap.println("line:"+routeShortName+" headsign:"+headsign+" trip:"+tripId+" from:"+srcStopId+" to:"+dstStopId);
                
                JSONObject srcStop = ServiceMap.getStopFromId(con, srcStopId, agency_id);
                String srcStopUri = (String)srcStop.get("stop_uri");
                src.put("stop_uri", srcStop.get("stop_uri"));
                src.put("stop_name", srcStop.get("stop_name"));
                src.put("stop_type", srcStop.get("serviceType"));
                arc.put("transport_provider_name", srcStop.get("agencyName"));
                
                JSONObject dstStop = ServiceMap.getStopFromId(con, dstStopId, agency_id);
                String dstStopUri = (String)dstStop.get("stop_uri");
                dst.put("stop_uri", dstStop.get("stop_uri"));
                dst.put("stop_name", dstStop.get("stop_name"));
                dst.put("stop_type", dstStop.get("serviceType"));

                arc.put("desc",routeShortName+" : "+srcStop.get("stop_name")+" - "+dstStop.get("stop_name"));
                
                JSONObject stops = ServiceMap.getStopsFromTo(con, srcStopUri, dstStopUri, tripId, time, date);
                arc.put("stops", stops);
                
                Object stopsWKT = stops.remove("wkt");
                if(!stopsWKT.equals("")) {
                  arc.put("wkt", "LINESTRING("+stopsWKT+")");
                  wkt += stopsWKT+",";
                }                        
              }
            }
            if(arcs.size()>0) {
              JSONObject arc=(JSONObject) arcs.get(arcs.size()-1);
              JSONObject dst = (JSONObject)arc.get("destination_node");
              wkt += dst.get("lon") + " " + dst.get("lat") + ")";
              route.put("wkt",wkt);
            }

            if(!endTime.equals("")) {
              SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
              Date date1 = format.parse(startTime);
              Date date2 = format.parse(endTime);
              long adjust = 0;
              if(startTime.compareTo(endTime)>0) {
                adjust = 1000*60*60*24; //add one day if startTime>endTime
              }
              long millis = date2.getTime() - date1.getTime() + adjust;
              long second = (millis / 1000) % 60;
              long minute = (millis / (1000 * 60)) % 60;
              long hour = (millis / (1000 * 60 * 60)) % 24;
              String timeTaken = String.format("%02d:%02d:%02d", hour, minute, second);
              route.put("time", timeTaken);
            }
          }
        } else { //no route or failure
          ServiceMap.logNoRoute(r);
        }
        long time=System.currentTimeMillis()-start;
        r.put("elapsed_ms", time);
        out.print(r.toString());
      } else {
        String result = EntityUtils.toString(response.getEntity());
        //out.println("{ errorcode: "+statusCode+" }");
        ServiceMap.logNoRoute(statusCode+"-"+JSON);
        ServiceMap.println("FAILED shorttest path "+statusCode+"-"+JSON+"-"+result);
        throw new Exception("Failed shortest path "+JSON);
      }
    } finally {
      httpclient.getConnectionManager().closeExpiredConnections();
      httpclient.getConnectionManager().shutdown();
    }    
  }
  
  public int queryLocationSearch(JspWriter out, String search, String searchMode, String position, String maxDists, boolean excludePOI, String categories, String maxResults, boolean sortByDist) throws Exception {
    Configuration conf = Configuration.getInstance();
    String urlString = conf.get("solrKm4cIndexUrl", "http://192.168.0.207:8983/solr/km4c-index");
    String fuzzyMatch = conf.get("solrFuzzyValue", "0.7");
    String exactMatch = conf.get("solrExactMatch", "0");
    String prefixMatch = conf.get("solrPrefixMatch", "1");
    SolrClient solr = new HttpSolrClient(urlString);
    int i=0;
    try {
      String tilde = "~";
      String[] s=search.split("[^0-9A-Za-z']+");
      String ss = "";
      boolean haveNumber = false;
      for(String x:s) {
        if(x.isEmpty())
          continue;
        if(i>0 && "AND".equalsIgnoreCase(searchMode))
          ss+="AND ";
        if(x.matches("\\d+[^0-9]*")) {
          haveNumber=true;
          ss+=x;
        }
        else /*if(i==s.length-1)
          ss+="*";
        else*/
          if(!exactMatch.equals("0"))
            ss+="("+x+"^"+exactMatch+" OR "+x+tilde+fuzzyMatch+")";
          else if(!prefixMatch.equals("0"))
            ss+="("+x+"*^"+prefixMatch+" OR "+x+tilde+fuzzyMatch+")";
          else
            ss+="("+x+tilde+fuzzyMatch+")";
        ss+=" ";
        i++;
      }
      //ServiceMap.println(ss);
      SolrQuery query = new SolrQuery(ss);
      //ServiceMap.println(position);
      if(position!=null && !position.isEmpty()) {
        if(CheckParameters.checkLatLng(position)!=null) {
          throw new IllegalArgumentException("invalid position "+position);
        }
        position=position.replace(";",",");
        query.setParam("pt", position);
        query.setParam("sfield", "geo_coordinate_p");
        if(maxDists!=null && !maxDists.isEmpty())
          query.addFilterQuery("{!geofilt d="+Double.parseDouble(maxDists)+"}");
        if(sortByDist) {
          //ServiceMap.println("sort by distance");
          query.addSort("geodist()",SolrQuery.ORDER.asc);
        }
      }
      if(categories!=null && !categories.isEmpty()) {
        String[] cts = categories.split(";");
        String q = "";
        for(int j=0; j<cts.length; j++) {
          if(j>0)
            q += " OR ";
          if(cts[j].trim().equals("StreetNumber") || cts[j].trim().equals("Municipality"))
            q += "entityType_s:\"" + ServiceMap.stringEncode(cts[j].trim()) +"\"";
          else
            q += "entityType_txt:\"" + ServiceMap.stringEncode(cts[j].trim()) +"\"";
        }
        //ServiceMap.println(q);
        query.addFilterQuery(q);
      } else if(excludePOI)
        query.addFilterQuery("entityType_s:StreetNumber OR entityType_s:Municipality");
      query.setRows(Integer.parseInt(maxResults));
      query.addField("id");
      query.addField("entityType_s");
      query.addField("geo_coordinate_p");
      query.addField("roadName_s_lower");
      query.addField("streetNumber_s");
      query.addField("municipalityName_s_lower");
      query.addField("name_s_lower");
      query.addField("score");
      QueryResponse qr=solr.query(query);
      SolrDocumentList sdl=qr.getResults();
      long nfound=sdl.getNumFound();

      out.println("{\"type\": \"FeatureCollection\",\n"
              + "\"count\": "+nfound+",\n"
              + "\"features\": [ ");
      i=0;
      for(SolrDocument d: sdl) {
        String id = (String)d.getFieldValue("id");
        String type = (String)d.getFieldValue("entityType_s");
        String geo_coordinate = (String)d.getFieldValue("geo_coordinate_p");
        String[] g=geo_coordinate.split(",");
        if(i!=0)
          out.println(",");
        String name=null, roadName=null, streetNumber=null, municipalityName=null;
        if(type.equals("StreetNumber")) {
          roadName = (String)d.getFieldValue("roadName_s_lower");
          streetNumber = (String)d.getFieldValue("streetNumber_s");
          municipalityName = (String)d.getFieldValue("municipalityName_s_lower");
        } else if(type.equals("Municipality")) {
          municipalityName = (String)d.getFieldValue("municipalityName_s_lower");
        } else {
          name = escapeJSON((String)d.getFieldValue("name_s_lower"));
          municipalityName = (String)d.getFieldValue("municipalityName_s_lower");
        }
        if(municipalityName==null)
          municipalityName = "";
        Object score = d.getFieldValue("score");
        out.println("{  \"geometry\": {\n" +
            "     \"type\": \"Point\",\n" +
            "    \"coordinates\": [ "+ g[1]+","+g[0] +"]\n" +
            " },\n" +
            " \"type\": \"Feature\",\n" +
            " \"properties\": {\n" +
            "    \"serviceUri\": \"" + id + "\",\n" +
            "    \"serviceType\": \"" + type.replace(" ", "_") + "\",\n" +
            (name==null ?         "" : "    \"name\": \""+name+"\",\n") +
            (roadName==null ?     "" : "    \"address\": \""+roadName+"\",\n") +
            (streetNumber==null ? "" : "    \"civic\": \""+streetNumber+"\",\n") +
            "    \"city\": \""+municipalityName+"\",\n" +
            "    \"score\": \""+score+"\",\n" +
            " \"id\": " + Integer.toString(i + 1) + "}}");
        i++;
      }
      out.println("]}");
    } finally {
      solr.shutdown();
    }
    return i;
  }

  public void queryNextPOS(JspWriter out, RepositoryConnection con, String range, final String[] coords, String categories, final String dist, final String textFilter, boolean photos) throws Exception {
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    boolean searchEvents = false;
    List<String> listaCategorieServizi = new ArrayList<String>();
    if (categories != null) {
      String[] arrayCategorie = categories.split(";");
      // GESTIONE CATEGORIE
      listaCategorieServizi = new ArrayList(Arrays.asList(arrayCategorie));
      if(listaCategorieServizi.contains("Event")) {
        searchEvents = true;
        listaCategorieServizi.remove("Event");
      }
    }
    //ServiceMap.println("nextPOS");
    out.println("{\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    int i = 0;
    
    String[] km4cPositions = conf.get("km4cAppPositions", "11.2555,43.7730;11.2494,43.7803").split(";");
    String km4cAppDescIT = conf.get("km4cAppDescIT", "la soluzione Km4City http://www.km4city.org/app ti ha fornito i dati e ti aiuta nei movimenti e a trovare ciò che cerchi, dagli eventi ai mezzi pubblici, dalla benzina ai parcheggi, e molto altro.");
    String km4cAppDescEN = conf.get("km4cAppDescEN", "Km4City solution http://www.km4city.org/app gave you the information and helps you moving in the city and finding what you are looking for, from events to public  transport, from fuel stations to parking, and much more.");

    for(String p:km4cPositions) {
      if(i!=0)
        out.println(",");
      if(!p.trim().isEmpty()) {
        out.println("{  \"geometry\": {\n" +
            "     \"type\": \"Point\",\n" +
            "    \"coordinates\": [ "+ p +"]\n" +
            " },\n" +
            " \"type\": \"Feature\",\n" +
            " \"properties\": {\n" +
            "    \"serviceType\": \"Event\",\n" +
            "    \"name\": \"Firenze, dove, cosa\",\n" +
            "    \"nameEN\": \"Firenze, where, what\",\n" +
            "    \"startDate\": \"2017-01-01\",\n" +
            "    \"endDate\": \"2050-01-01\",\n" +
            "    \"startTime\": \"00:00 - 23:59\",\n" +
            "    \"fromTime\": \"00:00\",\n" +
            "    \"toTime\": \"23:59\",\n" +
            "    \"freeEvent\": \"YES\",\n" +
            "    \"price\": \"\",\n" +
            "    \"place\": \"\",\n" +
            "    \"address\": \"\",\n" +
            "    \"civic\": \"\",\n" +
            "    \"city\": \"Firenze\",\n" +
            "    \"province\": \"FI\",\n" +
            "    \"categoryIT\": \"Applicazione\",\n" +
            "    \"categoryEN\": \"Application\",\n" +
            "    \"phone\": \"\",\n" +
            "    \"descriptionIT\": \""+km4cAppDescIT+"\",\n" +
            "    \"descriptionEN\": \""+km4cAppDescEN+"\",\n" +
            "    \"website\": \"www.km4city.org/app/\"},\n" +
            " \"id\": " + Integer.toString(i + 1) + "}");
        i++;
      }
    }
    
    if(searchEvents) {
      String eventRange = range;
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
      Date data_attuale = new Date();
      String data_inizio = df.format(data_attuale);
      String data_fine = df.format(data_attuale);
      if (eventRange.equals("week")) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(data_attuale);
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        data_inizio = df.format(calendar.getTime());
      } else if (eventRange.equals("15-days")) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(data_attuale);
        calendar.add(Calendar.DAY_OF_YEAR, 15);
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
      args += range + ";" + dist + ";" + textFilter;

      String queryEvList = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> \n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
              + "PREFIX schema:<http://schema.org/> \n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> \n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/> \n"
              + "SELECT DISTINCT ?ev ?elat ?elong  ?nameIta ?nameEng ?place (substr(str(?sDate), 1,10) AS ?startDate)  (substr(str(?eDate), 1,10) AS ?endDate) ?time ?cost ?identifier ?catIta ?catEng ?civic ?address ?city ?province ?price ?website ?phone ?descIta ?descEng ?x WHERE{\n"
              + " ?ev rdf:type km4c:Event. \n"
              + " ?ev dcterms:identifier ?identifier. \n"
              + ServiceMap.textSearchQueryFragment("?ev", "?p", textFilter)
              + " ?ev geo:lat ?elat; \n"
              + "   geo:long ?elong. \n"
              + " ?ev schema:name ?nameIta. FILTER(LANG(?nameIta)= \"it\") \n"
              + " OPTIONAL {?ev schema:name ?nameEng. FILTER(LANG(?nameEng)= \"en\")}\n"
              + " ?ev km4c:placeName ?place. \n"
              + " ?ev schema:startDate ?sDate. FILTER (?sDate <= \"" + data_inizio + "\"^^xsd:date). \n"
              //+ " OPTIONAL {"
              + " ?ev schema:endDate ?eDate. FILTER (xsd:date(?eDate) >= \"" + data_fine + "\"^^xsd:date).\n"
              //+ " } \n"
              + " ?ev km4c:eventTime ?time. \n"
              + " ?ev km4c:freeEvent ?cost. \n"
              + " ?ev km4c:eventCategory ?catIta. FILTER(LANG(?catIta)= \"it\") \n"
              + " ?ev km4c:eventCategory ?catEng. FILTER(LANG(?catEng)= \"en\") \n"
              + " OPTIONAL {?ev schema:streetAddress ?address.} \n"
              + " OPTIONAL {?ev km4c:houseNumber ?civic.} \n"
              + " OPTIONAL {?ev schema:addressLocality ?city.} \n"
              + " OPTIONAL {?ev schema:addressRegion ?province.} \n"
              + " OPTIONAL {?ev schema:price ?price.} \n"
              //+ ServiceMap.geoSearchQueryFragment("?ev", coords, dist)
              + " OPTIONAL {?ev schema:description ?descIta. FILTER(LANG(?descIta)= \"it\")} \n"
              + " OPTIONAL {?ev schema:description ?descEng. FILTER(LANG(?descEng)= \"en\")} \n"
              + " OPTIONAL {?ev schema:url ?website.} \n"
              + " OPTIONAL {?ev schema:telephone ?phone.} \n"
              + "} ORDER BY asc(?sDate)";

      //ServiceMap.println(queryEvList);

      long ts = System.nanoTime();
      TupleQuery tupleQueryEvList = con.prepareTupleQuery(QueryLanguage.SPARQL, queryEvList);
      TupleQueryResult resultEvList = tupleQueryEvList.evaluate();
      logQuery(queryEvList, "API-nextpos-event-list", sparqlType, args, System.nanoTime() - ts);

      Map<String,String> processed=new HashMap<String,String>();
      while (resultEvList.hasNext()) {
        BindingSet bindingSetEvList = resultEvList.next();
        String valueOfName = "";
        if (bindingSetEvList.getValue("nameIta") != null) {
          valueOfName = bindingSetEvList.getValue("nameIta").stringValue();
        }
        String valueOfNameEng = "";
        if (bindingSetEvList.getValue("nameEng") != null) {
          valueOfNameEng = bindingSetEvList.getValue("nameEng").stringValue();
        }

        String valueOfELat = bindingSetEvList.getValue("elat").stringValue();
        String valueOfELong = bindingSetEvList.getValue("elong").stringValue();
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

        }
        String valueOfEDate = "";
        if (bindingSetEvList.getValue("endDate") != null) {
          valueOfEDate = bindingSetEvList.getValue("endDate").stringValue();
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
        String valueOfCity = "";
        if (bindingSetEvList.getValue("city") != null) {
          valueOfCity = bindingSetEvList.getValue("city").stringValue();
        }
        String valueOfProvince = "";
        if (bindingSetEvList.getValue("province") != null) {
          valueOfProvince = bindingSetEvList.getValue("province").stringValue();
        }
        String valueOfCatIta = "";
        if (bindingSetEvList.getValue("catIta") != null) {
          valueOfCatIta = bindingSetEvList.getValue("catIta").stringValue();
        }
        String valueOfCatEng = "";
        if (bindingSetEvList.getValue("catEng") != null) {
          valueOfCatEng = bindingSetEvList.getValue("catEng").stringValue();
        }
        String valueOfPrice = "";
        if (bindingSetEvList.getValue("price") != null) {
          valueOfPrice = bindingSetEvList.getValue("price").stringValue();
        }
        String valueOfDescIta = "";
        if (bindingSetEvList.getValue("descIta") != null) {
          valueOfDescIta = bindingSetEvList.getValue("descIta").stringValue();
        }
        String valueOfDescEng = "";
        if (bindingSetEvList.getValue("descEng") != null) {
          valueOfDescEng = bindingSetEvList.getValue("descEng").stringValue();
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

        if ((!valueOfEDate.equals("")) || (valueOfEDate.equals("") && (valueOfSDate.equals(data_inizio)))) {
          if(processed.containsKey(valueOfName) || valueOfDescIta.contains("�") || valueOfTime.contains("�") || valueOfDescIta.contains("?") || valueOfName.contains("?"))
            continue;
          if (i != 0) {
            out.println(", ");
          }
          String tt = "";
          String[] times = ServiceMap.findTime(valueOfTime);
          if(times[0]!=null) {
            tt = "    \"fromTime\": \""+times[0]+"\",\n";
          }
          if(times[1]!=null) {
            tt += "    \"toTime\": \""+times[1]+"\",\n";
          }

          out.println("{ "
                  + " \"geometry\": {\n"
                  + "     \"type\": \"Point\",\n"
                  + "    \"coordinates\": [ " + valueOfELong + "," + valueOfELat + "]\n"
                  + " },\n"
                  + " \"type\": \"Feature\",\n"
                  + " \"properties\": {\n"
                  + "    \"serviceType\": \"" + serviceType + "\",\n"
                  + "    \"serviceUri\": \"" + valueOfIdentifier + "\",\n"
                  + "    \"name\": \"" + escapeJSON(valueOfName) + "\",\n"
                  + "    \"nameEN\": \"" + escapeJSON(valueOfNameEng) + "\",\n"
                  + "    \"startDate\": \"" + escapeJSON(valueOfSDate) + "\",\n"
                  + "    \"endDate\": \"" + escapeJSON(valueOfEDate) + "\",\n"
                  + "    \"startTime\": \"" + escapeJSON(valueOfTime) + "\",\n"
                  + tt
                  + "    \"freeEvent\": \"" + escapeJSON(valueOfCost) + "\",\n"
                  + "    \"price\": \"" + escapeJSON(valueOfPrice) + "\",\n"
                  + "    \"place\": \"" + escapeJSON(valueOfPlace) + "\",\n"
                  + "    \"address\": \"" + escapeJSON(valueOfAddress) + "\",\n"
                  + "    \"civic\": \"" + escapeJSON(valueOfCivic) + "\",\n"
                  + "    \"city\": \"" + escapeJSON(valueOfCity) + "\",\n"
                  + "    \"province\": \"" + escapeJSON(valueOfProvince) + "\",\n"
                  + "    \"categoryIT\": \"" + escapeJSON(valueOfCatIta) + "\",\n"
                  + "    \"categoryEN\": \"" + escapeJSON(valueOfCatEng) + "\",\n"
                  + "    \"phone\": \"" + escapeJSON(valueOfPhone) + "\",\n"
                  + "    \"descriptionIT\": \"" + escapeJSON(valueOfDescIta) + "\",\n"
                  + "    \"descriptionEN\": \"" + escapeJSON(valueOfDescEng) + "\",\n"
                  + "    \"website\": \"" + escapeJSON(valueOfWebsite) + "\""
                  + (photos ? ",\n    \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfIdentifier,"thumbs") + "\n" : "")
                  + "},\n"
                  + "\"id\": " + Integer.toString(i + 1) + "\n"
                  + "}");
          i++;
          processed.put(valueOfName, valueOfIdentifier);
        }
      }
    }

    if(!listaCategorieServizi.isEmpty()) {
      String fcc = "";
      try {
        fcc = ServiceMap.filterServices(listaCategorieServizi);
      } catch (Exception e) {
        ServiceMap.notifyException(e);
      }
      final String fc = fcc;
      String args = "";
      for (String c : coords) {
        args += c + ";";
      }
      args += categories + ";" + textFilter + ";" + dist;

      SparqlQuery queryNearServices = new SparqlQuery() {
        @Override
        public String query(String type, String geoMode) throws Exception {
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
                  + "SELECT DISTINCT ?ser ?serAddress ?elat ?elong ?sType ?sCategory ?sTypeIta ?sTypeEng (IF(?sName1,?sName1,?sName2) as ?sName) ?address ?number ?city ?prov ?cap ?phone ?fax ?email ?website ?note ?multimedia ?description1 ?description2 ?ag ?agname ?dist ?x WHERE {\n"
                  + " ?ser rdf:type km4c:Service" + (sparqlType.equals("virtuoso") ? " OPTION (inference \"urn:ontology\")" : "") + ".\n"
                  + " OPTIONAL {?ser <http://schema.org/name> ?sName1}\n"
                  + " OPTIONAL {?ser foaf:name ?sName2}\n"
                  + ServiceMap.textSearchQueryFragment("?ser", "?p", textFilter)
                  + " {\n"
                    + "  ?ser km4c:hasAccess ?entry.\n"
                    + "  ?entry geo:lat ?elat.\n"
                    + "  ?entry geo:long ?elong.\n"
                    + ServiceMap.geoSearchQueryFragment("?entry", coords, dist, geoMode)
                    + " } UNION {\n"
                    + "  ?ser geo:lat ?elat.\n"
                    + "  ?ser geo:long ?elong.\n"
                    + ServiceMap.geoSearchQueryFragment("?ser", coords, dist, geoMode)
                    + " }\n"
                  + fc
                  + " graph ?g {?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:BusStop && ?sType!=km4c:SensorSite)}\n"
                  + " ?sType rdfs:subClassOf* ?sCategory. FILTER(?sCategory != <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>)\n"
                  + " ?sCategory rdfs:subClassOf km4c:Service.\n"
                  + " ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\")\n"
                  + " ?sType rdfs:label ?sTypeEng. FILTER(LANG(?sTypeEng)=\"en\")\n"
                  + " OPTIONAL {?ser schema:streetAddress ?address.}\n"
                  + " OPTIONAL {?ser km4c:houseNumber ?number}.\n"
                  + " OPTIONAL {?ser dcterms:description ?description1}\n"
                  + " OPTIONAL {?ser dcterms:description ?description2 FILTER(?description2!=?description1)}\n"
                  + " OPTIONAL {?ser skos:note ?note}.\n"
                  + " OPTIONAL {?ser km4c:atecoCode ?ateco } FILTER(!BOUND(?ateco))\n"
                  + " OPTIONAL {?ser schema:email ?email }.\n"
                  + " OPTIONAL {?ser schema:faxNumber ?fax}\n"
                  + " OPTIONAL {?ser schema:telephone ?phone}\n"
                  + " OPTIONAL {?ser schema:addressRegion ?prov}\n"
                  + " OPTIONAL {?ser schema:addressLocality ?city}\n"
                  + " OPTIONAL {?ser schema:postalCode ?cap}\n"
                  + " OPTIONAL {?ser schema:url ?website}\n"
                  + " OPTIONAL {?ser km4c:multimediaResource ?multimedia}.\n"
                  //+ " OPTIONAL {?st gtfs:stop ?ser.\n"
                  //+ "     ?st gtfs:trip/gtfs:route/gtfs:agency ?ag.\n"
                  //+ "     ?ag foaf:name ?agname.}\n"
                  + "}"
                  + (type != null ? "}" : " ORDER BY ?dist");
        }
      };

      TupleQueryResult result = ServiceMap.geoSparqlQuery(con, queryNearServices, null, "API-nextpos-nearservices", args);
      
      Map<String,String> processed = new HashMap<>();
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String valueOfSer = bindingSet.getValue("ser").stringValue();
        if(processed.containsKey(valueOfSer))
          continue;

        String valueOfSTypeEng = "";
        if (bindingSet.getValue("sTypeEng") != null) {
          valueOfSTypeEng = bindingSet.getValue("sTypeEng").stringValue();
        }
        String valueOfSTypeIta = "";
        if (bindingSet.getValue("sTypeIta") != null) {
          valueOfSTypeIta = bindingSet.getValue("sTypeIta").stringValue();
        }

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
          valueOfSName = subCategory.toUpperCase();
        }
        valueOfSName = valueOfSName.replace("_", " ");

        String valueOfELat = bindingSet.getValue("elat").stringValue();
        String valueOfELong = bindingSet.getValue("elong").stringValue();      
        String valueOfAddress = "";
        if(bindingSet.getValue("address")!=null)
          valueOfAddress = bindingSet.getValue("address").stringValue();
        String valueOfNumber = "";
        if(bindingSet.getValue("number")!=null)
          valueOfNumber = bindingSet.getValue("number").stringValue();
        String valueOfCity = "";
        if(bindingSet.getValue("city")!=null)
          valueOfCity = bindingSet.getValue("city").stringValue();
        String valueOfProv = "";
        if(bindingSet.getValue("prov")!=null)
          valueOfProv = bindingSet.getValue("prov").stringValue();
        String valueOfCap = "";
        if(bindingSet.getValue("cap")!=null)
          valueOfCap = bindingSet.getValue("cap").stringValue();
        String valueOfPhone = "";
        if(bindingSet.getValue("phone")!=null)
          valueOfPhone = bindingSet.getValue("phone").stringValue();
        String valueOfFax = "";
        if(bindingSet.getValue("fax")!=null)
          valueOfFax = bindingSet.getValue("fax").stringValue();
        String valueOfEmail = "";
        if(bindingSet.getValue("email")!=null)
          valueOfEmail = bindingSet.getValue("email").stringValue();
        String valueOfWebsite = "";
        if(bindingSet.getValue("website")!=null)
          valueOfWebsite = bindingSet.getValue("website").stringValue();
        String valueOfNote = "";
        if(bindingSet.getValue("note")!=null)
          valueOfNote = bindingSet.getValue("note").stringValue();
        String valueOfDescription1 = "";
        if(bindingSet.getValue("description1")!=null)
          valueOfDescription1 = ServiceMap.replaceHTMLEntities(bindingSet.getValue("description1").stringValue());
        String valueOfDescription2 = "";
        if(bindingSet.getValue("description2")!=null)
          valueOfDescription2 = ServiceMap.replaceHTMLEntities(bindingSet.getValue("description2").stringValue());
        String valueOfMultimedia = "";
        if(bindingSet.getValue("multimedia")!=null)
          valueOfMultimedia = bindingSet.getValue("multimedia").stringValue();
        String valueOfAgUri = null, valueOfAgName = null;
        if(bindingSet.getValue("ag")!=null)
          valueOfAgUri = bindingSet.getValue("ag").stringValue();
        if(bindingSet.getValue("agname")!=null)
          valueOfAgName = bindingSet.getValue("agname").stringValue();

        String valueOfDescriptionIta = "", valueOfDescriptionEng = "";
        if(!valueOfDescription1.isEmpty() && valueOfDescription1.contains(" the ")) {
          valueOfDescriptionEng = valueOfDescription1;
          valueOfDescriptionIta = valueOfDescription2;
        } else {
          valueOfDescriptionIta = valueOfDescription1;
          valueOfDescriptionEng = valueOfDescription2;        
        }
        if(valueOfDescriptionIta.isEmpty() && valueOfNote.length()>50) {
          valueOfDescriptionIta = valueOfNote;
          valueOfNote = "";
        }

        if(valueOfDescriptionIta.contains("?") || valueOfSName.contains("?"))
          continue;
        if(valueOfDescriptionEng.contains("?"))
          valueOfDescriptionEng = "";

        String tt = "";
        String[] times=ServiceMap.findTime(valueOfNote);
        if(times[0]!=null) {
          tt = "   \"startTime\": \""+valueOfNote+"\",\n"
            +  "   \"fromTime\": \""+times[0]+"\",\n";
          if(times[1]!=null) {
            tt += "   \"toTime\": \""+times[1]+"\",\n";
          }
          valueOfNote = "";
        } else {
          tt = "   \"fromTime\": \"00:00\",\n"
             + "   \"toTime\": \"23:59\",\n";
        }

        if (i != 0) {
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
                + "   \"serviceType\": \"" + escapeJSON(serviceType) + "\",\n"
                + "   \"serviceUri\": \"" + valueOfSer + "\",\n"
                + "   \"categoryIT\": \"" + escapeJSON(valueOfSTypeIta) + "\",\n"
                + "   \"categoryEN\": \"" + escapeJSON(valueOfSTypeEng) + "\",\n"
                + "   \"address\": \"" + escapeJSON(valueOfAddress) + "\",\n"
                + "   \"civic\": \"" + escapeJSON(valueOfNumber) + "\",\n"
                + "   \"city\": \"" + escapeJSON(valueOfCity) + "\",\n"
                + "   \"province\": \"" + escapeJSON(valueOfProv) + "\",\n"
                + "   \"cap\": \"" + escapeJSON(valueOfCap) + "\",\n"
                + "   \"email\": \"" + escapeJSON(valueOfEmail) + "\",\n"
                + "   \"phone\": \"" + escapeJSON(valueOfPhone) + "\",\n"
                + "   \"fax\": \"" + escapeJSON(valueOfFax) + "\",\n"
                + "   \"website\": \"" + escapeJSON(valueOfWebsite) + "\",\n"
                + "   \"note\": \"" + escapeJSON(valueOfNote) + "\",\n"
                + "   \"descriptionIT\": \"" + escapeJSON(valueOfDescriptionIta) + "\",\n"
                + "   \"descriptionEN\": \"" + escapeJSON(valueOfDescriptionEng) + "\",\n"
                + tt
                + "   \"multimedia\": \"" + valueOfMultimedia + "\""
                + (photos ? ",\n   \"photoThumbs\": " + ServiceMap.getServicePhotos(valueOfSer,"thumbs") : "")
                + (valueOfAgName!=null ? ",\n   \"agency\": \"" + valueOfAgName + "\"" : "")
                + (valueOfAgUri!=null ? ",\n   \"agencyUri\": \"" + valueOfAgUri + "\"" : "")
                + "\n  },\n"
                + "  \"id\": " + Integer.toString(i + 1) + "\n"
                + " }");
        i++;
        processed.put(valueOfSer, "");
      }
    }
    out.println("]}");
  }

  public void queryValueTypeList(JspWriter out, RepositoryConnection con) throws Exception {
    Configuration conf = Configuration.getInstance();
    final String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");

    String queryVTList = 
            "SELECT ?vt ?u WHERE{\n"
          + " ?vt a ssn:Property.\n"
          + " ?vt km4c:value_unit ?u.\n"
          + "} ORDER BY asc(?vt)";

    //ServiceMap.println(queryEvList);

    long ts = System.nanoTime();
    TupleQuery tupleQueryVTList = con.prepareTupleQuery(QueryLanguage.SPARQL, queryVTList);
    TupleQueryResult resultVTList = tupleQueryVTList.evaluate();
    logQuery(queryVTList, "API-value_type-list", sparqlType, "", System.nanoTime() - ts);

    out.println("[");
    int i=0;
    while (resultVTList.hasNext()) {
      BindingSet bindingSetVTList = resultVTList.next();
      if(i!=0)
        out.println(",");
      String value_type = bindingSetVTList.getValue("vt").stringValue();
      value_type = value_type.substring(value_type.lastIndexOf("/")+1);
      String value_unit = bindingSetVTList.getValue("u").stringValue();
      out.print("{\"value_type\":\""+value_type+"\",\"value_unit\":\""+value_unit+"\"}");
      i++;
    } 
    out.println("]");
  }
 
}  
