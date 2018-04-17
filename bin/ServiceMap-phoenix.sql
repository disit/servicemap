CREATE TABLE AccessLog (
  id BIGINT NOT NULL,
  timestamp timestamp,
  mode varchar(45) DEFAULT NULL,
  ip varchar(45) DEFAULT NULL,
  userAgent varchar(255) DEFAULT NULL,
  uid varchar(255) DEFAULT NULL,
  serviceUri varchar(255) DEFAULT NULL,
  selection varchar DEFAULT NULL,
  categories varchar,
  maxResults varchar(255) DEFAULT NULL,
  maxDistance varchar(255) DEFAULT NULL,
  reqfrom varchar(45) DEFAULT NULL,
  text varchar DEFAULT NULL,
  queryId varchar(45) DEFAULT NULL,
  format varchar(45) DEFAULT NULL,
  email varchar(45) DEFAULT NULL,
  referer varchar DEFAULT NULL,
  site varchar DEFAULT NULL,
  CONSTRAINT pk PRIMARY KEY (id)
)

create sequence accesslog_sequence;
