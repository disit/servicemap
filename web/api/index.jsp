<%@page import="java.net.URLDecoder"%>
<%@page import="org.disit.servicemap.api.ServiceMapApi"%>
<%@ page import="org.openrdf.query.algebra.Count"%>
<%@ page import="java.io.IOException"%> 
<%@ page import="org.json.simple.JSONObject"%>
<%@ page import="org.openrdf.model.Value"%>
<%@ page import="java.util.*"%>
<%@ page import="org.openrdf.repository.Repository"%>
<%@ page import="org.openrdf.repository.sparql.SPARQLRepository"%>
<%@ page import="java.sql.*"%>
<%@ page import="java.util.List"%>
<%@ page import="org.openrdf.query.BooleanQuery"%>
<%@ page import="org.openrdf.OpenRDFException"%>
<%@ page import="org.openrdf.repository.RepositoryConnection"%>
<%@ page import="org.openrdf.query.TupleQuery"%>
<%@ page import="org.openrdf.query.resultio.sparqljson.SPARQLBooleanJSONWriter"%>
<%@ page import="org.openrdf.query.TupleQueryResult"%>
<%@ page import="org.openrdf.query.BindingSet"%>
<%@ page import="org.openrdf.query.QueryLanguage"%>
<%@ page import="java.io.File"%>
<%@ page import="java.net.URL"%>
<%@ page import="org.apache.http.HttpResponse"%>
<%@ page import="org.apache.http.client.HttpClient"%>
<%@ page import="org.openrdf.rio.RDFFormat"%>
<%@ page import="java.text.Normalizer"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file= "/include/parameters.jsp" %>
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

String uid = request.getParameter("uid");

if ("html".equals(request.getParameter("format")) || (request.getParameter("format") == null && request.getParameter("queryId") != null)) {%>
<jsp:include page="../mappa.jsp" > <jsp:param name="mode" value="query"/> </jsp:include>
<%
    response.setContentType("text/html; charset=UTF-8");
    String idService = request.getParameter("serviceUri");
    String selection = request.getParameter("selection");
    String categorie = "";
    categorie = request.getParameter("categories");
    if (categorie == null) {
      categorie = request.getParameter("categorie");
    }
    String raggi = "";
    String[] arrayRaggi = null;
    String raggioServizi = "";
    String raggioSensori = "";
    String raggioBus = "";

    raggi = request.getParameter("maxDists");
    if (raggi == null) {
      raggi = request.getParameter("raggi");
    }
    if (raggi != null) {
      arrayRaggi = raggi.split(";");
      if (arrayRaggi.length > 0) {
        raggioServizi = arrayRaggi[0];
      }
      if (arrayRaggi.length > 1) {
        raggioSensori = arrayRaggi[1];
      } else {
        raggioSensori = raggioServizi;
      }
      if (arrayRaggi.length > 2) {
        raggioBus = arrayRaggi[2];
      } else {
        raggioBus = raggioSensori;
      }
    }
    String risultati = "100";
    risultati = request.getParameter("maxResults");
    if (risultati == null) {
      risultati = request.getParameter("risultati");
    }

    String[] arrayRisultati = null;
    String risultatiServizi = "100";
    String risultatiSensori = "100";
    String risultatiBus = "100";
    if (risultati != null) {
      arrayRisultati = risultati.split(";");
      if (arrayRisultati.length > 0) {
        risultatiServizi = arrayRisultati[0];
      }
      if (arrayRisultati.length > 1) {
        risultatiSensori = arrayRisultati[1];
      } else {
        risultatiSensori = risultatiServizi;
      }
      if (arrayRisultati.length > 2) {
        risultatiBus = arrayRisultati[2];
      } else {
        risultatiBus = risultatiSensori;
      }
    }
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    String queryId = request.getParameter("queryId");
    String text = request.getParameter("text");
    if (queryId == null) {
      if (idService != null) {
        ServiceMap.logAccess(request, null, null, null, idService, "api-service-info", null, null, null, null, "html", uid, null);
      } else {
        ServiceMap.logAccess(request, null, selection, categorie, null, "api-services", risultati, raggi, null, text, "html", uid, null);
      }
    } else {
      ServiceMap.logAccess(request, null, null, null, null, "api-services-by-queryid", null, null, queryId, null, "html", uid, null);
    }
  } else { //format json
    ServiceMapApi serviceMapApi = new ServiceMapApi();
    response.setContentType("application/json; charset=UTF-8");

    RepositoryConnection con = ServiceMap.getSparqlConnection();

    String idService = request.getParameter("serviceUri");
    String selection = request.getParameter("selection");
    String categorie = "";
    categorie = request.getParameter("categories");
    if (categorie == null) {
      categorie = request.getParameter("categorie");
    }
    if(categorie==null) {
      categorie="Service;RoadSensor;NearBusStops";
    }
    categorie = categorie.replace("NearBusStops","BusStop");
    categorie = categorie.replace("RoadSensor","SensorSite");

    String raggi = "";
    String typeSaving = "";
    String textToSearch = "";
    raggi = request.getParameter("maxDists");
    if (raggi == null) {
      raggi = request.getParameter("raggio");
    }
    String[] arrayRaggi = null;
    String raggioServizi = "";
    String raggioSensori = "";
    String raggioBus = "";
    if (raggi == null) {
      raggi = "0.1;0.1;0.1";
    }
    arrayRaggi = raggi.split(";");
    if (arrayRaggi.length > 0) {
      raggioServizi = arrayRaggi[0];
    }
    if (arrayRaggi.length > 1) {
      raggioSensori = arrayRaggi[1];
    } else {
      raggioSensori = raggioServizi;
    }
    if (arrayRaggi.length > 2) {
      raggioBus = arrayRaggi[2];
    } else {
      raggioBus = raggioSensori;
    }

    String risultati = "";
    risultati = request.getParameter("maxResults");
    if (risultati == null) {
      risultati = request.getParameter("risultati");
    }
    if (risultati == null) {
      risultati = "100;100;100";
    }
    String[] arrayRisultati = null;
    String risultatiServizi = "";
    String risultatiSensori = "";
    String risultatiBus = "";
    arrayRisultati = risultati.split(";");
    if (arrayRisultati.length > 0) {
      risultatiServizi = arrayRisultati[0];
    }
    if (arrayRisultati.length > 1) {
      risultatiSensori = arrayRisultati[1];
    } else {
      risultatiSensori = risultatiServizi;
    }
    if (arrayRisultati.length > 2) {
      risultatiBus = arrayRisultati[2];
    } else {
      risultatiBus = risultatiSensori;
    }
    textToSearch = request.getParameter("search");
    if(textToSearch!=null)
      typeSaving = "freeText";
    else {
      textToSearch = request.getParameter("text");
    }
    String limit = request.getParameter("limit");
    if(limit==null)
      limit=risultatiServizi;

    String queryId = request.getParameter("queryId");
    if (queryId != null) {
      Connection conMySQL = null;
      Statement st = null;
      ResultSet rs = null;
      conMySQL = ConnectionPool.getConnection(); //DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
      String queryForType = "select * from Queries where id=\"" + queryId + "\"";
      st = conMySQL.createStatement();
      rs = st.executeQuery(queryForType);
      String typeOfSaving = "";
      if (rs.next()) {
        typeOfSaving = rs.getString("typeSaving");
        if ("service".equals(typeOfSaving)) {
          idService = rs.getString("idService");
        }
        else {
          categorie = rs.getString("categorie");
          categorie = categorie.replace("[", "");
          categorie = categorie.replace("]", "");
          categorie = categorie.replace(",", ";");
          categorie = categorie.replaceAll("\\s+", "");
          risultatiServizi = rs.getString("numeroRisultatiServizi");
          risultatiSensori = rs.getString("numeroRisultatiSensori");
          risultatiBus = rs.getString("numeroRisultatiBus");
          typeSaving = rs.getString("typeSaving");
          textToSearch = rs.getString("text");
          if (rs.getString("actualSelection").indexOf("COMUNE di") != -1) {
            selection = rs.getString("actualSelection");
          } else {
            selection = URLDecoder.decode(rs.getString("coordinateSelezione"));
          }
          raggioServizi = rs.getString("raggioServizi");
          raggioSensori = rs.getString("raggioSensori");
          raggioBus = rs.getString("raggioBus");
        }
      }
      else {
        response.sendError(400, "'queryId' not found");
        conMySQL.close();
        return;
      }
      conMySQL.close();
    }
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    String reqFrom = request.getParameter("requestFrom");

    if (idService != null) {
      //get data of a single service
      int i = 0;
      ServiceMap.logAccess(request, null, null, null, idService, "api-service-info", null, null, queryId, null, "json", uid, reqFrom);
      ArrayList<String> serviceTypes = ServiceMap.getTypes(con, idService);
      if(serviceTypes.size()==0) {
        response.sendError(400, "no type found for "+idService);
      }
      if (serviceTypes.contains("BusStop")|| serviceTypes.contains("NearBusStops")) {
        serviceMapApi.queryBusStop(out, con, idService);
      }
      else if (serviceTypes.contains("WeatherReport") || serviceTypes.contains("Municipality")) {
        serviceMapApi.queryMeteo(out, con, idService);
      }
      else if (serviceTypes.contains("SensorSite") || serviceTypes.contains("RoadSensor")) {
        serviceMapApi.querySensor(out, con, idService);
      }
      else if (serviceTypes.contains("Service") || serviceTypes.contains("RegularService")) {
        serviceMapApi.queryService(out, con, idService);
      }
      else
        response.sendError(400, "no info found for "+idService);
      con.close();
    } else {
      try {
        List<String> listaCategorie = new ArrayList<String>();
        if ("textSearch".equals(typeSaving)) {
          //... da gestire?
          throw new Exception("to be done");
        } else if ("freeText".equals(typeSaving)) {
          if (textToSearch != null && !"".equals(textToSearch)) {
            //String limit=request.getParameter("limit");
            //search=unescapeUri(search);
            serviceMapApi.queryFulltext(out, con, textToSearch, selection, raggioServizi, limit);
            ServiceMap.logAccess(request, null, selection, null, null, "api-text-search", null, raggioServizi, queryId, textToSearch, "json", uid, reqFrom);
          }
        } else {
          if (selection!=null && selection.indexOf("COMUNE di") != -1) {
            //getServices in Municipality
            serviceMapApi.queryMunicipalityServices(out, con, selection, categorie, textToSearch, risultatiBus, risultatiSensori, risultatiServizi);
            ServiceMap.logAccess(request, null, selection, categorie, null, "api-services-by-municipality", risultati, null, queryId, textToSearch, "json", uid, reqFrom);
          } else {
            String[] coords = null;
            if (selection!=null && selection.startsWith("http://")) {
              String queryForCoordinates = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
                      + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>"
                      + "SELECT ?lat ?long {{"
                      + " <" + selection + "> km4c:hasAccess ?entry."
                      + " ?entry geo:lat ?lat."
                      //+ " FILTER (?lat>40) "
                      + " ?entry geo:long ?long."
                      //+ " FILTER (?long>10) ."
                      + "}UNION{"
                      + " <" + selection + "> km4c:isInRoad ?road."
                      + " <" + selection + "> geo:lat ?lat."
                      //+ " FILTER (?lat>40) "
                      + " <" + selection + "> geo:long ?long."
                      //+ " FILTER (?long>10) ."
                      + "}UNION{"
                      + " <" + selection + "> geo:lat ?lat."
                      //+ " FILTER (?lat>40) "
                      + " <" + selection + "> geo:long ?long."
                      //+ " FILTER (?long>10) ."
                      + "} "
                      + "}LIMIT 1";
              TupleQuery tupleQueryForCoordinates = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForCoordinates);
              TupleQueryResult resultCoord = tupleQueryForCoordinates.evaluate();
              if(resultCoord.hasNext()) {
                BindingSet bindingSetCoord = resultCoord.next();
                selection = bindingSetCoord.getValue("lat").stringValue() + ";" + bindingSetCoord.getValue("long").stringValue();
              }
            }
            else if(selection!=null && selection.contains(";")) {
              coords = selection.split(";");
            }
            // get services by lat/long
            if(coords!=null && (coords.length==2 || coords.length==4))
              serviceMapApi.queryLatLngServices(out, con, coords, categorie, textToSearch, raggioBus, raggioSensori, raggioServizi, risultatiBus, risultatiSensori, risultatiServizi);
            ServiceMap.logAccess(request, null, selection, categorie, null, "api-services-by-gps", risultati, raggi, queryId, textToSearch, "json", uid, reqFrom);
          }
        }
      } catch (Exception e) {
        response.sendError(500);
        ServiceMap.notifyException(e);
        throw e;
      }
    }
  }
%>