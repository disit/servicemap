//this class provides a database connection pool, using org.apache.commons.dbcp and org.apache.commons.pool libraries
package org.disit.servicemap;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import javax.sql.DataSource;

/**
 *
 * @author Daniele Cenni, daniele.cenni@unifi.it
 */
public class ConnectionPool {

  /**
   *
   */
  public static final String DRIVER = "com.mysql.jdbc.Driver";

  /**
   *
   */
  public String URL;

  /**
   *
   */
  public String USERNAME;

  /**
   *
   */
  public String PASSWORD;

  /**
   *
   */
  public int connections;
  public int maxWait;
  public String type;

  private GenericObjectPool connectionPool = null;

  private static ConnectionPool connPool;
  private static DataSource dataSource;
  
  /**
   *
   * @param url
   * @param username
   * @param password
   * @throws IOException
   */
  public ConnectionPool(String url, String username, String password, int maxConnections, String type, int maxWait) throws IOException {
    URL = url;
    USERNAME = username;
    PASSWORD = password;
    connections = maxConnections;
    this.maxWait = maxWait;
    this.type = type;
  }

  /**
   *
   * @return @throws Exception
   */
  public DataSource setUp()  {
    try {
      /**
       * Load JDBC Driver class.
       */
      Class.forName(ConnectionPool.DRIVER).newInstance();
    } catch (ClassNotFoundException ex) {
      Logger.getLogger(ConnectionPool.class.getName()).log(Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      Logger.getLogger(ConnectionPool.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      Logger.getLogger(ConnectionPool.class.getName()).log(Level.SEVERE, null, ex);
    }

    /**
     * Creates an instance of GenericObjectPool that holds our pool of
     * connections object.
     */
    Configuration conf = Configuration.getInstance();
    connectionPool = new GenericObjectPool();
    // set the max number of connections
    connectionPool.setMaxActive(connections);
    // if the pool is exhausted (i.e., the maximum number of active objects has been reached), the borrowObject() method should simply create a new object anyway
    if("block".equals(type))
      connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
    else if("fail".equals(type))
      connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
    else
      connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
    connectionPool.setMaxWait(maxWait);
    /**
     * Creates a connection factory object which will be use by the pool to
     * create the connection object. We passes the JDBC url info, username and
     * password.
     */
    ConnectionFactory cf = new DriverManagerConnectionFactory(
            URL,
            USERNAME,
            PASSWORD);

    /**
     * Creates a PoolableConnectionFactory that will wraps the connection object
     * created by the ConnectionFactory to add object pooling functionality.
     */
    String validationQuery = conf.get("validationQueryMySql", "SELECT 1");
    if(validationQuery.equals("null") || validationQuery.trim().isEmpty())
      validationQuery = null;
    else
      connectionPool.setTestOnBorrow(true);

    PoolableConnectionFactory pcf
            = new PoolableConnectionFactory(cf, connectionPool,
                    null, validationQuery, false, true);
    return new PoolingDataSource(connectionPool);
  }

  /**
   *
   * @return
   */
  public GenericObjectPool getConnectionPool() {
    return connectionPool;
  }

  // Prints connection pool status
  public void printStatus() {
    System.out.println("ConnectionPool Max   : " + getConnectionPool().getMaxActive() + "; "
            + "Active: " + getConnectionPool().getNumActive() + "; "
            + "Idle  : " + getConnectionPool().getNumIdle());
  }
  
  public static String getStatus() {
    if(connPool == null)
      return "NO POOL";
    GenericObjectPool cp = connPool.getConnectionPool();
    
    return "Max: " + cp.getMaxActive() + "; "
            + "Active: " + cp.getNumActive() + "; "
            + "Idle: " + cp.getNumIdle();
  }

  public static Connection getConnection() throws IOException, SQLException {
    if (connPool == null || dataSource == null) {
      Configuration conf = Configuration.getInstance();
      String url = conf.get("urlMySqlDB", "")+conf.get("dbMySql", "ServiceMap")+"?useUnicode=true&characterEncoding=utf-8&autoReconnect=true";
      int maxConnections = Integer.parseInt(conf.get("maxConnectionsMySql", "10"));
      int maxWait = Integer.parseInt(conf.get("maxWaitMySql", "1000"));
      String poolType = conf.get("poolTypeMySql", "block");
      synchronized(ConnectionPool.class) {
        if(connPool==null) {
          connPool = new ConnectionPool(url, conf.get("userMySql", ""), conf.get("passMySql", ""),maxConnections, poolType, maxWait);
          System.out.println("connected "+url+" maxConnections: "+maxConnections);
        }
        if (dataSource == null) {
          dataSource = connPool.setUp();
        }
      }
    } else {
      //connPool.printStatus();
    }

    return dataSource.getConnection();
  }
}
