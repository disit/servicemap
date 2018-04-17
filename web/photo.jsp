<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page import="java.sql.Connection"%>
<%@page import="org.disit.servicemap.ConnectionPool"%>
<%@page trimDirectiveWhitespaces="true" %>
<%@include file="/include/parameters.jsp" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
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
  
      //is client behind something?
      String ipAddress = ServiceMap.getClientIpAddress(request);  

      Configuration conf = Configuration.getInstance();
      if(!ipAddress.startsWith(conf.get("internalNetworkIpPrefix", "192.168.0.")) && !ipAddress.equals("127.0.0.1")) {
        response.sendError(403, "unaccessible from "+ipAddress);
        return;
      }
%>
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Uploaded photos</title>
    <script src="https://code.jquery.com/jquery-1.10.1.min.js"></script>
    <script type="text/javascript">
      $(document).ready( function() {
        $(".sstatus").change(function() {
          var id = $(this).attr("id");
          var status = $(this).find(":selected").text();
          var s =$(this);
          s.hide();
          $.ajax("ajax/update-photo.jsp", {
            data: { id: id, status: status},
            success: function() {
              s.show();
            },
            error: function(xhr,status,error) {
              alert("error "+status+" "+error);
            }
          })
        })
        $(".rotate").click(function() {
          var id = $(this).attr("id");
          var dir = $(this).attr("dir");
          var s =$(this);
          s.hide();
          $.ajax("ajax/update-photo.jsp", {
            data: { id: id, rotate: dir},
            success: function() {
              s.show();
              var img=s.parent().parent().find("img");
              var src=img.attr("src");
              if(src.indexOf("?")== -1)
                src += "?";
              img.attr("src",src+"u");
            },
            error: function(xhr,status,error) {
              alert("error "+status+" "+error);
              s.show();
            }
          })
        })
      })
    </script>      
    <style type="text/css">
      * {font-family: Arial}
      #center {width: 800px; margin-left: auto; margin-right: auto;}
      .image img {height:100px}
      .image {text-align: center}
      .elem {background-color: #dedede}
      .time,.sname {padding: 0px 15px;}
      .rotate { cursor: pointer; font-size:20px;}
      th {text-align: left; background-color: #999; color:white}
    </style>
  </head>
  <body>
    <div id="center">
    <h1>Photos uploaded from users</h1>
    <table>
      <tr><th>date submitted</th><th>service</th><th>photo</th><th>status</th></tr>
    <%
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("SELECT * FROM ServicePhoto ORDER BY timestamp DESC");
      ResultSet rs = st.executeQuery();
      while(rs.next()) {
        String id = rs.getString("id");
        String status = rs.getString("status");
        String thumbUrl = baseApiUri+"v1/photo/thumbs/"+rs.getString("file");
        String url = baseApiUri+"v1/photo/"+rs.getString("file");
        String uri = baseApiUri+"v1/?format=html&serviceUri="+rs.getString("serviceUri");
        out.println("<tr>");
        out.println("<td class='elem time'>"+rs.getString("timestamp")+"</td>");
        out.println("<td class='elem sname'><a href=\""+uri+"\" target=\"_blank\">"+rs.getString("serviceName")+"</td>");
        out.println("<td class='elem image'><a href=\""+url+"\" target=\"_blank\"><img src=\""+thumbUrl+"\"></a><div><span class='rotate' style='float:left' id='"+id+"' dir='right'>&#8635;</span><span class='rotate' style='float:right' id='"+id+"' dir='left'>&#8634;</span></div></td>");
        out.println("<td class='elem status'><select class='sstatus' id=\""+id+"\">");
        out.println("<option "+(status.equals("submitted")?"selected":"")+">submitted</option>");
        out.println("<option "+(status.equals("validated")?"selected":"")+">validated</option>");
        out.println("<option "+(status.equals("rejected")?"selected":"")+">rejected</option>");
        out.println("</select></td>");
        out.println("</tr>");
      }
      st.close();
      connection.close();
    } catch (SQLException ex) {
      ServiceMap.notifyException(ex);
    }%>
    </table>
    </div>
  </body>
</html>
