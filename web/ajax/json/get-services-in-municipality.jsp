<%@page import="org.disit.servicemap.api.IoTChecker"%>
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
<%@page import="java.text.Normalizer"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
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
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();
    response.setContentType("application/json; charset=UTF-8");
    
    String cat_servizi = request.getParameter("cat_servizi");
    if(cat_servizi==null)
      cat_servizi="categorie";
    String nomeProvincia = request.getParameter("nomeProvincia");
    String nomeComune = request.getParameter("nomeComune");
    String raggio = request.getParameter("raggio");
    String numeroRisultatiServizi = request.getParameter("numeroRisultatiServizi");
    //String numeroRisultatiSensori = request.getParameter("numeroRisultatiSensori");
    //String numeroRisultatiBus = request.getParameter("numeroRisultatiBus");  
    String categorie = request.getParameter("categorie");
    String textFilter = request.getParameter("textFilter");
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");

    /*try {
      serviceMapApi.queryMunicipalityServices(out, con, nomeComune, categorie, textFilter, numeroRisultatiServizi, numeroRisultatiServizi, numeroRisultatiServizi);
    } catch (Exception e) {
        ServiceMap.notifyException(e);
    }
    finally{
      con.close() ;
    }*/   
    
    ServiceMap.logAccess(request, null, nomeComune, categorie, null, "ui-services-by-municipality", numeroRisultatiServizi, null, null, textFilter, null, null, null);

    if(textFilter==null)
      textFilter="";
    if (!"".equals(categorie)) {
        List<String> listaCategorie = new ArrayList<String>();
        if (categorie != null) {
            String[] arrayCategorie = categorie.split(";");
            // GESTIONE CATEGORIE
            listaCategorie = Arrays.asList(arrayCategorie);
        }

        String filtroLocalita = "";

        if (nomeComune==null || nomeComune.equals("")) {
          //nessun filtro per localita'
            filtroLocalita += " { ?ser km4c:hasAccess ?entry.\n";
            filtroLocalita += "  ?entry geo:lat ?elat.\n";
            filtroLocalita += "  ?entry geo:long ?elong.\n";
            filtroLocalita += " } UNION {\n";
            filtroLocalita += "  ?ser geo:lat ?elat.\n";
            filtroLocalita += "  ?ser geo:long ?elong.\n";
            filtroLocalita += " }\n";
        }
        else if (nomeComune.equals("all")) {
            filtroLocalita += "?prov foaf:name \"" + ServiceMap.stringEncode(nomeProvincia) + "\"^^xsd:string.\n";
            filtroLocalita += "?mun km4c:isPartOfProvince ?prov.\n";
        } else {
            filtroLocalita += " { ?ser km4c:hasAccess ?entry.\n";
            filtroLocalita += "  ?entry geo:lat ?elat.\n";
            filtroLocalita += "  ?entry geo:long ?elong.\n";
            filtroLocalita += "  ?nc km4c:hasExternalAccess ?entry.\n";
            filtroLocalita += "  ?nc km4c:belongToRoad ?road.\n";
            filtroLocalita += "  ?road km4c:inMunicipalityOf ?mun.\n";
            filtroLocalita += "  ?mun foaf:name \"" + ServiceMap.stringEncode(nomeComune) + "\"^^xsd:string.\n";
            filtroLocalita += " } UNION {\n";
            filtroLocalita += "  ?ser km4c:isInRoad ?road .\n";
            filtroLocalita += "  ?ser geo:lat ?elat.\n";
            filtroLocalita += "  ?ser geo:long ?elong.\n";
            filtroLocalita += "  ?road km4c:inMunicipalityOf ?mun.\n";
            filtroLocalita += "  ?mun foaf:name \"" + ServiceMap.stringEncode(nomeComune) + "\"^^xsd:string.\n";
            filtroLocalita += " } UNION {\n";
            filtroLocalita += "  ?ser geo:lat ?elat.\n";
            filtroLocalita += "  ?ser geo:long ?elong.\n";
            filtroLocalita += "  ?ser km4c:inMunicipalityOf ?mun.\n";
            filtroLocalita += "  ?mun foaf:name \"" + ServiceMap.stringEncode(nomeComune) + "\"^^xsd:string.\n";
            filtroLocalita += " }\n";
        }

        out.println("{ "
                + "\"type\": \"FeatureCollection\", "
                + "\"features\": [ ");
        int i = 0;
        int numeroServizi = 0;
        int numeroBus = 0;
        int limitBusStop = 0;
        //if (listaCategorie.contains("NearBusStops")) {
        if (listaCategorie.contains("BusStop") || listaCategorie.contains("NearBusStops")) {
            // INSERISCI ANCHE BUS STOP
            String queryString = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                    + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
                    + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                    + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX schema:<http://schema.org/#>\n"
                    + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
                    + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
                    + "SELECT DISTINCT ?bs ?nomeFermata ?bslat ?bslong ?ag ?agname ?x WHERE {\n"
                    + " ?bs rdf:type km4c:BusStop.\n"
                    + " ?bs foaf:name ?nomeFermata.\n"
                    + ServiceMap.textSearchQueryFragment("?bs", "foaf:name", textFilter)
                    + " ?bs geo:lat ?bslat.\n"
                    + " ?bs geo:long ?bslong.\n"
                    + " ?bs km4c:isInMunicipality ?com.\n"
                    + " ?com foaf:name \"" + nomeComune + "\"^^xsd:string.\n"
                    + " OPTIONAL { ?bs gtfs:agency ?ag. ?ag foaf:name ?agname. }\n"
                    + "}";
            if (!numeroRisultatiServizi.equals("0")) {
                if(cat_servizi.equals("categorie")){
                    limitBusStop = ((Integer.parseInt(numeroRisultatiServizi))/10*3);
                    queryString += " LIMIT " + limitBusStop;
                }else{
                     if(cat_servizi.contains(":")){
                        String parts[] = cat_servizi.split(":");
                        if(!parts[1].equals("0")){
                            limitBusStop = ((Integer.parseInt(parts[1]))/10*3);
                            queryString += " LIMIT " + limitBusStop;
                        }
                    }else{
                    queryString += " LIMIT " + numeroRisultatiServizi;
                    }
                }
            }
            //ServiceMap.println(queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
            if(sparqlType.equals("owlim"))
              tupleQuery.setMaxQueryTime(maxTime);
            long start = System.nanoTime();
            TupleQueryResult result = tupleQuery.evaluate();
            logQuery(filterQuery(queryString),"get-services-in-municipality-bus","any",nomeComune+";"+nomeProvincia+";"+numeroRisultatiServizi, System.nanoTime()-start);
            queryString = queryString.replace("\"", "'");
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String valueOfBS = bindingSet.getValue("bs").stringValue();
                String valueOfNomeFermata = bindingSet.getValue("nomeFermata").stringValue();
                String valueOfBSLat = bindingSet.getValue("bslat").stringValue();
                String valueOfBSLong = bindingSet.getValue("bslong").stringValue();
                String valueOfAgName = null;
                String valueOfAgUri = null;
                if(bindingSet.getValue("agname")!=null)
                  valueOfAgName = bindingSet.getValue("agname").stringValue();
                if(bindingSet.getValue("ag")!=null)
                  valueOfAgUri = bindingSet.getValue("ag").stringValue();
               
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
                        + "    \"popupContent\": \"" + valueOfNomeFermata + " - Fermata\", "
                        + "    \"name\": \"" + valueOfNomeFermata + "\", "
                        + "    \"tipo\": \"fermata\", "
                        + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\", "
                        + "    \"email\": \"\", "
                        + "    \"note\": \"\", "
                        + "    \"serviceUri\": \"" + valueOfBS + "\", "
                        + (valueOfAgName!=null ? ",    \"agency\": \"" + escapeJSON(valueOfAgName) + "\" " : "")
                        + (valueOfAgUri!=null ? ",   \"agencyUri\": \"" + valueOfAgUri + "\", " : "")
                        + "    \"indirizzo\": \"\" "
                        + "}, "
                        + "\"id\": " + Integer.toString(i + 1) + "  "
                        + "}");
                i++;
                numeroServizi++;
                numeroBus++;
            }
        }
        int numeroSensori = 0;
        int limitSensori = 0;
        if (listaCategorie.contains("SensorSite") || listaCategorie.contains("RoadSensor")) {
            String queryStringSensori = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
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
                    + "SELECT DISTINCT ?sensor ?idSensore ?lat ?long ?address ?x WHERE {\n"
                    + " ?sensor rdf:type km4c:SensorSite.\n"
                    + " ?sensor geo:lat ?lat.\n"
                    + " FILTER regex(str(?lat), \"^4\")\n"
                    + " ?sensor geo:long ?long.\n"
                    + " ?sensor dcterms:identifier ?idSensore.\n"
                    + " ?sensor km4c:placedOnRoad ?road.\n"
                    + " ?road km4c:inMunicipalityOf ?mun.\n"
                    + " ?mun foaf:name \"" + ServiceMap.stringEncode(nomeComune) + "\"^^xsd:string.\n"
                    + " ?sensor schema:streetAddress ?address.\n"
                    + ServiceMap.textSearchQueryFragment("?sensor", "?p", textFilter)
                    + "}";
            if (!numeroRisultatiServizi.equals("0")) {
                if(cat_servizi.equals("categorie")){
                    limitSensori = ((Integer.parseInt(numeroRisultatiServizi))/10*2);
                    queryStringSensori += " LIMIT " + limitSensori ;
                }else{
                    if(cat_servizi.contains(":")){
                        String parts[] = cat_servizi.split(":");
                        if(!parts[1].equals("0")){
                            limitSensori = ((Integer.parseInt(parts[1]))/10*2);
                            queryStringSensori += " LIMIT " + limitSensori;
                        }
                    }else{
                    queryStringSensori += " LIMIT " + numeroRisultatiServizi;
                    }
                }
            }
            //ServiceMap.println(queryStringSensori);
            TupleQuery tupleQuerySensori = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringSensori));
            if(sparqlType.equals("owlim"))
              tupleQuerySensori.setMaxQueryTime(maxTime);
            long start = System.nanoTime();
            TupleQueryResult resultSensori = tupleQuerySensori.evaluate();
            logQuery(filterQuery(queryStringSensori),"get-services-in-municipality-sensor","any",nomeComune+";"+nomeProvincia+";"+numeroRisultatiServizi, System.nanoTime()-start);
            queryStringSensori = queryStringSensori.replace("\"", "'");
           
            while (resultSensori.hasNext()) {
                BindingSet bindingSetSensori = resultSensori.next();
                String valueOfId = bindingSetSensori.getValue("idSensore").stringValue();
                String valueOfIdService = bindingSetSensori.getValue("sensor").stringValue();
                String valueOfLat = bindingSetSensori.getValue("lat").stringValue();
                String valueOfLong = bindingSetSensori.getValue("long").stringValue();
                String valueOfAddress = bindingSetSensori.getValue("address").stringValue();

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
                        + "    \"popupContent\": \"" + valueOfId + " - Sensore\", "
                        + "    \"name\": \"" + valueOfId + "\", "
                        + "    \"tipo\": \"sensore\", "
                        + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\", "
                        + "    \"serviceUri\": \"" + valueOfIdService + "\", "
                        + "    \"indirizzo\": \"" + valueOfAddress + "\" "
                        + "},  "
                        + "\"id\": " + Integer.toString(i + 1) + "  "
                        + "}");
                i++;
                numeroServizi++;
                numeroSensori++;
            }
        }
        int limitServizi = 0;
        try{
        if (!categorie.equals("BusStop") &&
                  !categorie.equals("SensorSite") &&
                  !categorie.equals("Event") &&
                  !categorie.equals("PublicTransportLine") &&
                  !categorie.equals("Event;PublicTransportLine") &&
                  !categorie.equals("Event;SensorSite") &&
                  !categorie.equals("Event;BusStop") &&
                  !categorie.equals("PublicTransportLine;SensorSite") &&
                  !categorie.equals("PublicTransportLine;BusStop") &&
                  !categorie.equals("SensorSite;BusStop") &&
                  !categorie.equals("BusStop;SensorSite") &&
                  !categorie.equals("Event;PublicTransportLine;SensorSite") &&
                  !categorie.equals("Event;PublicTransportLine;BusStop") &&
                  !categorie.equals("Event;SensorSite;BusStop") &&
                  !categorie.equals("PublicTransportLine;SensorSite;BusStop") &&
                  !categorie.equals("Event;PublicTransportLine;SensorSite;BusStop")) {
          String fc = "";
          try {
              fc = filterServices(listaCategorie);
          } catch (Exception e) {
            ServiceMap.notifyException(e);
          }
          String queryString = "";
          if(cat_servizi.equals("categorie") || cat_servizi.contains(":")){
            queryString = 
                    "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
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
                    + "SELECT distinct ?ser ?serAddress ?elat ?elong (IF(?sName1,?sName1,?sName2) as ?sName) ?sType ?sCategory ?email ?note ?labelIta ?identifier ?ag ?agname ?x WHERE {\n"
                    + " ?ser rdf:type km4c:Service"+(sparqlType.equals("virtuoso")? " OPTION (inference \"urn:ontology\")":"")+".\n"
                    + ServiceMap.textSearchQueryFragment("?ser", "?p", textFilter)
                    + " OPTIONAL{?ser schema:name ?sName1. }\n"
                    + " OPTIONAL{?ser foaf:name ?sName2. }\n"
                    + " OPTIONAL{?ser schema:streetAddress ?serAddress.}\n"
                    + " OPTIONAL{?ser dcterms:identifier ?identifier }\n"
                    + filtroLocalita
                    + fc
                    + " ?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service&& ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:BusStop && ?sType!=km4c:SensorSite)\n"
                    + " OPTIONAL{?sType rdfs:subClassOf* ?sCategory. ?sCategory rdfs:subClassOf km4c:Service.}\n"
                    + " OPTIONAL{?sType rdfs:label ?labelIta. FILTER(LANG(?labelIta)=\"it\")}\n"
                    + " OPTIONAL{?ser gtfs:agency ?ag. ?ag foaf:name ?agname. }\n"                  
                    + "}";
              if (!numeroRisultatiServizi.equals("0")) {
                limitServizi = ((Integer.parseInt(numeroRisultatiServizi))-(numeroBus + numeroSensori));
                queryString += " LIMIT " + limitServizi;
              }
            }else{
              String filtroDL = "";
              if(listaCategorie.contains("Fresh_place")){
                filtroDL = " OPTIONAL {?ser a ?sTypeDL. FILTER(?sTypeDL=km4c:DigitalLocation)}";  
              }else{
                filtroDL = " ?ser a ?sTypeDL. FILTER(?sTypeDL=km4c:DigitalLocation)"; 
              }
              queryString =
                "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
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
                    + "SELECT distinct ?ser ?serAddress ?elat ?elong (IF(?sName1,?sName1,?sName2) as ?sName) ?sType ?sTypeDL ?sCategory ?email ?note ?labelIta ?identifier ?ag ?agname ?x WHERE {\n"
                    + " ?ser rdf:type km4c:Service"+(sparqlType.equals("virtuoso")? " OPTION (inference \"urn:ontology\")":"")+".\n"
                    + ServiceMap.textSearchQueryFragment("?ser", "?p", textFilter)
                    + " OPTIONAL {?ser schema:name ?sName1. }\n"
                    + " OPTIONAL {?ser foaf:name ?sName2. }\n"
                    + " OPTIONAL {?ser schema:streetAddress ?serAddress.}\n"
                    + " OPTIONAL { ?ser dcterms:identifier ?identifier }\n"
                    + filtroLocalita
                    + fc
                    + " ?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service&& ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService)\n"
                    //+ " ?ser a ?sTypeDL. FILTER(?sTypeDL=km4c:DigitalLocation)"
                    + filtroDL
                    + " OPTIONAL { ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory!=<http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>)}\n"
                    + " OPTIONAL { ?sType rdfs:label ?labelIta. FILTER(LANG(?labelIta)=\"it\")}\n"
                    + " OPTIONAL { ?ser gtfs:agency ?ag. ?ag foaf:name ?agname. }\n"
                    + "}";
              if (!numeroRisultatiServizi.equals("0")) {
                  queryString += " LIMIT " + numeroRisultatiServizi;
              }    
            }
            //ServiceMap.println(queryString);
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
            if(sparqlType.equals("owlim"))
              tupleQuery.setMaxQueryTime(maxTime);
            long start = System.nanoTime();            
            TupleQueryResult result = tupleQuery.evaluate();
            logQuery(filterQuery(queryString),"get-services-in-municipality-services",sparqlType,nomeComune+";"+nomeProvincia+";"+numeroRisultatiServizi+";"+categorie, System.nanoTime()-start);
            //ServiceMap.println(queryString);

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String valueOfSer = bindingSet.getValue("ser").stringValue();
                if(!IoTChecker.checkIoTService(valueOfSer, (String) request.getSession().getAttribute("apikey"))) {
                  continue;
                }
                String valueOfSType = "";
                if(bindingSet.getValue("sType")!=null)
                  valueOfSType = escapeJSON(bindingSet.getValue("sType").stringValue());
                String valueOfSTypeIta = "";
                if (bindingSet.getValue("labelIta") != null) {
                    valueOfSTypeIta = escapeJSON(bindingSet.getValue("labelIta").stringValue());
                    //valueOfSTypeIta = Character.toLowerCase(valueOfSTypeIta.charAt(0)) + valueOfSTypeIta.substring(1);
                    valueOfSTypeIta = valueOfSTypeIta.replace(" ", "_");
                    valueOfSTypeIta = valueOfSTypeIta.replaceAll("[^\\P{Punct}_]+", "");
                    //valueOfSTypeIta = valueOfSTypeIta.replace("@it", "");
                }
                // DICHIARAZIONE VARIABILI serviceType e serviceCategory per ICONA
                String subCategory = "";
                if(bindingSet.getValue("sType") != null) {
                  subCategory = bindingSet.getValue("sType").stringValue();
                  subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
                  //subCategory = Character.toLowerCase(subCategory.charAt(0)) + subCategory.substring(1);
                  //subCategory = subCategory.replace(" ", "_");
                }
                
                String category = "";
                if(bindingSet.getValue("sCategory") != null) {
                  category = bindingSet.getValue("sCategory").stringValue();
                  category = category.replace("http://www.disit.org/km4city/schema#", "");
                  //category = Character.toLowerCase(category.charAt(0)) + category.substring(1);
                  //category = category.replace(" ", "_");
                }
                String serviceType = category+"_"+subCategory;
                // Se la risorsa non ha nessun NOME, questo viene settato impostando la subcategory.
                String valueOfSName = "";
                if (bindingSet.getValue("sName") != null) {
                    valueOfSName = bindingSet.getValue("sName").stringValue();
                }else{
                    valueOfSName = subCategory.replace("_", " ").toUpperCase();
                }
                String identifier="";
                 if (bindingSet.getValue("identifier") != null) {
                    identifier = escapeJSON(bindingSet.getValue("identifier").stringValue());
                }
                String valueOfELat = bindingSet.getValue("elat").stringValue();
                String valueOfELong = bindingSet.getValue("elong").stringValue();
                String valueOfAgName = null;
                String valueOfAgUri = null;
                if(bindingSet.getValue("agname")!=null)
                  valueOfAgName = bindingSet.getValue("agname").stringValue();
                if(bindingSet.getValue("ag")!=null)
                  valueOfAgUri = bindingSet.getValue("ag").stringValue();

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
                        + "\"properties\": {  ");
                        if(valueOfSName!=null && !valueOfSName.equals(""))
                            out.println( "    \"popupContent\": \"" + escapeJSON(valueOfSName) + " - " + escapeJSON(subCategory) + "\", ");
                        if(identifier!=null && !identifier.equals(""))
                            out.println( "    \"identifier\": \"" + escapeJSON(identifier) + " - " + escapeJSON(subCategory) + "\", ");
                        out.println( "    \"name\": \"" + escapeJSON(valueOfSName) + "\", "
                        + "    \"tipo\": \"" + escapeJSON(valueOfSTypeIta) + "\", "
                        + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\", "
                        + "    \"category\": \"" + escapeJSON(category) + "\", "
                        + "    \"subCategory\": \"" + escapeJSON(subCategory) + "\", "
                        + "    \"serviceUri\": \"" + valueOfSer + "\" "
                        + (valueOfAgName!=null ? ",    \"agency\": \"" + escapeJSON(valueOfAgName) + "\" " : "")
                        + (valueOfAgUri!=null ? ",   \"agencyUri\": \"" + valueOfAgUri + "\", " : "")
                        + "}, "
                        + "\"id\": " + Integer.toString(i + 1) + "  "
                        + "}");
                i++;
                numeroServizi++;
            }

        }
        } catch(Exception e) {
          ServiceMap.notifyException(e);
          throw e;
        }

        out.println("] }");
    }
    con.close();
%>