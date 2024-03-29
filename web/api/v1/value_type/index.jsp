<%@page import="org.openrdf.repository.RepositoryConnection"%>
<%@page import="org.disit.servicemap.ServiceMap"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@ page trimDirectiveWhitespaces="true" %>
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

response.setContentType("application/json; charset=UTF-8");
RepositoryConnection con = ServiceMap.getSparqlConnection();
try {
  ServiceMapApiV1 api=new ServiceMapApiV1();
  api.queryValueTypeList(out, con);
} finally {
  con.close();
}  
%>
