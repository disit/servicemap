/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.disit.servicemap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolver;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.security.SignatureException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author bellini
 */
public class JwtUtil {
  static private SigningKeyResolver skr = null;
  
  static public boolean isValid(String accessToken) {
    if(skr==null) {
      try {
        loadPublicKeys();
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
    if(skr==null)
      return false;
    try {
      Jwts.parser().setSigningKeyResolver(skr).parseClaimsJws(accessToken);
      return true;
    } catch(Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  
  static public class User {
    final public String username;
    final public String role;
    final public String accessToken;
    
    User(String u, String r, String at) {
      username = u;
      role = r;
      accessToken = at;
    }
  }
  
  static public User getUserFromJwt(String accessToken) throws Exception {
    if(skr == null)
      loadPublicKeys();
    
    String roles[] = new String[] {"RootAdmin", "ToolAdmin", "AreaManager", "Manager", "Observer"};
    Jws<Claims> t = Jwts.parser().setSigningKeyResolver(skr).setAllowedClockSkewSeconds(10).parseClaimsJws(accessToken);
    String u = (String) t.getBody().get("username");
    if(u==null)
      u = (String) t.getBody().get("preferred_username");
    if(u==null)
      throw new Exception("username not found in "+accessToken);
    List<String> rr = null;
    Map<String,List<String>> x = (Map<String,List<String>>) t.getBody().get("realm_access");
    if(x!=null) {
      rr = x.get("roles");
    } else {
      rr = (List<String>) t.getBody().get("roles");
    }
    if(rr!=null) {
      for(String role: roles) {
        if(rr.contains(role))
          return new User(u,role, accessToken);
      }
      throw new Exception("user "+u+" with not valid role "+rr+" in "+accessToken);
    }
    throw new Exception("user "+u+" with no roles found in "+accessToken);
  }
  
  static public String getTokenFromRequest(HttpServletRequest r) throws Exception {
    String token = r.getParameter("accessToken");
    if(token==null || token.equals("null")) {
      token = null;
      String a = r.getHeader("Authorization");
      if(a != null) {
        String[] auth = a.split(" ");
        if(auth.length==2 && auth[0].equals("Bearer")) {
          token = auth[1];
        }
      }
    }
    return token;
  }
  
  static public User getUserFromRequest(HttpServletRequest r) throws Exception {
    try {
      String token = getTokenFromRequest(r);
      if(token!=null)
        return getUserFromJwt(token);
    } catch(SignatureException e) {
      Configuration conf = Configuration.getInstance();
      if(!conf.get("disableJwtSignatureException", "false").equals("true")) {
        throw e;
      }
    }
    return null;
  }
  
  static private void loadPublicKeys() throws Exception {
    Configuration conf = Configuration.getInstance();
    
    JsonParser p = new JsonParser();
    BufferedReader rd = new BufferedReader(new InputStreamReader(new URL(conf.get("jwtCerts", "https://www.snap4city.org/auth/realms/master/protocol/openid-connect/certs")).openStream(), Charset.forName("UTF-8")));
    JsonArray keys = p.parse(rd).getAsJsonObject().getAsJsonArray("keys");

    JsonObject keyInfo = null;
    if(keys.size()==0) {
      throw new Exception("no keys found for jwt validation");
    }

    Map<String,PublicKey> map = new HashMap<>();
    for(int i=0; i<keys.size(); i++) {
        keyInfo = keys.get(i).getAsJsonObject();
        
        String kid = keyInfo.get("kid").getAsString();
        String kty = keyInfo.get("kty").getAsString();
        if("RSA".equals(kty)) {
            String modulusBase64 = keyInfo.get("n").getAsString();
            String exponentBase64 = keyInfo.get("e").getAsString();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            // see org.keycloak.jose.jwk.JWKBuilder#rs256
            Base64.Decoder urlDecoder = Base64.getUrlDecoder();
            BigInteger modulus = new BigInteger(1, urlDecoder.decode(modulusBase64));
            BigInteger publicExponent = new BigInteger(1, urlDecoder.decode(exponentBase64));

            PublicKey key = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
            map.put(kid, key);
        } else {
            ServiceMap.println("key "+kid+" kty: "+kty+" not RSA, skipped");
        }
    }
    
    skr = new SigningKeyResolverAdapter() {
        @Override
        public Key resolveSigningKey(JwsHeader jh, Claims claims) {
            return map.get(jh.getKeyId());
        }
    };

  }
  
  static public void reset() {
    skr = null;
  }
}
