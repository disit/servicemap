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
package org.disit.servicemap.api;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.disit.servicemap.Configuration;

/**
 *
 * @author bellini
 */
public class LdapSearch {
  private static LdapSearch instance = null; 
  private final LDAPConnectionPool pool;
  private final String baseDN;
  private final String defaultOrg;
  
  public static LdapSearch getInstance() throws LDAPException {
    if(instance==null)
      instance = new LdapSearch();
    return instance;
  }
  
  public LdapSearch() throws LDAPException {
    Configuration conf = Configuration.getInstance();
    String host = conf.get("ldapHost", "localhost");
    int port = Integer.parseInt(conf.get("ldapPort", "389"));
    String bindDN = conf.get("ldapBindDN", "cn=admin,dc=ldap,dc=disit,dc=org");
    String passw = conf.get("ldapBindPassword", "password");
    baseDN = conf.get("ldapBaseDN", "dc=ldap,dc=disit,dc=org");
    defaultOrg = conf.get("ldapDefaultOrganization", "DISIT");
    LDAPConnection c = new LDAPConnection(host, port, bindDN, passw);
    pool = new LDAPConnectionPool(c, 1, Integer.parseInt(conf.get("ldapMaxPoolConnections", "10")));
  }
  
  public List<String> getOrganization(String user) throws LDAPException {
    String userDN = new DN(new RDN("cn", user), new DN(baseDN)).toString();
    Filter query = Filter.createANDFilter(
        Filter.createEqualityFilter("objectClass", "organizationalUnit"),
        Filter.createEqualityFilter("l", userDN));
    SearchRequest sr = new SearchRequest(baseDN, SearchScope.ONE, query, "ou");
    Set<String> organizations = new LinkedHashSet<String>();
    LDAPConnection c = pool.getConnection();
    try {
      SearchResult result = c.search(sr);
      if(result.getEntryCount()>0) {
        for(SearchResultEntry e : result.getSearchEntries()) {
          RDN rdn = e.getRDN();
          if(rdn.hasAttribute("ou")) {
            for(String value : rdn.getAttributeValues()) {
              if(value!=null && !value.isEmpty()) {
                organizations.add(value);
              }
            }
          }
        }
        if(organizations.isEmpty() && result.getEntryCount()>1) {
          organizations.add(defaultOrg);
        }
      }
    } finally {
      c.close();
    }
    return new ArrayList<String>(organizations);
  }
 
  public List<String> getGroups(String user, String organization) throws LDAPException {
    String userDN = new DN(new RDN("cn", user), new DN(baseDN)).toString();
    String safeOrgDN = new DN(new RDN("ou", organization), new DN(baseDN)).toString();
    Filter query = Filter.createANDFilter(
        Filter.createEqualityFilter("objectClass", "groupOfNames"),
        Filter.createEqualityFilter("member", userDN));
    SearchRequest sr = new SearchRequest(safeOrgDN, SearchScope.ONE, query);
    List<String> groups = new ArrayList<String>();
    LDAPConnection c = pool.getConnection();
    try {
      SearchResult result = c.search(sr);
      for(SearchResultEntry e : result.getSearchEntries()) {
        RDN rdn = e.getRDN();
        
        if(rdn.hasAttribute("cn")) {
          String group = rdn.getAttributeValues()[0];
          groups.add(group);
        }
      }
    } finally {
      c.close();
    }
    return groups;
  }
  
}
