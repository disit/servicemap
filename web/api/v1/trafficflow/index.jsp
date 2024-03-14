
<%@page import="org.disit.servicemap.api.TrafficFlow"%>
<%@page import="org.disit.servicemap.api.ServiceMapApiV1"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file= "/include/parameters.jsp" %>
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
    TrafficFlow tf = new TrafficFlow();
    PrintWriter outt = response.getWriter();

    
    String geometry = request.getParameter("geometry");
    String dateObservedStart = request.getParameter("dateObservedStart");
    String dateObservedEnd = request.getParameter("dateObservedEnd");
    String scenarioName = request.getParameter("scenario");
    String roadElement = request.getParameter("roadElement");
    String kind = request.getParameter("kind");
    
    
    // check if date have time zone
    System.out.println(dateObservedStart);
        System.out.println(dateObservedEnd);

    dateObservedStart = tf.isoDateDefault(dateObservedStart); 
    dateObservedEnd = tf.isoDateDefault(dateObservedEnd);
    System.out.println(dateObservedStart);
        System.out.println(dateObservedEnd);

    //String isoDateFormatRegex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([+-]\\d{2}:\\d{2})?$";
    
    
    if (geometry != null && !tf.wktValidator(geometry)) {
        // geometry is not a correct wkt
        String errorMessage = "Invalid geometry format. Please provide the geometry in WKT format.";
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        outt.println(errorMessage);

        ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
        System.out.println(errorMessage);
        return;
    }

    if (dateObservedStart != null && !tf.isoDateValidator(dateObservedStart)) {
        // Date is not in ISO format
        String errorMessage = "Invalid dateObservedStart format. Please provide the date in ISO 8601 format.";
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        System.out.println(errorMessage);
        outt.println(errorMessage);
        return;
    }
    
    if (dateObservedEnd != null && !tf.isoDateValidator(dateObservedEnd)) {
        // Date is not in ISO format
        String errorMessage = "Invalid dateObservedEnd format. Please provide the date in ISO 8601 format.";
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        System.out.println(errorMessage);
        outt.println(errorMessage);
        return;
    }
    
    if (kind != null && !kind.equals("reconstructed") && !kind.equals("predicted") && !kind.equals("TTT") && !kind.equals("measured")){
        // Only this kind are allowed
        String errorMessage = "Invalid kind. Only 'reconstructed' or 'predicted' or 'TTT' or 'measured' are allowed";
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
        System.out.println(errorMessage);
        outt.println(errorMessage);
        return;
    }

    if ((dateObservedStart == null && dateObservedEnd != null) || (dateObservedStart != null && dateObservedEnd == null)) {
        String errorMessage = "Specify both 'dateObservedStart' and 'dateObservedEnd' parameters or leave them both null.";
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
        System.out.println(errorMessage);
        outt.println(errorMessage);
        return;
    }
    
    if (kind == null && dateObservedStart == null && dateObservedEnd == null && geometry == null && roadElement == null && scenarioName == null){
        String errorMessage = "Specify at least one of the following parameters: 'geometry', 'dateObservedStart' and 'dateObservedEnd', 'scenario', 'roadElement', 'kind'";
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ServiceMap.logError(request, response, HttpServletResponse.SC_BAD_REQUEST, errorMessage);
        System.out.println(errorMessage);
        outt.println(errorMessage);
        return;
    }
    
    
    String resp = tf.trafficFlowSearch(geometry, dateObservedStart, dateObservedEnd, scenarioName, roadElement, kind);
    outt.println(resp);
    
    

    return;

%>

