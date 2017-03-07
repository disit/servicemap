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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sparql.SPARQLRepository;
import virtuoso.sesame2.driver.VirtuosoRepository;

/**
 *
 * @author bellini
 */
public class ServiceMap {
  
  static private List<String> stopWords = null;
  
  static public void initLogging() {
  }
  
  static public RepositoryConnection getSparqlConnection() throws Exception {
    Repository repo;
    Configuration conf = Configuration.getInstance();
    String virtuosoEndpoint = conf.get("virtuosoEndpoint", null);
    if(virtuosoEndpoint!=null)
      repo = new VirtuosoRepository(virtuosoEndpoint, conf.get("virtuosoUser", "dba"), conf.get("virtuosoPwd", "dba"));
    else {
      String sparqlEndpoint = conf.get("sparqlEndpoint", null);
      repo = new SPARQLRepository(sparqlEndpoint);
    }
    repo.initialize();
    return repo.getConnection();
  }
  
  static public void logQuery(String query, String id, String type, String args, long time) {
    try {
      String queryLog = Configuration.getInstance().get("queryLogFile", "query-log.txt");
      File file =new File(queryLog);

      if(!file.exists()){
        file.createNewFile();
      }

      //System.out.println("file: "+file.getAbsolutePath());
      //true = append file
      Date now = new Date();
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String formattedDate = formatter.format(now);
      FileWriter fileWritter = new FileWriter(file.getAbsolutePath(),true);
      BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
      bufferWritter.write(("#QUERYID|"+id+"|"+type+"|"+args+"|"+(time/1000000)+"|"+formattedDate+"|"+query+"\n#####################\n").replace("\n", "\r\n"));
      bufferWritter.close();
      System.out.println("#QUERYID:"+id+":"+type+":"+args+":"+(time/1000000));
      //System.out.println(query);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  static public void logAccess(String ip, String email, String UA, String sel, String categorie, String serviceUri, String mode, String numeroRisultati, String raggio, String queryId, String text, String format, String uid, String reqFrom) throws IOException, SQLException {
    Configuration conf = Configuration.getInstance();
    BufferedWriter out = null;
    File f = null;
    String filePath = conf.get("accessLogFile",null);
    try {
        Date now = new Date();
        if(filePath!=null) {
          FileWriter fstream = new FileWriter(filePath, true); //true tells to append data.
          out = new BufferedWriter(fstream);
          out.write( now + "|" + mode + "|" + ip + "|" + UA + "|" + serviceUri + "|" + email + "|" + sel + "|" + categorie + "|" + numeroRisultati +"|" + raggio + "|" + queryId + "|" + text + "|" + format + "|"+uid+"|"+reqFrom+"\n");
        }  

        //Class.forName("com.mysql.jdbc.Driver");
        Connection conMySQL = ConnectionPool.getConnection();
        if(conMySQL== null) {
          System.out.println("ERROR logAccess: connection==null");
          ((ConnectionPool)ConnectionPool.getConnection()).printStatus();
          return;
        }
        // DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);;
        String query = "INSERT INTO AccessLog(mode,ip,userAgent,serviceUri,email,selection,categories,maxResults,maxDistance,queryId,text,format,uid,`reqfrom`) VALUES "+
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        //System.out.println(query);
        PreparedStatement st = conMySQL.prepareStatement(query);
        st.setString(1, mode);
        st.setString(2, ip);
        st.setString(3, UA);
        st.setString(4, serviceUri);
        st.setString(5, email);
        st.setString(6, sel);
        st.setString(7, categorie);
        st.setString(8, numeroRisultati);
        st.setString(9, raggio);
        st.setString(10, queryId);
        st.setString(11, text);
        st.setString(12, format);
        st.setString(13, uid);
        st.setString(14, reqFrom);
        st.executeUpdate();
        st.close();
        conMySQL.close();
    } catch (IOException e) {
        System.err.println("Error: " + e.getMessage());
    } finally {
        if (out != null) {
            out.close();
        }
    }
  }

  static public String escapeJSON(String s) {
    if(s==null)
      return null;
    return JSONObject.escape(s); //)s.replace("\"", "\\\"").replace("\t", "\\t").replace("\n", "\\n");
  }

  static final public String prefixes =
              "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource/>\n"
            + "PREFIX schema:<http://schema.org/>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX omgeo:<http://www.ontotext.com/owlim/geo#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX vcard:<http://www.w3.org/2006/vcard/ns#>\n"
            + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
            + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n";
  
  
   static public String routeLineQuery(String line) {
    String filtroLinee = "";
    if (!line.equals("all")) {
      filtroLinee = " ?tpll dcterms:identifier \"" + line + "\"^^xsd:string.\n";
    }
    return  prefixes 
   
            + "SELECT DISTINCT ?dir ?line ?rName ?code ?bsFirst ?bsLast ?x WHERE{\n"
              + " ?tpll rdf:type km4c:PublicTransportLine.\n"
              + " ?tpll dcterms:identifier ?line.\n"
              + filtroLinee      
              + " ?tpll km4c:hasRoute ?route.\n"
              + " ?route km4c:hasFirstStop ?bss.\n"
              + " ?route km4c:hasLastStop ?bse.\n"
              + " ?route km4c:direction ?dir.\n"
              + " ?route dcterms:identifier ?code.\n"
              + " ?bss foaf:name ?bsFirst.\n"
              + " ?bse foaf:name ?bsLast.\n"
              + " ?route foaf:name ?rName.\n"
              + " ?route opengis:hasGeometry ?geometry .\n"
              + " ?geometry opengis:asWKT ?polyline .\n"
              + "} ORDER BY ?line ?dir ";
  }
  
  static public String busLineQuery(String route) {
    /*String filtroLinee = "";
    if (!line.equals("all")) {
      filtroLinee = " ?tpll dcterms:identifier \"" + line + "\"^^xsd:string.\n";
    }*/
    return  prefixes 
            + "SELECT DISTINCT ?nomeFermata ?x WHERE {\n"
            + " ?tpll rdf:type km4c:PublicTransportLine.\n"
            //+ filtroLinee
            + " ?tpll km4c:hasRoute ?route.\n"
            + " ?route dcterms:identifier \"" + route + "\".\n"
            + "?route km4c:hasSection ?rs.\n"
            + " ?rs km4c:endsAtStop ?bs1.\n"
            + " ?rs km4c:startsAtStop ?bs2.\n"
            + " { ?bs1 foaf:name ?nomeFermata. }\n"
            + " UNION\n"
            + " { ?bs2 foaf:name ?nomeFermata. }\n"
            + "} ORDER BY ?nomeFermata";  
  }
  
  static public String latLngToAddressQuery(String lat, String lng, String sparqlType) {
    return prefixes
            + "SELECT DISTINCT ?via ?numero ?comune	(?nc as ?uriCivico) ?uriComune WHERE {\n"
            + " ?entry rdf:type km4c:Entry.\n"
            + " ?nc km4c:hasExternalAccess ?entry.\n"
            + " ?nc km4c:extendNumber ?numero.\n"
            + " ?nc km4c:belongToRoad ?road.\n"
            + " ?road km4c:extendName ?via.\n"
            + " ?entry geo:lat ?elat.\n"
            + " ?entry geo:long ?elong.\n"
            + " ?road km4c:inMunicipalityOf ?uriComune.\n"
            + " ?uriComune foaf:name ?comune.\n"
            + (sparqlType.equals("virtuoso") ? 
               //" ?ser geo:geometry ?geo.  filter(bif:st_distance(?geo, bif:st_point ("+longitudine+","+latitudine+"))<= "+raggioBus+")" :
                 " ?entry geo:geometry ?geo.  filter(bif:st_intersects (?geo, bif:st_point ("+lng+","+lat+"), 0.1))\n" 
               + " BIND( bif:st_distance(?geo, bif:st_point(" + lng + ", " + lat + ")) AS ?dist)\n":
                 " ?entry omgeo:nearby(" + lat + " " + lng + " \"0.1km\").\n"
               + " BIND( omgeo:distance(?elat, ?elong, " + lat + ", " + lng + ") AS ?dist)\n")
            + "} ORDER BY ?dist "
            + "LIMIT 1";
  }
  
  static public String latLngToMunicipalityQuery(String lat, String lng, String sparqlType) {
    return prefixes
            + "SELECT DISTINCT ?comune ?uriComune WHERE {\n"
            + " ?entry rdf:type km4c:Entry.\n"
            + " ?nc km4c:hasExternalAccess ?entry.\n"
            + " ?nc km4c:belongToRoad ?road.\n"
            + " ?entry geo:lat ?elat.\n"
            + " ?entry geo:long ?elong.\n"
            + " ?road km4c:inMunicipalityOf ?uriComune.\n"
            + " ?uriComune foaf:name ?comune.\n"
            + (sparqlType.equals("virtuoso") ? 
               //" ?ser geo:geometry ?geo.  filter(bif:st_distance(?geo, bif:st_point ("+longitudine+","+latitudine+"))<= "+raggioBus+")" :
                 " ?entry geo:geometry ?geo.  filter(bif:st_intersects (?geo, bif:st_point ("+lng+","+lat+"), 5))\n" 
               + " BIND( bif:st_distance(?geo, bif:st_point(" + lng + ", " + lat + ")) AS ?dist)\n":
                 " ?entry omgeo:nearby(" + lat + " " + lng + " \"5km\").\n"
               + " BIND( omgeo:distance(?elat, ?elong, " + lat + ", " + lng + ") AS ?dist)\n")
            + "} ORDER BY ?dist "
            + "LIMIT 1";
  }
  
  static public String servicesNearLatLongQuery(String lat, String lng) {
    return "";
  }
  
  static public ArrayList<String> getTypes(RepositoryConnection con, String uri) {
    return ServiceMap.getTypes(con, uri, true);
  }
  
  static public ArrayList<String> getTypes(RepositoryConnection con, String uri, boolean inference) {
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "");
    
    String queryString
            = "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
            + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
            + "SELECT DISTINCT ?type WHERE{"
            + " <" + uri + "> rdf:type ?type"+(sparqlType.equals("virtuoso") && inference ? " OPTION (inference \"urn:ontology\")":"")+".\n"
            + "}";

    ArrayList<String> types=new ArrayList<String>();
    try {
      TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      long ts = System.nanoTime();
      TupleQueryResult result = tupleQuery.evaluate();
      logQuery(queryString, "get-types", sparqlType, uri+";"+inference,System.nanoTime()-ts);
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String type = bindingSet.getValue("type").stringValue();
        type = type.replace("http://www.disit.org/km4city/schema#", "");
        types.add(type);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return types;
  }

  static public String filterServices(List<String> listaCategorie) throws Exception {      
    String filtroQuery = "";
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "");
    String km4cVersion = conf.get("km4cVersion", "");
    String urlMySqlDB = conf.get("urlMySqlDB", "");
    String dbMySql = conf.get("dbMySql", "");
    String userMySql = conf.get("userMySql", "");
    String passMySql = conf.get("passMySql", "");

    if(!km4cVersion.equals("old")) {
      /*int n=0;
      for(String c:listaCategorie) {
        if(!c.equals("Service") && !c.equals("NearBusStops") && !c.equals("RoadSensor")) {
          if(n>0)
            filtroQuery += "UNION";
          filtroQuery += " { ?ser a km4c:"+(c.substring(0, 1).toUpperCase()+c.substring(1))+(sparqlType.equals("virtuoso")? " OPTION (inference \"urn:ontology\")":"")+".}\n";
          n++;
        }
      }*/
      //if(listaCategorie.contains("theatre"))
      //  listaCategorie.add("teathre"); //FIX DA RIMUOVERE
      String microCat="";
      String mCats[] = {"Accommodation", "Advertising", "AgricultureAndLivestock", "CivilAndEdilEngineering", "CulturalActivity", "EducationAndResearch", "Emergency", "Environment", "Entertainment", "FinancialService",
        "GovernmentOffice", "HealthCare", "IndustryAndManufacturing", "MiningAndQuarrying", "ShoppingAndService", "TourismService", "TransferServiceAndRenting", "UtilitiesAndSupply", "Wholesale", "WineAndFood"};
      List<String> macroCats=Arrays.asList(mCats); //TODO prendere da query e fare caching
      int n=0;
      for(String c:listaCategorie) {
        //FIX temporaneo
        c = c.trim();
        if(macroCats.contains(c)) {
          if(n>0)
            filtroQuery += "UNION";
          filtroQuery += " { ?ser a km4c:"+(c.substring(0, 1).toUpperCase()+c.substring(1))+(sparqlType.equals("virtuoso")? " OPTION (inference \"urn:ontology\")":"")+".}\n";
          n++;
        }
        else if(!c.equals("Service") && !c.equals("BusStop") && !c.equals("SensorSite") && !c.equals("Event") && !c.equals("PublicTransportLine")) {
          if(microCat.length()>0)
            microCat+=",";
          microCat+="km4c:"+(c.substring(0, 1).toUpperCase()+c.substring(1))+"\n";
        }
      }
      if(microCat.length()>0) {
        if(filtroQuery.length()>0) {
          filtroQuery += "UNION ";
        }
        filtroQuery += "{ ?ser a ?t. FILTER(?t IN ("+microCat+"))}\n";
      }
      return filtroQuery;          
    }

    Connection conMySQL = null;
    Statement st = null;
    ResultSet rs = null;

    //Class.forName("com.mysql.jdbc.Driver");
    conMySQL = ConnectionPool.getConnection(); //DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);;

    String query = "SELECT distinct Ita, Eng FROM SiiMobility.ServiceCategory ORDER BY SubClasse";

    // create the java statement
    st = conMySQL.createStatement();

    // execute the query, and get a java resultset
    rs = st.executeQuery(query);
    int categorieInserite = 0;
    while (rs.next()) {
        //String id = rs.getString("ID");
        String nomeEng = rs.getString("Eng");
        String nomeIta = rs.getString("Ita");
        String NomeIta = Character.toUpperCase(nomeIta.charAt(0)) + nomeIta.substring(1);
        NomeIta = NomeIta.replace("_", " ");

        if (listaCategorie.contains(nomeEng) || listaCategorie.contains(nomeIta)) {
            if (categorieInserite == 0) {
                filtroQuery += " { ?ser km4c:hasServiceCategory <http://www.disit.org/km4city/resource/" + nomeEng + ">.\n";
                filtroQuery += " BIND (\"" + nomeEng + "\"^^xsd:string AS ?sType).\n";
                filtroQuery += " BIND (\"" + NomeIta + "\"^^xsd:string AS ?sTypeIta) }\n";
            } else {
                filtroQuery += " UNION {?ser km4c:hasServiceCategory <http://www.disit.org/km4city/resource/" + nomeEng + ">.\n";
                filtroQuery += " BIND (\"" + nomeEng + "\"^^xsd:string AS ?sType).\n";
                filtroQuery += " BIND (\"" + NomeIta + "\"^^xsd:string AS ?sTypeIta)}\n";
            }
            categorieInserite++;
        }
    }
    st.close();
    conMySQL.close();
    return filtroQuery;
  }
  
  static public String textSearchQueryFragment(String subj, String pred, String textToSearch) throws Exception {
    if(textToSearch==null || textToSearch.equals(""))
      return "";
    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    boolean removeStopWords = conf.get("removeStopWords", "true").equals("true");

    loadStopWords();

    int n = 0;
    if (sparqlType.equals("virtuoso")) {
        String[] s = textToSearch.split(" +");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length; i++) {
          String word = s[i].trim();
          if(!removeStopWords || !stopWords.contains(word)) {
            if(n>0)
              sb.append(" and ");
            sb.append("'"+word+"'");
            n++;
          }
        }
        textToSearch = sb.toString();
        if("".equals(textToSearch))
          return "";
    }
    return " "+subj+" "+pred+" ?txt.\n"
       + (sparqlType.equals("owlim")
          ? " ?txt luc:myIndex \"" + textToSearch + "\".\n"
          + " ?txt luc:score ?sc.\n"
          : " ?txt bif:contains \"" + textToSearch + "\" OPTION (score ?sc).\n");
  }
  
  static private void loadStopWords() {
    if(stopWords != null)
      return; //already loaded;

    stopWords = new ArrayList<String>();
    String token1 = "";
    try {
      // create Scanner
      Scanner scanner = new Scanner(new File(System.getProperty("user.home")+"/servicemap/stop_words.txt"));
      while (scanner.hasNext()) {
        // find next line
        token1 = scanner.next();
        //System.out.println("-"+token1+"-");
        stopWords.add(token1);
      }
      scanner.close();    
    } catch (FileNotFoundException ex) {
      System.out.println("Exception: File not found "+ex.toString());
    }
  }
  
  static public String geoSearchQueryFragment(String subj, String lat, String lng, String dist) {
    if (lat == null || lng == null || dist == null) {
      return "";
    }

    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");

    return (sparqlType.equals("virtuoso")
            ? " " + subj + " geo:geometry ?geo.  filter(bif:st_distance(?geo, bif:st_point (" + lng + "," + lat + "))<= " + dist + ")\n"
            + " BIND(bif:st_distance(?geo, bif:st_point (" + lng + "," + lat + ")) AS ?dist)\n"
            //"  ?entry geo:geometry ?geo.  filter(bif:st_intersects (?geo, bif:st_point ("+lng+","+lat+"), "+dist+"))\n" :
            : " " + subj + " omgeo:nearby(" + lat + " " + lng + " \"" + dist + "km\") .\n"); //ATTENZIONE per OWLIM non viene aggiunto il BIND
  }
  
  static public String geoSearchQueryFragment(String subj, String[] coords, String dist) throws IOException, SQLException {
    if (coords==null || (!coords[0].startsWith("wkt:") && !coords[0].startsWith("geo:") && coords.length!=2 && coords.length!=4) || dist == null ) {
      return "";
    }

    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");

    if(coords.length==1 && (coords[0].startsWith("wkt:") || coords[0].startsWith("geo:"))) {
      String geom = coords[0].substring(4);
      if(coords[0].startsWith("geo:")) {
        //cerca su tabella la geometria dove fare ricerca
        Connection conMySQL = ConnectionPool.getConnection();
        Statement st = conMySQL.createStatement();
        ResultSet rs = st.executeQuery("SELECT wkt FROM ServiceMap.Geometry WHERE label='"+geom+"'");
        if (rs.next()) {
          geom = rs.getString("wkt");
        }
        else 
          throw new IOException("geometry "+geom+" not found");
        st.close();
        conMySQL.close();
      }
      return  " " + subj + " geo:geometry ?geo.  filter(bif:st_intersects (?geo, bif:st_geomfromtext(\""+geom+"\"), 0.0005))\n"
              + " BIND( 1.0 AS ?dist)\n";
    }

    String lat = coords[0];
    String lng = coords[1];
    
    if(coords.length==2) {
      return (sparqlType.equals("virtuoso")
              ? " " + subj + " geo:geometry ?geo.  filter(bif:st_distance(?geo, bif:st_point (" + lng + "," + lat + "))<= " + dist + ")\n"
              //"  " + subj + " geo:geometry ?geo.  filter(bif:st_intersects (?geo, bif:st_point ("+lng+","+lat+"), "+dist+"))\n"
              + " BIND(bif:st_distance(?geo, bif:st_point (" + lng + "," + lat + ")) AS ?dist)\n"
              : " " + subj + " omgeo:nearby(" + lat + " " + lng + " \"" + dist + "km\") .\n"); //ATTENZIONE per OWLIM non viene aggiunto il BIND
    }
    String lat2 = coords[2];
    String lng2 = coords[3];
    return (sparqlType.equals("virtuoso")
            ? " " + subj + " geo:geometry ?geo.  filter(bif:st_x(?geo)>=" + lng + " && bif:st_x(?geo)<=" + lng2 + " && bif:st_y(?geo)>=" + lat + " && bif:st_y(?geo)<=" + lat2 + ")\n"
            + " BIND(bif:st_distance(?geo, bif:st_point (" + (Float.parseFloat(lng)+Float.parseFloat(lng2))/2. + "," + (Float.parseFloat(lat)+Float.parseFloat(lat2))/2. + ")) AS ?dist)\n"
            //"  ?entry geo:geometry ?geo.  filter(bif:st_intersects (?geo, bif:st_point ("+lng+","+lat+"), "+dist+"))\n" :
            : " " + subj + " omgeo:nearby(" + lat + " " + lng + " \"" + dist + "km\") .\n"); //ATTENZIONE per OWLIM non viene aggiunto il BIND    
  }
  
  static public String toFloat(String a, String b) {
    //return " (bif:sprintf(\"%.10f\","+a+") AS "+b+")";
    return " ("+a+" AS "+b+")";
  }
  
  static public int countQuery(RepositoryConnection con, String query) throws Exception {
    //return -1;
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
    TupleQueryResult result = tupleQuery.evaluate();
    if(result.hasNext()) {
      BindingSet binding = result.next();
      String count = binding.getValue("count").stringValue();
      return Integer.parseInt(count);
    }
    return 0;
  }
  
  static public boolean sendEmail(String dest, String subject, String msg) {
    boolean emailSent;
    String to[] = dest.split(";");
    Properties properties = System.getProperties();
    Configuration conf=Configuration.getInstance();
    properties.put("mail.smtp.host", conf.get("smtp", "musicnetwork.dsi.unifi.it"));
    properties.put("mail.smtp.port", conf.get("portSmtp","25"));
    Session mailSession = Session.getDefaultInstance(properties);
    try {
      MimeMessage message = new MimeMessage(mailSession);
      message.setFrom(new InternetAddress(conf.get("mailFrom","info@disit.org")));
      for(String t: to)
        message.addRecipient(Message.RecipientType.TO,
              new InternetAddress(t));
      message.setSubject(subject);
      message.setContent(msg, "text/plain; charset=utf-8");
      Transport.send(message);
      emailSent = true;
    } catch (MessagingException mex) {
      mex.printStackTrace();
      emailSent = false;
    }        
    return emailSent;
  }
  
  static public String getServiceName(String serviceUri) throws Exception {
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String serviceName = getServiceName(con, serviceUri);
    con.close();
    return serviceName;
  }
  
  static public String getServiceName(RepositoryConnection con, String serviceUri) throws Exception {
    String serviceName = null;
    TupleQuery tq = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE {{<"+serviceUri+"> <http://schema.org/name> ?name}UNION{<"+serviceUri+"> <http://xmlns.com/foaf/0.1/name> ?name}} LIMIT 1");
    TupleQueryResult result = tq.evaluate();
    if(result.hasNext()) {
      BindingSet binding = result.next();
      serviceName = binding.getValue("name").stringValue();
    }
    return serviceName;
  }
  
  static public String getServiceIdentifier(RepositoryConnection con, String serviceUri) throws Exception {
    String serviceId = null;
    TupleQuery tq = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE {<"+serviceUri+"> <http://purl.org/dc/terms/identifier> ?id} LIMIT 1");
    TupleQueryResult result = tq.evaluate();
    if(result.hasNext()) {
      BindingSet binding = result.next();
      serviceId = binding.getValue("id").stringValue();
    }
    return serviceId;
  }
  
  static public Map<String,String> getServiceInfo(String idService, String lang) throws Exception {
    Map<String,String> info = new HashMap<>();
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String queryService = "PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
            + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX schema:<http://schema.org/>\n"
            + "PREFIX skos:<http://www.w3.org/2004/02/skos/core#>\n"
            + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
            + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
            + "PREFIX gtfs:<http://vocab.gtfs.org/terms#>\n"
            + "SELECT ?name ?lat ?long ?type ?category ?typeLabel ?ag ?agname WHERE{\n"
            + " {\n"
            + "  <" + idService + "> km4c:hasAccess ?entry.\n"
            + "  ?entry geo:lat ?lat.\n"
            + "  ?entry geo:long ?long.\n"
            + " }UNION{\n"
            + "  <" + idService + "> km4c:isInRoad ?road.\n"
            + "  <" + idService + "> geo:lat ?lat.\n"
            + "  <" + idService + "> geo:long ?long.\n"
            + " }UNION{\n"
            + "  <" + idService + "> geo:lat ?lat.\n"
            + "  <" + idService + "> geo:long ?long.\n"
            + " }\n"
            + " {<" + idService + "> schema:name ?name.}UNION{<" + idService + "> foaf:name ?name.}UNION{<" + idService + "> dcterms:identifier ?name.}\n"
            + " <" + idService + "> a ?type . FILTER(?type!=km4c:RegularService && ?type!=km4c:Service && ?type!=km4c:DigitalLocation)\n"
            + " ?type rdfs:label ?typeLabel. FILTER(LANG(?typeLabel) = \""+lang+"\")\n"
            + " OPTIONAL{?type rdfs:subClassOf ?category FILTER(STRSTARTS(STR(?category),\"http://www.disit.org/km4city/schema#\"))}.\n"
            + " OPTIONAL {\n"
            + "  {?st gtfs:stop <"+idService+">.}UNION{?st gtfs:stop [owl:sameAs <"+idService+">]}\n" 
            + "  ?st gtfs:trip ?t.\n"
            +"   ?t gtfs:route/gtfs:agency ?ag.\n"
            +"   ?ag foaf:name ?agname.\n"
            + " }\n"
            + "} LIMIT 1";
    TupleQuery tq = con.prepareTupleQuery(QueryLanguage.SPARQL, queryService);
    TupleQueryResult result = tq.evaluate();
    if(result.hasNext()) {
      BindingSet binding = result.next();
      info.put("serviceName", binding.getValue("name").stringValue());
      info.put("lat", binding.getValue("lat").stringValue());
      info.put("long", binding.getValue("long").stringValue());
      info.put("typeLabel", binding.getValue("typeLabel").stringValue());
      if(binding.getValue("category")!=null)
        info.put("serviceType", makeServiceType(binding.getValue("category").stringValue(), binding.getValue("type").stringValue()));
      else
        info.put("serviceType", binding.getValue("type").stringValue().replace("http://www.disit.org/km4city/schema#", ""));
      if(binding.getValue("ag")!=null) {
        info.put("agencyUri", binding.getValue("ag").stringValue());
        info.put("agency", binding.getValue("agname").stringValue());
      }
    }
    con.close();
    
    return info;
  }
  
  static public String map2Json(Map<String,String> info) throws Exception {
    String json  = "";
    for(String k:info.keySet()) {
      json += "\""+k+"\":\""+escapeJSON(info.get(k))+"\", ";
    }
    return json;
  }
  
  static public Map<String,String> getUriInfo(String uri) throws Exception {
    Map<String,String> info = new HashMap<>();
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String query="SELECT * WHERE { <"+uri+"> ?p ?o }";
    TupleQuery tq = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
    TupleQueryResult result = tq.evaluate();
    while(result.hasNext()) {
      BindingSet binding = result.next();
      String p = binding.getValue("p").stringValue();
      String o = binding.getValue("o").stringValue();
      info.put(p,o);
    }
    con.close();
    return info;
  }
  
  static public String makeServiceType(String category, String type) {
    return category.replace("http://www.disit.org/km4city/schema#", "")+"_"+type.replace("http://www.disit.org/km4city/schema#", "");
  }
  
  static public String getServicePhotos(String uri) throws IOException,SQLException {
    return getServicePhotos(uri, "");
  }
  
  static public String getServicePhotos(String uri, String type) throws IOException,SQLException {
    String json = "[";
    Connection connection = ConnectionPool.getConnection();
    String baseApiUrl = Configuration.getInstance().get("baseApiUrl", "");
    if(!"".equals(type) && !type.endsWith("/"))
      type += "/";
    
    try {
      int i=0;
      PreparedStatement st = connection.prepareStatement("SELECT file FROM ServicePhoto WHERE serviceUri=? AND status='validated'");
      st.setString(1, uri);
      ResultSet rs = st.executeQuery();
      while(rs.next()) {
        json += (i>0?",":"")+"\""+baseApiUrl+"v1/photo/"+type+rs.getString("file")+"\"";
        i++;
      }
      json +="]";
      st.close();
      connection.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    return json;
  }
  
  static public float[] getAvgServiceStars(String serviceUri) throws IOException,SQLException {
    float[] r = new float[2];
    r[0]=0;
    r[1]=0;
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("SELECT avg(stars) as avgStars, count(*) as count FROM ServiceStars WHERE serviceUri=?");
      st.setString(1, serviceUri);
      ResultSet rs = st.executeQuery();
      if(rs.next()) {
        r[0] = rs.getFloat("avgStars");
        r[1] = rs.getInt("count");
      }
      st.close();
      connection.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    return r;
  }

  static public int getServiceStarsByUid(String serviceUri, String uid) throws IOException,SQLException {
    int stars = 0;
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("SELECT stars FROM ServiceStars WHERE serviceUri=? AND uid=?");
      st.setString(1, serviceUri);
      st.setString(2, uid);
      ResultSet rs = st.executeQuery();
      if(rs.next()) {
        stars = rs.getInt("stars");
      }
      st.close();
      connection.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    return stars;
  }
  
  static public String getServiceComments(String serviceUri) throws IOException,SQLException {
    String json = "[";
    Connection connection = ConnectionPool.getConnection();
    String baseApiUrl = Configuration.getInstance().get("baseApiUrl", "");
    try {
      int i=0;
      PreparedStatement st = connection.prepareStatement("SELECT comment, timestamp FROM ServiceComment WHERE serviceUri=? AND status='validated' ORDER BY timestamp");
      st.setString(1, serviceUri);
      ResultSet rs = st.executeQuery();
      while(rs.next()) {
        json += (i>0?",":"")+"{\"text\":\""+escapeJSON(rs.getString("comment"))+"\", \"timestamp\":\""+rs.getString("timestamp")+"\"}";
        i++;
      }
      json +="]";
      st.close();
      connection.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    return json;
  }
  
  public static String fixWKT(String wkt) {
    return wkt.replace("((", "(").replace("))", ")");
  }
  
  public static String makeLOGUri(String uri) {
    Configuration conf=Configuration.getInstance();
    String logEndPoint = conf.get("logEndPoint","http://log.disit.org/service/?sparql=http://192.168.0.207:8080/ServiceMap/sparql&uri=");

    return logEndPoint+uri;
  }
    
  public static String cleanCategories(String categorie) {
    if (categorie != null && !"".equals(categorie)) {
      String[] arrayCategorie = categorie.split(";");
      categorie = "";
      for(int i=0; i<arrayCategorie.length; i++) {
        String v = arrayCategorie[i].trim();
        if(v.equals(""))
          arrayCategorie[i]=null;
        else {
          if(i!=0)
            categorie += ";"+v;
          else
            categorie += v;
        }
      }
    }
    return categorie;
  }
  
  private static final String[] HEADERS_TO_TRY = { 
      "X-Forwarded-For",
      "Proxy-Client-IP",
      "WL-Proxy-Client-IP",
      "HTTP_X_FORWARDED_FOR",
      "HTTP_X_FORWARDED",
      "HTTP_X_CLUSTER_CLIENT_IP",
      "HTTP_CLIENT_IP",
      "HTTP_FORWARDED_FOR",
      "HTTP_FORWARDED",
      "HTTP_VIA",
      "REMOTE_ADDR" };

  public static String getClientIpAddress(HttpServletRequest request) {
      for (String header : HEADERS_TO_TRY) {
          String ip = request.getHeader(header);
          if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip) && !"127.0.0.1".equals(ip)) {
              return ip;
          }
      }
      return request.getRemoteAddr();
  }
  
  public static String[] parsePosition(String position) throws Exception {
    if(position==null)
      return null;
    String[] latLng = position.split(";");
    if(latLng.length==2)
      return latLng;
    if(!position.startsWith("http://")) {
      return null;
    }
    latLng = new String[2];
    Map<String,String> info = getServiceInfo(position,"en");
    if(info.isEmpty())
      return null;
    latLng[0] = info.get("lat");
    latLng[1] = info.get("long");
    return latLng;
  }
  
  public static String[] findTime(String s) {
    String[] times=new String[2];
    if(s==null || s.isEmpty())
      return times;
    Pattern r = Pattern.compile("[^0-9]([0-9]?[0-9])[\\.:,]([0-5][0-9])\\s*-\\s*([0-9]?[0-9])[\\.:,]([0-5][0-9])[^0-9]");
    Matcher m = r.matcher(" "+s+" ");
    int pos=0;
    System.out.println("-- "+s);
    int i=0;
    if(m.find(pos)) {
      System.out.println(">> Found: " + m.group(1)+":"+m.group(2)+"-"+m.group(3)+":"+m.group(4));
      String c = m.group(1);
      if(c.length()<2)
        c="0"+c;
      times[i] = c+":"+m.group(2);
      if(times[i].compareTo("00:00")>=0 && times[i].compareTo("23:59")<=0) {
        c = m.group(3);
        if(c.length()<2)
          c="0"+c;
        times[i+1] = c+":"+m.group(4);
        if(times[i+1].compareTo("00:00")>=0 && times[i+1].compareTo("23:59")<=0) {
          i += 2;
        } else
          times[i+1]=null;
      } else
        times[i]=null;
    }
    if(i==0) {
      r = Pattern.compile("[^0-9]([0-9]?[0-9])[\\.:,]([0-5][0-9])[^0-9]");
      m = r.matcher(" "+s+" ");
      if(m.find()) {
        System.out.println(">> Found: " + m.group(1)+":"+m.group(2));
        String c = m.group(1);
        if(c.length()<2)
          c="0"+c;
        times[i] = c+":"+m.group(2);
        if(!(times[i].compareTo("00:00")>=0 && times[i].compareTo("23:59")<=0)) {
          times[i] = null;
        }
      }
    }
    return times;
  }
  
  public static boolean validateUID(String uid) {
    if(uid==null || (uid.length()!=64 && uid.length()!=47))
      return false;
    if(uid.equals("null") || uid.matches("[0-9a-fA-F]*"))
      return true;
    return false;
  }
  
  public static String replaceHTMLEntities(String s) {
    return s.replace("&agrave;", "à")
            .replace("&egrave;", "è")
            .replace("&ugrave;", "ù")
            .replace("&igrave;", "ì")
            .replace("&ograve;", "ò")
            .replace("&eacute;", "é")
            .replace("&aacute;", "á")
            .replace("&eacuto;", "é")
            .replace("&aacuto;", "á");
  }
}
