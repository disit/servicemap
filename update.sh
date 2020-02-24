service tomcat8 stop
cp dist/ServiceMap.war /var/lib/tomcat8/webapps/
rm -rf /var/lib/tomcat8/webapps/ServiceMap
service tomcat8 start
