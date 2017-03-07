<%/* ServiceMap.
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
%>
<%@page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@taglib prefix='c' uri='http://java.sun.com/jsp/jstl/core' %>
<%@include file="/include/parameters.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <title>ServiceMap</title>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/leaflet-gps.css" type="text/css" />
        <script src='https://api.tiles.mapbox.com/mapbox.js/v2.1.4/mapbox.js'></script>
        <link href='https://api.tiles.mapbox.com/mapbox.js/v2.1.4/mapbox.css' rel='stylesheet' />
        <script src="http://code.jquery.com/jquery-1.11.2.min.js"></script>
        <script src="http://code.jquery.com/jquery-migrate-1.2.1.min.js"></script>
        <script src="http://code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
        <link rel="stylesheet" href="http://code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css" />	
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/leaflet.awesome-markers.css">
        <script src="${pageContext.request.contextPath}/js/leaflet.awesome-markers.min.js"></script>
        <script src="${pageContext.request.contextPath}/js/jquery.dialogextend.js"></script>
        <script src="${pageContext.request.contextPath}/js/leaflet-gps.js"></script>
        <script src="${pageContext.request.contextPath}/js/leaflet.markercluster.js"></script>
        <script src="${pageContext.request.contextPath}/js/mustache.js"></script>
        <script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
        <!-- code per gallery -->
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/lib/jquery.mousewheel-3.0.6.pack.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/fancybox/source/jquery.fancybox.css" type="text/css" media="screen" />
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/source/jquery.fancybox.pack.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-buttons.css" type="text/css" media="screen" />
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-buttons.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-media.js"></script>
        <script src="${pageContext.request.contextPath}/js/wicket.js"></script>
        <script src="${pageContext.request.contextPath}/js/wicket-leaflet.js"></script>

        <link rel="stylesheet" href="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-thumbs.css" type="text/css" media="screen" />
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-thumbs.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/MarkerCluster.css" type="text/css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/MarkerCluster.Default.css" type="text/css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" type="text/css" />
    </head>
    <body class="Chrome">
        <div id="dialog"></div>        
        <div id="mappa" style="height:80%;width:100%">
            <div class="menu" id="help">
                <a href="http://www.disit.org/servicemap" title="Aiuto Service Map" target="_blank"><img src="${pageContext.request.contextPath}/img/help.png" alt="help SiiMobility ServiceMap" width="28" /></a>
            </div>
        </div>
        <div id="query" >
            <textarea style="width:100%;height:20%;">
PREFIX ogis:<http://www.opengis.net/ont/geosparql#>
select distinct ?wkt { 
  ?p a km4c:Path.
  ?p rdf:first/geo:geometry ?g1. 
  ?p km4c:distFromSink ?d. 
  ?p ogis:asWKT ?wkt. 
  filter(bif:st_x(?g1)>=$WEST && bif:st_x(?g1)<=$EAST && bif:st_y(?g1)>=$SOUTH && bif:st_y(?g1)<=$NORTH) 
} order by desc(?d) limit 1000
        </textarea>
        </div>
        <script>
            var ctx = "${pageContext.request.contextPath}";
            Array.prototype.equals = function (array) {
                // if the other array is a falsy value, return
                if (!array)
                    return false;
                // compare lengths - can save a lot of time 
                if (this.length != array.length)
                    return false;
                for (var i = 0, l = this.length; i < l; i++) {
                    // Check if we have nested arrays
                    if (this[i] instanceof Array && array[i] instanceof Array) {
                        // recurse into the nested arrays
                        if (!this[i].equals(array[i]))
                            return false;
                    }
                    else if (this[i] != array[i]) {
                        // Warning - two different object instances will never be equal: {x:20} != {x:20}
                        return false;
                    }
                }
                return true;
            }


            /***  codice per mantenere aperto più di un popup per volta ***/
            L.Map = L.Map.extend({
                openPopup: function (popup) {
                    //        this.closePopup();  // just comment this
                    this._popup = popup;
                    return this.addLayer(popup);
                }

            });

            var mbAttr = 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
                    '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
                    'Imagery © <a href="http://mapbox.com">Mapbox</a>',
                    mbUrl = 'https://{s}.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=<%=mapAccessToken%>';
            var streets = L.tileLayer(mbUrl, {id: 'mapbox.streets', attribution: mbAttr}),
                    satellite = L.tileLayer(mbUrl, {id: 'mapbox.streets-satellite', attribution: mbAttr}),
                    grayscale = L.tileLayer(mbUrl, {id: 'pbellini.f33fdbb7', attribution: mbAttr});
            var map = L.map('mappa', {
                center: [43.3555664, 11.0290384],
                zoom: 8,
                layers: [satellite]
            });
            var baseMaps = {
                "Streets": streets,
                "Satellite": satellite,
                "Grayscale": grayscale
            };
            var toggleMap = L.control.layers(baseMaps, null, {position: 'bottomright', width: '50px', height: '50px'});
            toggleMap.addTo(map);

            // DEFINIZIONE DEI CONFINI MASSIMI DELLA MAPPA
            var bounds = new L.LatLngBounds(new L.LatLng(41.7, 8.4), new L.LatLng(44.930222, 13.4));
            map.setMaxBounds(bounds);
            // AGGIUNTA DEL PLUGIN PER LA GEOLOCALIZZAZIONE
            var GPSControl = new L.Control.Gps({
                maxZoom: 16,
                style: null
            });
            map.addControl(GPSControl);
            map.on('click', function (e) {
                        //var latLngPunto = e.latlng;
                        //coordinateSelezione = latLngPunto.lat + ";" + latLngPunto.lng;
                var bnds = map.getBounds()
                console.log(bnds.getSouth() + ";" + bnds.getWest() + ";" + bnds.getNorth() + ";" + bnds.getEast());
                //query="select ?s ?wkt { ?s a km4c:Junction. ?s geo:geometry ?wkt. filter(bif:st_x(?wkt)>=" + bnds.getWest() + " && bif:st_x(?wkt)<=" + bnds.getEast() + " && bif:st_y(?wkt)>=" + bnds.getSouth() + " && bif:st_y(?wkt)<=" + bnds.getNorth() + ")} limit 1000"
                //query="select ?s ?wkt { ?s a km4c:RoadElement. ?s km4c:startsAtNode ?n1. ?s km4c:endsAtNode ?n2. ?n1 geo:geometry ?g1. ?n2 geo:geometry ?g2. filter(bif:st_x(?g1)>=" + bnds.getWest() + " && bif:st_x(?g1)<=" + bnds.getEast() + " && bif:st_y(?g1)>=" + bnds.getSouth() + " && bif:st_y(?g1)<=" + bnds.getNorth() + ") BIND(concat('linestring(',bif:st_x(?g1),' ',bif:st_y(?g1),',',bif:st_x(?g2),' ',bif:st_y(?g2),')') as ?wkt)} limit 1000"
                //query="select distinct ?wkt { ?p a km4c:Path. ?p rdf:first/geo:geometry ?g1. ?p km4c:distFromSink ?d. ?p <http://www.opengis.net/ont/geosparql#asWKT> ?wkt. filter(bif:st_x(?g1)>=" + bnds.getWest() + " && bif:st_x(?g1)<=" + bnds.getEast() + " && bif:st_y(?g1)>=" + bnds.getSouth() + " && bif:st_y(?g1)<=" + bnds.getNorth() + ") BIND(concat('linestring(',bif:st_x(?g1),' ',bif:st_y(?g1),',',bif:st_x(?g2),' ',bif:st_y(?g2),')') as ?wkt)} order by desc(?d) limit 1000"
                query = $("#query textarea").val();
                query = query.replace(/\\$WEST/g,""+bnds.getWest())
                query = query.replace(/\\$EAST/g,""+bnds.getEast())
                query = query.replace(/\\$SOUTH/g,""+bnds.getSouth())
                query = query.replace(/\\$NORTH/g,""+bnds.getNorth())
                query = query.replace(/\\$CLICK_LAT/g,""+e.latlng.lat)
                query = query.replace(/\\$CLICK_LNG/g,""+e.latlng.lng)
                alert(query);
                geoQuery(query);
            });
            var wktLayer = new L.LayerGroup();


            $(document).ready(function () {
                // funzione di inizializzazione all'avvio della mappa
                //query="select ?s ?wkt { ?s geo:geometry ?wkt.} limit 100";
                //geoQuery(query);
            });
            function geoQuery(query) {
                defaults = {
                icon: new L.DivIcon(),
                editable: true,
                color: '#AA0000',
                weight: 3,
                opacity: 0.5,
                fillColor: '#AA0000',
                fillOpacity: 0.2
                };

                $.ajax({
                    data: {
                        query: query
                    },
                    url: "ajax/geoquery.jsp",
                    type: "GET",
                    async: true,
                    dataType: 'json',
                    success: function (data) {
                      var rx;
                      wktLayer.clearLayers();
                      for(rx=0; rx<data.length; rx++) {
                        console.log(data[rx].wkt);
                        try {
                          var wkt = new Wkt.Wkt();
                          wkt.read(data[rx].wkt);
                          var obj = wkt.toObject(defaults);
                          obj.addTo(wktLayer);
                          obj.on('click',function() {alert(data[rx].s)})
                        } catch(e) {
                          console.log(e);
                        }
                      }
                      wktLayer.addTo(map);
                      //map.fitBounds(wktLayer.getLayers()[0].getBounds());
                    },
                    error: function(err) {
                      alert(err);
                    }
                });
            }
        </script>
    </body>
    <div id="overMap"></div>
</html>    