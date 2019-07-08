/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.disit.servicemap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author bellini
 */
public class JwtUtil {
  static private PublicKey pk = null;
  
  static public boolean isValid(String accessToken) {
    if(pk==null) {
      try {
        loadPublicKey();
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
    if(pk==null)
      return false;
    try {
      Jwts.parser().setSigningKey(pk).parseClaimsJws(accessToken);
      return true;
    } catch(Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  
  static public class User {
    final public String username;
    final public String role;
    
    private User(String u, String r) {
      username = u;
      role = r;
    }
  }
  
  static public User getUserFromJwt(String accessToken) throws Exception {
    if(pk==null)
      loadPublicKey();

    String roles[] = new String[] {"RootAdmin", "ToolAdmin", "AreaManager", "Manager", "Observer"};
    Jws<Claims> t = Jwts.parser().setSigningKey(pk).parseClaimsJws(accessToken);
    String u = (String) t.getBody().get("username");
    Map<String,List<String>> x = (Map<String,List<String>>) t.getBody().get("realm_access");
    List<String> rr = x.get("roles");
    if(rr!=null) {
      for(String role: roles) {
        if(rr.contains(role))
          return new User(u,role);
      }
    }
    throw new Exception("user "+u+" with not valid role "+rr);
  }
  
  static public User getUserFromRequest(HttpServletRequest r) throws Exception {
    String token = r.getParameter("accessToken");
    if(token==null) {
      String a = r.getHeader("Authorization");
      if(a!=null) {
        String[] auth = a.split(" ");
        if(auth.length==2 && auth[0].equals("Bearer")) {
          token = auth[1];
        }
      }
    }
    if(token!=null)
      return getUserFromJwt(token);
    return null;
  }
  
  static private void loadPublicKey() throws Exception {
    Configuration conf = Configuration.getInstance();
    
    JsonParser p = new JsonParser();
    BufferedReader rd = new BufferedReader(new InputStreamReader(new URL(conf.get("jwtCerts", "https://www.snap4city.org/auth/realms/master/protocol/openid-connect/certs")).openStream(), Charset.forName("UTF-8")));
    JsonArray keys = p.parse(rd).getAsJsonObject().getAsJsonArray("keys");

    JsonObject keyInfo = null;
    if(keys.size()>0) {
      keyInfo = keys.get(0).getAsJsonObject();
    } else  {
      throw new Exception("no keys found for jwt validation");
    }

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    String modulusBase64 = keyInfo.get("n").getAsString();
    String exponentBase64 = keyInfo.get("e").getAsString();

    // see org.keycloak.jose.jwk.JWKBuilder#rs256
    Base64.Decoder urlDecoder = Base64.getUrlDecoder();
    BigInteger modulus = new BigInteger(1, urlDecoder.decode(modulusBase64));
    BigInteger publicExponent = new BigInteger(1, urlDecoder.decode(exponentBase64));

    pk = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));

  }
}
