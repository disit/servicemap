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
    
    String nomeProvincia = request.getParameter("nomeProvincia");
    String nomeComune = request.getParameter("nomeComune");
    String raggio = request.getParameter("raggio");
    String numeroRisultatiServizi = request.getParameter("numeroRisultatiServizi");
    String categorie = request.getParameter("categorie");
    String textFilter = request.getParameter("textFilter");
    
    //String ip = ServiceMap.getClientIpAddress(request);
    //String ua = request.getHeader("User-Agent");
    //logAccess(ip, null, ua, nomeComune, categorie, null, "ui-services-by-municipality", numeroRisultatiServizi, null, null, textFilter, null, null);
    
    
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
            filtroLocalita += "?prov foaf:name \"" + nomeProvincia + "\"^^xsd:string.\n";
            filtroLocalita += "?mun km4c:isPartOfProvince ?prov.\n";
        } else {
            filtroLocalita += " { ?ser km4c:hasAccess ?entry.\n";
            filtroLocalita += "  ?entry geo:lat ?elat.\n";
            filtroLocalita += "  ?entry geo:long ?elong.\n";
            filtroLocalita += "  ?nc km4c:hasExternalAccess ?entry.\n";
            filtroLocalita += "  ?nc km4c:belongToRoad ?road.\n";
            filtroLocalita += "  ?road km4c:inMunicipalityOf ?mun.\n";
            filtroLocalita += "  ?mun foaf:name \"" + nomeComune + "\"^^xsd:string.\n";
            filtroLocalita += " } UNION {\n";
            filtroLocalita += "  ?ser km4c:isInRoad ?road .\n";
            filtroLocalita += "  ?ser geo:lat ?elat.\n";
            filtroLocalita += "  ?ser geo:long ?elong.\n";
            filtroLocalita += "  ?road km4c:inMunicipalityOf ?mun.\n";
            filtroLocalita += "  ?mun foaf:name \"" + nomeComune + "\"^^xsd:string.\n";
            filtroLocalita += " }\n";
        }

        out.println("{ "
                + "\"type\": \"CategoryCollection\","
                + "\"categories\": [");

        int i = 0;
        int total = 0;
        int numeroServizi = 0;
        int numeroBus = 0;
        int limitBusStop = 0;
        if (listaCategorie.contains("BusStop") || listaCategorie.contains("NearBusStops")) {
            // INSERISCI ANCHE BUS STOP
            String queryString = 
                      "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
                    + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"
                    + "PREFIX schema:<http://schema.org/#>\n"
                    + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
                    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                    + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
                    + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
                    + "Select (count(*) as ?count_bus) where{\n"
                    + "SELECT DISTINCT ?bs (STR(?nome) AS ?nomeFermata)"+ServiceMap.toFloat("?bslt","?bslat")+ServiceMap.toFloat("?bslg","?bslong")+" ?x WHERE {\n"
                    + " ?bs rdf:type km4c:BusStop.\n"
                    + " ?bs foaf:name ?nome.\n"
                    + ServiceMap.textSearchQueryFragment("?bs", "foaf:name", textFilter)
                    + " ?bs geo:lat ?bslt.\n"
                    + " ?bs geo:long ?bslg.\n"
                    + " ?bs km4c:isInMunicipality ?com.\n"
                    + " ?com foaf:name \"" + nomeComune + "\"^^xsd:string.\n"
                    + "}";
            if (!numeroRisultatiServizi.equals("0")) {
                    limitBusStop = ((Integer.parseInt(numeroRisultatiServizi))/10*3);
                    queryString += " LIMIT " + limitBusStop;
            }
            queryString += "}order by ASC(?count_bus)";
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
            if(sparqlType.equals("owlim"))
            tupleQuery.setMaxQueryTime(maxTime);
            TupleQueryResult result = tupleQuery.evaluate();
            //logQuery(filterQuery(queryString),"get-services-in-municipality-bus","any",nomeComune+";"+nomeProvincia+";"+numeroRisultatiServizi, System.nanoTime()-start);

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                String numeroBusValue = (bindingSet.getValue("count_bus")).stringValue();
                numeroBus = Integer.parseInt(numeroBusValue);
                
                
                /*out.println("{ "
                        + "\"type\": \"Feature\",  "
                        + "\"properties\": {  "
                        + "    \"popupContent\": \"" + numeroBus + " - Bus Trovati \", "
                        // *** INSERIMENTO serviceType
                        + "    \"category\": \"TransferServiceAndRenting\" "
                        // **********************************************
                        + "}, "
                        + "}");*/

            }
        }
        int numeroSensori = 0;
        int limitSensori = 0;
        if (listaCategorie.contains("SensorSite")) {
            String queryStringSensori = 
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
                    + "Select (count(*) as ?count_sensor) where{\n"
                    + "SELECT DISTINCT ?sensor ?idSensore"+ServiceMap.toFloat("?lt","?lat")+ServiceMap.toFloat("?lg","?long")+" ?address ?x WHERE {\n"
                    + " ?sensor rdf:type km4c:SensorSite.\n"
                    + " ?sensor geo:lat ?lt.\n"
                    + " FILTER regex(str(?lt), \"^4\")\n"
                    + " ?sensor geo:long ?lg.\n"
                    + " ?sensor dcterms:identifier ?idSensore.\n"
                    + " ?sensor km4c:placedOnRoad ?road.\n"
                    + " ?road km4c:inMunicipalityOf ?mun.\n"
                    + " ?mun foaf:name \"" + nomeComune + "\"^^xsd:string.\n"
                    + " ?sensor schema:streetAddress ?address.\n"
                    + ServiceMap.textSearchQueryFragment("?sensor", "?p", textFilter)
                    + "}";
                    
            if (!numeroRisultatiServizi.equals("0")) {
                    limitSensori = ((Integer.parseInt(numeroRisultatiServizi))/10*2);
                    queryStringSensori += " LIMIT " + limitSensori ;
            }
            queryStringSensori += "}order by ASC(?count_sensor)";
            TupleQuery tupleQuerySensori = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringSensori));
            if(sparqlType.equals("owlim"))
              tupleQuerySensori.setMaxQueryTime(maxTime);
            TupleQueryResult resultSensori = tupleQuerySensori.evaluate();
            //logQuery(filterQuery(queryStringSensori),"get-services-in-municipality-sensor","any",nomeComune+";"+nomeProvincia+";"+numeroRisultatiServizi, System.nanoTime()-start);
            queryStringSensori = queryStringSensori.replace("\"", "'");
            
            while (resultSensori.hasNext()) {
                BindingSet bindingSetSensori = resultSensori.next();
                String numeroSensorValue = (bindingSetSensori.getValue("count_sensor")).stringValue();
                numeroSensori = Integer.parseInt(numeroSensorValue);
                
                
                 /*out.println("{ "
                        + "\"type\": \"Feature\",  "
                        + "\"properties\": {  "
                        + "    \"popupContent\": \"" + numeroSensori + " - Sensori Trovati \", "
                        // *** INSERIMENTO serviceType
                        + "    \"category\": \"TransferServiceAndRenting\" "
                        // **********************************************
                        + "}, "
                        + "}");*/

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
            }
            String queryString = "";
            queryString = 
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
                    + "Select (count(*) as ?c) ?sCategory ?sType where{\n"
                    //+ "SELECT distinct ?ser ?serAddress"+ServiceMap.toFloat("?elt","?elat")+ServiceMap.toFloat("?elg","?elong")+" ?sType ?sCategory ?sName ?email ?note ?labelIta ?identifier ?x WHERE {\n"
                    + "SELECT distinct ?ser ?serAddress ?elat ?elong ?sType ?sCategory ?sName ?email ?note ?labelIta ?identifier ?x WHERE {\n"
                    + " ?ser rdf:type km4c:Service"+(sparqlType.equals("virtuoso")? " OPTION (inference \"urn:ontology\")":"")+".\n"
                    + ServiceMap.textSearchQueryFragment("?ser", "?p", textFilter)
                    + " OPTIONAL {?ser schema:name ?sName}\n"
                    + " ?ser schema:streetAddress ?serAddress.\n"
                    + " OPTIONAL { ?ser dcterms:identifier ?identifier }\n"
                    + filtroLocalita
                    + fc
                    + " ?ser a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service&& ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:BusStop && ?sType!=km4c:SensorSite)\n"
                    + " OPTIONAL { ?sType rdfs:subClassOf ?sCategory. FILTER(?sCategory!=<http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> && ?sCategory!=<http://www.pms.ifi.uni-muenchen.de/OTN#Line>)}\n"
                    + " OPTIONAL { ?sType rdfs:label ?labelIta. FILTER(LANG(?labelIta)=\"it\")}\n"
                    + "}";
            if (!numeroRisultatiServizi.equals("0")) {
                limitServizi = ((Integer.parseInt(numeroRisultatiServizi))-(numeroBus + numeroSensori));
                queryString += " LIMIT " + limitServizi;
            }
            queryString += "}order by ASC(?sCategory)";
            
            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
            if(sparqlType.equals("owlim"))
              tupleQuery.setMaxQueryTime(maxTime);
            TupleQueryResult result = tupleQuery.evaluate();
            //logQuery(filterQuery(queryString),"get-services-in-municipality-services",sparqlType,nomeComune+";"+nomeProvincia+";"+numeroRisultatiServizi+";"+categorie, System.nanoTime()-start);
            //ServiceMap.println(queryString);
            String cat = "";
            String subCat = "";
            int num = 0;
            int flag = 0;
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();

                String category = "";
                if(bindingSet.getValue("sCategory") != null) {
                  category = bindingSet.getValue("sCategory").stringValue();
                  category = category.replace("http://www.disit.org/km4city/schema#", "");
                }
                String numeroServiceValue = (bindingSet.getValue("c")).stringValue();
                numeroServizi= Integer.parseInt(numeroServiceValue);
                String subCategory = "";
                if(bindingSet.getValue("sType") != null) {
                  subCategory = bindingSet.getValue("sType").stringValue();
                  subCategory = subCategory.replace("http://www.disit.org/km4city/schema#", "");
                }
                if(!cat.equals(category) && !cat.equals("")){
                    if (i != 0) {
                        out.println(",");
                    }
                    if (cat.equals("TransferServiceAndRenting") && flag == 0){
                        if(numeroBus > 0){
                            num += numeroBus;
                            subCat = subCat+";BusStop";
                        }
                        if(numeroSensori > 0){
                            num += numeroSensori;
                            subCat = subCat+";SensorSite";
                        }
                        flag = 1;
                    }
                    total = (total + num);
                    out.println("{ "
                        + "    \"category\": \"" + cat+ "\", "
                        + "    \"numItem\": " + num + ", "
                        + "    \"subCategories\": \"" + subCat+ "\" "   
                        + "}");
                    i++;
                    
                    cat = category;
                    num = numeroServizi;
                    subCat = subCategory;
 
                }else{
                    cat = category;
                    num = num + numeroServizi;
                    if(subCat.equals("")){
                        subCat = subCategory;
                    }else{
                        subCat = subCat+";"+subCategory;
                    }
                    
                    if (cat.equals("TransferServiceAndRenting") && flag == 0){
                        if(numeroBus > 0){
                            num += numeroBus;
                            subCat = subCat+";BusStop";
                        }
                        if(numeroSensori > 0){
                            num += numeroSensori;
                            subCat = subCat+";SensorSite";
                        }
                        flag = 1;
                    }
                }
                
                
                /*if (category.equals("TransferServiceAndRenting")){
                    numeroServizi += (numeroBus + numeroSensori);
                }
                total = (total + numeroServizi);
                if (i != 0) {
                    out.println(",");
                }
                out.println("{ "
                        + "    \"category\": \"" + category + "\", "
                        + "    \"numItem\": " + numeroServizi + " "
                        + "}");
                i++;
                numeroServizi++;*/
            }
            
            
            if (cat.equals("TransferServiceAndRenting") && flag == 0){
                        if(numeroBus > 0){
                            num += numeroBus;
                            subCat = subCat+";BusStop";
                        }
                        if(numeroSensori > 0){
                            num += numeroSensori;
                            subCat = subCat+";SensorSite";
                        }
                        flag = 1;
                        //total = (total + num);
                    }
            total = (total + num);
            if(i != 0){
            out.println(", ");
            }
            out.println("{ \"category\": \"" + cat+ "\", "
                        + "    \"numItem\": " + num + ", "
                        + "    \"subCategories\": \"" + subCat+ "\" "   
                        + "}");
            
            if(flag == 0){
                if(numeroBus > 0){
                    num = numeroBus;
                    subCat = "BusStop";
                }
                if(numeroSensori > 0){
                    if (numeroBus > 0){
                        num += numeroSensori;
                        subCat = subCat+";SensorSite";
                    }
                    else{
                        num = numeroSensori;
                        subCat = "SensorSite";
                    }
                }
                if(numeroBus > 0 || numeroSensori > 0){
                    flag = 1;
                    out.println(",{ \"category\": \"TransferServiceAndRenting\", "
                        + "    \"numItem\": " + num + ", "
                        + "    \"subCategories\": \"" + subCat+ "\" "   
                        + "}");
                    total = (total + num);
                }
            }
             
        }else{
              total = numeroSensori + numeroBus;
              String subCategory = "";
              if(numeroSensori > 0){
                  subCategory = "SensorSite";
              }
              if(numeroBus > 0){
                  if(subCategory.equals("")){
                      subCategory = "BusStop";
                  }else{
                      subCategory = subCategory+";BusStop";
                  }
              }
              out.println("{ \"category\": \"TransferServiceAndRenting\", "
                        + "    \"numItem\": " + total + ", "
                        + "    \"subCategories\": \"" + subCategory+ "\" "   
                        + "}");
          }
        } catch(Exception e) {
          ServiceMap.notifyException(e);
        }
        out.println("],"
        + " \"total\": \"" + total + "\" }");
     
        con.close() ;
    }
%>