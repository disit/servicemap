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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.disit.servicemap.Configuration;
import org.disit.servicemap.ServiceMap;
import org.json.simple.parser.JSONParser;

public class IoTChecker {
    private static class IoTCacheData {
      boolean isPublic;
      long generationTime;

      public IoTCacheData(boolean isPublic) {
        this.isPublic = isPublic;
        this.generationTime = System.currentTimeMillis();
      }      
    }
    
    static private final Map<String,IoTCacheData> cache = new HashMap<>();
    static private int nAccess = 0, nPublicHit = 0, nPrivateHit = 0;

    static public boolean checkIoTService(String serviceUri, String apiKey) throws IllegalAccessException {
      if(!serviceUri.startsWith("http://www.disit.org/km4city/resource/iot/")) {
        return true;
      }
      
      // check if the IoT is public or private
      Configuration conf = Configuration.getInstance();
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
      if(apiKey!=null && apiKey.startsWith("user:")) {
        accessToken = apiKey.split(" at:")[1];
      } else if(apiKey!=null && apiKey.equals(conf.get("iotCheckerPassword", "password"))) { //set in the configuration a VERY strong password
        System.out.println("IotChecker "+serviceUri+" GRANTED ACCESS using password");
        return true;
      }
      
      boolean isPublic = false;
      boolean allow = false;
      synchronized(cache) {
        IoTCacheData data = cache.get(serviceUri);
        nAccess++;
        if(data!=null && System.currentTimeMillis()-data.generationTime<=Integer.parseInt(conf.get("iotCacheMaxTime", "600"))*1000) {
          if(data.isPublic) {
            nPublicHit++;
            return true;
          }
          else if(accessToken == null) {
            nPrivateHit++;
            return false;
          }
        }
      }
      // find if serviceUri is public
      Pattern p = Pattern.compile("^.*\\/iot\\/([a-zA-Z\\-_0-9]*)\\/([a-zA-Z0-9_]*)\\/(.*)");
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
      ServiceMap.println("iotchecker: "+serviceUri+" "+elementId+" "+accessToken);
      
      HttpClient httpclient = HttpClients.createDefault();
      HttpGet httpget = null;
      HttpResponse response = null;
    
      String datamanagerEndpoint = conf.get("datamanagerEndpoint", "http://localhost:8080/datamanager/api/");

      try {
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
        if(accessToken!=null) {
          httpget.addHeader("Authorization", "Bearer "+accessToken);
        }
        
        // Create a response handler
        response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        if(statusCode == 200) { //OK
          String result = EntityUtils.toString(response.getEntity());
          JsonParser parser = new JsonParser();
          JsonObject resultJson = parser.parse(result).getAsJsonObject();
          ServiceMap.performance("IoTChecker "+builder.build()+" 200 "+resultJson+" "+(System.currentTimeMillis()-start)+"ms");
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
      } catch(Exception e) {
        ServiceMap.notifyException(e);
        return false;
      }
      
      synchronized(cache) {
        if(conf.get("iotCheckerForcePublicCache","true").equals("true") || isPublic || accessToken==null) {
          cache.put(serviceUri, new IoTCacheData(isPublic));
        } else {
          cache.put(serviceUri, null);          
        }
      }
      return allow;
    }

    static public void reset() {
      synchronized(cache) {
        cache.clear();
      }
    }
    
    static public String print() {
      synchronized(cache) {
        String hitPerc="NA";
        if(nAccess>0)
          hitPerc = ((nPublicHit+nPrivateHit)*100.0/nAccess)+"%";
        String r = "IoTPublicCache ( hit: pub "+nPublicHit+"+ priv "+nPrivateHit+" access:"+nAccess+" "+hitPerc+") <ol>";
        for(Entry<String,IoTCacheData> d: cache.entrySet()) {
          if(d.getValue()!=null) {
            r+="<li>"+d.getKey()+": "+(d.getValue().isPublic ? "public" : "private")+" "+(System.currentTimeMillis()-d.getValue().generationTime)+"ms old</li>";
          } else {
            r+="<li>"+d.getKey()+": null </li>";            
          }
        }
        r+="</ol>";
        return r;
      }
    }
}
