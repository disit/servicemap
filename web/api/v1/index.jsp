<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@page import="java.net.URLDecoder"%>
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

String uid = request.getParameter("uid");
if ("html".equals(request.getParameter("format")) || (request.getParameter("format") == null && request.getParameter("queryId") != null)) {%>
<jsp:include page="../../mappa.jsp" > <jsp:param name="mode" value="query"/> </jsp:include>
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
    String ip = request.getRemoteAddr();
    String ua = request.getHeader("User-Agent");
    String queryId = request.getParameter("queryId");
    String text = request.getParameter("text");
    if (queryId == null) {
      if (idService != null) {
        logAccess(ip, null, ua, null, null, idService, "api-service-info", null, null, null, null, "html", uid);
      } else {
        logAccess(ip, null, ua, selection, categorie, null, "api-services", risultati, raggi, null, text, "html", uid);
      }
    } else {
      logAccess(ip, null, ua, null, null, null, "api-services-by-queryid", null, null, queryId, null, "html", uid);
    }
  } else { //format json
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();
    response.setContentType("application/json; charset=UTF-8");
    response.addHeader("Access-Control-Allow-Origin", "*");

    Repository repo = new SPARQLRepository(sparqlEndpoint);
    repo.initialize();
    RepositoryConnection con = repo.getConnection();

    String idService = request.getParameter("serviceUri");
    String realtime = request.getParameter("realtime");
    if(!"false".equals(realtime))
      realtime="true";
    String selection = request.getParameter("selection");
    String categorie = "";
    categorie = request.getParameter("categories");
    if (categorie == null) {
      categorie = request.getParameter("categorie");
    }
    
    if(categorie==null || categorie.equals("Service")) {
      categorie="Service;RoadSensor;NearBusStops";
    }
    categorie = categorie.replace("NearBusStops","BusStop");
    categorie = categorie.replace("RoadSensor","SensorSite");
    if(categorie.contains("TransferServiceAndRenting")){
      categorie = categorie.replace("TransferServiceAndRenting","TransferServiceAndRenting;BusStop;SensorSite");
    }

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
    
    String lang = request.getParameter("lang");
    if(lang==null)
      lang = "en";

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
      else 
        response.sendError(400, "'queryId' not found");
      st.close();
      conMySQL.close();
    }
    String ip = request.getRemoteAddr();
    String ua = request.getHeader("User-Agent");

    if (idService != null) {
      //get data of a single service
      int i = 0;
      ArrayList<String> serviceTypes = ServiceMap.getTypes(con, idService);
      if(serviceTypes.size()==0) {
        response.sendError(400, "no type found for "+idService);
        return;
      }
      String types = null;
      if (serviceTypes.contains("BusStop")|| serviceTypes.contains("NearBusStops")) {
        serviceMapApi.queryBusStop(out, con, idService, lang, realtime);
        types = "TransferServiceAndRenting;BusStop";
      }
      else if (serviceTypes.contains("WeatherReport") || serviceTypes.contains("Municipality")) {
        serviceMapApi.queryMeteo(out, con, idService, lang);
      }
      else if (serviceTypes.contains("SensorSite") || serviceTypes.contains("RoadSensor")) {
        serviceMapApi.querySensor(out, con, idService, lang, realtime);
        types = "TransferServiceAndRenting;SensorSite";
      }
      else if (serviceTypes.contains("Service") || serviceTypes.contains("RegularService")) {
        types = serviceMapApi.queryService(out, con, idService, lang, realtime);
      }
      else if (serviceTypes.contains("Event")) {
        serviceMapApi.queryEvent(out, con, idService, lang);
        types = "Service;Event";
      }
      else
        response.sendError(400, "no info found for "+idService);
      logAccess(ip, null, ua, null, types, idService, "api-service-info", null, null, queryId, null, "json", uid);
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
            serviceMapApi.queryFulltext(out, con, textToSearch, selection, raggioServizi, limit, lang);
            logAccess(ip, null, ua, selection, null, null, "api-text-search", null, raggioServizi, queryId, textToSearch, "json", uid);
          }
        } else {
          if (selection!=null && selection.indexOf("COMUNE di") != -1) {
            //getServices in Municipality
            serviceMapApi.queryMunicipalityServices(out, con, selection, categorie, textToSearch, risultatiBus, risultatiSensori, risultatiServizi, lang);
            logAccess(ip, null, ua, selection, categorie, null, "api-services-by-municipality", risultati, null, queryId, textToSearch, "json", uid);
          } else {
            String[] coords = null;
            if (selection.indexOf("http:") != -1) {
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
            if(selection.contains(";")) {
              coords = selection.split(";");
            }
            // get services by lat/long
            if(coords!=null && (coords.length==2 || coords.length==4))
              serviceMapApi.queryLatLngServices(out, con, coords, categorie, textToSearch, raggioBus, raggioSensori, raggioServizi, risultatiBus, risultatiSensori, risultatiServizi, lang);
              logAccess(ip, null, ua, selection, categorie, null, "api-services-by-gps", risultati, raggi, queryId, textToSearch, "json", uid);
          }
        }
      } catch (Exception e) {
        out.println(e.getMessage());
        e.printStackTrace();
      }
    }
  }
%>