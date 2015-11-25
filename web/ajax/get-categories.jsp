<%@page trimDirectiveWhitespaces="true" %>
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

    Connection conMySQL = null;
    Statement st = null;
    ResultSet rs = null;
    Statement st2 = null;
    ResultSet rs2 = null;

    Class.forName("com.mysql.jdbc.Driver");
    conMySQL = ConnectionPool.getConnection(); //DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
    String query = "SELECT distinct SubClasse FROM SiiMobility.ServiceCategory where SubClasse not like 'SubClasse' order by SubClasse";
    // create the java statement
    st = conMySQL.createStatement();
    // execute the query, and get a java resultset
    rs = st.executeQuery(query);
    // iterate through the java resultset
    while (rs.next()) {
        String classe = rs.getString("SubClasse");
        out.println("<input type='checkbox' name='" + classe + "' value='" + classe + "' class='macrocategory' /> <span class='" + classe + " macrocategory-label'>" + classe + "</span> <span class='toggle-subcategory' title='Mostra sottocategorie'>+</span>");
        out.println("<div class='subcategory-content'>");
        String query2 = "SELECT distinct Ita, Eng FROM SiiMobility.ServiceCategory WHERE SubClasse = '" + classe + "' ORDER BY SubClasse";
        // create the java statement
        st2 = conMySQL.createStatement();
        // execute the query, and get a java resultset
        rs2 = st2.executeQuery(query2);
        // iterate through the java resultset
        while (rs2.next()) {
            String sub_nome = rs2.getString("Ita");
            String sub_en_name = rs2.getString("Eng");
            out.println("<input type='checkbox' name='" + sub_nome + "' value='" + sub_en_name + "' class='sub_" + classe + " subcategory' />");
            out.println("<span class='" + classe + " subcategory-label'>" + sub_en_name + "</span>");
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
    conMySQL.close();
%>