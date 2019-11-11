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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
      String[] parts = serviceUri.split("/");
      int n = parts.length;
      if(n<3)
        throw new IllegalAccessException("invalid iot uri "+serviceUri);
      
      String elementId = parts[n-2]+":"+parts[n-3]+":"+parts[n-1];
      ServiceMap.println("iotchecker: "+serviceUri+" "+elementId+" "+accessToken);
      
      HttpClient httpclient = HttpClients.createDefault();
      HttpGet httpget = null;
      HttpResponse response = null;
    
      String datamanagerEndpoint = conf.get("datamanagerEndpoint", "http://localhost:8080/datamanager/api/");

      try {
        long start = System.currentTimeMillis();
        URIBuilder builder;
        if(accessToken!=null) {
          builder = new URIBuilder(datamanagerEndpoint+"v1/apps/"+elementId+"/access/check");
          builder.setParameter("sourceRequest", "servicemap");
        } else {
          builder = new URIBuilder(datamanagerEndpoint+"v1/public/access/check");
          builder.setParameter("sourceRequest", "servicemap")
                  .setParameter("elementID", elementId)
                  .setParameter("elementType", "IOTID");
        }
        httpget = new HttpGet(builder.build());
        httpget.addHeader("Authorization", "Bearer "+accessToken);
        
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
            if(sMessage.equals("PUBLIC")) {
              isPublic = true;
            }
            allow = true;
          }          
        } else {
          ServiceMap.performance("IoTChecker "+builder.build()+" "+statusCode+" { } "+(System.currentTimeMillis()-start)+"ms");
          ServiceMap.notifyException(null, "IoTChecker failed "+statusCode+" call to "+builder.build()+" ");
        }
      } catch(Exception e) {
        ServiceMap.notifyException(e);
        return false;
      }
      
      synchronized(cache) {
        cache.put(serviceUri, new IoTCacheData(isPublic));
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
        String r = "IoTPublicCache ( hit:"+nPublicHit+"+"+nPrivateHit+" access:"+nAccess+" "+hitPerc+") <ol>";
        for(Entry<String,IoTCacheData> d: cache.entrySet()) {
          r+="<li>"+d.getKey()+": "+(d.getValue().isPublic ? "public" : "private")+" "+(System.currentTimeMillis()-d.getValue().generationTime)+"ms old</li>";
        }
        r+="</ol>";
        return r;
      }
    }
}
