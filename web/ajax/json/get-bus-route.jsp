<%@page import="java.io.IOException"%>
<%@page import="org.openrdf.model.Value"%>
<%@ page import="java.util.*"%>
<%@ page import="org.openrdf.repository.Repository"%>
<%@ page import="org.openrdf.repository.sparql.SPARQLRepository"%>
<%@ page import="java.sql.*"%>
<%@ page import="java.util.List"%>
<%@ page import="org.openrdf.query.BooleanQuery"%>
<%@ page import="org.openrdf.OpenRDFException"%>
<%@ page import="org.openrdf.repository.RepositoryConnection"%>
<%@ page import="org.openrdf.query.TupleQuery"%>

<%@ page import="org.openrdf.query.TupleQueryResult"%>
<%@ page import="org.openrdf.query.BindingSet"%>
<%@ page import="org.openrdf.query.QueryLanguage"%>
<%@ page import="java.io.File"%>
<%@ page import="java.net.URL"%>
<%@ page import="org.openrdf.rio.RDFFormat"%>
<%@include file= "/include/parameters.jsp" %>
<%
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


  //Repository repo = new SPARQLRepository(sparqlEndpoint);
  //repo.initialize();

/* CODICE MARCO TEMPORANEAMENTE NON UTILIZZATO */
/*  
  String numeroRoute = request.getParameter("numeroRoute");

//out.println(filtroBounds);
  RepositoryConnection con = repo.getConnection();
  //query modificata marco
  String queryStringPrimeDueFermate = "PREFIX km4c:<http://www.disit.org/km4city/schema#> "
          + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
          + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> 	 "
          + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
          + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> 	 "
          + "PREFIX schema:<http://schema.org/#>"
          + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> 	 "
          + "PREFIX dcterms:<http://purl.org/dc/terms/>  "
          + "SELECT distinct ?bs1 ?bs2	?nomeFermata1 ?nomeFermata2 ?bslat1 ?bslong1 ?bslat2 ?bslong2 "
          + "WHERE { 	 "
          + "?tplr rdf:type km4c:Route . "
          + "?tplr dcterms:identifier \"" + numeroRoute + "\"^^xsd:string . "
          + "?tplr km4c:hasFirstSection ?rs . "
          + "?rs km4c:startsAtStop ?bs1 . "
          + "?rs km4c:endsAtStop ?bs2 . "
          + "?bs1 foaf:name ?nomeFermata1 . "
          + "?bs2 foaf:name ?nomeFermata2 . "
          + "?bs1 geo:lat ?bslat1 . "
          + "?bs1 geo:long ?bslong1 . "
          + "?bs2 geo:lat ?bslat2 . "
          + "?bs2 geo:long ?bslong2 . "
          + "}  "
          + "LIMIT 100";
  //query modificata marco
  //out.println(queryString);

  TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryStringPrimeDueFermate));
  logQuery(filterQuery(queryStringPrimeDueFermate),"get-bus-route","any");

  TupleQueryResult result = tupleQuery.evaluate();

  out.println("{ "
          + "\"type\": \"FeatureCollection\", "
          + "\"features\": [ ");

  try {

    if (result.hasNext()) {
      BindingSet bindingSet = result.next();

      String valueOfBS1 = bindingSet.getValue("bs1").stringValue();
      String valueOfBS2 = bindingSet.getValue("bs2").stringValue();

      String valueOfNomeFermata1 = bindingSet.getValue("nomeFermata1").stringValue();
      String valueOfBSLat1 = bindingSet.getValue("bslat1").stringValue();
      String valueOfBSLong1 = bindingSet.getValue("bslong1").stringValue();


      String valueOfNomeFermata2 = bindingSet.getValue("nomeFermata2").stringValue();
      String valueOfBSLat2 = bindingSet.getValue("bslat2").stringValue();
      String valueOfBSLong2 = bindingSet.getValue("bslong2").stringValue();

      out.println("{ "
              + " \"geometry\": {  "
              + "     \"type\": \"LineString\",  "
              + "    \"coordinates\": [  "
              + "      [ " + valueOfBSLong1 + ",  "
              + "      " + valueOfBSLat1 + "],  "
              + "      [ " + valueOfBSLong2 + ",  "
              + "      " + valueOfBSLat2 + "],  ");

                  // SECONDA PARTE
      String fermataDiPartenza = valueOfBS2;

      int i = 0;
      //query modificata marco
      while (fermataDiPartenza != null) {
        String querySecondaParte = "PREFIX km4c:<http://www.disit.org/km4city/schema#> "
                + "PREFIX km4cr:<http://www.disit.org/km4city/resource#>"
                + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> 	 "
                + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
                + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> 	 "
                + "PREFIX schema:<http://schema.org/#> 	 "
                + "PREFIX foaf:<http://xmlns.com/foaf/0.1/> 	 "
                + "PREFIX dcterms:<http://purl.org/dc/terms/>  "
                + "SELECT distinct ?bs2  ?nomeFermata2 ?bslat2 ?bslong2 "
                + "WHERE { 	 "
                + "?tplr rdf:type km4c:Route . "
                + "?tplr dcterms:identifier \"" + numeroRoute + "\"^^xsd:string . "
                + "?tplr km4c:hasSection ?rs . "
                + "?rs km4c:startsAtStop <" + fermataDiPartenza + "> . "
                + "?rs km4c:endsAtStop ?bs2 . "
                + "?bs2 foaf:name ?nomeFermata2 . "
                + "?bs2 geo:lat ?bslat2 . "
                + "?bs2 geo:long ?bslong2 . "
                + "}  "
                + "LIMIT 100 ";

                  //query modificata marco
        TupleQuery tupleQuerySecondaParte = con.prepareTupleQuery(QueryLanguage.SPARQL, querySecondaParte);
        TupleQueryResult resultSecondaParte = tupleQuerySecondaParte.evaluate();

        if (resultSecondaParte.hasNext()) {

          BindingSet bindingSetSecondaParte = resultSecondaParte.next();

          valueOfBS2 = bindingSetSecondaParte.getValue("bs2").stringValue();
          valueOfNomeFermata2 = bindingSetSecondaParte.getValue("nomeFermata2").stringValue();
          valueOfBSLat2 = bindingSetSecondaParte.getValue("bslat2").stringValue();
          valueOfBSLong2 = bindingSetSecondaParte.getValue("bslong2").stringValue();

          if (i == 0) {
            out.println("      [ " + valueOfBSLong2 + ",  "
                    + "      " + valueOfBSLat2 + "]  ");
          } else {
            out.println("   ,   [ " + valueOfBSLong2 + ",  "
                    + "      " + valueOfBSLat2 + "]  ");
          }

          fermataDiPartenza = valueOfBS2;
          i++;
        } else {
          fermataDiPartenza = null;
        }
      }
      out.println(" ]  "
              + "},  "
              + "\"type\": \"Feature\",  "
              + "\"properties\": {  "
              + "    \"popupContent\": \"" + valueOfNomeFermata1 + "\" "
              + "}, "
              + "\"id\": " + Integer.toString(i + 1) + " "
              + "}");
    }
  } catch (Exception e) {
    out.println(e.getMessage());
  } finally {
    con.close();
  }

  out.println("] "
          + "}");
 */

    
    
  Repository repo = new SPARQLRepository(sparqlEndpoint);
    repo.initialize();
    RepositoryConnection con = repo.getConnection();
    String numLinea = request.getParameter("numLinea");

    String queryString = " PREFIX km4c:<http://www.disit.org/km4city/schema#>\n"
              + "PREFIX km4cr:<http://www.disit.org/km4city/resource/>\n"
              + "PREFIX schema:<http://schema.org/#>\n"
              + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
              + "PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
              + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "PREFIX vcard:<http://www.w3.org/2006/vcard/ns#>\n"
              + "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
              + "PREFIX dcterms:<http://purl.org/dc/terms/>\n"
              + "PREFIX opengis:<http://www.opengis.net/ont/geosparql#>\n"
              + "SELECT DISTINCT ?dir ?code ?bsFirst ?bsLast ?rName ?x WHERE{\n"
              + " ?tpll rdf:type km4c:PublicTransportLine.\n"
              + " ?tpll dcterms:identifier \"" + numLinea + "\".\n"
              + " ?tpll km4c:hasRoute ?route.\n"
              + " ?route km4c:hasFirstStop ?bss.\n"
              + " ?route km4c:hasLastStop ?bse.\n"
              + " ?route km4c:direction ?dir.\n"
              + " ?route dcterms:identifier ?code.\n"
              + " ?route foaf:name ?rName.\n"
              + " ?route opengis:hasGeometry ?geometry .\n"
              + " ?geometry opengis:asWKT ?polyline .\n"
              + " ?bss foaf:name ?bsFirst.\n"
              + " ?bse foaf:name ?bsLast.\n"
              + "} ORDER BY ?line ?dir ";

    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(filterQuery(queryString),"get-bus-route","any",numLinea);
    
    try{
        if(result.hasNext() == false){
        out.println("<div class=\"message\">");
        out.println("<b>Bus Route under development.</b></div>");
        }else{
        out.println("<div class=\"Route\">");
        out.println("<b>Paths:</b>");
        out.println("<table>");
        out.println("<tr>");
        out.println("<td><b>Line</b></td>");
        out.println("<td><b>Route</b></td>");
        //out.println("<td><b>Codice</b></td>");
        out.println("</tr>");
        }
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            out.println("<tr>");
            String bsFirst = bindingSet.getValue("bsFirst").stringValue();
            String bsLast = bindingSet.getValue("bsLast").stringValue();
            String code = bindingSet.getValue("code").stringValue();
            String nomeLinea = bindingSet.getValue("rName").stringValue();
            // PROVA ESTRAZIONE LETTERA LINEA BUS ATAF
        /*Class.forName("com.mysql.jdbc.Driver");
        conMySQL = ConnectionPool.getConnection(); //DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
        String query = "SELECT routeLetter FROM Codici_route_ataf where codRoute = '"+code+"'";
        // create the java statement
        st = conMySQL.createStatement();
        // execute the query, and get a java resultset
        rs = st.executeQuery(query);
        String letter = "";
        // iterate through the java resultset
        while (rs.next()) {
            if(!(rs.getString("routeLetter")).equals("_")){
            letter = rs.getString("routeLetter");
            }
        }*/
            String direction = bsFirst+" &#10132; "+bsLast;
            //if(new File("../../ServiceMap/web/ajax/xml/linea_"+nomeLinea+"_"+code+".xml").exists()){
                out.println("<td>"+nomeLinea+"</td>");
                out.println("<td class='percorso' onclick='showLinea(\""+ numLinea +"\",\""+code+"\",\""+direction+"\",\""+nomeLinea+"\")'>"+direction+"</td>");
                //out.println("<td>"+code+"</td>");
                out.println("</tr>");
        }
        out.println("</table>");
        out.println("</div>");     
    }catch (Exception e) {
        out.println(e.getMessage());
    }finally{con.close();}  
%>