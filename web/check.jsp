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
%>
<%@page import="javax.mail.MessagingException"%>
<%@page import="javax.mail.Transport"%>
<%@page import="javax.mail.Message"%>
<%@page import="javax.mail.internet.InternetAddress"%>
<%@page import="javax.mail.internet.MimeMessage"%>
<%@page import="javax.mail.Session"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file= "/include/parameters.jsp" %>
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Check RT</title>
  </head>
  <body>
    <h1>Realtime check</h1>
    <%
      RepositoryConnection con = ServiceMap.getSparqlConnection();
      int nn=0;
      Date d = new Date();
      int delayMin;
      String msg = "";
      
      System.out.println("start check realtime @ "+d);
      int avm_delay = avm_max_delay;
      if(d.getHours()>=1 && d.getHours()<=5)
        avm_delay = 300;
      
      out.println(d+"<br><br>");

      String queryLastAVM = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "?x a km4c:AVMRecord."
              + "?x km4c:concernLine <http://www.disit.org/km4city/resource/4>."
              + "?x dcterms:created ?d."
              + "}  order by desc(?d) limit 1";
      msg += check(out, con, queryLastAVM, "AVM 4", avm_delay);

      queryLastAVM = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "?x a km4c:AVMRecord."
              + "?x km4c:concernLine <http://www.disit.org/km4city/resource/6>."
              + "?x dcterms:created ?d."
              + "}  order by desc(?d) limit 1";
      msg += check(out, con, queryLastAVM, "AVM 6", avm_delay);

      queryLastAVM = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "?x a km4c:AVMRecord."
              + "?x km4c:concernLine <http://www.disit.org/km4city/resource/17>."
              + "?x dcterms:created ?d."
              + "}  order by desc(?d) limit 1";
      msg += check(out, con, queryLastAVM, "AVM 17", avm_delay);

      String queryLastMeteo = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "?x a km4c:WeatherReport."
              + "?x km4c:updateTime/<http://schema.org/value> ?d."
              + "}  order by desc(?d) limit 1";
      msg += check(out, con, queryLastMeteo, "Meteo", meteo_max_delay);

      String queryLastParking = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "   ?x a km4c:SituationRecord."
              + "   ?x km4c:observationTime/dcterms:identifier ?d."
              + "}  order by desc(?d) limit 1";
      msg += check(out, con, queryLastParking, "Parking", parking_max_delay);

      String queryLastSensor = "PREFIX dcterms:<http://purl.org/dc/terms/>"
              + "PREFIX km4c:<http://www.disit.org/km4city/schema#>"
              + "select ?d where {"
              + "   ?x a km4c:Observation."
              + "   ?x km4c:measuredTime/dcterms:identifier ?d."
              + "}  order by desc(?d) limit 1";
      msg += check(out, con, queryLastSensor, "Sensor", sensor_max_delay);


      if(!msg.equals("")) {
        boolean emailSent = false;
        String to[] = check_send_to.split(";");
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", smtp);
        properties.put("mail.smtp.port", portSmtp);
        Session mailSession = Session.getDefaultInstance(properties);
        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(mailFrom));
            for(String t: to)
              message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(t));
            message.setSubject(request.getLocalAddr()+" ServiceMap RT DELAY ");
            String emailMessage = d+"\n\n"+msg;

            message.setContent(emailMessage, "text/plain");
            Transport.send(message);
            emailSent = true;
        } catch (MessagingException mex) {
            mex.printStackTrace();
            emailSent = false;
        }        
      }
      System.out.println("end check realtime");
      %>
  </body>
</html>
<%!
    private Date getLastData(RepositoryConnection con, final String queryLast) throws Exception {
      TupleQuery tupleQuery;
      TupleQueryResult result;
      Date date = null;

      tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryLast);
      result = tupleQuery.evaluate();
      if(result.hasNext()) {
        BindingSet bindingSet = result.next();
        String lst = bindingSet.getValue("d").stringValue();
        date = javax.xml.bind.DatatypeConverter.parseDateTime(lst).getTime(); 
      }
      return date;
    }
    private int getDelayMin(Date d) {
      if(d == null)
        return -1;
      Date now = new Date();
      return (int)(now.getTime()-d.getTime())/(1000*60);
    }
    
    private String check(JspWriter out, RepositoryConnection con, String query, String type, int maxDelay) throws Exception {
      String msg = "";
      Date d = getLastData(con, query);
      int delayMin=getDelayMin(d);
      String delay ="";
      if(delayMin>maxDelay) {
        msg = type+" delay "+delay2string(delayMin)+" (max "+delay2string(maxDelay)+")\n";
        delay = "***";
      }
      out.println(type+" delay "+delay2string(delayMin)+" (max "+delay2string(maxDelay)+") "+delay+"<br>");
      return msg;
    }
    
    private String delay2string(int delayMin) {
      String out = "";
      int dmin = delayMin % 60;
      int dhour = (delayMin / 60) % 24;
      int dday = (delayMin / 60) / 24;
      if(dday>0)
        out += dday+"days ";
      if(dhour>0)
        out += dhour+"h ";
      if(dmin>0)
        out += dmin+"m";
      return out;
    }
%>