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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.CoordinateSequenceComparator;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.GeometryFilter;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.jsp.JspWriter;
import org.disit.servicemap.Configuration;
import org.disit.servicemap.Configuration;
import org.disit.servicemap.ServiceMap;
import org.disit.servicemap.ServiceMap;
import static org.disit.servicemap.ServiceMap.escapeJSON;
import static org.disit.servicemap.ServiceMap.logQuery;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 *
 * @author bellini
 */
public class ServiceMapApi {

    static public String filterQuery(String query, String km4cVersion) {
        if (km4cVersion.equals("new")) {
            return query.replace("^^xsd:string", "");
        }
        return query;
    }

    public void queryFulltext(JspWriter out, RepositoryConnection con, String textToSearch, String selection, String dist, String limit) throws Exception {
        Configuration conf = Configuration.getInstance();
        String sparqlType = conf.get("sparqlType", "virtuoso");
        String km4cVersion = conf.get("km4cVersion", "new");
        String[] coords = null;

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
                        + "  ?cat rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\") }\n"
                        : " {\n"
                        + "  ?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService)\n"
                        + "  OPTIONAL { ?sType rdfs:subClassOf ?sCategory. FILTER(STRSTARTS(STR(?sCategory),\"http://www.disit.org/km4city/schema#\"))}\n"
                        + "  ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\")\n"
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
            BindingSet bindingSet = result.next();

            String serviceUri = bindingSet.getValue("ser").stringValue();
            if(!IoTChecker.checkIoTService(serviceUri, null)) {
              continue;
            }
            
            if (i != 0) {
                out.println(", ");
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
                label = label.replace(" ", "_");
                label = label.replace("'", "");
            }
            // DICHIARAZIONE VARIABILI serviceType e serviceCategory per ICONA
            String subCategory = "";
            if (bindingSet.getValue("sType") != null) {
                subCategory = bindingSet.getValue("sType").stringValue();
                subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
                subCategory = Character.toLowerCase(subCategory.charAt(0)) + subCategory.substring(1);
                subCategory = subCategory.replace(" ", "_");
            }

            String category = "";
            if (bindingSet.getValue("sCategory") != null) {
                category = bindingSet.getValue("sCategory").stringValue();
                category = category.replace("http://www.disit.org/km4city/schema#", "");
                category = Character.toLowerCase(category.charAt(0)) + category.substring(1);
                category = category.replace(" ", "_");
            }
            String sName = "";
            if (bindingSet.getValue("sName") != null) {
                sName = bindingSet.getValue("sName").stringValue();
            }else{
                sName = subCategory.replace("_", " ").toUpperCase();
            }
              
            // Controllo categoria SensorSite e BusStop per ricerca testuale.
            String serviceType = "";
            String valueOfLinee = "";
            
            //Da verificare se OK
            /*if (subCategory.equals("sensorSite")) {
                serviceType = "RoadSensor";
            } else if (subCategory.equals("busStop")) {
                serviceType = "NearBusStop";
                TupleQueryResult resultLinee =  queryBusLines(sName, con);
                if(resultLinee != null){
                while (resultLinee.hasNext()) {
                    BindingSet bindingSetLinee = resultLinee.next();
                    String idLinee = bindingSetLinee.getValue("id").stringValue();
                    valueOfLinee = valueOfLinee + " - "+idLinee;
                }
                if(valueOfLinee.length()>3)
                  valueOfLinee = valueOfLinee.substring(3);
                }
            } else if(! "".equals(category)){
                serviceType = category + "_" + subCategory;
            }*/
            
            serviceType = category + "_" + subCategory;
            if (subCategory.equals("BusStop")) {
                TupleQueryResult resultLinee =  queryBusLines(sName, con);
                if(resultLinee != null){
                while (resultLinee.hasNext()) {
                    BindingSet bindingSetLinee = resultLinee.next();
                      String idLinee = bindingSetLinee.getValue("id").stringValue();
                      valueOfLinee = valueOfLinee + " - "+idLinee;
                    }
                if(valueOfLinee.length()>3)
                  valueOfLinee = valueOfLinee.substring(3);
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
                    + "\"nome\": \"" + ServiceMap.escapeJSON(sName) + "\", ");
            if(!"".equals(category))
                    out.println("\"tipo\": \"servizio\", "
                       + "    \"serviceType\": \"" + escapeJSON(mapServiceType(serviceType)) + "\", ");
            out.println("\"type\": \"" + ServiceMap.escapeJSON(label) + "\", "
                    + "    \"linee\": \"" + escapeJSON(valueOfLinee) + "\" "
                    + "}, "
                    + "\"id\": " + Integer.toString(i + 1) + "  "
                    // + "\"query\": \"" + queryString + "\" "
                    + "}");
            i++;
        }
        out.println("] "
                + "}");
    }

    public void queryService(JspWriter out, RepositoryConnection con, String idService) throws Exception {
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
                        : " <" + idService + "> a ?type . FILTER(?type!=km4c:RegularService && ?type!=km4c:Service)"
                        + " ?type rdfs:label ?nome . "
                        + " ?type rdfs:subClassOf ?sCategory. "
                        + " BIND (?nome  AS ?sType) ."
                        + " BIND (?nome  AS ?sTypeIta) ."
                        + " FILTER(LANG(?nome) = \"it\") .")
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
                if (bindingSetDBpedia.getValue("linkDBpedia") != null ){
                    if(!("[".equals(valueOfDBpedia))){
                        valueOfDBpedia = valueOfDBpedia+", \""+bindingSetDBpedia.getValue("linkDBpedia").stringValue()+"\"";
                    }else{
                        valueOfDBpedia = valueOfDBpedia+"\""+bindingSetDBpedia.getValue("linkDBpedia").stringValue()+"\"";
                    }
                }
            }
            valueOfDBpedia = valueOfDBpedia+"]";  
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
            valueOfSTypeIta = valueOfSTypeIta.replace(" ", "_");
            valueOfSTypeIta = valueOfSTypeIta.replace("@it", "");
            TOS = valueOfSTypeIta;

            Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
            valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 ]+", "");

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
                    + "    \"nome\": \"" + escapeJSON(valueOfSName) + "\", "
                    + "    \"tipo\": \"" + TOS + "\", "
                    // *** INSERIMENTO serviceType
                    + "    \"serviceType\": \"" + escapeJSON(mapServiceType(serviceType)) + "\", "
                    + "    \"phone\": \"" + valueOfPhone + "\", "
                    + "    \"fax\": \"" + valueOfFax + "\", "
                    + "    \"website\": \"" + valueOfUrl + "\", "
                    + "    \"province\": \"" + valueOfProv + "\", "
                    + "    \"city\": \"" + valueOfCity + "\", "
                    + "    \"cap\": \"" + valueOfCap + "\", "
                    // **********************************************
                    + "    \"email\": \"" + escapeJSON(valueOfEmail) + "\", "
                    + "    \"linkDBpedia\": " + valueOfDBpedia + ", "
                    + "    \"note\": \"" + escapeJSON(valueOfNote) + "\", "
                    + "    \"description\": \"" + escapeJSON(valueOfDescriptionIta) + "\", "
                    + "    \"description2\": \"" + escapeJSON(valueOfDescriptionEng) + "\", "
                    + "    \"multimedia\": \"" + valueOfMultimediaResource + "\", "
                    + "    \"serviceUri\": \"" + idService + "\", "
                    + "    \"indirizzo\": \"" + valueOfSerAddress + "\", \"numero\": \"" + valueOfSerNumber + "\" "
                    + "}, "
                    + "\"id\": " + Integer.toString(i + 1) + "  "
                    + "}");
            i++;
        }
        out.println("] }");
        if ("Parcheggio_auto".equals(TOS)) {
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
                        + "\"parcheggio\":[ "
                        + "\"" + NOS + "\""
                        + "],"
                        + "\"vars\":[ "
                        + "\"Capacità\", "
                        + "\"PostiLiberi\","
                        + "\"PostiOccupati\","
                        + "\"Occupazione\","
                        + "\"Aggiornamento\""
                        + "]"
                        + "},");
                out.println("\"results\": {");
                out.println("\"bindings\": [");

                
                int p = 0;
                while (resultParkingStatus.hasNext()) {
                    BindingSet bindingSetParking = resultParkingStatus.next();
                    
                    String valueOfInstantDateTime = "";
                    if(bindingSetParking.getValue("instantDateTime") != null){
                     valueOfInstantDateTime = bindingSetParking.getValue("instantDateTime").stringValue();
                    }
                    String valueOfOccupancy = "";
                    if(bindingSetParking.getValue("occupancy") != null){        
                    valueOfOccupancy = bindingSetParking.getValue("occupancy").stringValue();
                    }
                    String valueOfFree = "";
                    if(bindingSetParking.getValue("free") != null){   
                           valueOfFree = bindingSetParking.getValue("free").stringValue();
                    }
                    String valueOfOccupied = "";
                    if(bindingSetParking.getValue("occupied") != null){   
                    valueOfOccupied = bindingSetParking.getValue("occupied").stringValue();
                    }
                    String valueOfCapacity = "";
                     if(bindingSetParking.getValue("capacity") != null){
                    valueOfCapacity = bindingSetParking.getValue("capacity").stringValue();
                     }
                    String valueOfcpStatus = "";
                    if(bindingSetParking.getValue("cpStatus") != null){
                    valueOfcpStatus = bindingSetParking.getValue("cpStatus").stringValue();
                    }

                    if (p != 0) {
                        out.println(", ");
                    }

                    out.println("{"
                            + "\"Capacità\": {"
                            + "\"value\": \"" + valueOfCapacity + "\" "
                            + " },"
                            + "\"PostiLiberi\": { "
                            + "\"value\": \"" + valueOfFree + "\" "
                            + " },"
                            + "\"PostiOccupati\": {"
                            + "\"value\": \"" + valueOfOccupied + "\" "
                            + " },"
                            + "\"Occupazione\": {"
                            + "\"value\": \"" + valueOfOccupancy + "\" "
                            + " },"
                            + "\"Stato\": {"
                            + "\"value\": \"" + valueOfcpStatus + "\" "
                            + " },"
                            + "\"Aggiornamento\": {"
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
    }

    public void queryBusStop(JspWriter out, RepositoryConnection con, String idService) throws Exception {
        Configuration conf = Configuration.getInstance();
        String sparqlType = conf.get("sparqlType", "virtuoso");
        String km4cVersion = conf.get("km4cVersion", "new");
        String nomeFermata = "";
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
        out.println("{\"Fermata\": ");
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
                if( bindingSetBusStop.getValue("address")!= null){
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
                        + "    \"nome\": \"" + nomeFermata + "\", "
                        + "    \"serviceUri\": \"" + idService + "\", "
                        + "    \"tipo\": \"fermata\", "
                        + "    \"address\": \"" + valueOfRoad + "\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"NearBusStop\" "
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

        /*String queryForLine = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
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
                + " { ?bs1 foaf:name \"" + nomeFermata + "\"."
                + " }UNION "
                + " {?bs2 foaf:name \"" + nomeFermata + "\" . "
                + " } "
                + "} ORDER BY ?id ";

        TupleQuery tupleQueryForLine = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForLine);*/
        TupleQueryResult resultLines = queryBusLines(nomeFermata, con);
        
        if(resultLines != null){
        try {
            out.println("\"linee\":");
            out.println("{ \"head\": {"
                    + "\"fermata\": "
                    + "\"" + nomeFermata + "\""
                    + ","
                    + "\"vars\": "
                    + "\"linea\""
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
                        + "\"linea\": {"
                        + "\"type\": \"literal\","
                        + "\"value\": \"" + idLine + "\" "
                        + " }"
                        + " }");
                j++;
            }
            out.println("]}},");

        } catch (Exception e) {
            out.println(e.getMessage());
        }
        }
        
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
        if(i==0) {
        out.println("\"realtime\": {}}");
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

        /*String queryStringAVM2 = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>"
                + "PREFIX dcterms:<http://purl.org/dc/terms/>"
                + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
                + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
                + "PREFIX schema:<http://schema.org/>"
                + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
                + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                + "PREFIX time:<http://www.w3.org/2006/time#>"
                + "SELECT distinct ?avmr ?linea ?stato ?arrivoPrevistoIstante ?idRide WHERE {"
                + "{"
                + "SELECT ?ride (MAX(?avmr) AS ?avmrLast) WHERE{"
                + "?bs rdf:type km4c:BusStop."
                + "?bs foaf:name \"" + nomeFermata + "\"."
                + "?bs km4c:hasForecast ?bsf."
                + "?avmr km4c:includeForecast ?bsf."
                //+ "?avmr km4c:concernLine ?tpll."
                + "?ride km4c:hasAVMRecord ?avmr."
                //+ "?avmr km4c:hasLastStopTime/schema:value ?timeInstant."
                + "}"
                + "GROUP BY ?ride "
                + "ORDER BY DESC (?avmrLast)"
                + "LIMIT 15"
                + "}"
                + "?bs rdf:type km4c:BusStop."
                + "?bs foaf:name \"" + nomeFermata + "\"."
                + "?bs km4c:hasForecast ?previsione."
                + "?avmrLast km4c:includeForecast ?previsione."
                + "?previsione km4c:expectedTime ?arrivoPrevistoIstante."
                + "?avmrLast km4c:concernLine ?linea."
                + "?avmrLast km4c:rideState ?stato."
                + "?ride km4c:hasAVMRecord ?avmrLast."
                + "?ride dcterms:identifier ?idRide."
                + "FILTER(?arrivoPrevistoIstante>=now())"
                + "}"
                + "order by ?arrivoPrevistoIstante";*/
        TupleQuery tupleQueryAVM2 = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringAVM2);
        String idRide = "";
        TupleQueryResult resultAVM2 = tupleQueryAVM2.evaluate();
        out.println("\"realtime\":");
        if (resultAVM2.hasNext()) {
            out.println("{ \"head\": {"
                    + "\"fermata\":[ "
                    + "\"" + nomeFermata + "\""
                    + "],"
                    + "\"vars\":[ "
                    + "\"orario\", "
                    + "\"linea\","
                    + "\"stato\","
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
                String direction = bsFirst+" &#10132; "+bsLast;
                if (bindingSet2.getValue("rName") != null) {
                    nomeLinea = bindingSet2.getValue("rName").stringValue();
                }

                valueOfLinea = valueOfLinea.replace("http://www.disit.org/km4city/resource/", "");
                valueOfArrivoPrevistoIstante = valueOfArrivoPrevistoIstante.substring(11, 19);

                if (k != 0) {
                    out.println(", ");
                }

                out.println("{"
                        + "\"orario\": {"
                        + "\"type\": \"literal\","
                        + "\"value\": \"" + valueOfArrivoPrevistoIstante + "\" "
                        + " },"
                        + "\"linea\": { "
                        + "\"type\": \"literal\","
                        + "\"value\": \"" + nomeLinea + "\" "
                        + " },"
                        + "\"stato\": {"
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
            out.println("]}}}");
        } else {
            out.println("{}}");
        }
    }

    public void queryMeteo(JspWriter out, RepositoryConnection con, String idService) throws Exception {
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
        //ServiceMap.println("comune: " + nomeComune);
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
                    + "\"giorno\", "
                    + "\"descrizione\","
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
                            + "\"giorno\": {"
                            + "\"type\": \"literal\","
                            + "\"value\": \"" + valueOfGiorno + "\" "
                            + " },"
                            + "\"descrizione\": { "
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

    public void querySensor(JspWriter out, RepositoryConnection con, String idService) throws Exception {
        Configuration conf = Configuration.getInstance();
        String sparqlType = conf.get("sparqlType", "virtuoso");
        String km4cVersion = conf.get("km4cVersion", "new");
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
        out.println("{\"Sensore\": ");
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
                        + "    \"nome\": \"" + valueOfId + "\", "
                        + "    \"tipo\": \"sensore\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"RoadSensor\", "
                        // **********************************************
                        + "    \"serviceUri\": \"" + idService + "\", "
                        + "    \"nomeComune\": \"" + nomeComune + "\", "
                        + "    \"indirizzo\": \"" + valueOfAddress + "\" "
                        + "},  "
                        + "\"id\": " + Integer.toString(s + 1) + "  "
                        + "}");
                s++;
            }
            out.println("]},");
        } catch (Exception e) {
            out.println(e.getMessage());
        }
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

            out.println("\"realtime\":");
            if (resultSensorData.hasNext()) {
                out.println("{ \"head\": {"
                        + "\"sensore\":[ "
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
                        + "\"timeInstant\""
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
                out.println("]}}}");

            } else {
                out.println("{}}");
            }
        } catch (Exception e) {
            out.println(e.getMessage());
        }
    }

    public void queryMunicipalityServices(JspWriter out, RepositoryConnection con, String selection, String categorie, String textToSearch, String risultatiBus, String risultatiSensori, String risultatiServizi) throws Exception {
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
        if(selection.startsWith("COMUNE di "))
          nomeComune = selection.substring(10);
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
            ServiceMap.notifyException(e);
        }
        int b = 0;
        int numeroBus = 0;
        //if (listaCategorie.contains("NearBusStops")) {  OLD
        if (listaCategorie.contains("BusStop")) { 
            String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                    + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
                    + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                    + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX schema:<http://schema.org/#>\n"
                    + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
                    + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
                    + "SELECT DISTINCT ?bs ?nomeFermata ?bslat ?bslong ?x WHERE {\n"
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
            long ts = System.nanoTime();
            TupleQueryResult resultBS = tupleQueryBusStop.evaluate();
            ServiceMap.logQuery(queryString, "API-fermate", sparqlType, nomeComune + ";" + textToSearch, System.nanoTime()-ts);

            out.println("{\"Fermate\": ");
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
                        + "    \"nome\": \"" + escapeJSON(valueOfNomeFermata) + "\", "
                        + "    \"tipo\": \"fermata\", "
                        + "    \"serviceUri\": \"" + valueOfBS + "\" "
                        + "}, "
                        + "\"id\": " + Integer.toString(b + 1) + "  "
                        + "}");
                b++;
                numeroBus++;
            }
            out.println("]}");
            //if (categorie.equals("NearBusStop")) {
            if (categorie.equals("BusStop")) {
                out.println("}");
            }
        }
        int numeroSensori = 0;
        //if (listaCategorie.contains("RoadSensor")) {
        if (listaCategorie.contains("SensorSite")) {
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
                    + "?sensor <http://schema.org/streetAddress> ?address ."
                    + "}";
            if (!risultatiSensori.equals("0")) {

                queryStringSensori += " LIMIT " + risultatiSensori;
            }
            TupleQuery tupleQuerySensori = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringSensori, km4cVersion));
            TupleQueryResult resultSensori = tupleQuerySensori.evaluate();
            ServiceMap.logQuery(queryStringSensori, "API-sensori", sparqlType, nomeComune + ";" + textToSearch, 0);
            //if (!listaCategorie.contains("NearBusStops")) {
            if (!listaCategorie.contains("BusStop")) {    
                out.println("{\"Sensori\": ");
            } else {
                out.println(",\"Sensori\":");
            }
            out.println("{ "
                    + "\"type\": \"FeatureCollection\", "
                    + "\"features\": [ ");
            int s = 0;
            while (resultSensori.hasNext()) {
                // out.println(result);
                BindingSet bindingSetSensori = resultSensori.next();
                String valueOfId = bindingSetSensori.getValue("idSensore").stringValue();
                String valueOfIdService = bindingSetSensori.getValue("sensor").stringValue();
                String valueOfLat = bindingSetSensori.getValue("lat").stringValue();

                String valueOfLong = bindingSetSensori.getValue("long").stringValue();
                String valueOfAddress = bindingSetSensori.getValue("address").stringValue();

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
                        + "    \"nome\": \"" + escapeJSON(valueOfId) + "\", "
                        + "    \"tipo\": \"sensore\", "
                        + "    \"serviceUri\": \"" + valueOfIdService + "\", "
                        + "    \"indirizzo\": \"" + escapeJSON(valueOfAddress) + "\" "
                        + "},  "
                        + "\"id\": " + Integer.toString(s + 1) + "  "
                        + "}");

                s++;
                numeroSensori++;

            }
            out.println("]}");
            //if (categorie.equals("RoadSensor") || categorie.equals("RoadSensor;NearBusStops")) {
            if (categorie.equals("SensorSite") || categorie.equals("SensorSite;BusStop")) {
                out.println("}");
            }
        }
        int numeroServizi = 0;
        //if (!categorie.equals("NearBusStops") && !categorie.equals("RoadSensor") && !categorie.equals("RoadSensor;NearBusStops") && !categorie.equals("NearBusStops;RoadSensor")) {
        if (!categorie.equals("BusStop") && !categorie.equals("SensorSite") && !categorie.equals("SensorSite;BusStop") && !categorie.equals("BusStop;SensorSite")) {    
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
                    + "SELECT distinct ?ser ?serAddress ?serNumber ?elat ?elong ?sName ?sType ?email ?note ?labelIta ?multimedia ?description ?identifier WHERE {\n"
                    + " ?ser rdf:type km4c:Service" + (sparqlType.equals("virtuoso") ? " OPTION (inference \"urn:ontology\")" : "") + ".\n"
                    + " OPTIONAL{?ser schema:name ?sName. }\n"
                    + " ?ser schema:streetAddress ?serAddress.\n"
                    + " OPTIONAL {?ser km4c:houseNumber ?serNumber}.\n"
                    + " OPTIONAL {?ser dc:description ?description FILTER(LANG(?description) = \"it\")}\n"
                    + " OPTIONAL {?ser km4c:multimediaResource ?multimedia }\n"
                    + " OPTIONAL { ?ser dcterms:identifier ?identifier }\n"
                    + " OPTIONAL {?ser skos:note ?note }\n"
                    + " OPTIONAL {?ser schema:email ?email }\n"
                    + ServiceMap.textSearchQueryFragment("?ser", "?p", textToSearch)
                    + filtroLocalita
                    + fc
                    + " ?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService)\n"
                    + " ?sType rdfs:label ?labelIta. FILTER(LANG(?labelIta)=\"it\")\n"
                    + "}";
            if (!risultatiServizi.equals("0")) {
                queryStringServices += " LIMIT " + risultatiServizi;
            }
            TupleQuery tupleQueryServices = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringServices, km4cVersion));
            TupleQueryResult resultServices = tupleQueryServices.evaluate();
            ServiceMap.logQuery(queryStringServices, "API-servizi", sparqlType, nomeComune + ";" + textToSearch + ";" + categorie, 0);

            //if (!listaCategorie.contains("NearBusStops") && !listaCategorie.contains("RoadSensor")) {
            if (!listaCategorie.contains("BusStop") && !listaCategorie.contains("SensorSite")) {    
                out.println("{\"Servizi\": ");
            } else {
                out.println(", \"Servizi\": ");
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
                String valueOfSerAddress = bindingSetServices.getValue("serAddress").stringValue();
                String valueOfSerNumber = "";
                if (bindingSetServices.getValue("serNumber") != null) {
                    valueOfSerNumber = bindingSetServices.getValue("serNumber").stringValue();
                }
                String valueOfSType = bindingSetServices.getValue("sType").stringValue();
                String valueOfSTypeIta = "";
                if (bindingSetServices.getValue("labelIta") != null) {
                    valueOfSTypeIta = bindingSetServices.getValue("labelIta").stringValue();
                }
                String valueOfELat = bindingSetServices.getValue("elat").stringValue();
                String valueOfELong = bindingSetServices.getValue("elong").stringValue();
                String valueOfNote = "";
                if (bindingSetServices.getValue("note") != null) {
                    valueOfNote = bindingSetServices.getValue("note").stringValue();
                }

                String valueOfEmail = "";
                if (bindingSetServices.getValue("email") != null) {
                    valueOfEmail = bindingSetServices.getValue("email").stringValue();
                }

                //valueOfSTypeIta = Character.toLowerCase(valueOfSTypeIta.charAt(0)) + valueOfSTypeIta.substring(1);
                valueOfSTypeIta = valueOfSTypeIta.replace(" ", "_");
                valueOfSTypeIta = valueOfSTypeIta.replace("'", "");

                Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 ]+", "");

                valueOfEmail = valueOfEmail.replace("\"^^<http://www.w3.org/2001/XMLSchema#string>", "");
                valueOfEmail = valueOfEmail.replace("\"", "");

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
                        + "    \"nome\": \"" + escapeJSON(valueOfSName) + "\", "
                        + "    \"tipo\": \"" + escapeJSON(valueOfSTypeIta) + "\", "
                        + "    \"email\": \"" + valueOfEmail + "\", "
                        + "    \"note\": \"" + escapeJSON(valueOfNote) + "\", "
                        + "    \"serviceUri\": \"" + valueOfSer + "\", "
                        + "    \"indirizzo\": \"" + escapeJSON(valueOfSerAddress) + "\", \"numero\": \"" + escapeJSON(valueOfSerNumber) + "\" "
                        + "}, "
                        + "\"id\": " + Integer.toString(t + 1) + "  "
                        + "}");
                t++;
                numeroServizi++;
            }
            out.println("]}}");
        }
    }

    public void queryLatLngServices(JspWriter out, RepositoryConnection con, String[] coords, String categorie, String textToSearch, String raggioBus, String raggioSensori, String raggioServizi, String risultatiBus, String risultatiSensori, String risultatiServizi) throws Exception {
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
        //if (listaCategorieServizi.contains("NearBusStops")) {
        if (listaCategorieServizi.contains("BusStop")) {
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
                queryStringNearBusStop += " LIMIT " + risultatiBus;
            }

            TupleQuery tupleQueryNearBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringNearBusStop);
            TupleQueryResult resultNearBS = tupleQueryNearBusStop.evaluate();
            out.println("{\"Fermate\": ");
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
                TupleQueryResult resultLinee =  queryBusLines(valueOfNomeFermata, con);
                String valueOfLinee = "";
                if(resultLinee != null){
                  while (resultLinee.hasNext()) {
                      BindingSet bindingSetLinee = resultLinee.next();
                      String idLinee = bindingSetLinee.getValue("id").stringValue();
                      valueOfLinee = valueOfLinee + " - "+idLinee;
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
                        + "    \"nome\": \"" + escapeJSON(valueOfNomeFermata) + "\", "
                        + "    \"tipo\": \"fermata\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"NearBusStop\", "
                        + "    \"linee\": \"" + valueOfLinee.substring(3) + "\", "
                        // **********************************************
                        + "    \"serviceUri\": \"" + valueOfBS + "\" "
                        + "}, "
                        + "\"id\": " + Integer.toString(s + 1) + " "
                        + "}");
                s++;
                numeroBus++;
            }
            out.println("]}");
            //if (categorie.equals("NearBusStops")) {
            if (categorie.equals("BusStop")) {
                out.println("}");
            }
        }

        int numeroSensori = 0;
        //if (listaCategorieServizi.contains("RoadSensor")) {
        if (listaCategorieServizi.contains("SensorSite")) {    
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
                    + " ?sensor schema:streetAddress ?address.\n"
                    + "} ORDER BY ?dist";
            if (!risultatiSensori.equals("0")) {
                queryStringNearSensori += " LIMIT " + risultatiSensori;
            }

            TupleQuery tupleQuerySensori = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringNearSensori);
            TupleQueryResult resultNearSensori = tupleQuerySensori.evaluate();

            //if (!listaCategorieServizi.contains("NearBusStops")) {
            if (!listaCategorieServizi.contains("BusStop")) {    
                out.println("{\"Sensori\": ");
            } else {
                out.println(", \"Sensori\": ");
            }
            out.println("{ "
                    + "\"type\": \"FeatureCollection\", "
                    + "\"features\": [ ");

            while (resultNearSensori.hasNext()) {
                // out.println(result);
                BindingSet bindingSetNearSensori = resultNearSensori.next();
                String valueOfId = bindingSetNearSensori.getValue("idSensore").stringValue();
                String valueOfIdService = bindingSetNearSensori.getValue("sensor").stringValue();
                String valueOfLat = bindingSetNearSensori.getValue("lat").stringValue();

                String valueOfLong = bindingSetNearSensori.getValue("long").stringValue();
                String valueOfAddress = bindingSetNearSensori.getValue("address").stringValue();

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
                        + "    \"nome\": \"" + escapeJSON(valueOfId) + "\", "
                        + "    \"tipo\": \"sensore\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"RoadSensor\", "
                        // **********************************************
                        + "    \"serviceUri\": \"" + valueOfIdService + "\", "
                        + "    \"indirizzo\": \"" + escapeJSON(valueOfAddress) + "\" "
                        + "},  "
                        + "\"id\": " + Integer.toString(i + 1) + "  "
                        + "}");

                i++;
                numeroSensori++;
            }
            out.println("]}");
            //if (categorie.equals("RoadSensor")) {
            if (categorie.equals("SensorSite")) {    
                out.println("}");
            }
        }

        int numeroServizi = 0;
        //if (!categorie.equals("NearBusStops") && !categorie.equals("RoadSensor") && !categorie.equals("RoadSensor;NearBusStops") && !categorie.equals("NearBusStops;RoadSensor")) {
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
                    + " ?ser <http://schema.org/streetAddress> ?serAddress.\n"
                    + " OPTIONAL {?ser skos:note ?note}\n"
                    + " OPTIONAL {?ser dc:description ?description"
                    + " FILTER(LANG(?description) = \"it\")"
                    + " }\n"
                    + " OPTIONAL {?ser km4c:multimediaResource ?multimedia}\n"
                    + "	OPTIONAL {?ser schema:email ?email }\n"
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
                            ? " graph ?g {?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService)}\n"
                            + " ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory != <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>) "
                            + " ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\")\n" : "")
                    + "} ORDER BY ?dist";
            if (!risultatiServizi.equals("0")) {
                queryStringServiceNear += " LIMIT " + risultatiServizi;
            }

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringServiceNear);
            TupleQueryResult result = tupleQuery.evaluate();

            //if (!listaCategorieServizi.contains("NearBusStops") && !listaCategorieServizi.contains("RoadSensor")) {
            if (!listaCategorieServizi.contains("BusStop") && !listaCategorieServizi.contains("SensorSite")) {    
                out.println("{\"Servizi\": ");
            } else {
                out.println(", \"Servizi\": ");
            }
            out.println("{ "
                    + "\"type\": \"FeatureCollection\", "
                    + "\"features\": [ ");
            int w = 0;
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String valueOfSer = bindingSet.getValue("ser").stringValue();
                if(!IoTChecker.checkIoTService(valueOfSer, null)) {
                  continue;
                }
                String valueOfSerAddress = "";
                if (bindingSet.getValue("serAddress") != null) {
                    valueOfSerAddress = bindingSet.getValue("serAddress").stringValue();
                }
                String valueOfSType = "";
                if (bindingSet.getValue("sType") != null) {
                    valueOfSType = bindingSet.getValue("sType").stringValue();
                }
                String valueOfSTypeIta = "";
                if (bindingSet.getValue("sTypeIta") != null) {
                    valueOfSTypeIta = bindingSet.getValue("sTypeIta").stringValue();
                    //valueOfSTypeIta = Character.toLowerCase(valueOfSTypeIta.charAt(0)) + valueOfSTypeIta.substring(1);
                    valueOfSTypeIta = valueOfSTypeIta.replace(" ", "_");
                    valueOfSTypeIta = valueOfSTypeIta.replace("'", "");
                }
                // DICHIARAZIONE VARIABILI serviceType e serviceCategory per ICONA
                String subCategory = "";
                if (bindingSet.getValue("sType") != null) {
                    subCategory = bindingSet.getValue("sType").stringValue();
                    subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
                    subCategory = Character.toLowerCase(subCategory.charAt(0)) + subCategory.substring(1);
                    subCategory = subCategory.replace(" ", "_");
                }

                String category = "";
                if (bindingSet.getValue("sCategory") != null) {
                    category = bindingSet.getValue("sCategory").stringValue();
                    category = category.replace("http://www.disit.org/km4city/schema#", "");
                    category = Character.toLowerCase(category.charAt(0)) + category.substring(1);
                    category = category.replace(" ", "_");
                }

                String serviceType = category + "_" + subCategory;
                
                String valueOfSName = "";
                if (bindingSet.getValue("sName") != null) {
                    valueOfSName = bindingSet.getValue("sName").stringValue();
                }else{
                    valueOfSName = subCategory.replace("_", " ").toUpperCase();
                }
                String valueOfELat = bindingSet.getValue("elat").stringValue();
                String valueOfELong = bindingSet.getValue("elong").stringValue();
                String valueOfNote = "";
                if (bindingSet.getValue("note") != null) {
                    valueOfNote = bindingSet.getValue("note").stringValue();
                }

                String valueOfEmail = "";
                if (bindingSet.getValue("email") != null) {
                    valueOfEmail = bindingSet.getValue("email").stringValue();
                }

                Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 ]+", "");

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
                        + "    \"nome\": \"" + escapeJSON(valueOfSName) + "\", "
                        + "    \"tipo\": \"" + escapeJSON(valueOfSTypeIta) + "\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"" + escapeJSON(mapServiceType(serviceType)) + "\", "
                        // **********************************************
                        + "    \"email\": \"" + escapeJSON(valueOfEmail) + "\", "
                        + "    \"note\": \"" + escapeJSON(valueOfNote) + "\", "
                        + "    \"serviceUri\": \"" + escapeJSON(valueOfSer) + "\", "
                        + "    \"indirizzo\": \"" + escapeJSON(valueOfSerAddress) + "\" "
                        + "}, "
                        + "\"id\": " + Integer.toString(w + 1) + " "
                        + "}");
                w++;
                numeroServizi++;
            }
            out.println("]}}");
        }else{
            if(categorie.equals("SensorSite;BusStop") || categorie.equals("BusStop;SensorSite")){
                out.println("}");
            }
        }
    }

    public TupleQueryResult queryBusLines(String nameBusStop, RepositoryConnection con){
    String queryForLine = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
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
                + " { ?bs1 foaf:name \"" + ServiceMap.stringEncode(nameBusStop) + "\"."
                + " }UNION "
                + " {?bs2 foaf:name \"" + ServiceMap.stringEncode(nameBusStop) + "\" . "
                + " } "
                + "} ORDER BY ?id ";

        TupleQuery tupleQueryForLine;
        try {
            tupleQueryForLine = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForLine);
            long ts = System.nanoTime();
            TupleQueryResult result = tupleQueryForLine.evaluate();
            ServiceMap.logQuery(queryForLine,"API-buslines","",nameBusStop,System.nanoTime()-ts);
            return result;
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
            Logger.getLogger(ServiceMapApi.class.getName()).log(Level.SEVERE, null, ex);
        }
    return null;    
    }
    
    public JSONObject queryLocation(RepositoryConnection con, String lat, String lng, String findGeometry, Double wktDist) throws Exception {
      String sparqlType=Configuration.getInstance().get("sparqlType", "virtuoso");
      JSONObject obj = null;
      if(CheckParameters.checkLatLng(lat+";"+lng)!=null) {
        throw new IllegalArgumentException("invalid lat lng coordinates");
      }
              
      String query = ServiceMap.latLngToAddressQuery(lat, lng, sparqlType);
      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
      long ts = System.nanoTime();
      TupleQueryResult results = tupleQuery.evaluate();
      ServiceMap.logQuery(query,"API-get-address",sparqlType,lat+";"+lng,System.nanoTime()-ts);

      if (results.hasNext()) {
        obj = new JSONObject();
        BindingSet binding = results.next();
        String valueOfVia = binding.getValue("via").stringValue();
        String valueOfNumero = null;
        if(binding.getValue("numero")!=null)
          valueOfNumero = binding.getValue("numero").stringValue();
        String valueOfComune = binding.getValue("comune").stringValue();
        String valueOfUriComune = binding.getValue("uriComune").stringValue();
        String valueOfUriCivico = null;
        if(binding.getValue("uriCivico")!=null)
          valueOfUriCivico = binding.getValue("uriCivico").stringValue();
        String valueOfProvincia = null;
        if(binding.getValue("provincia") != null)
          valueOfProvincia = binding.getValue("provincia").stringValue();
        String valueOfUriProvincia = null;
        if(binding.getValue("uriProvincia") != null)
          valueOfUriProvincia = binding.getValue("uriProvincia").stringValue();
        String valueOfUriStrada = binding.getValue("uriStrada").stringValue();
        obj.put("address", valueOfVia);
        if(valueOfNumero!=null)
          obj.put("number", valueOfNumero);
        obj.put("addressUri", valueOfUriCivico);
        obj.put("municipality", valueOfComune);
        obj.put("municipalityUri", valueOfUriComune);
        if(valueOfProvincia != null)
          obj.put("province", valueOfProvincia);
        if(valueOfUriProvincia != null)
          obj.put("provinceUri", valueOfUriProvincia);        
        if(valueOfUriStrada!=null)
          obj.put("roadUri", valueOfUriStrada);
      }
      if(obj == null) {
        query = ServiceMap.latLngToMunicipalityQuery(lat, lng, sparqlType);
        tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
        results = tupleQuery.evaluate();
        //ServiceMap.logQuery(query,"get-municipality",sparqlType,lat+";"+lng,0);

        if (results.hasNext()) {
          obj = new JSONObject();
          BindingSet binding = results.next();
          String valueOfComune = binding.getValue("comune").stringValue();
          String valueOfUriComune = binding.getValue("uriComune").stringValue();
          String valueOfProvincia = binding.getValue("provincia").stringValue();
          String valueOfUriProvincia = binding.getValue("uriProvincia").stringValue();
          obj.put("municipality", valueOfComune);
          obj.put("municipalityUri", valueOfUriComune);
          obj.put("province", valueOfProvincia);
          obj.put("provinceUri", valueOfUriProvincia);
        }        
      }
      if(obj!=null && findGeometry!=null && (findGeometry.equals("true") || findGeometry.equals("geometry"))) {
        //double wktDist = Double.parseDouble(Configuration.getInstance().get("wktDistance", "0.0004"));
        query="select * {\n" +
            "{\n" +
            "select distinct ?s ?name ?class ?geo where {\n" +
            "?s <http://www.opengis.net/ont/geosparql#hasGeometry> [geo:geometry ?geo].\n" +
            "filter(bif:st_contains(?geo,bif:st_point("+lng+","+lat+"),0.01))\n" +
            "?s a ?class.\n" +
            "{?s <http://schema.org/name> ?name} UNION {?s foaf:name ?name}.\n" +
            "filter (?class!=km4c:RegularService && ?class!=km4c:DigitalLocation && ?class!=km4c:Route && ?class!=km4c:Tramline)\n" +
            "}\n" +
            "} UNION {\n" +
            "select (min(?t) as ?s) ?name (gtfs:Route as ?class) ?agency ?dir ?geo ?rtype {\n" +
            "?r a gtfs:Route.\n" +
            "OPTIONAL{?r gtfs:shortName ?sname.}\n" +
            "?r gtfs:longName ?lname.\n" +
            "?r gtfs:agency/foaf:name ?agency.\n" +
            "?t gtfs:route ?r.\n" +
            "?r gtfs:routeType ?rtype.\n" +
            "?t <http://www.opengis.net/ont/geosparql#hasGeometry> ?sh.\n" +
            "OPTIONAL{?t gtfs:headsign ?dir.}\n" +
            "?sh geo:geometry ?geo.\n" +
            "?t gtfs:service/dcterms:date ?d.\n" +
            "filter(xsd:date(?d)=xsd:date(now())) \n" +
            "filter(bif:st_contains(?geo,bif:st_point("+lng+","+lat+"),0.01))\n" +
            "BIND(IF(?sname,?sname,?lname) as ?name)\n" +
            "} group by ?sh ?name ?agency ?rtype ?dir ?geo\n" +
            "}\n" +
            "}order by ?agency ?name";
        tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
        //ServiceMap.println(query);
        results = tupleQuery.evaluate();
        ServiceMap.logQuery(query,"get-area",sparqlType,lat+";"+lng,0);

        JSONArray areas=new JSONArray();
        WKTReader wktReader=new WKTReader();
        Geometry position=wktReader.read("POINT("+lng+" "+lat+")");
        Geometry buffer=position.buffer(wktDist);
        position.setSRID(4326);
        ServiceMap.println("location check: "+lat+","+lng);
        while (results.hasNext()) {
          JSONObject area = new JSONObject();
          BindingSet binding = results.next();
          String s = binding.getValue("s").stringValue();
          String _class = binding.getValue("class").stringValue();
          String name = binding.getValue("name")==null ? "" : binding.getValue("name").stringValue();
          String geo = binding.getValue("geo").stringValue();
          String agency = binding.getValue("agency")==null ? null : binding.getValue("agency").stringValue();
          String routeType = binding.getValue("rtype")==null ? null : binding.getValue("rtype").stringValue();
          String direction = binding.getValue("dir")==null ? null : binding.getValue("dir").stringValue();
          
          try {
            Geometry g=wktReader.read(geo);
            g.setSRID(4326);
            //ServiceMap.println("dist: "+ g.distance(position));
            if(g.intersects(buffer)) {
              area.put("uri", s);
              area.put("class", _class);
              area.put("name", name);
              if(g instanceof Polygon)
                area.put("type", "Polygon");
              else if(g instanceof LineString)
                area.put("type", "LineString");
              else if(g instanceof Point)
                area.put("type", "Point");
              if(agency!=null)
                area.put("agency", agency);
              if(routeType!=null)
                area.put("routeType", routeType.replace("http://vocab.gtfs.org/terms#",""));
              if(direction!=null)
                area.put("direction", direction);
              if(findGeometry.equals("geometry"))
                area.put("geometry", geo);
              area.put("distance", g.distance(position));
              areas.add(area);
              //ServiceMap.println("INCLUDED "+name+" "+ _class+" dist:"+g.distance(position));
            }
            else {
              //ServiceMap.println("excluded "+name+" "+ _class+" dist:"+g.distance(position));
            }
          }catch(Exception e) {
            ServiceMap.notifyException(e,"name: "+name+" uri:"+s);
          }
        }
        obj.put("intersect", areas);
      }
      return obj;
    }
    
    public int queryLocation(JspWriter out, RepositoryConnection con, String lat, String lng, String findArea, double wktDist) throws Exception {
      JSONObject obj = queryLocation(con, lat, lng, findArea, wktDist);
      if(obj!=null)
        out.print(obj.toString());
      else
        out.println("{}");
      return 1;
    }
    
    static private Map<String,String> serviceTypeMap = null;
    static private Map<String,String> serviceTypeCategMap = null;
    static public String mapServiceType(String st) {
      if(serviceTypeMap == null) {
          String[][] t1=new String[][]{
            {"shoppingAndService_hypermarket", "shopping_hypermarket"},
            {"shoppingAndService_shopping_centre", "shopping_shopping_centre"},
            {"shoppingAndService_tobacco_shop", "shopping_tobacco_shop"},
            {"transferServiceAndRenting_fuel_station", "transferService_fuel_station"},
            {"shoppingAndService_bookshop", "shopping_bookshop"},
            {"shoppingAndService_pharmacy", "emergency_pharmacy"},
            {"transferServiceAndRenting_car_park", "transferService_car_park"},
            {"transferServiceAndRenting_courier", "transferService_courier"},
            {"agricultureAndLivestock_veterinary", "healthCare_veterinary"},
            {"transferServiceAndRenting_vehicle_rental", "transferService_vehicle_rental"},
            {"transferServiceAndRenting_bike_rental", "transferService_bike_rental"},
            {"educationAndResearch_training_school", "education_training_school"},
            {"educationAndResearch_language_courses", "education_language_courses"},
            {"culturalActivity_theatre", "entertainment_theatre"},
            {"shoppingAndService_jeweller", "shopping_jeweller"},
            {"transferServiceAndRenting_sensorSite", "transferService_sensorSite"},
            {"transferServiceAndRenting_busStop", "transferService_busStop"},
            {"environment_weather_sensor", "environmentAndAgriculture_weather_sensor"},
            {"shoppingAndService_mechanic_workshop", "tourismService_mechanic_workshop"},
            {"educationAndResearch_ski_school", "education_ski_school"},
            {"educationAndResearch_diving_school", "education_diving_school"},
            {"educationAndResearch_sailing_school", "education_sailing_school"},
            {"educationAndResearch_private_junior_school", "education_private_junior_school"},
            {"educationAndResearch_public_junior_school", "education_public_junior_school"},
            {"educationAndResearch_private_infant_school", "education_private_infant_school"},
            {"educationAndResearch_public_infant_school", "education_public_infant_school"},
            {"educationAndResearch_private_junior_high_school", "education_private_junior_high_school"},
            {"educationAndResearch_public_junior_high_school", "education_public_junior_high_school"},
            {"educationAndResearch_training_school_for_teachers", "education_training_school_for_teachers"},
            {"educationAndResearch_private_professional_institute", "education_private_professional_institute"},
            {"educationAndResearch_public_professional_institute", "education_public_professional_institute"},
            {"educationAndResearch_private_polytechnic_school", "education_private_polytechnic_school"},
            {"educationAndResearch_public_polytechnic_school", "education_public_polytechnic_school"},
            {"educationAndResearch_private_high_school", "education_private_high_school"},
            {"educationAndResearch_public_high_school", "education_public_high_school"},
            {"educationAndResearch_conservatory", "education_conservatory"},
            {"educationAndResearch_public_university", "education_public_university"},
            {"shoppingAndService_non_food_large_retailers", "shopping_non_food_large_retailers"},
            {"shoppingAndService_single_brand_store", "shopping_single_brand_store"},
            {"shoppingAndService_clothing_factory_outlet", "shopping_clothing_factory_outlet"},
            {"shoppingAndService_artisan_shop", "shopping_artisan_shop"},
            {"transferServiceAndRenting_civil_airport", "transferService_civil_airport"},
            {"transferServiceAndRenting_helipads", "transferService_helipads"},
            {"transferServiceAndRenting_urban_bus", "transferService_urban_bus"},
            {"transferServiceAndRenting_taxi_park", "transferService_taxi_park"},
            {"transferServiceAndRenting_bus_tickets_retail", "transferService_bus_tickets_retail"},
            {"transferServiceAndRenting_airfields", "transferService_airfields"},
            {"transferServiceAndRenting_train_station", "transferService_train_station"},
            {"educationAndResearch_private_preschool", "education_private_preschool"},
            {"shoppingAndService_footwear_factory_outlet", "shopping_footwear_factory_outlet"},
            {"transferServiceAndRenting_rTZgate", "transferService_rTZgate"},
            {"transferServiceAndRenting_controlled_parking_zone", "transferService_controlled_parking_zone"},
            {"transferServiceAndRenting_cycle_paths", "transferService_cycle_paths"},
            {"transferServiceAndRenting_tram_stops", "transferService_tram_stops"},
            {"transferServiceAndRenting_tramline", "transferService_tramline"},
            {"transferServiceAndRenting_bike_rack", "transferService_bike_rack"},
            {"transferServiceAndRenting_charging_stations", "transferService_charging_stations"},
            {"environment_photovoltaic_system", "environmentAndAgriculture_photovoltaic_system"}
          };
          String[] t2 = {
            "accommodation_agritourism",
            "accommodation_beach_resort",
            "accommodation_bed_and_breakfast",
            "accommodation_boarding_house",
            "accommodation_camping",
            "accommodation_day_care_centre",
            "accommodation_farm_house",
            "accommodation_historic_residence",
            "accommodation_holiday_village",
            "accommodation_hostel",
            "accommodation_hotel",
            "accommodation_religiuos_guest_house",
            "accommodation_rest_home",
            "accommodation_summer_residence",
            "accommodation_vacation_resort",
            "culturalActivity_archaeological_site",
            "culturalActivity_auditorium",
            "culturalActivity_churches",
            "culturalActivity_cultural_centre",
            "culturalActivity_historical_buildings",
            "culturalActivity_library",
            "culturalActivity_monument_location",
            "culturalActivity_museum",
            "culturalActivity_squares",
            "emergency_carabinieri",
            "emergency_civil_protection",
            "emergency_coast_guard_harbormaster",
            "emergency_commissariat_of_public_safety",
            "emergency_corps_of_forest_rangers",
            "emergency_emergency_medical_care",
            "emergency_emergency_services",
            "emergency_fire_brigade",
            "emergency_first_aid",
            "emergency_italian_finance_police",
            "emergency_local_police",
            "emergency_traffic_corps",
            "emergency_useful_numbers",
            "entertainment_aquarium",
            "entertainment_boxoffice",
            "entertainment_cinema",
            "entertainment_climbing",
            "entertainment_discotheque",
            "entertainment_fishing_reserve",
            "entertainment_game_reserve",
            "entertainment_game_room",
            "entertainment_gardens",
            "entertainment_golf",
            "entertainment_green_areas",
            "entertainment_gym_fitness",
            "entertainment_hippodrome",
            "entertainment_pool",
            "entertainment_rafting_kayak",
            "entertainment_recreation_room",
            "entertainment_riding_stables",
            "entertainment_skiing_facility",
            "entertainment_social_centre",
            "entertainment_sports_facility",
            "financialService_atm",
            "financialService_bank",
            "financialService_financial_institute",
            "financialService_insurance",
            "governmentOffice_airport_lost_property_office",
            "governmentOffice_consulate",
            "governmentOffice_department_of_motor_vehicles",
            "governmentOffice_district",
            "governmentOffice_employment_exchange",
            "governmentOffice_income_revenue_authority",
            "governmentOffice_other_office",
            "governmentOffice_police_headquarters",
            "governmentOffice_postal_office",
            "governmentOffice_prefecture",
            "governmentOffice_social_security_service_office",
            "governmentOffice_train_lost_property_office",
            "governmentOffice_welfare_worker_office",
            "governmentOffice_youth_information_centre",
            "healthCare_addiction_recovery_centre",
            "healthCare_community_centre",
            "healthCare_dentist",
            "healthCare_doctor_office",
            "healthCare_family_counselling",
            "healthCare_group_practice",
            "healthCare_healthcare_centre",
            "healthCare_health_district",
            "healthCare_health_reservations_centre",
            "healthCare_local_health_authority",
            "healthCare_mental_health_centre",
            "healthCare_physical_therapy_centre",
            "healthCare_poison_control_centre",
            "healthCare_private_clinic",
            "healthCare_public_hospital",
            "healthCare_red_cross",
            "healthCare_senior_centre",
            "healthCare_youth_assistance",
            "tourismService_beacon",
            "tourismService_camper_service",
            "tourismService_fresh_place",
            "tourismService_pedestrian_zone",
            "tourismService_toilet",
            "tourismService_tourist_complaints_office",
            "tourismService_tourist_information_office",
            "tourismService_tourist_trail",
            "tourismService_tour_operator",
            "tourismService_travel_agency",
            "tourismService_travel_bureau",
            "tourismService_wifi",
            "wineAndFood_bakery",
            "wineAndFood_catering",
            "wineAndFood_dining_hall",
            "wineAndFood_drinking_fountain",
            "wineAndFood_grill",
            "wineAndFood_highway_stop",
            "wineAndFood_ice_cream_parlour",
            "wineAndFood_literary_cafe",
            "wineAndFood_pastry_shop",
            "wineAndFood_pizzeria",
            "wineAndFood_restaurant",
            "wineAndFood_sandwich_shop_pub",
            "wineAndFood_small_shop",
            "wineAndFood_sushi_bar",
            "wineAndFood_trattoria",
            "wineAndFood_wine_shop_and_wine_bar"
          };
          serviceTypeMap = new HashMap<>();
          for(int i=0; i<t1.length; i++)
            serviceTypeMap.put(t1[i][0], t1[i][1]);
          for(int i=0; i<t2.length; i++)
            serviceTypeMap.put(t2[i], t2[i]);
      }
      String r = serviceTypeMap.get(st);
      if(r==null) {
        if(serviceTypeCategMap==null) {
          String[][] t = {
            {"accommodation", "accommodation"},
            {"advertising", "shopping"},
            {"agricultureAndLivestock","environmentAndAgriculture"},
            {"civilAndEdilEngineering","financialService"},
            {"culturalActivity","culturalActivity"},
            {"educationAndResearch","education"},
            {"emergency","emergency"},
            {"entertainment","entertainment"},
            {"environment","environmentAndAgriculture"},
            {"financialService","financialService"},
            {"governmentOffice","governmentOffice"},
            {"healthCare","healthCare"},
            {"industryAndManufacturing","financialService"},
            {"miningAndQuarrying","financialService"},
            {"shoppingAndService","shopping"},
            {"tourismService","tourismService"},
            {"transferServiceAndRenting","transferService"},
            {"utilitiesAndSupply","shopping"},
            {"wholesale","shopping"},
            {"wineAndFood","wineAndFood"}
          };
          serviceTypeCategMap = new HashMap<>();
          for(int i=0; i<t.length; i++)
            serviceTypeCategMap.put(t[i][0], t[i][1]);
        }
        String categ = st.substring(0, st.indexOf("_"));
        r = serviceTypeCategMap.get(categ);
        if(r==null)
          r = "?"+categ;
      }
      return r;
    }
}
