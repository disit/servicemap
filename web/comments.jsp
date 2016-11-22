<%@page import="org.springframework.web.util.HtmlUtils"%>
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
      //is client behind something?
      String ipAddress = request.getHeader("X-Forwarded-For");  
      if (ipAddress == null) {  
        ipAddress = ServiceMap.getClientIpAddress(request);  
      }
      
      if(!ipAddress.startsWith("192.168.0.") && !ipAddress.equals("127.0.0.1")) {
        response.sendError(403, "unaccessible from "+ipAddress);
        return;
      }
%>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Service comments</title>
    <script src="http://code.jquery.com/jquery-1.10.1.min.js"></script>
    <script type="text/javascript">
      $(document).ready( function() {
        $(".sstatus").change(function() {
          var id = $(this).attr("id");
          var status = $(this).find(":selected").text();
          var s =$(this);
          s.hide();
          $.ajax("ajax/update-comment.jsp", {
            data: { id: id, status: status},
            success: function() {
              s.show();
            },
            error: function(xhr,status,error) {
              alert("error "+status+" "+error);
            }
          })
        })
      })
    </script>      
    <style type="text/css">
      * {font-family: Arial}
      #center {width: 800px; margin-left: auto; margin-right: auto;}
      .image img {height:100px}
      .elem {background-color: #dedede}
      .time,.sname {padding: 0px 15px;}
      th {text-align: left; background-color: #999; color:white}
    </style>
  </head>
  <body>
    <div id="center">
    <h1>Comments from users</h1>
    <table>
      <tr><th>date submitted</th><th>service</th><th>comment</th><th>status</th></tr>
    <%
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("SELECT * FROM ServiceComment ORDER BY timestamp DESC");
      ResultSet rs = st.executeQuery();
      while(rs.next()) {
        String id = rs.getString("id");
        String status = rs.getString("status");
        String uri = baseApiUri+"v1/?format=html&serviceUri="+rs.getString("serviceUri");
        out.println("<tr>");
        out.println("<td class='elem time'>"+rs.getString("timestamp")+"</td>");
        out.println("<td class='elem sname'><a href=\""+uri+"\" target=\"_blank\">"+rs.getString("serviceName")+"</td>");
        out.println("<td class='elem comment'>"+HtmlUtils.htmlEscape(rs.getString("comment"))+"</td>");
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
      ex.printStackTrace();
    }%>
    </table>
    </div>
  </body>
</html>
