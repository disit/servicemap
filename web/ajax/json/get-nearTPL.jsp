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

    Repository repo = new SPARQLRepository(sparqlEndpoint);
    repo.initialize();
    RepositoryConnection con = repo.getConnection();

    String ip = request.getRemoteAddr();
    String ua = request.getHeader("User-Agent");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();
    response.setContentType("application/json; charset=UTF-8");
    String raggio = request.getParameter("raggio");
    String centro = request.getParameter("centro");
    String numLinee = request.getParameter("numLinee");
    String[] coord = centro.split(";");

    logAccess(ip, null, ua, null, null, null, "ui-location", null, null, null, null, null, null);

    try {
        serviceMapApi.queryLatLngTPL(out, con, coord, raggio,numLinee);
    } catch (Exception e) {
        out.println(e.getMessage());
    }finally{con.close() ;}
%>