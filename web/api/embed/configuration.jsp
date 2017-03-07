<%@page import="org.json.simple.JSONObject"%>
<%@page import="java.io.IOException"%>
<%@page import="org.openrdf.model.Value"%>
<%@ page import="org.openrdf.repository.Repository"%>
<%@ page import="org.openrdf.repository.sparql.SPARQLRepository"%>
<%@ page import="java.sql.*"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.*"%>
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
    Statement st = null;
    ResultSet rs = null;
    conMySQL = ConnectionPool.getConnection(); //DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
    String idConf = request.getParameter("idConf");
    String scale = request.getParameter("scale");
    String translate = request.getParameter("translate");
    String query = "";
    String nomeProvincia = "";
    String nomeComune = "";
    String categorie = "";
    String numeroRisultatiServizi = "";
    String numeroRisultatiSensori = "";
    String numeroRisultatiBus = "";
    String popupOpen = "";
    String pins = "";
    String actualSelection = "";
    String zoom = "";
    String center = "";
    String weatherCity = "";
    String raggioServizi = "";
    String raggioSensori = "";
    String raggioBus = "";
    String mapType = "";
    String line = "";
    String stop = "";
    String email = "";
    String description = "";
    String title = "";
    String coordinateSelezione="";
    String text="";
   
    // query = "select * from SiiMobility.Configurations where id= " + idConf;
    query = "select * from Queries where confR=\"" + idConf + "\"";
   
    st = conMySQL.createStatement();
    rs = st.executeQuery(query);

    while (rs.next()) {
        email = rs.getString("email");
        description = rs.getString("description");
        title = rs.getString("title");
        text= rs.getString("text");
        nomeProvincia = rs.getString("nomeProvincia");
        nomeComune = rs.getString("nomeComune");
        categorie = rs.getString("categorie");
        numeroRisultatiServizi = rs.getString("numeroRisultatiServizi");
        numeroRisultatiSensori = rs.getString("numeroRisultatiSensori");
        numeroRisultatiBus = rs.getString("numeroRisultatiBus");
        popupOpen = rs.getString("popupOpen");
        actualSelection = rs.getString("actualSelection");
        coordinateSelezione = rs.getString("coordinateSelezione");
        zoom = rs.getString("zoom");
        center = rs.getString("center");
        weatherCity = rs.getString("weatherCity");
        raggioServizi = rs.getString("raggioServizi");
        raggioSensori = rs.getString("raggioSensori");
        raggioBus = rs.getString("raggioBus");
        line = rs.getString("line");
        stop = rs.getString("stop");
    }
    JSONObject obj=new JSONObject();
    obj.put("nomeProvincia",nomeProvincia);
    obj.put("nomeComune",nomeComune);
    obj.put("categorie",categorie);
    obj.put("numeroRisultatiServizi", numeroRisultatiServizi);
    obj.put("numeroRisultatiSensori", numeroRisultatiSensori);
    obj.put("numeroRisultatiBus", numeroRisultatiBus);
    obj.put("actualSelection", actualSelection);
    obj.put("raggioServizi", raggioServizi);
    obj.put("raggioSensori", raggioSensori);
    obj.put("raggioBus", raggioBus);
    obj.put("email", email);
    obj.put("title", title);
    obj.put("description", description);
    obj.put("popupOpen",popupOpen);
    obj.put("actualSelection",actualSelection);
    obj.put("zoom",zoom);
    obj.put("text",text); 
    obj.put("center",center);
    obj.put("coordinateSelezione",coordinateSelezione);
    obj.put("weatherCity",weatherCity);
    obj.put("line",line);
    obj.put("stop",stop);
    out.println(obj);
    st.close();
    conMySQL.close();
%>