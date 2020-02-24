CREATE SCHEMA ServiceMap;
USE ServiceMap;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `AccessLog`
--

DROP TABLE IF EXISTS `AccessLog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `AccessLog` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `mode` varchar(45) DEFAULT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `userAgent` varchar(255) DEFAULT NULL,
  `uid` varchar(255) DEFAULT NULL,
  `serviceUri` varchar(255) DEFAULT NULL,
  `selection` text,
  `categories` text,
  `maxResults` varchar(255) DEFAULT NULL,
  `maxDistance` varchar(255) DEFAULT NULL,
  `reqfrom` varchar(45) DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `queryId` varchar(45) DEFAULT NULL,
  `format` varchar(45) DEFAULT NULL,
  `email` varchar(45) DEFAULT NULL,
  `referer` varchar(255) DEFAULT NULL,
  `site` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `timestamp` (`timestamp`),
  KEY `mode` (`mode`),
  KEY `ip` (`ip`),
  KEY `uid` (`uid`)
) ENGINE=InnoDB AUTO_INCREMENT=145797576 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ApiKey`
--

DROP TABLE IF EXISTS `ApiKey`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ApiKey` (
  `idApiKey` int(11) NOT NULL AUTO_INCREMENT,
  `key` varchar(255) NOT NULL,
  `dateFrom` datetime NOT NULL,
  `dateTo` datetime DEFAULT NULL,
  `valid` tinyint(4) NOT NULL,
  `username` varchar(45) DEFAULT NULL,
  `description` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`idApiKey`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Geometry`
--

DROP TABLE IF EXISTS `Geometry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Geometry` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `label_id` varchar(30) NOT NULL,
  `label` varchar(30) NOT NULL,
  `wkt` longtext NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4232 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PrivateData`
--

DROP TABLE IF EXISTS `PrivateData`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `PrivateData` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `graphUri` varchar(255) DEFAULT NULL,
  `idApiKey` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Queries`
--

DROP TABLE IF EXISTS `Queries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Queries` (
  `id` varchar(45) NOT NULL DEFAULT '0',
  `email` varchar(128) DEFAULT NULL,
  `description` text,
  `categorie` longtext,
  `numeroRisultatiServizi` int(10) unsigned DEFAULT NULL,
  `numeroRisultatiSensori` int(11) DEFAULT NULL,
  `numeroRisultatiBus` int(11) DEFAULT NULL,
  `actualSelection` varchar(128) DEFAULT NULL,
  `raggioServizi` varchar(50) DEFAULT NULL,
  `raggioSensori` varchar(50) DEFAULT NULL,
  `raggioBus` varchar(50) DEFAULT NULL,
  `idRW` varchar(128) NOT NULL DEFAULT '',
  `nomeProvincia` varchar(45) DEFAULT NULL,
  `nomeComune` varchar(128) DEFAULT NULL,
  `parentQuery` varchar(128) DEFAULT NULL,
  `line` varchar(45) DEFAULT NULL,
  `stop` varchar(45) DEFAULT NULL,
  `coordinateSelezione` varchar(128) DEFAULT NULL,
  `title` text,
  `idService` varchar(128) DEFAULT NULL,
  `typeService` varchar(45) DEFAULT NULL,
  `nameService` varchar(128) DEFAULT NULL,
  `zoom` int(11) DEFAULT NULL,
  `center` varchar(128) DEFAULT NULL,
  `weatherCity` varchar(45) DEFAULT NULL,
  `popupOpen` text,
  `confR` varchar(128) DEFAULT NULL,
  `typeSaving` varchar(45) DEFAULT NULL,
  `text` longtext,
  `timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServiceCategory_menu_NEW`
--

DROP TABLE IF EXISTS `ServiceCategory_menu_NEW`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ServiceCategory_menu_NEW` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `labelITA` varchar(200) DEFAULT NULL,
  `SubClass` varchar(200) DEFAULT NULL,
  `MacroClass` varchar(200) DEFAULT NULL,
  `ItaAdditional` varchar(200) DEFAULT NULL,
  `TypeOfService` varchar(45) DEFAULT 'Service',
  `Visible` int(11) DEFAULT '1',
  `icon` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1543 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServiceComment`
--

DROP TABLE IF EXISTS `ServiceComment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ServiceComment` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `serviceUri` varchar(255) NOT NULL DEFAULT '',
  `serviceName` varchar(255) NOT NULL DEFAULT '',
  `uid` varchar(255) NOT NULL DEFAULT '',
  `comment` text CHARACTER SET utf8,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` enum('submitted','validated','rejected') NOT NULL DEFAULT 'submitted',
  `longitude` varchar(45) DEFAULT NULL,
  `latitude` varchar(45) DEFAULT NULL,
  `city` varchar(45) DEFAULT NULL,
  `province` varchar(45) DEFAULT NULL,
  `address` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `serviceUri` (`serviceUri`)
) ENGINE=InnoDB AUTO_INCREMENT=176 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServiceLimit`
--

DROP TABLE IF EXISTS `ServiceLimit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ServiceLimit` (
  `ipaddr` varchar(100) NOT NULL,
  `requestType` varchar(45) NOT NULL,
  `date` date NOT NULL DEFAULT '0000-00-00',
  `doneCount` int(11) NOT NULL DEFAULT '0',
  `limitedCount` int(11) NOT NULL DEFAULT '0',
  `resultsCount` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`date`,`requestType`,`ipaddr`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServiceMapping`
--

DROP TABLE IF EXISTS `ServiceMapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ServiceMapping` (
  `serviceType` varchar(50) NOT NULL,
  `apiVersion` varchar(45) NOT NULL DEFAULT '1',
  `priority` int(11) NOT NULL DEFAULT '100',
  `section` varchar(45) DEFAULT 'Service',
  `serviceDetailsSparqlQuery` text,
  `serviceAttributesSparqlQuery` text,
  `serviceRealTimeSparqlQuery` text,
  `serviceRealTimeSqlQuery` text,
  `serviceRealTimeSolrQuery` text,
  `servicePredictionSqlQuery` text,
  `serviceTrendSqlQuery` text,
  PRIMARY KEY (`serviceType`,`apiVersion`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServicePhoto`
--

DROP TABLE IF EXISTS `ServicePhoto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ServicePhoto` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `serviceUri` varchar(255) NOT NULL DEFAULT '',
  `serviceName` varchar(255) DEFAULT NULL,
  `uid` varchar(255) NOT NULL DEFAULT '',
  `file` varchar(255) NOT NULL DEFAULT '',
  `status` enum('submitted','validated','rejected') NOT NULL DEFAULT 'submitted',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ip` varchar(45) NOT NULL DEFAULT '',
  `userAgent` text,
  `longitude` varchar(45) DEFAULT NULL,
  `latitude` varchar(45) DEFAULT NULL,
  `city` varchar(45) DEFAULT NULL,
  `province` varchar(45) DEFAULT NULL,
  `address` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `serviceUri` (`serviceUri`)
) ENGINE=InnoDB AUTO_INCREMENT=368 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServiceStars`
--

DROP TABLE IF EXISTS `ServiceStars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ServiceStars` (
  `serviceUri` varchar(255) NOT NULL DEFAULT '',
  `uid` varchar(255) NOT NULL DEFAULT '',
  `stars` int(10) unsigned NOT NULL DEFAULT '0',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`serviceUri`,`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServiceValues`
--

DROP TABLE IF EXISTS `ServiceValues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ServiceValues` (
  `serviceUri` varchar(200) NOT NULL,
  `valueName` varchar(200) NOT NULL,
  `valueUnit` varchar(45) DEFAULT NULL,
  `valueType` varchar(45) DEFAULT NULL,
  `dataType` varchar(45) DEFAULT NULL,
  `refresh_rate` int(11) DEFAULT NULL,
  `different_values` int(11) DEFAULT NULL,
  `value_bounds` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`serviceUri`,`valueName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2019-09-19 16:55:19
