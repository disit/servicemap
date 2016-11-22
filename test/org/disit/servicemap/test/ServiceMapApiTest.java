/* ServiceMap.
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */

package org.disit.servicemap.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bellini
 */
public class ServiceMapApiTest {
  
  public ServiceMapApiTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  //String urlPrefix = "http://servicemap.disit.org/WebAppGrafo/api/v1/?";
  //String urlPrefix = "http://www.disit.org/ServiceMap/api/v1/?";
  String urlPrefix = "http://localhost:8080/ServiceMap/api/v1/?";
  //String urlPrefix = "http://192.168.0.207:8080/ServiceMap/api/v1/?";
  
  JSONObject getJSON(String json_url) throws Exception {
    System.out.println(json_url);
    URL url =new URL(json_url);
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
/*    String line;
    while((line = in.readLine()) != null){
        System.out.println(line);
    }*/
    JSONParser p = new JSONParser();
    Object o = p.parse(in);
    assertNotNull(o);
    return (JSONObject)o;    
  }
  
  static public Object jsonPath(JSONObject o, String path) {
    return jsonPath(o,path.split("\\."),0);
  }
  static private Object jsonPath(JSONObject o, String[] path, int start) {
    if(o.containsKey(path[start])) {
      if(start+1<path.length) {
        Object x=o.get(path[start]);
        if(x instanceof JSONObject)
          return jsonPath((JSONObject)x,path,start+1);
        else if(x instanceof JSONArray)
          return jsonPath((JSONArray)x,path,start+1);
      }
      return o.get(path[start]);
    }
    return null;
  }
  static private Object jsonPath(JSONArray a, String[] path, int start) {
    if(path[start].equals("*") && start+1<path.length) {
      for(Object x: a) {
        Object r=null;
        if(x instanceof JSONObject)
          r=jsonPath((JSONObject)x,path,start+1);
        else if(x instanceof JSONArray)
          r=jsonPath((JSONArray)x,path,start+1);
        if (r!=null)
          return r;
      }
      return null;
    }
    int p=Integer.parseInt(path[start]);
    if(p<a.size()) {
      if(start+1<path.length) {
        Object x=a.get(p);
        if(x instanceof JSONObject)
          return jsonPath((JSONObject)x,path,start+1);
        else if(x instanceof JSONArray)
          return jsonPath((JSONArray)x,path,start+1);
      }
      return a.get(p);
    }
    return null;
  }

  @Test
  public void test_municipality_services() throws Exception {
    JSONObject jobj=getJSON(urlPrefix+"selection=COMUNE%20di%20FIRENZE&categorie=boarding_house%3Bagritourism%3Bhotel%3Bbed_and_breakfast%3Bcamping%3Brest_home%3Breligiuos_guest_house%3Bsummer_residence%3Bday_care_center%3Bhostel%3Bvacation_resort%3Bfarm_house%3Bhistoric_residence%3Bmountain_dew%3Bbeach_resort%3Bholiday_village%3BRoadSensor%3BNearBusStops&risultati=100&raggio=100&format=json");
    System.out.println(jobj.toString());
    assertNotNull(jsonPath(jobj,"SensorSites.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"SensorSites.type"));
    assertNotNull(jsonPath(jobj,"Services.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"Services.type"));
    assertNotNull(jsonPath(jobj,"BusStops.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"BusStops.type"));
    assertNotNull(jsonPath(jobj,"SensorSites.features.0"));
    assertNotNull(jsonPath(jobj,"Services.features.0"));
    assertNotNull(jsonPath(jobj,"BusStops.features.0"));
  }

  @Test
  public void test_latlng_services() throws Exception {
    JSONObject jobj=getJSON(urlPrefix+"selection=43.7923;11.2841&categorie=Boarding_house%3BAgritourism%3BHotel%3BBed_and_breakfast%3BCamping%3BRest_home%3BReligiuos_guest_house%3BSummer_residence%3BDay_care_center%3BHostel%3BVacation_resort%3BFarm_house%3BHistoric_residence%3BMountain_dew%3BBeach_resort%3BHoliday_village%3BRoadSensor%3BNearBusStops&maxResults=100&maxDists=0.5&format=json");
    System.out.println(jobj.toString());
    assertNotNull(jsonPath(jobj,"SensorSites.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"SensorSites.type"));
    assertNotNull(jsonPath(jobj,"Services.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"Services.type"));
    assertNotNull(jsonPath(jobj,"BusStops.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"BusStops.type"));
    assertEquals(0,((JSONArray)jsonPath(jobj,"SensorSites.features")).size());
    assertNotNull(jsonPath(jobj,"Services.features.0"));
    assertNotNull(jsonPath(jobj,"BusStops.features.0"));
  }
  
  @Test
  public void test_meteo() throws Exception {
    JSONObject jobj=getJSON(urlPrefix+"serviceUri=http://www.disit.org/km4city/resource/048017&format=json");
    
    System.out.println(jobj.toString());
    assertNotNull(jsonPath(jobj,"results"));
    Object bindings=jsonPath(jobj,"results.bindings");
    assertNotNull(bindings);
    for(Object x : ((JSONArray)bindings)) {
      assertTrue(((String)jsonPath((JSONObject)x,"maxTemp.value")).matches("[0-9]*"));
      assertEquals("literal",jsonPath((JSONObject)x,"maxTemp.type"));
      assertTrue(((String)jsonPath((JSONObject)x,"minTemp.value")).matches("[0-9]*"));
      assertEquals("literal",jsonPath((JSONObject)x,"minTemp.type"));
      assertTrue(((String)jsonPath((JSONObject)x,"day.value")).matches("(Lunedi)|(Martedi)|(Mercoledi)|(Giovedi)|(Venerdi)|(Sabato)|(Domenica)"));
      assertEquals("literal",jsonPath((JSONObject)x,"day.type"));
      assertNotNull(jsonPath((JSONObject)x,"description.value"));
      assertEquals("literal",jsonPath((JSONObject)x,"description.type"));
      assertNotNull(jsonPath((JSONObject)x,"instantDateTime.value"));
      assertEquals("literal",jsonPath((JSONObject)x,"instantDateTime.type"));
    }
    assertEquals("FIRENZE",jsonPath(jobj,"head.location"));
  }

  @Test
  public void test_pensilina() throws MalformedURLException, IOException, Exception {
    JSONObject jobj=getJSON(urlPrefix+"serviceUri=http://www.disit.org/km4city/resource/FM0022&format=json");
    
    System.out.println(jobj.toString());
    assertEquals((long)1,jsonPath(jobj,"BusStop.features.0.id"));
    assertEquals("http://www.disit.org/km4city/resource/FM0022",jsonPath(jobj,"BusStop.features.0.properties.serviceUri"));
    assertEquals("TransferServiceAndRenting_BusStop",jsonPath(jobj,"BusStop.features.0.properties.serviceType"));
    assertEquals("STAZIONE PENSILINA",jsonPath(jobj,"BusStop.features.0.properties.name"));
    assertEquals("Point",jsonPath(jobj,"BusStop.features.0.geometry.type"));
    assertEquals(11.2491,(Double)jsonPath(jobj,"BusStop.features.0.geometry.coordinates.0"),0.001);
    assertEquals(43.7765,(Double)jsonPath(jobj,"BusStop.features.0.geometry.coordinates.1"),0.001);
    assertEquals("1",jsonPath(jobj,"busLines.results.bindings.0.busLine.value"));
    if(jsonPath(jobj,"realtime.results")!=null) {
      assertNotNull(jsonPath(jobj,"realtime.results.bindings.0.ride.value"));
      assertNotNull(jsonPath(jobj,"realtime.results.bindings.0.status.value"));
      assertNotNull(jsonPath(jobj,"realtime.results.bindings.0.arrivalTime.value"));
      assertNotNull(jsonPath(jobj,"realtime.results.bindings.0.busLine.value"));
      Object bindings=jsonPath(jobj,"realtime.results.bindings");
      assertNotNull(bindings);
      for(Object x : ((JSONArray)bindings)) {
        assertTrue(((String)jsonPath((JSONObject)x,"ride.value")).matches("[0-9]+"));
        assertTrue(((String)jsonPath((JSONObject)x,"arrivalTime.value")).matches("[0-9][0-9]:[0-9][0-9]:[0-9][0-9]"));
        assertTrue(((String)jsonPath((JSONObject)x,"busLine.value")).matches("[0-9]+[ A-D]*"));
        assertTrue(((String)jsonPath((JSONObject)x,"status.value")).matches("(In orario)|(Anticipo)|(Ritardo)"));
        assertNotNull(jsonPath((JSONObject)x,"direction.value"));
      }
    }
  }
  
  @Test
  public void test_parcheggio() throws MalformedURLException, IOException, Exception {
    JSONObject jobj=getJSON(urlPrefix+"serviceUri=http://www.disit.org/km4city/resource/RT04801702315PO&format=json");
    
    System.out.println(jobj.toString());
    assertEquals((long)1,jsonPath(jobj,"Service.features.0.id"));
    assertEquals("http://www.disit.org/km4city/resource/RT04801702315PO",jsonPath(jobj,"Service.features.0.properties.serviceUri"));
    assertEquals("TransferServiceAndRenting_Car_park",jsonPath(jobj,"Service.features.0.properties.serviceType"));
    assertEquals("Garage La Stazione Spa",jsonPath(jobj,"Service.features.0.properties.name"));
    assertNotNull(jsonPath(jobj,"realtime.results.bindings.0"));
    assertEquals("617",jsonPath(jobj,"realtime.results.bindings.0.capacity.value"));
  }

  @Test
  public void test_ferragamo() throws MalformedURLException, IOException, Exception {
    JSONObject jobj=getJSON(urlPrefix+"serviceUri=http://www.disit.org/km4city/resource/5967e716e49d6385518805e83e051854&format=json");
    
    System.out.println(jobj.toString());
    assertEquals((long)1,jsonPath(jobj,"Service.features.0.id"));
    assertEquals("http://www.disit.org/km4city/resource/5967e716e49d6385518805e83e051854",jsonPath(jobj,"Service.features.0.properties.serviceUri"));
    assertEquals("CulturalActivity_Historical_buildings",jsonPath(jobj,"Service.features.0.properties.serviceType"));
    assertEquals("Palazzo Spini Feroni",jsonPath(jobj,"Service.features.0.properties.name"));
    assertEquals("VIA DÈ TORNABUONI",jsonPath(jobj,"Service.features.0.properties.address"));
    assertEquals("2",jsonPath(jobj,"Service.features.0.properties.civic"));
    assertEquals("http://www.florenceheritage.it/mobileApp/immagini/aebTornabuoni/20.jpg",jsonPath(jobj,"Service.features.0.properties.multimedia"));
    assertEquals("Point",jsonPath(jobj,"Service.features.0.geometry.type"));
    assertEquals(11.251,(Double)jsonPath(jobj,"Service.features.0.geometry.coordinates.0"),0.001);
    assertEquals(43.7698,(Double)jsonPath(jobj,"Service.features.0.geometry.coordinates.1"),0.001);
  }

  @Test
  public void test_sensore() throws MalformedURLException, IOException, Exception {
    JSONObject jobj=getJSON(urlPrefix+"serviceUri=http://www.disit.org/km4city/resource/EM0100102&format=json");
    
    System.out.println(jobj.toString());
    assertEquals((long)1,jsonPath(jobj,"Sensor.features.0.id"));
    assertEquals("http://www.disit.org/km4city/resource/EM0100102",jsonPath(jobj,"Sensor.features.0.properties.serviceUri"));
    assertEquals("TransferServiceAndRenting_SensorSite",jsonPath(jobj,"Sensor.features.0.properties.serviceType"));
    assertEquals("EM0100102",jsonPath(jobj,"Sensor.features.0.properties.name"));
    assertEquals("VIALE GIOVANNI BOCCACCIO",jsonPath(jobj,"Sensor.features.0.properties.address"));
    assertEquals("Point",jsonPath(jobj,"Sensor.features.0.geometry.type"));
    assertEquals(10.9286,(Double)jsonPath(jobj,"Sensor.features.0.geometry.coordinates.0"),0.001);
    assertEquals(43.7232,(Double)jsonPath(jobj,"Sensor.features.0.geometry.coordinates.1"),0.001);
    assertNotNull(jsonPath(jobj,"realtime.results.bindings.0.instantTime.value"));
    assertTrue(((String)jsonPath(jobj,"realtime.results.bindings.0.concentration.value")).matches("([0-9]+\\.[0-9]+)|(NAN)|(Not Available)"));
    assertTrue(((String)jsonPath(jobj,"realtime.results.bindings.0.speedPercentile.value")).matches("([0-9]+\\.[0-9]+)|(NAN)|(Not Available)"));
    assertTrue(((String)jsonPath(jobj,"realtime.results.bindings.0.avgDistance.value")).matches("([0-9]+\\.[0-9]+)|(NAN)|(Not Available)"));
    assertTrue(((String)jsonPath(jobj,"realtime.results.bindings.0.averageSpeed.value")).matches("([0-9]+\\.[0-9]+)|(NAN)|(Not Available)"));
    assertTrue(((String)jsonPath(jobj,"realtime.results.bindings.0.occupancy.value")).matches("([0-9]+\\.[0-9]+)|(NAN)|(Not Available)"));
    assertTrue(((String)jsonPath(jobj,"realtime.results.bindings.0.avgTime.value")).matches("([0-9]+\\.[0-9]+)|(NAN)|(Not Available)"));
    assertTrue(((String)jsonPath(jobj,"realtime.results.bindings.0.thresholdPerc.value")).matches("([0-9]+\\.[0-9]+)|(NAN)|(Not Available)"));
    assertTrue(((String)jsonPath(jobj,"realtime.results.bindings.0.vehicleFlow.value")).matches("([0-9]+\\.[0-9]+)|(NAN)|(Not Available)"));
  }
  
  @Test
  public void test_fulltext() throws Exception {
    JSONObject jobj=getJSON(urlPrefix+"search=ferragamo&limit=10&format=json");
    System.out.println(jobj.toString());
    assertEquals((long)1,jsonPath(jobj,"features.0.id"));
  }

@Test
  public void test_path_services() throws Exception {
    JSONObject jobj=getJSON(urlPrefix+"selection=wkt:LINESTRING(11.251201629638672%2043.77165162742224,11.258068084716797%2043.771527670168346)&categories=Service&maxResults=100&maxDists=0.5&format=json");
    System.out.println(jobj.toString());
    assertNotNull(jsonPath(jobj,"SensorSites.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"SensorSites.type"));
    assertNotNull(jsonPath(jobj,"Services.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"Services.type"));
    assertNotNull(jsonPath(jobj,"BusStops.features"));
    assertEquals("FeatureCollection",jsonPath(jobj,"BusStops.type"));
    assertEquals(0,((JSONArray)jsonPath(jobj,"SensorSites.features")).size());
    assertEquals(0,((JSONArray)jsonPath(jobj,"BusStops.features")).size());
    assertNotNull(jsonPath(jobj,"Services.features.0"));
  }
  
  @Test
  public void test_bikepath() throws MalformedURLException, IOException, Exception {
    JSONObject jobj=getJSON(urlPrefix+"serviceUri=http://www.disit.org/km4city/resource/3b659537325f29662d399b67647da988&format=json");
    
    System.out.println(jobj.toString());
    assertEquals((long)1,jsonPath(jobj,"Service.features.0.id"));
    assertEquals("http://www.disit.org/km4city/resource/3b659537325f29662d399b67647da988",jsonPath(jobj,"Service.features.0.properties.serviceUri"));
    assertEquals("TransferServiceAndRenting_Cycle_paths",jsonPath(jobj,"Service.features.0.properties.serviceType"));
    assertEquals("percorsi_ciclabili.99",jsonPath(jobj,"Service.features.0.properties.name"));
    assertEquals("VIALE BELFIORE",jsonPath(jobj,"Service.features.0.properties.address"));
    assertEquals("10",jsonPath(jobj,"Service.features.0.properties.civic"));
    assertEquals("LINESTRING (11.24337753 43.7819972, 11.24290406 43.78157487)",jsonPath(jobj,"Service.features.0.properties.wktGeometry"));
    assertEquals("Point",jsonPath(jobj,"Service.features.0.geometry.type"));
    assertEquals(11.2434,(Double)jsonPath(jobj,"Service.features.0.geometry.coordinates.0"),0.001);
    assertEquals(43.782,(Double)jsonPath(jobj,"Service.features.0.geometry.coordinates.1"),0.001);
  }
}