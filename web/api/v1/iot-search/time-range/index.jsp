<%@page import="org.disit.servicemap.api.IoTSearchApi"%>
<%@page import="org.disit.servicemap.api.IoTChecker"%>
<%@page import="org.disit.servicemap.JwtUtil.User"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.ParseException"%>
<%@page import="org.disit.servicemap.ServiceMapping"%>
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
  } catch (Exception e) {
    ServiceMap.notifyException(e, "url:" + request.getRequestURL().append("?" + request.getQueryString()) + "\naccessToken:" + org.disit.servicemap.JwtUtil.getTokenFromRequest(request) + "\n");
  }
  if (u != null) {
    ServiceMap.println("user:" + u.username + " role:" + u.role);
  } else {
    ServiceMap.println("nouser");
  }

  String selection = request.getParameter("selection");
  String serviceUri = request.getParameter("serviceUri");
  String model = request.getParameter("model");
  String fromResult = request.getParameter("fromResult");
  String valueFilters = request.getParameter("valueFilters");
  String values = request.getParameter("values");
  String sortOnValue = request.getParameter("sortOnValue");
  String categories = request.getParameter("categories");
  String maxDists = request.getParameter("maxDists");
  String maxResults = request.getParameter("maxResults");
  String text = request.getParameter("text");
  String fromTime = request.getParameter("fromTime");
  String toTime = request.getParameter("toTime");
  String aggregate = request.getParameter("aggregate");

  if (selection == null && model==null && valueFilters==null && categories==null && serviceUri==null) {
    ServiceMap.logError(request, response, 400, "please specify 'selection' or 'model' or 'valueFilter' or 'categories' or 'serviceUri' parameter");
    return;
  }
  if (fromTime==null) {
    ServiceMap.logError(request, response, 400, "please specify 'fromTime' parameter");
    return;
  }
  
  String check;
  if (selection != null && (check = CheckParameters.checkSelection(selection)) != null) {
    ServiceMap.logError(request, response, 400, "invalid 'selection' parameter: " + check);
    return;
  }
  if(selection==null)
    selection = "";
  
  String[] suris = null;
  if (serviceUri!=null) {
    suris = IoTSearchApi.processServiceUris(serviceUri);
    for(String s: suris) {
      if((check = CheckParameters.checkUri(s))!=null) {
        ServiceMap.logError(request, response, 400, "invalid 'serviceUri' parameter: "+s+" "+check);  
        return;
      }
    }
  }
    
  if (model != null && (check = CheckParameters.checkAlphanumString(model)) != null) {
    ServiceMap.logError(request, response, 400, "invalid 'model' parameter: "+check);
    return;
  }
  if (fromResult != null && (check = CheckParameters.checkNumber(fromResult)) != null) {
    ServiceMap.logError(request, response, 400, "invalid 'fromResult' parameter: ");
    return;
  }
  if (maxDists != null && (check = CheckParameters.checkNumber(maxDists)) != null) {
    ServiceMap.logError(request, response, 400, "invalid 'maxDists' parameter: "+check);
    return;
  } else if(maxDists == null) {
    maxDists = "0.1"; //km
  }
  if (maxResults != null && (check = CheckParameters.checkInteger(maxResults)) != null) {
    ServiceMap.logError(request, response, 400, "invalid 'maxResults' parameter: "+check);
    return;
  } else if(maxResults == null) {
    maxResults = "100";
  }
  if (values != null && (check = CheckParameters.checkExtAlphanumString(values)) != null) {
    ServiceMap.logError(request, response, 400, "invalid 'values' parameter: "+check);
    return;
  }
  if(aggregate!=null && (check = CheckParameters.checkEnum(aggregate, new String[] {"true","false"})) != null) {
    ServiceMap.logError(request, response, 400, "invalid 'aggregate' parameter: "+check);
    return;
  }
  if(fromResult!=null && "true".equals(aggregate)) {
    ServiceMap.logError(request, response, 400, "fromResult not available when aggregating results");
    return;  
  }

  if(toTime!=null) {
    if(!toTime.matches("^\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d$")) {
      ServiceMap.logError(request, response, 400, "invalid 'toTime' parameter, expected yyyy-mm-ddThh:mm:ss");
      return;
    }
  }  

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
  } else if(!fromTime.matches("^\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d$")) {
    ServiceMap.logError(request, response, 400, "invalid 'fromTime' parameter, expected n-day,n-hour,n-minute or yyyy-mm-ddThh:mm:ss");
    return;
  }
  
  IoTSearchApi iotSearchApi = new IoTSearchApi();
  response.setContentType("application/json; charset=UTF-8");

  String fullCount = request.getParameter("fullCount");
  if (!"false".equals(fullCount)) {
    fullCount = "true";
  }

  categories = ServiceMap.cleanCategories(categories);

  String lang = request.getParameter("lang");
  if (lang == null || (!lang.equals("it") && !lang.equals("en"))) {
    lang = "en";
  }

  String ip = ServiceMap.getClientIpAddress(request);
  String ua = request.getHeader("User-Agent");
  String reqFrom = request.getParameter("requestFrom");
  String requestType = "api";
  String format = request.getParameter("format");
  if (format == null) {
    format = "json";
  }

  if (!ServiceMap.checkIP(ip, requestType)) {
    ServiceMap.logError(request, response, 403, "API calls daily limit reached");
    return;
  }

  RepositoryConnection con = null;
  try {
    String[] coords = null;
    if (selection.startsWith("http:")) {
      String msg;
      if ((msg = CheckParameters.checkUri(selection)) != null) {
        response.sendError(400, "invalid uri " + msg);
        return;
      }

      con = ServiceMap.getSparqlConnection();
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
      if (resultCoord.hasNext()) {
        BindingSet bindingSetCoord = resultCoord.next();
        selection = bindingSetCoord.getValue("lat").stringValue() + ";" + bindingSetCoord.getValue("long").stringValue();
      } else {
        response.sendError(400, "invalid serviceuri " + selection + " no geo point");
        return;
      }
    }
    if (selection.startsWith("wkt:") || selection.startsWith("geo:") || selection.startsWith("graph:")) {
        response.sendError(400, "selection type wkt, geo and graph are not supported ");
    } else if (selection.contains(";")) {
      coords = selection.split(";");
      //check are all double
      for (int i = 0; i < coords.length; i++) {
        Double.parseDouble(coords[i]);
      }
    }
    // get services by lat/long
    if (coords == null || coords.length == 2 || coords.length == 4 ) {
      try {
        int results = iotSearchApi.iotSearchOverTimeRange(out, coords, suris, categories, model, maxDists, valueFilters, u, fromResult, maxResults, values, sortOnValue, text, fromTime, toTime, aggregate);
        ServiceMap.updateResultsPerIP(ip, requestType, results);
        ServiceMap.logAccess(request, null, selection, categories, null, "api-iot-search-time-range", maxResults, maxDists, null, null, "json", null, reqFrom);
      } catch (IllegalArgumentException e) {
        response.sendError(400, e.getMessage());
      }
    }
  } catch (Exception e) {
    ServiceMap.notifyException(e);
    throw e;
  } finally {
    if (con != null) {
      con.close();
    }
  }
%>