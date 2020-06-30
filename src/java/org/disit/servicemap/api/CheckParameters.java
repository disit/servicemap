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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.disit.servicemap.ServiceMap;

public class CheckParameters {
  public static String checkLatLng(String position) {
    if(position==null)
      return null;
    
    String[] ll = position.split(";");
    String chk;
    if(ll.length==1)
      return "missing ; separator";
    else if(ll.length==2) {
      chk = checkLat(ll[0]);
      if(chk!=null)
        return chk;
      chk = checkLng(ll[1]);
      if(chk!=null)
        return chk;      
    } else if(ll.length==4) {
      chk = checkLat(ll[0]);
      if(chk!=null)
        return chk;
      chk = checkLng(ll[1]);
      if(chk!=null)
        return chk;      
      chk = checkLat(ll[2]);
      if(chk!=null)
        return chk;
      chk = checkLng(ll[3]);
      if(chk!=null)
        return chk;            
    } else
      return "invalid number of ; separators";
    return null; //OK
  }
  
  public static String checkLat(String slat) {
    try {
      double lat = Double.parseDouble(slat);
      if(lat < -90 || lat > 90)
        return "invalid latitude, out of [-90,90]";
    } catch(NumberFormatException e) {
      return "invalid latitude '"+slat+"'";
    }
    return null;
  }
  
  public static String checkLng(String slng) {
    try {
      double lng = Double.parseDouble(slng);
      if(lng < -180 || lng > 180)
        return "invalid longitude, out of [-180,180]";
    } catch(NumberFormatException e) {
      return "invalid longitude '"+slng+"'";
    }
    return null;
  }
  
  public static String checkSelection(String selection) {
    if (selection==null)
      return null;
    
    String[] x=selection.split(";");
    if(x.length==1) {
      if(selection.startsWith("wkt:"))
        return null;
      if(selection.startsWith("geo:"))
        return null;
      if(selection.startsWith("http://"))
        return null;
      String check;
      if((check = checkAlphanumString(selection))==null)
        return null;
      return "invalid selection no ; separator or wkt:/geo: prefix";
    } 
    return checkLatLng(selection);
  }
  
  public static String checkDistance(String d) {
    try {
      double dst = Double.parseDouble(d);
      if(dst<0)
        return "invalid negative distance";
    } catch(NumberFormatException e) {
      return "invalid distance '"+d+"'";
    }
    return null;
  }
  
  public static String checkNumber(String n) {
    try {
      Double.parseDouble(n);
    } catch(NumberFormatException e) {
      return "invalid number '"+n+"'";
    }
    return null;
  }
  
  public static String checkAlphanumString(String s) {
    if(!s.matches("^[\\p{L}0-9 ]*$"))
      return "invalid alphanum string";
    return null;
  }
  
  public static String checkDatetime(String dateTime) {
      try {
        new SimpleDateFormat(ServiceMap.dateFormatT).parse(dateTime);
      } catch(ParseException e) {
        try {
          new SimpleDateFormat(ServiceMap.dateFormatTmin).parse(dateTime);
        } catch(ParseException ex) {
          return "invalid date time string "+e.getMessage(); 
        }        
      }
    return null;
  }
  
  public static String checkUri(String uri) {
    if(uri==null || !uri.matches("^https?://[-a-zA-Z\\.0-9_/%\\+ :;()]*$"))
      return "invalid uri";
    return null;
  }
  
  public static String checkApiKey(String apikey) {
    if(apikey==null || !apikey.matches("[0-9A-Fa-f]+"))
      return "invalid apikey";
    return null;
  }
}
