<%@page import="org.disit.servicemap.api.IoTChecker"%>
<%@page import="org.disit.servicemap.JwtUtil.User"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.ParseException"%>
<%@page import="org.disit.servicemap.ServiceMapping"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@page import="org.disit.servicemap.api.CheckParameters"%>
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
User u = null;
try {
  u = org.disit.servicemap.JwtUtil.getUserFromRequest(request);
} catch(Exception e) {
  ServiceMap.notifyException(e, "url:" + request.getRequestURL().append("?" + request.getQueryString()) + "\naccessToken:" + org.disit.servicemap.JwtUtil.getTokenFromRequest(request)+"\n");
}
if(u!=null) {
  ServiceMap.println("user:"+u.username+" role:"+u.role);
} else {
  ServiceMap.println("nouser");
}

String uid = request.getParameter("uid");
if(uid!=null && !ServiceMap.validateUID(uid)) {
  ServiceMap.logError(request, response, 404, "invalid uid");
  return;
}
String idService = request.getParameter("serviceUri");
if(idService!=null)
  idService = ServiceMap.serviceUriEncode(idService.trim());
String selection = request.getParameter("selection");
String queryId = request.getParameter("queryId");
String search = request.getParameter("search");
String showBusPosition = request.getParameter("showBusPosition");
String value_type = request.getParameter("value_type");
String graphUri = request.getParameter("graphUri");
String valueName = request.getParameter("valueName");
String apikey = request.getParameter("apikey");
        
if(idService==null && selection==null && queryId==null && search==null && showBusPosition==null) {
    ServiceMap.logError(request, response, 400, "please specify 'selection', 'search', 'serviceUri' or 'queryId' parameters");
    return;
}
if(queryId!=null && (queryId.length()>32 || !queryId.matches("[0-9a-fA-F]+"))) {
    ServiceMap.logError(request, response, 400, "invalid queryId parameter");
    return;
}
String check;
if(idService!=null && (check=CheckParameters.checkUri(idService))!=null) {
    ServiceMap.logError(request, response, 400, "invalid 'serviceUri' parameter");
    return;
}
if((check=CheckParameters.checkSelection(selection))!=null) {
    ServiceMap.logError(request, response, 400, "invalid 'selection' parameter: " + check);
    return;  
}
if(apikey!=null && (check=CheckParameters.checkApiKey(apikey))!=null) {
    ServiceMap.logError(request, response, 400, check);
    return;  
}
if(apikey==null && u!=null) {
  apikey="user:"+u.username+" role:"+u.role+" at:"+u.accessToken;
}
  
if ("html".equals(request.getParameter("format")) || (request.getParameter("format") == null && request.getParameter("queryId") != null)) {%>
<jsp:include page="../../mappa.jsp" > <jsp:param name="mode" value="query"/> </jsp:include>
<%
    response.setContentType("text/html; charset=UTF-8");
    String categorie = "";
    categorie = request.getParameter("categories");
    if (categorie == null) {
      categorie = request.getParameter("categorie");
    }
    categorie = ServiceMap.cleanCategories(categorie);
    String raggi = "";
    String[] arrayRaggi = null;
    String raggioServizi = "";
    String raggioSensori = "";
    String raggioBus = "";

    raggi = request.getParameter("maxDists");
    if (raggi == null) {
      raggi = request.getParameter("raggi");
    }
    try {
      Double.parseDouble(raggi);
    } catch(Exception e) {
      raggi = "";
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
    try {
      Integer.parseInt(risultati);
    } catch(Exception e) {
      risultati = "100";
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
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();
    response.setContentType("application/json; charset=UTF-8");
    response.addHeader("Access-Control-Allow-Origin", "*");

    String realtime = request.getParameter("realtime");
    if(!"false".equals(realtime))
      realtime="true";

    String fullCount = request.getParameter("fullCount");
    if(!"false".equals(fullCount))
      fullCount="true";
    
    String checkHealthiness = request.getParameter("healthiness");
    if(!"true".equals(checkHealthiness))
      checkHealthiness="false";
    
    String textToSearch = request.getParameter("search");
    String typeSaving = "";
    if(textToSearch!=null)
      typeSaving = "freeText";
    else {
      textToSearch = request.getParameter("text");
    }

    if(idService==null && selection==null && queryId==null && textToSearch==null) {
        ServiceMap.logError(request, response, 400, "please specify one of 'selection', 'search', 'serviceUri' or 'queryId' parameters");
        return;
    }
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
    
    categorie = ServiceMap.cleanCategories(categorie);

    String raggi = "";
    raggi = request.getParameter("maxDists");
    if (raggi == null) {
      raggi = request.getParameter("raggio");
    }
    try {
      if(raggi!=null)
        Double.parseDouble(raggi);
    } catch(Exception e) {
      raggi = null;
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
    try {
      if(risultati!=null)
        Integer.parseInt(risultati);
    } catch(Exception e) {
      risultati = null;
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
    if(textToSearch!=null) {
      textToSearch = java.net.URLDecoder.decode(textToSearch, "UTF-8");
      //textToSearch = new String(textToSearch.getBytes("iso-8859-1"), "UTF-8"); //workaround! when utf8 data is sent via GET
    }
    String limit = request.getParameter("limit");
    try {
      if(limit!=null)
        Integer.parseInt(limit);
    } catch(Exception e) {
      limit=null;
    }
    if(limit==null)
      limit=risultatiServizi;
    
    String lang = request.getParameter("lang");
    if(lang==null || (!lang.equals("it") && !lang.equals("en")))
      lang = "en";
    
    String geometry = request.getParameter("geometry");
    boolean getGeometry = "true".equalsIgnoreCase(geometry);
    boolean findInside = "inside".equalsIgnoreCase(raggi);
    
    String toTime = request.getParameter("toTime");
    if(toTime!=null) {
      if(!toTime.matches("^\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d$")) {
        ServiceMap.logError(request, response, 400, "invalid 'toTime' parameter, expected yyyy-mm-ddThh:mm:ss");
        return;
      }
    }

    String fromTime = request.getParameter("fromTime");
    if(fromTime!=null) {
      if(fromTime.matches("^\\d*-(day|hour|minute)$")) {
        String[] d=fromTime.split("-");
        long n=Long.parseLong(d[0]);
        if(d[1].equals("day"))
          n=n*24*60*60;
        else if(d[1].equals("hour"))
          n=n*60*60;
        else if(d[1].equals("minute"))
          n=n*60;
        Date now = new Date();
        SimpleDateFormat dateFormatter=new SimpleDateFormat(ServiceMap.dateFormat);
        if(toTime!=null) {
          //parse toTime and store as now
          try {
            now = dateFormatter.parse(toTime.replace("T", " "));
          } catch(ParseException e) {
            ServiceMap.logError(request, response, 400, "invalid toTime "+toTime);
            return;
          }
        }          
        Date from=new Date(now.getTime()-n*1000);
        fromTime=dateFormatter.format(from).replace(" ", "T");
      } else if(!fromTime.matches("^last-\\d*$") && !fromTime.matches("^\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d$")) {
        ServiceMap.logError(request, response, 400, "invalid 'fromTime' parameter, expected n-day,n-hour,n-minute,last-n or yyyy-mm-ddThh:mm:ss");
        return;
      }
    }
    ServiceMap.println("fromTime:"+fromTime);
    ServiceMap.println("toTime:"+toTime);
    
    if (queryId != null) {
      Connection conMySQL = null;
      PreparedStatement st = null;
      ResultSet rs = null;
      conMySQL = ConnectionPool.getConnection();
      try {
        st = conMySQL.prepareStatement("select * from Queries where id=? or idRW=?");
        st.setString(1, queryId);
        st.setString(2, queryId);
        rs = st.executeQuery();
        String typeOfSaving = "";
        if (rs.next()) {
          typeOfSaving = rs.getString("typeSaving");
          if ("service".equals(typeOfSaving)) {
            idService = rs.getString("idService");
          }
          else {
            //nota: se alcuni parametri sono passati nella url vengono usati quelli invece di quelli specificati dal queryId
            if(request.getParameter("categories")==null) {
              categorie = rs.getString("categorie");
              categorie = categorie.replace("[", "");
              categorie = categorie.replace("]", "");
              categorie = categorie.replace(",", ";");
              categorie = categorie.replaceAll("\\s+", "");
            }
            if(request.getParameter("maxResults")==null) {
              risultatiServizi = rs.getString("numeroRisultatiServizi");
              risultatiSensori = rs.getString("numeroRisultatiSensori");
              risultatiBus = rs.getString("numeroRisultatiBus");
            }
            if(request.getParameter("text")==null)
              textToSearch = rs.getString("text");
            if(request.getParameter("selection")==null) {
              if (rs.getString("actualSelection").indexOf("COMUNE di") != -1) {
                selection = rs.getString("actualSelection");
              } else {
                selection = URLDecoder.decode(rs.getString("coordinateSelezione"));
              }
            }
            if(request.getParameter("maxDists")==null) {
              raggioServizi = rs.getString("raggioServizi");
              raggioSensori = rs.getString("raggioSensori");
              raggioBus = rs.getString("raggioBus");
            }
          }
        }
        else {
          ServiceMap.logError(request, response, 400, "'queryId' not found");
          return;
        }
        st.close();
      } finally {
        conMySQL.close();
      }
    }
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    String reqFrom = request.getParameter("requestFrom");
    String requestType = (idService==null ? "api" : "details");
    if(! ServiceMap.checkIP(ip, requestType)) {
      ServiceMap.logError(request, response, 403,"API calls daily limit reached");
      return;
    }

    RepositoryConnection con = ServiceMap.getSparqlConnection();

    if (idService != null) {
      //get data of a single service
      int i = 0;
      ArrayList<String> serviceTypes = ServiceMap.getTypes(con, ServiceMapping.getInstance().getServiceUriAlias(idService), apikey);
      if(serviceTypes.size()==0) {
        ServiceMap.logError(request, response, 400, "no type found for "+idService);
        con.close();
        return;
      }
      if(!IoTChecker.checkIoTService(idService, apikey)) {
        ServiceMap.logError(request, response, (apikey==null || apikey.isEmpty() ? 401 : 403), "cannot access to "+idService);
        con.close();
        return;
      }
      String types = null;
      ServiceMapping.MappingData md = ServiceMapping.getInstance().getMappingForServiceType(1, serviceTypes);
      if(md!=null && (md.realTimeSqlQuery!=null || md.realTimeSparqlQuery!=null || md.realTimeSolrQuery!=null )) {
        types = serviceMapApi.queryService(out, con, idService, lang, realtime, valueName, fromTime, toTime, checkHealthiness, uid, serviceTypes);        
      } 
      else if (serviceTypes.contains("http://vocab.gtfs.org/terms#Stop") && (serviceTypes.contains("BusStop")|| serviceTypes.contains("NearBusStops"))) {
        serviceMapApi.queryTplStop(out, con, idService, "BusStop", lang, realtime, uid, toTime);
        types = "TransferServiceAndRenting;BusStop";
      }
      else if (serviceTypes.contains("http://vocab.gtfs.org/terms#Stop") && serviceTypes.contains("Tram_stops")) {
        serviceMapApi.queryTplStop(out, con, idService, "Tram_stops", lang, realtime, uid, toTime);
        types = "TransferServiceAndRenting;Tram_stops";
      }
      else if (serviceTypes.contains("http://vocab.gtfs.org/terms#Stop") && serviceTypes.contains("Train_station")) {
        serviceMapApi.queryTplStop(out, con, idService, "Train_station", lang, realtime, uid, toTime);
        types = "TransferServiceAndRenting;Train_station";
      }
      else if (serviceTypes.contains("http://vocab.gtfs.org/terms#Stop") && serviceTypes.contains("Ferry_stop")) {
        serviceMapApi.queryTplStop(out, con, idService, "Ferry_stop", lang, realtime, uid, toTime);
        types = "TransferServiceAndRenting;Ferry_station";
      }
      else if (serviceTypes.contains("http://vocab.gtfs.org/terms#Stop") && serviceTypes.contains("Subway_station")) {
        serviceMapApi.queryTplStop(out, con, idService, "Subway_station", lang, realtime, uid, toTime);
        types = "TransferServiceAndRenting;Subway_station";
      }
      else if (serviceTypes.contains("WeatherReport") || serviceTypes.contains("Municipality")) {
        serviceMapApi.queryMeteo(out, con, idService, lang);
      }
      else if ((serviceTypes.contains("SensorSite") || serviceTypes.contains("RoadSensor")) && !serviceTypes.contains("Fuel_station")) {
        serviceMapApi.querySensor(out, con, idService, lang, realtime, checkHealthiness, uid, fromTime);
        types = "TransferServiceAndRenting;SensorSite";
      }
      else if (serviceTypes.contains("Service") || serviceTypes.contains("RegularService")) {
        types = serviceMapApi.queryService(out, con, idService, lang, realtime, valueName, fromTime, toTime, checkHealthiness, uid, serviceTypes);
      }
      else if (serviceTypes.contains("Event")) {
        serviceMapApi.queryEvent(out, con, idService, lang, uid);
        types = "Service;Event";
      }
      else {
        types = serviceMapApi.queryService(out, con, idService, lang, realtime, valueName, fromTime, toTime, checkHealthiness, uid, serviceTypes);
      }
      ServiceMap.logAccess(request, null, null, types, idService, "api-service-info", null, null, queryId, null, "json", uid, reqFrom);
      ServiceMap.updateResultsPerIP(ip, requestType, 1);
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
            serviceMapApi.queryFulltext(out, con, textToSearch, selection, raggioServizi, limit, lang, getGeometry, apikey);
            ServiceMap.logAccess(request, null, selection, null, null, "api-text-search", null, raggioServizi, queryId, textToSearch, "json", uid, reqFrom);
            ServiceMap.updateResultsPerIP(ip, requestType, 1);
          }
        } else {
          String[] coords = null;
          if (selection.startsWith("http:")) {
            String msg;
            if((msg=CheckParameters.checkUri(selection)) != null) {
              response.sendError(400, "invalid uri "+msg);
              return;
            }
            
            String queryForCoordinates = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
                    + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>"
                    + "SELECT ?lat ?long {{"
                    + " <" + selection + "> km4c:hasAccess ?entry."
                    + " ?entry geo:lat ?lat."
                    + " ?entry geo:long ?long."
                    + "}UNION{"
                    + " <" + selection + "> geo:lat ?lat;"
                    + "  geo:long ?long."
                    + "} "
                    + "}LIMIT 1";
            TupleQuery tupleQueryForCoordinates = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForCoordinates);
            TupleQueryResult resultCoord = tupleQueryForCoordinates.evaluate();
            if(resultCoord.hasNext()) {
              BindingSet bindingSetCoord = resultCoord.next();
              selection = bindingSetCoord.getValue("lat").stringValue() + ";" + bindingSetCoord.getValue("long").stringValue();
            } else {
              // the uri doesn't have a GPS position
              String queryForGraph = "SELECT * {"
                    + "GRAPH <"+selection+"> {?s ?p ?o}"
                    + "}LIMIT 1";
              TupleQuery tupleQueryForGraph = con.prepareTupleQuery(QueryLanguage.SPARQL, queryForGraph);
              TupleQueryResult resultGraph = tupleQueryForGraph.evaluate();
              if(resultGraph.hasNext()) {
                selection = "graph:"+selection;
                graphUri = selection;
              } else {
              }
            }
          }
          if(selection.startsWith("wkt:") || selection.startsWith("geo:") || selection.startsWith("graph:")) {
            String[] coordWkt = { selection };
            coords = coordWkt;              
          } else if(selection.contains(";")) {
            coords = selection.split(";");
            for(int i=0; i<coords.length; i++)
              Double.parseDouble(coords[i]);
          }
          // get services by lat/long
          if(coords!=null && (coords.length==2 || coords.length==4 || (coords.length==1 && (coords[0].startsWith("wkt:") || selection.startsWith("geo:") || selection.startsWith("graph:"))))) {
            int results = serviceMapApi.queryLatLngServices(out, con, coords, categorie, textToSearch, raggioBus, raggioSensori, raggioServizi, risultatiBus, risultatiSensori, risultatiServizi, lang, null, getGeometry, findInside, true, fullCount, value_type, graphUri, valueName, apikey);
            ServiceMap.updateResultsPerIP(ip, requestType, results);
            ServiceMap.logAccess(request, null, selection, categorie, null, "api-services-by-gps", risultati, raggi, queryId, textToSearch, "json", uid, reqFrom);
          }
          else {
            //getServices in Municipality
            int results = serviceMapApi.queryMunicipalityServices(out, con, selection, categorie, textToSearch, risultatiBus, risultatiSensori, risultatiServizi, lang, getGeometry, fullCount, apikey);
            ServiceMap.updateResultsPerIP(ip, requestType, results);
            ServiceMap.logAccess(request, null, selection, categorie, null, "api-services-by-municipality", risultati, null, queryId, textToSearch, "json", uid, reqFrom);
          }
        }
      } catch (Exception e) {
        ServiceMap.notifyException(e);
      }
    }
    con.close();
  }
%>