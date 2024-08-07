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

package org.disit.servicemap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javax.servlet.jsp.JspWriter;
import static org.disit.servicemap.ServiceMap.logQuery;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

/**
 *
 * @author bellini
 */
public class ServiceMapping {
  static private volatile ServiceMapping instance = null;
  
  static public ServiceMapping getInstance() throws Exception {
    if(instance==null) {
      synchronized(ServiceMapping.class) {
        if(instance==null) {
          instance = new ServiceMapping();
          instance.loadFromDB();
        }
      }
    }
    return instance;
  }
  
  static public void reset() {
    instance = null;
  }
  
  public class MappingData {
    public String serviceType;
    public int version;
    public String section;
    public String detailsQuery;
    public String attributesQuery;
    public String realTimeSparqlQuery;
    public String realTimeSqlQuery;
    public String realTimeSolrQuery;
    public String predictionSqlQuery;
    public String trendSqlQuery;
    
    public MappingData(String st, int v, String s,String d, String a, String rt, String sqlrt, String solrrt, String p, String pt) {
      serviceType = st;
      version = v;
      section = s;
      detailsQuery = d;
      attributesQuery = a;
      realTimeSparqlQuery = rt;
      realTimeSqlQuery = sqlrt;
      realTimeSolrQuery = solrrt;
      predictionSqlQuery = p;
      trendSqlQuery = pt;
    }
    
    public JsonObject getServiceAttributes(RepositoryConnection con, String serviceUri) throws Exception {
      //adds all attributes info
      String attrQuery = this.attributesQuery;
      if (attrQuery == null) {
        attrQuery = "select ?value_name ?data_type (replace(str(?vt),\"http://www.disit.org/km4city/resource/value_type/\",\"\") as ?value_type) (replace(str(?type),\"http://www.disit.org/km4city/schema#\",\"\") as ?attr_type) ?value_unit ?healthiness_criteria ?value_refresh_rate ?value_bounds ?different_values ?realtime {\n" +
            " <%SERVICE_URI> km4c:hasAttribute ?a.\n" +
            " ?a a ?type.\n" +
            " ?a km4c:value_name ?value_name.\n" +
            " ?a km4c:data_type ?data_type.\n" +
            " optional{?a km4c:value_type ?vt.}\n" +
            " ?a km4c:value_unit ?value_unit.\n" +
            " optional {?a km4c:order ?o}\n" +
            " optional {?a km4c:healthiness_criteria ?healthiness_criteria.}\n" +
            " optional {?a km4c:value_refresh_rate ?value_refresh_rate}\n" +
            " optional {?a km4c:value_bounds ?value_bounds}\n" +
            " optional {?a km4c:different_values ?different_values}\n" +
            " optional {?a km4c:realtime ?realtime}\n" +
            " } order by ?o";
      }
      
      Configuration conf = Configuration.getInstance();
      String sparqlType = conf.get("sparqlType", "virtuoso");

      attrQuery = attrQuery.replace("%SERVICE_URI", getServiceUriAlias(serviceUri));
      TupleQuery tupleQueryAttrs = con.prepareTupleQuery(QueryLanguage.SPARQL, attrQuery);
      ServiceMap.println("attrQuery:" + attrQuery);
      long ts = System.nanoTime();
      TupleQueryResult resultAttrs = tupleQueryAttrs.evaluate();
      logQuery(attrQuery, "API-service-attrs", sparqlType, serviceUri, System.nanoTime() - ts);
      List<String> bnames = resultAttrs.getBindingNames();

      JsonObject r = new JsonObject();
      while (resultAttrs.hasNext()) {
        BindingSet bs = resultAttrs.next();

        String name = bs.getBinding(bnames.get(0)).getValue().stringValue();
        JsonObject o = new JsonObject();
        for (int i=1; i<bnames.size(); i++) {
          String n = bnames.get(i);
          if(bs.getBinding(n) != null) {
            String v = bs.getBinding(n).getValue().stringValue();
            o.addProperty(n, v);
          }
        }
        r.add(name, o);
      }
      return r;
    }
    
    public JsonObject printServiceAttributes(JspWriter out, RepositoryConnection con, String serviceUri) throws Exception {
      //adds all attributes info
      JsonObject attrs = getServiceAttributes(con, serviceUri);
      out.println("    \"realtimeAttributes\":"+attrs+",");
      return attrs;
    }
  }
  
  private ArrayList<Map<String,MappingData>> maps;
  private Map<String,String> aliases;
  
  private ServiceMapping() {
    maps = new ArrayList<>();
    for(int i=0; i<2; i++)
      maps.add(new LinkedHashMap<>());
    aliases = new TreeMap<>();
  }
  
  private void loadFromDB() throws Exception {
    Connection conMySQL = null;
    Statement st = null;
    ResultSet rs = null;

    conMySQL = ConnectionPool.getConnection();
    try {
      String query = "SELECT * FROM ServiceMapping ORDER BY priority";
      st = conMySQL.createStatement();
      rs = st.executeQuery(query);
      while (rs.next()) {
          String serviceType = rs.getString("serviceType");
          String section = rs.getString("section");
          String detailsQuery = rs.getString("serviceDetailsSparqlQuery");
          String attributesQuery = rs.getString("serviceAttributesSparqlQuery");
          String realTimeQuery = rs.getString("serviceRealTimeSparqlQuery");
          String realTimeSqlQuery = rs.getString("serviceRealTimeSqlQuery");
          String realTimeSolrQuery = rs.getString("serviceRealTimeSolrQuery");
          String predictionSqlQuery = rs.getString("servicePredictionSqlQuery");
          String trendSqlQuery = rs.getString("serviceTrendSqlQuery");
          int version = rs.getInt("apiVersion");
          maps.get(version-1).put(serviceType, new MappingData(serviceType, version, section,
                  detailsQuery, 
                  attributesQuery, 
                  realTimeQuery, 
                  realTimeSqlQuery, 
                  realTimeSolrQuery, 
                  predictionSqlQuery, 
                  trendSqlQuery));
      }
      rs.close();
      st.close();
      
      st = conMySQL.createStatement();
      rs = st.executeQuery("SELECT * FROM ServiceAlias");
      while (rs.next()) {
          String serviceUri = rs.getString("serviceUri");
          String serviceUriAlias = rs.getString("serviceUriAlias");
          aliases.put(serviceUri,serviceUriAlias);
      }
      rs.close();
      st.close();
    } finally {
      conMySQL.close();
    }
  }
  
  public MappingData getMappingForServiceType(int version, List<String> types) {
    if(version<1)
      return null;
    for(Map.Entry<String,MappingData> e:maps.get(version-1).entrySet()) {
      String[] tt = e.getKey().split(";");
      //check if all mapping types are in the types
      boolean allIn = true;
      for(String x:tt) {
        if(!types.contains(x)) {
          allIn = false;
          break;
        }
      }
      if(allIn) {
        return e.getValue();
      }
    }
    return getMappingForServiceType(version-1, types);    
  }

  public MappingData getMappingForServiceType(int version, String type) {
    if(version<1)
      return null;
    for(Map.Entry<String,MappingData> e:maps.get(version-1).entrySet()) {
      if(type.equals(e.getKey()))
        return e.getValue();
    }
    return getMappingForServiceType(version-1, type);    
  }

  public String asHtml() {
    String html = "";
    html+="<table border=\"1\" cellspacing=\"0\">";
    html += "<tr><th>Class</th><th>Attributes</th><th>details</th><th>RT SOLR</th><th>RT SPARQL</th><th>RT SQL</th></tr>";
    for(Map.Entry<String,MappingData> e:maps.get(0).entrySet()) {
      MappingData md=e.getValue();
      html += "<tr><td>"+e.getKey()+
              "</td><td>"+md.attributesQuery+
              "</td><td>"+md.detailsQuery+
              "</td><td>"+md.realTimeSolrQuery+
              "</td><td>"+md.realTimeSparqlQuery+
              "</td><td>"+md.realTimeSqlQuery+
              "</td></tr>"
              ;
    }
    html+="</table><br>";
    html+="<table border=\"1\" cellspacing=\"0\">";
    html += "<tr><th>serviceUri</th><th>alias</th></tr>";
    for(Map.Entry<String,String> e:aliases.entrySet()) {
      html += "<tr><td>"+e.getKey()+
              "</td><td>"+e.getValue()+
              "</td></tr>"
              ;
    }
    html+="</table>";
    return html;
  }
  
  public String getServiceUriAlias(String serviceUri) {
    String suri = aliases.get(serviceUri);
    if(suri == null)
      return serviceUri;
    ServiceMap.println("suri_alias: "+serviceUri+"-->"+suri);
    return suri;
  }
}
