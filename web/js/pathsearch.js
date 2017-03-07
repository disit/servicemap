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
var pathColors = ["#FF0000","#0000FF","#00FF00","#FFFF00","#00FFFF"];

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
  
  $("#loading").show();
  if(!v) {
    $.ajax({
          url: ctx+"/ajax/json/shortestpath.jsp",
          type: "GET",
          data: { "source": pathStart, "destination": pathEnd},
          async: true,
          dataType: 'json',
          success: function (response) {
            pathResults = response;
            if(response.journey.routes && response.journey.routes.length>0) {
              var wkt = new Wkt.Wkt();
              var html = "Found "+response.journey.routes.length+" paths (in "+response.elapsed_ms/1000+"s)";
              for(var r=0; r<response.journey.routes.length; r++) {
                var route = response.journey.routes[r];
                pathDefaults.color=pathColors[r%pathColors.length];
                if(r==0) {
                  wkt.read(route.wkt);
                  var obj = wkt.toObject(pathDefaults);
                  obj.addTo(pathLayer);
                  pathLayer.addTo(map);
                  pathShown=0;
                  if(pathPins["start"]==null)
                    updatePathPin("start",response.journey.source_node.lat,response.journey.source_node.lon,"start",map);
                  if(pathPins["end"]==null)
                    updatePathPin("end",response.journey.destination_node.lat,response.journey.destination_node.lon,"finish2",map);
                }
                html +="<li><span style='color:"+pathDefaults.color+";font-weight:bold'>Length: "+Math.floor(response.journey.routes[r].distance*1000)+"m</span><ol>";
                for(var j=0;j<route.arc.length; j++) {
                  html+="<li><a style='cursor:pointer;' onclick='showPin("+r+","+j+")'>"+route.arc[j].transport+" "+Math.floor(route.arc[j].distance*1000)+"m ("+route.arc[j].end_datetime+")</a></li>";
                }
                html+="</ol>";
                $("#pathresult").html(html);
              }
            }
            if(fit)
              map.fitBounds(pathLayer.getLayers()[0].getBounds());
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
                html+="<li><a style='cursor:pointer;' onclick='showPin("+i+","+j+")'>"+response[i].roadPath[j].nameStreet+" ("+response[i].roadPath[j].length_edge.trim()+"m)</a></li>";
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

function showPin(p, pp, icon) {
  if(icon==undefined)
    icon="generic";
  if(p!=pathShown) {
    var wkt = new Wkt.Wkt();
    pathLayer.clearLayers();
    pathPins["position"]=null;
    pathDefaults.color=pathColors[p%pathColors.length];
    wkt.read(pathResults[p].wkt);
    var obj = wkt.toObject(pathDefaults);
    obj.addTo(pathLayer);
    pathLayer.addTo(map);
    pathShown=p;
  }
  var lat = pathResults.journey.routes[p].arc[pp].source_node.lat; //pathResults[p].roadPath[pp].startCoord.lat;
  var long = pathResults.journey.routes[p].arc[pp].source_node.lon; //pathResults[p].roadPath[pp].startCoord.long;
  updatePathPin("position",lat,long,icon,pathLayer);
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