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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.disit.servicemap.Configuration;
import org.disit.servicemap.ServiceMap;
import org.json.simple.parser.JSONParser;

public class IoTChecker {
    private static class IoTCacheData {
      boolean isPublic;
      long generationTime;
      Map<String,Long> grantedUsers;
      int nHits = 0;

      public IoTCacheData(boolean isPublic, String user) {
        this.isPublic = isPublic;
        this.generationTime = System.currentTimeMillis();
        this.grantedUsers = new HashMap<>();
        if(!isPublic && user!=null)
          this.grantedUsers.put(user, generationTime);
      }      
    }
    
    static private final Map<String,IoTCacheData> cache = new HashMap<>();
    static private int nAccess = 0, nPublicHit = 0, nPrivateHit = 0, nPrivateUserHit = 0, nRootAdminHits = 0, nPasswordHits = 0;
    static private Object nRootAdminSynch = new Object();
    static private Object nPasswordSynch = new Object();

    static public boolean checkIoTService(String serviceUri, String apiKey) throws IllegalAccessException {
      Configuration conf = Configuration.getInstance();
      if(!serviceUri.startsWith(conf.get("iotCheckerIoTServiceUriPrefix", "http://www.disit.org/km4city/resource/iot/"))) {
        return true;
      }
      
      // check if the IoT is public or private
      if(!conf.get("enableIoTChecker", "true").equals("true"))
        return true;
      if(conf.get("iotCheckerExclude", null)!=null) {
        String[] excl = conf.get("iotCheckerExclude", null).split(";");
        for(String e: excl) {
          if(serviceUri.contains(e.trim()))
            return true;
        }
      }
      
      String accessToken = null;
      String user = null;
      String role = null;
      if(apiKey!=null && apiKey.startsWith("user:")) {
        String[] t = apiKey.split(" at:");
        String[] tt = t[0].split(" role:");
        user = tt[0].substring(5); //skip user:
        role = tt[1];
        accessToken = t[1];
        if(role!=null && role.equals("RootAdmin")) {
          synchronized(nRootAdminSynch) {
            nRootAdminHits++;
          }
          ServiceMap.println("iotChecker "+serviceUri+" GRANTED ACCESS to RootAdmin");
          return true;
        }
      } else if(apiKey!=null && Arrays.asList(conf.get("iotCheckerPassword", "password").split(";")).contains(apiKey)) { //set in the configuration a VERY strong password
          synchronized(nPasswordSynch) {
            nPasswordHits++;
          }
        System.out.println("IotChecker "+serviceUri+" GRANTED ACCESS using password");
        return true;
      }
      
      boolean isPublic = false;
      boolean allow = false;
      IoTCacheData cacheData = null;
      synchronized(cache) {
        cacheData = cache.get(serviceUri);
        nAccess++;
        if(cacheData!=null && System.currentTimeMillis()-cacheData.generationTime<=Integer.parseInt(conf.get("iotCacheMaxTime", "600"))*1000) {
          if(cacheData.isPublic) {
            nPublicHit++;
            cacheData.nHits++;
            return true;
          } else if(accessToken == null) {
            nPrivateHit++;
            cacheData.nHits++;
            return false;
          }
          if(conf.get("iotCheckerEnableUserCache", "false").equals("true")) {
            Long lastUserAccess = cacheData.grantedUsers.get(user);
            if(lastUserAccess!=null) {
              if(System.currentTimeMillis()-lastUserAccess<=Integer.parseInt(conf.get("iotCacheUserMaxTime", "600"))*1000) {
                nPrivateUserHit++;
                cacheData.nHits++;
                return true;
              } else
                cacheData.grantedUsers.remove(user);
            }
          }
        } else {
          cacheData = null;
        }
      }
      // find if serviceUri is public
      Pattern p = Pattern.compile("^.*\\/iot\\/([a-zA-Z\\-_0-9]*)\\/([a-zA-Z0-9\\-_]*)\\/(.*)");
      Matcher m = p.matcher(serviceUri);
      String elementId;
      // if an occurrence if a pattern was found in a given string...
      if (m.find()) {
        String deviceId = m.group(3);
        try {
          deviceId = URLDecoder.decode(deviceId,"UTF8");
        } catch(UnsupportedEncodingException e) {
        }
        
        elementId = m.group(2)+":"+m.group(1)+":"+deviceId;
      } else {
        p = Pattern.compile("^.*\\/iot\\/([a-zA-Z\\-_0-9]*)\\/(.*)");
        m = p.matcher(serviceUri);
        if (m.find()) {
          String deviceId = m.group(2);
          try {
            deviceId = URLDecoder.decode(deviceId,"UTF8");
          } catch(UnsupportedEncodingException e) {
          }        
          elementId = m.group(1)+":"+deviceId;
        } else {        
          throw new IllegalAccessException("invalid iot uri "+serviceUri);
        }
      }
      ServiceMap.println("iotchecker: "+serviceUri+" "+elementId+" "+user+" "+accessToken);

      try {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
          HttpGet httpget = null;
          int CONNECTION_TIMEOUT_MS = Integer.parseInt(conf.get("iotCheckerDatamanagerTimeout", "60")) * 1000; // Timeout in millis.
          RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setSocketTimeout(CONNECTION_TIMEOUT_MS)
            .build();

          String datamanagerEndpoint = conf.get("datamanagerEndpoint", "http://localhost:8080/datamanager/api/");

          long start = System.currentTimeMillis();
          URIBuilder builder;
          if(accessToken!=null) {
            builder = new URIBuilder(datamanagerEndpoint+"v3/apps/"+elementId.replace("/", "%252F")+"/access/check");
            builder.setParameter("sourceRequest", "servicemap")
                    .setParameter("elementType", "IOTID");
          } else {
            builder = new URIBuilder(datamanagerEndpoint+"v1/public/access/check");
            builder.setParameter("sourceRequest", "servicemap")
                    .setParameter("elementID", elementId)
                    .setParameter("elementType", "IOTID");
          }
          httpget = new HttpGet(builder.build());
          httpget.setConfig(requestConfig);

          if(accessToken!=null) {
            httpget.addHeader("Authorization", "Bearer "+accessToken);
          }

          // Create a response handler
          try (CloseableHttpResponse response= httpclient.execute(httpget)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode == 200) { //OK
              String result = EntityUtils.toString(response.getEntity());
              JsonParser parser = new JsonParser();
              JsonObject resultJson = parser.parse(result).getAsJsonObject();
              String u = "";
              if(user!=null && conf.get("iotCheckerLogUsername", "false").equals("true"))
                u = " user:"+user;
              ServiceMap.performance("IoTChecker "+builder.build()+" 200 "+resultJson+u+" "+(System.currentTimeMillis()-start)+"ms");
              String sResult = resultJson.get("result").getAsString();
              String sMessage = resultJson.get("message").isJsonNull() ? "" : resultJson.get("message").getAsString();
              if(sResult.equals("true") ) {
                if(sMessage.contains("PUBLIC")) {
                  isPublic = true;
                }
                allow = true;
              }          
            } else {
              ServiceMap.performance("IoTChecker "+builder.build()+" "+statusCode+" { } "+(System.currentTimeMillis()-start)+"ms");
              ServiceMap.notifyException(null, "IoTChecker for "+elementId+" failed "+statusCode+" call to "+builder.build()+" accessToken:"+accessToken);
            }
          }
        }
      } catch(Exception e) {
        ServiceMap.notifyException(e);
        return false;
      } /*finally {
        if(httpget!=null)
          httpget.releaseConnection();
      }*/
      synchronized(cache) {
        if(conf.get("iotCheckerForcePublicCache","true").equals("true") || isPublic || accessToken==null) {
          if(cacheData==null || isPublic)
            cache.put(serviceUri, new IoTCacheData(isPublic, allow ? user : null));
          else {
            if(allow)
              cacheData.grantedUsers.put(user, System.currentTimeMillis());
          }
        } else {
          //not ForcePublicCache and is private and provided access token then non cache and recheck on next access
          cache.put(serviceUri, null);          
        }
      }
      return allow;
    }

    static public void reset() {
      synchronized(cache) {
        nAccess = 0;
        nPrivateHit = 0;
        nPublicHit = 0;
        nPrivateUserHit = 0;
        cache.clear();
      }
      synchronized(nPasswordSynch) {
        nPasswordHits = 0;
      }
      synchronized(nRootAdminSynch) {
        nRootAdminHits = 0;
      }
    }
    
    static public String print() {
      synchronized(cache) {
        String hitPerc="NA";
        if(nAccess>0)
          hitPerc = ((nPublicHit+nPrivateHit+nPrivateUserHit)*100.0/nAccess)+"%";
        String r = "IoTPublicCache ( nRootAdm: "+nRootAdminHits+" nPassw:"+nPasswordHits+" hit: pub "+nPublicHit+"+ priv "+nPrivateHit+" user "+nPrivateUserHit+" access:"+nAccess+" "+hitPerc+") <ol>";
        for(Entry<String,IoTCacheData> d: cache.entrySet()) {
          if(d.getValue()!=null) {
            Map<String, Long> u = d.getValue().grantedUsers;
            String users = u.keySet().toString();
            r+="<li>"+d.getKey()+": "+(d.getValue().isPublic ? "public" : "private")+" "+(System.currentTimeMillis()-d.getValue().generationTime)/1000.0+"s old, grantedusers:"+users+" hits:"+d.getValue().nHits+"</li>";
          } else {
            r+="<li>"+d.getKey()+": null </li>";            
          }
        }
        r+="</ol>";
        return r;
      }
    }
}
