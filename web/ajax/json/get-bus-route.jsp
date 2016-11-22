<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
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

String agency = request.getParameter("agency");
String numLinea = request.getParameter("numLinea");
String busStop = request.getParameter("busStop");
 
 ServiceMapApiV1 api = new ServiceMapApiV1();
 RepositoryConnection conn = ServiceMap.getSparqlConnection();
 api.queryBusRoutes(out, conn, agency, numLinea, busStop, false);

/*    
    RepositoryConnection con = ServiceMap.getSparqlConnection();
    String numLinea = request.getParameter("numLinea");
    String busStop = request.getParameter("busStop");

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
              + (busStop != null ? " ?route km4c:hasSection ?rs."
                      + " ?rs km4c:endsAtStop ?bs1."
                      + " ?rs km4c:startsAtStop ?bs2."
                      + " { ?bs1 foaf:name \"" + busStop + "\"."
                      + " }UNION "
                      + " {?bs2 foaf:name \"" + busStop + "\" . "
                      + " } " : "")
              + " ?route km4c:hasFirstStop ?bss.\n"
              + " ?route km4c:hasLastStop ?bse.\n"
              + " ?route km4c:direction ?dir.\n"
              + " ?route dcterms:identifier ?code.\n"
              + " ?route foaf:name ?rName.\n"
              //+ " ?route opengis:hasGeometry ?geometry .\n"
              //+ " ?geometry opengis:asWKT ?polyline .\n"
              + " ?bss foaf:name ?bsFirst.\n"
              + " ?bse foaf:name ?bsLast.\n"
              + "} ORDER BY ?dir ";

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
            // PROVA ESTRAZIONE LETTERA LINEA BUS ATAF*/
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
            /*String direction = bsFirst+" &#10132; "+bsLast;
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
    }finally{con.close();}  */

%>