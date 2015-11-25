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

/**
 *
 * @author bellini
 */
public class ServiceMapApiV1 extends ServiceMapApi {

  public void queryBusStop(JspWriter out, RepositoryConnection con, String idService, String lang, String realtime) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String nomeFermata = "";
    String type = "";
    if (lang.equals("it")) {
      type = "Fermata";
    } else {
      type = "BusStop";
    }
    String queryStringBusStop = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
            + "PREFIX schema:<http://schema.org/#> "
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#> "
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
            + "SELECT distinct ?nomeFermata ?bslat ?bslong ?address WHERE { "
            + "	<" + idService + "> rdf:type km4c:BusStop."
            + "	<" + idService + "> foaf:name ?nomeFermata."
            + " OPTIONAL {<" + idService + "> km4c:isInRoad ?road."
            + "     ?road km4c:extendName ?address}."
            + "	<" + idService + "> geo:lat ?bslat."
            + "	<" + idService + "> geo:long ?bslong."
            + "}LIMIT 1";

    TupleQuery tupleQueryBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringBusStop);
    TupleQueryResult busStopResult = tupleQueryBusStop.evaluate();
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
        if (bindingSetBusStop.getValue("address") != null) {
          valueOfRoad = bindingSetBusStop.getValue("address").stringValue();
        }
        nomeFermata = bindingSetBusStop.getValue("nomeFermata").stringValue();
        if (i != 0) {
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
                // + "    \"popupContent\": \"" + nomeFermata + "\", "
                + "    \"name\": \"" + nomeFermata + "\", "
                + "    \"serviceUri\": \"" + idService + "\", "
                + "    \"typeLabel\": \"" + type + "\", "
                + "    \"address\": \"" + valueOfRoad + "\", "
                // *** INSERIMENTO serviceType
                + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\" "
                // **********************************************
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

    TupleQueryResult resultLines = queryBusLines(nomeFermata, con);

    if (resultLines != null) {
      try {
        out.println("\"busLines\":");
        out.println("{ \"head\": {"
                + "\"busStop\": "
                + "\"" + nomeFermata + "\""
                + ","
                + "\"vars\": "
                + "\"busLine\""
                + "},");
        out.println("\"results\": {");
        out.println("\"bindings\": [");

        int j = 0;
        while (resultLines.hasNext()) {
          BindingSet bindingSetLines = resultLines.next();
          String idLine = bindingSetLines.getValue("id").stringValue();
          if (j != 0) {
            out.println(", ");
          }
          out.println("{"
                  + "\"busLine\": {"
                  + "\"type\": \"literal\","
                  + "\"value\": \"" + idLine + "\" "
                  + " }"
                  + " }");
          j++;
        }
        out.println("]}}");

      } catch (Exception e) {
        out.println(e.getMessage());
      }
    }

    if("true".equalsIgnoreCase(realtime)) {
      String queryStringAVM = "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
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
      TupleQueryResult resultAVM = tupleQueryAVM.evaluate();
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
          out.println(",\"realtime\": {}}");
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
      TupleQueryResult resultAVM2 = tupleQueryAVM2.evaluate();
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

  public void queryLatLngServices(JspWriter out, RepositoryConnection con, String[] coords, String categorie, String textToSearch, String raggioBus, String raggioSensori, String raggioServizi, String risultatiBus, String risultatiSensori, String risultatiServizi, String lang) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");

    List<String> listaCategorieServizi = new ArrayList<String>();
    if (categorie != null) {
      String[] arrayCategorie = categorie.split(";");
      // GESTIONE CATEGORIE
      listaCategorieServizi = Arrays.asList(arrayCategorie);

    }
    String fc = "";
    try {
      fc = ServiceMap.filterServices(listaCategorieServizi);
    } catch (Exception e) {
    }
    int i = 0;
    int numeroBus = 0;
    int resBusStop = 0;
    int resSensori = 0;
    int resServizi = 0;
    if (listaCategorieServizi.contains("BusStop")) {
      String type;
      if (lang.equals("it")) {
        type = "Fermata";
      } else {
        type = "BusStop";
      }
      String queryStringNearBusStop = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "SELECT DISTINCT ?bs (STR(?nome) AS ?nomeFermata) ?bslat ?bslong ?x WHERE {\n"
              + " ?bs geo:lat ?bslat.\n"
              + " ?bs geo:long ?bslong.\n"
              + ServiceMap.geoSearchQueryFragment("?bs", coords, raggioBus)
              + " ?bs rdf:type km4c:BusStop.\n"
              + " ?bs foaf:name ?nome.\n"
              + ServiceMap.textSearchQueryFragment("?bs", "foaf:name", textToSearch)
              + (km4cVersion.equals("old") ? " FILTER ( datatype(?bslat ) = xsd:float )\n"
                      + " FILTER ( datatype(?bslong ) = xsd:float )\n" : "")
              + "} ORDER BY ?dist";
      if (!risultatiBus.equals("0")) {
        resBusStop = ((Integer.parseInt(risultatiServizi))/10*3);
        queryStringNearBusStop += " LIMIT " + resBusStop;
      }

      TupleQuery tupleQueryNearBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringNearBusStop);
      TupleQueryResult resultNearBS = tupleQueryNearBusStop.evaluate();
      out.println("{\"BusStops\": ");
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      int s = 0;
      while (resultNearBS.hasNext()) {
        BindingSet bindingSetNearBS = resultNearBS.next();
        String valueOfBS = bindingSetNearBS.getValue("bs").stringValue();
        String valueOfNomeFermata = bindingSetNearBS.getValue("nomeFermata").stringValue();
        String valueOfBSLat = bindingSetNearBS.getValue("bslat").stringValue();
        String valueOfBSLong = bindingSetNearBS.getValue("bslong").stringValue();

        /*String queryForLinee = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
         + "PREFIX km4cr:<http://www.disit.org/km4city/resource/>"
         + "PREFIX schema:<http://schema.org/>"
         + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
         + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>"
         + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
         + "PREFIX vcard:<http://www.w3.org/2006/vcard/ns#>"
         + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>"
         + "PREFIX dcterms:<http://purl.org/dc/terms/>"
         + "SELECT DISTINCT ?id WHERE {"
         + " ?tpll rdf:type km4c:PublicTransportLine."
         + " ?tpll dcterms:identifier ?id."
         + " ?tpll km4c:hasRoute ?route."
         + " ?route km4c:hasSection ?rs."
         + " ?rs km4c:endsAtStop ?bs1."
         + " ?rs km4c:startsAtStop ?bs2."
         + " { ?bs1 foaf:name \"" + valueOfNomeFermata + "\"."
         + " }UNION "
         + " {?bs2 foaf:name \"" + valueOfNomeFermata + "\" . "
         + " } "
         + "} ORDER BY ?id ";

         TupleQuery tupleQueryForLinee = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForLinee);*/
        TupleQueryResult resultLinee = queryBusLines(valueOfNomeFermata, con);
        String valueOfLinee = "";
        if (resultLinee != null) {
          while (resultLinee.hasNext()) {
            BindingSet bindingSetLinee = resultLinee.next();
            String idLinee = bindingSetLinee.getValue("id").stringValue();
            valueOfLinee = valueOfLinee + " - " + idLinee;
          }
        }

        if (s != 0) {
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
                + "    \"name\": \"" + valueOfNomeFermata + "\", "
                + "    \"typeLabel\": \"" + type + "\", "
                // *** INSERIMENTO serviceType
                + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\", "
                + "    \"busLines\": \"" + valueOfLinee.substring(3) + "\", "
                // **********************************************
                + "    \"serviceUri\": \"" + valueOfBS + "\" "
                + "}, "
                + "\"id\": " + Integer.toString(s + 1) + " "
                + "}");
        s++;
        numeroBus++;
      }
      out.println("]}");
      if (categorie.equals("BusStop")) {
        out.println("}");
      }
    }

    int numeroSensori = 0;
    if (listaCategorieServizi.contains("SensorSite")) {
      String type;
      if (lang.equals("it")) {
        type = "Sensore";
      } else {
        type = "Sensor";
      }
      String queryStringNearSensori
              = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX dct:<http://purl.org/dc/terms/#>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "SELECT DISTINCT ?sensor ?idSensore  ?lat ?long ?address ?x WHERE {\n"
              + " ?sensor rdf:type km4c:SensorSite.\n"
              + " ?sensor geo:lat ?lat.\n"
              + " ?sensor geo:long ?long.\n"
              + " ?sensor dcterms:identifier ?idSensore.\n"
              + ServiceMap.textSearchQueryFragment("?sensor", "?p", textToSearch)
              + ServiceMap.geoSearchQueryFragment("?sensor", coords, raggioBus)
              + "} ORDER BY ?dist";
      if (!risultatiSensori.equals("0")) {
        resSensori = ((Integer.parseInt(risultatiServizi))/10*2);
        queryStringNearSensori += " LIMIT " + resSensori;
      }

      TupleQuery tupleQuerySensori = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringNearSensori);
      TupleQueryResult resultNearSensori = tupleQuerySensori.evaluate();

      if (!listaCategorieServizi.contains("BusStop")) {
        out.println("{\"SensorSites\": ");
      } else {
        out.println(", \"SensorSites\": ");
      }
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");

      while (resultNearSensori.hasNext()) {
        BindingSet bindingSetNearSensori = resultNearSensori.next();
        String valueOfId = bindingSetNearSensori.getValue("idSensore").stringValue();
        String valueOfIdService = bindingSetNearSensori.getValue("sensor").stringValue();
        String valueOfLat = bindingSetNearSensori.getValue("lat").stringValue();
        String valueOfLong = bindingSetNearSensori.getValue("long").stringValue();

        if (i != 0) {
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
                // *** INSERIMENTO serviceType
                + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\", "
                // **********************************************
                + "    \"serviceUri\": \"" + valueOfIdService + "\" "
                + "},  "
                + "\"id\": " + Integer.toString(i + 1) + "  "
                + "}");

        i++;
        numeroSensori++;
      }
      out.println("]}");
      if (categorie.equals("SensorSite")) {
        out.println("}");
      }
    }

    int numeroServizi = 0;
    if (!categorie.equals("BusStop") && !categorie.equals("SensorSite") && !categorie.equals("SensorSite;BusStop") && !categorie.equals("BusStop;SensorSite")) {
      String queryStringServiceNear
              = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n"
              + "SELECT distinct ?ser ?serAddress ?elat ?elong ?sType ?sCategory ?sTypeIta ?sName ?email ?note ?multimedia ?description ?x WHERE {\n"
              + " ?ser rdf:type km4c:Service" + (sparqlType.equals("virtuoso") ? " OPTION (inference \"urn:ontology\")" : "") + ".\n"
              + " OPTIONAL {?ser <http://schema.org/name> ?sName}\n"
              + ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch)
              + " {\n"
              + "  ?ser km4c:hasAccess ?entry.\n"
              + "  ?entry geo:lat ?elat.\n"
              + "  ?entry geo:long ?elong.\n"
              + ServiceMap.geoSearchQueryFragment("?entry", coords, raggioServizi)
              + " } UNION {\n"
              + "  ?ser geo:lat ?elat.\n"
              + "  ?ser geo:long ?elong.\n"
              + ServiceMap.geoSearchQueryFragment("?ser", coords, raggioServizi)
              + " }\n"
              + fc
              + (!km4cVersion.equals("old")
                      ? " graph ?g {?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:BusStop && ?sType!=km4c:SensorSite)}\n"
                      + " ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory != <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>) "
                      + " ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"" + lang + "\")\n" : "")
              + "} ORDER BY ?dist";
      if (!risultatiServizi.equals("0")) {
        resServizi = ((Integer.parseInt(risultatiServizi))-(resBusStop + resSensori));
        queryStringServiceNear += " LIMIT " + resServizi;
      }

      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringServiceNear);
      TupleQueryResult result = tupleQuery.evaluate();

      if (!listaCategorieServizi.contains("BusStop") && !listaCategorieServizi.contains("SensorSite")) {
        out.println("{\"Services\": ");
      } else {
        out.println(", \"Services\": ");
      }
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
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

        if (w != 0) {
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
                // *** INSERIMENTO serviceType
                + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\", "
                // **********************************************
                + "    \"serviceUri\": \"" + escapeJSON(valueOfSer) + "\" "
                + "}, "
                + "\"id\": " + Integer.toString(w + 1) + " "
                + "}");
        w++;
        numeroServizi++;
      }
      out.println("]}}");
    } else {
      if (categorie.equals("SensorSite;BusStop") || categorie.equals("BusStop;SensorSite")) {
        out.println("}");
      }
    }
  }

  public void queryFulltext(JspWriter out, RepositoryConnection con, String textToSearch, String selection, String dist, String limit, String lang) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String[] coords = null;
    String type;
    if (lang.equals("it")) {
      type = "Servizio";
    } else {
      type = "Service";
    }

    if (selection != null && selection.contains(";")) {
      coords = selection.split(";");
    }

    String queryText = "PREFIX luc: <http://www.ontotext.com/owlim/lucene#>\n"
            + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX schema:<http://schema.org/>\n"
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
            + "SELECT DISTINCT ?ser ?long ?lat ?sType ?sTypeIta ?sCategory ?sName ?txt ?x WHERE {\n"
            + ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch)
            + (km4cVersion.equals("old")
                    ? "  OPTIONAL { ?ser km4c:hasServiceCategory ?cat.\n"
                    + "  ?cat rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"" + lang + "\") }\n"
                    : " {\n"
                    + "  ?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService)\n"
                    + "  OPTIONAL { ?sType rdfs:subClassOf ?sCategory. FILTER(STRSTARTS(STR(?sCategory),\"http://www.disit.org/km4city/schema#\"))}\n"
                    + "  ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"" + lang + "\")\n"
                    + " }\n")
            + " OPTIONAL {{?ser schema:name ?sName } UNION { ?ser foaf:name ?sName }}\n"
            + " {\n"
            + "  ?ser geo:lat ?lat .\n"
            + "  ?ser geo:long ?long .\n"
            + ServiceMap.geoSearchQueryFragment("?ser", coords, dist)
            + " } UNION {\n"
            + "  ?ser km4c:hasAccess ?entry.\n"
            + "	 ?entry geo:lat ?lat.\n"
            + "	 ?entry geo:long ?long.\n"
            + ServiceMap.geoSearchQueryFragment("?entry", coords, dist)
            /*+ "} UNION {"
             + " ?ser km4c:hasStreetNumber/km4c:hasExternalAccess ?entry."
             + " ?entry geo:lat ?lat."
             + " ?entry geo:long ?long." 
             + ServiceMap.geoSearchQueryFragment("?entry", coords, dist)*/
            + " }\n"
            + "} ORDER BY DESC(?sc)";
    if (!"0".equals(limit)) {
      queryText += " LIMIT " + limit;
    }
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
    long start = System.nanoTime();
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(queryText, "free-text-search", sparqlType, textToSearch + ";" + limit, System.nanoTime() - start);
    int i = 0;
    out.println("{ "
            + "\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    while (result.hasNext()) {
      if (i != 0) {
        out.println(", ");
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
      if (subCategory.equals("BusStop")) {
        TupleQueryResult resultLinee = queryBusLines(sName, con);
        if (resultLinee != null) {
          while (resultLinee.hasNext()) {
            BindingSet bindingSetLinee = resultLinee.next();
            String idLinee = bindingSetLinee.getValue("id").stringValue();
            valueOfLinee = valueOfLinee + " - " + idLinee;
          }
          if (valueOfLinee.length() > 3) {
            valueOfLinee = valueOfLinee.substring(3);
          }
        }
      }

      String txt = "";
      if (bindingSet.getValue("txt") != null) {
        txt = bindingSet.getValue("txt").stringValue();
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
              // *** INSERIMENTO serviceType
              // **********************************************
              + "\"name\": \"" + ServiceMap.escapeJSON(sName) + "\" ");
      if (!"".equals(category)) {
        out.println(",    \"serviceType\": \"" + escapeJSON(serviceType) + "\" ");
      }
      if (!"".equals(label)) {
        out.println(", \"typeLabel\": \"" + ServiceMap.escapeJSON(label) + "\" ");
      }else{
        out.println(", \"typeLabel\": \"" + type + "\", ");
      }
      if ("BusStop".equals(subCategory)) {
          out.println(",    \"busLines\": \"" + escapeJSON(valueOfLinee) + "\" ");
      }   
              out.println( "}, "
              + "\"id\": " + Integer.toString(i + 1) + "  "
              // + "\"query\": \"" + queryString + "\" "
              + "}");
      i++;
    }
    out.println("] "
            + "}");
  }

  public void queryMunicipalityServices(JspWriter out, RepositoryConnection con, String selection, String categorie, String textToSearch, String risultatiBus, String risultatiSensori, String risultatiServizi, String lang) throws Exception {
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
      if(type.equals("it")){
          type = "Fermata";
      }else{
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
              //+ " FILTER ( datatype(?nomeFermata ) = xsd:string ).\n"
              + " ?bs geo:lat ?bslat.\n"
              //+ " FILTER (?bslat>40)\n"
              //+ " FILTER ( datatype(?bslat ) = xsd:float )\n"
              + " ?bs geo:long ?bslong.\n"
              //+ " FILTER ( datatype(?bslong ) = xsd:float )\n"
              //+ " FILTER (?bslong>10)\n"
              + " ?bs km4c:isInMunicipality ?com.\n"
              + " ?com foaf:name \"" + nomeComune + "\"^^xsd:string.\n"
              + "}";
      if (!risultatiBus.equals("0")) {
        queryString += " LIMIT " + risultatiBus;
      }
      TupleQuery tupleQueryBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString, km4cVersion));
      TupleQueryResult resultBS = tupleQueryBusStop.evaluate();
      ServiceMap.logQuery(queryString, "API-fermate", sparqlType, nomeComune + ";" + textToSearch, 0);

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
                + "    \"typeLabel\": \""+ escapeJSON(type) +"\", "
                + "    \"serviceUri\": \"" + valueOfBS + "\", "
                + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\" "
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
      if(type.equals("it")){
          type = "Sensore";
      }else{
          type = "Sensor";
      }
      String queryStringSensori = "PREFIX km4c:<http://www.disit.org/km4city/schema#> "
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
      TupleQueryResult resultSensori = tupleQuerySensori.evaluate();
      ServiceMap.logQuery(queryStringSensori, "API-sensori", sparqlType, nomeComune + ";" + textToSearch, 0);
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
      if(type.equals("it")){
          type = "Servizio";
      }else{
          type = "Service";
      }
      String queryStringServices
              = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "SELECT distinct ?ser ?serAddress ?serNumber ?elat ?elong ?sName ?sType ?email ?note ?labelIta ?multimedia ?description ?identifier ?sCategory WHERE {\n"
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
              + "}";
      if (!risultatiServizi.equals("0")) {
        queryStringServices += " LIMIT " + risultatiServizi;
      }
      TupleQuery tupleQueryServices = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringServices, km4cVersion));
      TupleQueryResult resultServices = tupleQueryServices.evaluate();
      ServiceMap.logQuery(queryStringServices, "API-servizi", sparqlType, nomeComune + ";" + textToSearch + ";" + categorie, 0);

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
    String queryForComune = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
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
    TupleQueryResult resultComuneMeteo = tupleQueryComuneMeteo.evaluate();
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
    String queryStringMeteo1 = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>"
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
    TupleQueryResult result1 = tupleQuery1.evaluate();
    if (result1.hasNext()) {
      BindingSet bindingSet1 = result1.next();
      String valueOfInstantDateTime = bindingSet1.getValue("instantDateTime").stringValue();
      String valueOfWRep = bindingSet1.getValue("wRep").stringValue();

      String wPred = "";
      String queryMeteo = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
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
      TupleQueryResult resultMeteo = tupleQueryMeteo.evaluate();
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

  public void querySensor(JspWriter out, RepositoryConnection con, String idService, String lang, String realtime) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String type = "";
    if (lang.equals("it")) {
      type = "Sensore";
    } else {
      type = "Sensor";
    }
    String querySensore = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
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
    TupleQueryResult resultSensor = tupleQuerySensor.evaluate();
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
                // + "    \"popupContent\": \"" + valueOfId + " - sensore\", "
                + "    \"name\": \"" + escapeJSON(valueOfId) + "\", "
                + "    \"typeLabel\": \"" + escapeJSON(type) + "\", "
                + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\", "
                // **********************************************
                + "    \"serviceUri\": \"" + idService + "\", "
                + "    \"municipality\": \"" + escapeJSON(nomeComune) + "\", "
                + "    \"address\": \"" + escapeJSON(valueOfAddress) + "\" "
                + "},  "
                + "\"id\": " + Integer.toString(s + 1) + "  "
                + "}");
        s++;
      }
      out.println("]}");
    } catch (Exception e) {
      out.println(e.getMessage());
    }
    if("true".equalsIgnoreCase(realtime)) {
      String querySensorData = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
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
              + " ?obs km4c:measuredTime ?time."
              + " ?time dcterms:identifier ?timeInstant."
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

  public String queryService(JspWriter out, RepositoryConnection con, String idService, String lang, String realtime) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    String r = "";
    int i = 0;
    String queryService = "PREFIX km4c:<http://www.disit.org/km4city/schema#> "
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> "
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX schema:<http://schema.org/>"
            //+ "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#> "
            //+ "PREFIX foaf:<http://xmlns.com/foaf/0.1/> "
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>"
            //+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> "
            + "PREFIX dcterms:<http://purl.org/dc/terms/>"
            + "SELECT ?serAddress ?serNumber ?elat ?elong ?sName ?sType ?type ?sCategory ?sTypeIta ?email ?note ?multimedia ?descriptionEng ?descriptionIta ?phone ?fax ?website ?prov ?city ?cap WHERE{"
            + " {"
            + "  <" + idService + "> km4c:hasAccess ?entry ."
            + "  ?entry geo:lat ?elat . "
            //+ "  FILTER (?elat>40) "
            + "  ?entry geo:long ?elong . "
            //+ "  FILTER (?elong>10) ."
            + " }UNION{"
            + "  <" + idService + "> km4c:isInRoad ?road."
            + "  <" + idService + "> geo:lat ?elat."
            //+ "  FILTER (?elat>40) "
            + "  <" + idService + "> geo:long ?elong."
            //+ "  FILTER (?elong>10) ."
            + " }UNION{"
            + "  <" + idService + "> geo:lat ?elat."
            //+ "  FILTER (?elat>40) "
            + "  <" + idService + "> geo:long ?elong."
            //+ "  FILTER (?elong>10) ."
            + " }"
            + "   OPTIONAL {<" + idService + "> schema:name ?sName.}"
            + "   OPTIONAL { <" + idService + "> schema:streetAddress ?serAddress.}"
            + (km4cVersion.equals("old")
                    ? " <" + idService + "> km4c:hasServiceCategory ?cat ."
                    + " ?cat rdfs:label ?nome . "
                    + " BIND (?nome  AS ?sType) ."
                    + " BIND (?nome  AS ?sTypeIta) ."
                    + " FILTER(LANG(?nome) = \"it\") ."
                    + " OPTIONAL {<" + idService + "> <http://purl.org/dc/elements/1.1/description> ?description ."
                    + " FILTER(LANG(?description) = \"it\")} ."
                    : " <" + idService + "> a ?type . FILTER(?type!=km4c:RegularService && ?type!=km4c:Service && ?type!=km4c:DigitalLocation)"
                    + " ?type rdfs:label ?nome . "
                    + " ?type rdfs:subClassOf ?sCategory. "
                    + " BIND (?nome  AS ?sType) ."
                    + " BIND (?nome  AS ?sTypeIta) ."
                    + " FILTER(LANG(?nome) = \""+lang+"\") .")
            + "   OPTIONAL {<" + idService + "> km4c:houseNumber ?serNumber} ."
            + "   OPTIONAL {<" + idService + "> dcterms:description ?descriptionIta"
            //+ " FILTER(LANG(?descriptionEng) = \"en\")"
            + "}"
            + "   OPTIONAL {<" + idService + "> dcterms:description ?descriptionEng FILTER(?descriptionEng!=?descriptionIta)"
            //+ " FILTER(LANG(?descriptionIta) = \"it\")"
            + "}"
            + "   OPTIONAL {<" + idService + "> km4c:multimediaResource ?multimedia} ."
            + "   OPTIONAL {<" + idService + "> skos:note ?note} . "
            + "   OPTIONAL {<" + idService + "> schema:email ?email } . "
            // AGGIUNTA CAMPI DA VISUALIZZARE NELLA SCHEDA
            + " OPTIONAL {<" + idService + "> schema:faxNumber ?fax }"
            + " OPTIONAL {<" + idService + "> schema:telephone ?phone }"
            + " OPTIONAL {<" + idService + "> schema:addressRegion ?prov }"
            + " OPTIONAL {<" + idService + "> schema:addressLocality ?city }"
            + " OPTIONAL {<" + idService + "> schema:postalCode ?cap }"
            + " OPTIONAL {<" + idService + "> schema:url ?website }"
            // ---- FINE CAMPI AGGIUNTI ---
            + "}LIMIT 1";
    // out.println("count = "+count);
    String queryDBpedia = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX cito:<http://purl.org/spar/cito/>\n"
            + "SELECT ?linkDBpedia WHERE{\n"
            + " OPTIONAL {<" + idService + "> km4c:isInRoad ?road.\n"
            + "?road cito:cites ?linkDBpedia.}\n"
            + "}";

    out.println("{ \"Service\":"
            + "{\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    TupleQuery tupleQueryService = con.prepareTupleQuery(QueryLanguage.SPARQL, queryService);
    TupleQueryResult resultService = tupleQueryService.evaluate();
    String TOS = "";
    String NOS = "";
    TupleQuery tupleQueryDBpedia = con.prepareTupleQuery(QueryLanguage.SPARQL, queryDBpedia);
    TupleQueryResult resultDBpedia = tupleQueryDBpedia.evaluate();
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

      // ---- FINE AGGIUNTA ---
      NOS = valueOfSName;

      //valueOfSTypeIta = Character.toLowerCase(valueOfSTypeIta.charAt(0)) + valueOfSTypeIta.substring(1);
      //valueOfSTypeIta = valueOfSTypeIta.replace(" ", "_");
      valueOfSTypeIta = valueOfSTypeIta.replace("@it", "");
      TOS = valueOfSTypeIta;

      Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
      valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 \\.:;,]+", "");

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
              + "},  "
              + "\"type\": \"Feature\",  "
              + "\"properties\": {  "
              + "    \"name\": \"" + escapeJSON(valueOfSName) + "\", "
              + "    \"typeLabel\": \"" + TOS + "\", "
              // *** INSERIMENTO serviceType
              + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\", "
              + "    \"phone\": \"" + escapeJSON(valueOfPhone) + "\", "
              + "    \"fax\": \"" + escapeJSON(valueOfFax) + "\", "
              + "    \"website\": \"" + escapeJSON(valueOfUrl) + "\", "
              + "    \"province\": \"" + escapeJSON(valueOfProv) + "\", "
              + "    \"city\": \"" + escapeJSON(valueOfCity) + "\", "
              + "    \"cap\": \"" + escapeJSON(valueOfCap) + "\", "
              // **********************************************
              + "    \"email\": \"" + escapeJSON(valueOfEmail) + "\", "
              + "    \"linkDBpedia\": " + valueOfDBpedia + ", "
              + "    \"note\": \"" + escapeJSON(valueOfNote) + "\", "
              + "    \"description\": \"" + escapeJSON(valueOfDescriptionIta) + "\", "
              + "    \"description2\": \"" + escapeJSON(valueOfDescriptionEng) + "\", "
              + "    \"multimedia\": \"" + valueOfMultimediaResource + "\", "
              + "    \"serviceUri\": \"" + idService + "\", "
              + "    \"address\": \"" + escapeJSON(valueOfSerAddress) + "\", \"civic\": \"" + escapeJSON(valueOfSerNumber) + "\" "
              + "}, "
              + "\"id\": " + Integer.toString(i + 1) + "  "
              + "}");
      i++;
    }
    out.println("] }");
    String labelPark = "Car park";
    if (lang.equals("it")){
        labelPark = "Parcheggio auto";
    }
    if (TOS.equals(labelPark) && "true".equalsIgnoreCase(realtime)) {
      String queryStringParkingStatus = "  PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
              + "PREFIX schema:<http://schema.org/#>"
              + "PREFIX time:<http://www.w3.org/2006/time#>"
              + "SELECT distinct ?situationRecord ?instantDateTime ?occupancy ?free ?occupied ?capacity ?cpStatus WHERE { "
              + "	?cps km4c:observeCarPark <" + idService + ">."
              + "	?cps km4c:capacity ?capacity."
              + "	?situationRecord km4c:relatedToSensor ?cps."
              + "	?situationRecord km4c:observationTime ?time."
              + "	?time <http://purl.org/dc/terms/identifier> ?instantDateTime."
              + "	?situationRecord km4c:parkOccupancy ?occupancy."
              + "	?situationRecord km4c:free ?free."
              + "	?situationRecord km4c:carParkStatus ?cpStatus."
              + "	?situationRecord km4c:occupied ?occupied."
              + "} ORDER BY DESC (?instantDateTime) LIMIT 1";
      TupleQuery tupleQueryParking = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringParkingStatus);
      TupleQueryResult resultParkingStatus = tupleQueryParking.evaluate();
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

  public void queryEvent(JspWriter out, RepositoryConnection con, String idService, String lang) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    int i = 0;
    String queryService = "PREFIX km4c:<http://www.disit.org/km4city/schema#> "
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
    TupleQueryResult resultService = tupleQueryService.evaluate();
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
              + "    \"eventCategory2\": \"" + escapeJSON(valueOfCateg2) + "\" "
              + "}, "
              + "\"id\": " + Integer.toString(i + 1) + "  "
              + "}");
      i++;
    }
    out.println("] }}");
  }


  public void queryEventList(JspWriter out, RepositoryConnection con, String range, String[] coords, String dist, String numEv, String textFilter) throws Exception {
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
    if (eventRange.equals("week")){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(data_attuale);
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        data_inizio = df.format(calendar.getTime()); 
    }else if(eventRange.equals("month")){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(data_attuale);
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        data_inizio = df.format(calendar.getTime());
    }
    String queryEvList = "PREFIX km4c:<http://www.disit.org/km4city/schema#> \n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#> \n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> \n"
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
            + " ?ev schema:startDate ?sDate. FILTER (?sDate <= \""+data_inizio+"\"^^xsd:date). \n"
            + " OPTIONAL {?ev schema:endDate ?eDate. FILTER (xsd:date(?eDate) >= \""+data_fine+"\"^^xsd:date).} \n"
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
    
    if (numEventi != null && !numEventi.equals("0")){
        queryEvList += " LIMIT " + numEventi;
    }
       

    System.out.println(queryEvList);
    out.println("{ \"Event\":"
            + "{\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    TupleQuery tupleQueryEvList = con.prepareTupleQuery(QueryLanguage.SPARQL, queryEvList);
    TupleQueryResult resultEvList = tupleQueryEvList.evaluate();
    

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
      
      String serviceType="Event";

      /*Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
      valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 \\.,:;]+", "");*/
      if((!valueOfEDate.equals("")) || (valueOfEDate.equals("") && (valueOfSDate.equals(data_inizio)))){
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
              + "    \"nome\": \"" + escapeJSON(valueOfName) + "\", "
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
              + "    \"serviceType\": \""+ serviceType + "\" "
              + "}, "
              + "\"id\": " + Integer.toString(i + 1) + "  "
              + "}");
      i++;
      }
    }
    out.println("] }}");
  }
  
    public void queryLatLngTPL(JspWriter out, RepositoryConnection con, String[] coords, String dist, String numLinee) throws Exception {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String km4cVersion = conf.get("km4cVersion", "new");
    
    int i = 0;
    
    String queryNearTPL = "PREFIX km4c:<http://www.disit.org/km4city/schema#> \n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#> \n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/> \n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> \n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> \n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
            + "PREFIX schema:<http://schema.org/> \n"
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> \n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/> \n"
            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> \n"
            + "SELECT DISTINCT ?route ?numLinea ?nomeLinea ?nomeFBS ?nomeLBS ?x  WHERE{\n"
            + " ?bs rdf:type km4c:BusStop. \n"
            + " ?bs geo:lat ?bslat. \n"
            + " ?bs geo:long ?bslong. \n"
            + ServiceMap.geoSearchQueryFragment("?bs", coords, dist)
            + " ?bs foaf:name ?nome. \n"
            + " OPTIONAL {?rs km4c:endsAtStop ?bs. } \n"
            + " OPTIONAL {?rs km4c:startsAtStop ?bs. } \n"
            + " ?route km4c:hasSection ?rs. \n"
            + " ?route foaf:name ?nomeLinea. \n"
            + " ?route km4c:hasFirstStop ?fbs. \n"
            + " ?route km4c:hasLastStop ?lbs. \n"
            + " ?fbs foaf:name ?nomeFBS. \n"
            + " ?lbs foaf:name ?nomeLBS. \n"
            + " ?tpl km4c:hasRoute ?route. \n"
            + " ?tpl dcterms:identifier ?numLinea. \n"
            + "} ";
    if (numLinee != null && !numLinee.equals("0")){
        queryNearTPL += " LIMIT " + numLinee;
    }
    out.println("{ \"PublicTransportLine\":"
            + "{\"type\": \"FeatureCollection\", "
            + "\"features\": [ ");
    TupleQuery tupleQueryNearTPL = con.prepareTupleQuery(QueryLanguage.SPARQL, queryNearTPL);
    TupleQueryResult resultNearTPL = tupleQueryNearTPL.evaluate();
    

    while (resultNearTPL.hasNext()) {
      BindingSet bindingSetNearTPL = resultNearTPL.next();
      
      String valueOfLineNumber = "";
      if (bindingSetNearTPL.getValue("numLinea") != null) {
        valueOfLineNumber = bindingSetNearTPL.getValue("numLinea").stringValue();
      }
 
      String valueOfLineName = "";
      if (bindingSetNearTPL.getValue("nomeLinea") != null) {
        valueOfLineName = bindingSetNearTPL.getValue("nomeLinea").stringValue();
      }
      
      String valueOfRoute = "";
      if (bindingSetNearTPL.getValue("route") != null) {
        valueOfRoute = bindingSetNearTPL.getValue("route").stringValue();
      }
      
      String valueOfFirstBS = "";
      if (bindingSetNearTPL.getValue("nomeFBS") != null) {
        valueOfFirstBS = bindingSetNearTPL.getValue("nomeFBS").stringValue();
      }
      
      String valueOfLastBS = "";
      if (bindingSetNearTPL.getValue("nomeLBS") != null) {
        valueOfLastBS = bindingSetNearTPL.getValue("nomeLBS").stringValue();
      }

      String serviceType="PublicTransportLine";
      String direction = valueOfFirstBS+" \u2794 "+valueOfLastBS;
      valueOfRoute = valueOfRoute.replace("http://www.disit.org/km4city/resource/", "");

      if (i != 0) {
        out.println(", ");
      }

      out.println("{ "
              
              + "\"type\": \"Feature\",  "
              + "\"properties\": {  "
              + "    \"lineNumber\": \"" + escapeJSON(valueOfLineNumber) + "\", "
              + "    \"lineName\": \"" + escapeJSON(valueOfLineName) + "\", "
              + "    \"route\": \"" + escapeJSON(valueOfRoute) + "\", "
              + "    \"direction\": \""+ escapeJSON(direction) +"\", "
              + "    \"serviceType\": \""+ serviceType + "\" "
              + "}, "
              + "\"id\": " + Integer.toString(i + 1) + "  "
              + "}");
      i++;
    }
    out.println("] }}");
  }
}
