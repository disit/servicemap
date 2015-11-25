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

package org.disit.servicemap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.asList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.jsp.JspWriter;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 * @author bellini
 */
public class ServiceMap {
  
  static private List<String> stopWords = null;
  
  static public void initLogging() {
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
      FileWriter fileWritter = new FileWriter(file.getAbsolutePath(),true);
      BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
      bufferWritter.write(("#QUERYID:"+id+":"+type+":"+args+":"+(time/1000000)+":"+now+":"+query+"\n#####################\n").replace("\n", "\r\n"));
      bufferWritter.close();
      //System.out.println("#QUERYID:"+id+":"+type+":"+args+":"+(time/1000000));
      //System.out.println(query);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  static public String escapeJSON(String s) {
    return s.replace("\"", "\\\"").replace("\t", "\\t");
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
            + "SELECT DISTINCT ?via ?numero ?comune	?uriComune WHERE {\n"
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
      TupleQueryResult result = tupleQuery.evaluate();
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
  
  static public String geoSearchQueryFragment(String subj, String[] coords, String dist) {
    if (coords==null || (coords.length!=2 && coords.length!=4) || dist == null) {
      return "";
    }

    Configuration conf = Configuration.getInstance();
    String sparqlType = conf.get("sparqlType", "virtuoso");
    String lat = coords[0];
    String lng = coords[1];
    
    if(coords.length==2) {
      return (sparqlType.equals("virtuoso")
              ? " " + subj + " geo:geometry ?geo.  filter(bif:st_distance(?geo, bif:st_point (" + lng + "," + lat + "))<= " + dist + ")\n"
              + " BIND(bif:st_distance(?geo, bif:st_point (" + lng + "," + lat + ")) AS ?dist)\n"
              //"  ?entry geo:geometry ?geo.  filter(bif:st_intersects (?geo, bif:st_point ("+lng+","+lat+"), "+dist+"))\n" :
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
}
