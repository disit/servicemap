source ./update-ontology.sh

mkdir -p /var/lib/tomcat8/servicemap
mkdir -p /var/log/tomcat8/servicemap
chown tomcat8.tomcat8 /var/log/tomcat8/servicemap
cp servicemap.properties /var/lib/tomcat8/servicemap

#initializes virtuoso
isql-vt localhost dba dba ServiceMap.vt

#ingest museums and historical building from dbpedia
isql-vt localhost dba dba servicemap-dbpedia.vt

