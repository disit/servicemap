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
    
    String cat_servizi = request.getParameter("cat_servizi");
    if(cat_servizi == null)
      cat_servizi = "categorie";
    String centroRicerca = request.getParameter("centroRicerca");
    String[] coord = centroRicerca.split(";");
    String latitudine = coord[0];
    String longitudine = coord[1];
    String latitudine2 = "";
    String longitudine2 = "";
    if(coord.length==4){
      latitudine2 = coord[2];
      longitudine2 = coord[3];
    }
    String raggioServizi = request.getParameter("raggioServizi");
    //String raggioSensori = request.getParameter("raggioSensori");
    //String raggioBus = request.getParameter("raggioBus");
    String categorie = request.getParameter("categorie");
    String textFilter = request.getParameter("textFilter");
    String numeroRisultatiServizi = request.getParameter("numeroRisultatiServizi");
    //String numeroRisultatiSensori = request.getParameter("numeroRisultatiSensori");
    //String numeroRisultatiBus = request.getParameter("numeroRisultatiBus");
    String ip = request.getRemoteAddr();
    String ua = request.getHeader("User-Agent");

    logAccess(ip, null, ua, centroRicerca, categorie, null, "ui-services-by-gps", numeroRisultatiServizi, raggioServizi, null, textFilter, null, null);
    
    if(textFilter==null)
      textFilter="";
    if (!"".equals(categorie)) {
        List<String> listaCategorie = new ArrayList<String>();
        if (categorie != null) {
            String[] arrayCategorie = categorie.split(";");
            // GESTIONE CATEGORIE
            listaCategorie = Arrays.asList(arrayCategorie);
        }
        RepositoryConnection con = repo.getConnection();

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
            String queryString = 
                    //(sparqlType.equals("virtuoso") ? "define sql:select-option \"order\"\n" : "") +
                      "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                    + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
                    + "PREFIX schema:<http://schema.org/#>\n"
                    + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                    + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
                    + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
                    + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
                    + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
                    + "SELECT DISTINCT ?bs (STR(?nome) AS ?nomeFermata)"+ServiceMap.toFloat("?bslt","?bslat")+ServiceMap.toFloat("?bslg","?bslong")+" ?x WHERE {\n"
                    + " ?bs rdf:type km4c:BusStop.\n"
                    + " ?bs geo:lat ?bslt.\n"
                    + " ?bs geo:long ?bslg.\n"
                    + ServiceMap.geoSearchQueryFragment("?bs", coord, raggioServizi)
                    + " ?bs foaf:name ?nome.\n"
                    + ServiceMap.textSearchQueryFragment("?bs", "foaf:name", textFilter)
                    + (km4cVersion.equals("old")? " FILTER ( datatype(?bslat ) = xsd:float )\n"
                    + " FILTER ( datatype(?bslong ) = xsd:float )\n" : "")
                    + "} ORDER BY ?dist";
            if (!numeroRisultatiServizi.equals("0")) {
                if(cat_servizi.equals("categorie")){
                    limitBusStop = ((Integer.parseInt(numeroRisultatiServizi))/10*3);
                    queryString += " LIMIT " + limitBusStop;
                }else{
                    queryString += " LIMIT " + numeroRisultatiServizi;
                }
            }
            //out.println(queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
            if(sparqlType.equals("owlim"))
              tupleQuery.setMaxQueryTime(maxTime);
            TupleQueryResult result = tupleQuery.evaluate();
            logQuery(filterQuery(queryString),"get-services-bus",sparqlType,latitudine+";"+longitudine+";"+raggioServizi+";"+numeroRisultatiServizi);

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String valueOfBS = bindingSet.getValue("bs").stringValue();
                String valueOfNomeFermata = bindingSet.getValue("nomeFermata").stringValue();
                String valueOfBSLat = bindingSet.getValue("bslat").stringValue();
                String valueOfBSLong = bindingSet.getValue("bslong").stringValue();

                if (i != 0) {
                    out.println(", ");
                }
                out.println("{ "
                        + " \"geometry\": {  "
                        + "     \"type\": \"Point\",  "
                        + "    \"coordinates\": [  "
                        + "       " + valueOfBSLong + ",  "
                        + "       " + valueOfBSLat + "  "
                        + " ]  "
                        + "},  "
                        + "\"type\": \"Feature\",  "
                        + "\"properties\": {  "
                        + "    \"popupContent\": \"" + valueOfNomeFermata + " - Fermata \", "
                        + "    \"nome\": \"" + valueOfNomeFermata + "\", "
                        + "    \"tipo\": \"fermata\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"TransferServiceAndRenting_BusStop\", "
                        // **********************************************
                        + "    \"email\": \"\", "
                        + "    \"note\": \"\", "
                        + "    \"serviceUri\": \"" + valueOfBS + "\", "
                        + "    \"indirizzo\": \"\" "
                        + "}, "
                        + "\"id\": " + Integer.toString(i + 1) + " "
                        + "}");
                i++;
                numeroServizi++;
                numeroBus++;
            }
        }

        int numeroSensori = 0;
        int limitSensori = 0;
        if (listaCategorie.contains("SensorSite")) {
            String queryStringSensori = 
                    //(sparqlType.equals("virtuoso") ? "define sql:select-option \"order\"\n" : "") +
                      "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
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
                    + "SELECT DISTINCT ?sensor ?idSensore"+ServiceMap.toFloat("?lt","?lat")+ServiceMap.toFloat("?lg","?long")+" ?address ?x WHERE {\n"
                    + " ?sensor geo:lat ?lt.\n"
                    //+ " FILTER regex(str(?lat), \"^4\")"
                    + " ?sensor geo:long ?lg.\n"
                    + " ?sensor dcterms:identifier ?idSensore.\n"
                    + ServiceMap.geoSearchQueryFragment("?sensor", coord, raggioServizi)
                    + ServiceMap.textSearchQueryFragment("?sensor", "?p", textFilter)
                    + " ?sensor schema:streetAddress ?address.\n"
                    + " ?sensor rdf:type km4c:SensorSite.\n"
                    + "} ORDER BY ?dist";
            if (!numeroRisultatiServizi.equals("0")) {
                if(cat_servizi.equals("categorie")){
                    limitSensori = ((Integer.parseInt(numeroRisultatiServizi))/10*2);
                    queryStringSensori += " LIMIT " + limitSensori ;
                }else{
                    queryStringSensori += " LIMIT " + numeroRisultatiServizi;
                }
            }

            TupleQuery tupleQuerySensori = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringSensori));
            if(sparqlType.equals("owlim"))
              tupleQuerySensori.setMaxQueryTime(maxTime);
            TupleQueryResult resultSensori = tupleQuerySensori.evaluate();
            logQuery(filterQuery(queryStringSensori),"get-services-sensor",sparqlType,latitudine+";"+longitudine+";"+raggioServizi+";"+numeroRisultatiServizi);
            //queryStringSensori = queryStringSensori.replace("\"", "'");
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
                        + "       " + valueOfLat + "  "
                        + " ]  "
                        + "},  "
                        + "\"type\": \"Feature\",  "
                        + "\"properties\": {  "
                        + "    \"popupContent\": \"" + valueOfId + " - Sensore\", "
                        + "    \"nome\": \"" + valueOfId + "\", "
                        + "    \"tipo\": \"sensore\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"TransferServiceAndRenting_SensorSite\", "
                        // **********************************************
                        + "    \"serviceUri\": \"" + valueOfIdService + "\", "
                        + "    \"indirizzo\": \"" + escapeJSON(valueOfAddress) + "\" "
                        + "},  "
                        + "\"id\": " + Integer.toString(i + 1) + "  "
                        //  + "\"query\": " + queryString + " "
                        + "}");
                i++;
                numeroServizi++;
                numeroSensori++;
            }
        }
        
        int limitServizi = 0;
        try {
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
                  !categorie.equals("Event;PublicTransportLine;SensorSite") &&
                  !categorie.equals("Event;PublicTransportLine;BusStop") &&
                  !categorie.equals("Event;SensorSite;BusStop") &&
                  !categorie.equals("PublicTransportLine;SensorSite;BusStop") &&
                  !categorie.equals("Event;PublicTransportLine;SensorSite;BusStop")) {
            String fc = "";
            try {
                fc = filterServices(listaCategorie);
            } catch (Exception e) {
            }
            String queryString = "";
            if(cat_servizi.equals("categorie")){
            queryString = 
                    //(sparqlType.equals("virtuoso") ? "define sql:select-option \"order\"\n" : "") +
                      "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
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
                    + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
                    + "PREFIX schema:<http://schema.org/>\n"
                    + "SELECT distinct ?ser ?serAddress"+ServiceMap.toFloat("?elt","?elat")+ServiceMap.toFloat("?elg","?elong")+" ?sType ?sTypeIta ?sCategory ?sName ?x WHERE {\n"
                    + " ?ser rdf:type km4c:Service"+(sparqlType.equals("virtuoso")? " OPTION (inference \"urn:ontology\")":"")+".\n"
                    + " OPTIONAL {?ser schema:name ?sName}\n"
                    + " OPTIONAL {?ser schema:streetAddress ?serAddress.}\n"
                    + ServiceMap.textSearchQueryFragment("?ser", "?p", textFilter)
                    + " {\n"
                    + "  ?ser km4c:hasAccess ?entry.\n"
                    + "  ?entry geo:lat ?elt.\n"
                    //+ "  FILTER ( datatype(?elat ) = xsd:float )\n"
                    + "  ?entry geo:long ?elg.\n"
                    //+ "  FILTER ( datatype(?elong ) = xsd:float )\n"
                    + ServiceMap.geoSearchQueryFragment("?entry", coord, raggioServizi)
                    + " } UNION {\n"
                    + "  ?ser geo:lat ?elt.\n"
                    + "  ?ser geo:long ?elg.\n"
                    + ServiceMap.geoSearchQueryFragment("?ser", coord, raggioServizi)
                    + " }\n"
                    + fc
                    + (!km4cVersion.equals("old") ? 
                        " graph ?g {?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:BusStop && ?sType!=km4c:SensorSite)}\n"
                      + " ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory!=<http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>)"
                      + " ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\")\n" : "")
                    + "} ORDER BY ?dist";
            if (!numeroRisultatiServizi.equals("0")) {
                limitServizi = ((Integer.parseInt(numeroRisultatiServizi))-(limitBusStop + limitSensori));
                queryString += " LIMIT " + limitServizi;
            }
            }else{
                // MODIFICA PER LA RICERCA DELLE DL COME SERVIZI TRASVERSALI, VIENE FATTO IL CONTROLLO SULLA CATEGORIA_SERVIZI
                String filtroDL = "";
                if(listaCategorie.contains("Fresh_place")){
                    filtroDL = " OPTIONAL {?ser a ?sTypeDL. FILTER(?sTypeDL=km4c:DigitalLocation)}";  
                }else{
                    filtroDL = " ?ser a ?sTypeDL. FILTER(?sTypeDL=km4c:DigitalLocation)"; 
                }
                queryString = 
                    //(sparqlType.equals("virtuoso") ? "define sql:select-option \"order\"\n" : "") +
                      "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
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
                    + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"    
                    + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
                    + "PREFIX schema:<http://schema.org/>\n"
                    + "SELECT distinct ?ser ?serAddress ?elat ?elong ?sType ?sTypeDL ?sTypeIta ?sCategory ?sName ?x WHERE {\n"
                    + " ?ser rdf:type km4c:Service"+(sparqlType.equals("virtuoso")? " OPTION (inference \"urn:ontology\")":"")+".\n"
                    + " OPTIONAL {?ser schema:name ?sName}\n"
                    + " OPTIONAL {?ser schema:streetAddress ?serAddress.}\n"
                    //+ " OPTIONAL {?ser opengis:hasGeometry ?geometry .\n"
                    //+ " ?geometry opengis:asWKT ?cordList .}\n"
                    + ServiceMap.textSearchQueryFragment("?ser", "?p", textFilter)
                    + " {\n"
                    + "  ?ser km4c:hasAccess ?entry.\n"
                    + "  ?entry geo:lat ?elat.\n"
                    //+ "  FILTER ( datatype(?elat ) = xsd:float )\n"
                    + "  ?entry geo:long ?elong.\n"
                    //+ "  FILTER ( datatype(?elong ) = xsd:float )\n"
                    + ServiceMap.geoSearchQueryFragment("?entry", coord, raggioServizi)
                    + " } UNION {\n"
                    + "  ?ser geo:lat ?elat.\n"
                    + "  ?ser geo:long ?elong.\n"
                    + ServiceMap.geoSearchQueryFragment("?ser", coord, raggioServizi)
                    + " }\n"
                    + fc
                    + (!km4cVersion.equals("old") ? 
                        " graph ?g {?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService)}\n"
                      //+ " ?ser a ?sTypeDL. FILTER(?sTypeDL=km4c:DigitalLocation)"  
                      + filtroDL
                      + " ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory!=<http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>)"
                      + " ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\")\n" : "")
                    + "} ORDER BY ?dist";
            if (!numeroRisultatiServizi.equals("0")) {
                queryString += " LIMIT " + numeroRisultatiServizi;
            }
            }
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
            if(sparqlType.equals("owlim"))
              tupleQuery.setMaxQueryTime(maxTime);
            TupleQueryResult result = tupleQuery.evaluate();
            logQuery(filterQuery(queryString),"get-services-services",sparqlType,latitudine+";"+longitudine+";"+raggioServizi+";"+numeroRisultatiServizi+";"+categorie);
            System.out.println(queryString);
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String valueOfSer = bindingSet.getValue("ser").toString();
                String valueOfSType = "";
                if (bindingSet.getValue("sType") != null) {
                  valueOfSType = bindingSet.getValue("sType").stringValue();
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
                if (bindingSet.getValue("sName") != null){
                    valueOfSName = bindingSet.getValue("sName").stringValue();
                }else{
                    valueOfSName = subCategory.replace("_", " ").toUpperCase();
                }
                
                String valueOfSTypeIta = "";
                if(bindingSet.getValue("sTypeIta") != null) {
                  valueOfSTypeIta = bindingSet.getValue("sTypeIta").stringValue();
                  //valueOfSTypeIta = Character.toLowerCase(valueOfSTypeIta.charAt(0)) + valueOfSTypeIta.substring(1);
                  valueOfSTypeIta = valueOfSTypeIta.replace(" ", "_");
                  valueOfSTypeIta = valueOfSTypeIta.replaceAll("[^\\P{Punct}_]+", "");
                }
                /*String valueOfCordList = "";
                if ((bindingSet.getValue("cordList") != null) && (!bindingSet.getValue("cordList").stringValue().contains("POINT"))) {
                    valueOfCordList = bindingSet.getValue("cordList").stringValue();
                    valueOfCordList = valueOfCordList.replace("^^<gis:wktLiteral>", "");
                }*/
                String valueOfELat = bindingSet.getValue("elat").stringValue();
                String valueOfELong = bindingSet.getValue("elong").stringValue();
                //String valueOfNote = "";
                //if (bindingSet.getValue("note") != null) {
                  //valueOfNote = bindingSet.getValue("note").stringValue();
                  //Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                  //valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 ]+", "");
                //}

                if (i != 0) {
                    out.println(", ");
                }

                out.println("{ "
                        + " \"geometry\": {  "
                        + "     \"type\": \"Point\",  "
                        + "    \"coordinates\": [  "
                        + "      " + valueOfELong + ",  "
                        + "      " + valueOfELat + "  "
                        + " ]  "
                        + "},  "
                        + "\"type\": \"Feature\",  "
                        + "\"properties\": {  "
                        + "    \"popupContent\": \"" + escapeJSON(valueOfSName) + " - " + escapeJSON(valueOfSType) + "\", "
                        + "    \"nome\": \"" + escapeJSON(valueOfSName) + "\", "
                        + "    \"tipo\": \"" + escapeJSON(valueOfSTypeIta) + "\", "
                        //+ "    \"cordList\": \"" + escapeJSON(valueOfCordList) + "\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\", "
                        + "    \"category\": \"" + escapeJSON(category) + "\", "
                        + "    \"subCategory\": \"" + escapeJSON(subCategory) + "\", "
                        + "    \"serviceUri\": \"" + valueOfSer + "\" "
                        + "}, "
                        + "\"id\": " + Integer.toString(i + 1) + " "
                        + "}");
                i++;
                numeroServizi++;
            }
             
        }// CODICE DA RICONTROLLARE
          /*else{
              String fc = "";
            try {
                fc = filterServices(listaCategorie);
            } catch (Exception e) {
            }
              String queryString = 
                    //(sparqlType.equals("virtuoso") ? "define sql:select-option \"order\"\n" : "") +
                      "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
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
                    + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
                    + "SELECT distinct ?ser ?serAddress ?elat ?elong ?sType ?sTypeIta ?sCategory ?sName ?x WHERE {\n"
                    + " ?ser rdf:type km4c:Service"+(sparqlType.equals("virtuoso")? " OPTION (inference \"urn:ontology\")":"")+".\n"
                    + " OPTIONAL {?ser <http://schema.org/name> ?sName}\n"
                    + " ?ser <http://schema.org/streetAddress> ?serAddress.\n"
                    + ServiceMap.textSearchQueryFragment("?ser", "?p", textFilter)
                    + " {\n"
                    + "  ?ser km4c:hasAccess ?entry.\n"
                    + "  ?entry geo:lat ?elat.\n"
                    //+ "  FILTER ( datatype(?elat ) = xsd:float )\n"
                    + "  ?entry geo:long ?elong.\n"
                    //+ "  FILTER ( datatype(?elong ) = xsd:float )\n"
                    + ServiceMap.geoSearchQueryFragment("?entry", latitudine, longitudine, raggioServizi)
                    + " } UNION {\n"
                    + "  ?ser geo:lat ?elat.\n"
                    + "  ?ser geo:long ?elong.\n"
                    + ServiceMap.geoSearchQueryFragment("?ser", latitudine, longitudine, raggioServizi)
                    + " }\n"
                    + fc
                    + (!km4cVersion.equals("old") ? 
                        " graph ?g {?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:TransverseService)}\n"
                      + " ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory!=<http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>)"
                      + " ?sType rdfs:label ?sTypeIta. FILTER(LANG(?sTypeIta)=\"it\")\n" : "")
                    + "} ORDER BY ?dist";
            if (!numeroRisultatiServizi.equals("0")) {
                queryString += " LIMIT " + numeroRisultatiServizi;
            }
            //out.println(queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
            if(sparqlType.equals("owlim"))
              tupleQuery.setMaxQueryTime(maxTime);
            TupleQueryResult result = tupleQuery.evaluate();
            logQuery(filterQuery(queryString),"get-services-services",sparqlType,latitudine+";"+longitudine+";"+raggioServizi+";"+numeroRisultatiServizi+";"+categorie);

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String valueOfSer = bindingSet.getValue("ser").toString();
                String valueOfSType = "";
                if (bindingSet.getValue("sType") != null) {
                  valueOfSType = bindingSet.getValue("sType").stringValue();
                }
                // DICHIARAZIONE VARIABILI serviceType e serviceCategory per ICONA
                String subCategory = "";
                if(bindingSet.getValue("sType") != null) {
                  subCategory = bindingSet.getValue("sType").stringValue();
                  subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
                  subCategory = Character.toLowerCase(subCategory.charAt(0)) + subCategory.substring(1);
                  subCategory = subCategory.replace(" ", "_");
                }
                
                String category = "";
                if(bindingSet.getValue("sCategory") != null) {
                  category = bindingSet.getValue("sCategory").stringValue();
                  category = category.replace("http://www.disit.org/km4city/schema#", "");
                  category = Character.toLowerCase(category.charAt(0)) + category.substring(1);
                  category = category.replace(" ", "_");
                }
                
                String serviceType = category+"_"+subCategory;
                
                // Se la risorsa non ha nessun NOME, questo viene settato impostando la subcategory.
                String valueOfSName = "";
                if (bindingSet.getValue("sName") != null){
                    valueOfSName = bindingSet.getValue("sName").stringValue();
                }else{
                    valueOfSName = subCategory.replace("_", " ").toUpperCase();
                }
                
                String valueOfSTypeIta = "";
                if(bindingSet.getValue("sTypeIta") != null) {
                  valueOfSTypeIta = bindingSet.getValue("sTypeIta").stringValue();
                  valueOfSTypeIta = Character.toLowerCase(valueOfSTypeIta.charAt(0)) + valueOfSTypeIta.substring(1);
                  valueOfSTypeIta = valueOfSTypeIta.replace(" ", "_");
                }
                String valueOfELat = bindingSet.getValue("elat").stringValue();
                String valueOfELong = bindingSet.getValue("elong").stringValue();
                String valueOfNote = "";
                //if (bindingSet.getValue("note") != null) {
                  //valueOfNote = bindingSet.getValue("note").stringValue();
                  //Normalizer.normalize(valueOfNote, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                  //valueOfNote = valueOfNote.replaceAll("[^A-Za-z0-9 ]+", "");
                //}

                if (i != 0) {
                    out.println(", ");
                }

                out.println("{ "
                        + " \"geometry\": {  "
                        + "     \"type\": \"Point\",  "
                        + "    \"coordinates\": [  "
                        + "      " + valueOfELong + ",  "
                        + "      " + valueOfELat + "  "
                        + " ]  "
                        + "},  "
                        + "\"type\": \"Feature\",  "
                        + "\"properties\": {  "
                        + "    \"popupContent\": \"" + escapeJSON(valueOfSName) + " - " + escapeJSON(valueOfSType) + "\", "
                        + "    \"nome\": \"" + escapeJSON(valueOfSName) + "\", "
                        + "    \"tipo\": \"" + escapeJSON(valueOfSTypeIta) + "\", "
                        // *** INSERIMENTO serviceType
                        + "    \"serviceType\": \"" + escapeJSON(serviceType) + "\", "
                        + "    \"category\": \"" + escapeJSON(category) + "\", "
                        + "    \"subCategory\": \"" + escapeJSON(subCategory) + "\", "
                        + "    \"serviceUri\": \"" + valueOfSer + "\" "
                        + "}, "
                        + "\"id\": " + Integer.toString(i + 1) + " "
                        + "}");
                i++;
                numeroServizi++;
            }
          
          }*/
        } catch(Exception e) {
          e.printStackTrace();
        }
        out.println("] }");
        
        con.close() ;
    }
%>