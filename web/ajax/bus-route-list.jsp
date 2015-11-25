<%@page import="org.disit.servicemap.ServiceMap"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@page import="java.io.IOException"%>
<%@page import="org.openrdf.model.Value"%>
<%@page import="java.util.*"%>
<%@page import="org.openrdf.repository.Repository"%>
<%@page import="org.openrdf.repository.sparql.SPARQLRepository"%>
<%@page import="java.sql.*"%>
<%@page import="java.util.List"%>
<%@page import="org.openrdf.query.BooleanQuery"%>
<%@page import="org.openrdf.OpenRDFException"%>
<%@page import="org.openrdf.repository.RepositoryConnection"%>
<%@page import="org.openrdf.query.TupleQuery"%>
<%@page import="org.openrdf.query.TupleQueryResult"%>
<%@page import="org.openrdf.query.BindingSet"%>
<%@page import="org.openrdf.query.QueryLanguage"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URL"%>
<%@page import="org.openrdf.rio.RDFFormat"%>
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
    
    Repository repo = new SPARQLRepository(sparqlEndpoint);
    repo.initialize();
    RepositoryConnection con = repo.getConnection();

    String nomeLinea = request.getParameter("nomeLinea");
    
    //out.println("<option value=\"all\">TUTTI I PERCORSI</option>");

    String queryString = ServiceMap.routeLineQuery(nomeLinea);
    TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, filterQuery(queryString));
    TupleQueryResult result = tupleQuery.evaluate();
    logQuery(filterQuery(queryString),"get-bus-route-list","any",nomeLinea);
    try {
      if(result.hasNext() == false){
          out.println("<option value=\"\">Bus Route under development</option>");
      }else{
          out.println("<option value=\"\"> - Select a Bus Route - </option>");
      }  
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String bsFirst = bindingSet.getValue("bsFirst").stringValue();
        String bsLast = bindingSet.getValue("bsLast").stringValue();
        String code = bindingSet.getValue("code").stringValue();

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
        
        String routeName = bindingSet.getValue("rName").stringValue();
        String direction = bsFirst+" &#10132; "+bsLast;
        //if(new File("../../ServiceMap/web/ajax/xml/linea_"+line+"_"+code+".xml").exists()){
        //System.out.println(pathXML);
        //if(new File(pathXML+"linea_"+line+"_"+code+".xml").exists()){
          out.println("<option value=\"" + code + "\">"+routeName+" - "+ direction + "</option>");
        //}
      }
      con.close();
    } catch (Exception e) {

      out.println(e.getMessage());
    }
%>