<%@page import="java.io.BufferedWriter"%>
<%@page import="java.io.FileWriter"%>
<%@page import="java.io.File"%>
<%@page import="org.json.simple.JSONObject"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery.ORDER"%>
<%@page import="org.apache.solr.common.SolrDocument"%>
<%@page import="org.apache.solr.common.SolrDocumentList"%>
<%@page import="org.apache.solr.client.solrj.response.QueryResponse"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>
<%@page import="org.apache.solr.common.params.SolrParams"%>
<%@page import="org.apache.solr.client.solrj.util.ClientUtils"%>
<%@page import="org.openrdf.query.BindingSet"%>
<%@page import="org.openrdf.query.TupleQueryResult"%>
<%@page import="org.openrdf.query.TupleQuery"%>
<%@page import="org.openrdf.query.QueryLanguage"%>
<%@page import="org.openrdf.repository.RepositoryConnection"%>
<%@page import="org.disit.servicemap.ServiceMap"%>
<%@page import="org.apache.solr.common.SolrInputDocument"%>
<%@page import="org.apache.solr.client.solrj.impl.HttpSolrClient"%>
<%@page import="org.apache.solr.client.solrj.SolrClient"%>
<%
String ipAddress = request.getHeader("X-Forwarded-For");  
if (ipAddress == null) {  
  ipAddress = ServiceMap.getClientIpAddress(request);  
}

if(!ipAddress.startsWith("192.168.0.") && !ipAddress.equals("127.0.0.1")) {
  response.sendError(403, "unaccessible from "+ipAddress);
  return;
}

Configuration conf = Configuration.getInstance();
String urlString = conf.get("solrKm4cIndex", "http://192.168.0.207:8983/solr/km4c-index");
System.out.println("SOLR index @ "+urlString);

SolrClient solr = new HttpSolrClient(urlString);
String add = request.getParameter("add");
if(request.getParameter("reset")!=null) {
  solr.deleteByQuery("*:*");
  solr.commit();
  out.println("RESETTED");
} else if(request.getParameter("index-address")!=null) {
  RepositoryConnection con = ServiceMap.getSparqlConnection();
  int offset = 0, n = 0;
  long start = System.currentTimeMillis();
  int size = 100000;
  System.out.println("START index address");
  do {
    String queryText = "select distinct ?sn ?snn ?cc ?r ?rn ?m ?mn ?lat ?lng ?x {\n" +
      "?sn a km4c:StreetNumber;\n"+
      "  km4c:belongToRoad ?r;\n"+
      "  km4c:extendNumber ?snn;\n"+
      "  km4c:hasExternalAccess [ geo:lat ?lat; geo:long ?lng].\n"+
      "optional{?sn km4c:classCode ?cc.}\n" +
      "?r km4c:extendName ?rn;\n"+
      "   km4c:inMunicipalityOf ?m.\n"+
      "?m foaf:name ?mn.\n"+
      "} limit "+size+" offset "+offset;
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
    TupleQueryResult result = tupleQuery.evaluate();
    n=0;
    while (result.hasNext()) {
        BindingSet bindingSet = result.next();

        String sn = bindingSet.getValue("sn").stringValue();
        String snn = bindingSet.getValue("snn").stringValue();
        String r = bindingSet.getValue("r").stringValue();
        String rn = bindingSet.getValue("rn").stringValue();
        String cc = (bindingSet.getValue("cc")!=null ? bindingSet.getValue("cc").stringValue() : "");
        String m = bindingSet.getValue("m").stringValue();
        String mn = bindingSet.getValue("mn").stringValue();
        String lat = bindingSet.getValue("lat").stringValue();
        String lng = bindingSet.getValue("lng").stringValue();
        if(cc.equals("Rosso"))
          snn+=" R";
        //out.println(rn+","+snn+","+mn+"<br>");
        SolrInputDocument doc=new SolrInputDocument();
        doc.setField("entityType_s", "StreetNumber");
        doc.setField("roadName_s_lower", rn);
        doc.setField("roadUri_s", r);
        doc.setField("streetNumber_s", snn);
        doc.setField("id", sn);
        doc.setField("municipalityName_s_lower", mn);
        doc.setField("municipalityUri_s", m);
        doc.setField("geo_coordinate_p", lat+","+lng);
        //doc.setField("snn", snn);
        solr.add(doc);
        n++;
    }
    solr.commit();
    offset += size;
    System.out.println("offset: "+offset+" time: "+(System.currentTimeMillis()-start)/1000);
  } while(n==size);
  out.println("END!");
  System.out.println("END!");
} else if(request.getParameter("index-service")!=null) {
  RepositoryConnection con = ServiceMap.getSparqlConnection();
  int offset = 0, n = 0;
  long start = System.currentTimeMillis();
  int size = 10000;
  do {
    String queryText = "select distinct ?s ?sType ?sCategory ?name ?city (IF(BOUND(?elat2),?elat2,?elat1) as ?lat) (IF(BOUND(?elong2),?elong2,?elong1) as ?long) {\n"
      + "?s a km4c:Service OPTION(inference \"urn:ontology\").\n"
      + "?s a ?sType. FILTER(?sType!=km4c:RegularService && ?sType!=km4c:Service && ?sType!=km4c:DigitalLocation && ?sType!=km4c:TransverseService && ?sType!=km4c:BusStop && ?sType!=km4c:SensorSite)\n"
      + "?sType rdfs:subClassOf* ?sCategory. FILTER(?sCategory != <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing>)\n"
      + "?sCategory rdfs:subClassOf km4c:Service.\n"
      + "?s schema:name ?name.\n"
      //+ "?s schema:streetAddress ?address.\n"
      //+ "?s km4c:houseNumber ?number.\n"
      //+ "?s schema:addressRegion ?prov.\n"
      + "?s schema:addressLocality ?city.\n"
      + "OPTIONAL{\n"
      + "?s km4c:hasAccess ?entry.\n"
      + "?entry geo:lat ?elat1.\n"
      + "?entry geo:long ?elong1.\n"
      + "}\n"
      + "OPTIONAL{\n"
      + "?s geo:lat ?elat2.\n"
      + "?s geo:long ?elong2.\n"
      + "}\n"
      + "FILTER(BOUND(?elat1)||BOUND(?elat2))\n"
      + "} limit "+size+" offset "+offset;
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
    TupleQueryResult result = tupleQuery.evaluate();
    n=0;
    while (result.hasNext()) {
        BindingSet bindingSet = result.next();

        String s = bindingSet.getValue("s").stringValue();
        String sType = bindingSet.getValue("sType").stringValue();
        sType = sType.substring(sType.lastIndexOf("#")+1);
        String sCategory = bindingSet.getValue("sCategory").stringValue();
        sCategory = sCategory.substring(sCategory.lastIndexOf("#")+1);
        String serviceType = sCategory + " " + sType;
        String name = bindingSet.getValue("name").stringValue().replace("_"," ");
        String city = bindingSet.getValue("city").stringValue();
        String lat = bindingSet.getValue("lat").stringValue().replace("'", "").replace(",",".");
        String lng = bindingSet.getValue("long").stringValue().replace("'", "").replace(",",".");

        SolrInputDocument doc=new SolrInputDocument();
        //doc.setDocumentBoost(5.0f);
        float boost=0.9f;
        if(sCategory.equals("CulturalActivity"))
          boost = 2.0f;
        doc.setField("id", s);
        doc.setField("entityType_s", serviceType);
        doc.setField("name_s_lower", name, boost);
        doc.setField("municipalityName_s_lower", city);
        doc.setField("geo_coordinate_p", lat+","+lng);
        //doc.setField("snn", snn);
        try {
          solr.add(doc);
        } catch(NumberFormatException e) {
          System.out.println(s+" --> "+lat+","+lng);
        }
        n++;
    }
    solr.commit();
    offset += size;
    System.out.println("offset: "+offset+" time: "+(System.currentTimeMillis()-start)/1000);
  } while(n==size);
  out.println("END!");
  System.out.println("END!");
} else if(request.getParameter("index-municipality")!=null) {
  RepositoryConnection con = ServiceMap.getSparqlConnection();
  int offset = 0, n = 0;
  long start = System.currentTimeMillis();
  int size = 1000;
  do {
    String queryText = "select ?m ?mn (avg(?lat) as ?mlat) (avg(?lng) as ?mlng) {\n" +
      " ?r a km4c:Road;\n" +
      " km4c:hasStreetNumber/km4c:hasExternalAccess ?ea;\n" +
      " km4c:inMunicipalityOf ?m.\n" +
      " ?ea geo:lat ?lat; geo:long ?lng.\n" +
      " ?m foaf:name ?mn\n" +
      "} group by ?m ?mn\n"+
      "limit "+size+" offset "+offset;
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
    TupleQueryResult result = tupleQuery.evaluate();
    n=0;
    while (result.hasNext()) {
        BindingSet bindingSet = result.next();

        String m = bindingSet.getValue("m").stringValue();
        String mn = bindingSet.getValue("mn").stringValue();
        String mlat = bindingSet.getValue("mlat").stringValue();
        String mlng = bindingSet.getValue("mlng").stringValue();
        //out.println(rn+","+snn+","+mn+"<br>");
        SolrInputDocument doc=new SolrInputDocument();
        doc.setField("entityType_s", "Municipality");
        doc.setField("id", m);
        doc.setField("municipalityName_s_lower", mn, 5.0f);
        doc.setField("geo_coordinate_p", mlat+","+mlng);
        solr.add(doc);
        n++;
    }
    solr.commit();
    offset += size;
    System.out.println("offset: "+offset+" time: "+(System.currentTimeMillis()-start)/1000);
  } while(n==size);
  out.println("END!");
  System.out.println("END!");
} else if(request.getParameter("index-busstop")!=null) {
  RepositoryConnection con = ServiceMap.getSparqlConnection();
  int offset = 0, n = 0;
  long start = System.currentTimeMillis();
  
  ServiceMapApiV1 api=new ServiceMapApiV1();
  File file =new File("tpl-stop-municipality.n3");
  if(!file.exists()){
    file.createNewFile();
  }

  FileWriter fileWriter = new FileWriter(file.getAbsolutePath());
  BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
  int size = 1000;
  System.out.println("START");
  do {
    String queryText = "select distinct ?s ?sType ?sCategory ?typeLabel ?name ?lat ?long ?city ?cityUri ?agency  ?x {\n" +
      "?s a gtfs:Stop;\n" +
      "  a ?sType.\n" +
      "?sType rdfs:subClassOf* ?sCategory.\n" +
      "?sCategory rdfs:subClassOf km4c:Service.\n" +
      "?s geo:lat ?lat;\n" +
      "   geo:long ?long;\n" +
      "   foaf:name ?name.\n" +
      "OPTIONAL{ ?s km4c:isInMunicipality ?cityUri. ?cityUri foaf:name ?city }" +
      "?y gtfs:stop ?s.\n" +
      "?y gtfs:trip/gtfs:route/gtfs:agency/foaf:name ?agency.\n" +
      "?sType rdfs:label ?typeLabel.\n" +
      "  filter(lang(?typeLabel)=\"it\")\n" +
      "} limit "+size+" offset "+offset;
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryText);
    TupleQueryResult result = tupleQuery.evaluate();
    n=0;
    while (result.hasNext()) {
        BindingSet bindingSet = result.next();

        String s = bindingSet.getValue("s").stringValue();
        String sType = bindingSet.getValue("sType").stringValue();
        sType = sType.substring(sType.lastIndexOf("#")+1);
        String sCategory = bindingSet.getValue("sCategory").stringValue();
        sCategory = sCategory.substring(sCategory.lastIndexOf("#")+1);
        String serviceType = sCategory + " " + sType;
        String typeLabel = bindingSet.getValue("typeLabel").stringValue();
        String name = bindingSet.getValue("name").stringValue().replace("_"," ");
        String city = null;
        if(bindingSet.getValue("city")!=null)
          city = bindingSet.getValue("city").stringValue();
        String cityUri = null;
        if(bindingSet.getValue("cityUri")!=null)
          cityUri = bindingSet.getValue("cityUri").stringValue();
        String lat = bindingSet.getValue("lat").stringValue().replace("'", "").replace(",",".");
        String lng = bindingSet.getValue("long").stringValue().replace("'", "").replace(",",".");
        
        if(city==null) {
          JSONObject json=api.queryLocation(con, lat, lng, "false",0.0);
          city = (json!=null ? (String)json.get("municipality") : "");
          cityUri = (json!=null ? (String)json.get("municipalityUri") : "");
          if(!cityUri.isEmpty())
            bufferWriter.write("<"+s+"> <http://www.disit.org/km4city/schema#isInMunicipality> <"+cityUri+">\r\n");
        }
        
        SolrInputDocument doc=new SolrInputDocument();
        //doc.setDocumentBoost(5.0f);
        float boost=1.1f;
        doc.setField("id", s);
        doc.setField("entityType_s", serviceType);
        doc.setField("entityType_txt", serviceType);
        doc.setField("typeLabel_s_lower", typeLabel, boost);
        doc.setField("name_s_lower", name, boost);
        doc.setField("municipalityName_s_lower", city);
        doc.setField("geo_coordinate_p", lat+","+lng);
        //doc.setField("snn", snn);
        try {
          solr.add(doc);
        } catch(NumberFormatException e) {
          System.out.println(s+" --> "+lat+","+lng);
        }
        n++;
    }
    solr.commit();
    offset += size;
    System.out.println("offset: "+offset+" time: "+(System.currentTimeMillis()-start)/1000);
  } while(n==size);
  bufferWriter.close();
  out.println("END!");
  System.out.println("END!");
} else if(request.getParameter("search")!=null) {
  String search = request.getParameter("search");
  String location = request.getParameter("location");
  String tilde = "~";
  String[] s=search.split("\\s+");
  String ss = "";
  int i=0;
  boolean haveNumber = false;
  for(String x:s) {
    if(i>0)
      ss+="AND ";
    ss+= x;
    if(x.matches("\\d+[^0-9]*"))
      haveNumber=true;
    else
      ss+=tilde+"0.7";
    ss+=" ";
    i++;
  }
  if(!haveNumber)
    ss+=" AND 1";
  System.out.println(ss);
  SolrQuery query = new SolrQuery(ss);
  if(location!=null) {
    query.addFilterQuery("{!geofilt pt="+location+" sfield=geo_coordinate_p d=5}");
    query.addSort("geodist(geo_coordinate_p,"+location+")",ORDER.asc);
  }
  QueryResponse qr=solr.query(query);
  SolrDocumentList sdl=qr.getResults();
  out.println("<ol>");
  for(SolrDocument d: sdl) {
    String id = (String)d.getFieldValue("id");
    String roadName = (String)d.getFieldValue("roadName_s_lower");
    String streetNumber = (String)d.getFieldValue("streetNumber_s");
    String municipalityName = (String)d.getFieldValue("municipalityName_s_lower");    
    String geo_coordinate = (String)d.getFieldValue("geo_coordinate_p");    
    out.println("<li>"+roadName+", "+streetNumber+", "+municipalityName+": "+geo_coordinate+" ("+id+")</li>");
  }
  out.println("</ol>");  
} else if(request.getParameter("search-json")!=null) {
  String search = request.getParameter("search-json");
  String location = request.getParameter("location");
  String tilde = "~";
  String[] s=search.split("\\s+");
  String ss = "";
  int i=0;
  boolean haveNumber = false;
  for(String x:s) {
    if(i>0)
      ss+="AND ";
    ss+= x;
    if(x.matches("\\d+[^0-9]*"))
      haveNumber=true;
    else if(i==s.length-1)
      ss+="*";
    else
      ss+=tilde+"0.7";
    ss+=" ";
    i++;
  }
  //if(!haveNumber)
  //  ss+=" AND 1";
  //System.out.println(ss);
  SolrQuery query = new SolrQuery(ss);
  if(location!=null) {
    query.addFilterQuery("{!geofilt pt="+location+" sfield=geo_coordinate_p d=5}");
    query.addSort("geodist(geo_coordinate_p,"+location+")",ORDER.asc);
  }
  QueryResponse qr=solr.query(query);
  SolrDocumentList sdl=qr.getResults();
  out.println("[");
  i=0;
  for(SolrDocument d: sdl) {
    String id = (String)d.getFieldValue("id");
    String type = (String)d.getFieldValue("entityType_s");
    String geo_coordinate = (String)d.getFieldValue("geo_coordinate_p");
    String[] g=geo_coordinate.split(",");
    if(i!=0)
      out.println(",");
    if(type.equals("StreetNumber")) {
      String roadName = (String)d.getFieldValue("roadName_s_lower");
      String streetNumber = (String)d.getFieldValue("streetNumber_s");
      String municipalityName = (String)d.getFieldValue("municipalityName_s_lower");
      out.println("{\"label\":\""+roadName+", "+streetNumber+", "+municipalityName+"\",\"id\":\""+id+"\",\"lat\":"+g[0]+",\"lng\":"+g[1]+"}");
    } else {
      String name = (String)d.getFieldValue("name_s_lower");
      out.println("{\"label\":\""+name+"\",\"id\":\""+id+"\",\"lat\":"+g[0]+",\"lng\":"+g[1]+"}");
    }
    i++;
  }
  out.println("]");  
}

  %>
