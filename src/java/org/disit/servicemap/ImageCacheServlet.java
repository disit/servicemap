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
import java.net.URL;
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
    try {
      Configuration conf = Configuration.getInstance();
      String imgUrl=request.getParameter("imageUrl");
      if(imgUrl==null) {
        response.sendError(400, "missing imageUrl parameter");
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
          System.out.println("SAVE "+imgUrl+" "+f.getAbsolutePath());
        } catch(Exception e) {
          //in caso di fallimento per medium prova a dare il thumbnail
          if(!ssize.equals("thumb")) {
            size = Integer.parseInt(conf.get("photoThumbSize", "260"));
            cacheImg = size+"-"+sha1(imgUrl)+ext;
            f=new File(cachePath,cacheImg);
            if(!f.exists())
              throw e;
          } else {
            throw e;
          }
        }
      }
      else
        System.out.println("CACHE "+imgUrl+" "+f.getAbsolutePath());
      String mimeType="image/jpeg";
      if(ext.equals(".png"))
        mimeType="image/png";
      response.setHeader("Content-Type", mimeType);
      Files.copy(f.toPath(), response.getOutputStream());
    } catch (NoSuchAlgorithmException ex) {
      ex.printStackTrace();
    }
  }

  public static String sha1(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {

    MessageDigest crypt = MessageDigest.getInstance("SHA-1");
    crypt.reset();
    crypt.update(s.getBytes("UTF-8"));

    return new BigInteger(1, crypt.digest()).toString(16);
 }  
}
