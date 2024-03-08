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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import net.coobird.thumbnailator.Thumbnailator;
import net.coobird.thumbnailator.Thumbnails;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;

/**
 *
 * @author bellini
 */
@WebServlet("/api/v1/photo/*")
@MultipartConfig
public class PhotoUploadServlet extends HttpServlet {

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String clength = request.getHeader("Content-Length");
    ServiceMap.println("length:"+clength);
    String uid = request.getParameter("uid"); // Retrieves <input type="text" name="description">
    if(uid==null || uid.trim().isEmpty() || uid.equals("null")) {
      response.sendError(400,"missing uid");
      ServiceMap.println("photo upload: request missing uid");
      return;
    }
    if(!ServiceMap.validateUID(uid)) {
      response.sendError(404, "invalid uid");
      ServiceMap.println("photo upload: request invalid uid");
      return;
    }
    String serviceUri = request.getParameter("serviceUri");
    if(serviceUri==null) {
      response.sendError(400,"missing serviceUri");
      ServiceMap.println("photo upload: request missing ServiceUri");
      return;
    }
    Configuration conf = Configuration.getInstance();
    //retrieve service name
    String serviceName = null;
    try {
      RepositoryConnection con = ServiceMap.getSparqlConnection();
      try {
        serviceName = ServiceMap.getServiceName(con, serviceUri);
        if(serviceName == null)
            serviceName = ServiceMap.getServiceIdentifier(con, serviceUri);
      } finally {
        con.close();
      }
      if(serviceName==null) {
        response.sendError(400,"invalid serviceUri (no name/id found)");
        ServiceMap.println("photo upload: request invalid serviceUri "+serviceUri);
        return;
      }
    } catch (Exception ex) {
      ServiceMap.notifyException(ex);
      response.sendError(500,"failed connection with RDF store");
      return;
    }
    ServiceMap.println("uid:" + uid);
    ServiceMap.println("uri:" + serviceUri);
    Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
    if (filePart == null) {
      response.sendError(400, "missing part named 'file'");
      ServiceMap.println("photo upload: missing part");
      return;
    }
    
    //check mimetype
    String mimeType = filePart.getContentType();
    ServiceMap.println("photo upload: mimetype:" + mimeType);
    if(mimeType == null) {
      String filename = filePart.getName();
      ServiceMap.println("photo upload: name:" + filename);
      if(filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg"))
        mimeType = "image/jpeg";
      else if(filename.toLowerCase().endsWith(".png"))
        mimeType = "image/png";
    }
    if(!"image/jpeg".equals(mimeType) && !"image/png".equals(mimeType)) {
      response.sendError(400, "supported only image/jpeg and image/png files");
      ServiceMap.println("photo upload: "+mimeType+": no jpeg/png");
      return;
    }
    
    String uploadPath = conf.get("photoUploadPath", "/tmp/servicemap");
    
    File uploads = new File(uploadPath);
    if(!uploads.exists())
      uploads.mkdirs();
    
    File originals = new File(uploads,"originals");
    if(!originals.exists())
      originals.mkdirs();
    
    String ext="";
    if("image/jpeg".equals(mimeType)) {
      ext=".jpg";
    }
    else if("image/png".equals(mimeType)) {
      ext=".png";
    }
    File file = File.createTempFile("file-", ext, originals);

    try (InputStream input = filePart.getInputStream()) {
      Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      input.close();
      ServiceMap.println("saved file to: " + file.getName());
      
      //rescale image and produce thumbnails
      produceImages(file, uploads);
      
      //save on Upload table
      String ip = ServiceMap.getClientIpAddress(request);
      String ua = request.getHeader("User-Agent");
      String reqFrom = request.getParameter("requestFrom");
      
      Connection conMySQL = ConnectionPool.getConnection();
      try {
        String query = "INSERT INTO ServicePhoto(serviceUri,serviceName,uid,file,ip,userAgent) VALUES(?,?,?,?,?,?)";
        PreparedStatement st = conMySQL.prepareStatement(query);
        st.setString(1, serviceUri);
        st.setString(2, serviceName);
        st.setString(3, uid);
        st.setString(4, file.getName());
        st.setString(5, ip);
        st.setString(6, ua);

        st.execute();
      } finally {
        conMySQL.close();
      }
      //response.addHeader("Access-Control-Allow-Origin", "*");
      //response.addHeader("Access-Control-Allow-Methods", "POST");
      response.setHeader("Location", request.getRequestURI()+"/"+file.getName());
      String baseApiUrl = conf.get("baseApiUrl", "http://www.disit.org/ServiceMap/api/");
      ServiceMap.sendEmail(conf.get("validationEmail","pierfrancesco.bellini@unifi.it"), "KM4CITY new photo uploaded for "+serviceName, 
              "new photo uploaded for:\n\n"
            + serviceName+"\n"
            + baseApiUrl+"v1/?format=html&serviceUri="+serviceUri+"\n\n"
            + "uid: "+uid+"\n\n"
            + "Validate it on "+baseApiUrl.replace("/api/", "/")+"photo.jsp", null);
      ServiceMap.logAccess(request, null, null, null, serviceUri, "api-service-photo", null, null, null, null, null, uid, reqFrom);
    } catch (SQLException ex) {
      ServiceMap.notifyException(ex);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    //response.addHeader("Access-Control-Allow-Origin", "*");
    String uri=request.getRequestURI();
    String file=uri.substring(uri.lastIndexOf("photo/")+6);
    if(file.isEmpty() || file.startsWith("..")) {
      response.sendError(400,"invalid file");
      return;
    }
    ServiceMap.println("GET "+uri+" "+file);
    String uploadPath = Configuration.getInstance().get("photoUploadPath", "/tmp/servicemap");
    File uploads = new File(uploadPath);
    File f = new File(uploads, file);
    if(!f.exists()) {
      response.sendError(404,"file not found");
      return;
    }
    String mimeType="image/jpeg";
    if(file.substring(file.lastIndexOf(".")+1).equals("png"))
      mimeType="image/png";
    response.setHeader("Content-Type", mimeType);
    Files.copy(f.toPath(), response.getOutputStream());
  }
  
  private static String getSubmittedFileName(Part part) {
    for (String cd : part.getHeader("content-disposition").split(";")) {
      if (cd.trim().startsWith("filename")) {
        String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
        return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1); // MSIE fix.
      }
    }
    return null;
  }

  public static void scaleImage(File src, int d, File dst) throws IOException {
    Thumbnails.of(src).size(d, d).toFile(dst);
  }

  public static void rotateImage(File src, int dir, File dst) throws IOException {
    Thumbnails.of(src).scale(1).rotate(dir).toFile(dst);
  }
  
  public static void produceImages(File file, File uploads) throws IOException {
    Configuration conf = Configuration.getInstance();
    int thumbSize = Integer.parseInt(conf.get("photoThumbSize", "260"));
    int mediumResSize = Integer.parseInt(conf.get("photoMediumResSize", "1024"));

    File thumbsUploads = new File(uploads,"thumbs");
    if(!thumbsUploads.exists())
      thumbsUploads.mkdirs();

    File thumb = new File(thumbsUploads, file.getName());
    File mediumRes = new File(uploads, file.getName());
    //BufferedImage bsrc = ImageIO.read(file);
    
    String format = "";
    if(file.getName().endsWith(".jpg"))
      format="JPEG";
    else if(file.getName().endsWith(".png"))
      format="PNG";
    
    //ImageIO.write(scaleImage(file, mediumResSize), format, mediumRes);
    //ImageIO.write(scaleImage(file, thumbSize), format, thumb);
    scaleImage(file, mediumResSize, mediumRes);
    scaleImage(file, thumbSize, thumb);
  }
}
