<%@page import="org.disit.servicemap.ServiceMap"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
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
<%@page import="java.text.Normalizer"%>

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

    RepositoryConnection con = ServiceMap.getSparqlConnection();

    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();
    response.setContentType("application/json; charset=UTF-8");
    
    String cat_servizi = request.getParameter("cat_servizi");
    if(cat_servizi == null)
      cat_servizi = "categorie";
    String centroRicerca = request.getParameter("centroRicerca");
    //String centroRicerca = "wkt:LINESTRING(11.250622272491455 43.769466843182045,11.251260638237 43.77023772474747,11.252800226211548 43.77021835598559,11.25272512435913 43.77342573746555,11.253132820129395 43.77343348455556,11.253089904785156 43.773867319994814,11.253218650817871 43.77423917643775,11.25272512435913 43.77493640103604,11.252210140228271 43.77574207266986,11.25072956085205 43.77513781996212,11.250107288360596 43.77501386993404,11.249721050262451 43.775850527637736,11.2489914894104 43.77770972506939,11.248304843902588 43.78003364056799)";
    //String centroRicerca = "wkt:LINESTRING(11.251201629638672 43.77165162742224,11.258068084716797 43.771527670168346)";
    //String centroRicerca = "wkt:POLYGON((11.25273585319519 43.774955768269955,11.253604888916016 43.77552516214396,11.255139112472534 43.77503323714287,11.257874965667725 43.77413846554611,11.257204413414001 43.773534196634465,11.257622838020325 43.77322818633103,11.257832050323486 43.772631655415395,11.255380511283875 43.77271687448194,11.255359053611755 43.77291830088374,11.25447392463684 43.77279047266896,11.254554390907288 43.77348384061621,11.253835558891296 43.773468346448226,11.253218650817871 43.774192694508834,11.25273585319519 43.774955768269955))";
    //String centroRicerca = "wkt:POLYGON((11.160049438476562 43.77853085219783,11.262359619140625 43.82610597028621,11.276779174804688 43.78894114656216,11.304244995117188 43.766135280960945,11.249313354492188 43.72297866632312,11.20330810546875 43.74778512063877,11.160049438476562 43.77853085219783))";
    String[] coord = null;
    if(centroRicerca.startsWith("wkt:")) {
      String[] coordWkt = { centroRicerca };
      coord = coordWkt;
    }
    else if(centroRicerca.startsWith("geo:")) {
      String[] coordGeo = { centroRicerca };
      coord = coordGeo;
    }
    else{
      coord = centroRicerca.split(";");
    }
    String raggioServizi = request.getParameter("raggioServizi");
    boolean inside="inside".equals(raggioServizi);
    
    String categorie = request.getParameter("categorie");
    categorie = ServiceMap.cleanCategories(categorie);
    String textFilter = request.getParameter("textFilter");
    String numeroRisultatiServizi = request.getParameter("numeroRisultatiServizi");
    System.out.println("get-services.jsp limit "+numeroRisultatiServizi);//Michela TODO
    if(centroRicerca.length()>255)
      centroRicerca=centroRicerca.substring(1, 255);
    
    
    
    logAccess(ip, null, ua, centroRicerca, categorie, null, "ui-services-by-gps", numeroRisultatiServizi, raggioServizi, null, textFilter, null, null, null);

    /*
    if(coord == null)
        coord = centroRicerca.split(",");
    */
    //System.out.println("MMMMMMMMMMMMM categoria: get-services.jsp  "+categorie);//Michela TODO
    
    try {
      serviceMapApi.queryLatLngServices(out, con, coord, categorie, textFilter,raggioServizi,raggioServizi,raggioServizi,numeroRisultatiServizi,numeroRisultatiServizi,numeroRisultatiServizi,null, cat_servizi, false, inside, true);
    } catch (Exception e) {
        e.printStackTrace();
        out.println(e.getMessage());
    }finally{con.close() ;}
%>