<%@page import="java.security.NoSuchAlgorithmException"%>
<%@page import="org.json.simple.JSONObject"%>
<%@page import="java.math.BigInteger"%>
<%@page import="java.security.MessageDigest"%>
<%@page import="java.io.IOException"%>
<%@page import="org.openrdf.model.Value"%>
<%@page import="java.util.*"%>
<%@page import="org.openrdf.repository.Repository"%>
<%@page import="org.openrdf.repository.sparql.SPARQLRepository"%>
<%@page import="java.sql.*"%>
<%@page import="java.util.List"%>
<%@page import="org.openrdf.query.BooleanQuery"%>
<%@page import="org.openrdf.OpenRDFException"%>
<%@page import="org.openrdf.repository.RepositoryConnection"%>
<%@page import="org.openrdf.query.TupleQuery"%>
<%@page import="org.openrdf.query.TupleQueryResult"%>
<%@page import="org.openrdf.query.BindingSet"%>
<%@page import="org.openrdf.query.QueryLanguage"%>
<%@page import="java.io.File"%>
<%@page import="java.net.URL"%>
<%@page import="org.openrdf.rio.RDFFormat"%>
<%@page import="java.io.*,java.util.*,javax.mail.*"%>
<%@page import="javax.mail.internet.*,javax.activation.*"%>
<%@page import="javax.servlet.http.*,javax.servlet.*" %>
<%@include file= "/include/parameters.jsp" %>
<%!    
/* ServiceMap.
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */

  public static String md5(String input) {
    String md5 = null;
    if (null == input) {
      return null;
    }
    try {
        //Create MessageDigest object for MD5
        MessageDigest digest = MessageDigest.getInstance("MD5");
        //Update input string in message digest
        digest.update(input.getBytes(), 0, input.length());
        //Converts message digest value in base 16 (hex) 
        md5 = new BigInteger(1, digest.digest()).toString(16);
    } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
    }
    return md5;
  }
%>
<%
    Connection conMySQL = null;
    PreparedStatement stmt = null;
    Statement st = null;
    ResultSet rs = null;
    JSONObject obj = new JSONObject();
    boolean queryDone = false;
    conMySQL = ConnectionPool.getConnection(); //DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
    String email = request.getParameter("email");
    String title = request.getParameter("title");
    String description = request.getParameter("description");
    String queryId = request.getParameter("idQuery");
    String confId = request.getParameter("idConf");
    String update = request.getParameter("update");
    String format = request.getParameter("format");
    String queryIdRW = "";
    String queryIdR = "";
    String linkR = "";
    String linkRW = "";
    String linkJSON = "";
    String linkHTML = "";
    String linkRJson = "";
    String linkRHtml = "";
    String apiVersion = "v1";
    
    if ("true".equals(update)) {
        queryIdRW = queryId;
        String queryforId="SELECT id FROM Queries WHERE idRW=\"" + queryIdRW + "\"";
        st = conMySQL.createStatement();
        rs = st.executeQuery(queryforId);
        if (rs.next()) {
            queryIdR = rs.getString("id");
        }       
    } else {
        queryIdR = queryId;
        queryIdRW = queryId + "write";
        queryIdR = md5(queryIdR);
        queryIdRW = md5(queryIdRW);
    }
    linkRJson = baseApiUri + apiVersion + "?queryId=" + queryIdR + "&format=json";
    linkRHtml = baseApiUri + apiVersion + "?queryId=" + queryIdR + "&format=html";
    linkRW = baseApiUri + "?queryId=" + queryIdRW;
    String categorie = request.getParameter("categorie");
    String numeroRisultatiServizi = request.getParameter("numeroRisultatiServizi");
    String numeroRisultatiSensori = request.getParameter("numeroRisultatiSensori");
    String numeroRisultatiBus = request.getParameter("numeroRisultatiBus");
    String actualSelection = request.getParameter("actSelect");
    String raggioServizi = request.getParameter("raggioServizi");
    if("area".equals(raggioServizi))
      raggioServizi = "-1";
    String raggioSensori = request.getParameter("raggioSensori");
    if("area".equals(raggioSensori))
      raggioSensori = "-1";
    String raggioBus = request.getParameter("raggioBus");
    if("area".equals(raggioBus))
      raggioBus = "-1";
    String coordSel = request.getParameter("coordinateSelezione");
    String parentQuery = request.getParameter("parentQuery");
    String nomeProvincia = request.getParameter("nomeProvincia");
    String nomeComune = request.getParameter("nomeComune");
    String line = request.getParameter("line");
    String stop = request.getParameter("stop");
    String typeService = request.getParameter("typeService");
    String idService = request.getParameter("idService");
    String nameService = request.getParameter("nameService");
    String typeOfSaving = request.getParameter("typeSaving");
    String idConfR=confId;
    String idConfRW=idConfR+"write";     
    String popupOpen = request.getParameter("popupOpen");
    String zoom = request.getParameter("zoom");
    String center = request.getParameter("center");
    String weatherCity = request.getParameter("weatherCity");
    String linkConfR="";
    String linkConfRW="";
    String linkFreeTextJson="";
    String linkFreeTextHtml="";
    String linkTextSearchJson="";
    String linkTextSearchHtml=""; 
    String text=request.getParameter("text");
    String textEscaped=escapeURI(text);
    String selection = "";
    if(actualSelection!=null && !"".equals(actualSelection)){
        actualSelection = unescapeUri(actualSelection);
        if (actualSelection.indexOf("COMUNE di") != -1) {
            selection = actualSelection;
        } else {
            selection = coordSel;
        }
    }
    String selectionEscaped = escapeURI(selection);
    String categorieEscaped = escapeURI(categorie);
    String raggi = "";
    if(raggioSensori.equals(raggioServizi) && raggioBus.equals(raggioServizi))
      raggi = raggioServizi;
    else
      raggi = raggioServizi + ";" + raggioSensori + ";" + raggioBus;
    String raggiEscaped = escapeURI(raggi);
    if("embed".equals(typeOfSaving)){
        idConfR=md5(idConfR);
        idConfRW=md5(idConfRW);
        linkConfR=baseApiUri+"embed/?idConf="+idConfR;
        linkConfRW=baseApiUri+"embed/?idConf="+idConfRW;
    }
    if("freeText".equals(typeOfSaving)){
        linkFreeTextJson=baseApiUri+apiVersion+"?search="+textEscaped+"&selection="+selectionEscaped+"&limit="+numeroRisultatiServizi+"&format=json";
        linkFreeTextHtml=baseApiUri+apiVersion+"?search="+textEscaped+"&limit="+numeroRisultatiServizi+"&format=html";
    }
    if("textSearch".equals(typeOfSaving)){
        linkTextSearchJson=baseApiUri+apiVersion+"?search="+textEscaped+"&categories="+categorieEscaped+"&limit="+numeroRisultatiServizi+"&maxDists=" + raggiEscaped + "&format=json";
        linkTextSearchHtml=baseApiUri+apiVersion+"?search="+textEscaped+"&limit="+numeroRisultatiServizi+"&format=html";
    }
    String queryString = "";
    List<String> listaCategorie = null;
    if (categorie != "") {
        listaCategorie = new ArrayList<String>();
        if (categorie != null) {
            String[] arrayCategorie = categorie.split(";");
            // GESTIONE CATEGORIE
            listaCategorie = Arrays.asList(arrayCategorie);
        }
    }
    String ip = ServiceMap.getClientIpAddress(request);
    String ua = request.getHeader("User-Agent");
    
    String linkHTMLService="";
    String linkJSONService = "";
    String linkServiceJson = "";
    String linkServiceHtml = "";    
    String risultati = "";
    if(numeroRisultatiSensori.equals(numeroRisultatiServizi) && numeroRisultatiBus.equals(numeroRisultatiServizi))
      risultati = numeroRisultatiServizi;
    else
      risultati = numeroRisultatiServizi+ ";" + numeroRisultatiSensori + ";" + numeroRisultatiBus;
    String risultatiEscaped = escapeURI(risultati);
    if ("query".equals(typeOfSaving)) {
        if (idService != null && !"".equals(idService)) {
            String link = baseApiUri + apiVersion + "?selection=" + idService + "&categories=" + categorieEscaped + "&maxResults=" + risultatiEscaped + "&maxDists=" + raggiEscaped;
            linkJSONService = link + "&format=json";
            linkHTMLService = link + "&format=html";
        }
        else {
            String link = baseApiUri + apiVersion + "?selection=" + selectionEscaped + "&categories=" + categorieEscaped + "&maxResults=" + risultatiEscaped + "&maxDists=" + raggiEscaped;
            if(!"".equals(text))
              link += "&text=" + textEscaped;
            linkJSON = link + "&format=json";
            linkHTML = link + "&format=html";
        }
    } else {
        if(!"freeText".equals(typeOfSaving) && !"embed".equals(typeOfSaving)){
            linkServiceJson = baseApiUri + apiVersion + "?serviceUri=" + idService + "&format=json";
            linkServiceHtml = baseApiUri + apiVersion + "?serviceUri=" + idService + "&format=html";
        }
    }
    logAccess(ip, email, ua, selection, categorie, null, "save", risultati, raggi, queryIdR, text, format, null, null);
    if ("false".equals(update)) {
        String query = "INSERT INTO Queries (id,email,description,categorie,numeroRisultatiServizi,numeroRisultatiSensori,numeroRisultatiBus,actualSelection,raggioServizi,raggioSensori,raggioBus,idRW,nomeProvincia,nomeComune,parentQuery, line, stop,coordinateSelezione,title,idService,typeService,nameService,zoom,center,weatherCity,popupOpen,confR,typeSaving,text)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try {
                stmt = conMySQL.prepareStatement(query);
                stmt.setString(1, queryIdR);
                stmt.setString(2, email);
                stmt.setString(3, description);
                stmt.setString(4, listaCategorie!=null ? listaCategorie.toString() : "" );
                stmt.setString(5, numeroRisultatiServizi);
                stmt.setString(6, numeroRisultatiSensori);
                stmt.setString(7, numeroRisultatiBus);
                stmt.setString(8, actualSelection);
                stmt.setString(9, raggioServizi);
                stmt.setString(10, raggioSensori);
                stmt.setString(11, raggioBus);
                stmt.setString(12, queryIdRW);
                stmt.setString(13, nomeProvincia);
                stmt.setString(14, nomeComune);
                stmt.setString(15, parentQuery);
                stmt.setString(16, line);
                stmt.setString(17, stop);
                stmt.setString(18, coordSel);
                stmt.setString(19, title);
                stmt.setString(20, idService);
                stmt.setString(21, typeService);
                stmt.setString(22, nameService);
                stmt.setString(23, zoom);
                stmt.setString(24, center);
                stmt.setString(25, weatherCity);
                stmt.setString(26, popupOpen);
                stmt.setString(27, idConfR);
                stmt.setString(28, typeOfSaving);
                stmt.setString(29, text);
                
                stmt.executeUpdate();
                conMySQL.close();
                queryDone = true;
                obj.put("queryDone", queryDone);
            } catch (Exception e) {
                e.printStackTrace();
                queryDone = false;
                obj.put("queryDone", queryDone);
            }
       } 
       else {
        String query = "UPDATE Queries SET email=?,"
                + "title= ?,"
                + "description= ?,"
                + "categorie= ?,"
                + "numeroRisultatiServizi= ?,"
                + "numeroRisultatiSensori= ?,"
                + "numeroRisultatiBus= ?,"
                + "actualSelection= ?,"
                + "raggioServizi= ?,"
                + "raggioSensori= ?,"
                + "raggioBus= ?,"
                + "nomeProvincia= ?,"
                + "nomeComune= ?,"
                + "parentQuery= ?,"
                + "line= ?,stop= ?,"
                + "coordinateSelezione= ?,"
                + "idService= ?,"
                + "typeService= ?,"
                + "nameService= ?,"
                + "zoom= ?,"
                + "center= ?,"
                + "weatherCity= ?,"
                + "popupOpen= ?,"
                + "confR= ?,"
                + "typeSaving= ? "
                + "WHERE idRW=?";

        try {
            stmt = conMySQL.prepareStatement(query);
            stmt.setString(1, email);
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setString(4, listaCategorie.toString());
            stmt.setString(5, numeroRisultatiServizi);
            stmt.setString(6, numeroRisultatiSensori);
            stmt.setString(7, numeroRisultatiBus);
            stmt.setString(8, actualSelection);
            stmt.setString(9, raggioServizi);
            stmt.setString(10, raggioSensori);
            stmt.setString(11, raggioBus);
            stmt.setString(12, nomeProvincia);
            stmt.setString(13, nomeComune);
            stmt.setString(14, parentQuery);
            stmt.setString(15, line);
            stmt.setString(16, stop);
            stmt.setString(17, coordSel);
            stmt.setString(18, idService);
            stmt.setString(19, typeService);
            stmt.setString(20, nameService);
            stmt.setString(21, zoom);
            stmt.setString(22, center);
            stmt.setString(23, weatherCity);
            stmt.setString(24, popupOpen);
            stmt.setString(25, idConfR);
            stmt.setString(26, typeOfSaving);
            stmt.setString(27, queryIdRW);
            stmt.executeUpdate();
            conMySQL.close();
            queryDone = true;
            obj.put("queryDone", queryDone);
        } catch (Exception e) {
            out.println(e.getMessage());
            queryDone = false;
            obj.put("queryDone", queryDone);
        }
        //obj.put("email", email);
        obj.put("query", query);
        //out.println(obj);
    }
    
    boolean emailSent = false;
    String to = email;
    Properties properties = System.getProperties();
    properties.put("mail.smtp.host", smtp);
    properties.put("mail.smtp.port", portSmtp);
    Session mailSession = Session.getDefaultInstance(properties);
    try {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(mailFrom));
        message.addRecipient(Message.RecipientType.TO,
                new InternetAddress(to));
        message.setSubject("Your link for query " + title);
        String emailMessage = "<html><head><title>ServiceMap</title></head>"
                + "<body><p>Thanks a lot for using Service Map by DISIT at <a href=\"http://servicemap.disit.org\">http://servicemap.disit.org</a></p>"
                + "<p>Your query \"" + title + "\" has been saved. </p>"
                + "<p>Description:</br>" + description + "</p>";
             // if("".equals(text) || text==null){  
              emailMessage= emailMessage  + "<p>You can access to the query results on Service Map by clicking on these links: </p>"
                + "<p>Link for read only result in format json: <a href='" + linkRJson + "'>'" + linkRJson + "'</a> <br></p>"
                + "<p>Link for read only result in html: <a href='" + linkRHtml + "'>'" + linkRHtml + "'</a> <br></p>"
                + "<p>Link for overwrite this query on Service Map: <a href='" + linkRW + "'>'" + linkRW + "'</a> <br></p>";
            //  }
        if ("query".equals(typeOfSaving)) {
            if (idService != null && !"".equals(idService)) {
                emailMessage = emailMessage + "<p>Link to obtain results in format json based on the selected Service Uri: <a href='" + linkJSONService + "'>'" + linkJSONService + "'</a><br></p>";
                emailMessage = emailMessage + "<p>Link to obtain results in format html  based on the selected Service Uri: <a href='" + linkHTMLService + "'>'" + linkHTMLService + "'</a><br></p>";
            } else {
                emailMessage = emailMessage + "<p>Link to obtain results in format json  based on the coordinates of selection: <a href='" + linkJSON + "'>'" + linkJSON + "'</a><br></p>";
                emailMessage = emailMessage + "<p>Link to obtain results in format html based on the coordinates of selection: <a href='" + linkHTML + "'>'" + linkHTML + "'</a><br></p>";
            }
        } else {
            if("embed".equals(typeOfSaving)){
                emailMessage = emailMessage +"<p>Link for access to your configuration: <a href='" + linkConfR + "'>'" + linkConfR + "'</a> <br></p>";
               // emailMessage = emailMessage +"<p>Link for access and modify to your configuration: <a href='" + linkConfRW + "'>'" + linkConfRW + "'</a> <br></p>";
            }else{
                if("freeText".equals(typeOfSaving)){
                    emailMessage = emailMessage + "Link to obtain  results in format json : <a href='" + linkFreeTextJson + "'>'" + linkFreeTextJson + "'</a><br>";
                    emailMessage = emailMessage + "Link to obtain  results in format html : <a href='" + linkFreeTextHtml + "'>'" + linkFreeTextHtml + "'</a><br>";
                }else{
                    emailMessage = emailMessage + "Link to obtain  results in format json : <a href='" + linkServiceJson + "'>'" + linkServiceJson + "'</a><br>";
                    emailMessage = emailMessage + "Link to obtain  results in format html : <a href='" + linkServiceHtml + "'>'" + linkServiceHtml + "'</a><br>";
                }
            }
        }
        //+ "The SPARQL query made to obatin the results is:<br> "+queryString + "'</a><br>"
        emailMessage = emailMessage + "<p>or copy paste it on your browser. </p>"
                + "<p>You can share the link with your friends.</p>"
                + "<p>Best regards<br>"
                + "ServiceMap.disit.org team<br>"
                + "You can contact us at info@disit.org or visit our web page at <a href=\"http://www.disit.org\">http://www.disit.org</a>"
                + "</body>"
                + "</html>";
        message.setContent(emailMessage, "text/html");
        Transport.send(message);
        emailSent = true;
        obj.put("emailSent", emailSent);
    } catch (MessagingException mex) {
        mex.printStackTrace();
        emailSent = false;
        obj.put("emailSent", emailSent);
    }
    obj.put("email", email);
    if("embed".equals(typeOfSaving))
      obj.put("idConfR", idConfR);
    out.println(obj);
%>