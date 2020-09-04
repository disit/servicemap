<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page import="org.openrdf.model.Literal"%>
<%@page import="org.openrdf.model.Value"%>
<%@page import="org.openrdf.query.BindingSet"%>
<%@page import="org.openrdf.query.TupleQueryResult"%>
<%@page import="org.openrdf.query.TupleQuery"%>
<%@page import="org.openrdf.query.QueryLanguage"%>
<%@page import="org.openrdf.repository.RepositoryConnection"%>
<%@page import="org.disit.servicemap.ServiceMap"%>
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
  
  
  String ipAddress = ServiceMap.getClientIpAddress(request);  
  
  Configuration conf = Configuration.getInstance();
  if(!ipAddress.startsWith(conf.get("internalNetworkIpPrefix", "192.168.0.")) && !ipAddress.equals("127.0.0.1")) {
    response.sendError(403, "unaccessible from "+ipAddress);
    return;
  }
  
  RepositoryConnection con = ServiceMap.getSparqlConnection();

  String query = request.getParameter("query");
  TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
  TupleQueryResult result = tupleQuery.evaluate();

  out.println("[");
  try {
    int i = 0;
    while (result.hasNext()) {
      BindingSet bindingSet = result.next();
      if(i!=0)
        out.println(",");
      out.println("{");
      int j=0;
      for(String n:result.getBindingNames()) {
        Value value = bindingSet.getValue(n);
        if(value!=null) {
          if(j!=0)
            out.println(",");
          out.print("\""+n+"\":\""+value.stringValue()+"\"");
          j++;
        }
      }
      out.print("}");
      i++;
    }
  } catch (Exception e) {
    ServiceMap.notifyException(e);
  }
  out.println("]");
  con.close();
%>