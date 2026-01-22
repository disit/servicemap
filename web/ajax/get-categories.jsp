<%@page trimDirectiveWhitespaces="true" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
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
<%@page import="java.text.ParseException"%>
<%@page import="java.text.SimpleDateFormat"%>
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
    Statement st = null;
    ResultSet rs = null;
    PreparedStatement st2 = null;
    ResultSet rs2 = null;

    Class.forName("com.mysql.jdbc.Driver");
    conMySQL = ConnectionPool.getConnection();
    try {
      String query = "SELECT distinct SubClasse FROM SiiMobility.ServiceCategory where SubClasse not like 'SubClasse' order by SubClasse";
      // create the java statement
      st = conMySQL.createStatement();
      // execute the query, and get a java resultset
      rs = st.executeQuery(query);
      // iterate through the java resultset
      while (rs.next()) {
          String classe = rs.getString("SubClasse");
          String classeHtml = escapeHtml(classe);
          out.println("<input type='checkbox' name='" + classeHtml + "' value='" + classeHtml + "' class='macrocategory' /> <span class='" + classeHtml + " macrocategory-label'>" + classeHtml + "</span> <span class='toggle-subcategory' title='Mostra sottocategorie'>+</span>");
          out.println("<div class='subcategory-content'>");
          String query2 = "SELECT distinct Ita, Eng FROM SiiMobility.ServiceCategory WHERE SubClasse = ? ORDER BY SubClasse";
          st2 = conMySQL.prepareStatement(query2);
          st2.setString(1, classe);
          rs2 = st2.executeQuery();
          // iterate through the java resultset
          while (rs2.next()) {
              String sub_nome = rs2.getString("Ita");
              String sub_en_name = rs2.getString("Eng");
              String subNomeHtml = escapeHtml(sub_nome);
              String subEnNameHtml = escapeHtml(sub_en_name);
              out.println("<input type='checkbox' name='" + subNomeHtml + "' value='" + subEnNameHtml + "' class='sub_" + classeHtml + " subcategory' />");
              out.println("<span class='" + classeHtml + " subcategory-label'>" + subEnNameHtml + "</span>");
              out.println("<br />");
          }
          out.println("</div>");
          out.println("<br />");
          st2.close();
      }
      out.println("<br />");
      out.println("<input type='checkbox' name='road-sensor' value='RoadSensor' class='macrocategory' /> <span class='road-sensor macrocategory-label'>Road Sensors</span>");
      out.println("<input type='checkbox' name='near-bus-stops' value='NearBusStops' class='macrocategory' /> <span class'near-bus-stops macrocategory-label'>Bus Stops</span>");
      st.close();
    } finally {
      conMySQL.close();
    }
%>
