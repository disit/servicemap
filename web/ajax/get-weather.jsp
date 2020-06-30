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

  String nomeComune = request.getParameter("nomeComune");

 
  String wPred ="";
  String queryString1 = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n" + 
  	"PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" + 
		"PREFIX dcterms:<http://purl.org/dc/terms/>\n" + 
		"PREFIX km4c:<http://www.disit.org/km4city/schema#>\n" + 
    "PREFIX km4cr:<http://www.disit.org/km4city/resource#>\n"+
		"PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" + 
		"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
		"PREFIX schema:<http://schema.org/#>\n" + 
		"PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n" + 
		"PREFIX time:<http://www.w3.org/2006/time#>\n" +                    
		"SELECT distinct ?wRep ?instantDateTime WHERE {\n" + 
		" ?comune rdf:type km4c:Municipality.\n" + 
		" ?comune foaf:name \""+ServiceMap.stringEncode(nomeComune)+"\"^^xsd:string.\n" +
		" ?comune km4c:hasWeatherReport ?wRep.\n" + 
		" ?wRep km4c:updateTime ?instant.\n" + 
		" ?instant <http://schema.org/value> ?instantDateTime.\n" + 
		"} " + 
		"ORDER BY DESC (?instantDateTime) " + 
		"LIMIT 1 ";
	TupleQuery tupleQuery1 = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString1));
	TupleQueryResult result1 = tupleQuery1.evaluate();
  logQuery(filterQuery(queryString1),"get-weather-1","any",nomeComune);
  //ServiceMap.println(queryString1);
  
	BindingSet bindingSet1 = (result1.hasNext() ? result1.next() : null);
  if(bindingSet1!=null) {
    String valueOfInstantDateTime = bindingSet1.getValue("instantDateTime").stringValue();
    String valueOfWRep = bindingSet1.getValue("wRep").stringValue();					
    String queryString = "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n" + 
				"PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n" + 
				"PREFIX dcterms:<http://purl.org/dc/terms/>\n" + 
				"PREFIX km4c:<http://www.disit.org/km4city/schema#>\n" + 
        "PREFIX km4cr:<http://www.disit.org/km4city/resource/>\n"+
				"PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" + 
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
				"PREFIX schema:<http://schema.org/>\n" + 
				"PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n" + 
				"PREFIX time:<http://www.w3.org/2006/time#>\n" + 
				"SELECT distinct ?giorno ?descrizione ?minTemp ?maxTemp ?instantDateTime ?wPred WHERE{\n" + 
				" <" + valueOfWRep + "> km4c:hasPrediction ?wPred.\n" + 
				" ?wPred dcterms:description ?descrizione.\n" + 
				" ?wPred km4c:day  ?giorno.\n" + 
				" ?wPred km4c:hour ?g. FILTER(STR(?g)=\"giorno\")\n" + 
				" OPTIONAL { ?wPred km4c:minTemp ?minTemp.}\n" + 
				" OPTIONAL { ?wPred km4c:maxTemp ?maxTemp.}\n" + 
				"}";
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString); //tolto filterQuery() per problema temporaneo, da rimettere
    if(sparqlType.equals("owlim"))
      tupleQuery.setMaxQueryTime(maxTime);
    //ServiceMap.println(queryString);

    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(filterQuery(queryString),"get-weather-2","any",valueOfWRep);

    out.println("<div class=\"meteo\" id=\"" + nomeComune + "\"><div id=\"meteo_title\"><span name=\"lbl\" caption=\"weather_mun\">Weather Forecast for Mucipality of </span>: <b>" + nomeComune+ "</b></div>");
	
    try {
      int i = 0;
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        wPred = bindingSet.getValue("wPred").stringValue(); 
        String valueOfGiorno = bindingSet.getValue("giorno").stringValue();
        String valueOfDescrizione = bindingSet.getValue("descrizione").stringValue();
        
        if (valueOfGiorno.equals("Lunedi")){
          valueOfGiorno = "Monday";
        }
        if (valueOfGiorno.equals("Martedi")){
          valueOfGiorno = "Tuesday";
        }
        if (valueOfGiorno.equals("Mercoledi")){
          valueOfGiorno = "Wednesday";
        }
        if (valueOfGiorno.equals("Giovedi")){
          valueOfGiorno = "Thursday";
        }
        if (valueOfGiorno.equals("Venerdi")){
          valueOfGiorno = "Friday";
        }
        if (valueOfGiorno.equals("Sabato")){
          valueOfGiorno = "Saturday";
        }
        if (valueOfGiorno.equals("Domenica")){
          valueOfGiorno = "Sunday";
        }
        
        String valueOfMinTemp = "";
        if (bindingSet.getValue("minTemp") != null){
          valueOfMinTemp = bindingSet.getValue("minTemp").stringValue()+"&deg;C";
        }
        String valueOfMaxTemp = "";
        if (bindingSet.getValue("maxTemp") != null){
          valueOfMaxTemp = bindingSet.getValue("maxTemp").stringValue()+"&deg;C";
        }

        out.println("<div class=\"previsione-giorno\">");
        out.println("<span class=\"giorno\" id=\"day"+ i +"\">" + valueOfGiorno + "</span>");
        out.println("<br />");

        String nomeImmagine = "";
        if (valueOfDescrizione.equals("sereno")){
          nomeImmagine = "sereno.png";
          valueOfDescrizione = "cloudless";
        }
        else if (valueOfDescrizione.equals("poco nuvoloso")){
          nomeImmagine = "poco-nuvoloso.png";
          valueOfDescrizione = "bit cloudy";
        }
        else if (valueOfDescrizione.equals("velato")){
          nomeImmagine = "poco-nuvoloso.png";
          valueOfDescrizione = "bleary";
        }
        else if (valueOfDescrizione.equals("pioggia debole e schiarite")){
          nomeImmagine = "pioggia-sole.png";
          valueOfDescrizione = "light rain and sunny intervals";
        }
        else if (valueOfDescrizione.equals("nuvoloso")){
          nomeImmagine = "nuvoloso.png";
          valueOfDescrizione = "cloudy";
        }
        else if (valueOfDescrizione.equals("pioggia debole")){
          nomeImmagine = "pioggia.png";
          valueOfDescrizione = "light rain";
        }
        else if (valueOfDescrizione.equals("coperto")){
          nomeImmagine = "coperto.png";
          valueOfDescrizione = "overcast";
        }
        else if (valueOfDescrizione.equals("pioggia e schiarite")){
          nomeImmagine = "pioggia-sole.png";
          valueOfDescrizione = "rain and sunny intervals";
        }
        else if (valueOfDescrizione.equals("pioggia moderata-forte")){
          nomeImmagine = "temporale.png";
          valueOfDescrizione = "moderate rain";
        }
        else if (valueOfDescrizione.equals("foschia")){
          nomeImmagine = "foschia.png";
          valueOfDescrizione = "mist";
        }
        else if (valueOfDescrizione.equals("temporale")){
          nomeImmagine = "temporale.png";
          valueOfDescrizione = "rainstorm";
        }
        else if (valueOfDescrizione.equals("neve debole e schiarite")){
          nomeImmagine = "neve-sole.png";
          valueOfDescrizione = "light snow and sunny intervals";
        }
        else if (valueOfDescrizione.equals("temporale e schiarite")){
          nomeImmagine = "temporale-schiarite.png";
          valueOfDescrizione = "rainstorm and sunny intervals";
        }
        else if (valueOfDescrizione.equals("neve moderata-forte")){
          nomeImmagine = "neve.png";
          valueOfDescrizione = "moderate snow";
        }
        else if (valueOfDescrizione.equals("neve e schiarite")){
          nomeImmagine = "neve-sole.png";
          valueOfDescrizione = "snow and sunny intervals";
        }
        else if (valueOfDescrizione.equals("neve debole")){
          nomeImmagine = "pioggia-neve.png";
          valueOfDescrizione = "light snow";
        }
        else if (valueOfDescrizione.equals("pioggia neve")){
          nomeImmagine = "pioggia-neve.png";
          valueOfDescrizione = "light rain";
        }	
        else if (valueOfDescrizione.equals("nebbia")){
          nomeImmagine = "fog.png";
          valueOfDescrizione = "fog";
        }	
        out.println("<img class=\"immagine-meteo\" src=\""+request.getContextPath()+"/img/" + nomeImmagine + "\" width=\"48\" />");
        out.println("<br />");
        out.println("<span class=\"descrizione-meteo\" id=\"desc"+ i +"\">" + valueOfDescrizione +"</span>");
        out.println("<br />");
        out.println("<span class=\"temperature\"><span class=\"min\">" + valueOfMinTemp + "</span> / <span class=\"max\">" + valueOfMaxTemp + "</span></span>");
        out.println("</div>");
        i++;
      }
      out.println("<div class=\"clearer\"></div>");
      out.println("<div class=\"aggiornamento\"><span name=\"lbl\" caption=\"last_update\">Latest Update</span>: " + valueOfInstantDateTime + "</div>");
      out.println("<div ><a href='" +logEndPoint + valueOfWRep + "' title='Linked Open Graph' target='_blank'>"+valueOfWRep+"</a></div>");
      String divSavePin= "savePin-weather-"+nomeComune;
      out.println("<div id=\""+divSavePin+"\" class=\"savePin\" onclick=save_handler('weather','"+valueOfWRep+"','meteo"+nomeComune+"')></div>");
      out.println("</div>");
    }
    finally{con.close();}
  }
%>