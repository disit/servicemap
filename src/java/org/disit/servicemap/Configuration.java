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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bellini
 */
public class Configuration {
  static final private String PROP = "/servicemap/servicemap.properties";
  static private volatile Configuration  conf=null;
  static public Configuration getInstance() {
    if(conf==null) {
      synchronized(Configuration.class) {
        if(conf==null) {
          try {
            conf= new Configuration();
          } catch(Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    return conf;
  }

  private Map<String, String> map = new TreeMap<String, String>();

  Configuration() {
    load();
  }
  
  public void load() {
    try {
      System.out.println("Loading properties from "+System.getProperty("user.home")+PROP);
      Properties p = new Properties();
      FileInputStream file=new FileInputStream(System.getProperty("user.home")+PROP);
      p.load(file);
      file.close();
      map = new TreeMap<>();
      Enumeration<?> names = p.propertyNames();
      System.out.println("Configuration:");
      while(names.hasMoreElements()) {
        String n=(String)names.nextElement();
        System.out.println("  "+n+"=\""+p.getProperty(n)+"\"");
        map.put(n,p.getProperty(n));
      }
    } catch (IOException ex) {
      ex.printStackTrace();
      Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
    }    
  }

  public String get(String key, String def) {
    String env = System.getenv("SMAP_"+key);
    if(env!=null)
      return env;
    String envFile = System.getenv("SMAP_"+key+"_FILE");
    if(envFile!=null) {
        try {
            return new String(Files.readAllBytes(Paths.get(envFile)));
        } catch (IOException e) {
            System.out.println("Cannot read "+envFile+" for "+key+" ["+e.getMessage()+"]");
        }      
    }
    String value=map.get(key);
    if(value==null)
      return def;
    return value;
  }

  public String getSet(String key, String def) {
    String value=map.get(key);
    if(value==null) {
      map.put(key,def);
      return def;
    }
    return value;
  }

  public void set(String key, String value) {
    map.put(key, value);
  }
  
  public String asHtml() {
    String html="<p>"+System.getProperty("user.home")+PROP+"</p><ul>";
    for(Entry<String, String> x: map.entrySet()) {
      html+="<li><b>"+x.getKey()+"</b>: \""+x.getValue()+"\"</li>";
    }
    html+="</ul>";
    if(map.size()==0) {
      html+="<p><b>WARNING! empty configuration file</b></p>";
    }
    return html;
  }
}

