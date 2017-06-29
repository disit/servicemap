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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
    public String detailsQuery;
    public String realTimeSparqlQuery;
    public String realTimeSqlQuery;
    public String predictionSqlQuery;
    public String trendSqlQuery;
    public MappingData(String d, String rt, String sqlrt, String p, String pt) {
      detailsQuery = d;
      realTimeSparqlQuery = rt;
      realTimeSqlQuery = sqlrt;
      predictionSqlQuery = p;
      trendSqlQuery = pt;
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

    String query = "SELECT * FROM ServiceMap.ServiceMapping ORDER BY priority";

    st = conMySQL.createStatement();

    rs = st.executeQuery(query);
    while (rs.next()) {
        String serviceType = rs.getString("serviceType");
        String detailsQuery = rs.getString("serviceDetailsSparqlQuery");
        String realTimeQuery = rs.getString("serviceRealTimeSparqlQuery");
        String realTimeSqlQuery = rs.getString("serviceRealTimeSqlQuery");
        String predictionSqlQuery = rs.getString("servicePredictionSqlQuery");
        String trendSqlQuery = rs.getString("serviceTrendSqlQuery");
        int version = rs.getInt("apiVersion");
        maps.get(version-1).put(serviceType, new MappingData(detailsQuery, realTimeQuery, realTimeSqlQuery, predictionSqlQuery, trendSqlQuery));
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
}
