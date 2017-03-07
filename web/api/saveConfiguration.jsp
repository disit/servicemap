<%@page import="org.json.simple.JSONObject"%>
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

    Connection conMySQL = null;
    PreparedStatement stmt = null;
    
    conMySQL = ConnectionPool.getConnection(); //DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
    JSONObject obj = new JSONObject();
    String idConfiguration = request.getParameter("idConfiguration");
    String nomeProvincia = request.getParameter("nomeProvincia");
    String nomeComune = request.getParameter("nomeComune");
    String categorie = request.getParameter("categorie");
    String numeroRisultatiServizi = request.getParameter("numeroRisultatiServizi");
    String numeroRisultatiSensori = request.getParameter("numeroRisultatiSensori");
    String numeroRisultatiBus = request.getParameter("numeroRisultatiBus");
    String popupOpen = request.getParameter("popupOpen");
    String coordSel = request.getParameter("coordinateSelezione");
    String actualSelection = request.getParameter("actSelect");
    String zoom = request.getParameter("zoom");
    String center = request.getParameter("center");
    String weatherCity = request.getParameter("weatherCity");
    String raggioServizi = request.getParameter("raggioServizi");
    String raggioSensori = request.getParameter("raggioSensori");
    String raggioBus = request.getParameter("raggioBus");
    String mapType = request.getParameter("mapType");
    String line = request.getParameter("line");
    String stop = request.getParameter("stop");
    List<String> listaCategorie = null;
    boolean queryDone=false ;
    if (categorie != "") {
        listaCategorie = new ArrayList<String>();
        if (categorie != null) {
            String[] arrayCategorie = categorie.split(";");
            // GESTIONE CATEGORIE
            listaCategorie = Arrays.asList(arrayCategorie);
        }
    }
    String query = "INSERT INTO Configurations (id,nomeProvincia, nomeComune, categorie,numeroRisultati, popupOpen,pins,actualSelection,zoom,center,weatherCity,radius,mapType,line,stop)"
            + " VALUES (\"" + idConfiguration + "\","
            + "\"" + nomeProvincia + "\","
            + " \"" + nomeComune + "\","
            + " \"" + listaCategorie + "\","
            + " \"" + numeroRisultati + "\","
            + " ? , "
           // + " \""+popupOpen+"\","
           // + " \""+pins+"\","
            + " ? , "
            + " \"" + actualSelection + "\","
            + " \"" + zoom + "\","
             + " ? , "
         //   + " \"" + center + "\","
            + " \"" + weatherCity + "\","
            + " \"" + radius + "\","
            + " \"" + mapType + "\","
            + " \"" + line + "\","
            + " \"" + stop + "\")";
    try{
      stmt = conMySQL.prepareStatement(query);
      stmt.setString(1, popupOpen);
      stmt.setString(2, pins);
      stmt.setString(3, center);
      stmt.executeUpdate();
      conMySQL.close();
      queryDone = true;
      obj.put("queryDone", queryDone);
    }catch(Exception e) {
      queryDone = false;
      obj.put("queryDone", queryDone);
    }
%>