/* ServiceMap.
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */


// FUNZIONE CARICAMENTO CONTENUTO MENU LINEE ATAF
function getBusLines(whoCalls) {
    //if(mode!="query" && mode!="embed"){
    $.ajax({
        url: ctx + "/ajax/get-lines.jsp",
        type: "GET",
        async: true,
        //dataType: 'json',
        success: function (msg) {
            $("#elencolinee").html(msg);
        }
    });
}

// MOSTRA ELENCO FERMATE DI UNA LINEA NELLA SELECT
function mostraElencoFermate(selectOption, stop) {
    nascondiRisultati();
    if ($("#elencopercorsi").val() != null) {
        //if (selectOption.options.selectedIndex != 0){	
        $('#elencoprovince')[0].options.selectedIndex = 0;
        $('#elencocomuni').html('<option value=""> - Seleziona un Comune - </option>');
        $('#loading').show();

        $.ajax({
            url: ctx + "/ajax/bus-stops-list.jsp",
            type: "GET",
            async: true,
            //dataType: 'json',
            data: {
                //nomeLinea: selectOption.options[selectOption.options.selectedIndex].value
                codeRoute: selectOption.options[selectOption.options.selectedIndex].value
            },
            success: function (msg) {
                if (mode == "embed")
                    $("#elencofermate").val(stop);
                else
                    $('#elencofermate').html(msg);
                $('#loading').hide();
            }
            //   timeout:5000
        });
    }
}

// MOSTRA ELENCO FERMATE DI UNA LINEA NELLA SELECT
function mostraElencoPercorsi(selectOption, route) {
    nascondiRisultati();
    if ($("#elencolinee").val() != null) {
        //if (selectOption.options.selectedIndex != 0){	
        $('#elencoprovince')[0].options.selectedIndex = 0;
        $('#elencocomuni').html('<option value=""> - Seleziona un Comune - </option>');
        $('#loading').show();
        $.ajax({
            url: ctx + "/ajax/bus-route-list.jsp",
            type: "GET",
            async: true,
            //dataType: 'json',
            data: {
                nomeLinea: selectOption.options[selectOption.options.selectedIndex].value
            },
            success: function (msg) {
                if (mode == "embed")
                    $("#elencopercorsi").val(route);
                else
                    $('#elencopercorsi').html(msg);
                $('#loading').hide();
            }
            //   timeout:5000
        });
    }
}

// MOSTRA ELENCO FERMATE DI UNA LINEA SULLA MAPPA
function mostraFermate(selectOption) {
    //$('#info-aggiuntive .content').html(''); //forse da disattivare per mostrare le previsioni del tempo
    nascondiRisultati();
    if (selectOption.options.selectedIndex != 0) {

        GPSControl._isActive = false;
        //svuotaLayers();
        $('#loading').show();
        if (selectOption.options[selectOption.options.selectedIndex].value == 'all') {
            $('#raggioricerca').prop('disabled', true);
            $('#nResultsServizi').prop('disabled', true);
            $('#nResultsSensori').prop('disabled', true);
            $('#nResultsBus').prop('disabled', true);
            if (pins.lenght > 0)
                pins = "";
            /* TEMPORANEAMENTE DISABILITATO
             if ($('#elencolinee')[0].options[$('#elencolinee')[0].options.selectedIndex].value == 'LINE17'){
             // SE E' LA LINEA 17 FACCIO VEDERE LE ROUTESECTION
             
             $.ajax({
             url : "${pageContext.request.contextPath}/ajax/json/get-bus-route.jsp",
             type : "GET",
             async: true,
             dataType: 'json',
             data : {
             numeroRoute: '438394'
             },
             success : function(msg) {
             var busRouteLayer = L.geoJson(msg).addTo(map);
             
             }
             });	  
             
             }
             */
            //var numBusstop = 0;
            var position = ($("#elencopercorsi option:selected").text()).indexOf("-");
            if ($("#elencolinee").val() == "all") {
                var pos_space = ($("#elencopercorsi option:selected").text()).indexOf(" ");
                var linea = ($("#elencopercorsi option:selected").text()).substring(0, pos_space);
            } else {
                var linea = $("#elencolinee").val();
            }

            var direction = ($("#elencopercorsi option:selected").text()).substring(position + 2);
            var codRoute = $("#elencopercorsi").val();
            var nomeLinea = ($("#elencopercorsi option:selected").text()).substring(0, position - 1);

            showLinea(linea, codRoute, direction, nomeLinea);

            //query = saveQueryBusStopLine(nomeLinea);

            //DA SOSTTITURE CON LA CHIAMATA A SHOWLINEA //
            /*
             $.ajax({
             url: ctx + "/ajax/json/get-bus-stops-of-line.jsp",
             type: "GET",
             async: true,
             dataType: 'json',
             data: {
             nomeLinea: $('#elencolinee')[0].options[$('#elencolinee')[0].options.selectedIndex].value,
             codRoute: "vuoto"
             },
             success: function (msg) {
             
             //faccio partire il firing delle previsioni di firenze
             
             $.ajax({
             url: ctx + "/ajax/get-weather.jsp",
             type: "GET",
             async: true,
             //dataType: 'json',
             data: {
             nomeComune: "FIRENZE"
             },
             success: function (msg) {
             $('#info-aggiuntive .content').html(msg);
             
             }
             });
             
             selezione = 'Linea Bus: ' + $('#elencolinee')[0].options[$('#elencolinee')[0].options.selectedIndex].value;
             $('#selezione').html(selezione);
             coordinateSelezione = "";
             busStopsLayer = L.geoJson(msg, {
             pointToLayer: function (feature, latlng) {
             marker = showmarker(feature, latlng);
             return marker;
             },
             onEachFeature: function (feature, layer) {
             var popupContent = "";
             var divId = feature.id + "-" + feature.properties.tipo;
             popupContent = popupContent + "<div id=\"" + divId + "\" ></div>";
             layer.bindPopup(popupContent);
             numBusstop++;
             layer.on({
             //mouseover: aggiornaAVM
             });
             
             }
             }).addTo(map);
             var markerJson = JSON.stringify(msg.features);
             pins = markerJson;
             var confiniMappa = busStopsLayer.getBounds();
             map.fitBounds(confiniMappa, {padding: [50, 50]});
             $('#loading').hide();
             var numeroBus = (numBusstop + " -  Linea Bus: " + nomeLinea);
             risultatiRicerca(0, numeroBus, 0, 1);
             
             },
             error: function (request, status, error) {
             $('#loading').hide();
             }
             
             });*/

        }
        else {
            if (pins.length > 0)
                pins = "";
            $('#raggioricerca').prop('disabled', false);
            $('#nResultsServizi').prop('disabled', false);
            $('#nResultsSensori').prop('disabled', false);
            $('#nResultsBus').prop('disabled', false);
            $.ajax({
                url: ctx + "/ajax/json/get-bus-stop.jsp",
                type: "GET",
                async: true,
                dataType: 'json',
                data: {
                    nomeFermata: selectOption.options[selectOption.options.selectedIndex].value
                },
                success: function (msg) {

                    //$('#elencofermate').html(msg);

                    //faccio partire il firing delle previsioni del tempo di Firenze
                    $.ajax({
                        url: "ajax/get-weather.jsp",
                        type: "GET",
                        async: true,
                        //dataType: 'json',
                        data: {
                            nomeComune: "FIRENZE"
                        },
                        success: function (msg) {
                            $('#info-aggiuntive .content').html(msg);
                        }
                    });
                    //    var i=0;
                    selezione = 'Bus Stop: ' + selectOption.options[selectOption.options.selectedIndex].value;
                    $('#selezione').html(selezione);
                    var longBusStop = msg.features[0].geometry.coordinates[0];
                    var latBusStop = msg.features[0].geometry.coordinates[1];

                    coordinateSelezione = latBusStop + ";" + longBusStop;
                    busStopsLayer = L.geoJson(msg, {
                        pointToLayer: function (feature, latlng) {
                            marker = showmarker(feature, latlng);
                            return marker;
                        },
                        onEachFeature: function (feature, layer) {
                            var popupContent = "";
                            var divId = feature.id + "-" + feature.properties.tipo;
                            /*var divLinee = divId + "-linee";
                             var popupContent = "<h3>FERMATA : " + feature.properties.popupContent + "</h3>";
                             popupContent += "<a href='" + logEndPoint + feature.properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
                             popupContent += "<div id=\"" + divLinee + "\" ></div>";
                             popupContent += "<div id=\"" + divId + "\" ></div>";
                             var divSavePin = "savePin-" + feature.id;
                             var name = feature.properties.popupContent;
                             name = escape(name);
                             popupContent = popupContent + "<div id=\"" + divSavePin + "\" class=\"savePin\" onclick=save_handler('" + feature.properties.tipo + "','" + feature.properties.serviceUri + "','" + name + "')></div>";*/
                            popupContent = popupContent + "<div id=\"" + divId + "\" ></div>";
                            layer.bindPopup(popupContent);
                            layer.on({
                                //mouseover: aggiornaAVM
                            });

                        }

                    }).addTo(map);
                    var markerJson = JSON.stringify(msg.features);
                    pins = markerJson;
                    map.setView(new L.LatLng(latBusStop, longBusStop), 16);
                    $('#loading').hide();

                }
                // timeout:5000
            });
        }
    }
}

//FUNZIONE VISUALIZAZIONE LINEA DA POPUP
function showLinea(numLinea, codRoute, direction, nomeLinea, typeSer) {
    //$('#loading').show();
    //
    //$(".leaflet-marker-icon.leaflet-zoom-animated.leaflet-clickable").not(".selected").remove();
    //$(".route_ATAF.leaflet-clickable").hide();

    $('#elencolinee')[0].options.selectedIndex = 0;
    $('#elencopercorsi').html('<option value=""> - Select a Route - </option>');
    $('#elencofermate').html('<option value=""> - Select a Bus Stop - </option>');
    var line = createLinestring(numLinea, codRoute);
    //PROVA DISEGNO PERCORSO
    line = line.substr(13, line.length - 15);
    var vet_point = line.split(", ");
    var Point_pol = new Array();
    var className = "route_ATAF_" + codRoute;
    Point_pol = [];
    for (var j = 0; j < vet_point.length; j++) {
        var point = vet_point[j].split(" ");
        Point_pol[j] = [parseFloat(point[1]), parseFloat(point[0])];
    }

    var drawnLine = L.polyline(Point_pol, {color: "#085EE8", weight: 3, className: className, opacity: 0.9}).addTo(servicesLayer);
    servicesLayer.addTo(map);
    drawnLine.on('mouseover', function (e) {
        var layer = e.target;
        var mouseX = e.originalEvent.pageX;
        var mouseY = e.originalEvent.pageY;
        layer.bringToFront();
        layer.setStyle({
            color: 'yellow',
            opacity: 0.9,
            weight: 4,
            title: nomeLinea
        });

        var popup = $("<div></div>", {
            id: "popup-" + className,
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
        // Insert a headline into that popup
        var head = $("<div></div>", {
            text: "Linea: " + nomeLinea + " - " + direction,
            css: {fontSize: "13px", marginBottom: "1px"}
        }).appendTo(popup);
        popup.appendTo("#map");
    });
    drawnLine.on('mouseout', function (e) {
        var layer = e.target;
        $("#popup-" + className).remove();
        layer.setStyle({
            color: '#085EE8',
            opacity: 0.9,
            weight: 3,
            title: nomeLinea
        });
    });
    if(typeSer != "transverse"){
        map.fitBounds(drawnLine.getBounds());
    }    
    var numBusstop = 0;

    $.ajax({
        url: ctx + "/ajax/json/get-bus-stops-of-line.jsp",
        type: "GET",
        async: true,
        dataType: 'json',
        data: {
            nomeLinea: numLinea,
            codRoute: codRoute
        },
        success: function (msg) {
            if(typeSer != "transverse"){
            $.ajax({
                url: ctx + "/ajax/get-weather.jsp",
                type: "GET",
                async: true,
                //dataType: 'json',
                data: {
                    nomeComune: "FIRENZE"
                },
                success: function (msg) {
                    $('#info-aggiuntive .content').html(msg);

                }
            });
            
                selezione = 'Bus Line: ' + nomeLinea + " - " + direction;
                $('#selezione').html(selezione);
                coordinateSelezione = "";
            }
            
            busStopsLayer = L.geoJson(msg, {
                pointToLayer: function (feature, latlng) {
                    marker = showmarker(feature, latlng);
                    return marker;
                },
                onEachFeature: function (feature, layer) {
                    var popupContent = "";
                    var divId = feature.id + "-" + feature.properties.tipo;
                    popupContent = popupContent + "<div id=\"" + divId + "\" ></div>";
                    layer.bindPopup(popupContent);
                    numBusstop++;
                    layer.on({
                    });

                }

            }).addTo(map);

            var markerJson = JSON.stringify(msg.features);
            pins = markerJson;
            var confiniMappa = busStopsLayer.getBounds();
            if(typeSer != "transverse"){
                map.fitBounds(confiniMappa, {padding: [50, 50]});
                $('#loading').hide();
                var numeroBus = (numBusstop + " -  Bus Line: " + nomeLinea);
                risultatiRicerca(0, numeroBus, 0, 1, direction);
            }

        },
        error: function (request, status, error) {
            $('#loading').hide();
        }

    });

}

function showRoute(numLinea, divRoute) {
    $.ajax({
        url: ctx + "/ajax/json/get-bus-route.jsp",
        type: "GET",
        async: true,
        //dataType: 'json',
        data: {
            numLinea: numLinea,
        },
        success: function (msg) {
            $('#' + divRoute).html(msg);
            popup_fixpos(divRoute);
            //$('#info-aggiuntive .content').html(msg);
        }
    });
}

//FUNZIONE VISUALIZAZIONE PATH
function Estract_features(Str_location, id, serType) {
    //Determinazione tipologia area
    var n = Str_location.search(/POLYGON/i);
    if ($('#' + id).attr('visible') == 'false' || id == null) {
        $('#' + id).attr('visible', 'true');
        if($('#lang').attr('value') == 'ENG'){
            $('#' + id+' span').text("Remove from map");
        }else{
            $('#' + id+' span').text("Rimuovi dalla mappa");
        }
        if (n != -1) {
            //var descr_area="Area:  ";
            Str_location = Str_location.substr(11, Str_location.length - 14);
            var Vet_str = Str_location.split(")), (("); //verifichiamo se sono singole aree o aree separate
            var Point_pol = new Array();
            for (var i = 0; i < Vet_str.length; i++) {
                //vengono visualizzate le singole aree
                var vet_point = Vet_str[i].split(", ");
                Point_pol = [];
                for (var j = 0; j < vet_point.length; j++) {
                    var point = vet_point[j].split(" ");
                    Point_pol[j] = [parseFloat(point[1]), parseFloat(point[0])];
                }
                if (JSON.stringify(serType).indexOf('Controlled_parking_zone') != -1)
                    var polygon3 = L.polygon(Point_pol, {color: '#FF00FF', weight: 2, className: id, opacity: 0.6, fillOpacity: 0.2}).addTo(servicesLayer);
                else {
                    var polygon3 = L.polygon(Point_pol, {color: 'green', weight: 3, className: id, opacity: 0.9, fillOpacity: 0.7}).addTo(servicesLayer);
                }
                servicesLayer.addTo(map);
                map.fitBounds(polygon3.getBounds());
                /*if(desc!=""){
                 polygon3.bindPopup(descr_area +   desc);
                 }*/
            }
        } else {
            n = Str_location.search(/LINESTRING/i);
            if (n != -1) {
                Str_location = Str_location.substr(13, Str_location.length - 15);
                var vet_point = Str_location.split(", ");
                var numberMarker;
                var Point_pol = new Array();
                Point_pol = [];
                for (var j = 0; j < vet_point.length; j++) {
                    var point = vet_point[j].split(" ");
                    Point_pol[j] = [parseFloat(point[1]), parseFloat(point[0])];
                    if (((JSON.stringify(serType).indexOf('Tourist_trail') != -1) && (j != 0) && (j != (vet_point.length - 1)))) {
                        numberMarker = L.marker([parseFloat(point[1]), parseFloat(point[0])], {
                            icon: new L.DivIcon({
                                className: "number-icon",
                                iconSize: [20, 20],
                                html: j
                            })
                        });
                        numberMarker.addTo(map);
                        $(numberMarker._icon).addClass(id);
                    }
                }
                var Line = L.polyline(Point_pol, {color: "#BD021E", weight: 5, className: id, opacity: 0.9}).addTo(servicesLayer);
                servicesLayer.addTo(map);
                map.fitBounds(Line.getBounds());
                var last_point = vet_point[(vet_point.length - 1)].split(" ");


                if (JSON.stringify(serType).indexOf('Tramline') != -1) {

                    $.ajax({
                        url: ctx + "/ajax/json/get-services.jsp",
                        type: "GET",
                        async: true,
                        dataType: 'json',
                        data: {
                            categorie: 'tram_stops',
                            raggioServizi: 5,
                            centroRicerca: '43.76247809670398;11.206483840942383',
                            numeroRisultatiServizi: 30
                        },
                        success: function (msg) {
                            if (mode == "JSON") {
                                $("#body").html(JSON.stringify(msg));
                            }
                            else {
                                if (msg.features.length > 0) {
                                    busStopsLayer = L.geoJson(msg, {
                                        pointToLayer: function (feature, latlng) {
                                            var marker = showmarker(feature, latlng);
                                            return marker;

                                        },
                                        onEachFeature: function (feature, layer) {
                                            var contenutoPopup = "";
                                            var divId = feature.id + "-" + feature.properties.tipo;
                                            contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                            layer.bindPopup(contenutoPopup);
                                        }
                                    });
                                    busStopsLayer.addTo(map);
                                }
                            }
                        }
                    });
                }


                //Disegno il marker finish

                var finishIcon = L.icon({
                    iconUrl: ctx + '/img/mapicons/finish.png',
                    iconRetinaUrl: ctx + '/img/mapicons/finish.png',
                    iconSize: [26, 29],
                    iconAnchor: [13, 29]});
                var finishMarker = L.marker([parseFloat(last_point[1]), parseFloat(last_point[0])]);
                finishMarker.setIcon(finishIcon);
                finishMarker.addTo(map);
                $(finishMarker._icon).addClass("finish" + id);

                /*if(desc!=""){
                 Line.bindPopup(descr_linea + ": "+ desc);
                 }*/

            }
        }
    } else {

        $('#' + id).attr('visible', 'false');
        if($('#lang').attr('value') == 'ENG'){
            $('#' + id+' span').text("Show on map");
        }else{
            $('#' + id+' span').text("Visualizza sulla mappa");
        }
        //rimuove il percorso o l'area dalla mappa
        $("." + id + ".leaflet-clickable").remove();
        // rimuove le fermate del tram se presenti
        if (JSON.stringify(serType).indexOf('tramline') != -1) {
            busStopsLayer.clearLayers();
        }
        // rimuove il finishmarker dalla mappa
        $('.leaflet-marker-icon.leaflet-zoom-animated.leaflet-clickable.finish' + id).remove();
    }

}

function createLinestring(numLinea, codRoute) {
    /*var request = new XMLHttpRequest();
     request.open("GET", ctx + "/ajax/json/get-xml-route.jsp?linea=" + numLinea + "&route=" + codRoute, false);
     request.send();
     var polyline = "LINESTRING ((";
     var xml = request.responseXML;
     var points = xml.getElementsByTagName("point");
     for (var i = 0; i < points.length; i++) {
     var lon = points[i].getAttribute("lon");
     var lat = points[i].getAttribute("lat");
     if (i != points.length - 1) {
     polyline = polyline + lon + " " + lat + ", ";
     } else {
     polyline = polyline + lon + " " + lat + "))";
     }
     }
     return polyline;*/
    var linestring;
    $.ajax({
        data: {
            codRoute: codRoute
        },
        url: ctx + "/ajax/json/get-xml-route.jsp",
        type: "GET",
        async: false,
        dataType: 'json',
        success: function (data) {
            if (data.polyline.length > 0) {
                linestring = data.polyline;

            }

        }
    });
    return linestring;
}

//FUNZIONE CHE CARICA NEL POPUP LE INFORMAZIONI DELLA SCHEDA DI UN SERVIZIO
function loadServiceInfo(uri, div, id, coord) {
    if (uri.indexOf("Event") != -1) {
    } else {
        if (uri.indexOf("busCode") == -1) {
            $.ajax({
                data: {
                    serviceUri: uri
                },
                url: ctx + "/api/service.jsp",
                type: "GET",
                async: true,
                dataType: 'json',
                success: function (data) {
                    if (data.features.length > 0) {
                        var tipo = data.features[0].properties.tipo;
                        //selezione = 'Servizio: ' + data.features[0].properties.nome;
                        if (tipo == 'fermata'){
                            selezione = 'Bus Stop: ' + data.features[0].properties.nome;
                        }else{
                            selezione = 'Service: ' + data.features[0].properties.nome;
                        }
                        $('#selezione').html(selezione);

                        var divInfo = div + "-info";
                        var contenutoPopup = createContenutoPopup(data.features[0], div, id);
                        $("#" + div).html(contenutoPopup);
                        if (data.features[0].multimedia != null)
                            $(".leaflet-popup-content-wrapper").css("width", "300px");
                        popup_fixpos(div);
                        var name = data.features[0].properties.nome;

                        if (tipo == 'Parcheggio_auto' || tipo == "Parcheggio_Coperto" || tipo == "Parcheggi_all'aperto" || tipo == "Car_park@en") {
                            mostraParcheggioAJAX(name, divInfo);
                        } else if (tipo == 'sensore') {
                            mostraSensoreAJAX(name, divInfo);
                        } else if (tipo == 'fermata') {
                            var divLinee = div + "-linee";
                            var divRoute = div + "-route";
                            mostraAVMAJAX(name, divInfo);
                            mostraLineeBusAJAX(name, divLinee, divRoute);
                        }
                    } else {
                        contenutoPopup = "No info related to Service" + uri;
                        $("#" + div).html(contenutoPopup);
                        popup_fixpos(div);
                    }
                }
            });
        }
        else {
            var latitude = coord[1];
            var longitude = coord[0];
            var numeroBus = uri.replace("busCode", "");
            selezione = 'Bus Number: ' + numeroBus;
            $('#selezione').html(selezione);
            $.ajax({
                url: ctx + "/ajax/get-address.jsp",
                type: "GET",
                async: true,
                //dataType: 'json',
                data: {
                    lat: latitude,
                    lng: longitude
                },
                success: function (msg) {
                    $('#approximativeAddress').html(msg);
                    ricercaInCorso = false;
                }
            });
        }
    }
}
function createContenutoPopup(feature, div, id) {
    var contenutoPopup = "";
    var divMM = id + "-multimedia";
    var multimediaResource = feature.properties.multimedia;
    var htmlDiv;
    if (multimediaResource != null)
    {
        var format = multimediaResource.substring(multimediaResource.length - 4);
        if (format == ".mp3") {
            htmlDiv = "<div id=\"" + divMM + "\"class=\"multimedia\"><audio controls class=\"audio-controls\"><source src=\"" + multimediaResource + "\" type=\"audio/mpeg\"></audio></div>";
        } else {
            if ((format == ".wav") || (format == ".ogg")) {
                htmlDiv = "<div id=\"" + divMM + "\"class=\"multimedia\"><audio controls class=\"audio-controls\"><source src=\"" + multimediaResource + "\" type=\"audio/" + format + "\"></audio></div>";
            } else if (format == ".pdf") {
                htmlDiv = "<div id=\"" + divMM + "\"class=\"multimedia\"><a target=\"blank\" href=\"" + multimediaResource + "\"><img src=\"" + ctx + "/img/mapicons/pdf-icon.png\" width=\"80\" height=\"80\"></a></div>";
            } else {
                htmlDiv = "<div id=\"" + divMM + "\"class=\"multimedia\"><img src=\"" + multimediaResource + "\" width=\"80\" height=\"80\"></div>";
            }
        }
    }
    var divInfo = div + "-info";
    var divInfoPlus = div + "-infoplus";
    var linkDBpedia = "";
    if (feature.properties.tipo != "fermata") {
        if (feature.properties.tipo != "Road") {
            contenutoPopup = "<div class=\"description\"><h3>" + feature.properties.nome + "</h3></div>";
        }
        contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + feature.properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
        contenutoPopup = contenutoPopup + "<b><span name=\"lbl\" caption=\"tipology\">Tipology</span>:</b> " + feature.properties.tipologia + "<br />";
        if (feature.properties.digitalLocation != "" && feature.properties.digitalLocation)
            contenutoPopup = contenutoPopup + "<span style='border:1px solid #E87530; padding:2px;'><b>Digital Location</b></span><br />";
        if (feature.properties.email != "" && feature.properties.email)
            contenutoPopup = contenutoPopup + "<b>Email:</b><a href=\"mailto:" + feature.properties.email + "?Subject=information request\" target=\"_top\"> " + feature.properties.email + "</a><br />";

        if (feature.properties.website != "" && feature.properties.website)
            if (((JSON.stringify(feature.properties.website)).indexOf("http\://")) != -1) {
                contenutoPopup = contenutoPopup + "<b>Website:</b><a href=\"" + feature.properties.website + "\" target=\"_blank\" title=\"" + feature.properties.nome + " - website\"> " + feature.properties.website + "</a><br />";
            } else {
                contenutoPopup = contenutoPopup + "<b>Website:</b><a href=\"http\://" + feature.properties.website + "\" target=\"_blank\" title=\"" + feature.properties.nome + " - website\"> " + feature.properties.website + "</a><br />";
            }
        if (feature.properties.phone != "" && feature.properties.phone)
            contenutoPopup = contenutoPopup + "<b><span name=\"lbl\" caption=\"phone\">Phone</span>:</b> " + feature.properties.phone + "<br />";
        if (feature.properties.fax != "" && feature.properties.fax)
            contenutoPopup = contenutoPopup + "<b>Fax:</b> " + feature.properties.fax + "<br />";
        if (feature.properties.indirizzo != "" && feature.properties.indirizzo)
            contenutoPopup = contenutoPopup + "<b><span name=\"lbl\" caption=\"address\">Address:</span></b> " + feature.properties.indirizzo;
        if (feature.properties.numero != "" && feature.properties.numero)
            contenutoPopup = contenutoPopup + ", " + feature.properties.numero + "<br />";
        else
            contenutoPopup = contenutoPopup + "<br />";
        if (feature.properties.linkDBpedia != "" && feature.properties.linkDBpedia) {
            for (var i = 0; i < feature.properties.linkDBpedia.length; i++) {
                linkDBpedia = (JSON.stringify(feature.properties.linkDBpedia[i])).replace("http://it.dbpedia.org/resource/", "");
                contenutoPopup = contenutoPopup + "<b>DBpedia:</b> " + "<a href='" + feature.properties.linkDBpedia[i] + "' title='Linked DBpedia' target='_blank'>" + linkDBpedia + "</a><br />";
            }
        }
        if (feature.properties.cap != "" && feature.properties.cap)
            contenutoPopup = contenutoPopup + "<b>Cap:</b> " + feature.properties.cap + "<br />";
        if (feature.properties.city != "" && feature.properties.city)
            contenutoPopup = contenutoPopup + "<b><span name=\"lbl\" caption=\"city\">City</span>:</b> " + feature.properties.city + "<br />";
        if (feature.properties.province != "" && feature.properties.province)
            contenutoPopup = contenutoPopup + "<b>Prov.:</b> " + feature.properties.province + "<br />";
        if (feature.properties.multimedia != "" && feature.properties.multimedia) {
            contenutoPopup = contenutoPopup + "<b><span name=\"lbl\" caption=\"multimedia\">Multimedia Content</span>:</b></br>" + htmlDiv;
        }
        if (feature.properties.description != "" && feature.properties.description) {
            if (include(feature.properties.description, "@it"))
                feature.properties.description = feature.properties.description.replace("@it", "");
            contenutoPopup = contenutoPopup + "<div class=\"description\"><b><span name=\"lbl\" caption=\"description\">Description:</b> " + feature.properties.description + "</div><br />";
        }
        if (feature.properties.note != "" && feature.properties.note)
            contenutoPopup = contenutoPopup + "<b><div class=\"description\">Note:</b> " + feature.properties.note + "</div><br />";
        if (feature.properties.cordList != "" && feature.properties.cordList) {
            if ($("." + divInfoPlus + ".leaflet-clickable").html() == null) {
                contenutoPopup = contenutoPopup + "<div class='pulsante' id=\"" + divInfoPlus + "\" visible='false' onclick='Estract_features(\"" + feature.properties.cordList + "\",\"" + divInfoPlus + "\",\"" + feature.properties.serviceType + "\")'><b><span name=\"lbl\" caption=\"show_on_map\">Show on map</span></b></div><br />";
            } else {
                contenutoPopup = contenutoPopup + "<div class='pulsante' id=\"" + divInfoPlus + "\" visible='true' onclick='Estract_features(\"" + feature.properties.cordList + "\",\"" + divInfoPlus + "\",\"" + feature.properties.serviceType + "\")'><b><span name=\"lbl\" caption=\"remove_from_map\">Remove from map</span></b></div><br />";
            }
        }
        contenutoPopup = contenutoPopup + "<div id=\"" + divInfo + "\" ></div>";
        var name = feature.properties.nome;
        nameEscaped = escape(name);
        var divSavePin = "savePin-" + id;
        contenutoPopup = contenutoPopup + "<div id=\"" + divSavePin + "\" class=\"savePin\" onclick=save_handler('" + feature.properties.tipo + "','" + feature.properties.serviceUri + "','" + nameEscaped + "')></div>";
        //layer.addTo(map).bindPopup(contenutoPopup).openPopup();
        // CREAZIONE DIV INFO LINKED SERVICE
        if (feature.properties.linkserUri != "" && feature.properties.linkserUri && (JSON.stringify(id).indexOf('LS') == -1)) {
            var divLS = div + "-LinkedService";
            var idLS = id + "-LS";
            contenutoPopup = contenutoPopup + "<div class='link-serv-container'><b>LinkedService:</b><span>" + feature.properties.linkedService + "</span><span class='toggle-linked-service' id='toggle-" + divLS + "' title='Mostra servizio collegato' onclick=openLinkServInfo('" + feature.properties.linkserUri + "','" + divLS + "','" + idLS + "')>+</span><br/><div id=\"" + divLS + "\"></div></div>";
        }
    } else {
        var divLinee = div + "-linee";
        var divRoute = div + "-route";
        var contenutoPopup = "<h3>BUS STOP : " + feature.properties.nome + "</h3>";
        contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + feature.properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
        contenutoPopup = contenutoPopup + "<div id=\"" + divLinee + "\" ></div>";
        contenutoPopup = contenutoPopup + "<div id=\"" + divRoute + "\" ></div>";
        contenutoPopup = contenutoPopup + "<div id=\"" + divInfo + "\" ></div>";
        var divSavePin = "savePin-" + id;
        var name = feature.properties.nome;
        nameEscaped = escape(name);
        contenutoPopup = contenutoPopup + "<div id=\"" + divSavePin + "\" class=\"savePin\" onclick=save_handler('" + feature.properties.tipo + "','" + feature.properties.serviceUri + "','" + nameEscaped + "')></div>";
    }
    return contenutoPopup;
}
//FUNZIONE PER MOSTRARE/NASCONDERE I MENU
$(".header").click(function () {
    $header = $(this);
    //getting the next element
    $content = $header.next();
    //open up the content needed - toggle the slide- if visible, slide up, if not slidedown.
    $content.slideToggle(200, function () {
        //execute this after slideToggle is done
        //change text of header based on visibility of content div
        $header.text(function () {
            //change text based on condition
            return $content.is(":visible") ? "- Hide Menu" : "+ Show Menu";
        });
    });
});

//FUNZIONE PER MOSTRARE/NASCONDERE LE SUB CATEGORY
$(".toggle-subcategory").click(function () {
    $tsc = $(this);
    //getting the next element
    $content = $tsc.next();
    if (!$content.is(":visible")) {
        $('.subcategory-content').hide();
        $('.toggle-subcategory').html('+');
    }
    //open up the content needed - toggle the slide- if visible, slide up, if not slidedown.
    $content.slideToggle(200, function () {
        //execute this after slideToggle is done
        //change text of header based on visibility of content div
        $tsc.text(function () {
            //change text based on condition
            return $content.is(":visible") ? "-" : "+";
        });
    });
});

//FUNZIONE PER MOSTRARE/NASCONDERE LS SCHEDA DEL LINKED SERVICE
function openLinkServInfo(uri, divID, id) {
    var content = $("#" + divID).html();
    if (($("#" + divID).html()) == '') {
        loadServiceInfo(uri, divID, id);
        $("#toggle-" + divID).html('-');
    } else {
        if (($("#toggle-" + divID).html()) == '-') {
            $("#" + divID).hide();
            $("#toggle-" + divID).html('+');
            popup_fixpos(divID);
        } else {
            $("#" + divID).show();
            $("#toggle-" + divID).html('-');
            popup_fixpos(divID);
        }
    }
}

//CHECKBOX SELECT/DESELECT ALL
$('#macro-select-all').change(function () {
    if ($('#macro-select-all').prop('checked')) {
        $('#categorie .macrocategory').prop('checked', 'checked');
        $("#categorie .macrocategory").trigger("change");

    }
    else {
        $('#categorie .macrocategory').prop('checked', false);
        $("#categorie .macrocategory").trigger("change");

    }
});

//CHECKBOX SELECT/DESELECT ALL TRANSVERSAL
$('#macro-select-all_t').change(function () {
    if ($('#macro-select-all_t').prop('checked')) {
        
        $('#categorie_t .macrocategory').not('#PublicTransportLine').prop('checked', 'checked');
        $("#categorie_t .macrocategory").not('#PublicTransportLine').trigger("change");
        if(!($('#PublicTransportLine').prop('disabled'))){
            $('#PublicTransportLine').prop('checked', 'checked');
            $('#PublicTransportLine').trigger("change");
        }
        //$('#Sensor').prop('checked', 'checked');
        //$('#Bus').prop('checked', 'checked');
    }
    else {
        $('#categorie_t .macrocategory').prop('checked', false);
        $("#categorie_t .macrocategory").trigger("change");
        //$('#Sensor').prop('checked', false);
        //$('#Bus').prop('checked', false);
    }
});

// DESELEZIONE SELECT_ALL REGULAR
$('#categorie .macrocategory').change(function () {
    if (($('#categorie .macrocategory:checked').length) == 0) {
        $('#macro-select-all').prop('checked', false);
    }
});

// DESELEZIONE SELECT_ALL TRANSVERSAL
$('#categorie_t .macrocategory').change(function () {
    if (($('#categorie_t .macrocategory:checked').length) == 0) {
        $('#macro-select-all_t').prop('checked', false);
    }
});




function getUrlParameter(sParam)
{
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++)
    {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam)
        {
            return sParameterName[1];
        }
    }
}

// RESET DI TUTTI I LAYERS SULLA MAPPA
function svuotaLayers() {
    //clickLayer.clearLayers();
    busStopsLayer.clearLayers();
    servicesLayer.clearLayers();
    eventLayer.clearLayers();
    GPSLayer.clearLayers();
    if ($("div.leaflet-marker-pane").html() != null) {
        $("img.leaflet-marker-icon.leaflet-zoom-animated.leaflet-clickable").hide();
        $("div.leaflet-marker-icon.leaflet-zoom-animated.leaflet-clickable").hide();
    }
    if ($("div.leaflet-popup-pane").html() != null) {
        $("div.leaflet-popup.leaflet-zoom-animated").hide();
    }
    $(".circle.leaflet-clickable").remove();
    $("#eventList").html('');
    $("#event").hide();
    $("#listTPL").html('');
    $("#resultTPL").hide();
    $(':radio').prop('checked', false);
}

// CANCELLAZIONE DEL CONTENUTO DEL BOX INFO AGGIUNTIVE
function svuotaInfoAggiuntive() {
    $('#info-aggiuntive .content').html('');
}

function cancellaSelezione() {
    $('#selezione').html('No selection');
    selezione = null;
    coordinateSelezione = null;
}

// FUNZIONE DI RESET GENERALE
function resetTotale() {
    /*  if(markerArray.lenght>0)
     markerArray=[]; */
    if (pins.lenth > 0)
        pins = "";
    svuotaInfoAggiuntive();
    svuotaLayers();
    cancellaSelezione();
    nascondiRisultati();
    $('#macro-select-all').prop('checked', false);
    $('#macro-select-all_t').prop('checked', false);
    $('.macrocategory').prop('checked', false);
    $(".macrocategory").trigger("change");
    //$('#raggioricercaServizi')[0].options.selectedIndex = 0;
    //$('#raggioricercaServizi').prop('disabled', false);
    //$('#raggioricercaSensori')[0].options.selectedIndex = 0;
    //$('#raggioricercaSensori').prop('disabled', false);
    //$('#raggioricercaBus')[0].options.selectedIndex = 0;
    //$('#raggioricercaBus').prop('disabled', false);
    $('#raggioricerca')[0].options.selectedIndex = 0;
    $('#raggioricerca').prop('disabled', false);
    $('#nResultsServizi')[0].options.selectedIndex = 0;
    $('#nResultsServizi').prop('disabled', false);
    $('#raggioricerca_t')[0].options.selectedIndex = 0;
    $('#raggioricerca_t').prop('disabled', false);
    $('#nResultsServizi_t')[0].options.selectedIndex = 0;
    $('#nResultsServizi_t').prop('disabled', false);
    //$('#nResultsSensor')[0].options.selectedIndex = 0;
    //$('#nResultsSensor').prop('disabled', false);
    //$('#nResultsBus')[0].options.selectedIndex = 0;
    //$('#nResultsBus').prop('disabled', false);
    $('#elencolinee')[0].options.selectedIndex = 0;
    $('#elencoprovince')[0].options.selectedIndex = 0;
    $('#elencofermate').html('<option value=""> - Select a Bus Stop - </option>');
    $('#elencocomuni').html('<option value=""> - Select a Municipality - </option>');
    $('#PublicTransportLine').prop('disabled', false);
    $('#info').removeClass("active");
    $("#queryBox").remove();
    $("#serviceTextFilter").val("");
    $("#serviceTextFilter_t").val("");
    $("#approximativeAddress").html('');
    $("path.leaflet-clickable").hide();
    $("img.leaflet-marker-icon.leaflet-zoom-animated.leaflet-clickable").hide();
    $("div.leaflet-marker-icon.leaflet-zoom-animated.leaflet-clickable").hide();
    selezioneAttiva = false;
}

// SELEZIONA/DESELEZIONA TUTTE LE CATEGORIE - SOTTOCATEGORIE
$('.macrocategory').change(function () {
    $cat = $(this).next().next().attr('class');
    $cat = $cat.replace(" macrocategory-label", "");
    //console.log($cat);

    if ($(this).prop('checked')) {
        $('.sub_' + $cat).prop('checked', 'checked');
    }
    else {
        $('.sub_' + $cat).prop('checked', false);
    }
});

// SELEZIONE/DESELEZIONE MACROCATEGORIA DA SOTTOCATEGORIA
$('.subcategory').change(function () {
    $subcat = $(this).next().next().attr('class');
    $cat = $subcat.replace(" subcategory-label", "");

    if ($(this).prop('checked')) {
        $('.' + $cat + '.macrocategory-label').prev().prev().prop('checked', 'checked');
    }
    else {
        if (($('input.sub_' + $cat + '.subcategory:checked').length) == 0) {
            $('.' + $cat + '.macrocategory-label').prev().prev().prop('checked', false);
        }

    }
});


function getRequest(name) {
    if (name = (new RegExp('[?&]' + encodeURIComponent(name) + '=([^&]*)')).exec(location.search))
        return decodeURIComponent(name[1]);
}

// funzione per la visualizzazione dei marker con le icone appropriate
function showmarker(feature, latlng, mType) {
    var serviceType = feature.properties.serviceType;
    var serviceIcon = serviceType;
    if (serviceType == "")
        serviceIcon = "generic";
    var marker;
    if (serviceType != "bus_real_time") {
        var icon = L.icon({
            iconUrl: ctx + '/img/mapicons/' + serviceIcon + '.png',
            iconRetinaUrl: ctx + '/img/mapicons/' + serviceIcon + '.png',
            iconSize: [26, 29],
            iconAnchor: [13, 29],
            className: mType,
            popupAnchor: [0, -27],
        });
        marker = L.marker(latlng, {icon: icon, title: serviceType + " - " + feature.properties.nome, riseOnHover: true});
        marker.on('mouseover', function (e) {
            if ($(e.target._icon).hasClass('selected')) {
            } else {
                var overIcon = L.icon({
                    iconUrl: ctx + '/img/mapicons/over/' + serviceIcon + '_over.png',
                    iconRetinaUrl: ctx + '/img/mapicons/over/' + serviceIcon + '_over.png',
                    iconSize: [26, 29],
                    iconAnchor: [13, 29],
                    popupAnchor: [0, -27],
                });
                e.target.setIcon(overIcon)
            }
        });
        marker.on('mouseout', function (e) {
            if ($(e.target._icon).hasClass('selected')) {
            } else {
                var defIcon = L.icon({
                    iconUrl: ctx + '/img/mapicons/' + serviceIcon + '.png',
                    iconRetinaUrl: ctx + '/img/mapicons/' + serviceIcon + '.png',
                    iconSize: [26, 29],
                    iconAnchor: [13, 29],
                    popupAnchor: [0, -27],
                });
                e.target.setIcon(defIcon)
            }
        });
    }
    else {
        var icon = L.icon({
            iconUrl: ctx + '/img/mapicons/' + serviceIcon + '.gif',
            iconRetinaUrl: ctx + '/img/mapicons/' + serviceIcon + '.gif',
            iconSize: [15, 15],
            iconAnchor: [7, 15],
            popupAnchor: [0, -7],
            className: "busRT",
        });
        marker = L.marker(latlng, {icon: icon, title: serviceType + " - " + feature.properties.vehicleNum + " - " + feature.properties.line, riseOnHover: true});
    }
    return marker;
}

//Function per mantenere fissa la posizione del popup anche al variare del suo contenuto
function popup_fixpos(divId) {
    var $leaflet = $('#' + divId).closest('div.leaflet-popup.leaflet-zoom-animated');
    var width = $leaflet.width();
    $leaflet.css({left: "-" + (width / 2) + "px"});

}

//Function che aggiorna il box di visualizzazione con il numero dei risultati di servizi-fermate-sensori
function risultatiRicerca(numServizi, numBus, numSensori, msg, dir) {
    var circle = $('path.leaflet-clickable');
    if (msg != 0) {
        $("#msg").hide();
        if (numServizi != 0) {
            if (msg == 1) {
                $("#serviceRes .value").text(numServizi);
                $("#serviceRes span.label").text("Services");
                $("#serviceRes img").attr("src", ctx + '/img/mapicons/TourismService.png');
                $("#serviceRes").show();
            } else {
                $("#serviceRes .value").text(numServizi);
                $("#serviceRes span.label").text("DigitalLocation");
                $("#serviceRes span.label").css("color", "#ad0a61");
                $("#serviceRes img").attr('src', ctx + '/img/mapiconsOLD/digitalLocation.png');
                $("#serviceRes").show();
            }
        } else {
            $("#serviceRes").hide();
        }
        if (numBus != 0) {
            $("#busstopRes .value").text(numBus);
            $("#busstopRes").show();
            if (dir != null) {
                $("#busDirection .value").text(dir);
                $("#busDirection").show();
            } else {
                $("#busDirection").hide();
            }
        } else {
            $("#busstopRes").hide();
            $("#busDirection").hide();
        }
        if (numSensori != 0) {
            $("#sensorRes .value").text(numSensori);
            $("#sensorRes").show();
        } else {
            $("#sensorRes").hide();
        }
        if (dir == null) {
            $(".circle.leaflet-clickable").css({stroke: "#0c0", fill: "#0c0"});
        }
    } else {
        if (dir == null) {
            $(".circle.leaflet-clickable").css({stroke: "#f03", fill: "#f03"});
        }
        $("#serviceRes").hide();
        $("#busstopRes").hide();
        $("#busDirection").hide();
        $("#sensorRes").hide();
        $("#msg").text("Search has not results.");
        $("#msg").show();
    }
    $("#searchOutput").show();
}

//Function che nasconde i risltati della ricerca
function nascondiRisultati() {
    $(".result .value").html("");
    $(".result").hide();
    $("#searchOutput").hide();
    $("#resultTPL").hide();
}


var circle = $('.leaflet-overlay-pane').children('path');

//Function che mostra gli autobur RT
function mostraAutobusRT(zoom) {


    $('.leaflet-marker-icon.busRT').not(".selected").remove();
    if (($('.leaflet-marker-icon.busRT.leaflet-zoom-animated.leaflet-clickable.selected').length) == 0) {
        $('#selezione').html("No selection");
        $('#approximativeAddress').html("");
    }
    //$('.popup_autobusRT').closest('.leaflet-popup.leaflet-zoom-animated').hide();

    $.ajax({
        url: ctx + "/ajax/json/get-autobusRT.jsp",
        type: "GET",
        async: true,
        dataType: 'json',
        success: function (msg) {

            $('#loading').hide();
            if (msg.features.length > 0) {
                servicesLayer = L.geoJson(msg, {
                    pointToLayer: function (feature, latlng) {
                        marker = showmarker(feature, latlng);
                        return marker;
                    },
                    onEachFeature: function (feature, layer) {
                        var contenutoPopup = "";
                        contenutoPopup = contenutoPopup + "<div class=\"popup_autobusRT\" >";
                        contenutoPopup = contenutoPopup + "<h3> AUTOBUS REAL TIME</h3>";
                        if (feature.properties.vehicleNum != "" && feature.properties.vehicleNum)
                            contenutoPopup = contenutoPopup + "<b>Bus Number: </b> " + feature.properties.vehicleNum + "<br />";
                        if (feature.properties.line != "" && feature.properties.line)
                            contenutoPopup = contenutoPopup + "<b>Bus Line: </b> " + feature.properties.line + "<br />";
                        if (feature.properties.direction != "" && feature.properties.direction)
                            contenutoPopup = contenutoPopup + "<b>Direction: </b> " + feature.properties.direction + "<br />";
                        if (feature.properties.detectionTime != "" && feature.properties.detectionTime)
                            contenutoPopup = contenutoPopup + "<b>Info: </b> position acquired " + feature.properties.detectionTime + "min. ago.<br />";
                        layer.bindPopup(contenutoPopup);
                    },
                    filter: function (feature, layer) {
                        var coords= feature.geometry.coordinates;
                        return (coords[0]!=-1 || coords[1]!=-1);
                    }
                });
                servicesLayer.addTo(map);
                if (zoom != undefined) {
                    var confiniMappa = servicesLayer.getBounds();
                    map.fitBounds(confiniMappa, {padding: [50, 50]});
                }
            }
        }});
    setTimeout(mostraAutobusRT, 30000);
}

function searchEvent(param, raggioRic, centroRic, numEv, text) {
    //$('#selection').hide();
    //$('.leaflet-marker-icon.event.leaflet-zoom-animated.leaflet-clickable').remove();
    if(eventLayer != null){
    map.removeLayer(eventLayer);
    }
    /*if(param != "transverse"){
        $('#searchOutput').hide();
    }*/
    
    var eventNum = 0;
    
    $.ajax({
        url: ctx + "/ajax/json/get-event-list.jsp",
        type: "GET",
        async: false,
        dataType: 'json',
        data: {
            range: param,
            raggio:raggioRic,
            centro:centroRic,
            numeroEventi:numEv,
            textFilter:text
        },
        success: function (msg) {
            $.ajax({
                        url: "ajax/get-weather.jsp",
                        type: "GET",
                        async: true,
                        //dataType: 'json',
                        data: {
                            nomeComune: "FIRENZE"
                        },
                        success: function (msg) {
                            $('#info-aggiuntive .content').html(msg);
                        }
                    });
            
            if(param != "free_text"){
                $('#loading').hide();
            }
            var i = 0;
            if (msg.Event.features.length > 0) {
                $('#event').show();
                // Funzione per definire nuova posizione a due marker sovrapposti
                var array = new Array();
                for (var r = 0;r<msg.Event.features.length;r++) {
                    array[r]=new Array();
                    for (var c = 0;c<2;c++) {
                        array[r][c]=0;
                    }
                } 
                
                for (i = 0; i < msg.Event.features.length; i++) {
                    if (msg.Event.features[i].properties.freeEvent == "YES") {
                        msg.Event.features[i].properties.evFree = ctx + '/img/mapicons/no_euro.png';
                    } else {
                        msg.Event.features[i].properties.evFree = ctx + '/img/mapicons/euro.png';
                    }
                    
                    if(i==0){
                        array[0][0]= msg.Event.features[i].geometry.coordinates[0];
                        array[0][1]= msg.Event.features[i].geometry.coordinates[1];
                    }else{
                        for(var k=0; k<i; k++){
                            if ((msg.Event.features[i].geometry.coordinates[0] == array[k][0]) && (msg.Event.features[i].geometry.coordinates[1] == array[k][1])){
                                array[i][0]= msg.Event.features[i].geometry.coordinates[0] + (Math.random() -.8) / 1500;
                                array[i][1]= msg.Event.features[i].geometry.coordinates[1] + (Math.random() -.8) / 1500;
                                msg.Event.features[i].geometry.coordinates[0] = array[i][0];
                                msg.Event.features[i].geometry.coordinates[1] = array[i][1];
                            }else{
                                array[i][0]= msg.Event.features[i].geometry.coordinates[0];
                                array[i][1]= msg.Event.features[i].geometry.coordinates[1];
                            }
                            
                        }
                            
                    }
                
                }
                // Fine riposizionamento marker
                
                eventNum = i;
                if(eventNum > 0){
                    $('#eventNum').html(eventNum + " events found.");
                }else{
                    $('#eventNum').html("No events planned.");    
                }
                $('#eventNum').show();
                var template = "{{#features}}" +
                        "<div class=\"eventItem\" id=\"event_{{id}}\" style=\"margin-top:5px; border:1px solid #000; padding:6px; overflow:auto;\" onclick=\"selectEvent({{id}})\">\n\
                            <div class=\"eventName\"><b style=\"color:#751a6a;\">{{properties.nome}}</b></div>" +
                        "<div class=\"eventInfo\" style=\"float:left; margin-top:7px; display:block; width:85%;\"><b>Place:</b> {{properties.place}}<br>" +
                        "<b>Date:</b> da {{properties.startDate}} {{#properties.endDate}}a {{properties.endDate}}{{/properties.endDate}}<br>" +
                        "{{#properties.startTime}}<b>Time:</b> {{properties.startTime}}{{/properties.startTime}}<br></div>" +
                        "<div class=\"eventFree\" style=\"float:right; margin-top:10px; display:block; width:15%;\"><img src=\"{{properties.evFree}}\"></img></div></div>" +
                        "{{/features}}";


                var output = Mustache.render(template, msg.Event);
                document.getElementById('eventList').innerHTML = output;
                var markerType = "event";

                eventLayer = L.geoJson(msg.Event, {
                    pointToLayer: function (feature, latlng) {
                        if (latlng[0] != -1 && latlng[1] != -1) {
                            marker = showmarker(feature, latlng, markerType);
                            return marker;
                        }
                    },
                    onEachFeature: function (feature, layer) {
                        var contenutoPopup = "";
                        contenutoPopup = contenutoPopup + "<div id=\"" + feature.id + "-"+feature.properties.tipo+"\" >";
                        contenutoPopup = contenutoPopup + "<div class=\"description\"><h3> " + feature.properties.nome + "</h3></div>";
                        contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + feature.properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
                        if (feature.properties.categoryIT != "" && feature.properties.categoryIT)
                        contenutoPopup = contenutoPopup + "<b>Event Type:</b> " + feature.properties.categoryIT + "<br />";
                        if (feature.properties.place != "" && feature.properties.place) {
                            contenutoPopup = contenutoPopup + "<b>Place: </b> " + feature.properties.place + "<br />";
                        }
                        if (feature.properties.address != "" && feature.properties.address)
                            contenutoPopup = contenutoPopup + "<b>Address:</b> " + feature.properties.address;
                        if (feature.properties.civic != "" && feature.properties.civic)
                            contenutoPopup = contenutoPopup + ", " + feature.properties.civic + "<br />";
                        else
                            contenutoPopup = contenutoPopup + "<br />";
                        
                        if (feature.properties.startDate != "" && feature.properties.startDate)
                            contenutoPopup = contenutoPopup + "<b>Date:</b> da " + feature.properties.startDate;
                        if (feature.properties.endDate != "" && feature.properties.endDate)
                            contenutoPopup = contenutoPopup + " a " + feature.properties.endDate + "<br />";
                        else
                            contenutoPopup = contenutoPopup + "<br />";
                        if (feature.properties.startTime != "" && feature.properties.startTime)
                            contenutoPopup = contenutoPopup + "<b>Time:</b> " + feature.properties.startTime + "<br />";
                        if (feature.properties.freeEvent != "" && feature.properties.freeEvent){
                            if(feature.properties.freeEvent == "YES"){
                            contenutoPopup = contenutoPopup + "<b>Price:</b> Ingresso libero<br />";
                            }else{
                                if (feature.properties.price != "" && feature.properties.price){
                                contenutoPopup = contenutoPopup + "<b>Price:</b> " + feature.properties.price + "<br />";    
                                }
                            }
                        }
                        if (feature.properties.website != "" && feature.properties.website)
                            if (((JSON.stringify(feature.properties.website)).indexOf("http\://")) != -1) {
                                contenutoPopup = contenutoPopup + "<b>Website:</b><a href=\"" + feature.properties.website + "\" target=\"_blank\" title=\"" + feature.properties.nome + " - website\"> " + feature.properties.website + "</a><br />";
                            } else {
                                contenutoPopup = contenutoPopup + "<b>Website:</b><a href=\"http\://" + feature.properties.website + "\" target=\"_blank\" title=\"" + feature.properties.nome + " - website\"> " + feature.properties.website + "</a><br />";
                            }
                        
                        if (feature.properties.phone != "" && feature.properties.phone)
                            contenutoPopup = contenutoPopup + "<b>Phone:</b> " + feature.properties.phone + "<br />";
                        if (feature.properties.descriptionIT != "" && feature.properties.descriptionIT)
                            contenutoPopup = contenutoPopup + "<div class=\"description\"><b>Description:</b> " + feature.properties.descriptionIT + "</div><br />";
                        var name = feature.properties.nome;
                        nameEscaped = escape(name);
                        var divSavePin = "savePin-" + feature.id;
                        contenutoPopup = contenutoPopup + "<div id=\"" + divSavePin + "\" class=\"savePin\" onclick=save_handler('" + feature.properties.tipo + "','" + feature.properties.serviceUri + "','" + nameEscaped + "')></div>";
                        layer.bindPopup(contenutoPopup);

                    }

                });
                eventLayer.addTo(map);
                var confiniMappa = eventLayer.getBounds();
                if(param != "free_text" && param != "transverse"){
                    map.fitBounds(confiniMappa, {padding: [50, 50]});
                }
            }else{
                    $('#event').show();
                    $('#eventNum').html("No events planned.");    
                    $('#eventNum').show();
                }
                
        }
    });
    return(eventNum);
}

function selectEvent(arg) {
    map.eachLayer(function (marker) {
        if (marker.feature != null && marker.feature.id != null ) {
            var id = marker.feature.properties.tipo+marker.feature.id;
            if (id === ("event"+arg)) {
                marker.openPopup();

            }
        }
    });
}

function selectRoute(code){
     map.eachLayer(function (e) {
        if(e._path != null){
            
            var route_id = e._path.attributes.class.nodeValue;
            if (route_id === ("route_ATAF_"+code+" leaflet-clickable")){
                e.bringToFront();
                e._path.attributes.stroke.value = "yellow";
            }
        }
         /*if (path.feature != null && marker.feature.id != null ) {
            var id = marker.feature.properties.tipo+marker.feature.id;
            if (id === ("event"+arg)) {
                marker.openPopup();

            }
        }*/
    });
}

function deselectRoute(code){
       map.eachLayer(function (e) {
        if(e._path != null){
            
            var route_id = e._path.attributes.class.nodeValue;
            if (route_id === ("route_ATAF_"+code+" leaflet-clickable")){
                   e._path.attributes.stroke.value = "#085ee8";
            }
        }
         /*if (path.feature != null && marker.feature.id != null ) {
            var id = marker.feature.properties.tipo+marker.feature.id;
            if (id === ("event"+arg)) {
                marker.openPopup();

            }
        }*/
    });   
}

function getLanguageResources(lang) {
    /*var it = new Array(); 
    var en = new Array();
 
    it['tabs1'] = "Bus Firenze"; en['tabs1'] = "Florence Bus";
    it['tabs2'] = "Comuni Toscana"; en['tabs2'] = "Tuscan Municipality";
    it['tabs3'] = "Ricerca testuale"; en['tabs3'] = "Text Search";
    it['tabs4'] = "Eventi"; en['tabs4'] = "Events";
 
    var resources = new Array();
    resources['it'] = it;
    resources['en'] = en;*/
    var resource = new Array();
    $.ajax({
        url: ctx + "/js/label_"+lang+".json",
        type: "GET",
        async: false,
        dataType: 'json',
        success: function (msg) {
            
            resource['Bus_Search'] = msg.Bus_Search;
            resource['Municipality_Search'] = msg.Municipality_Search;
            resource['Text_Search'] = msg.Text_Search;
            resource['Event_Search'] = msg.Event_Search;
            resource['Select_Line'] = msg.Select_Line;
            resource['Select_Route'] = msg.Select_Route;
            resource['Select_BusStop'] = msg.Select_BusStop;
            resource['Position_Bus'] = msg.Position_Bus;
            resource['Hide_Menu_dx'] = msg.Hide_Menu;
            resource['Hide_Menu_sx'] = msg.Hide_Menu;
            resource['Hide_Menu_meteo'] = msg.Hide_Menu;
            resource['Actual_Selection'] = msg.Actual_Selection;
            //resource['Selection'] = msg.Selection;
            resource['Select_Province'] = msg.Select_Province;
            resource['Select_Municipality'] = msg.Select_Municipality;
            resource['Select_Text_sx'] = msg.Select_Text_sx;
            resource['Num_Results_sx'] = msg.Num_Results_sx;
            resource['Select_Time'] = msg.Select_Time;
            resource['Day'] = msg.Day;
            resource['Week'] = msg.Week;
            resource['Month'] = msg.Month;
            resource['Event_Florence'] = msg.Event_Florence;
            resource['Search_Regular_Services'] = msg.Search_Regular_Services;
            resource['Search_Transversal_Services'] = msg.Search_Transversal_Services;
            resource['Services_Categories_R'] = msg.Services_Categories_R;
            resource['Select_All_R'] = msg.Select_All_R;
            resource['Num_Results_dx_R'] = msg.Num_Results_dx_R;
            resource['Search_Range_R'] = msg.Search_Range_R;
            resource['Services_Categories_T'] = msg.Services_Categories_T;
            resource['Select_All_T'] = msg.Select_All_T;
            resource['Num_Results_dx_T'] = msg.Num_Results_dx_T;
            resource['Search_Range_T'] = msg.Search_Range_T;
            resource['Search_Results'] = msg.Search_Results;
            resource['Results_BusLines'] = msg.Results_BusLines;
            resource['weather_mun'] = msg.weather_mun;
            resource['last_update'] = msg.last_update;
            resource['Loading_Message'] = msg.Loading_Message;
            resource['tipology'] = msg.tipology;
            resource['city'] = msg.city;
            resource['multimedia'] = msg.multimedia;
            resource['address'] = msg.address;
            resource['phone'] = msg.phone;
            resource['description'] = msg.description;
            resource['sensor_data'] = msg.sensor_data;
            resource['msg_real_time'] = msg.msg_real_time;
            resource['next_transits'] = msg.next_transits;
            resource['parking_data'] = msg.parking_data;
            resource['park_close'] = msg.park_close;
            resource['park_open'] = msg.park_open;
            resource['show_on_map'] = msg.show_on_map;
            resource['remove_from_map'] = msg.remove_from_map;
        }});
       
    //var resources = new Array();
    //resources['it'] = it;
    return resource;
}

function changeLanguage(lang) {
    var langResources = getLanguageResources(lang);
    if(lang == 'ENG'){
        $("#icon_lang").attr('src', ctx + "/img/icon_ITA.png");
        $("#icon_lang").attr('onclick', "changeLanguage('ITA')");
        $("#lang").attr('value', 'ENG');
    }else{
        $("#icon_lang").attr('src', ctx + "/img/icon_ENG.png");
        $("#icon_lang").attr('onclick', "changeLanguage('ENG')");
        $("#lang").attr('value', 'ITA');
    }
    
    $("span[name='lbl']").each(function (i, elt) {
        $(elt).text(langResources[$(elt).attr("caption")]);
    });
    
    $("span[class='giorno']").each(function (i, elt) {
        var id_day = $(elt).attr('id');
        if($('#'+id_day).text() != null){
        setWeekDay(id_day, $('#'+id_day).text());
        }
    });
    
    $("span[class='descrizione-meteo']").each(function (i, elt) {
        var id_meteo = $(elt).attr('id');
        if($('#'+id_meteo).text() != null){
        setWeatherPred(id_meteo, $('#'+id_meteo).text());
        }
    });
    
    /*if($("#day0").text() != null){
        setWeekDay('day0', $("#day0").text());
    }
    if($("#day1").text() != null){
        setWeekDay('day1', $("#day1").text());
    }
    if($("#day2").text() != null){
        setWeekDay('day2', $("#day2").text());
    }
    if($("#day3").text() != null){
        setWeekDay('day3', $("#day3").text());
    }
    if($("#day4").text() != null){
        setWeekDay('day4', $("#day4").text());
    }*/
    
}

function setWeekDay(div, day){
    switch (day) {
    case "Lunedi":
        $('#'+div).text('Monday');
        break;
    case "Martedi":
        $('#'+div).text('Tuesday');
        break;
    case "Mercoledi":
        $('#'+div).text('Wednesday');
        break;
    case "Giovedi":
        $('#'+div).text('Thursday');
        break;
    case "Venerdi":
        $('#'+div).text('Friday');
        break;
    case "Sabato":
        $('#'+div).text('Saturday');
        break;
    case "Domenica":
        $('#'+div).text('Sunday');
        break;
    
    case "Monday":
        $('#'+div).text('Lunedi');
        break;
    case "Tuesday":
        $('#'+div).text('Martedi');
        break;
    case "Wednesday":
        $('#'+div).text('Mercoledi');
        break;
    case "Thursday":
        $('#'+div).text('Giovedi');
        break;
    case "Friday":
        $('#'+div).text('Venerdi');
        break;
    case "Saturday":
        $('#'+div).text('Sabato');
        break;
    case "Sunday":
        $('#'+div).text('Domenica');
        break;    
        
    }
}

function setWeatherPred(div, desc){
    switch (desc) {
    case "sereno":
        $('#'+div).text('cloudless');
        break;
    case "poco nuvoloso":
        $('#'+div).text('bit cloudy');
        break;
    case "velato":
        $('#'+div).text('bleary');
        break;
    case "pioggia debole e schiarite":
        $('#'+div).text('light rain and sunny intervals');
        break;
    case "nuvoloso":
        $('#'+div).text('cloudy');
        break;
    case "pioggia debole":
        $('#'+div).text('light rain');
        break;
    case "coperto":
        $('#'+div).text('overcast');
        break;
    case "pioggia e schiarite":
        $('#'+div).text('rain and sunny intervals');
        break;
    case "pioggia moderata-forte":
        $('#'+div).text('moderate rainfall');
        break;
    case "foschia":
        $('#'+div).text('mist');
        break;
    case "temporale":
        $('#'+div).text('rainstorm');
        break;
    case "neve debole e schiarite":
        $('#'+div).text('light snow and sunny intervals');
        break;
    case "temporale e schiarite":
        $('#'+div).text('rainstorm and sunny intervals');
        break;
    case "neve moderata-forte":
        $('#'+div).text('moderate snowfall');
        break;
    case "neve e schiarite":
        $('#'+div).text('snow and sunny intervals');
        break;
    case "neve debole":
        $('#'+div).text('light snow');
        break;
    case "pioggia debole":
        $('#'+div).text('light rain');
        break; 
        
    case "cloudless":
        $('#'+div).text('sereno');
        break;
    case "bit cloudy":
        $('#'+div).text('poco nuvoloso');
        break;
    case "bleary":
        $('#'+div).text('velato');
        break;
    case "light rain and sunny intervals":
        $('#'+div).text('pioggia debole e schiarite');
        break;
    case "cloudy":
        $('#'+div).text('nuvoloso');
        break;
    case "light rain":
        $('#'+div).text('pioggia debole');
        break;
    case "overcast":
        $('#'+div).text('coperto');
        break;
    case "rain and sunny intervals":
        $('#'+div).text('pioggia e schiarite');
        break;
    case "moderate rainfall":
        $('#'+div).text('pioggia moderata-forte');
        break;
    case "mist":
        $('#'+div).text('foschia');
        break;
    case "rainstorm":
        $('#'+div).text('temporale');
        break;
    case "light snow and sunny intervals":
        $('#'+div).text('neve debole e schiarite');
        break;
    case "rainstorm and sunny intervals":
        $('#'+div).text('temporale e schiarite');
        break;
    case "moderate snowfall":
        $('#'+div).text('neve moderata-forte');
        break;
    case "snow and sunny intervals":
        $('#'+div).text('neve e schiarite');
        break;
    case "light snow":
        $('#'+div).text('neve debole');
        break;
    case "light rain":
        $('#'+div).text('pioggia debole');
        break;
        
    }
}