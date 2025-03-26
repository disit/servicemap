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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import net.coobird.thumbnailator.Thumbnails;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sparql.SPARQLRepository;

/**
 *
 * @author bellini
 */
@WebServlet("/api/v1/imgcache")
public class ImageCacheServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, UnsupportedEncodingException {
    String imgUrl=null;
    try {
      Configuration conf = Configuration.getInstance();
      imgUrl=request.getParameter("imageUrl");
      if(imgUrl==null) {
        response.sendError(400, "missing imageUrl parameter");
        return;
      }
      try {
          URL u = new URL(imgUrl);
          String prot = u.getProtocol();
          if(!prot.equals("http") && !prot.equals("https")) {
            response.sendError(400, "invalid imageUrl parameter (wrong protocol)");
            return;
          }
          String hostname = u.getHost();
          if(hostname.isEmpty() || hostname.equals("localhost") || isLocalIP(hostname) ) {
            response.sendError(400, "invalid imageUrl parameter (wrong hostname)");
            return;  
          }
      } catch(MalformedURLException e) {
          response.sendError(400, "invalid imageUrl parameter: "+e.getMessage());
          return;
      }
      String ssize=request.getParameter("size");
      if(ssize==null) {
        response.sendError(400, "missing size parameter (thumb|medium|1..2000)");
        return;
      }
      int size = 0;
      if("thumb".equals(ssize)) {
        size = Integer.parseInt(conf.get("photoThumbSize", "260"));
      } else if("medium".equals(ssize)) {
        size = Integer.parseInt(conf.get("photoMediumResSize", "1024"));        
      }
      if(size<=0 || size>2000) {
        response.sendError(400, "wrong size parameter");
        return;
      }
      boolean force=request.getParameter("force")!=null;
      
      String ip = ServiceMap.getClientIpAddress(request);
      if(!ServiceMap.checkIP(ip, "api-imgcache")) {
        response.sendError(403,"API calls daily limit reached");
        return;
      }
      int p = imgUrl.indexOf(".",imgUrl.lastIndexOf("/"));
      String ext = p<0 ? ".jpg" : imgUrl.substring(p);
      if(".mp3".equals(ext) || ".pdf".equals(ext)) {
        response.sendRedirect(imgUrl);
        return;
      }
      
      String cachePath = conf.get("imageCachePath", "/tmp/cache");
      String cacheImg = size+"-"+sha1(imgUrl)+ext;
      File f=new File(cachePath,cacheImg);
      if(force || !f.exists()) {
        try {
          Thumbnails.of(new URL(imgUrl)).size(size, size).toFile(f);
          ServiceMap.println("SAVE "+imgUrl+" "+f.getAbsolutePath());
        } catch(Exception e) {
          //in caso di fallimento per medium prova a dare il thumbnail
          boolean fail = true;
          if(!ssize.equals("thumb")) {
            size = Integer.parseInt(conf.get("photoThumbSize", "260"));
            cacheImg = size+"-"+sha1(imgUrl)+ext;
            f=new File(cachePath,cacheImg);
            fail = !f.exists();
          }
          if(fail) {
            ext=".png";
            f = new File(cachePath,"error.png");
            if(!f.exists())
              throw e;
            ServiceMap.notifyException(e);
          }        
        }
      }
      else
        ServiceMap.println("CACHE "+imgUrl+" "+f.getAbsolutePath());
      String mimeType="image/jpeg";
      if(ext.equals(".png"))
        mimeType="image/png";
      response.setHeader("Content-Type", mimeType);
      Files.copy(f.toPath(), response.getOutputStream());
      String ua = request.getHeader("User-Agent");
      ServiceMap.logAccess(request, null, null, null, null, "api-imgcache", null, null, null, null, null, null, null);
    } catch (Exception ex) {
      ServiceMap.notifyException(ex);
      if(imgUrl !=null)
        response.sendRedirect(imgUrl);
    }
  }

  public static String sha1(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {

    MessageDigest crypt = MessageDigest.getInstance("SHA-1");
    crypt.reset();
    crypt.update(s.getBytes("UTF-8"));

    return new BigInteger(1, crypt.digest()).toString(16);
 }
  
 public static boolean isLocalIP(String ipAddress) {
    try {
        InetAddress inet = InetAddress.getByName(ipAddress);
        byte[] bytes = inet.getAddress();

        // Controllo se è loopback (127.0.0.0/8 per IPv4, ::1 per IPv6)
        if (inet.isLoopbackAddress()) {
            return true;
        }
            
        // Controllo IP di Classe A (10.0.0.0 - 10.255.255.255)
        if ((bytes[0] & 0xFF) == 10) {
            return true;
        }
        // Controllo IP di Classe B (172.16.0.0 - 172.31.255.255)
        if ((bytes[0] & 0xFF) == 172 && (bytes[1] & 0xF0) == 16) {
            return true;
        }
        // Controllo IP di Classe C (192.168.0.0 - 192.168.255.255)
        if ((bytes[0] & 0xFF) == 192 && (bytes[1] & 0xFF) == 168) {
            return true;
        }
        return false;
    } catch (UnknownHostException e) {
        return false; // Se non è un IP valido, restituiamo false
    }
}
}
