<%@page import="java.security.NoSuchAlgorithmException"%>
<%@page import="org.json.simple.JSONObject"%>
<%@page import="java.math.BigInteger"%>
<%@page import="java.security.MessageDigest"%>
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
<%@page import="java.io.*,java.util.*,javax.mail.*"%>
<%@page import="javax.mail.internet.*,javax.activation.*"%>
<%@page import="javax.servlet.http.*,javax.servlet.*" %>
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
    PreparedStatement st = null;
    ResultSet rs = null;
    PreparedStatement st2 = null;
    ResultSet rs2 = null;
    conMySQL = ConnectionPool.getConnection();
    String queryId = request.getParameter("queryId");
    String format= request.getParameter("format");
    String query = "";
    String nomeProvincia = "";
    String nomeComune = "";
    String line = "";
    String stop = "";
    String actualSelection = "";
    String id = "";
    String email = "";
    String description = "";
    String categorie = "";
    String numeroRisultatiServizi = "";
    String numeroRisultatiSensori = "";
    String numeroRisultatiBus = "";
    String raggioServizi = "";
    String raggioSensori = "";
    String raggioBus = "";
    String title = "";
    String parentQuery = "";
    String linkRW = "";
    String coordinateSelezione="";
    String idService="";
    String typeService="";
    String text= "";
    String center = "";
    String popupOpen = "";
    String typeSaving="";
    
    String nameService= "";
    boolean isReadOnly=true;
    try {
      st = conMySQL.prepareStatement("select * from Queries where id=?");
      st.setString(1, queryId);
      rs = st.executeQuery();
      if (rs.next()) {
          id = rs.getString("id");
          email = rs.getString("email");
          description = rs.getString("description");
          categorie = rs.getString("categorie");
          numeroRisultatiServizi = rs.getString("numeroRisultatiServizi");
          numeroRisultatiSensori = rs.getString("numeroRisultatiSensori");
          numeroRisultatiBus = rs.getString("numeroRisultatiBus");
          actualSelection = rs.getString("actualSelection");
          raggioServizi = rs.getString("raggioServizi");
          if(raggioServizi.equals("-1"))
            raggioServizi = "area";
          raggioSensori = rs.getString("raggioSensori");
          if(raggioSensori.equals("-1"))
            raggioSensori = "area";
          raggioBus = rs.getString("raggioBus");
          if(raggioBus.equals("-1"))
            raggioBus = "area";
          linkRW = rs.getString("idRW");
          nomeComune = rs.getString("nomeComune");
          nomeProvincia = rs.getString("nomeProvincia");
          stop = rs.getString("stop");
          line = rs.getString("line");
          title = rs.getString("title");
          parentQuery = rs.getString("parentQuery");
          coordinateSelezione=rs.getString("coordinateSelezione");
          idService = rs.getString("idService");
          typeService = rs.getString("typeService");
          nameService = rs.getString("nameService");
          typeSaving = rs.getString("typeSaving");
          text = rs.getString("text");
          center = rs.getString("center");
          popupOpen = rs.getString("popupOpen");

      }
      else{
          st2=conMySQL.prepareStatement("select * from Queries where idRW=?");
          st2.setString(1, queryId);
          rs2=st2.executeQuery();

          if (rs2.next()) {
              id = rs2.getString("id");
              email = rs2.getString("email");
              description = rs2.getString("description");
              categorie = rs2.getString("categorie");
              numeroRisultatiServizi = rs2.getString("numeroRisultatiServizi");
              numeroRisultatiSensori = rs2.getString("numeroRisultatiSensori");
              numeroRisultatiBus = rs2.getString("numeroRisultatiBus");
              actualSelection = rs2.getString("actualSelection");
              raggioServizi = rs2.getString("raggioServizi");
              //michela: ho messo -1 nel caso inside
              if(raggioServizi.equals("-1"))
                raggioServizi = "inside";
              raggioSensori = rs2.getString("raggioSensori");
              if(raggioSensori.equals("-1"))
                raggioSensori = "inside";
              raggioBus = rs2.getString("raggioBus");
              if(raggioBus.equals("-1"))
                raggioBus = "inside";
              linkRW = rs2.getString("idRW");
              nomeComune = rs2.getString("nomeComune");
              nomeProvincia = rs2.getString("nomeProvincia");
              stop = rs2.getString("stop");
              line = rs2.getString("line");
              title = rs2.getString("title");
              parentQuery = rs2.getString("parentQuery");
              coordinateSelezione=rs2.getString("coordinateSelezione");
              idService = rs2.getString("idService");
              typeService = rs2.getString("typeService");
              typeSaving = rs2.getString("typeSaving");
              nameService = rs2.getString("nameService");
              text = rs2.getString("text");
              center = rs2.getString("center");
              popupOpen = rs2.getString("popupOpen");
              isReadOnly=false;
          }
      }
      JSONObject obj = new JSONObject();
      obj.put("id",id);
      obj.put("nomeProvincia", nomeProvincia);
      obj.put("nomeComune", nomeComune);
      obj.put("categorie", categorie);
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
      obj.put("parentQuery", parentQuery);
      obj.put("line", line);
      obj.put("stop", stop);
      obj.put("idRW", linkRW);
      obj.put("isReadOnly",isReadOnly);
      obj.put("coordinateSelezione",coordinateSelezione);
      obj.put("format",format);
      obj.put("idService",idService);
      obj.put("typeService",typeService);
      obj.put("nameService",nameService);
      obj.put("typeSaving",typeSaving);
      obj.put("text",text);
      obj.put("popupOpen",popupOpen);
      out.println(obj);
    } finally {
      st.close();
      if(st2!=null)
          st2.close();
      conMySQL.close();
    }
%>