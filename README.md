**ServiceMap**

Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

**Dependencies**

- Tomcat 7
- Mysql 5.5+
- Virtuoso 7.2

**Installation Guide**

- Install Tomcat 7, Mysql and Virtuoso
- In Mysql created the database ServiceMap and create the tables using the bin/ServiceMap.sql
- Build the .war or use the one provided in the bin directory
- Copy the file bin/servicemap.properties into the User home into servicemap/servicemap.properties (e.g. /usr/share/tomcat7/servicemap/servicemap.properties)
- Edit the servicemap.properties to set al least the db connection properties (user and password), and the token of mapbox to be used for map requests.
- deploy the .war
- open http://localhost:8080/ServiceMap
- in case of problems you can use http://localhost:8080/ServiceMap/conf.jsp (only from localhost or 192.168.0.x ip address) to see the configuration loaded

