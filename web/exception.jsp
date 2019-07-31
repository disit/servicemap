<%@page trimDirectiveWhitespaces="true" %>
<%@page import="org.json.simple.JSONObject"%>
<%@page import="org.disit.servicemap.ServiceMap"%>
<%
	Throwable throwable = (Throwable) request
				.getAttribute("javax.servlet.error.exception");
	Integer statusCode = (Integer) request
				.getAttribute("javax.servlet.error.status_code");
	String servletName = (String) request
				.getAttribute("javax.servlet.error.servlet_name");
	if (servletName == null) {
		servletName = "Unknown";
	}
	String requestUri = (String) request
			.getAttribute("javax.servlet.error.request_uri");
	if (requestUri == null) {
		requestUri = "Unknown";
	}
  if(!requestUri.contains("/api/")) {
    response.setContentType("text/html;charset=UTF-8");
%>
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Exception</title>
  </head>
  <body style="font-family: monospace;">
    <h1>Exception</h1>
<%
		// Analyze the servlet exception
    if(statusCode != 500){
	    	  out.write("<h3>Error Details</h3>");
	    	  out.write("<strong>Status Code</strong>:"+statusCode+"<br>");
	    	  out.write("<strong>Requested URI</strong>:"+requestUri);
	  }else{
	    	  out.write("<h3>Exception Details</h3>");
	    	  out.write("<ul><li><b>Servlet Name:</b> "+servletName+"</li>");
	    	  out.write("<li><b>Exception Name:</b> "+throwable.getClass().getName()+"</li>");
	    	  out.write("<li><b>Requested URI:</b> "+requestUri+"</li>");
          /*if(throwable.getMessage()!=null)
            out.write("<li><b>Exception Message:</b> "+throwable.getMessage().replace("&","&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br />") +"</li>");*/
	    	  out.write("</ul>");
	  }	      
	  out.write("<br><br>");
	  out.write("<a href=\""+request.getContextPath()+"\">Home</a>");
    ServiceMap.notifyException(throwable,"url: "+requestUri);
%>
  </body>
</html>
<% } else { 
  response.setContentType("application/json");
%>
{ "failure" : "EXCEPTION",
  "httpcode" : <%= statusCode %>,
  "exception": "<%= throwable.getClass().getName() %>"
}          
<% 
  ServiceMap.notifyException(throwable,"url: "+requestUri);
  } 
%>