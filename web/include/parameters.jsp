<%@page import="org.json.simple.JSONObject"%>
<%@page import="org.disit.servicemap.ConnectionPool"%>
<%@page import="org.disit.servicemap.ServiceMap"%>
<%@page trimDirectiveWhitespaces="true" %>
<%@page import="java.nio.charset.Charset"%>
<%@page import="java.io.IOException" %>
<%@page import="org.openrdf.model.Value"%>
<%@page import="java.util.*"%>
<%@page import="org.apache.http.HttpResponse"%>
<%@page import="org.apache.http.client.HttpClient"%>
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
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.io.*"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="org.springframework.web.context.request.ServletRequestAttributes"%>
<%@page import="org.springframework.web.context.request.RequestContextHolder"%>
<%@page import="org.disit.servicemap.Configuration"%>
<%!
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

    static Configuration conf = Configuration.getInstance();
    String urlMySqlDB = conf.getSet("urlMySqlDB", "jdbc:mysql://localhost:3306/");
    String dbMySql = conf.getSet("dbMySql","ServiceMap");
    String driverMysql = conf.getSet("driverMysql","com.mysql.jdbc.Driver");
    String userMySql = conf.getSet("userMySql","user");
    String passMySql = conf.getSet("passMySql","password");
    String baseApiUri = conf.getSet("baseApiUrl","http://localhost:8080/ServiceMap/api/");

    String logEndPoint = conf.getSet("logEndPoint","http://log.disit.org/service/?sparql=http://localhost:8080/ServiceMap/sparql&uri=");
    String mailFrom = conf.getSet("mailFrom","servicemap@mail.org");
    String smtp = conf.getSet("smtp","");
    String portSmtp = conf.getSet("portSmtp","25");
    String accessLogFile = conf.getSet("accessLogFile", "servicemap-access.log");
    String sparqlEndpoint = conf.get("sparqlEndpoint","http://localhost:8890/sparql");
    String virtuosoEndpoint = conf.get("virtuosoEndpoint","jdbc:virtuoso://localhost:1111");
    static String sparqlType = conf.getSet("sparqlType","virtuoso");
    static int maxTime = 3*60*(sparqlType.equals("virtuoso")?1000:1); //3 min
    static String km4cVersion = conf.getSet("km4cVersion","new");
    String gaCode = conf.get("gaCode", "");
    int clusterResults = Integer.parseInt(conf.get("clusterResults","4000"));
    int clusterDistance = Integer.parseInt(conf.get("clusterDistance","40"));
    int noClusterAtZoom = Integer.parseInt(conf.get("noClusterAtZoom","17"));
    String mapAccessToken = conf.get("mapAccessToken","...map token...");
    String check_send_to = conf.get("check_send_to","");
    int avm_max_delay = Integer.parseInt(conf.get("avm_max_delay","30"));
    int meteo_max_delay = Integer.parseInt(conf.get("meteo_max_delay","1440"));
    int parking_max_delay = Integer.parseInt(conf.get("parking_max_delay","30"));
    int sensor_max_delay = Integer.parseInt(conf.get("sensor_max_delay","60"));
    String enable_road_ftsearch = conf.get("enable_road_ftsearch","true");

%>

<%!
    private static String escapeURI(final String query) {
        if(query==null)
          return null;
        return query.replace("%", "%25").replace(" ", "%20").replace(":", "%3A").replace("\"", "%22").replace("#", "%23").replace("<", "%3C").replace(">", "%3E").replace("'", "%27").replace("^", "%5E").replace("{", "%7B").replace("}", "%7D").replace(";", "%3B").replace(",", "%2C").replace("[", "%5B").replace("]", "%5D");
    }
    private static String unescapeUri(final String query) {
        if(query==null)
          return null;
        return query.replace("%20", " ").replace("%3B", ";").replace("%3A", ":").replace("%2C", ",").replace("%5B", "[").replace("%5D", "]").replace("%22", "\"").replace("%23", "#").replace("%3C", "<").replace("%3E", ">").replace("%5E", "^").replace("%7B", "{").replace("%7D", "}").replace("%25", "%").replace("%27", "'");
    }
    
    private void logAccess(String ip, String email, String UA, String sel, String categorie, String serviceUri, String mode, String numeroRisultati, String raggio, String queryId, String text, String format, String uid, String reqFrom) throws IOException, SQLException {
      ServiceMap.logAccess(ip, email, UA, sel, categorie, serviceUri, mode, numeroRisultati, raggio, queryId, text, format, uid, reqFrom);
    }
    
    private String sqlValue(Object x) {
      if(x==null)
        return "NULL";
      if(x instanceof String)
        return "'"+x+"'";
      return x.toString();
    }
    
    private String escapeJSON(String s) {
      return JSONObject.escape(s); //s.replace("\"", "\\\"").replace("\t", "\\t");
    }
    
    private String filterQuery(String s) {
      if(!km4cVersion.equals("old"))
        return s.replace("^^xsd:string", "");
      return s;
    }
%>
<%!
  public String filterServices(List<String> listaCategorie) throws Exception {      
    return ServiceMap.filterServices(listaCategorie);
  }
  
  public void logQuery(String query, String id, String type, String args) {
    ServiceMap.logQuery(query, id, type, args, -1);
  }
  public void logQuery(String query, String id, String type) {
    ServiceMap.logQuery(query, id, type, "", -1);
  }
  public void logQuery(String query, String id, String type, long time) {
    ServiceMap.logQuery(query, id, type, "", time);
  }
  public void logQuery(String query, String id, String type, String args, long time) {
    ServiceMap.logQuery(query, id, type, args, time);
  }
%>
