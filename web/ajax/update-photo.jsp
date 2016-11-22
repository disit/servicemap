<%@page import="org.disit.servicemap.ServiceMap"%>
<%@page import="org.disit.servicemap.PhotoUploadServlet"%>
<%@page import="net.coobird.thumbnailator.Thumbnails"%>
<%@page import="java.nio.file.Files"%>
<%@page import="java.awt.Graphics2D"%>
<%@page import="java.awt.image.BufferedImage"%>
<%@page import="javax.imageio.ImageIO"%>
<%@page import="java.io.File"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="org.disit.servicemap.Configuration"%>
<%@page import="java.sql.Connection"%>
<%@page import="org.disit.servicemap.ConnectionPool"%>
<%@page trimDirectiveWhitespaces="true" %>
<%@page contentType="application/json" pageEncoding="UTF-8"%>
<%
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

  //is client behind something?
  String ipAddress = ServiceMap.getClientIpAddress(request);  

  if(!ipAddress.startsWith("192.168.0.") && !ipAddress.equals("127.0.0.1")) {
    response.sendError(403, "unaccessible from "+ipAddress);
    return;
  }
  
  String id = request.getParameter("id");
  String status = request.getParameter("status");
  String rotate = request.getParameter("rotate");
  if (id != null && status != null) {
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("UPDATE ServicePhoto SET status=? WHERE id=?");
      st.setString(1, status);
      st.setString(2, id);
      int n = st.executeUpdate();
      out.println(n);
      st.close();
    } catch (SQLException ex) {
      ex.printStackTrace();
    }
    finally {
      connection.close();
    }
  } else if(id!=null && rotate!=null) {
    Configuration conf = Configuration.getInstance();
    String uploadPath = conf.get("photoUploadPath", "/tmp/servicemap");
    File uploads = new File(uploadPath);
    if(!uploads.exists())
      uploads.mkdirs();
    Connection connection = ConnectionPool.getConnection();
    try {
      PreparedStatement st = connection.prepareStatement("SELECT file FROM ServicePhoto WHERE id=?");
      st.setString(1, id);
      ResultSet rs = st.executeQuery();
      if(rs.next()) {
        int angle = 90;
        if("left".equals(rotate))
          angle = -angle;

        String file = rs.getString("file");
        //rotate original photo
        File forig = new File(uploadPath, "originals/"+file);
        File fcopy = new File(uploadPath, "originals/original-"+file);
        if(!fcopy.exists()) {
          Files.copy(forig.toPath(), fcopy.toPath());
          System.out.println("copiato file originale di "+file);
        }
        PhotoUploadServlet.rotateImage(forig, angle, forig);
        //rotate thumbnail photo
        File fthumb = new File(uploadPath, "thumbs/"+file);
        PhotoUploadServlet.rotateImage(fthumb, angle, fthumb);
        //rotate mediumres photo
        File f = new File(uploadPath, file);
        PhotoUploadServlet.rotateImage(f, angle, f);
        out.println("\"OK\"");
      }
      st.close();
    } catch(Exception e) {
      e.printStackTrace();
      response.sendError(500, "FAILED ROTATION");
    }
    finally {
      connection.close();
    }
  }
%>
