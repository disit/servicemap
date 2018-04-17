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

var pathStart = "";
var pathEnd = "";
var pathPins = {"start":null, "end":null, "position": null};
var pathLayer = null;
var pathResults = null;
var pathShown = null;
var pathDefaults = {
      icon: new L.DivIcon(),
      editable: true,
      color: '#AA0000',
      weight: 4,
      opacity: 0.6,
      fillColor: '#AA0000',
      fillOpacity: 0.2
      };
var pathColors = ["#FF0000","#0000FF","#00FF00","#AEAE00","#00AEAE","#AE00AE"];
var tplPathsColors = ["#FFF","#888","#AAA","#555"];

function setStartSearchPath(lat,long) {
  pathStart = lat+";"+long;
  $("#path_start").html("From: "+$("#actualAddress").text());
  $("#path").show();
  updatePathPin("start",lat,long,"start",map);
}

function setEndSearchPath(lat,long) {
  pathEnd = lat+";"+long;
  $("#path_end").html("To: "+$("#actualAddress").text());
  $("#path").show();
  updatePathPin("end",lat,long,"finish2",map);
}

function doSearchPath(v, fit) {
  if(pathStart=="") {
    alert("Please select from position");
    return;
  }
  if(pathEnd=="") {
    alert("Please select to position");
    return;
  }
  if(pathLayer==null)
    pathLayer=new L.LayerGroup();
  else {
    pathLayer.clearLayers();
    pathPins["position"]=null;
  }
  
  var routeType = $("#path_type").val();
  var date = $("#path_date").datepicker("getDate");
  console.log(date);
  var time = $("#path_time").timepicker("getTime", date);
  console.log(time);
  if(time==null)
    time = new Date();
  var datetime = moment(time).format("YYYY-MM-DD[T]HH:mm:ss");
  console.log("datetime: "+datetime);
  $("#loading").show();
  if(!v) {
    $.ajax({
          url: ctx+"/ajax/json/shortestpath.jsp",
          type: "GET",
          data: { "source": pathStart, "destination": pathEnd, "routeType": routeType, "startDatetime": datetime},
          async: true,
          dataType: 'json',
          success: function (response) {
            pathResults = response;
            if(response.journey && response.journey.routes && response.journey.routes.length>0) {
              var html = "Found "+response.journey.routes.length+" paths (in "+response.elapsed_ms/1000+"s)";
              html+="<div id='pathResultAccordion'>"
              for(var r=0; r<response.journey.routes.length; r++) {
                var route = response.journey.routes[r];
                pathDefaults.color=pathColors[r%pathColors.length];
                if(r==0) {
                  showPath(0);
                  if(pathPins["start"]==null)
                    updatePathPin("start",response.journey.source_node.lat,response.journey.source_node.lon,"start",map);
                  if(pathPins["end"]==null)
                    updatePathPin("end",response.journey.destination_node.lat,response.journey.destination_node.lon,"finish2",map);
                }
                html +="<h3 route='"+r+"'><span style='color:"+pathDefaults.color+";font-weight:bold'>Length: "+
                        Math.floor(response.journey.routes[r].distance*1000)+"m arrival time:"+
                        response.journey.routes[r].eta+" ("+response.journey.routes[r].time+")</span></h3><div><ol>";
                var street = "";
                var dist = 0;
                var h = "";
                var startTime;
                var showPin;
                var img = "";
                for(var j=0;j<route.arc.length; j++) {
                  if(route.arc[j].desc!=street) {
                    if(street!="")
                      html+="<li>"+img+"<a class='pathStreet' style='cursor:pointer;' onclick='"+showPin+"'><b>"+street+"</b> "+Math.floor(dist*1000)+"m ("+startTime+")</a><ol style='display:none'>"+h+"</ol></li>";
                    street=route.arc[j].desc;
                    h = "";
                    dist = 0;
                    startTime = route.arc[j].start_datetime
                    showPin = "showPin(this,"+r+","+j+")";
                  }
                  if(route.arc[j].transport=="public transport") {
                    var icon = "TransferServiceAndRenting_BusStop";
                    if(route.arc[j].stops.features.length>0) {
                      icon = route.arc[j].stops.features[0].properties.serviceType;
                      icon += "_"+route.arc[j].transport_provider_name.toLowerCase().replace(/\./g, "").replace(/&/g, "").replace(/ù/g, "u").replace(/à/g, "a").replace(/ /g, "");
                    }
                    img="<img src='"+ctx+"/img/mapicons/"+icon+".png' height='19' width='16' align='top'>&nbsp;";
                    h+="<li><a style='cursor:pointer;' onclick='showPin(this,"+r+","+j+")'>"+route.arc[j].transport+" "+Math.floor(route.arc[j].distance*1000)+"m ("+route.arc[j].transport_provider_name+")</a><ol>";
                    h+="<li>"+img+"<a style='cursor:pointer;' onclick='showPinLatLon(this,"+r+","+route.arc[j].source_node.lat+","+route.arc[j].source_node.lon+")'>"+route.arc[j].source_node.stop_name+" ("+route.arc[j].start_datetime+")</a></li>";
                    for(var stop = 1; stop<route.arc[j].stops.features.length-1; stop++) {
                      var s = route.arc[j].stops.features[stop];
                      h+="<li>"+img+"<a style='cursor:pointer;' onclick='showPinLatLon(this,"+r+","+s.geometry.coordinates[1]+","+s.geometry.coordinates[0]+")'>"+s.properties.name+"</a></li>";
                    }
                    h+="<li>"+img+"<a style='cursor:pointer;' onclick='showPinLatLon(this,"+r+","+route.arc[j].destination_node.lat+","+route.arc[j].destination_node.lon+")'>"+route.arc[j].destination_node.stop_name+" ("+route.arc[j].end_datetime+")</a></li>";
                    h+="</ol></li>"
                  } else {
                    h+="<li><a style='cursor:pointer;' onclick='showPin(this,"+r+","+j+")'>"+route.arc[j].transport+" "+Math.floor(route.arc[j].distance*1000)+"m ("+route.arc[j].end_datetime+")</a></li>";
                    img="";
                  }
                  dist += route.arc[j].distance;
                }
                html+="<li>"+img+"<a class='pathStreet' style='cursor:pointer;' onclick='"+showPin+"'><b>"+street+"</b> "+Math.floor(dist*1000)+"m ("+startTime+")</a><ol style='display:none'>"+h+"</ol></div>";
              }
              html+="</div>";
              $("#pathresult").html(html);
              $("#pathResultAccordion").accordion({ heightStyle: "content", collapsible: true, active: false,
                  activate: function( event, ui ) {
                    var route=ui.newHeader.attr("route");
                    if(route!=undefined)
                      showPath(route);
                  }
                });
              if(fit)
                map.fitBounds(pathLayer.getLayers()[0].getBounds());
            } else {
              $("#pathresult").html("No paths found.");
            }
            $("#loading").hide();
          },
          error: function () {
            $("#loading").hide();
            $("#pathresult").html("Sorry cannot find a path.");
            pathResults = null;
            //alert("error");
          }
      });
  } else {
    $.ajax({
          url: "/Path/api/v1/",
          type: "GET",
          data: { "source": pathStart, "sink": pathEnd, "geometry": true},
          async: true,
          dataType: 'json',
          success: function (response) {
            pathResults = response;
            var wkt = new Wkt.Wkt();
            var html = "Found "+response.length+" paths:<ol>";
            for(var i=0; i<response.length; i++) {
              pathDefaults.color=pathColors[i%pathColors.length];
              if(i==0) {
                wkt.read(response[i].wkt);
                var obj = wkt.toObject(pathDefaults);
                obj.addTo(pathLayer);
                pathLayer.addTo(map);
                pathShown=0;
              }
              html +="<li><span style='color:"+pathDefaults.color+";font-weight:bold'>Length: "+response[i].lengthPath+"m</span><ol>";
              for(var j=0;j<response[i].roadPath.length; j++) {
                var coord=response[i].roadPath[j].startCoord;
                html+="<li><a style='cursor:pointer;' onclick='showPin(this, "+i+","+j+")'>"+response[i].roadPath[j].nameStreet+" ("+response[i].roadPath[j].length_edge.trim()+"m)</a></li>";
              }
              html+="</ol>";
            }
            html+="</ol>";
            $("#pathresult").html(response.length==0 ? "Sorry no paths found." : html);
            //map.fitBounds(clickLayer.getLayers()[0].getBounds());
            $("#loading").hide();
          },
          error: function () {
            $("#loading").hide();
            $("#pathresult").html("Sorry cannot find a path.");
            pathResults = null;
            //alert("error");
          }
      });
    }
}

function makePathPin(lat, long, icon, layer) {
  var icon = L.icon({
      iconUrl: ctx + '/img/mapicons/'+icon+'.png',
      iconRetinaUrl: ctx + '/img/mapicons/'+icon+'.png',
      iconSize: [26, 29],
      iconAnchor: [13, 29],
      popupAnchor: [0, -27],
  });
  var latlng = new L.LatLng(lat, long);
  var pin = L.marker(latlng, {icon: icon, riseOnHover: true});
  pin.addTo(layer);
  if(layer!=map)
    layer.addTo(map);
  return pin;
}

function updatePathPin(pin,lat,long,icon,layer) {
  var latlng = new L.LatLng(lat, long);
  if(pathPins[pin]==null)
    pathPins[pin]=makePathPin(lat,long,icon,layer);
  else {
    pathPins[pin].setLatLng(latlng);
  }
  map.panTo(latlng);
}

function showPin(element, p, pp, icon) {
  if($(element).hasClass('pathStreet'))
    $(element).siblings("ol").toggle();
  if(icon==undefined)
    icon="generic";
  if(p!=pathShown) {
    showPath(p);
  }
  var lat = pathResults.journey.routes[p].arc[pp].source_node.lat; //pathResults[p].roadPath[pp].startCoord.lat;
  var long = pathResults.journey.routes[p].arc[pp].source_node.lon; //pathResults[p].roadPath[pp].startCoord.long;
  updatePathPin("position",lat,long,icon,pathLayer);
}

function showPinLatLon(element, p, lat, lon, icon) {
  if($(element).hasClass('pathStreet'))
    $(element).siblings("ol").toggle();
  if(icon==undefined)
    icon="generic";
  if(p!=pathShown) {
    showPath(p);
  }
  updatePathPin("position",lat,lon,icon,pathLayer);
}

function showPath(p) {
  var wkt = new Wkt.Wkt();
  pathLayer.clearLayers();
  pathPins["position"] = null;
  pathDefaults.color = pathColors[p % pathColors.length];
  var route = pathResults.journey.routes[p];
  wkt.read(route.wkt);
  var obj = wkt.toObject(pathDefaults);
  obj.addTo(pathLayer);
  var n = 0;
  for (var i = 0; i < route.arc.length; i++) {
    if (route.arc[i].transport == "public transport") {
      try {
        wkt.read(route.arc[i].wkt);
        var wktobj = wkt.toObject({
          color: tplPathsColors[n % tplPathsColors.length],
          weight: 4,
          opacity: 0.6});
        wktobj.addTo(pathLayer);
        wktobj.arc = route.arc[i];
        wktobj.color = tplPathsColors[n % tplPathsColors.length];
        wktobj.on('mouseover', function (e) {
          var layer = e.target;
          var mouseX = e.originalEvent.pageX;
          var mouseY = e.originalEvent.pageY;
          layer.bringToFront();
          layer.setStyle({
            color: 'yellow',
            opacity: 0.9,
            weight: 5
          });
          var popup = $("<div></div>", {
            id: "popup-route",
            css: {
              position: "absolute",
              top: (mouseY + 5) + "px",
              left: (mouseX + 15) + "px",
              zIndex: 1002,
              backgroundColor: "white",
              padding: "1px",
              border: "1px solid #ccc"
            }
          });
          var head = $("<div></div>", {
            html: layer.arc.desc,
            css: {fontSize: "13px", marginBottom: "1px"}
          }).appendTo(popup);
          popup.appendTo("#map");
        });
        wktobj.on('mouseout', function (e) {
          var layer = e.target;
          $("#popup-route").remove();
          layer.setStyle({
            color: layer.color,
            opacity: 0.6,
            weight: 4
          });
        });
      } 
      catch (e) {
        console.log(e);
      }
      /*for (var j = 0; j < route.arc[i].stops.length; j++) {
        var stop = route.arc[i].stops[j];
        var feature = {
          properties: {name: stop.stop_name + " (" + stop.time + ")",
            serviceType: stop.serviceType,
            agency: route.arc[i].transport_provider_name,
            serviceUri: stop.stop_uri
          },
          geometry: {coordinates: [stop.lat, stop.lon]},
          id: i * 100 + j};
        var marker = showmarker(feature, new L.LatLng(stop.lat, stop.lon));
        marker.bindPopup("<div id='" + feature.id + "-" + feature.properties.serviceType + "'></div>");
        var popup = marker.getPopup();
        popup._source.feature = feature;

        marker.addTo(pathLayer);
      }*/
      var stops = L.geoJson(route.arc[i].stops, {
          pointToLayer: function (feature, latlng) {
              if(!feature.properties.name_)
                feature.properties.name_=feature.properties.name;
              feature.properties.name = feature.properties.name_+" ("+feature.properties.arrivalTime+")";
              marker = showmarker(feature, latlng);
              return marker;
          },
          onEachFeature: function (feature, layer) {
              var contenutoPopup = "";
              feature.id = i*100 + feature.id
              var divId = feature.id + "-" + feature.properties.serviceType;
              contenutoPopup = "<div id=\"" + divId + "\" ></div>";
              layer.bindPopup(contenutoPopup);
          }
        });
      stops.addTo(pathLayer);
      n++;
    }
  }
  pathLayer.addTo(map);
  pathShown = p;
}

function resetPath() {
  $("#path").hide();
  pathStart = "";
  $("#pathresult").html("");
  $("#path_start").html("From: ?");
  pathEnd = "";
  $("#path_end").html("To: ?");
  pathPins = {"start":null, "end":null, "position": null};
  pathLayer = null;
  pathShown = null;
}