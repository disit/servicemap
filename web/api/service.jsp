<%@page import="org.disit.servicemap.ServiceMapping"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
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
<%@page trimDirectiveWhitespaces="true" %>
<%@page import="java.text.Normalizer"%>
<%@include file= "/include/parameters.jsp" %>
<%
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

  RepositoryConnection con = ServiceMap.getSparqlConnection();
  String idService = request.getParameter("serviceUri");
  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  //ServiceMap.println("MIC-----------------  "+idService);
  ServiceMap.logAccess(request, null, null, null, idService, "ui-service-info", null, null, null, null, null, null, null);

  String queryString = "";
  String filtroQuery = "";
  int i = 0;
  long s = System.nanoTime();

  List<String> types = ServiceMap.getTypes(con, idService);
  //for(int x=0; x<types.size(); x++)
  //  ServiceMap.println(idService+" types: "+types.get(x));
  
  try {
    //se esiste un mapping per un tipo associato al servizio usa le API altrimenti ritorna al vecchio codice
    ServiceMapping.MappingData serviceMapping = ServiceMapping.getInstance().getMappingForServiceType(1, types);
    if (serviceMapping!=null && (serviceMapping.detailsQuery!=null || 
            serviceMapping.realTimeSparqlQuery!=null || 
            serviceMapping.realTimeSqlQuery!=null || 
            serviceMapping.realTimeSolrQuery!=null)) {
      ServiceMapApiV1 api = new ServiceMapApiV1();
      api.queryService(out, con, idService, "en", "true", null, null, "false", null, types);
    } else if ((types.contains("BusStop") || types.contains("Tram_stops") || types.contains("Train_station") || types.contains("Ferry_stop")) && !types.contains("DigitalLocation")) {
      String nomeFermata = "";
      String queryStringBusStop = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "SELECT distinct ?nomeFermata ?sType ?bslat ?bslong ?agname WHERE {\n"
              + "	<" + idService + "> \n"
              + "  a ?sType;"
              + "	 foaf:name ?nomeFermata;\n"
              + "	 geo:lat ?bslat;\n"
              + "	 geo:long ?bslong;\n"
              + "  gtfs:agency ?ag."
              //+ "  {?st gtfs:stop <"+idService+">.}UNION{?st gtfs:stop [owl:sameAs <"+idService+">]}" 
              //+ "  ?st gtfs:trip ?t."
              //+ "   ?t gtfs:route/gtfs:agency ?ag."
              + "   ?ag foaf:name ?agname."
              + "  FILTER(?sType!=gtfs:Stop)"
              + "}LIMIT 1";
      //ServiceMap.println("MIC service.jsp -----------------  "+queryStringBusStop);

      TupleQuery tupleQueryBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringBusStop);
      TupleQueryResult busStopResult = tupleQueryBusStop.evaluate();
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      try {
        i = 0;
        while (busStopResult.hasNext()) {
          BindingSet bindingSetBusStop = busStopResult.next();
          String valueOfBSLat = bindingSetBusStop.getValue("bslat").stringValue();
          String valueOfBSLong = bindingSetBusStop.getValue("bslong").stringValue();
          nomeFermata = bindingSetBusStop.getValue("nomeFermata").stringValue();
          String agency = bindingSetBusStop.getValue("agname").stringValue();
          String serviceType = bindingSetBusStop.getValue("sType").stringValue();
          serviceType = serviceType.replace("http://www.disit.org/km4city/schema#", "");
          if (i != 0) {
            out.println(", ");
          }
          float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);
          out.println("{\n"
                  + " \"geometry\": {\n"
                  + "     \"type\": \"Point\",\n"
                  + "    \"coordinates\": [" + valueOfBSLong + ","+ valueOfBSLat + "]\n"
                  + "},\n"
                  + "\"type\": \"Feature\",\n"
                  + "\"properties\": {\n"
                  + "    \"popupContent\": \"" + escapeJSON(nomeFermata) + " - fermata\",\n"
                  + "    \"name\": \"" + escapeJSON(nomeFermata) + "\",\n"
                  + "    \"agency\": \"" + escapeJSON(agency) + "\",\n"
                  + "    \"serviceUri\": \"" + idService + "\",\n"
                  + "    \"tipo\": \"fermata\",\n"
                  + "    \"serviceType\": \"TransferServiceAndRenting_" + serviceType + "\",\n"
                  + "    \"photos\": " + ServiceMap.getServicePhotos(idService) + ",\n"
                  + "    \"avgStars\": " + avgServiceStars[0] + ",\n"
                  + "    \"starsCount\": " + (int)avgServiceStars[1] + ",\n"
                  + "    \"comments\": " + ServiceMap.getServiceComments(idService)
                  + "},\n"
                  + "\"id\": " + Integer.toString(i + 1) + "\n"
                  + "}\n");
          i++;
        }
      } catch (Exception e) {
        out.println(e.getMessage());
      }
      out.println("] "
              + "}");
    } /*else if (types.contains("Tram_stops")) {
      String nomeFermata = "";
      String queryStringBusStop = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "SELECT distinct ?nomeFermata ?bslat ?bslong WHERE {\n"
              + "	<" + idService + "> rdf:type km4c:Tram_stops;\n"
              + "	 foaf:name ?nomeFermata;\n"
              + "	 geo:lat ?bslat;\n"
              + "	 geo:long ?bslong.\n"
              + "}LIMIT 1";

      TupleQuery tupleQueryBusStop = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringBusStop);
      TupleQueryResult busStopResult = tupleQueryBusStop.evaluate();
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      try {
        i = 0;
        while (busStopResult.hasNext()) {
          BindingSet bindingSetBusStop = busStopResult.next();
          String valueOfBSLat = bindingSetBusStop.getValue("bslat").stringValue();
          String valueOfBSLong = bindingSetBusStop.getValue("bslong").stringValue();
          nomeFermata = bindingSetBusStop.getValue("nomeFermata").stringValue();
          if (i != 0) {
            out.println(", ");
          }
          float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);
          out.println("{\n"
                  + " \"geometry\": {\n"
                  + "     \"type\": \"Point\",\n"
                  + "    \"coordinates\": [" + valueOfBSLong + ","+ valueOfBSLat + "]\n"
                  + "},\n"
                  + "\"type\": \"Feature\",\n"
                  + "\"properties\": {\n"
                  + "    \"popupContent\": \"" + nomeFermata + " - fermata\",\n"
                  + "    \"name\": \"" + nomeFermata + "\",\n"
                  + "    \"serviceUri\": \"" + idService + "\",\n"
                  + "    \"tipo\": \"fermata\",\n"
                  + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\",\n"
                  + "    \"photos\": "+ServiceMap.getServicePhotos(idService) + ",\n"
                  + "    \"avgStars\": "+avgServiceStars[0] + ",\n"
                  + "    \"starsCount\": "+(int)avgServiceStars[1] + ",\n"
                  + "    \"comments\": "+ServiceMap.getServiceComments(idService)
                  + "},\n"
                  + "\"id\": " + Integer.toString(i + 1) + "\n"
                  + "}\n");
          i++;
        }
      } catch (Exception e) {
        out.println(e.getMessage());
      }
      out.println("] "
              + "}");
    }*/ else if (types.contains("SensorSite")) {
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      String querySensor = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "SELECT DISTINCT ?lat ?long ?address ?idSensore WHERE{\n"
              + " <" + idService + "> rdf:type km4c:SensorSite;\n"
              + "  geo:lat ?lat;\n"
              + "  geo:long ?long;\n"
              + "  dcterms:identifier ?idSensore;\n"
              + "  schema:streetAddress ?address.\n"
              + " FILTER regex(str(?lat), \"^4\").\n"
              + "} LIMIT 1";

      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, querySensor);
      TupleQueryResult result = tupleQuery.evaluate();

      while (result.hasNext()) {
        BindingSet bindingSetSensore = result.next();
        String valueOfId = bindingSetSensore.getValue("idSensore").stringValue();
        String valueOfLat = bindingSetSensore.getValue("lat").stringValue();
        String valueOfLong = bindingSetSensore.getValue("long").stringValue();
        String valueOfAddress = bindingSetSensore.getValue("address").stringValue();
         String valueOfDBpedia = "";

        if (i != 0) {
          out.println(", ");
        }

        float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);
        out.println("{ "
                + " \"geometry\": {  "
                + "     \"type\": \"Point\",  "
                + "    \"coordinates\": [  "
                + "      " + valueOfLong + ",  "
                + "      " + valueOfLat + "  "
                + " ]  "
                + "},  "
                + "\"type\": \"Feature\",  "
                + "\"properties\": {  "
                + "    \"popupContent\": \"" + valueOfId + " - sensore\", "
                + "    \"name\": \"" + valueOfId + "\", "
                + "    \"tipo\": \"sensore\", "
                + "    \"tipologia\": \"TransferServiceAndRenting - SensorSite\", "
                + "    \"serviceUri\": \"" + idService + "\", "
                + "    \"address\": \"" + valueOfAddress + "\", "
                + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\",\n"
                + "    \"photos\": "+ServiceMap.getServicePhotos(idService) + ",\n"
                + "    \"avgStars\": "+avgServiceStars[0] + ",\n"
                + "    \"starsCount\": "+(int)avgServiceStars[1] + ",\n"
                + "    \"comments\": "+ServiceMap.getServiceComments(idService)
                + "},  "
                + "\"id\": " + Integer.toString(i + 1) + "  "
                //  + "\"query\": " + queryString + " "
                + "}");
        i++;
      }
      out.println("] "
              + "}");
    } else if (types.contains("WeatherReport")) {
      String queryForComune = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX time:<http://www.w3.org/2006/time#>\n"
              + "SELECT DISTINCT ?nomeComune WHERE{\n"
              + " <" + idService + "> km4c:refersToMunicipality ?mun.\n"
              + " ?mun foaf:name ?nomeComune ."
              + "}";
      TupleQuery tupleQueryComuneMeteo = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForComune);
      TupleQueryResult resultComuneMeteo = tupleQueryComuneMeteo.evaluate();
      String nomeComune = "";
      try {
        i = 0;
        while (resultComuneMeteo.hasNext()) {
          BindingSet bindingSetComuneMeteo = resultComuneMeteo.next();
          nomeComune = bindingSetComuneMeteo.getValue("nomeComune").stringValue();
          out.println("{ \"meteo\": {"
                  + "\"location\": "
                  + "\"" + nomeComune + "\""
                  + "}}");
        }

      } catch (Exception e) {
        out.println(e.getMessage());
      }
    } else if (types.contains("Service") || types.contains("RegularService") || types.contains("TransverseService")) {
      String queryStringService = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "SELECT ?serAddress ?serNumber ?elat ?elong ?lsUri ?lsName (IF(?sName1,?sName1,?sName2) as ?sName) ?sNum ?sType ?type ?sTypeIta ?sCategory ?email ?note ?description ?multimedia ?phone ?fax ?website ?prov ?city ?cap ?DLtype ?cordList WHERE{\n"
              + " BIND(<" + idService + "> as ?ser)\n"
              + " OPTIONAL {{\n"
              + "  ?ser km4c:hasAccess ?entry.\n"
              + "  ?entry geo:lat ?elat;\n"
              + "   geo:long ?elong.\n"
              + " } UNION {\n"
              + "  ?ser geo:lat ?elat;\n"
              + "   geo:long ?elong.\n"
              + " }}\n"
              + " OPTIONAL {?ser schema:name ?sName1.}\n"
              + " OPTIONAL {?ser foaf:name ?sName2.}\n"
              + " OPTIONAL {?ser schema:streetAddress ?serAddress.}\n"
              + " OPTIONAL { {<"+idService+"> km4c:hasRegularService ?lsUri. OPTIONAL { ?lsUri schema:name ?lsName. }}\n"
              + "  UNION {<"+idService+"> km4c:hasTransverseService ?lsUri. OPTIONAL { ?lsUri schema:name ?lsName. }}}\n"
              + " OPTIONAL {?ser a ?DLtype . FILTER(?DLtype=km4c:DigitalLocation)}\n"
              + " OPTIONAL {?ser opengis:hasGeometry ?geometry .\n"
              + " ?geometry opengis:asWKT ?cordList .}\n"
              + (km4cVersion.equals("old")
                      ? " ?ser km4c:hasServiceCategory ?cat."
                      + " ?cat rdfs:label ?nome."
                      + " BIND (?nome  AS ?sType)"
                      + " BIND (?nome  AS ?sTypeIta)"
                      + " FILTER(LANG(?nome) = \"it\")"
                      : " ?ser a ?type. FILTER(?type!=km4c:RegularService && ?type!=km4c:Service && ?type!=km4c:DigitalLocation && ?type!=km4c:TransverseService)\n"
                      + " ?type rdfs:label ?nome.\n"
                      + " ?type rdfs:subClassOf ?sCategory.\n"
                      + " BIND (?nome  AS ?sType)\n"
                      + " BIND (?nome  AS ?sTypeIta)\n"
                      + " FILTER(LANG(?nome) = \"it\")\n")
              + " OPTIONAL {?ser dcterms:description ?description}\n" // FILTER(LANG(?description) = \"it\")}"
              + " OPTIONAL {?ser km4c:multimediaResource ?multimedia}\n"
              + " OPTIONAL {?ser km4c:houseNumber ?serNumber}\n"
              + " OPTIONAL {?ser skos:note ?note}\n"
              + " OPTIONAL {?ser schema:email ?email}\n"
              // AGGIUNTA CAMPI DA VISUALIZZARE NELLA SCHEDA
              + " OPTIONAL {?ser schema:faxNumber ?fax}\n"
              + " OPTIONAL {?ser schema:telephone ?phone}\n"
              + " OPTIONAL {?ser schema:addressRegion ?prov}\n"
              + " OPTIONAL {?ser schema:addressLocality ?city}\n"
              + " OPTIONAL {?ser schema:postalCode ?cap}\n"
              + " OPTIONAL {?ser schema:url ?website}\n"
              //+ " OPTIONAL {?ser km4c:isInRoad ?road.}\n"
              //+ " OPTIONAL {?road cito:cites ?linkDBpedia.}\n"
              // ---- FINE CAMPI AGGIUNTI ---
              + "}LIMIT 1";
      
      //ServiceMap.println("MICHELA-----------------  "+queryStringService);
      //PROVA QUERY DBPEDIA
      String queryDBpedia = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX cito:<http://purl.org/spar/cito/>\n"
              + "SELECT ?linkDBpedia WHERE{\n"
              + " OPTIONAL {<" + idService + "> km4c:isInRoad ?road.\n"
              + "?road cito:cites ?linkDBpedia.}\n"
              + "}";
      
      //ServiceMap.println(queryDBpedia);
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      
      
      TupleQuery tupleQueryService = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringService);
      long start = System.nanoTime();
      TupleQueryResult resultService = tupleQueryService.evaluate();
      logQuery(filterQuery(queryStringService), "get-service-details", "any", idService, System.nanoTime() - start);
      
      TupleQuery tupleQueryDBpedia = con.prepareTupleQuery(QueryLanguage.SPARQL, queryDBpedia);
      long start2 = System.nanoTime();
      TupleQueryResult resultDBpedia = tupleQueryDBpedia.evaluate();
      logQuery(filterQuery(queryDBpedia), "get-link-DBpedia", "any", idService, System.nanoTime() - start2);
      String valueOfDBpedia = "[";
      
      // out.println(queryString);
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
        // Controllo categoria SensorSite e BusStop per ricerca testuale.
        // Da testare
        /*String serviceType = "";
        if (subCategory.equals("SensorSite")) {
          serviceType = "RoadSensor";
        } else if (subCategory.equals("BusStop")) {
          serviceType = "NearBusStop";
        } else {
          serviceType = category + "_" + subCategory;
        }*/
        String serviceType = category + "_" + subCategory;

        String valueOfSName = "";
        if (bindingSetService.getValue("sName") != null) {
          valueOfSName = bindingSetService.getValue("sName").stringValue();
        } else {
          valueOfSName = subCategory.replace("_", " ").toUpperCase();
        }

        String valueOfSTypeIta = "";
        if (bindingSetService.getValue("sTypeIta") != null) {
          valueOfSTypeIta = bindingSetService.getValue("sTypeIta").stringValue();
        }

        String valueOfELat = "";
        if (bindingSetService.getValue("elat") != null) {
          valueOfELat = bindingSetService.getValue("elat").stringValue();
        }
        String valueOfELong = "";
        if (bindingSetService.getValue("elong") != null) {
          valueOfELong = bindingSetService.getValue("elong").stringValue();
        }
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
        if (bindingSetService.getValue("description") != null) {
          valueOfDescriptionIta = bindingSetService.getValue("description").stringValue();
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

        String valueOfDL = "";
        if (bindingSetService.getValue("DLtype") != null) {
          valueOfDL = bindingSetService.getValue("DLtype").stringValue();
        }

        String valueOfCap = "";
        if (bindingSetService.getValue("cap") != null) {
          valueOfCap = bindingSetService.getValue("cap").stringValue();
        }

        String valueOfCordList = "";
        if ((bindingSetService.getValue("cordList") != null) && (!bindingSetService.getValue("cordList").stringValue().contains("POINT"))) {
          valueOfCordList = bindingSetService.getValue("cordList").stringValue();
          valueOfCordList = valueOfCordList.replace("^^<gis:wktLiteral>", "");
        }

        // ---- FINE AGGIUNTA ---
        if (valueOfSTypeIta.length() > 0) {
          //valueOfSTypeIta = Character.toLowerCase(valueOfSTypeIta.charAt(0)) + valueOfSTypeIta.substring(1);
          valueOfSTypeIta = valueOfSTypeIta.replace(" ", "_");
          valueOfSTypeIta = valueOfSTypeIta.replaceAll("[^\\P{Punct}_]+", "");
        }

        Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 ]+", "");

        String valueOfLSUri = "";
        String valueOfLSName = "";
        if (bindingSetService.getValue("lsUri") != null) {
          valueOfLSUri = bindingSetService.getValue("lsUri").stringValue();
        }
        if (bindingSetService.getValue("lsName") != null) {
          valueOfLSName = bindingSetService.getValue("lsName").stringValue();
        }
        if (i != 0) {
          out.println(", ");
        }

        float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);
        out.println("{ "
                + " \"geometry\": {\n"
                + "     \"type\": \"Point\""
                + (!"".equals(valueOfELat) && !"".equals(valueOfELong)
                        ? ",\"coordinates\": [" + valueOfELong + "," + valueOfELat + "]\n"
                        : "")
                + "},\n"
                + "\"type\": \"Feature\",\n"
                + "\"properties\": {\n"
                + "    \"popupContent\": \"" + escapeJSON(valueOfSName) + " - " + escapeJSON(valueOfSType) + "\",\n"
                + "    \"name\": \"" + escapeJSON(valueOfSName) + "\",\n"
                + "    \"tipo\": \"" + escapeJSON(valueOfSTypeIta) + "\",\n"
                + "    \"tipologia\": \"" + category + " - " + subCategory + "\",\n"
                + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\",\n"
                + "    \"cordList\": \"" + escapeJSON(valueOfCordList) + "\",\n"
                + "    \"phone\": \"" + valueOfPhone + "\",\n"
                + "    \"fax\": \"" + valueOfFax + "\",\n"
                + "    \"website\": \"" + valueOfUrl + "\",\n"
                + "    \"province\": \"" + valueOfProv + "\",\n"
                + "    \"city\": \"" + valueOfCity + "\",\n"
                + "    \"cap\": \"" + valueOfCap + "\",\n"
                + "    \"digitalLocation\": \"" + valueOfDL + "\",\n"
                + "    \"linkserUri\": \"" + escapeJSON(valueOfLSUri) + "\",\n"
                + "    \"linkedService\": \"" + escapeJSON(valueOfLSName) + "\",\n"
                + "    \"email\": \"" + escapeJSON(valueOfEmail) + "\",\n"
                + "    \"linkDBpedia\": " + valueOfDBpedia + ",\n"
                + "    \"note\": \"" + escapeJSON(valueOfNote) + "\",\n"
                + "    \"description\": \"" + escapeJSON(valueOfDescriptionIta) + "\",\n"
                + "    \"multimedia\": \"" + escapeJSON(valueOfMultimediaResource) + "\",\n"
                + "    \"serviceUri\": \"" + idService + "\",\n"
                + "    \"address\": \"" + escapeJSON(valueOfSerAddress) + "\", \"civic\": \"" + escapeJSON(valueOfSerNumber) + "\",\n"
                + "    \"photos\": "+ServiceMap.getServicePhotos(idService) + ",\n"
                + "    \"avgStars\": "+avgServiceStars[0] + ",\n"
                + "    \"starsCount\": "+(int)avgServiceStars[1] + ",\n"
                + "    \"comments\": "+ServiceMap.getServiceComments(idService)
                + "},\n"
                + "\"id\": " + Integer.toString(i + 1) + "  "
                // + "\"query\": \"" + queryString + "\" "
                + "}");
        i++;
      }
      out.println("] "
              + "}");
    }
    else { //is not a service
      String queryStringNoService = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX schema:<http://schema.org/>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
              + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
              + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "SELECT ?elat ?elong ?name ?type ?typeLabel ?description WHERE{\n"
              + " BIND(<" + idService + "> as ?ser)\n"
              + " OPTIONAL {{\n"
              + "  ?ser km4c:hasAccess ?entry.\n"
              + "  ?entry geo:lat ?elat;\n"
              + "   geo:long ?elong.\n"
              + " } UNION {\n"
              + "  ?ser geo:lat ?elat;\n"
              + "   geo:long ?elong.\n"
              + " }}\n"
              + " <" + idService + "> a ?type. FILTER(?type!=km4c:RegularService && ?type!=km4c:Service && ?type!=km4c:DigitalLocation && ?type!=km4c:TransverseService)\n"
              + " ?type rdfs:label ?typeLabel filter(lang(?typeLabel)=\"en\").\n"              
              + " OPTIONAL {{?ser rdfs:label ?name.}UNION{?ser schema:name ?name.}UNION{?ser foaf:name ?name.}UNION{?ser km4c:extendName ?name.}}\n"
              // ---- FINE CAMPI AGGIUNTI ---
              + "}LIMIT 1";
      
      String queryDBpedia = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX cito:<http://purl.org/spar/cito/>\n"
              + "SELECT ?linkDBpedia WHERE{\n"
              + " OPTIONAL {<" + idService + "> cito:cites ?linkDBpedia.}\n"
              + "}";
      
      out.println("{ "
              + "\"type\": \"FeatureCollection\", "
              + "\"features\": [ ");
      
      
      
      TupleQuery tupleQueryService = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStringNoService);
      long start = System.nanoTime();
      TupleQueryResult resultService = tupleQueryService.evaluate();
      logQuery(filterQuery(queryStringNoService), "get-noservice-details", "any", idService, System.nanoTime() - start);
      
      TupleQuery tupleQueryDBpedia = con.prepareTupleQuery(QueryLanguage.SPARQL, queryDBpedia);
      long start2 = System.nanoTime();
      TupleQueryResult resultDBpedia = tupleQueryDBpedia.evaluate();
      logQuery(filterQuery(queryDBpedia), "get-link-DBpedia", "any", idService, System.nanoTime() - start2);
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
        String valueOfType = "";
        if (bindingSetService.getValue("type") != null) {
          valueOfType = bindingSetService.getValue("type").stringValue();
        }

        String valueOfTypeLabel = "";
        if (bindingSetService.getValue("typeLabel") != null) {
          valueOfTypeLabel = bindingSetService.getValue("typeLabel").stringValue();
        }

        String valueOfName = "";
        if (bindingSetService.getValue("name") != null) {
          valueOfName = bindingSetService.getValue("name").stringValue();
        } else {
        }
        
        String valueOfELat = "";
        if (bindingSetService.getValue("elat") != null) {
          valueOfELat = bindingSetService.getValue("elat").stringValue();
        }        
        String valueOfELong = "";
        if (bindingSetService.getValue("elong") != null) {
          valueOfELong = bindingSetService.getValue("elong").stringValue();
        }
        
        float[] avgServiceStars = ServiceMap.getAvgServiceStars(idService);

        out.println("{ "
                + " \"geometry\": {  "
                + "     \"type\": \"Point\""
                + (!"".equals(valueOfELat) && !"".equals(valueOfELong)
                        ? ",\"coordinates\": [" + valueOfELong + "," + valueOfELat + "]"
                        : "")
                + "},"
                + "\"type\": \"Feature\","
                + "\"properties\": {"
                + "    \"popupContent\": \"" + escapeJSON(valueOfName) + " - " + escapeJSON(valueOfTypeLabel) + "\", "
                + "    \"name\": \"" + escapeJSON(valueOfName) + "\", "
                + "    \"serviceType\": \"" + escapeJSON(valueOfTypeLabel) + "\", "
                + "    \"tipo\": \"" + escapeJSON(valueOfTypeLabel) + "\", "
                + "    \"tipologia\": \"" + escapeJSON(valueOfTypeLabel) + "\", "
                + "    \"linkDBpedia\": " + valueOfDBpedia + ", "
                //+ "    \"description\": \"" + escapeJSON(valueOfDescription) + "\", "
                + "    \"serviceUri\": \"" + idService + "\",\n"
                + "    \"photos\": "+ServiceMap.getServicePhotos(idService) + ",\n"
                + "    \"avgStars\": "+avgServiceStars[0] + ",\n"
                + "    \"starsCount\": "+(int)avgServiceStars[1] + ",\n"
                + "    \"comments\": "+ServiceMap.getServiceComments(idService)
                + "}, "
                + "\"id\": " + Integer.toString(i + 1) + "  "
                // + "\"query\": \"" + queryString + "\" "
                + "}");
        i++;
      }
      out.println("] "
              + "}");
    }
  } catch (Exception e) {
    ServiceMap.notifyException(e);
  } finally {
    con.close();
  }
  logQuery("", "service.jsp", "any", idService, System.nanoTime() - s);
%>
