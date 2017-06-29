<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page isErrorPage="true" %>
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>ERROR</title>
  </head>
  <body style="font-family: monospace;">
    <h1>SmartCity API - ERROR 400</h1>
    ${requestScope['javax.servlet.error.message']}<br><br>
    see <a href="http://www.disit.org/6991" target="_blank">API documentation</a>
  </body>
</html>
