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
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
  private long cacheMaxAgeNanos;
  private int cacheMaxEntries;
  private long cacheGeneration;
  private long cacheHits;
  private long cacheMisses;
  private final Map<List<String>, CacheEntry> cache =
      new LinkedHashMap<List<String>, CacheEntry>(16, 0.75f, true);

  private static class CacheEntry {
    final long createdAt = System.nanoTime();
    final List<String> values;

    CacheEntry(List<String> values) {
      this.values = new ArrayList<String>(values);
    }
  }
  
  public static synchronized LdapSearch getInstance() throws LDAPException {
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
    resetCache();
    LDAPConnection c = new LDAPConnection(host, port, bindDN, passw);
    pool = new LDAPConnectionPool(c, 1, Integer.parseInt(conf.get("ldapMaxPoolConnections", "10")));
    pool.setCreateIfNecessary(conf.get("ldapPoolCreateIfNecessary", "false").equals("true"));
    pool.setMaxWaitTimeMillis(Integer.parseInt(conf.get("ldapMaxPoolWaitMillis", "1000")));
  }
  
  public List<String> getOrganization(String user) throws LDAPException {
    String userDN = new DN(new RDN("cn", user), new DN(baseDN)).toString();
    List<String> cacheKey = Arrays.asList("organizations", userDN);
    long generation = getCacheGeneration();
    List<String> cached = getCached(cacheKey);
    if(cached!=null)
      return cached;
    Filter query = Filter.createANDFilter(
        Filter.createEqualityFilter("objectClass", "organizationalUnit"),
        Filter.createEqualityFilter("l", userDN));
    SearchRequest sr = new SearchRequest(baseDN, SearchScope.ONE, query, "ou");
    Set<String> organizations = new LinkedHashSet<String>();
    SearchResult result = pool.search(sr);
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
    List<String> values = new ArrayList<String>(organizations);
    cacheResult(cacheKey, values, generation);
    return values;
  }
 
  public List<String> getGroups(String user, String organization) throws LDAPException {
    String userDN = new DN(new RDN("cn", user), new DN(baseDN)).toString();
    String safeSearchBaseDN = new DN(baseDN).toString();
    if(organization!=null && !organization.isEmpty())
      safeSearchBaseDN = new DN(new RDN("ou", organization), new DN(baseDN)).toString();
    List<String> cacheKey = Arrays.asList("groups", userDN, safeSearchBaseDN);
    long generation = getCacheGeneration();
    List<String> cached = getCached(cacheKey);
    if(cached!=null)
      return cached;
    
    Filter query = Filter.createANDFilter(
        Filter.createEqualityFilter("objectClass", "groupOfNames"),
        Filter.createEqualityFilter("member", userDN));
    SearchRequest sr = new SearchRequest(safeSearchBaseDN, SearchScope.ONE, query);
    List<String> groups = new ArrayList<String>();
    SearchResult result = pool.search(sr);
    for(SearchResultEntry e : result.getSearchEntries()) {
      RDN rdn = e.getRDN();

      if(rdn.hasAttribute("cn")) {
        String group = rdn.getAttributeValues()[0];
        groups.add(group);
      }
    }
    cacheResult(cacheKey, groups, generation);
    return groups;
  }

  private synchronized List<String> getCached(List<String> key) {
    if(cacheMaxAgeNanos<=0 || cacheMaxEntries<=0) {
      cacheMisses++;
      return null;
    }
    CacheEntry entry = cache.get(key);
    if(entry==null) {
      cacheMisses++;
      return null;
    }
    if(System.nanoTime()-entry.createdAt>=cacheMaxAgeNanos) {
      cache.remove(key);
      cacheMisses++;
      return null;
    }
    cacheHits++;
    return new ArrayList<String>(entry.values);
  }

  /**
   * Returns cache statistics since the last settings change. Misses include
   * expired entries, disabled caching and lookups followed by LDAP errors.
   * Entry count includes expired entries not yet removed or replaced.
   */
  public synchronized String getCacheStats() {
    long requests = cacheHits + cacheMisses;
    double hitRate = requests==0 ? 0.0 : cacheHits * 100.0 / requests;
    return String.format(Locale.ROOT,
        "LdapSearch cache (enabled: %s, requests: %d, hits: %d, misses: %d, hit rate: %.2f%%, entries: %d/%d)",
        cacheMaxAgeNanos>0 && cacheMaxEntries>0, requests, cacheHits, cacheMisses,
        hitRate, cache.size(), cacheMaxEntries);
  }

  /**
   * Reads cache settings from the current Configuration instance. The caller
   * must reload Configuration first to pick up changes to the properties file.
   * Clears cached results and statistics only when TTL or capacity changed.
   *
   * @return true if settings changed and the cache was reset
   */
  public synchronized boolean resetCache() {
    Configuration conf = Configuration.getInstance();
    // TTL in seconds; setting either option to zero disables caching.
    long maxAgeNanos = TimeUnit.SECONDS.toNanos(Long.parseLong(conf.get("ldapCacheMaxTime", "60")));
    int maxEntries = Integer.parseInt(conf.get("ldapCacheMaxEntries", "10000"));
    if(cacheMaxAgeNanos==maxAgeNanos && cacheMaxEntries==maxEntries)
      return false;
    cacheMaxAgeNanos = maxAgeNanos;
    cacheMaxEntries = maxEntries;
    cacheGeneration++;
    cache.clear();
    cacheHits = 0;
    cacheMisses = 0;
    return true;
  }

  private synchronized long getCacheGeneration() {
    return cacheGeneration;
  }

  private synchronized void cacheResult(List<String> key, List<String> values, long generation) {
    if(generation!=cacheGeneration || cacheMaxAgeNanos<=0 || cacheMaxEntries<=0)
      return;
    cache.put(key, new CacheEntry(values));
    if(cache.size()>cacheMaxEntries)
      cache.remove(cache.keySet().iterator().next());
  }
  
}
