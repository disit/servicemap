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

    RepositoryConnection con = ServiceMap.getSparqlConnection();

    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    ServiceMapApiV1 serviceMapApi = new ServiceMapApiV1();
    response.setContentType("application/json; charset=UTF-8");
    
    String textToSearch = request.getParameter("search");
    String limit = request.getParameter("limit");
    textToSearch = unescapeUri(textToSearch);
    String apikey = (String) request.getSession().getAttribute("apikey");
    
   
    ServiceMap.logAccess(request, null, null, null, null, "ui-text-search", limit, null, null, textToSearch, null, null, null);

    try {
        serviceMapApi.queryFulltext(out, con, textToSearch, null, null, limit, "it", false, apikey);
    } catch (Exception e) {
        out.println(e.getMessage());
    }finally{
      con.close() ;
    }
%>