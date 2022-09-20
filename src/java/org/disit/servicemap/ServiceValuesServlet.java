/* ServiceMap.
   Copyright (C) 2019 DISIT Lab http://www.disit.org - University of Florence

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

/* TABLES

CREATE TABLE ServiceDataTrends (
 serviceUri VARCHAR(255) NOT NULL,
 valueName VARCHAR(50) NOT NULL,
 trendType VARCHAR(20) NOT NULL,
 timeAggregation VARCHAR(20) NOT NULL,
 hour VARCHAR(5) NOT NULL,
 "value" VARCHAR(20) DEFAULT NULL,
 CONSTRAINT pk PRIMARY KEY (serviceUri, valueName, trendType, timeAggregation, hour)
)

CREATE TABLE ServiceDataValues (
 serviceUri VARCHAR(255) NOT NULL,
 valueName VARCHAR(50) NOT NULL,
 valueDate TIME NOT NULL,
 valueAcqDate TIME NOT NULL,
 "value" VARCHAR(20) DEFAULT NULL,
 CONSTRAINT pk PRIMARY KEY (serviceUri, valueName, valueDate, valueAcqDate)
)
*/

package org.disit.servicemap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.management.Query.attr;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import org.disit.servicemap.JwtUtil.User;
import org.disit.servicemap.api.CheckParameters;
import org.disit.servicemap.api.ServiceMapApiV1;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.repository.RepositoryConnection;

@WebServlet("/api/v1/values/*")
public class ServiceValuesServlet extends HttpServlet {
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    User user = checkAccess(request, response, true);
    if(user==null) 
      return;

    String ip = ServiceMap.getClientIpAddress(request);
    try {
      if(! ServiceMap.checkIP(ip, "api")) {
        ServiceMap.logError(request, response, 403, "API calls daily limit reached");
        return;
      }    
    } catch(Exception e) {
      ServiceMap.notifyException(e);
      return;
    }
    
    JsonObject obj;
    try {
      JsonParser parser = new JsonParser();
      InputStream inputStream = request.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream , StandardCharsets.UTF_8));
      obj = parser.parse(reader).getAsJsonObject();
    } catch (JsonIOException | JsonSyntaxException | IOException e) {
      e.printStackTrace();
      ServiceMap.logError(request, response, 400, "invalid JSON object");
      return;
    }
    String path = request.getPathInfo();
    if(path==null)
      path="/";
    else if(!path.endsWith("/"))
      path += "/";
    
    String uid = request.getParameter("uid");
    if(uid!=null && !ServiceMap.validateUID(uid)) {
      ServiceMap.logError(request, response, 404, "invalid uid");
      return;
    }
    if(uid==null) {
      uid = user.username;
    }
    String reqFrom = request.getParameter("requestFrom");
    
    try {
      switch (path) {
        case "/register/":
          if(registerValues(obj, request, response)) {
            ServiceMap.updateResultsPerIP(ip, "api", 1);
            ServiceMap.logAccess(request, null, "", null, obj.get("serviceUris").getAsString(), "api-values-register", null, null, null, null, "json", uid, reqFrom);
          } break;
        case "/":
          //save data to phoenix
          if(saveValues(obj, request, response)) {
            ServiceMap.updateResultsPerIP(ip, "api", 1);
            ServiceMap.logAccess(request, null, "", null, obj.get("serviceUri").getAsString(), "api-values-post", null, null, null, null, "json", uid, reqFrom);
          } break;
        case "/trends/":
          if(saveTrends(obj, request, response)) {
            ServiceMap.updateResultsPerIP(ip, "api", 1);
            ServiceMap.logAccess(request, null, "", null, obj.get("serviceUri").getAsString(), "api-values-trends", null, null, null, null, "json", uid, reqFrom);
          } break;
        case "/typicaltrends/":
          if(saveTypicalTrends(obj, request, response)) {
            ServiceMap.updateResultsPerIP(ip, "api", 1);
            ServiceMap.logAccess(request, null, "", null, obj.get("serviceUri").getAsString(), "api-values-typicaltrends", null, null, null, null, "json", uid, reqFrom);
          } break;
        default:
          ServiceMap.logError(request, response, 400, "invalid path of request");
          break;
      }
    } catch(Exception e) {
      ServiceMap.notifyException(e);
      ServiceMap.logError(request, response, 500, "error");
    }
  }

  private boolean registerValues(JsonObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
    //check attributes
    JsonElement serviceUris = obj.get("serviceUris");
    if (serviceUris == null || !serviceUris.isJsonArray()) {
      ServiceMap.logError(request, response, 400, "invalid object: missing serviceUris array");
      return false;
    }
    String apikey = request.getParameter("apikey");
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    try {
      for (JsonElement s : serviceUris.getAsJsonArray()) {
        if (!s.isJsonPrimitive()) {
          ServiceMap.logError(request, response, 400, "invalid object: serviceUri not valid");
          return false;
        }
        ArrayList<String> types = ServiceMap.getTypes(con, s.getAsString(), apikey);
        if (types.isEmpty()) {
          ServiceMap.logError(request, response, 400, "invalid object: serviceUri not not found " + s);
          return false;
        }
      }
      JsonElement attributes = obj.get("attributes");
      if (attributes == null || !attributes.isJsonArray()) {
        ServiceMap.logError(request, response, 400, "invalid object: missing attributes array");
        return false;
      }
      for (JsonElement a : attributes.getAsJsonArray()) {
        if (!a.isJsonObject()) {
          ServiceMap.logError(request, response, 400, "invalid object: attribute no object");
          return false;
        }
        if (!a.getAsJsonObject().has("valueName")) {
          ServiceMap.logError(request, response, 400, "invalid object: attribute with no valueName");
          return false;
        }
        if (!a.getAsJsonObject().has("dataType")) {
          ServiceMap.logError(request, response, 400, "invalid object: attribute with no dataType");
          return false;
        }
      }

      //save data
      Connection conMySQL = ConnectionPool.getConnection();
      try {
        for (JsonElement s : serviceUris.getAsJsonArray()) {
          String serviceUri = s.getAsString();
          for (JsonElement a : attributes.getAsJsonArray()) {
            JsonObject attr = a.getAsJsonObject();
            String valueName = attr.get("valueName").getAsString();
            String valueUnit = attr.has("valueUnit") ? attr.get("valueUnit").getAsString() : null ;
            String valueType = attr.has("valueType") ? attr.get("valueType").getAsString() : null ;
            String dataType = attr.has("dataType") ? attr.get("dataType").getAsString() : null ;
            String refresh_rate = attr.has("value_refresh_rate") ? attr.get("value_refresh_rate").getAsString() : null ;
            String different_values = attr.has("different_values") ? attr.get("different_values").getAsString() : null ;
            String value_bounds = attr.has("value_bounds") ? attr.get("value_bounds").getAsString() : null ;
            if("null".equals(valueUnit))
              valueUnit=null;
            if("null".equals(valueType))
              valueType=null;
            if("null".equals(dataType))
              dataType=null;
            if("null".equals(refresh_rate))
              refresh_rate=null;
            if("null".equals(different_values))
              different_values=null;
            if("null".equals(value_bounds))
              value_bounds=null;
                    
            String query = "REPLACE INTO ServiceValues(serviceUri,valueName,valueUnit,valueType,dataType,refresh_rate,different_values,value_bounds) VALUES "
                    + "(?,?,?,?,?,?,?,?)";
            PreparedStatement st = conMySQL.prepareStatement(query);
            st.setString(1, serviceUri);
            st.setString(2, valueName);
            st.setString(3, valueUnit);
            st.setString(4, valueType);
            st.setString(5, dataType);
            st.setString(6, refresh_rate);
            st.setString(7, different_values);
            st.setString(8, value_bounds);

            st.executeUpdate();
            st.close();
            
            //save value types on KB
            String healthiness = (refresh_rate!=null ? "  km4c:value_refresh_rate \""+refresh_rate+"\";\n" : "") +
                    (different_values!=null ? "  km4c:different_values \""+different_values+"\";\n" : "") +
                    (value_bounds!=null ? "  km4c:value_bounds \""+value_bounds+"\";\n" : "");
            if(!healthiness.isEmpty())
              healthiness = "  km4c:healthiness_criteria \"any\"; \n"+healthiness;
            
            query = "PREFIX sosa:<http://www.w3.org/ns/sosa/>\n" +
                "PREFIX km4cvt:<http://www.disit.org/km4city/resource/value_type/>\n" +
                "WITH <urn:snap4city:values>\n"+
                  "DELETE {" + 
                  "<"+serviceUri+"> km4c:hasAttribute <"+serviceUri+"/"+valueName+">.\n" +
                  "<"+serviceUri+"/"+valueName+"> ?p ?o" +
                  "} WHERE {" +
                  "<"+serviceUri+"/"+valueName+"> ?p ?o" +
                  "}INSERT {\n" +
                  "<"+serviceUri+"> km4c:hasAttribute <"+serviceUri+"/"+valueName+">.\n" +
                  "<"+serviceUri+"/"+valueName+"> a km4c:CustomAttribute;\n" +
                  "  km4c:value_name\""+valueName+"\";\n"+
                  (valueUnit!=null ? "  km4c:value_unit \""+valueUnit+"\"; \n" : "") +
                  (valueType!=null ?"  km4c:value_type km4cvt:"+valueType+";\n" : "") +
                  healthiness +
                  "  km4c:data_type \""+dataType+"\".\n" +
                "}";
                 
            Update stm = con.prepareUpdate(QueryLanguage.SPARQL, query);
            ServiceMap.println(query);
            stm.execute();
          }
        }
      } finally {
        conMySQL.close();
      }
    } finally {
      con.close();
    }
    return true;
  }
  
  private JsonObject getRegisterValues(String serviceUri) throws Exception {
    JsonObject r = new JsonObject();
    Connection conMySQL = ConnectionPool.getConnection();
    try {                   
      String query = "SELECT valueName,valueUnit,dataType,valueType,refresh_rate,different_values,value_bounds FROM ServiceValues WHERE serviceUri=?";
      PreparedStatement st = conMySQL.prepareStatement(query);
      st.setString(1, serviceUri);

      ResultSet rs = st.executeQuery();
      while(rs.next()) {
        String valueName = rs.getString("valueName");
        JsonObject a = new JsonObject();
        a.addProperty("valueUnit", rs.getString("valueUnit"));
        a.addProperty("valueType", rs.getString("valueType"));
        a.addProperty("dataType", rs.getString("dataType"));
        a.addProperty("value_refresh_rate", rs.getString("refresh_rate"));
        a.addProperty("different_values", rs.getString("different_values"));
        a.addProperty("value_bounds", rs.getString("value_bounds"));
        r.add(valueName, a);
      }
      st.close();
    } finally {
      conMySQL.close();
    }
    return r;
  }

  private boolean saveValues(JsonObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
    JsonElement serviceUri = obj.get("serviceUri");
    if (serviceUri == null || !serviceUri.isJsonPrimitive()) {      
      ServiceMap.logError(request, response, 400, "invalid object: missing serviceUri");
      return false;
    }
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    try {
      ArrayList<String> types = ServiceMap.getTypes(con, serviceUri.getAsString(), request.getParameter("apikey"));
      if (types.isEmpty()) {
        ServiceMap.logError(request, response, 400, "invalid object: serviceUri not not found " + serviceUri.getAsString());
        return false;
      }
      JsonObject attrs = getRegisterValues(serviceUri.getAsString());
      
      JsonElement attributes = obj.get("attributes");
      if (attributes == null || (!attributes.isJsonArray() && !attributes.isJsonObject())) {
        ServiceMap.logError(request, response, 400, "invalid object: missing attributes array");
        return false;
      }
      if(attributes.isJsonObject()) {
        JsonArray a=new JsonArray();
        a.add(attributes);
        attributes = a;
      }
      for (JsonElement a : attributes.getAsJsonArray()) {
        if (!a.isJsonObject()) {
          ServiceMap.logError(request, response, 400, "invalid object: attribute no object");
          return false;
        }
        if (!a.getAsJsonObject().has("valueName")) {
          ServiceMap.logError(request, response, 400, "invalid object: attribute with no valueName");
          return false;
        }
        String valueName = a.getAsJsonObject().get("valueName").getAsString();
        if(!attrs.has(valueName)) {
          ServiceMap.logError(request, response, 400, "invalid object: valueName "+valueName+" not registered for "+serviceUri.getAsString());
          return false;          
        }
        if (!a.getAsJsonObject().has("value")) {
          ServiceMap.logError(request, response, 400, "invalid object: attribute with no value");
          return false;
        }
        if (!a.getAsJsonObject().has("valueDate")) {
          ServiceMap.logError(request, response, 400, "invalid object: attribute with no valueDate");
          return false;
        }
        String vd = a.getAsJsonObject().get("valueDate").getAsString();
        try {
          long d = df.parse(vd).getTime();
        } catch(ParseException e) {
          ServiceMap.logError(request, response, 400, "invalid valueDate: "+vd);
          return false;
        }
        if(a.getAsJsonObject().has("valueAcqDate")) {
          String vad = a.getAsJsonObject().get("valueAcqDate").getAsString();
          try {
            long d = df.parse(vad).getTime();
          } catch(ParseException e) {
            ServiceMap.logError(request, response, 400, "invalid valueAcqDate: "+vad);
            return false;
          }
        }
      }
      Connection conn = ServiceMap.getRTConnection();
      if(conn==null) {
        ServiceMap.logError(request, response, 500, "failed hbase phoenix connection");
        return false;
        //throw new Exception("missing hbase phoenix connection");
      }
      try {
        for(JsonElement a : attributes.getAsJsonArray()) {
          JsonObject attr = a.getAsJsonObject();
          String valueName = attr.get("valueName").getAsString();
          String value = attr.get("value").getAsString();
          String valueDate = attr.get("valueDate").getAsString();
          String valueAcqDate = attr.has("valueAcqDate") ? attr.get("valueAcqDate").getAsString() : null;
          Date acqDate, date;
          if(valueAcqDate == null) {
            acqDate = new Date(new java.util.Date().getTime());
          } else {
            acqDate = new Date(df.parse(valueAcqDate).getTime());
          }
          date = new Date(df.parse(valueDate).getTime());

          ServiceMap.println(serviceUri+" "+valueName);
          String query = "UPSERT INTO ServiceDataValues(serviceUri,valueName,\"value\",valueDate, valueAcqDate) VALUES (?,?,?,?,?)";
          PreparedStatement st = conn.prepareStatement(query);
          st.setString(1, serviceUri.getAsString());
          st.setString(2, valueName);
          st.setString(3, value);
          st.setDate(4, date);
          st.setDate(5, acqDate);

          st.executeUpdate();
          st.close();
        }
        conn.commit();
      } finally {
        conn.close();
      }
    } finally {
      con.close();
    }
    return true;
  }
  
/*
  saveTrends
  { 
    "serviceUri":"...",
    "valueNames":[ "vn1", "vn2" ],
    "trendType": "type of trend",
    "trends": {
      "timeAggregation1" : [ 
          ["hour1","value vn1","value vn2"],
          ["hour2","value vn1","value vn2"]
      ],
      "timeAggregation2" : [ 
          ["day1","value vn1","value vn2"],
          ["day2","value vn1","value vn2"]
      ],
      "timeAggregation3" : [ 
          ["month1","value vn1","value vn2"],
          ["month2","value vn1","value vn2"]
      ]
    }
  }
  */
  private boolean saveTrends(JsonObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
    JsonElement serviceUri = obj.get("serviceUri");
    if (serviceUri == null || !serviceUri.isJsonPrimitive()) {      
      ServiceMap.logError(request, response, 400, "invalid object: missing serviceUri");
      return false;
    }
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    try {
      ArrayList<String> types = ServiceMap.getTypes(con, serviceUri.getAsString(), request.getParameter("apikey"));
      if (types.isEmpty()) {
        ServiceMap.logError(request, response, 400, "invalid object: serviceUri not found " + serviceUri.getAsString());
        return false;
      }
      JsonObject attrs = getRegisterValues(serviceUri.getAsString());
      
      JsonElement valueNames = obj.get("valueNames");
      if (valueNames == null || !valueNames.isJsonArray()) {
        ServiceMap.logError(request, response, 400, "invalid object: missing valueNames array");
        return false;
      }
      for (JsonElement vn : valueNames.getAsJsonArray()) {
        if (!vn.isJsonPrimitive()) {
          ServiceMap.logError(request, response, 400, "invalid object: invalid valueName "+vn);
          return false;
        }
        //TBD check valueNames and all other data
      }
      
      JsonElement trendType = obj.get("trendType");
      if (trendType == null || !trendType.isJsonPrimitive()) {      
        ServiceMap.logError(request, response, 400, "invalid object: missing or invalid trendType");
        return false;
      }
      JsonElement trends = obj.get("trends");
      if (trends == null || !trends.isJsonObject()) {      
        ServiceMap.logError(request, response, 400, "invalid object: missing or invalid trends");
        return false;
      }
      
      Connection conn = ServiceMap.getRTConnection();
      if(conn==null) {
        //throw new Exception("missing hbase phoenix connection");
        ServiceMap.logError(request, response, 500, "failed hbase phoenix connection");
        return false;
      }
      
      try {
          for(Entry<String,JsonElement> e : trends.getAsJsonObject().entrySet()) {
            String timeAggregation = e.getKey();
            JsonArray values = e.getValue().getAsJsonArray();
            for(JsonElement v : values) {
              JsonArray d = v.getAsJsonArray();
              String hour = d.get(0).getAsString();
              for(int i=1; i<d.size(); i++) {
                String query = "UPSERT INTO ServiceDataTrends(serviceUri,valueName,trendType,timeAggregation,hour,\"value\") VALUES (?,?,?,?,?,?)";
                PreparedStatement st = conn.prepareStatement(query);
                st.setString(1, serviceUri.getAsString());
                st.setString(2, valueNames.getAsJsonArray().get(i-1).getAsString() );
                st.setString(3, trendType.getAsString());
                st.setString(4, timeAggregation);
                st.setString(5, hour);
                st.setString(6, d.get(i).getAsString());

                st.executeUpdate();
                st.close();
              }
            }
          }
        conn.commit();
      } finally {
        conn.close();
      }
    } finally {
      con.close();
    }
    return false;
  }

  /*
  {"serviceUri":"http://www.disit.org/km4city/resource/METRO11/averageSpeed","referenceDate":"2020-10-12","numberOfPeriods":4,"from":"2020-06-01","to":"2020-09-30","trendType":"monthWeek","computationType":"average","typicalMonthWeek":[59.245,59.6181,59.7886,59.8779,59.3241,59.1078,58.341,59.5112,59.5923,59.7581,59.5514,59.6383,59.2078,58.3709,59.5183,59.9054,59.8156,60.0388,59.4937,59.2385,58.0064,59.1914,59.9179,59.9104,60.1879,60.0105,59.0463,57.9984],"wrongValues":["In this period:  2020-06-01T00:00:00  -  2020-06-01T23:59:59  there aren't value","In this period:  2020-06-02T00:00:00  -  2020-06-02T23:59:59  there aren't value"]}
  {"serviceUri":"http://www.disit.org/km4city/resource/METRO11/averageSpeed","referenceDate":"2020-10-12","numberOfPeriods":4,"from":"2020-06-01","to":"2020-09-30","trendType":"monthDay","computationType":"average","typicalMonthD":[59.3766,59.2889,59.5879,59.3438,59.108,59.1622,59.3858,59.3301,59.3082,59.6467,59.7067,59.2473,58.6599,59.7313,59.5945,59.4743,59.8187,59.584,59.2144,59.4186,58.9122,59.0496,59.4828,59.7774,59.745,59.45,59.6638,59.0943,59.2296,59.555],"wrongValues":["In this period:  2020-06-01T00:00:00  -  2020-06-01T23:59:59  there aren't value","In this period:  2020-06-02T00:00:00  -  2020-06-02T23:59:59  there aren't value"]}
  {"serviceUri":"http://www.disit.org/km4city/resource/METRO11/averageSpeed","referenceDate":"2020-10-12","numberOfPeriods":4,"from":"2020-09-14","to":"2020-10-11","trendType":"dayHour","computationType":"average","typicalDays":{"Monday":[59.5147,59.2027,57.8108,57.8201,58.4649,59.3629,59.8118,59.7628,59.4238,60.1693,59.5446,59.7692,59.1127,60.2924,60.0773,59.2405,58.8973,59.2017,59.8434,60.1771,57.8881,60.755,59.9231,60.8436],"Tuesday":[59.5417,58.8056,58.6925,57.9875,58.8188,60.6736,60.2255,59.4792,59.5706,60.2524,60.1953,60.1028,60.8259,60.5001,61.2038,59.3917,58.3197,59.303,60.6453,59.6106,58.2928,60.6828,60.8476,58.7601],"Wednesday":[59.0765,57.8696,58.7438,58.0342,59.5169,59.8382,59.7351,59.9449,59.8324,59.7464,60.1544,59.9211,61.5111,60.1872,60.9436,59.3161,59.8752,58.7315,59.7803,59.8148,59.6681,59.4661,60.5263,58.443],"Thursday":[59.1717,58.1898,60.0123,58.7486,59.5051,61.6471,60.0252,59.6599,59.7022,60.1263,59.5155,59.8164,60.5717,61.3921,60.9367,59.7341,60.4072,60.6343,59.583,60.1405,59.0703,59.3992,59.6486,60.7156],"Friday":[59.2169,58.41,60.2465,58.6742,58.5522,61.6208,59.8275,59.7278,59.8798,59.6756,59.7387,59.6423,60.618,60.4007,60.6449,61.8301,60.2244,60.1264,59.5526,58.6165,59.0501,58.8322,58.7798,59.7891],"Saturday":[60.0545,58.3569,59.0589,57.9088,59.8703,62.223,59.4829,59.997,59.9904,60.0924,59.8907,59.7796,59.7481,59.6464,59.9748,60.0133,58.8221,58.5955,58.6722,58.454,59.0222,59.4719,59.3483,59.7503],"Sunday":[58.9455,58.899,58.4832,58.0381,59.0313,59.4299,59.1527,60.0107,59.8962,59.1515,59.775,59.5978,59.4485,60.6336,60.1931,59.9391,59.4553,58.9743,58.3323,58.6754,59.053,59.3311,58.8116,60.8708]},"wrongValues":{"mondayError":"No error for this trend","tuesdayError":"No error for this trend","wednesdayError":["In this period:  2020-09-30T12:00:00  -  2020-09-30T13:00:00  there aren't value","In this period:  2020-09-30T13:00:00  -  2020-09-30T14:00:00  there aren't value","In this period:  2020-09-30T14:00:00  -  2020-09-30T15:00:00  there aren't value","In this period:  2020-09-30T15:00:00  -  2020-09-30T16:00:00  there aren't value","In this period:  2020-09-30T16:00:00  -  2020-09-30T17:00:00  there aren't value","In this period:  2020-09-30T17:00:00  -  2020-09-30T18:00:00  there aren't value","In this period:  2020-09-30T18:00:00  -  2020-09-30T19:00:00  there aren't value","In this period:  2020-09-30T19:00:00  -  2020-09-30T20:00:00  there aren't value","In this period:  2020-09-30T20:00:00  -  2020-09-30T21:00:00  there aren't value","In this period:  2020-09-30T21:00:00  -  2020-09-30T22:00:00  there aren't value","In this period:  2020-09-30T22:00:00  -  2020-09-30T23:00:00  there aren't value","In this period:  2020-09-30T23:00:00  -  2020-09-30T23:59:59  there aren't value"],"thursdayError":["In this period:  2020-10-01T00:00:00  -  2020-10-01T01:00:00  there aren't value","In this period:  2020-10-01T01:00:00  -  2020-10-01T02:00:00  there aren't value","In this period:  2020-10-01T02:00:00  -  2020-10-01T03:00:00  there aren't value","In this period:  2020-10-01T03:00:00  -  2020-10-01T04:00:00  there aren't value","In this period:  2020-10-01T04:00:00  -  2020-10-01T05:00:00  there aren't value","In this period:  2020-10-01T05:00:00  -  2020-10-01T06:00:00  there aren't value","In this period:  2020-10-01T06:00:00  -  2020-10-01T07:00:00  there aren't value","In this period:  2020-10-01T07:00:00  -  2020-10-01T08:00:00  there aren't value","In this period:  2020-10-01T08:00:00  -  2020-10-01T09:00:00  there aren't value","In this period:  2020-10-01T09:00:00  -  2020-10-01T10:00:00  there aren't value"],"fridayError":["In this period:  2020-09-25T16:00:00  -  2020-09-25T17:00:00  there aren't value","In this period:  2020-09-25T17:00:00  -  2020-09-25T18:00:00  there aren't value","In this period:  2020-09-25T18:00:00  -  2020-09-25T19:00:00  there aren't value","In this period:  2020-09-25T19:00:00  -  2020-09-25T20:00:00  there aren't value","In this period:  2020-09-25T20:00:00  -  2020-09-25T21:00:00  there aren't value","In this period:  2020-09-25T21:00:00  -  2020-09-25T22:00:00  there aren't value"],"saturdayError":"No error for this trend","sundayError":"No error for this trend"}}
  */
  private boolean saveTypicalTrends(JsonObject obj, HttpServletRequest request, HttpServletResponse response) throws Exception {
    JsonElement serviceUri = obj.get("serviceUri");
    if (serviceUri == null || !serviceUri.isJsonPrimitive()) {      
      ServiceMap.logError(request, response, 400, "invalid object: missing serviceUri");
      return false;
    }
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    try {
      ArrayList<String> types = ServiceMap.getTypes(con, serviceUri.getAsString(), request.getParameter("apikey"));
      if (types.isEmpty()) {
        ServiceMap.logError(request, response, 400, "invalid object: serviceUri not found " + serviceUri.getAsString());
        return false;
      }
      JsonObject attrs = getRegisterValues(serviceUri.getAsString());

      JsonElement valueName = obj.get("valueName");
      if (valueName == null || !valueName.isJsonPrimitive()) {
        ServiceMap.logError(request, response, 400, "invalid object: missing valueName");
        return false;
      }

      JsonElement trendType = obj.get("trendType");
      if (trendType == null || !trendType.isJsonPrimitive()) {      
        ServiceMap.logError(request, response, 400, "invalid object: missing or invalid trendType");
        return false;
      }
      
      Configuration conf = Configuration.getInstance();
      User access = checkAccess(request, response, false);
      if(access==null) {
        ServiceMap.logError(request, response, 401, "invalid user");
        return false;
      } else if(!Arrays.asList(conf.get("saveTrendsRoles", "AreaManager;ToolAdmin;RootAdmin").split(";")).contains(access.role)) {
         ServiceMap.logError(request, response, 401, "invalid user role");     
      }

      if(access.username!=null && conf.get("saveUserOnTypicalTrends", "true").equals("true"))
        obj.addProperty("username", Encrypter.encrypt(access.username));

      try (RestHighLevelClient client = ServiceMap.createElasticSearchClient(conf)) {
        String trendsIndex = conf.get("elasticSearchTrendsIndex","typicaltrends");

        IndexRequest indexRequest = new IndexRequest(trendsIndex); 
        //indexRequest.id("1");
        indexRequest.type("_doc");
        indexRequest.source(obj.toString(), XContentType.JSON);

        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
        ServiceMap.println("typicaltrends: result="+indexResponse.getResult());
      }
    } catch (Exception e) {
      ServiceMap.logError(request, response, 500, "request failed: "+e.getMessage());
      ServiceMap.notifyException(e);
      return false;
    } finally {
      con.close();
    }
    //indexResponse.getResult()
    return true;
  }
  
  public String getTypicalTrends(String serviceUri, String valueName, String trendType, String date, String from, String to, String computationType) throws IOException {
    Configuration conf = Configuration.getInstance();
    try (RestHighLevelClient client = ServiceMap.createElasticSearchClient(conf)) {
      String trendsIndex = conf.get("elasticSearchTrendsIndex","typicaltrends");
      
      String q = "serviceUri.keyword:\""+serviceUri+"\"";
      if(valueName!=null) {
        q += " AND valueName.keyword:\"" + valueName + "\"";
      }
      if(trendType!=null) {
        q += " AND trendType.keyword:\"" + trendType + "\"";
      }
      if(date!=null) {
        q += " AND from:[* TO "+date+"] AND to:["+date+" TO *]";
      }
      if(computationType!=null) {
        q += " AND computationType.keyword:\"" + computationType + "\"";
      }
      if(from!=null) {
        q += " AND from:\"" + from + "\"";
      }
      if(to!=null) {
        q += " AND to:\"" + to + "\"";
      }
      SearchRequest sr = new SearchRequest();
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
      searchSourceBuilder.query(QueryBuilders.boolQuery().must(
              QueryBuilders.queryStringQuery(q)
      ));
      searchSourceBuilder.sort("referenceDate", SortOrder.DESC);
      
      sr.source(searchSourceBuilder);
      if(conf.get("elasticSearchScrollSearch","false").equals("true"))
        sr.scroll(TimeValue.timeValueMinutes(1));
      sr.indices(trendsIndex);

      //long ts = System.currentTimeMillis();
      SearchResponse r = client.search(sr, RequestOptions.DEFAULT);
      SearchHit[] hits = r.getHits().getHits();
      StringBuilder sb = new StringBuilder().append('[');
      for(int i=0; i<hits.length; i++) {
        if(i>0)
          sb.append(',');
        sb.append(hits[i].getSourceAsString());
      }
      sb.append(']');
      return sb.toString();
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Configuration conf = Configuration.getInstance();
    String path = request.getPathInfo();
    if(path==null)
      path="/";
    else if(!path.endsWith("/"))
      path += "/";

    //response.addHeader("Access-Control-Allow-Origin", "*");
    response.addHeader("Content-Type", "application/json");

    String serviceUri = request.getParameter("serviceUri");
    String uid = request.getParameter("uid");
    String reqFrom = request.getParameter("requestFrom");

    if(conf.get("enableGetValuesAccessCheck", "false").equals("true") && checkAccess(request, response, true)==null) 
      return;

    if(uid!=null && !ServiceMap.validateUID(uid)) {
      ServiceMap.logError(request, response, 404, "invalid uid");
      return;
    }

    String ip = ServiceMap.getClientIpAddress(request);
    try {
      if(! ServiceMap.checkIP(ip, "api")) {
        ServiceMap.logError(request, response, 403,"API calls daily limit reached");
        return;
      }    
    } catch(Exception e) {
      return;
    }

    if(path.equals("/typicaltrends/")) {
      if(serviceUri==null || CheckParameters.checkUri(serviceUri)!=null) {
        ServiceMap.logError(request, response, 400, "missing or invalid serviceUri");
        return;
      }
      String valueName = request.getParameter("valueName");
      if(valueName!=null && CheckParameters.checkAlphanumString(valueName)!=null) {
        ServiceMap.logError(request, response, 400, "invalid valueName");
        return;
      }
      
      try {
        String trendType = request.getParameter("trendType");
        if(trendType!=null && CheckParameters.checkEnum(trendType, new String[]{"monthDay","monthWeek","dayHour"})!=null) {
          ServiceMap.logError(request, response, 400, "invalid trendType expected 'monthDay','monthWeek','dayHour'");
          return;    
        }
        String date = request.getParameter("atDate");
        if(date!=null && CheckParameters.checkDate(date)!=null) {
          ServiceMap.logError(request, response, 400, "invalid atDate");
          return;          
        }
        String from = request.getParameter("from");
        if(from!=null && CheckParameters.checkDate(from)!=null) {
          ServiceMap.logError(request, response, 400, "invalid 'from' date");
          return;          
        }        
        String to = request.getParameter("to");
        if(to!=null && CheckParameters.checkDate(to)!=null) {
          ServiceMap.logError(request, response, 400, "invalid 'to' date");
          return;          
        }        
        String computationType = request.getParameter("computationType");
        if(computationType!=null && CheckParameters.checkEnum(computationType, new String[]{"min","max","average","median"})!=null) {
          ServiceMap.logError(request, response, 400, "invalid computationType expected 'min','max','average','median'");
          return;    
        }
        String trends = getTypicalTrends(serviceUri, valueName, trendType, date, from, to, computationType);
        response.getWriter().print(trends);
        ServiceMap.updateResultsPerIP(ip, "api", 1);
        ServiceMap.logAccess(request, null, "", null, serviceUri, "api-values-typicltrends", null, null, null, null, "json", uid, reqFrom);
      } catch (Exception ex) {
        ServiceMap.notifyException(ex);
      }
    } else if(path.equals("/")) {
      String fromTime = request.getParameter("fromTime");
      String toTime = request.getParameter("toTime");
      String limit = request.getParameter("limit");
      String valueName = request.getParameter("valueName");
      String lastValue = request.getParameter("lastValue");
      String healthiness = request.getParameter("healthiness");
      String trendType = request.getParameter("trendType");
      String trendTimeAggregation = request.getParameter("trendTimeAggregation");

      if(serviceUri == null) {
          ServiceMap.logError(request, response, 400, "missing serviceUri");
          return;
      }

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
          java.util.Date from=new java.util.Date(new java.util.Date().getTime()-n*1000);
          fromTime=new SimpleDateFormat(ServiceMap.dateFormat).format(from).replace(" ", "T");
        }
        else if(!fromTime.matches("^\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d$")) {
          ServiceMap.logError(request, response, 400, "invalid 'fromTime' parameter, expected n-day,n-hour,n-minute or yyyy-mm-ddThh:mm:ss");
          return;
        }
      }

      try {
        RepositoryConnection con = ServiceMap.getSparqlConnection();
        try {
          //get type of service
          ArrayList<String> types = ServiceMap.getTypes(con, serviceUri, request.getParameter("apikey"));
          if(types.isEmpty()) {
            ServiceMap.logError(request, response, 400, "serviceUri not found");
            return;          
          }

          //get all value names for serviceUri
          ServiceMapping.MappingData md = ServiceMapping.getInstance().getMappingForServiceType(1, types);
          JsonObject attrs = null;
          if(md!=null) {
            attrs = md.getServiceAttributes(con, serviceUri);
          } else {
            attrs = new JsonObject();
          }
          JsonObject otherAttrs = getRegisterValues(serviceUri);
          if(valueName == null) {
            //retrieve last status of all values
            JsonObject result = new JsonObject();
            result.addProperty("serviceUri", serviceUri);
            if(lastValue==null || lastValue.equals("true")) {
              if(md!=null) {
                //retrive last data from phoenix/solr/sparql
                if(md.realTimeSqlQuery!=null) {
                  JsonArray l = new JsonArray();
                  realTimeSqlQuery(md, serviceUri, null, 1, conf, types, l);
                  ServiceMap.println(l);
                  JsonElement d = null;
                  if(l.size()>0) {
                    JsonObject o = l.get(0).getAsJsonObject();
                    if(o.has("instantTime"))
                      d = o.get("instantTime");
                    else if(o.has("measuredTime"))
                      d = o.get("measuredTime");
                  }
                  for(Entry<String,JsonElement> e : attrs.entrySet()) {
                    if(!otherAttrs.has(e.getKey())) {
                      JsonObject x = null;
                      if(l.size()>0) {
                        x = new JsonObject();
                        x.add("value", l.get(0).getAsJsonObject().get(e.getKey()));
                        x.add("valueDate", d);
                      }
                      e.getValue().getAsJsonObject().add("last", x);
                    }
                  }
                }
              }
              Connection rtCon = ServiceMap.getRTConnection();
              if(rtCon!=null) {
                for(Entry<String,JsonElement> e : attrs.entrySet()) {
                  if(otherAttrs.has(e.getKey())) {
                    JsonArray l = new JsonArray();
                    getValues(rtCon, conf, null, toTime, "1", serviceUri, e.getKey(), l, null);
                    e.getValue().getAsJsonObject().add("last", l.size()>0 ? l.get(0) : null);
                  }
                  e.getValue().getAsJsonObject().add("trends", getTrends(rtCon, conf, serviceUri, e.getKey(), trendType, trendTimeAggregation));
                }
                rtCon.close();
              }
            }
            result.add("attributes", attrs);
            response.getWriter().print(result);
            ServiceMap.updateResultsPerIP(ip, "api", 1);
            ServiceMap.logAccess(request, null, "", null, serviceUri, "api-values-last", null, null, null, null, "json", uid, reqFrom);          
          } else {
            //retrive historical values
            if(!attrs.has(valueName) && !otherAttrs.has(valueName)) {
              ServiceMap.logError(request, response, 400, "invalid valueName");
              return;    
            }
            if(fromTime == null && limit == null) {
              limit = "1";                
            }
            JsonArray rtData = new JsonArray();
            JsonObject attr = new JsonObject();
            if(otherAttrs.has(valueName)) {
              //valueName is one of the values table
              Connection rtCon = ServiceMap.getRTConnection();
              if(rtCon != null) {
                if(healthiness!=null && healthiness.equals("true")) {
                  attr.add(valueName, otherAttrs.get(valueName));
                  int l = ServiceMap.getHealthCount(attrs);
                  if(l>0) {
                    fromTime=null;
                    limit = ""+l;
                  }
                }

                getValues(rtCon, conf, fromTime, toTime, limit, serviceUri, valueName, rtData, null);
                rtCon.close();
              }
            } else {
              //valueName is on phoenix specific table or solr
              if(md==null) {
                //ERROR serviceuri with no mapping
                ServiceMap.logError(request, response, 400, "serviceUri "+serviceUri+" is not configured for value retrieval");
                return;
              }
              if(healthiness!=null && healthiness.equals("true")) {
                attr.add(valueName, otherAttrs.get(valueName));
                int l = ServiceMap.getHealthCount(attrs);
                if(l>0) {
                  fromTime=null;
                  limit = ""+l;
                }
              }
              if(md.realTimeSqlQuery!=null) {
                realTimeSqlQuery(md, serviceUri, valueName, fromTime, limit, conf, types, rtData);
              }
            }
            JsonObject result = new JsonObject();
            result.addProperty("serviceUri", serviceUri);
            result.addProperty("valueName", valueName);
            result.add("values", rtData);
            if(healthiness!=null && healthiness.equals("true")) {
              JsonObject health = ServiceMap.computeHealthiness(rtData, attr, "valueAcqDate", toTime);
              result.add("health", health.get(valueName));
            }
            Connection rtCon = ServiceMap.getRTConnection();
            if(rtCon!=null) {
              result.add("trends", getTrends(rtCon, conf, serviceUri, valueName, trendType, trendTimeAggregation));
              rtCon.close();
            }
            response.getWriter().print(result);
            ServiceMap.updateResultsPerIP(ip, "api", rtData.size());
            ServiceMap.logAccess(request, null, "", null, serviceUri, "api-values-hist-"+valueName, null, null, null, null, "json", uid, reqFrom);          
          }
        } finally {
          con.close();
        }
      } catch(Exception e) {
          ServiceMap.notifyException(e);
      }
    }
  }

  private User checkAccess(HttpServletRequest request, HttpServletResponse response, boolean allowByIpAddr) throws IOException {
    Configuration conf = Configuration.getInstance();
    try {
      User u = org.disit.servicemap.JwtUtil.getUserFromRequest(request);
      if(u!=null) {
        ServiceMap.println("user:"+u.username+" role:"+u.role);
        return u;
      }
    } catch(Exception e) {
      ServiceMap.logError(request, response, 401, "access token is not valid");
      return null;
    }
    
    if(!allowByIpAddr)
      return null;
    
    String ipAddress = ServiceMap.getClientIpAddress(request);
    String[] otherIps = conf.get("allowedNetworkIpPrefixes", "127.0.0.1;192.168.0.;192.168.1.").split(";");
    boolean allowed = false;
    for(String s: otherIps) {
      if(ipAddress.startsWith(s.trim())) {
        allowed=true;
        break;
      }
    }
    if (!allowed) {
      ServiceMap.logError(request, response, 401, "cannot access from "+ipAddress);
      return null;
    }
    return new User(ipAddress, null, null);
  }

  public static void getValues(Connection rtCon, Configuration conf, String fromTime, String toTime, String limit, String serviceUri, String valueName, JsonArray rtData, String mode) throws SQLException, NumberFormatException {
    Statement s = rtCon.createStatement();
    s.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
    String fromToTime = "";
    if(fromTime!=null) {
      fromToTime += "AND valueDate>=to_date('"+fromTime.replace("T"," ")+"',null,'CET') ";
    }
    if(toTime!=null) {
      fromToTime += "AND valueDate<=to_date('"+toTime.replace("T"," ")+"',null,'CET') ";
    }
    String lmt = "";
    if(limit!=null) {
      lmt = " LIMIT "+limit;
    }
    String query = "SELECT \"value\", convert_tz(valueDate,'UTC','CET') AS \"valueDate\", convert_tz(valueAcqDate,'UTC','CET') AS \"valueAcqDate\" FROM ServiceDataValues WHERE serviceUri='"+serviceUri+"' AND valueName='"+valueName+"' "+fromToTime+" ORDER BY valueDate DESC "+lmt;
    ServiceMap.println(query);
    ResultSet rs = s.executeQuery(query);
    int nCols = rs.getMetaData().getColumnCount();
    while (rs.next()) {
      JsonObject rt = new JsonObject();
      if(mode==null) {
        for(int c=1; c<=nCols; c++) {
          String v = rs.getMetaData().getColumnLabel(c);
          String value = rs.getString(c);
          if(value!=null && rs.getMetaData().getColumnType(c)==java.sql.Types.DATE ||
                  rs.getMetaData().getColumnType(c)==java.sql.Types.TIMESTAMP ||
                  rs.getMetaData().getColumnType(c)==java.sql.Types.TIME) {
            value=value.replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
          }
          if(value==null)
            value = "";
          rt.addProperty(v, value);
        }
      } else { //SPARQL
        JsonObject v = new JsonObject();
        v.addProperty("value", rs.getString(1));
        rt.add(valueName, v);
        v = new JsonObject();
        String valueDate = rs.getString(2).replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
        v.addProperty("value", valueDate);
        rt.add("measuredTime", v);
        v = new JsonObject();
        String valueAcqDate = rs.getString(3).replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
        v.addProperty("value", valueAcqDate);
        rt.add("acquisitionTime", v);
      }
      rtData.add(rt);
    }
  }

  private void realTimeSqlQuery(ServiceMapping.MappingData md, String serviceUri, String valueName, String fromTime, String limit, Configuration conf, List<String> serviceTypes, JsonArray rtData) throws NumberFormatException, IOException {
    // get RT data from phoenix
    boolean isWeatherSensor = false;
    String query = md.realTimeSqlQuery;
    long ts = System.currentTimeMillis();
    query = query.replace("%SERVICE_URI", serviceUri);
    String serviceId = serviceUri.substring(serviceUri.lastIndexOf("/")+1);
    query = query.replace("%SERVICE_ID", serviceId);
    String frmTime = "";
    if(fromTime!=null) {
      frmTime = " AND observationTime>=to_date('"+fromTime.replace("T"," ")+"',null,'CET') ";
      limit = conf.get("fromTimeLimit","1500");
    } else if(serviceTypes.contains("Weather_sensor")) {
      if(limit.equals("1"))
        limit = "2";
      isWeatherSensor = true;
    }
    query = query.replace("%FROM_TIME", frmTime).replace("%LIMIT", ""+limit);
    ServiceMap.println("realtime query: "+query);
    try {
      Connection rtCon = ServiceMap.getRTConnection();
      if(rtCon==null) {
        //throw new Exception("missing hbase phoenix connection");
        ServiceMap.println("realTimeSqlQuery2 missing hbase phoenix connection");
        return;
      }
      Statement s = rtCon.createStatement();
      s.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
      ResultSet rs = s.executeQuery(query);
      int nCols = rs.getMetaData().getColumnCount();
      int valueCol = 0;
      int dateCol = 0;
      for(int c=1; c<=nCols; c++) {
        String v = rs.getMetaData().getColumnLabel(c);
        if(v.equals(valueName)) {
          valueCol = c;
        }          
        if(dateCol==0 && (rs.getMetaData().getColumnType(c)==java.sql.Types.DATE ||
                rs.getMetaData().getColumnType(c)==java.sql.Types.TIMESTAMP ||
                rs.getMetaData().getColumnType(c)==java.sql.Types.TIME)) {
          dateCol = c;
        }
      }
      int p = 0;
      if(!isWeatherSensor) {
        while (rs.next()) {
          JsonObject rt = new JsonObject();
          String value = rs.getString(valueCol);
          if(value==null)
            value = "";
          rt.addProperty(valueName, value);
          String valueDate = rs.getString(dateCol);
          if(valueDate==null)
            valueDate = "";
          valueDate=valueDate.replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
          rt.addProperty("valueDate", valueDate);
          rtData.add(rt);
        }
      } else {
        //FIX per problema con weather sensor
        String[] row = new String[nCols];
        while (rs.next()) {
          for(int c=1; c<=nCols; c++) {
            String v = rs.getMetaData().getColumnLabel(c);
            if(!v.startsWith("_")) {
              String value = rs.getString(c);
              if(value!=null && rs.getMetaData().getColumnType(c)==java.sql.Types.DATE ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIMESTAMP ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIME)
                value=value.replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
              if(row[c-1]==null)
                row[c-1] = value;
            }
          }
          p++;
        }
        if(p>0) {
          JsonObject rt = new JsonObject();
          String value = row[valueCol-1];
          if(value==null)
            value = "";
          rt.addProperty(valueName, value);
          String valueDate = row[dateCol-1];
          if(valueDate==null)
            valueDate = "";
          valueDate=valueDate.replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
          rt.addProperty("valueDate", valueDate);
          rtData.add(rt);
        }
      }
      rtCon.close();
      ServiceMap.performance("phoenix time realtime: "+(System.currentTimeMillis()-ts)+"ms "+serviceUri+" from:"+fromTime);
    } catch(Exception e) {
      ServiceMap.notifyException(e);
    }
  }
  
  private void realTimeSqlQuery(ServiceMapping.MappingData md, String serviceUri, String fromTime, int limit, Configuration conf, List<String> serviceTypes, JsonArray rtData) throws NumberFormatException, IOException {
    // get RT data from phoenix
    boolean isWeatherSensor = false;
    String query = md.realTimeSqlQuery;
    long ts = System.currentTimeMillis();
    query = query.replace("%SERVICE_URI", serviceUri);
    String serviceId = serviceUri.substring(serviceUri.lastIndexOf("/")+1);
    query = query.replace("%SERVICE_ID", serviceId);
    String frmTime = "";
    if(fromTime!=null) {
      frmTime = " AND observationTime>=to_date('"+fromTime.replace("T"," ")+"',null,'CET') ";
      limit = Integer.parseInt(conf.get("fromTimeLimit","1500"));
    } else if(serviceTypes.contains("Weather_sensor")) {
      if(limit==1)
        limit = 2;
      isWeatherSensor = true;
    }
    query = query.replace("%FROM_TIME", frmTime).replace("%LIMIT", ""+limit);
    ServiceMap.println("realtime query: "+query);
    try {
      Connection rtCon = ServiceMap.getRTConnection();
      if(rtCon==null) {
        //throw new Exception("missing hbase phoenix connection");
        ServiceMap.println("realTimeSqlQuery missing hbase phoenix connection");
        return;
      }
      Statement s = rtCon.createStatement();
      s.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
      ResultSet rs = s.executeQuery(query);
      int nCols = rs.getMetaData().getColumnCount();
      int p = 0;
      if(!isWeatherSensor) {
        while (rs.next()) {
          JsonObject rt = new JsonObject();
          for(int c=1; c<=nCols; c++) {
            String v = rs.getMetaData().getColumnLabel(c);
            if(!v.startsWith("_")) {
              String value = rs.getString(c);
              if(value!=null && rs.getMetaData().getColumnType(c)==java.sql.Types.DATE ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIMESTAMP ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIME)
                value=value.replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
              if(value==null)
                value = "";
              //ServiceMap.println("v:"+v+" -->"+value);
              rt.addProperty(v, value);
            }
          }
          p++;
          rtData.add(rt);
        }
      } else {
        //FIX per problema con weather sensor
        String[] row = new String[nCols];
        while (rs.next()) {
          for(int c=1; c<=nCols; c++) {
            String v = rs.getMetaData().getColumnLabel(c);
            if(!v.startsWith("_")) {
              String value = rs.getString(c);
              if(value!=null && rs.getMetaData().getColumnType(c)==java.sql.Types.DATE ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIMESTAMP ||
                      rs.getMetaData().getColumnType(c)==java.sql.Types.TIME)
                value=value.replace(" ", "T").replace(".000", "")+ServiceMap.getCurrentTimezoneOffset();
              if(row[c-1]==null)
                row[c-1] = value;
            }
          }
          p++;
        }
        if(p>0) {
          JsonObject rt = new JsonObject();
          for(int c=1; c<=nCols; c++) {
            String v = rs.getMetaData().getColumnLabel(c);
            if(!v.startsWith("_")) {
              String value=row[c-1];
              if(value==null)
                value="";
              rt.addProperty(v,value);
            }
          }
          rtData.add(rt);
        }
      }
      rtCon.close();
      ServiceMap.performance("phoenix time realtime: "+(System.currentTimeMillis()-ts)+"ms "+serviceUri+" from:"+fromTime);
    } catch(Exception e) {
      ServiceMap.notifyException(e);
    }
  }

  public JsonObject getTrends(Connection rtCon, Configuration conf, String serviceUri, String valueName, String trendType, String timeAggregation) throws SQLException, NumberFormatException {
    Statement s = rtCon.createStatement();
    s.setQueryTimeout(Integer.parseInt(conf.get("rtQueryTimeoutSeconds", "60")));
    //ServiceDataTrends(serviceUri,valueName,trendType,timeAggregation,hour,\"value\")
    String query;
    if(timeAggregation == null && trendType == null)
      query = "SELECT trendType as \"trendType\",timeAggregation AS \"timeAggregation\",hour AS \"hour\",\"value\" FROM ServiceDataTrends WHERE serviceUri='"+serviceUri+"' AND valueName='"+valueName+"' ORDER BY trendType, timeAggregation";
    else if(trendType == null)
      query = "SELECT trendType as \"trendType\",timeAggregation AS \"timeAggregation\",hour AS \"hour\",\"value\" FROM ServiceDataTrends WHERE serviceUri='"+serviceUri+"' AND valueName='"+valueName+"' AND timeAggregation='"+timeAggregation+"' ORDER BY trendType";
    else if(trendType != null)
      query = "SELECT trendType as \"trendType\",timeAggregation AS \"timeAggregation\",hour AS \"hour\",\"value\" FROM ServiceDataTrends WHERE serviceUri='"+serviceUri+"' AND valueName='"+valueName+"' AND trendType='"+trendType+"' AND timeAggregation='"+timeAggregation+"' ORDER BY trendType";
    else
      query = "SELECT trendType as \"trendType\",timeAggregation AS \"timeAggregation\",hour AS \"hour\",\"value\" FROM ServiceDataTrends WHERE serviceUri='"+serviceUri+"' AND valueName='"+valueName+"' AND trendType='"+trendType+"' ORDER BY timeAggregation";
    ServiceMap.println(query);
    ResultSet rs = s.executeQuery(query);
    int nCols = rs.getMetaData().getColumnCount();
    JsonObject trends = new JsonObject();
    String tt = null;
    JsonObject trend = new JsonObject();
    String ta = null;
    JsonObject aggregation = new JsonObject();
    while (rs.next()) {
      String trendType_ = rs.getString(1);
      String timeAggr = rs.getString(2);
      String hour = rs.getString(3);
      String value = rs.getString(4);
      if(tt!=null && !tt.equals(trendType_)) {
        trend.add(ta, aggregation);
        trends.add(tt, trend);
        trend=new JsonObject();
        aggregation = new JsonObject();
        ta=null;
      }
      if(ta==null || ta.equals(timeAggr)) {
        aggregation.addProperty(hour, value);
      } else {
        trend.add(ta, aggregation);
        aggregation = new JsonObject();
        aggregation.addProperty(hour, value);
      }
      ta=timeAggr;
      tt=trendType_;
    }
    if(ta!=null)
      trend.add(ta, aggregation);
    if(tt!=null)
      trends.add(tt, trend);
    return trends;
  }
}

