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

package org.disit.servicemap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.phoenix.shaded.org.mortbay.util.ajax.JSON;
import org.json.simple.JSONObject;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.URI;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

/**
 *
 */
public class SparqlProxy extends HttpServlet {

  private Log log = LogFactory.getLog(SparqlProxy.class);

  @Override
  public void doGet(HttpServletRequest theReq, HttpServletResponse theResp) throws ServletException, IOException {
    Configuration conf = Configuration.getInstance();
    String sparqlProxyMode = conf.get("sparqlProxyMode", "http");
    if (sparqlProxyMode.equals("jdbc") || "jdbc".equals(theReq.getParameter("mode"))) {
        String query = theReq.getParameter("query");
        String format = theReq.getParameter("format");
        try {
            sparqlProxy(query, format, theReq, theResp);
        } catch (Exception e) {
          ServiceMap.notifyException(e);
        }
    } else if(sparqlProxyMode.equals("http")) {
        if (log.isDebugEnabled()) {
          String aSparqlEndpointQuery = theReq.getParameter("query");
          log.debug(aSparqlEndpointQuery);
        }

        try {
          redirectGet(theReq, theResp);
        } catch (Exception e) {
          ServiceMap.notifyException(e);
        }
    }
  }

  @Override
  public void doPost(HttpServletRequest theReq, HttpServletResponse theResp) throws ServletException, IOException {
    Configuration conf = Configuration.getInstance();
    String sparqlProxyMode = conf.get("sparqlProxyMode", "http");
    if(sparqlProxyMode.equals("http"))
    {
        if (log.isDebugEnabled()) {
          String aSparqlEndpointQuery = theReq.getParameter("query");
          log.debug(aSparqlEndpointQuery);
        }

        try {
          redirectPost(theReq, theResp);
        } catch (Exception e) {
          ServiceMap.notifyException(e);
        }
    } else if (sparqlProxyMode.equals("jdbc")) {
        String query = theReq.getParameter("query");
        String format = theReq.getParameter("format");
        try {
            sparqlProxy(query, format, theReq, theResp);
        } catch (Exception e) {
          ServiceMap.notifyException(e);
        }
    }
  }
  
  private void redirectGet(HttpServletRequest theReq, HttpServletResponse theResp) throws Exception {
    String ip = ServiceMap.getClientIpAddress(theReq);
    if(!ServiceMap.checkIP(ip, "sparql")) {
      theResp.sendError(403, "API calls daily limit reached");
      return;
    }
    
    HttpClient httpclient = HttpClients.createDefault();
    HttpGet httpget = null;
    HttpResponse response = null;
    
    long startTime = System.nanoTime();

    String theServerHost = Configuration.getInstance().get("sparqlEndpoint", null);
    if(theServerHost==null)
      theServerHost = "http://192.168.0.207:8890/sparql";

    //String aEncodedQuery = URLEncoder.encode(query, "UTF-8");
    //String serviceUri = theReq.getParameter("query");
    String theReqUrl = theServerHost+"?";
    String query = theReq.getParameter("query");
    if(query!=null) {
      String aEncodedQuery = URLEncoder.encode(query, "UTF-8");
      theReqUrl += "query=" + aEncodedQuery;
    }
    String format = theReq.getParameter("format");
    if(format!=null)
      theReqUrl += "&format="+format;

    try {
      httpget = new HttpGet(theReqUrl);

      //ServiceMap.println("request header:");
      Enumeration<String> aHeadersEnum = theReq.getHeaderNames();
      while (aHeadersEnum.hasMoreElements()) {
        String aHeaderName = aHeadersEnum.nextElement();
        String aHeaderVal = theReq.getHeader(aHeaderName);
        httpget.setHeader(aHeaderName, aHeaderVal);
        //ServiceMap.println("  " + aHeaderName + ": " + aHeaderVal);
      }

      if (log.isDebugEnabled()) {
        log.debug("executing request " + httpget.getURI());
      }

      // Create a response handler
      response = httpclient.execute(httpget);

      //ServiceMap.println("response header:");
      // set the same Headers
      for (Header aHeader : response.getAllHeaders()) {
        if (!aHeader.getName().equals("Transfer-Encoding") || !aHeader.getValue().equals("chunked")) {
          theResp.setHeader(aHeader.getName(), aHeader.getValue());
        }
        //ServiceMap.println("  " + aHeader.getName() + ": " + aHeader.getValue());
      }
      theResp.setHeader("Access-Control-Allow-Origin", "*");

      // set the same locale
      if (response.getLocale() != null) {
        theResp.setLocale(response.getLocale());
      }

      // set the content
      theResp.setContentLength((int) response.getEntity().getContentLength());
      theResp.setContentType(response.getEntity().getContentType().getValue());

      // set the same status
      theResp.setStatus(response.getStatusLine().getStatusCode());

      // redirect the output
      InputStream aInStream = null;
      OutputStream aOutStream = null;
      try {
        aInStream = response.getEntity().getContent();
        aOutStream = theResp.getOutputStream();

        byte[] buffer = new byte[1024];
        int r = 0;
        do {
          r = aInStream.read(buffer);
          if (r != -1) {
            aOutStream.write(buffer, 0, r);
          }
        } while (r != -1);
        /*
         int data = aInStream.read();
         while (data != -1) {
         aOutStream.write(data);

         data = aInStream.read();
         }
         */
      } catch (IOException ioe) {
        ServiceMap.notifyException(ioe);
      } finally {
        if (aInStream != null) {
          aInStream.close();
        }
        if (aOutStream != null) {
          aOutStream.close();
        }
      }
    } finally {
      httpclient.getConnectionManager().closeExpiredConnections();
      httpclient.getConnectionManager().shutdown();
    }
    String ua = theReq.getHeader("User-Agent");
    ServiceMap.logQuery(query, "SPARQL", ip, ua, System.nanoTime()-startTime);
    ServiceMap.logAccess(theReq, null, null, null, null, "api-sparql", null, null, null, null, null, null, null);
  }
  
  private void redirectPost(HttpServletRequest theReq, HttpServletResponse theResp) throws Exception {
    String ip = ServiceMap.getClientIpAddress(theReq);
    if(!ServiceMap.checkIP(ip, "sparql")) {
      theResp.sendError(403, "API calls daily limit reached");
      return;
    }

    HttpClient httpclient = HttpClients.createDefault();
    HttpPost httppost = null;
    HttpResponse response = null;
    
    long startTime = System.nanoTime();

    String theServerHost = Configuration.getInstance().get("sparqlEndpoint", null);
    if(theServerHost==null)
      theServerHost = "http://192.168.0.207:8890/sparql";

    String theReqUrl = theServerHost;
    String query = theReq.getParameter("query");
    String format = theReq.getParameter("format");

    //ServiceMap.println("POST to "+theReqUrl+" query="+query);
    try {
      httppost = new HttpPost(theReqUrl);

      //ServiceMap.println("request header:");
      Enumeration<String> aHeadersEnum = theReq.getHeaderNames();
      while (aHeadersEnum.hasMoreElements()) {
        String aHeaderName = aHeadersEnum.nextElement();
        String aHeaderVal = theReq.getHeader(aHeaderName);
        if(!aHeaderName.equalsIgnoreCase("content-length")) {
          httppost.setHeader(aHeaderName, aHeaderVal);
          //ServiceMap.println("  " + aHeaderName + ": " + aHeaderVal);
        }
      }
      List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
      urlParameters.add(new BasicNameValuePair("query", query));
      if(format!=null)
        urlParameters.add(new BasicNameValuePair("format", format));

      HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
      httppost.setEntity(postParams);

      if (log.isDebugEnabled()) {
        log.debug("executing request " + httppost.getURI());
      }
      
      // Create a response handler
      response = httpclient.execute(httppost);

      //ServiceMap.println("response header:");
      // set the same Headers
      for (Header aHeader : response.getAllHeaders()) {
        if (!aHeader.getName().equals("Transfer-Encoding") || !aHeader.getValue().equals("chunked")) {
          theResp.setHeader(aHeader.getName(), aHeader.getValue());
        }
        //ServiceMap.println("  " + aHeader.getName() + ": " + aHeader.getValue());
      }
      theResp.setHeader("Access-Control-Allow-Origin", "*");

      // set the same locale
      if (response.getLocale() != null) {
        theResp.setLocale(response.getLocale());
      }

      // set the content
      theResp.setContentLength((int) response.getEntity().getContentLength());
      theResp.setContentType(response.getEntity().getContentType().getValue());

      // set the same status
      theResp.setStatus(response.getStatusLine().getStatusCode());

      // redirect the output
      InputStream aInStream = null;
      OutputStream aOutStream = null;
      try {
        aInStream = response.getEntity().getContent();
        aOutStream = theResp.getOutputStream();

        byte[] buffer = new byte[1024];
        int r = 0;
        do {
          r = aInStream.read(buffer);
          if (r != -1) {
            aOutStream.write(buffer, 0, r);
          }
        } while (r != -1);
        /*
         int data = aInStream.read();
         while (data != -1) {
         aOutStream.write(data);

         data = aInStream.read();
         }
         */
      } catch (IOException ioe) {
        ServiceMap.notifyException(ioe);
      } finally {
        if (aInStream != null) {
          aInStream.close();
        }
        if (aOutStream != null) {
          aOutStream.close();
        }
      }
    } catch(Exception e) {
      ServiceMap.notifyException(e);
    } finally {
      httpclient.getConnectionManager().closeExpiredConnections();
      httpclient.getConnectionManager().shutdown();
    }
    String ua = theReq.getHeader("User-Agent");
    ServiceMap.logQuery(query, "SPARQL", ip, ua, System.nanoTime()-startTime);
    ServiceMap.logAccess(theReq, null, null, null, null, "api-sparql", null, null, null, null, null, null, null);
  }

  private void sparqlProxy(String query, String format, HttpServletRequest theReq, HttpServletResponse theResp) throws Exception {
    String ip = ServiceMap.getClientIpAddress(theReq);
    if(!ServiceMap.checkIP(ip, "sparql")) {
      theResp.sendError(403, "API calls daily limit reached");
      return;
    }
    long startTime = System.nanoTime();
    
    RepositoryConnection connection = ServiceMap.getSparqlConnection();
    ServletOutputStream os = theResp.getOutputStream();
    try {
        TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
        TupleQueryResult result = q.evaluate();
        
        List<String> names = result.getBindingNames();
        String vars = "[";
        boolean firstName = true;
        for(String v: names) {
            if(!firstName)
                vars+=",";
            vars += "\""+v+"\"";
            firstName = false;
        }
        vars += "]";
        
        os.print("{ \"head\": { \"vars\": " + vars +" }, \"results\":{ \"bindings\": [ ");
        boolean firstTuple=true;
        while(result.hasNext()) {
            BindingSet bs = result.next();
            os.print((firstTuple ? "" : ",") + "{");
            boolean firstValue = true;
            for(Binding b : bs) {
                String name = b.getName();
                Value v = b.getValue();
                String other = "";
                if(v instanceof URI) {
                    other = ",\"type\":\"uri\"";
                } else if(v instanceof Literal) {
                    other = ",\"type\":\"literal\"";
                    Literal l = (Literal)v;
                    URI datatype = l.getDatatype();
                    String lang = l.getLanguage();
                    if(lang!=null)
                        other += ",\"xml:lang\":\"" + lang + "\"";
                    if(datatype!=null)
                        other += ",\"datatype\":\"" + datatype + "\"";
                } else if(v instanceof BNode) {
                    other = ",\"type\":\"bnode\"";                    
                }
                os.print( (firstValue? "":",") + "\""+name+"\":{ \"value\":\"" + JSONObject.escape(v.stringValue()) + "\"" + other + " }");
                //System.out.println("name: "+name+" "+v.stringValue()+" "+v.toString());
                firstValue = false;
            }
            os.print("}");
            firstTuple = false;
        }
        os.print("]}}");
    } catch(Exception e) {
        //theResp.sendError(500, e.getMessage());
        theResp.setStatus(500);
        os.println(e.getMessage());
        throw e;
    } finally {
        connection.close();
    }
    
    String ua = theReq.getHeader("User-Agent");
    ServiceMap.logQuery(query, "SPARQL", ip, ua, System.nanoTime()-startTime);
    ServiceMap.logAccess(theReq, null, null, null, null, "api-sparql", null, null, null, null, null, null, null);      
  }
}
