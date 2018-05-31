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
  static private ServiceMapping instance = null;
  
  static public ServiceMapping getInstance() throws Exception {
    if(instance==null) {
      instance = new ServiceMapping();
      instance.loadFromDB();
    }
    return instance;
  }
  
  static public void reset() {
    instance = null;
  }
  
  public class MappingData {
    public String section;
    public String detailsQuery;
    public String attributesQuery;
    public String realTimeSparqlQuery;
    public String realTimeSqlQuery;
    public String realTimeSolrQuery;
    public String predictionSqlQuery;
    public String trendSqlQuery;
    
    public MappingData(String s,String d, String a, String rt, String sqlrt, String solrrt, String p, String pt) {
      section = s;
      detailsQuery = d;
      attributesQuery = a;
      realTimeSparqlQuery = rt;
      realTimeSqlQuery = sqlrt;
      realTimeSolrQuery = solrrt;
      predictionSqlQuery = p;
      trendSqlQuery = pt;
    }
    
    public JsonObject printServiceAttributes(JspWriter out, RepositoryConnection con, String serviceUri) throws Exception {
      //adds all attributes info
      String attrQuery = this.attributesQuery;
      if (attrQuery == null) {
        return null;
      }
      Configuration conf = Configuration.getInstance();
      String sparqlType = conf.get("sparqlType", "virtuoso");

      attrQuery = attrQuery.replace("%SERVICE_URI", serviceUri);
      TupleQuery tupleQueryAttrs = con.prepareTupleQuery(QueryLanguage.SPARQL, attrQuery);
      ServiceMap.println("attrQuery:" + attrQuery);
      long ts = System.nanoTime();
      TupleQueryResult resultAttrs = tupleQueryAttrs.evaluate();
      logQuery(attrQuery, "API-service-attrs", sparqlType, serviceUri, System.nanoTime() - ts);
      List<String> bnames = resultAttrs.getBindingNames();

      JsonObject r = new JsonObject();
      out.println("    \"realtimeAttributes\":{");
      int pp = 0;
      while (resultAttrs.hasNext()) {
        BindingSet bs = resultAttrs.next();

        if (pp != 0) {
          out.println(",");
        } 

        int ppp = 0;
        String name = bs.getBinding(bnames.get(0)).getValue().stringValue();
        out.print("      \""+name+"\":{");
        JsonObject o = new JsonObject();
        for (int i=1; i<bnames.size(); i++) {
          String n = bnames.get(i);
          if(bs.getBinding(n) != null) {
            String v = bs.getBinding(n).getValue().stringValue();
            if (ppp != 0) {
              out.print(",");
            }
            out.print("\"" + n + "\":\"" + v + "\"");
            o.addProperty(n, v);
            ppp++;
          }
        }
        r.add(name, o);
        out.print("}");
        pp++;
      }
      out.println("},");
      return r;
    }
  }
  
  private ArrayList<Map<String,MappingData>> maps;
  
  private ServiceMapping() {
    maps = new ArrayList<>();
    for(int i=0; i<2; i++)
      maps.add(new LinkedHashMap<String,MappingData>());
  }
  
  private void loadFromDB() throws Exception {
    Connection conMySQL = null;
    Statement st = null;
    ResultSet rs = null;

    conMySQL = ConnectionPool.getConnection();

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
        maps.get(version-1).put(serviceType, new MappingData(section,
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
    conMySQL.close();
  }
  
  public MappingData getMappingForServiceType(int version, List<String> types) {
    if(version<1)
      return null;
    for(Map.Entry<String,MappingData> e:maps.get(version-1).entrySet()) {
      if(types.contains(e.getKey()))
        return e.getValue();
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
    html+="<table>";
    html += "<tr><td>Class</td><td>attributes</td><td>details</td><td>RT SOLR</td><td>RT SPARQL</td><td>RT SQL</td></tr>";
    for(String key:maps.get(0).keySet()) {
      MappingData md=maps.get(0).get(key);
      html += "<tr><td>"+key+
              "</td><td>"+md.attributesQuery+
              "</td><td>"+md.detailsQuery+
              "</td><td>"+md.realTimeSolrQuery+
              "</td><td>"+md.realTimeSparqlQuery+
              "</td><td>"+md.realTimeSqlQuery+
              "</td></tr>"
              ;
    }
    html+="</table>";
    return html;
  }
}
