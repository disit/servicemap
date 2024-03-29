<%@page contentType="application/json" pageEncoding="UTF-8"%>
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

  String label = request.getParameter("label");
  Connection con = ConnectionPool.getConnection();
  try {
    PreparedStatement st = con.prepareStatement("SELECT wkt FROM Geometry WHERE label=?");
    st.setString(1, label);
    ResultSet rs = st.executeQuery();
    if (rs.next()) {
        String wkt = rs.getString("wkt");
        out.println("{ "
                  + "\"wkt\": \"" + wkt + "\" "
                  + "}");
    }
    st.close();
  } finally {
    con.close();
  }
%>