<%@page trimDirectiveWhitespaces="true" %>
<%@page import="org.json.simple.JSONObject"%>
<%@page isErrorPage="true" %>
<%
	String requestUri = (String) request
			.getAttribute("javax.servlet.error.request_uri");
	if (requestUri == null) {
		requestUri = "Unknown";
	}
	String errorMsg = (String) request
			.getAttribute("javax.servlet.error.message");
  if(!requestUri.contains("/api/")) {
    response.setContentType("text/html;charset=UTF-8");
%>
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>ERROR</title>
  </head>
  <body style="font-family: monospace;">
    <h1>SmartCity API - ERROR 400</h1>
    <%= errorMsg%><br><br>
    see <a href="http://www.disit.org/6991" target="_blank">API documentation</a>
  </body>
</html>
<% } else { 
    response.setContentType("application/json");
%>
{ 
  "failure" : "ERROR",
  "httpcode" : 400,
  "message" : "<%= JSONObject.escape(errorMsg) %>",
  "apiDoc" : "http://www.disit.org/6991"
}
<% } %>