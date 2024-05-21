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
package org.disit.servicemap.api;

import com.google.gson.Gson;
import com.unboundid.ldap.sdk.LDAPException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.jsp.JspWriter;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.disit.servicemap.Configuration;
import org.disit.servicemap.Encrypter;
import org.disit.servicemap.JwtUtil.User;
import org.disit.servicemap.ServiceMap;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregator;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.json.simple.JSONObject;

/**
 *
 * @author bellini
 */
public class IoTSearchApi {
  
  public static String[] processServiceUris(String serviceUris) {
    int p = serviceUris.indexOf('|');
    if(p>=0) {
      String pfx = serviceUris.substring(0,p);
      String[] suris = serviceUris.substring(p+1).split(";");
      for(int i=0; i<suris.length; i++) {
        suris[i] = pfx+suris[i];
      }
      return suris;
    } else {
      return serviceUris.split(";");
    }
  }

  public int iotSearch(JspWriter out, final String[] coords, String[] serviceUris, String categories, String model, final String maxDist, String condition, User user, String offset, String limit, String fields, String sortField, String text, String notHealthy, String forceCheck, User u) throws Exception {
    Configuration conf = Configuration.getInstance();

    RestHighLevelClient client = ServiceMap.createElasticSearchClient(conf);
    String[] index = conf.get("elasticSearchDevicesIndex", "devices-state-all").split(";");

    Set<String> fieldList = new HashSet<>();
    if (fields != null) {
      Collections.addAll(fieldList, fields.split(";"));
      fieldList.add("serviceUri");
      fieldList.add("nature");
      fieldList.add("subnature");
    }
    Set<String> skipFields = new HashSet<>(Arrays.asList("src", "uuid", "username",
            "user_delegations", "organization_delegations", "sensorID", "latlon",
            "kind", "groups", "value_name", "value_type"));
    List<String> stdFields = Arrays.asList("serviceUri", "nature", "subnature", "organization", "deviceName", "deviceModel", "date_time", "expected_next_date_time", "deviceDelay_s");
    try {
      String q = null;
      if (serviceUris != null && serviceUris.length > 0) {
        q = serviceUriQueryBuilder(q, serviceUris);
      }
      if (model != null) {
        q = modelQueryBuilder(q, model);
      }
      if (categories != null && !categories.isEmpty()) {
        q = categoriesQueryBuilder(q, categories);
      }
      ArrayList<String[]> delayConds = new ArrayList<>();
      ArrayList<String[]> rangeConds = new ArrayList<>();
      if (condition != null) {
        q = conditionQueryBuilder(q, condition, delayConds, rangeConds);
      }
      
      if(! "true".equals(forceCheck) )
        q = userQueryBuilder(q, user);

      if (text != null) {
        q = textQueryBuilder(q, text);
      }
      
      if("true".equals(notHealthy)) {
        q = notHealthyQuery(q, conf);
      }

      SearchRequest sr = new SearchRequest();

      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

      QueryBuilder geoQuery = QueryBuilders.matchAllQuery();
      double lat = 0, lon = 0;
      if (coords != null) {
        switch (coords.length) {
          case 2:
            lat = Double.parseDouble(coords[0]);
            lon = Double.parseDouble(coords[1]);
            geoQuery = QueryBuilders.geoDistanceQuery("latlon").distance(maxDist + "km").point(lat, lon);
            break;
          case 4:
            lat = (Double.parseDouble(coords[0]) + Double.parseDouble(coords[2])) / 2;
            lon = (Double.parseDouble(coords[1]) + Double.parseDouble(coords[3])) / 2;
            geoQuery = QueryBuilders.geoBoundingBoxQuery("latlon").setCorners(
                    Double.parseDouble(coords[2]), Double.parseDouble(coords[1]),
                    Double.parseDouble(coords[0]), Double.parseDouble(coords[3])
            );
            break;
          default:
            throw new IllegalArgumentException("selection type not supported");
        }
      }

      QueryBuilder dataQuery = QueryBuilders.matchAllQuery();
      if (q != null) {
        dataQuery = QueryBuilders.queryStringQuery(q).defaultField("serviceUri").lenient(Boolean.TRUE);
      }
      BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(dataQuery).filter(geoQuery);
      
      for(String[] rangeCnd: rangeConds) {
        if(rangeCnd[1].equals("gt"))
            boolQuery.must().add(QueryBuilders.rangeQuery(rangeCnd[0]+".value_str.keyword").gt(rangeCnd[2]));
        else if(rangeCnd[1].equals("lt"))
            boolQuery.must().add(QueryBuilders.rangeQuery(rangeCnd[0]+".value_str.keyword").lt(rangeCnd[2]));
        else if(rangeCnd[1].equals("lte"))
            boolQuery.must().add(QueryBuilders.rangeQuery(rangeCnd[0]+".value_str.keyword").lte(rangeCnd[2]));
        else if(rangeCnd[1].equals("gte"))
            boolQuery.must().add(QueryBuilders.rangeQuery(rangeCnd[0]+".value_str.keyword").gte(rangeCnd[2]));
      }
      
      for(String[] delayCnd: delayConds) {
        Map<String, Object> params = new HashMap<>();
        Script delay_s_script = new Script(ScriptType.INLINE, "painless",
                      "Instant Currentdate = Instant.ofEpochMilli(new Date().getTime());\n" +
                      "Instant Startdate = Instant.ofEpochMilli(doc['date_time'].value.getMillis());\n" +
                      "ChronoUnit.SECONDS.between(Startdate, Currentdate) "+delayCnd[0]+" "+delayCnd[1]+";", params);
        boolQuery.filter(QueryBuilders.scriptQuery(delay_s_script));
      }
      
      searchSourceBuilder.query(boolQuery);
      searchSourceBuilder.size(limit == null ? 100 : Integer.parseInt(limit));
      searchSourceBuilder.from(offset == null ? 0 : Integer.parseInt(offset));

      if (coords != null) {
        Map<String, Object> params = new HashMap<>();
        params.put("lat", lat);
        params.put("lon", lon);
        searchSourceBuilder.scriptField("geo_distance",
                new Script(ScriptType.INLINE, "painless",
                        "doc['latlon'].arcDistance(params.lat,params.lon)/1000", params));
        if (sortField == null) {
          searchSourceBuilder.sort(new GeoDistanceSortBuilder("latlon", lat, lon));
        }
      }

      boolean sortOnDelay = false;
      if (sortField != null && !sortField.equals("none")) {
        String[] sortF = sortField.split(":");
        String check;
        if ((check = CheckParameters.checkAlphanumString(sortF[0])) != null) {
          throw new IllegalArgumentException("sort value name is not valid: " + check);
        }
        if (sortF.length > 1 && (check = CheckParameters.checkEnum(sortF[1], new String[]{"asc", "desc"})) != null) {
          throw new IllegalArgumentException("invalid sort type asc/desc");
        }
        if (sortF.length > 2 && (check = CheckParameters.checkEnum(sortF[2], new String[]{"string", "date", "long", "short"})) != null) {
          throw new IllegalArgumentException("invalid sort datatype string/date/long/short");
        }

        SortOrder order = SortOrder.ASC;
        if (sortF.length > 1 && sortF[1].equals("desc")) {
          order = SortOrder.DESC;
        }
        String unmappedType = "string";
        if (sortF.length > 2) {
          unmappedType = sortF[2];
        }
        if(sortF[0].equals("deviceDelay_s")) {
          sortOnDelay = true;
          Map<String, Object> params = new HashMap<>();
          Script delay_s_script = new Script(ScriptType.INLINE, "painless",
                        "Instant Currentdate = Instant.ofEpochMilli(new Date().getTime());\n" +
                        "Instant Startdate = Instant.ofEpochMilli(doc['date_time'].value.getMillis());\n" +
                        "ChronoUnit.SECONDS.between(Startdate, Currentdate);", params);
          searchSourceBuilder.sort(SortBuilders.scriptSort(delay_s_script, ScriptSortBuilder.ScriptSortType.NUMBER).order(order));
        } else if (stdFields.contains(sortF[0].trim())) {
          searchSourceBuilder.sort(new FieldSortBuilder(sortF[0] + ".keyword").order(order).unmappedType(unmappedType));
        } else {
          searchSourceBuilder.sort(new FieldSortBuilder(sortF[0] + ".value").order(order).unmappedType(unmappedType));
          searchSourceBuilder.sort(new FieldSortBuilder(sortF[0] + ".value_str.keyword").order(order).unmappedType(unmappedType));
        }
      }

      searchSourceBuilder.fetchSource(true);
      sr.source(searchSourceBuilder);

      sr.indices(index);

      long ts = System.currentTimeMillis();
      SearchResponse r = client.search(sr, RequestOptions.DEFAULT);
      SearchHit[] hits = r.getHits().getHits();
      long nfound = r.getHits().totalHits;
      String jsonQuery = "NA";
      if (conf.get("elasticSearchDebugQuery", "false").equals("true")) {
        try {
          jsonQuery = searchSourceBuilder.toString();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      ServiceMap.performance("elasticsearch device " + q + "  : " + (System.currentTimeMillis() - ts) + "ms nfound:" + nfound + " query:" + jsonQuery);

      out.println("{");
      out.println("\"type\":\"FeatureCollection\"");
      if (conf.get("debug", "false").equals("true")) {
        out.println(",\"query\":\"" + JSONObject.escape(q) + "\"");
      }
      out.println(",\"features\":[");
      Gson gson = new Gson();

      int pp = 0;
      for (int i = 0; i < hits.length; i++) {
        Map<String, Object> src = hits[i].getSourceAsMap();
        Map<String, DocumentField> fld = hits[i].getFields();
        if("true".equals(forceCheck)) {
            String serviceUri = ((String) src.get("serviceUri"));
            String apikey = null;
            if(u!=null)
                apikey="user:"+u.username+" role:"+u.role+" at:"+u.accessToken;
            
            if(!IoTChecker.checkIoTService(serviceUri, apikey)) {
                nfound--;
                continue;
            }
        }
        String[] latlon = ((String) src.get("latlon")).split(",");
        out.println((pp++ > 0 ? "," : "") + "{\"type\":\"Feature\"");
        out.println(", \"geometry\":{ \"type\":\"Point\", \"coordinates\":[" + latlon[1] + "," + latlon[0] + "] }");
        out.println(", \"properties\":{");
        int p = 0;
        for (String f : stdFields) {
          Object value = src.get(f);
          if (value == null) {
            continue;
          }
          if (value instanceof Map) {
            Map vv = (Map) value;
            if (vv.containsKey("value") && (value = vv.get("value"))!=null) {
              //nothing to do
            } else if (vv.containsKey("value_str")) {
              value = "\"" + JSONObject.escape(vv.get("value_str").toString()) + "\"";
            }
          } else {
            value = "\"" + JSONObject.escape(value.toString()) + "\"";
          }
          out.println((p++ > 0 ? "," : "") + "\"" + f + "\":" + value);
        }
        if(sortOnDelay) {
          out.println(",\"deviceDelay_s\":" + hits[i].getSortValues()[0]);
        }
        if (coords != null) {
          out.println(",\"geo_distance\":" + fld.get("geo_distance").getValue());
        }
        out.println(",\"values\":{");
        p = 0;
        Set<String> flds;
        if (fieldList.isEmpty()) {
          flds = src.keySet();
        } else {
          flds = new HashSet(fieldList);
        }
        for (String f : flds) {
          if (skipFields.contains(f) || stdFields.contains(f)) {
            continue;
          }
          Object value = src.get(f);
          if (value == null) {
            continue;
          }

          if (value instanceof Map) {
            Map vv = (Map) value;
            if (vv.containsKey("value") && (value = vv.get("value"))!=null) {
              //nothing to do
            } else if (vv.containsKey("value_str")) {
              value = "\"" + JSONObject.escape(vv.get("value_str").toString()) + "\"";
            } else if (vv.containsKey("value_arr_obj")) {
              value = gson.toJson(vv.get("value_arr_obj"));
              if (conf.get("elasticSearchValueObjAsString", "false").equals("false")) {
                value = "\"" + JSONObject.escape(value.toString()) + "\"";
              }
            } else if (vv.containsKey("value_obj")) {
              value = gson.toJson(vv.get("value_obj"));
              if (conf.get("elasticSearchValueObjAsString", "false").equals("false")) {
                value = "\"" + JSONObject.escape(value.toString()) + "\"";
              }
            }
          } else {
            value = "\"" + JSONObject.escape(value.toString()) + "\"";
          }
          out.println((p++ > 0 ? "," : "") + "\"" + f + "\":" + value);
        }
        out.println("}}}");
      }
      out.println("]");
      out.println(",\"fullCount\":" + nfound+ " }");
      return (int) hits.length;
    } finally {
      client.close();
    }
  }

  private static String conditionQueryBuilder(String q, String condition, ArrayList<String[]> delayConds, ArrayList<String[]> rangeConds) throws IllegalArgumentException {
    String[] conds = condition.split(";");
    for (String c : conds) {
      //String[] cc = c.split("((?=(:|=|!|>|<))|(?<=(:|=|!|>|<)))",5);
      String[] cc = split(c,":=<>!");
      //System.out.println(Arrays.asList(cc));
      if (cc.length != 3) {
        throw new IllegalArgumentException("invalid condition " + c);
      }
      cc[0] = cc[0].trim();
      if(cc[0].equals("deviceDelay_s")) {
        if(delayConds == null)
          throw new IllegalArgumentException("variable 'deviceDelay_s' cannot be used with over-time search");
        String[] delayCond = new String[2];
        if(cc.length==3) {
          delayCond[0] = cc[1]; //op
          delayCond[1] = cc[2]; //value
        } else {
          delayCond[0] = cc[1]+cc[2]; //op
          delayCond[1] = cc[3]; //value
        }
        if(!"/</<=/>/>=/".contains("/" + delayCond[0] + "/")) {
          throw new IllegalArgumentException("invalid operator " + delayCond[0] + " in special condition " + c);
        }
        Double.parseDouble(delayCond[1]);
        
        delayConds.add(delayCond);
        continue;
      }
      if(cc[0].isEmpty()) {
        throw new IllegalArgumentException("invalid condition " + c + " missing valuename");
      }
      
      String cond = null;
      if (cc.length == 3) {
        switch (cc[1]) {
          case ":":
            cond = cc[0] + ".value_str.keyword:\"" + QueryParserBase.escape(concat(cc,2)) + "\"";
            break;
          case "=":
            cond = cc[0] + ".value:" + Double.parseDouble(cc[2]);
            break;
          case "<":
            cond = cc[0] + ".value:<" + Double.parseDouble(cc[2]);
            break;
          case ">":
            cond = cc[0] + ".value:>" + Double.parseDouble(cc[2]);
            break;
          case "<=":
            cond = cc[0] + ".value:<=" + Double.parseDouble(cc[2]);
            break;
          case ">=":
            cond = cc[0] + ".value:>=" + Double.parseDouble(cc[2]);
            break;
          case "!=":
            cond = "!("+cc[0] + ".value:" + Double.parseDouble(cc[2])+")";
            break;
          case "!:":
            cond = "!("+cc[0] + ".value_str.keyword:\"" + QueryParserBase.escape(cc[2]) + "\")";
            break;
          case ":<":
            if(rangeConds!=null) {
                rangeConds.add(new String[]{cc[0],"lt",cc[2]});
            }
            break;
          case ":>":
            if(rangeConds!=null) {
                rangeConds.add(new String[]{cc[0],"gt",cc[2]});
            }
            break;
          case ":<=":
            if(rangeConds!=null) {
                rangeConds.add(new String[]{cc[0],"lte",cc[2]});
            }
            break;
          case ":>=":
            if(rangeConds!=null) {
                rangeConds.add(new String[]{cc[0],"gte",cc[2]});
            }
            break;
          default:
            throw new IllegalArgumentException("invalid operator " + cc[1] + " in condition " + c);
        }
      }
      if(cond!=null) {
        if (q == null) {
          q = cond;
        } else {
          q += " AND " + cond;
        }
      }
    }
    return q;
  }

  private static String concat(String[] s, int from) {
      String r = s[from];
      for(int i=from+1; i<s.length;i++)
          r += s[i];
      return r;
  }
  
  private static String[] split(String s, String seps) {
      String token1 = "";
      String sep = "";
      String token2 = "";
      
      int state = 0;
      for(int i=0; i<s.length(); i++) {
          char c = s.charAt(i);
          boolean isSep = seps.contains(""+c);
          if(state == 0 && isSep) {
              state = 1;
          } else if(state == 1 && !isSep) {
              state = 2;
          }
              
          if(state == 0) {
              token1 += c;
          } else if(state == 1) {
              sep += c;
          } else {
              token2 += c;
          }
      }
      if(state == 0)
          return new String[] {token1};
      else if(state == 1)
          return new String[] {token1, sep};
      else
        return new String[] {token1, sep, token2};
  }
  
  private String categoriesQueryBuilder(String q, String categories) {
    String[] cats = categories.split(";");
    if (q == null) {
      q = "(";
    } else {
      q += " AND (";
    }
    for (int i = 0; i < cats.length; i++) {
      String c = cats[i];
      if (i > 0) {
        q += " OR ";
      }
      q += "nature:" + c + " OR subnature:" + c;
    }
    q += ")";
    return q;
  }

  private static String serviceUriQueryBuilder(String q, String[] serviceUris) {
    if(q == null)
      q = "(";
    else
      q += "AND (";
    boolean first = true;
    for (String suri : serviceUris) {
      if (!first) {
        q += "OR ";
      }
      q += "serviceUri:\"" + QueryParserBase.escape(suri.trim()) + "\" ";
      first = false;
    }
    q += ")";
    return q;
  }

  private static String notHealthyQuery(String q, Configuration conf) {
    if (q != null) {
      q += " AND";
    } else {
      q = "";
    }
    q += " expected_next_date_time:<now"+conf.get("elasticSearchDevicesHealthyDelay", "-1m");
    return q;
  }

  public int iotSearchOverTimeRange(JspWriter out, final String[] coords, String[] serviceUris, String categories, String model, final String maxDist, String condition, User user, String offset, String limit, String fields, String sortField, String text, String fromTime, String toTime, String aggregate) throws Exception {
    Configuration conf = Configuration.getInstance();

    RestHighLevelClient client = ServiceMap.createElasticSearchClient(conf);
    String[] index = conf.get("elasticSearchFullDevicesIndex", "ot-devices-state-disit").split(";");

    Set<String> fieldList = new HashSet<>();
    if (fields != null) {
      Collections.addAll(fieldList, fields.split(";"));
      fieldList.add("serviceUri");
      fieldList.add("nature");
      fieldList.add("subnature");
    }
    Set<String> skipFields = new HashSet<>(Arrays.asList("src", "uuid", "username",
            "user_delegations", "organization_delegations", "sensorID", "latlon",
            "kind", "groups", "value_name", "value_type"));
    List<String> stdFields = Arrays.asList("serviceUri", "nature", "subnature", "organization", "deviceName", "deviceModel");
    try {
      String q = null;
      if (serviceUris != null && serviceUris.length > 0) {
        q = serviceUriQueryBuilder(q, serviceUris);
      }
      if (model != null) {
        q = modelQueryBuilder(q, model);
      }
      if (categories != null) {
        q = categoriesQueryBuilder(q, categories);
      }
      if (condition != null) {
        q = conditionQueryBuilder(q, condition, null, null);
      }
      q = userQueryBuilder(q,user);

      if (text != null) {
        q = textQueryBuilder(q, text);
      }
      
      q = fromToTimeQueryBuilder(q, fromTime, toTime);

      SearchRequest sr = new SearchRequest();

      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

      QueryBuilder geoQuery = QueryBuilders.matchAllQuery();
      double lat = 0, lon = 0;
      if (coords != null) {
        switch (coords.length) {
          case 2:
            lat = Double.parseDouble(coords[0]);
            lon = Double.parseDouble(coords[1]);
            geoQuery = QueryBuilders.geoDistanceQuery("latlon").distance(maxDist + "km").point(lat, lon);
            break;
          case 4:
            lat = (Double.parseDouble(coords[0]) + Double.parseDouble(coords[2])) / 2;
            lon = (Double.parseDouble(coords[1]) + Double.parseDouble(coords[3])) / 2;
            geoQuery = QueryBuilders.geoBoundingBoxQuery("latlon").setCorners(
                    Double.parseDouble(coords[2]), Double.parseDouble(coords[1]),
                    Double.parseDouble(coords[0]), Double.parseDouble(coords[3])
            );
            break;
          default:
            throw new IllegalArgumentException("selection type not supported");
        }
      }

      QueryBuilder dataQuery = QueryBuilders.matchAllQuery();
      if (q != null) {
        dataQuery = QueryBuilders.queryStringQuery(q).defaultField("serviceUri").lenient(Boolean.TRUE);
      }
      BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(dataQuery).filter(geoQuery);
           
      searchSourceBuilder.query(boolQuery);
      boolean aggreg = ("true".equalsIgnoreCase(aggregate));

      int lmt = limit == null ? 100 : Integer.parseInt(limit);
      searchSourceBuilder.size( aggreg ? 0 : lmt);
      searchSourceBuilder.from(offset == null ? 0 : Integer.parseInt(offset));
      
      if (coords != null) {
        Map<String, Object> params = new HashMap<>();
        params.put("lat", lat);
        params.put("lon", lon);
        searchSourceBuilder.scriptField("geo_distance",
                new Script(ScriptType.INLINE, "painless",
                        "doc['latlon'].arcDistance(params.lat,params.lon)/1000", params));
        if (sortField == null && !aggreg) {
          searchSourceBuilder.sort(new GeoDistanceSortBuilder("latlon", lat, lon));
          sortField = "latlon";
        }
      }
      
      if (!aggreg) {
        if(sortField==null)
          sortField = "date_time:desc";
        if(!sortField.equals("none") && !sortField.equals("latlon")) {
          String[] sortF = sortField.split(":");
          String check;
          if ((check = CheckParameters.checkAlphanumString(sortF[0])) != null) {
            throw new IllegalArgumentException("sort value name is not valid: " + check);
          }
          if (sortF.length > 1 && (check = CheckParameters.checkEnum(sortF[1], new String[]{"asc", "desc"})) != null) {
            throw new IllegalArgumentException("invalid sort type asc/desc");
          }
          if (sortF.length > 2 && (check = CheckParameters.checkEnum(sortF[2], new String[]{"string", "date", "long", "short"})) != null) {
            throw new IllegalArgumentException("invalid sort datatype string/date/long/short");
          }

          SortOrder order = SortOrder.ASC;
          if (sortF.length > 1 && sortF[1].equals("desc")) {
            order = SortOrder.DESC;
          }
          String unmappedType = "string";
          if (sortF.length > 2) {
            unmappedType = sortF[2];
          }
          if(sortF[0].trim().equals("date_time")) {
            searchSourceBuilder.sort(new FieldSortBuilder("date_time").order(order));
          } else if (stdFields.contains(sortF[0].trim())) {
            searchSourceBuilder.sort(new FieldSortBuilder(sortF[0] + ".keyword").order(order).unmappedType(unmappedType));
          } else {
            searchSourceBuilder.sort(new FieldSortBuilder(sortF[0] + ".value").order(order).unmappedType(unmappedType));
            searchSourceBuilder.sort(new FieldSortBuilder(sortF[0] + ".value_str.keyword").order(order).unmappedType(unmappedType));
          }
        }
      }
      
      if(aggreg) {
        //perform serviceUri aggregation
        searchSourceBuilder.aggregation(AggregationBuilders.terms("serviceUri_agg").field("serviceUri.keyword").size(lmt)
                .subAggregation(AggregationBuilders.topHits("sample").size(1))
        );
        searchSourceBuilder.size(0);
      }

      searchSourceBuilder.fetchSource(true);
      sr.source(searchSourceBuilder);

      if (conf.get("elasticSearchScrollSearch", "false").equals("true")) {
        sr.scroll(TimeValue.timeValueMinutes(1));
      }

      sr.indices(index);

      long ts = System.currentTimeMillis();
      SearchResponse r = client.search(sr, RequestOptions.DEFAULT);
      String jsonQuery = "NA";
      if (conf.get("elasticSearchDebugQuery", "false").equals("true")) {
        try {
          jsonQuery = searchSourceBuilder.toString();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if(!aggreg) {
        SearchHit[] hits = r.getHits().getHits();
        long nfound = r.getHits().totalHits;

        ServiceMap.performance("elasticsearch device " + q + "  : " + (System.currentTimeMillis() - ts) + "ms nfound:" + nfound + " query:" + jsonQuery);

        out.println("{");
        out.println("\"type\":\"FeatureCollection\"");
        if (conf.get("debug", "false").equals("true")) {
          out.println(",\"query\":\"" + JSONObject.escape(q) + "\"");
        }
        out.println(",\"fullCount\":" + nfound);
        out.println(",\"features\":[");
        Gson gson = new Gson();

        for (int i = 0; i < hits.length; i++) {
          Map<String, Object> src = hits[i].getSourceAsMap();
          Map<String, DocumentField> fld = hits[i].getFields();
          String[] latlon = ((String) src.get("latlon")).split(",");
          out.println((i > 0 ? "," : "") + "{\"type\":\"Feature\"");
          out.println(", \"geometry\":{ \"type\":\"Point\", \"coordinates\":[" + latlon[1] + "," + latlon[0] + "] }");
          out.println(", \"properties\":{");
          int p = 0;
          for (String f : stdFields) {
            Object value = src.get(f);
            if (value == null) {
              continue;
            }
            if (value instanceof Map) {
              Map vv = (Map) value;
              if (vv.containsKey("value") && (value = vv.get("value"))!=null) {
                //nothing to do
              } else if (vv.containsKey("value_str")) {
                value = "\"" + JSONObject.escape(vv.get("value_str").toString()) + "\"";
              }
            } else {
              value = "\"" + JSONObject.escape(value.toString()) + "\"";
            }
            out.println((p++ > 0 ? "," : "") + "\"" + f + "\":" + value);
          }
          if (coords != null) {
            out.println(",\"geo_distance\":" + fld.get("geo_distance").getValue());
          }
          out.println(",\"values\":{");
          p = 0;
          Set<String> flds;
          if (fieldList.isEmpty()) {
            flds = src.keySet();
          } else {
            flds = new HashSet(fieldList);
          }
          for (String f : flds) {
            if (skipFields.contains(f) || stdFields.contains(f)) {
              continue;
            }
            Object value = src.get(f);
            if (value == null) {
              continue;
            }

            if (value instanceof Map) {
              Map vv = (Map) value;
              if (vv.containsKey("value") && (value = vv.get("value"))!=null) {
                //nothing to do
              } else if (vv.containsKey("value_str")) {
                value = "\"" + JSONObject.escape(vv.get("value_str").toString()) + "\"";
              } else if (vv.containsKey("value_arr_obj")) {
                value = gson.toJson(vv.get("value_arr_obj"));
                if (conf.get("elasticSearchValueObjAsString", "false").equals("false")) {
                  value = "\"" + JSONObject.escape(value.toString()) + "\"";
                }
              } else if (vv.containsKey("value_obj")) {
                value = gson.toJson(vv.get("value_obj"));
                if (conf.get("elasticSearchValueObjAsString", "false").equals("false")) {
                  value = "\"" + JSONObject.escape(value.toString()) + "\"";
                }
              }
            } else {
              value = "\"" + JSONObject.escape(value.toString()) + "\"";
            }
            out.println((p++ > 0 ? "," : "") + "\"" + f + "\":" + value);
          }
          out.println("}}}");
        }
        out.println("]}");
        return (int) hits.length; 
      } else {
        ServiceMap.performance("elasticsearch device aggregation " + q + "  : " + (System.currentTimeMillis() - ts) + "ms query:" + jsonQuery);

        out.println("{");
        out.println("\"type\":\"FeatureCollection\"");
        if (conf.get("debug", "false").equals("true")) {
          out.println(",\"query\":\"" + JSONObject.escape(q) + "\"");
        }
        Aggregations a = r.getAggregations();
        Terms terms = a.get("serviceUri_agg");
        List<? extends Terms.Bucket> b = terms.getBuckets();
        out.println(",\"sumOtherDocs\":"+terms.getSumOfOtherDocCounts());
        out.println(",\"features\":[");
        int i=0;
        for(Terms.Bucket x: b) {
          TopHits sample = x.getAggregations().get("sample");
          Map<String, Object> src = sample.getHits().getHits()[0].getSourceAsMap();
          String[] latlon = ((String) src.get("latlon")).split(",");
          out.println((i++ > 0 ? "," : "") + "{\"type\":\"Feature\"");
          out.println(", \"geometry\":{ \"type\":\"Point\", \"coordinates\":[" + latlon[1] + "," + latlon[0] + "] }");
          out.println(", \"properties\":{");
          int p=0;
          for (String f : stdFields) {
            Object value = src.get(f);
            if (value == null) {
              continue;
            }
            if (value instanceof Map) {
              Map vv = (Map) value;
              if (vv.containsKey("value")) {
                value = vv.get("value");
              } else if (vv.containsKey("value_str")) {
                value = "\"" + JSONObject.escape(vv.get("value_str").toString()) + "\"";
              }
            } else {
              value = "\"" + JSONObject.escape(value.toString()) + "\"";
            }
            out.println((p++ > 0 ? "," : "") + "\"" + f + "\":" + value);
          }
          out.println(", \"aggregationCount\": "+x.getDocCount());
          out.println("}}");
        }
        out.println("]}");
        return b.size();
      }
    } finally {
      client.close();
    }
  }

  private static String fromToTimeQueryBuilder(String q, String fromTime, String toTime) throws ParseException {
    DateFormat dateFormatterT = new SimpleDateFormat(ServiceMap.dateFormatT);
    Date tTime = new Date();
    if(toTime!=null) {
      tTime = dateFormatterT.parse(toTime);
    }
    Date fTime = new Date();
    if(fromTime!=null) {
      fTime = dateFormatterT.parse(fromTime);
    }
    if(fromTime!=null || toTime!=null) {
      DateFormat dateFormatterGMT = new SimpleDateFormat(ServiceMap.dateFormatGMT);
      String fq = null;
      String toTimeZ = null;
      String fromTimeZ = null;
      if(fromTime!=null) {
        fromTimeZ = dateFormatterGMT.format(fTime);
      }
      if(toTime!=null) {
        toTimeZ = dateFormatterGMT.format(tTime);
      }
      if(fromTime!=null && toTime==null)
        fq = "date_time:["+fromTimeZ+" TO *]";
      else if(fromTime==null && toTime!=null)
        fq = "date_time:[* TO "+toTimeZ+"]";
      else
        fq = "date_time:["+fromTimeZ+" TO "+toTimeZ+"]";
      q += " AND "+fq;
    }
    return q;
  }

  private String modelQueryBuilder(String q, String model) {
    if (q == null) {
      q = "";
    } else {
      q += " AND ";
    }
    q += "deviceModel:\"" + QueryParserBase.escape(model) + "\"";
    return q;
  }

  private static String textQueryBuilder(String q, String text) {
    String[] words = text.split("\\s+");
    boolean isPhrase = false;
    String word = "";
    for (String w : words) {
      if (isPhrase && !w.endsWith("\"")) {
        word += " " + w;
      } else if (w.startsWith("\"") && !isPhrase && w.length() > 1 && !w.endsWith("\"")) {
        word += " " + w.substring(1);
        isPhrase = true;
      } else if ((!w.startsWith("\"") /*|| w.length()==1*/) && w.endsWith("\"")) {
        word += " " + w.substring(0, w.length() - 1);
        isPhrase = false;
        if (word.trim().length() > 1) {
          q += " AND *:\"" + QueryParserBase.escape(word.trim()) + "\"";
        }
        word = "";
      } else {
        if (w.startsWith("\"") && w.endsWith("\"") && w.length() > 1) {
          w = w.substring(1, w.length() - 1);
        }
        if (w.length() > 0 && !w.equals("\"")) {
          q += " AND *:\"" + QueryParserBase.escape(w) + "\"";
        }
      }
    }
    //q += " AND " + text;
    return q;
  }

  private static String anonymous = null;
  
  private static String userQueryBuilder(String q, User user) throws Exception {
    if (user == null || !user.role.equals("RootAdmin")) {
      if (q != null) {
        q += " AND";
      } else {
        q = "";
      }
      if(anonymous == null)
        anonymous = Encrypter.encrypt("ANONYMOUS");
      q += " (user_delegations:" + anonymous;
      if (user != null) {
        String encUsername = Encrypter.encrypt(user.username);
        q += " OR username:" + encUsername + " OR user_delegations:" + encUsername;
        if(Configuration.getInstance().get("enableLdapSearch", "false").equals("true")) {
          try {
            LdapSearch ldap = LdapSearch.getInstance();
            String organization = ldap.getOrganization(user.username);
            if(organization!=null) {
              List<String> groups = ldap.getGroups(user.username, organization);
              q += " OR organization_delegations:" + organization;
              for(String grp : groups) {
                q += " OR groups:" + grp;
              }
            } else {
              System.out.println("WARNING user "+user.username+" org not found on ldap");
            }
          } catch(LDAPException e) {
            e.printStackTrace();
          }
        }
      }
      q += ")";
    }
    return q;
  }
  
}
