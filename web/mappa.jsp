<%@page import="org.json.simple.JSONArray"%>
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
        <!--<script src='https://api.tiles.mapbox.com/mapbox.js/v2.1.4/mapbox.js'></script>
        <link href='https://api.tiles.mapbox.com/mapbox.js/v2.1.4/mapbox.css' rel='stylesheet' />-->
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.5.1/dist/leaflet.css"
   integrity="sha512-xwE/Az9zrjBIphAcBb3F6JVqxf46+CDLwfLMHloNu6KEQCAWi6HcDUbeOfBIptF7tcCzusKFjFw2yuvEpDL9wQ=="
   crossorigin="" />
        <script src="https://unpkg.com/leaflet@1.5.1/dist/leaflet.js"
   integrity="sha512-GffPMF3RvMeYyc1LWMHtK8EbPv0iNZ8/oTtHPx9/cc2ILxQ+u905qIwdpULaqDkyBKgOaB57QTMg7ztg8Jm2Og=="
   crossorigin=""></script>
        <!--<script src="http://code.jquery.com/jquery-1.10.1.min.js"></script>-->
        <script src="https://code.jquery.com/jquery-1.11.2.min.js"></script>
        <script src="https://code.jquery.com/jquery-migrate-1.2.1.min.js"></script>
        <!--<script src="http://code.jquery.com/ui/1.10.4/jquery-ui.js"></script>-->
        <script src="https://code.jquery.com/ui/1.11.2/jquery-ui.js"></script>
        <script src="https://code.highcharts.com/highcharts.js"></script>
        <script src="https://code.highcharts.com/highcharts-3d.js"></script>
        <link rel="stylesheet" href="https://code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css" />	
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/leaflet.awesome-markers.css">
        <script src="${pageContext.request.contextPath}/js/leaflet.awesome-markers.min.js"></script>
        <script src="${pageContext.request.contextPath}/js/jquery.dialogextend.js"></script>
        <script src="${pageContext.request.contextPath}/js/leaflet-gps.js"></script>
        <script src="${pageContext.request.contextPath}/js/leaflet.markercluster.js"></script>
        <script src="${pageContext.request.contextPath}/js/mustache.js"></script> 
        <script src="${pageContext.request.contextPath}/js/mustache.min.js"></script>
        <script src="${pageContext.request.contextPath}/js/ViewManager.js"></script>
        <script src="${pageContext.request.contextPath}/js/jquery.dataTables.min.js"></script>
<!--        <script src="${pageContext.request.contextPath}/js/dataTables.bootstrap.min.js"></script>-->
        <script src="${pageContext.request.contextPath}/js/moment.min.js"></script>
        <script src="${pageContext.request.contextPath}/js/datetime-moment.js"></script>
        <script src="${pageContext.request.contextPath}/js/jquery.timepicker.min.js"></script>
        <script src="${pageContext.request.contextPath}/js/zoomHandler.js"></script>
        <script src="${pageContext.request.contextPath}/js/oms.min.js"></script>
        <!-- code per gallery -->
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/lib/jquery.mousewheel-3.0.6.pack.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/fancybox/source/jquery.fancybox.css" type="text/css" media="screen" />
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/source/jquery.fancybox.pack.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-buttons.css" type="text/css" media="screen" />
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-buttons.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-media.js"></script>
        <script src="${pageContext.request.contextPath}/js/wicket.js"></script>
        <script src="${pageContext.request.contextPath}/js/wicket-leaflet.js"></script>
        <!--  CARICAMENTO DEL FILE utility.js CON FUNZIONI NECESSARIE  -->
        <script type="text/javascript" charset="utf-8" src="${pageContext.request.contextPath}/js/utility.js?force=a"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/pathsearch.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/save_embed.js"></script>

        <link rel="stylesheet" href="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-thumbs.css" type="text/css" media="screen" />
        <script type="text/javascript" src="${pageContext.request.contextPath}/fancybox/source/helpers/jquery.fancybox-thumbs.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/MarkerCluster.css" type="text/css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/MarkerCluster.Default.css" type="text/css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css" type="text/css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/jquery.dataTables.min.css" type="text/css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/jquery.timepicker.min.css" type="text/css" />
        
        <!-- aggiunto michela -->
        <!-- Michela: mettere qui fancy tree -->
      
        <!-- 
        <script src="${pageContext.request.contextPath}/js/jquery.fancytree-all.min.js"></script> 
        -->
        <!-- Initialize the tree when page is loaded -->
        
        <!--
        <script>
        $("#tree2").fancytree({
            checkbox: true,
            selectMode: 2,
            source: "${pageContext.request.contextPath}/ajax/json/taxonomy.jsp",
            select: function(event, data) {
                    // Display list of selected nodes
                    var selNodes = data.tree.getSelectedNodes();
                    // convert to title/key array
                    var selKeys = $.map(selNodes, function(node){
                               return "[" + node.key + "]: '" + node.title + "'";
                            });
                    $("#echoSelection2").text(selKeys.join(", "));
            },
            click: function(event, data) {
                    // We should not toggle, if target was "checkbox", because this
                    // would result in double-toggle (i.e. no toggle)
                    if( $.ui.fancytree.getEventTargetType(event) === "title" ){
                            data.node.toggleSelected();
                    }
            },
            keydown: function(event, data) {
                    if( event.which === 32 ) {
                            data.node.toggleSelected();
                            return false;
                    }
            },
            // The following options are only required, if we have more than one tree on one page:
            cookieId: "fancytree-Cb2",
            idPrefix: "fancytree-Cb2-"
		});
        </script>
            -->
            
    </head>
    <body class="Chrome" onload="">
        <% if (!gaCode.isEmpty()) {%>
        <script>
            (function (i, s, o, g, r, a, m) {
                i['GoogleAnalyticsObject'] = r;
                i[r] = i[r] || function () {
                    (i[r].q = i[r].q || []).push(arguments)
                }, i[r].l = 1 * new Date();
                a = s.createElement(o),
                        m = s.getElementsByTagName(o)[0];
                a.async = 1;
                a.src = g;
                m.parentNode.insertBefore(a, m)
            })(window, document, 'script', '//www.google-analytics.com/analytics.js', 'ga');
            ga('create', '<%=gaCode%>', 'auto');
            ga('send', 'pageview');
        </script>
        <% } %>
        <script>
            var mode = "${param.mode}";
            var api = "${param.api}";
        </script>

        <div id="dialog"></div>        
        <!-- <div id="QueryConfirmSave" title="'Save Query"> <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0; display: none;"></span>Do you want to save this query?</p> </div> !-->
        <div id="map">
            <div class="menu" id="help">
                <a href="http://www.disit.org/servicemap" title="Aiuto Service Map" target="_blank"><img src="${pageContext.request.contextPath}/img/help.png" alt="help SiiMobility ServiceMap" width="28" /></a>
            </div>
        </div>
        <%
            if(request.getParameter("apikey")!=null) {
              request.getSession().setAttribute("apikey",request.getParameter("apikey"));
            }
            Connection conMySQL = null;
            Statement st = null;
            ResultSet rs = null;
            Connection conMySQL2 = null;
            Statement st2 = null;
            ResultSet rs2 = null;
            Connection conMySQL3 = null;
            Statement st3 = null;
            ResultSet rs3 = null;
            Repository repo = new SPARQLRepository(sparqlEndpoint);
            repo.initialize();
            RepositoryConnection con = repo.getConnection();
        %>
        <div class="menu" id="save">
            <img src="${pageContext.request.contextPath}/img/save.png" alt="Salva la configurazione" width="28" onclick="save_handler(null, null, null, true);" />
        </div>
        <div class="menu" id="embed">
            <img src="${pageContext.request.contextPath}/img/embed_icon.png" alt="Embed Service Map" width="28" onclick="embedConfiguration();" /> 
        </div>
        <div id="menu-alto-hidden" class="menu-logo">
            <div id="siiMob"> <a href="http://km4city.org"  target="_blank" ><img src="${pageContext.request.contextPath}/img/km4city.png"
                                                          height="28" margin-left="15px" alt="KM4City" > </img></a></div>
            <div id="km4city"> <a href="http://www.sii-mobility.org"  target="_blank" ><img src="${pageContext.request.contextPath}/img/SiiMobility_logo.png"
                                                                      width="60"  alt="SiiMobility" > </img></a></div>
        </div>
                                                                      
        <div id="menu-alto" class="menu">
            <div id="lang" value="ENG"><img width="40" id="icon_lang"></img></div>
            <div id="siiMob"> <a href="http://km4city.org"  target="_blank" ><img src="${pageContext.request.contextPath}/img/km4city.png"
                                                                      height="28" margin-left="15px" alt="KM4City" > </img></a></div>
            <div id="km4city"> <a href="http://www.sii-mobility.org"  target="_blank" ><img src="${pageContext.request.contextPath}/img/SiiMobility_logo.png"
                                                                      width="60"  alt="SiiMobility" > </img></a></div>
            
            <!--
            <a href="http://www.google.com" target="_blank"> 
                <img width="40" height="40" border="0" align="center"  src="${pageContext.request.contextPath}/img/embed_icon.png" /> 
            </a>-->
            <div class="header-container">
            <div class="header">
                <span name="lbl" caption="Hide_Menu_sx"> - Hide Menu</span>
            </div>
            </div>
            <div class="content">
                <div id="tabs">
                    <ul>
                        <li><a href="#tabs-1"><span name="lbl" caption="Bus_Search">Tuscan Public Transport</span></a></li>
                        <li><a href="#tabs-2"><span name="lbl" caption="Municipality_Search">Municipalities</span></a></li>
                            <%-- <li><a href="#tabs-3">Posizione</a></li> --%>
                        <li><a href="#tabs-search"><span name="lbl" caption="Text_Search">Text Search</span></a></li>
                        <li><a href="#tabs-addr-search"><span name="lbl" caption="Text_AddrSearch">Address Search</span></a></li>
                        <li><a href="#tabs-Event"><span name="lbl" caption="Event_Search">Events</span></a></li>
                    </ul>
                    <div id="tabs-1">
                        <div class="use-case-1">
                            <span name="lbl" caption="Select_Agency">Select an agency</span>:
                            <br/>
                            <select id="elencoagenzie" name="elencoagenzie" onchange="mostraElencoLinee(this);"></select>
                            <br/>
                            <span name="lbl" caption="Select_Line">Select a Line</span>:
                            <br/>
                            <!--<select id="elencolinee" name="elencolinee" onchange="mostraElencoFermate(this);"> </select>-->
                            <select id="elencolinee" name="elencolinee" onchange="mostraElencoPercorsi(this);">
                                 <option value=""> - Select a Line -</option>
                            </select>
                            <br/>
                            <span name="lbl" caption="Select_Route">Select a route</span>:
                            <br/>
                            <!--<select id="elencopercorsi" name="elencopercorsi" onchange="mostraElencoPercorsi(this);"></select>-->
                            <select id="elencopercorsi" name="elencopercorsi" onchange="mostraElencoFermate(this);">
                                <option value=""> - Select a Route -</option>
                            </select>
                            <br/>
                            <span name="lbl" caption="Select_BusStop">Select a Stop</span>:
                            <br/>
                            <select id="elencofermate" name="elencofermate" onchange="mostraFermate(this);">
                                <option value=""> - Select a Stop - </option>	
                            </select>
                            <div id="pulsanteRT" name="autobusRealTime" onclick="mostraAutobusRT(true);"><span name="lbl" caption="Position_Bus">Position of selected Busses</span></div>
                        </div>
                    </div>
                    <div id="tabs-2">
                        <div class="use-case-2">
                            <span name="lbl" caption="Select_Province">Select a province</span>:
                            <br/>
                            <select id="elencoprovince" name="elencoprovince" onchange="mostraElencoComuni(this);">
                                <option value=""> - Select a Province - </option>
                                <option value="all">ALL THE PROVINCES</option>
                                <option disabled>--TOSCANA----------</option>
                                <option value="Arezzo">AREZZO</option>
                                <option value="Firenze">FIRENZE</option>
                                <option value="Grosseto">GROSSETO</option>
                                <option value="Livorno">LIVORNO</option>
                                <option value="Lucca">LUCCA</option>
                                <option value="Massa Carrara">MASSA-CARRARA</option>
                                <option value="Pisa">PISA</option>
                                <option value="Pistoia">PISTOIA</option>
                                <option value="Prato">PRATO</option>
                                <option value="Siena">SIENA</option>
                                <option disabled>--SARDEGNA---------</option>
                                <option value="Cagliari">CAGLIARI</option>
                            </select>
                            <br />
                            <span name="lbl" caption="Select_Municipality">Select a municipality</span>:
                            <br/>
                            <select id="elencocomuni" name="elencocomuni" onchange="updateSelection();
                                    ">
                                <option value=""> - Select a Municipality - </option>
                            </select>
                        </div>
                    </div>
                    <%-- <div id="tabs-3">
                        <div class="use-case-3">Seleziona un punto sulla mappa:
                            <img src="${pageContext.request.contextPath}/img/info.png" alt="Seleziona un punto della mappa" width="26" id="choosePosition" />    
                        </div>
                    </div> --%>
                    <div id="tabs-search">
                        <div class="use-case-search"><span name="lbl" caption="Select_Text_sx">Search serviceTextFilterby Text</span>:
                            <input type="text" name="search" id="freeSearch" onkeypress="event.keyCode == 13 ? searchText() : false">
                            <br><br><span name="lbl" caption="Num_Results_sx">Max number of results</span>:
                            <select id="numberResults" name="numberResults">
                                <option value="100">100</option>
                                <option value="200">200</option>
                                <option value="500">500</option>
                                <option value="0">No limit</option>
                            </select>
                            <div class="menu" id="serviceTextSearch">
                                <img src="${pageContext.request.contextPath}/img/search_icon.png" alt="Search Services" width="24" onclick="searchText()" />
                            </div>
                            <div class="menu" id="saveQuerySearch">
                                <img src="${pageContext.request.contextPath}/img/save.png" alt="Salva la query" width="28" onclick="save_handler(null, null, null, false, 'freeText');" />
                            </div> 
                            <!--<fieldset id="address-selection" style="margin-top:10px"> 
                                <legend><span name="lbl" caption="">Quick address/location search</span></legend>
                                exclude POI:<input type="checkbox" id="quick-search-poi">
                                AND mode:<input type="checkbox" id="quick-search-and">
                                <input id="quick-search" >
                            </fieldset>-->
                        </div>
                    </div>                          
                    <div id="tabs-addr-search">
                        <div class="use-case-addr-search">
                                exclude POI:<input type="checkbox" id="quick-search-poi">
                                AND mode:<input type="checkbox" checked="checked" id="quick-search-and">
                                sort by distance:<input type="checkbox" checked="checked" id="quick-search-sortdist"><br>
                                position:<input id="quick-search-position" value="">
                                maxDists:<input id="quick-search-maxdists" value=""><br>
                                categories:<input id="quick-search-categories" value="BusStop;StreetNumber;Municipality">
                                <input id="quick-search" >
                        </div>
                    </div>                          
                    <div id="tabs-Event" style="padding-top:10px;">
                        <span name="lbl" caption="Select_Time">Select a time interval: </span>
                        <input type="radio" id= "event_choice_d" name="event_choice" value="day" onchange="searchEvent(this.value, null, null)"><span name="lbl" caption="Day">Day</span></input>
                        <input type="radio" id= "event_choice_w" name="event_choice" value="week" onchange="searchEvent(this.value, null, null)"><span name="lbl" caption="Week">Week</span></input>
                        <input type="radio" id= "event_choice_m" name="event_choice" value="month" onchange="searchEvent(this.value, null, null)"><span name="lbl" caption="Month">Month</span></input>
                        <img id="saveEventSearch" src="${pageContext.request.contextPath}/img/save.png" alt="Salva la query" width="28" onclick="save_handler('event', null, null, false, null);" />
                        <fieldset id="event"> 
                            <legend><span name="lbl" caption="Event_Florence">Events in Florence</span></legend>
                            <div id="eventNum" style="display:none;"></div>
                            <div id="eventList"></div>
                        </fieldset>
                    </div>
                    <!--  
                    <div id="tabs-Event" style="padding-top:10px;">
                        <div id="event_radio">
                        <span name="lbl" caption="Select_Time">Select a time interval: </span>
                        <input type="radio" id= "event_choice" name="event_choice" value="day" ><span name="lbl" caption="Day">Day</span></input>
                        <input type="radio" id= "event_choice" name="event_choice" value="week" ><span name="lbl" caption="Week">Week</span></input>
                        <input type="radio" id= "event_choice" name="event_choice" value="month" ><span name="lbl" caption="Month">Month</span></input>
                        </div>
                        <img id="EventSearch" src="${pageContext.request.contextPath}/img/search_icon.png" alt="Salva la query" width="28" onclick="searchEvent($(#event_radio).value), null, null)" />
                        <img id="saveEventSearch" src="${pageContext.request.contextPath}/img/save.png" alt="Salva la query" width="28" onclick="save_handler('event', null, null, false, null);" />
                        <fieldset id="event"> 
                            <legend><span name="lbl" caption="Event_Florence">Events in Florence</span></legend>
                            <div id="eventNum" style="display:none;"></div>
                            <div id="eventList"></div>
                        </fieldset>
                    -->
                    </div>
                    <fieldset id="selection"> 
                        <legend><span name="lbl" caption="Actual_Selection">Actual Selection</span></legend>
                        <span id="selezione" >No selection</span> <br>
                        <div id="approximativeAddress"></div>
                    </fieldset>
                    <fieldset id="path" style="display:none"> 
                        <legend><span name="lbl" caption="Path">Path</span></legend>
                        <div id="path_start">From: ?</div>
                        <div id="path_end">To: ?</div>
                        Route via: <select id="path_type">
                          <option value="foot_shortest">foot_shortest</option>
                          <option value="foot_quiet">foot_quiet</option>
                          <option value="car">car</option>
                          <option value="public_transport">public_transport</option>
                        </select><br>
                        Start date&amp;time: <input type="text" id="path_date" placeholder="today" maxlength="10"/>
                        <input type="text" id="path_time" placeholder="now" max length="10"/><br>
                        <button style="margin:10px 0px;" onclick="doSearchPath()">Search Path</button>
                        <hr>
                        <div id="pathresult" style="max-height:250px;overflow:auto;"></div>
                    </fieldset>
                    <div id="queryBox"></div>
                </div>
            </div>
        </div>
        <div id="loading">
            <div id="messaggio-loading">
                <img src="${pageContext.request.contextPath}/img/ajax-loader.gif" width="32" />
                <h3>Loading...</h3>
                <span name="lbl" caption="Loading_Message">Loading may take time</span>
            </div>
        </div>
        <div id="serviceMap_query_toggle"></div>
        <div id="menu-dx" class="menu">
            <div class="header">
                <span name="lbl" caption="Hide_Menu_dx"> - Hide Menu</span>
            </div>
            <div class="content">
                <div id="tabs-servizi">
                    <ul>
                        <li><a href="#tabs-4"><span name="lbl" caption="Search_Regular_Services">Regular Services</span></a></li>
                        <li><a href="#tabs-5"><span name="lbl" caption="Search_Transversal_Services">Transversal Services</span></a></li>
                        <!-- <li><a href="#tree"><span name="lbl" caption="Search_Services">Test Tree Services</span></a></li> -->
                        
                    </ul>    
                    <div id="tabs-4">
                        <div class="use-case-4">
                            <span name="lbl" caption="Services_Categories_R">Services Categories</span> 
                            <br /> 
                            <input type="checkbox" name="macro-select-all" id="macro-select-all" value="Select All" /> <span name="lbl" caption="Select_All_R">De/Select All</span>
                            <div id="categorie">
                                
                                <%
                                   conMySQL = ConnectionPool.getConnection();
                                   try {
                                    String query = "SELECT distinct MacroClass FROM ServiceCategory_menu_NEW where TypeOfService not like 'T_Service' AND Visible = '1' order by MacroClass";

                                     // create the java statement
                                     st = conMySQL.createStatement();
                                     // execute the query, and get a java resultset
                                     rs = st.executeQuery(query);

                                     // iterate through the java resultset
                                     while (rs.next()) {

                                         String macroClass = rs.getString("MacroClass");
                                         out.println("<input type='checkbox' name='" + macroClass + "' value='" + macroClass + "' class='macrocategory' /> <img src='" + request.getContextPath() + "/img/mapicons/" + macroClass + ".png' height='23' width='20' align='top'> <span class='" + macroClass + " macrocategory-label'>" + macroClass + "</span> <span class='toggle-subcategory' title='Mostra sottocategorie'>+</span>");
                                         out.println("<div class='subcategory-content'>");

                                         String query2 = "SELECT distinct SubClass,icon FROM ServiceCategory_menu_NEW WHERE MacroClass = '" + macroClass + "' AND TypeOfService not like 'T_Service' AND Visible = '1' ORDER BY SubClass ASC";
                                         // create the java statement
                                         st2 = conMySQL.createStatement();
                                         // execute the query, and get a java resultset
                                         rs2 = st2.executeQuery(query2);
                                         // iterate through the java resultset

                                         while (rs2.next()) {

                                             //String sub_nome = rs2.getString("Ita");
                                             String subClass = rs2.getString("SubClass");
                                             String subClass_ico = rs2.getString("icon");
                                             if(subClass_ico == null)
                                               subClass_ico = macroClass + "_" + subClass;

                                             out.println("<input type='checkbox' name='" + subClass + "' value='" + subClass + "' class='sub_" + macroClass + " subcategory' /> <img src='" + request.getContextPath() + "/img/mapicons/" + subClass_ico + ".png' height='19' width='16' align='top'>");
                                             // modifica per RTZgate
                                             //if (sub_en_name.equals("rTZgate")) {
                                             //sub_en_name = "RTZgate";
                                             //}
                                             out.println("<span class='" + macroClass + " subcategory-label'>" + subClass + "</span>");
                                             out.println("<br />");
                                         }

                                         out.println("</div>");
                                         out.println("<br />");
                                         st2.close();
                                         //conMySQL2.close();
                                     }
                                     st.close();
                                   } finally {
                                     conMySQL.close();
                                   }
                                %>
                                <br />
                            </div>
                            <span name="lbl" caption="Filter_Results_dx_R">Filter</span>:
                            <input type="text" name="serviceTextFilter" id="serviceTextFilter" placeholder="search text into service" onkeypress="event.keyCode == 13 ? ricercaServizi('categorie', null, null) : false"/><br />    
                            <% if(conf.get("searchByValueType", "false").equals("true") || request.getParameter("debug")!=null ) {
                              out.println("Service providing value type:<select id=\"valueTypeFilter\" name=\"valueTypeFilter\">");
                              out.println("<option value=\"\">select value type</option>");
                              RepositoryConnection rcon = ServiceMap.getSparqlConnection();
                              try {
                                String sparql = "SELECT DISTINCT ?vt WHERE{\n"
                                   + " ?vt a ssn:Property.\n"
                                   //+ " ?vt km4c:value_unit ?u.\n"
                                   + "} ORDER BY asc(STR(?vt))";
                                TupleQuery q = rcon.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
                                TupleQueryResult tqr = q.evaluate();
                                while(tqr.hasNext()) {
                                  BindingSet bs = tqr.next();
                                  String value_type = bs.getBinding("vt").getValue().stringValue();
                                  value_type = value_type.substring(value_type.lastIndexOf("/")+1);                                  
                                  out.println("<option>"+value_type+"</option>");
                                }
                              } finally {
                                rcon.close();
                              }
                              out.println("</select><br>");
                            } %>
                            <span name="lbl" caption="Num_Results_dx_R">N. results</span>:
                            <select id="nResultsServizi" name="nResultsServizi">
                                <option value="10">10</option>
                                <option value="20">20</option>
                                <option value="50">50</option>
                                <option value="100" selected="selected">100</option>
                                <option value="200">200</option>
                                <option value="500">500</option>
                                <option value="0">No Limit</option>
                            </select>
                            <br />
                            <hr />
                            <span name="lbl" caption="Search_Range_R">Search range</span>
                            <select id="raggioricerca" name="raggioricerca">
                                <option value="0.1">100 mt</option>
                                <option value="0.2">200 mt</option>
                                <option value="0.3">300 mt</option>
                                <option value="0.5">500 mt</option>
                                <option value="1">1 km</option>
                                <option value="2">2 km</option>
                                <option value="5">5 km</option>
                                <option value="area" selected="selected">visible area</option>
                                <option value="geo">specific area</option>
                                <option value="inside">inside</option>
                            </select><br />
                            <span name="lbl" caption="Search_Area_R">Search area</span>
                            <select id="geosearch" name="geosearch" disabled="disabled">
                              <option value='select'>select...</option>
                              <%                              
                                    conMySQL = ConnectionPool.getConnection();
                                    try {
                                      String queryLabel = "SELECT label FROM Geometry ORDER by label";

                                      // create the java statement
                                      st = conMySQL.createStatement();
                                      // execute the query, and get a java resultset
                                      rs = st.executeQuery(queryLabel);

                                      // iterate through the java resultset
                                      while (rs.next()) {
                                          String label = rs.getString("label");
                                          out.println("<option value='" + label + "'>"+label+"</option>");
                                      }
                                      st.close();
                                    } finally {
                                      conMySQL.close();
                                    }
                                %>    
                            </select>
                            <hr />
                            <!--<input type="button" value="Cerca!" id="pulsante-ricerca" onclick="ricercaServizi();" />
                             <input type="button" value="Pulisci" id="pulsante-reset" onclick="resetTotale();" /> !-->
                            <div class="menu" id="serviceSearch">
                                <img src="${pageContext.request.contextPath}/img/search_icon.png" alt="Search Services" width="24" onclick="ricercaServizi('categorie', null, null);" />
                            </div>
                            <div class="menu" id="serviceSearchChart">
                                <img src="${pageContext.request.contextPath}/img/search_chart.png" alt="Search Chart" width="24" onclick="showChart(selezione, $('#raggioricerca').val(), coordinateSelezione);" />
                            </div>
                            <div class="menu" id="clearAll">
                                <img src="${pageContext.request.contextPath}/img/clear_icon.png" alt="Clear all" width="28" onclick="resetTotale();" />
                            </div>
                            <div id="chart_dialog" title="Query results organized by category">
                                <p></p>
                                <div id="chart_container"></div>
                            </div>
                            <div class="menu" id="saveQuery">
                                <img src="${pageContext.request.contextPath}/img/save.png" alt="Salva la query" width="28" onclick="save_handler();" />
                            </div>
                            <br />

                        </div>
                    </div>
                    <div id="tabs-5">
                        <div class="use-case-5">
                            <!--<h3>Coming soon...</h3>-->
                            <!-- AGGIUNTA TRANSVERSAL SERVICES -->
                            <!-- <input type="text" name="serviceTextFilter_t" id="serviceTextFilter_t" placeholder="search text into service" onkeypress="event.keyCode == 13 ? ricercaServizi('categorie_t', null, null) : false"/><br /> -->
                            <span name="lbl" caption="Services_Categories_T">Services Categories</span> 
                            <br />
                            <input type="checkbox" name="macro-select-all_t" id="macro-select-all_t" value="Select All" /> <span name="lbl" caption="Select_All_T">De/Select All</span>
                            <div id="categorie_t">
                                
                                <% 
                                    conMySQL = ConnectionPool.getConnection();
                                    try {
                                      String query_T = "SELECT distinct MacroClass FROM ServiceCategory_menu_NEW where TypeOfService not like 'Service' AND Visible = '1' order by MacroClass";
                                      // create the java statement
                                      st = conMySQL.createStatement();

                                      // execute the query, and get a java resultset
                                      rs = st.executeQuery(query_T);

                                      // iterate through the java resultset
                                      while (rs.next()) {
                                          String classe = rs.getString("MacroClass");
                                          //String iniziale = classe.substring(0, 1).toLowerCase();
                                          //String classe_ico = iniziale.concat(classe.substring(1, classe.length()));
                                          out.println("<input type='checkbox' name='" + classe + "' value='" + classe + "' class='macrocategory' /> <img src='" + request.getContextPath() + "/img/mapicons/" + classe + ".png' height='23' width='20' align='top'> <span class='" + classe + " macrocategory-label'>" + classe + "</span> <span class='toggle-subcategory' title='Mostra sottocategorie'>+</span>");
                                          out.println("<div class='subcategory-content'>");
                                          //conMySQL2 = DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
                                          String query2 = "SELECT distinct labelITA, SubClass FROM ServiceCategory_menu_NEW WHERE MacroClass = '" + classe + "' AND Visible = '1' ORDER BY SubClass ASC";
                                          // create the java statement
                                          st2 = conMySQL.createStatement();
                                          // execute the query, and get a java resultset
                                          rs2 = st2.executeQuery(query2);
                                          // iterate through the java resultset
                                          while (rs2.next()) {
                                              //String sub_nome = rs2.getString("Ita");
                                              String sub_en_name = rs2.getString("SubClass");
                                              //String subclasse_ico = classe_ico + "_" + sub_en_name;

                                              //conMySQL3 = DriverManager.getConnection(urlMySqlDB + dbMySql, userMySql, passMySql);
                                              if (sub_en_name.equals("Event")) {
                                                  String subclasse_ico = "HappeningNow_" + sub_en_name;
                                                  out.println("<input type='checkbox' name='" + sub_en_name + "' value='" + sub_en_name + "' class='sub_" + classe + " subcategory' /> <img src='" + request.getContextPath() + "/img/mapicons/" + subclasse_ico + ".png' height='19' width='16' align='top'>");
                                                  out.println("<span class='" + classe + " subcategory-label'>" + sub_en_name + "</span>");
                                                  out.println("<br />");
                                              } else {
                                                  String query3 = "SELECT distinct MacroClass FROM ServiceCategory_menu_NEW WHERE SubClass = '" + sub_en_name + "' AND TypeOfService not like 'T_Service'";
                                                  st3 = conMySQL.createStatement();
                                                  rs3 = st3.executeQuery(query3);
                                                  while (rs3.next()) {
                                                      String macro_cat = rs3.getString("MacroClass");

                                                      //String subclasse_ico = (macro_cat.substring(0, 1).toLowerCase()).concat(macro_cat.substring(1, macro_cat.length())) + "_" + sub_en_name;
                                                      String subclasse_ico = macro_cat + "_" + sub_en_name;

                                                      out.println("<input type='checkbox' name='" + sub_en_name + "' value='" + sub_en_name + "' class='sub_" + classe + " subcategory' /> <img src='" + request.getContextPath() + "/img/mapicons/" + subclasse_ico + ".png' height='19' width='16' align='top'>");
                                                      out.println("<span class='" + classe + " subcategory-label'>" + sub_en_name + "</span>");
                                                      out.println("<br />");
                                                  }
                                              }
                                              //String macro_cat = rs3.getString("SubClasse");
                                              //ServiceMap.println("res3_macrocat:" + macro_cat);
                                              //String subclasse_ico = (macro_cat.substring(0, 1).toLowerCase()).concat(macro_cat.substring(1, classe.length()))+ "_" + sub_en_name;
                                              //String sub_numero = rs2.getString("NUMERO");
                                              //out.println("<input type='checkbox' name='" + sub_en_name + "' value='" + sub_en_name + "' class='sub_" + classe + " subcategory' /> <img src='" + request.getContextPath() + "/img/mapicons/" + subclasse_ico + ".png' height='19' width='16' align='top'>");
                                              //out.println("<span class='" + classe + " subcategory-label'>" + sub_en_name + "</span>");
                                              //out.println("<br />");

                                              st3.close();
                                          }

                                          out.println("</div>");
                                          out.println("<br />");

                                          st2.close();
                                      }
                                      st.close();
                                    } finally {
                                      conMySQL.close();
                                    }
                                %>
                                <input type="checkbox" name="fresh-place" value="Fresh_place" id="FreshPlace" class="macrocategory" /> <img src='${pageContext.request.contextPath}/img/mapicons/TourismService_Fresh_place.png' height='23' width='20' align='top'/> <span class="fresh-place macrocategory-label">Fresh Place</span>
                                <br/>
                                <input type="checkbox" name="public-transport-line" value="PublicTransportLine" id="PublicTransportLine" class="macrocategory" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Urban_bus.png' height='23' width='20' align='top'/> <span class="public-transport-line macrocategory-label">Public Transport Line</span> 
                                <br />
                                <input type="checkbox" name="road-sensor" value="SensorSite" id="Sensor" class="macrocategory" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_SensorSite.png' height='23' width='20' align='top'/> <span class="road-sensor macrocategory-label">Road Sensors</span> 
                                <br />
                                <input type="checkbox" name="near-bus-stops" value="BusStop" class="macrocategory" id="Bus" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_BusStop.png' height='23' width='20' align='top'/> <span class="near-bus-stops macrocategory-label">Bus Stops</span>
                                <br />
                                <input type="checkbox" name="near-tram-stops" value="Tram_stops" class="macrocategory" id="Tram" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Tram_stops.png' height='23' width='20' align='top'/> <span class="public-transport-line macrocategory-label">Tram_stops</span>
                                <br/>
                                <input type="checkbox" name="near-subway-station" value="Subway_station" class="macrocategory" id="Subway" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Subway_station.png' height='23' width='20' align='top'/> <span class="public-transport-line macrocategory-label">Subway_station</span>
                                <br/>
                                <input type="checkbox" name="near-train-station" value="Train_station" class="macrocategory" id="Train" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Train_station.png' height='23' width='20' align='top'/> <span class="public-transport-line macrocategory-label">Train_station</span>
                                <br/>
                                <input type="checkbox" name="near-ferry-stops" value="Ferry_stop" class="macrocategory" id="Ferry" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Ferry_stop.png' height='23' width='20' align='top'/> <span class="public-transport-line macrocategory-label">Ferry_stop</span>
                                <br/>
                                <input type="checkbox" name="near-car-park" value="Car_park" class="macrocategory" id="TransferServiceAndRenting" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Car_park.png' height='23' width='20' align='top'/> <span class="TransferServiceAndRenting macrocategory-label">Car_park</span>
                                <br/>
                                <input type="checkbox" name="near-rtz" value="Bike_sharing_rack" class="macrocategory" id="TransferServiceAndRenting" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Bike_sharing_rack.png' height='23' width='20' align='top'/> <span class="TransferServiceAndRenting macrocategory-label">Bike_sharing_rack</span>
                                <br/>
                                <input type="checkbox" name="near-rtz" value="RTZgate" class="macrocategory" id="DigitaLocation" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_RTZgate.png' height='23' width='20' align='top'/> <span class="TransferServiceAndRenting macrocategory-label">RTZgate</span>
                                <br/>
                                <input type="checkbox" name="near-rtz" value="Fuel_station" class="macrocategory" id="DigitaLocation" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Fuel_station.png' height='23' width='20' align='top'/> <span class="TransferServiceAndRenting macrocategory-label">Fuel_station</span>
                                <br/>
                                <input type="checkbox" name="near-rtz" value="Charging_stations" class="macrocategory" id="TransferServiceAndRenting" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Charging_stations.png' height='23' width='20' align='top'/> <span class="TransferServiceAndRenting macrocategory-label">Charging_stations</span>
                                <br/>
                                <input type="checkbox" name="near-underpass" value="Underpass " class="macrocategory" id="TransferServiceAndRenting" /> <img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_Underpass.png' height='23' width='20' align='top'/> <span class="TransferServiceAndRenting macrocategory-label">Underpass</span>
                                <br/>
                                <input type="checkbox" name="near-air-quality-stations" value="Air_quality_monitoring_station" class="macrocategory" id="Environment" /> <img src='${pageContext.request.contextPath}/img/mapicons/Environment_Air_quality_monitoring_station.png' height='23' width='20' align='top'/> <span class="Environment macrocategory-label">Air_quality_monitoring_station</span>
                                <br/>
                                <input type="checkbox" name="near-pollen-monitoring-stations" value="Pollen_monitoring_station"  class="macrocategory" id="Environment" /> <img src='${pageContext.request.contextPath}/img/mapicons/Environment_Pollen_monitoring_station.png' height='23' width='20' align='top'/> <span class="Environment macrocategory-label">Pollen_monitoring_station</span>
                                <br/>
                                <input type="checkbox" name="near-smart-waste-container" value="Smart_waste_container"  class="macrocategory" id="Environment" /> <img src='${pageContext.request.contextPath}/img/mapicons/Environment_Smart_waste_container.png' height='23' width='20' align='top'/> <span class="Environment macrocategory-label">Smart_waste_container</span>
                                <br/>
                                <input type="checkbox" name="near-smart-irrigator" value="Smart_irrigator"  class="macrocategory" id="Environment" /> <img src='${pageContext.request.contextPath}/img/mapicons/Environment_Smart_irrigator.png' height='23' width='20' align='top'/> <span class="Environment macrocategory-label">Smart_irrigator</span>
                                <br/>
                                <input type="checkbox" name="near-weather-sensor" value="Weather_sensor"  class="macrocategory" id="Environment" /> <img src='${pageContext.request.contextPath}/img/mapicons/Environment_Weather_sensor.png' height='23' width='20' align='top'/> <span class="Environment macrocategory-label">Weather_sensor</span>
                                <br/>
                                <input type="checkbox" name="near-noise_level_sensor" value="Noise_level_sensor"  class="macrocategory" id="Environment" /> <img src='${pageContext.request.contextPath}/img/mapicons/Environment_Noise_level_sensor.png' height='23' width='20' align='top'/> <span class="Environment macrocategory-label">Noise_level_sensor</span>
                                <br/>
                                <input type="checkbox" name="near-people_counter" value="People_counter"  class="macrocategory" id="Environment" /> <img src='${pageContext.request.contextPath}/img/mapicons/Environment_People_counter.png' height='23' width='20' align='top'/> <span class="Environment macrocategory-label">People_counter</span>
                                <br/>
                                <input type="checkbox" name="near-smart-bench" value="Smart_bench"  class="macrocategory" id="Entertainment" /> <img src='${pageContext.request.contextPath}/img/mapicons/Entertainment_Smart_bench.png' height='23' width='20' align='top'/> <span class="Entertainment macrocategory-label">Smart_bench</span>
                                <br/>
                                <input type="checkbox" name="near-first-aid" value="First_aid"  class="macrocategory" id="Emergency" /> <img src='${pageContext.request.contextPath}/img/mapicons/Emergency_First_aid.png' height='23' width='20' align='top'/> <span class="Emergency macrocategory-label">First_aid</span>
                                <br/>
                                <input type="checkbox" name="near-police" value="Police_headquarters "  class="macrocategory" id="GovernmentOffice" /> <img src='${pageContext.request.contextPath}/img/mapicons/GovernmentOffice_Police_headquarters.png' height='23' width='20' align='top'/> <span class="GovernmentOffice  macrocategory-label">Police_headquarters</span>
                            </div>                            
                            <br />
                            <span name="lbl" caption="Filter_Results_dx_T">Filter</span>:
                            <br>
                            <input type="text" name="serviceTextFilter_t" id="serviceTextFilter_t" placeholder="search text into service" onkeypress="event.keyCode == 13 ? ricercaServizi('categorie_t', null, null) : false"/><br />
                            <span name="lbl" caption="Num_Results_dx_T">N. results for each</span>:
                            <select id="nResultsServizi_t" name="nResultsServizi">
                                <option value="10">10</option>
                                <option value="20">20</option>
                                <option value="50">50</option>
                                <option value="100" selected="selected">100</option>
                                <option value="200">200</option>
                                <option value="500">500</option>
                                <option value="0">No Limit</option>
                            </select>
                            <br />
                            <hr />
                            <span name="lbl" caption="Search_Range_T">Search Range</span>
                            <select id="raggioricerca_t" name="raggioricerca">
                                <option value="0.1">100 mt</option>
                                <option value="0.2">200 mt</option>
                                <option value="0.3">300 mt</option>
                                <option value="0.5">500 mt</option>
                                <option value="1">1 km</option>
                                <option value="2">2 km</option>
                                <option value="5">5 km</option>
                                <option value="area" selected="selected">visible areas</option>
                            </select>
                            <hr />
                            <div class="menu" id="serviceSearch">
                                <img src="${pageContext.request.contextPath}/img/search_icon.png" alt="Search Services" width="24" onclick="ricercaServizi('categorie_t', null, null);" />
                            </div>
                            <!--<div class="menu" id="textSearch">
                                <img src="${pageContext.request.contextPath}/img/text_search.jpg" alt="Text Search" width="28" onclick="showTextSearchDialog();" />
                            </div>-->
                            <div class="menu" id="clearAll">
                                <img src="${pageContext.request.contextPath}/img/clear_icon.png" alt="Clear all" width="28" onclick="resetTotale();" />
                            </div>
                            <!--<input type="checkbox" name="open_path" value="open_path" id="apri_path" />  <span>Open Path/Area</span>-->
                            <!-- DA DECOMMENTARE QUANDO SARA' FATTO IL SALVATAGGIO DEI SERVIZI TRASVERSALI -->
                            <div class="menu" id="saveQuery">
                              <img src="${pageContext.request.contextPath}/img/save.png" alt="Salva la query" width="28" onclick="save_handler(null, null, null, null, 'query_t');" />
                            </div>
                            <br />

                        </div>
                    </div>
                    <div id="tree">
                    </div>
                </div>
                <fieldset id="searchOutput"> 
                    <legend><span name="lbl" caption="Search_Results">Search Results</span></legend>
                    <div class="result" id="cluster-msg"></div>
                    <div class="result" id="msg"></div>
                    <div class="result" id="serviceRes"><img src='${pageContext.request.contextPath}/img/mapicons/TourismService.png' height='21' width='18' align='top'/><span class="label">Services:</span><div class="value"></div></div>
                    <div class="result" id="busstopRes"><img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_BusStop.png' height='21' width='18' align='top'/><span class="label">Bus Stops:</span><div class="value"></div></div>
                    <div class="result" id="busDirection"><span class="label">Direction:</span><div class="value"></div></div>
                    <div class="result" id="sensorRes"><img src='${pageContext.request.contextPath}/img/mapicons/TransferServiceAndRenting_SensorSite.png' height='21' width='18' align='top'/><span class="label">Road Sensors:</span><div class="value"></div></div>
                </fieldset>
                <fieldset id="resultTPL"> 
                    <legend><span name="lbl" caption="Results_BusLines">Bus Lines</span></legend>
                    <div id="numTPL" style="display:none;"></div>
                    <div id="listTPL"></div>
                </fieldset>

            </div>
        </div>
        <div id="info-aggiuntive" class="menu">
            <div class="header"><span name="lbl" caption="Hide_Menu_meteo"> - Hide Menu</span>
            </div>
            <div class="content"></div>
        </div>
        <script>
                                    // $("#embed").hide();
                                    var ctx = "${pageContext.request.contextPath}";
                                    //var ctx = "http://servicemap.disit.org/WebAppGrafo";
                                    var query = new Object();
                                    //var query_event = new Object();//michela
                                    var parentQuery = "";
                                    var listOfPopUpOpen = [];
                                    var pins = "";
                                    var save_operation = "";
                                    var user_mail = "";
                                    var queryTitle = "";
                                    var description = "";
                                    var currentServiceUri = ""
                                    var currentIdConf = null;
                                    var lastEmail = "";
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
                                    function parseUrlQuery(queryString) {
                                        var params = {}, queries, temp, i, l;
                                        // Split into key/value pairs
                                        queries = queryString.split("&");
                                        // Convert the array of strings into an object
                                        for (i = 0, l = queries.length; i < l; i++) {
                                            temp = queries[i].split('=');
                                            params[temp[0]] = temp[1];
                                        }
                                        return params;
                                    }
                                    function include(arr, obj) {
                                        return (arr.indexOf(obj) != -1);
                                    }
                                    function saveQueryFreeText(text, limit) {
                                        var lastQuery = new Object();
                                        lastQuery["text"] = text;
                                        lastQuery["limit"] = limit;
                                        lastQuery["type"] = "freeText";
                                        return lastQuery;
                                    }
                                    function searchText() {
                                        nascondiRisultati();
                                        var numeroEventi = 0;
                                        var text = $("#freeSearch").val();
                                        var textEv = $("#freeSearch").val();
                                        var limit = $("#numberResults").val();
                                        var numService = 0;
                                        var numBusstop = 0;
                                        var numSensor = 0;
                                        var numTotRes = 0;
                                        text = escape(text);
                                        $('#loading').show();
                                        svuotaLayers();
                                        query = saveQueryFreeText(text, limit);
                                        numeroEventi = searchEvent("free_text", null, null, limit, textEv);
                                        if (numeroEventi != 0) {
                                            //risultatiRicerca((numService+numeroEventi), 0, 0, 1);
                                            numTotRes = numTotRes + numeroEventi;
                                            if(limit!=0)
                                              limit = (limit - numeroEventi);
                                            $("input[name=event_choice][value=day]").attr('checked', 'checked');
                                        }
                                        $.ajax({
                                            data: {
                                                search: text,
                                                limit: limit
                                            },
                                            url: ctx + "/ajax/json/free-text-search.jsp",
                                            //url: ctx + "/ajax/json/free-text-search_pro.jsp",
                                            type: "GET",
                                            dataType: 'json',
                                            async: true,
                                            success: function (data) {
                                                if (data.features.length > 0) {
                                                    numTotRes = numTotRes + data.fullCount;
                                                    servicesLayer = L.geoJson(data, {
                                                        pointToLayer: function (feature, latlng) {
                                                            marker = showmarker(feature, latlng);
                                                            return marker;
                                                        },
                                                        onEachFeature: function (feature, layer) {
                                                            popupContent = "";
                                                            var divId = feature.id + "-" + feature.properties.tipo;
                                                            if (feature.properties.typeLabel == "Strada") {
                                                                popupContent = popupContent + "<h3>" + feature.properties.name + " n. " + feature.properties.civic + "</h3>";
                                                            }
                                                            popupContent = popupContent + "<div id=\"" + divId + "\" ></div>";
                                                            layer.bindPopup(popupContent);
                                                            numService++;
                                                        }
                                                    });
            <%if (clusterResults > 0) {%>
                                                    if (data.features.length >=<%=clusterResults%>) {
                                                        markers = new L.MarkerClusterGroup({maxClusterRadius: <%=clusterDistance%>, disableClusteringAtZoom: <%=noClusterAtZoom%>});
                                                        servicesLayer = markers.addLayer(servicesLayer);
                                                        //$("#cluster-msg").text("più di <%=clusterResults%> risultati, attivato clustering");
                                                        $("#cluster-msg").text("more than <%=clusterResults%> results, clustering enable");
                                                        $("#cluster-msg").show();
                                                    }
                                                    else
                                                        $("#cluster-msg").hide();
            <%}%>
                                                    servicesLayer.addTo(map);
                                                    var confiniMappa = servicesLayer.getBounds();
                                                    map.fitBounds(confiniMappa, {padding: [50, 50]});
                                                    $('#loading').hide();
                                                    risultatiRicerca(numService + numeroEventi, 0, 0, 1, null, numTotRes, 0, 0);
                                                }
                                                else {
                                                    $('#loading').hide();
                                                    if (numeroEventi == 0) {
                                                        risultatiRicerca(0, 0, 0, 0, null, 0, 0, 0);
                                                    } else {
                                                        risultatiRicerca(numeroEventi, 0, 0, 1, null, 0, 0, 0);
                                                    }
                                                }
                                            },
                                            error: function (request, status, error) {
                                                $('#loading').hide();
                                                console.log(error);
                                                alert('Error in searching ' + decodeURIComponent(text) + "\n The error is " + error + " " + status);
                                            }
                                        });
                                    }

                                    function showResults(parameters) {
                                        if (mode != "embed") {//caso in cui il link che sto visualizando è del tipo: http://.../ServiceMap/api/v1?queryId=...' o 
                                            var queryId = parameters["queryId"];
                                            if (queryId != null) {
                                                $.ajax({
                                                    data: {
                                                        queryId: queryId
                                                    },
                                                    url: ctx + "/api/query.jsp",
                                                    type: "GET",
                                                    async: true,
                                                    dataType: 'json',
                                                    success: function (msg) {
                                                        /*     if(msg.idService != "null" && msg.idService != ""){
                                                         showService(msg.idService,msg.typeService);
                                                         }*/
                                                        queryTitle = msg.title;
                                                        description = msg.description;
                                                        user_mail = msg.email;
                                                        var htmlQueryBox = "<hr><h2>" + queryTitle + "</h2><p>" + description + "</p>";
                                                        $("#queryBox").append(htmlQueryBox);
                                                        if (!msg.isReadOnly)
                                                            save_operation = "write";
                                                        var typeSaving = msg.typeSaving;
                                                        if (typeSaving == "service") {
                                                            showService(unescape(msg.idService));
                                                        } else if (typeSaving == "freeText") {
                                                            $("#freeSearch").val(msg.text);
                                                            $("#numberResults").val(msg.numeroRisultatiServizi);
                                                            showResultsFreeSearch(msg.text, msg.numeroRisultatiServizi);
                                                        }else if(typeSaving == "event"){
                                                            //searchEvent(param, raggioRic, centroRic, numEv, text) 
                                                            searchEvent(msg.actualSelection, null, null, "0", null);
                                                        }
                                                        else if (typeSaving == "weather"){
                                                                //fammi vedere il meteo
                                                                $.ajax({
                                                                    url: "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                                                    type: "GET",
                                                                    async: true,
                                                                    data: {
                                                                        nomeComune: msg.weatherCity
                                                                    },
                                                                    success: function (msg) {
                                                                        parameters["info"]='wheather';
                                                                        $('#info-aggiuntive .content').html(msg);
                                                                    }
                                                                });
                                                            } 
                                                        else {//caso di embed
                                                            parentQuery = msg.parentQuery;
                                                            if (msg.nomeProvincia != "") {
                                                                $("#elencoprovince").val(msg.nomeProvincia);
                                                                mostraElencoComuni(msg.nomeProvincia, msg.nomeComune);
                                                            }
                                                            if (msg.line != "" && msg.line != null && msg.line != "null") {
                                                                mostraElencoAgenzie();
                                                                //getBusLines("query");
                                                                $("#elencolinee").val(msg.line);
                                                                mostraElencoFermate(msg.line, msg.stop);
                                                            }
                                                            if((msg.actualSelection=="day") || (msg.actualSelection=="week") ||
                                                                    (msg.actualSelection=="mounth") ){
                                                                searchEvent(msg.actualSelection, "", msg.center, "0", null);
                                                            }
                                                            var categorie = msg.categorie;
                                                            $("#categorie :not(:checked)").each(function () {
                                                                var category = $(this).val();
                                                                if (categorie.indexOf(category) > -1)
                                                                    $(this).attr("checked", true);
                                                            });
                                                            if (categorie.indexOf("NearBusStops") > -1)
                                                                $("#Bus").attr("checked", true);
                                                            if (categorie.indexOf("RoadSensor") > -1)
                                                                $("#Sensor").attr("checked", true);
                                                            var categorieArray = categorie.split(",");
                                                            var stringaCategorie = categorieArray.join(";");
                                                            stringaCategorie = stringaCategorie.replace("[", "");
                                                            stringaCategorie = stringaCategorie.replace("]", "");
                                                            stringaCategorie = stringaCategorie.replace(/\s+/g, '');
                                                            $("#raggioricerca").val(msg.raggioServizi);
                                                            $("#nResultsServizi").val(msg.numeroRisultatiServizi);
                                                            $("#nResultsSensori").val(msg.numeroRisultatiSensori);
                                                            $("#nResultsBus").val(msg.numeroRisultatiBus);
                                                            var text = msg.text;
                                                            $("#serviceTextFilter").val(text);
                                                            $('#selezione').html(msg.actualSelection);
                                                            selezione = unescape(msg.actualSelection);
                                                           
                                                            
                                                            if (typeSaving == "embed" && selezione.indexOf("COMUNE di") != -1)
                                                                coordinateSelezione = null;
                                                            else
                                                                coordinateSelezione = unescape(msg.coordinateSelezione);
                                                            //  if(msg.idService == "null" || msg.idService == "" || msg.idService== null)
                                                            if (stringaCategorie != "null")
                                                                mostraServiziAJAX_new(stringaCategorie, msg.actualSelection, coordinateSelezione, msg.nomeComune, msg.numeroRisultatiServizi, msg.numeroRisultatiSensori, msg.numeroRisultatiBus, msg.raggioServizi, msg.raggioSensori, msg.raggioBus, null, text, "categorie");
                                                            if(msg.weatherCity!=''){
                                                                $.ajax({
                                                                    url: "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                                                    type: "GET",
                                                                    async: true,
                                                                    data: {
                                                                        nomeComune: msg.weatherCity
                                                                    },
                                                                    success: function (msg) {
                                                                        parameters["info"]='wheather';
                                                                        $('#info-aggiuntive .content').html(msg);
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    }
                                                });
                                            } else if (parameters["selection"] != null) {
                                                var selection = unescape(parameters["selection"]);
                                                if (selection == "undefined")
                                                    selection = "";
                                                var categorie = unescape(parameters["categories"]);
                                                if (categorie == "undefined")
                                                    categorie = "Service;BusStop;SensorSite";
                                                var text = unescape(parameters["text"]);
                                                if (text == "undefined")
                                                    text = "";
                                                var risultati = unescape(parameters["maxResults"]);
                                                if (risultati == "undefined" || risultati == "")
                                                    risultati = "100";
                                                var arrayRisultati = risultati.split(";");
                                                var risultatiServizi = arrayRisultati[0];
                                                var risultatiSensori = (arrayRisultati.length >= 2 ? arrayRisultati[1] : risultatiServizi);
                                                var risultatiBus = (arrayRisultati.length >= 3 ? arrayRisultati[2] : risultatiSensori);
                                                var raggi = unescape(parameters["maxDists"]);
                                                if (raggi == "undefined" || raggi == "")
                                                    raggi = "0.1";
                                                var arrayRaggi = raggi.split(";");
                                                var raggioServizi = arrayRaggi[0];
                                                var raggioSensori = (arrayRaggi.length >= 2 ? arrayRaggi[1] : raggioServizi);
                                                var raggioBus = (arrayRaggi.length >= 3 ? arrayRaggi[2] : raggioSensori);
                                                if (selection.toLowerCase().indexOf("comune di") != -1 || selection == "") {
                                                    var nomeComune = selection.substring(selection.indexOf("COMUNE di") + 10);
                                                    mostraServiziAJAX_new(categorie, selection, coordSel, nomeComune, risultatiServizi, risultatiSensori, risultatiBus, raggioServizi, raggioSensori, raggioBus, null, text, "categorie");
                                                } else {
                                                    if (selection.indexOf("http://") != -1) {
                                                        var coordSel = "";
                                                        $.ajax({
                                                            data: {
                                                                serviceUri: selection
                                                            },
                                                            url: ctx + "/ajax/getCoordinates.jsp",
                                                            type: "GET",
                                                            dataType: 'json',
                                                            async: true,
                                                            success: function (msg) {
                                                                coordSel = msg.latitudine + ";" + msg.longitudine;
                                                                selection = "point";
                                                                mostraServiziAJAX_new(categorie, selection, coordSel, nomeComune, risultatiServizi, risultatiSensori, risultatiBus, raggioServizi, raggioSensori, raggioBus, null, text, "categorie");
                                                            }
                                                        });
                                                    }
                                                    else {
                                                        var coordSel = selection;
                                                        selection = "point";
                                                        mostraServiziAJAX_new(categorie, selection, coordSel, nomeComune, risultatiServizi, risultatiSensori, risultatiBus, raggioServizi, raggioSensori, raggioBus, null, text, "categorie");
                                                    }
                                                }
                                            } else if (parameters["search"] != null) {
                                                var textToSearch = unescape(parameters["search"]);
                                                var limit = parameters["maxResults"];
                                                if (limit == undefined)
                                                    limit = parameters["limit"];
                                                if (limit == undefined)
                                                    limit = "100";
                                                showResultsFreeSearch(textToSearch, limit);
                                            } else if (parameters["serviceUri"] != null) {
                                                var idServices = parameters["serviceUri"].split(";");
                                                //loadServiceInfo(idService)
                                                for (var i = 0; i < idServices.length; i++)
                                                    showService(idServices[i]);
                                            }
                                            else if (api=="shortestpath") {
                                              pathStart = parameters["source"];
                                              pathEnd = parameters["destination"];
                                              routeType = parameters["routeType"];
                                              startDatetime = parameters["startDatetime"];
                                              $("#path_start").html("From: "+pathStart);
                                              $("#path_end").html("To: "+pathEnd);
                                              if(routeType)
                                                $("#path_type").val(routeType);
                                              if(startDatetime) {
                                                var d=startDatetime.split("T");
                                                if(d.length==2) {
                                                  $("#path_date").val(d[0]);
                                                  $("#path_time").val(d[1]);
                                                }
                                              }
                                              $("#path").show();
                                              doSearchPath(false,true); //fit
                                            }
                                            else {
                                                alert("invalid API call");
                                            }
                                        } else { //mode=="embed" // 'http://.../ServiceMap/api/embed/?idConf=....
                                            var idConf = parameters["idConf"]; // SO che sono in embed e uso di relativo all'embed
                                            if (idConf != null) {
                                                $.ajax({
                                                    data: {
                                                        idConf: idConf
                                                    },
                                                    url: ctx + "/api/embed/configuration.jsp",
                                                    type: "GET",
                                                    async: true,
                                                    dataType: 'json',
                                                    success: function (msg) {
                                                        if (msg.weatherCity != "null" && msg.weatherCity != "") {
                                                            $.ajax({
                                                                url: "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                                                type: "GET",
                                                                async: true,
                                                                data: {
                                                                    nomeComune: msg.weatherCity
                                                                },
                                                                success: function (msg) {
                                                                    $('#info-aggiuntive .content').html(msg);
                                                                }
                                                            });
                                                        }
                                                        queryTitle = msg.title;
                                                        description = msg.description;
                                                        user_mail = msg.email;
                                                        var textToSearch = msg.text;
                                                        var c = unescape(msg.center);
                                                        var center = JSON.parse(c);
                                                        map.setView(new L.LatLng(center.lat, center.lng), msg.zoom);
                                                        var htmlQueryBox = "<hr><h2>" + queryTitle + "</h2><p>" + description + "</p>";
                                                        $("#queryBox").append(htmlQueryBox);
                                                        if (msg.nomeProvincia != "" && msg.nomeProvincia != "null") {
                                                            $("#elencoprovince").val(msg.nomeProvincia);
                                                            mostraElencoComuni(msg.nomeProvincia, msg.nomeComune);
                                                            $("#tabs").tabs("option", "active", 1);
                                                        }
                                                        if (msg.line != null && msg.line != "" && msg.line != "null") {
                                                            mostraElencoAgenzie();
                                                            //getBusLines("query");
                                                            $("#tabs").tabs("option", "active", 0);
                                                            $("#elencolinee").val(msg.line);
                                                            mostraElencoFermate(msg.line, msg.stop);
                                                        }
                                                        var categorie = msg.categorie;
                                                        $("#categorie :not(:checked)").each(function () {
                                                            var category = $(this).val();
                                                            if (categorie.indexOf(category) > -1)
                                                                $(this).attr("checked", true);
                                                        });
                                                        if (categorie.indexOf("NearBusStops") > -1)
                                                            $("#Bus").attr("checked", true);
                                                        if (categorie.indexOf("RoadSensor") > -1)
                                                            $("#Sensor").attr("checked", true);
                                                        $("#raggioricerca").val(msg.raggioServizi);
                                                        $("#nResultsServizi").val(msg.numeroRisultatiServizi);
                                                        $("#nResultsSensori").val(msg.numeroRisultatiSensori);
                                                        $("#nResultsBus").val(msg.numeroRisultatiBus);
                                                        $('#selezione').html(msg.actualSelection);
                                                        selezione = unescape(msg.actualSelection);
                                                        var openPins = msg.popupOpen;
                                                        if (textToSearch != "" && textToSearch != null) {
                                                            var nRes = msg.numeroRisultatiServizi;
                                                            if ((msg.actualSelection == "null" || msg.actualSelection == "") && (msg.categorie == "null" || msg.categorie == "") && (msg.coordinateSelezione == "null" || msg.coordinateSelezione == "")) {
                                                                showResultsFreeSearch(textToSearch, nRes);
                                                            }
                                                            else {
                                                                var range = msg.raggioServizi;
                                                                stringaCategorie = msg.categorie;
                                                                stringaCategorie = unescape(stringaCategorie);
                                                                if (msg.actualSelection.indexOf("COMUNE di") != -1) {
                                                                    var selection = msg.nomeComune;
                                                                    var startingPoint = "municipality";
                                                                }
                                                                else {
                                                                    var selection = msg.coordinateSelezione;
                                                                    var startingPoint = "point";
                                                                }
                                                                $("#loading").show();
                                                                svuotaLayers();
                                                                $.ajax({
                                                                    data: {
                                                                        search: textToSearch,
                                                                        results: nRes,
                                                                        range: range,
                                                                        selection: selection,
                                                                        startingPoint: startingPoint,
                                                                        categorie: stringaCategorie
                                                                    },
                                                                    url: ctx + "/ajax/json/get-services-by-text.jsp",
                                                                    type: "GET",
                                                                    dataType: 'json',
                                                                    async: true,
                                                                    success: function (data) {
                                                                        if (data.features.length > 0) {
                                                                            servicesLayer = L.geoJson(data, {
                                                                                pointToLayer: function (feature, latlng) {
                                                                                    marker = showmarker(feature, latlng);
                                                                                    return marker;
                                                                                },
                                                                                onEachFeature: function (feature, layer) {
                                                                                    popupContent = "";
                                                                                    var divId = feature.id + "-" + feature.properties.tipo;
                                                                                    popupContent = popupContent + "<div id=\"" + divId + "\" ></div>";
                                                                                    layer.bindPopup(popupContent);
                                                                                }
                                                                            }).addTo(map);
                                                                            if (mode == "embed") {
                                                                                for (i in servicesLayer._layers) {
                                                                                    var uri = servicesLayer._layers[i].feature.properties.serviceUri;
                                                                                    if (include(openPins, uri))
                                                                                        servicesLayer._layers[i].openPopup();
                                                                                }
                                                                            }
                                                                            var confiniMappa = servicesLayer.getBounds();
                                                                            map.fitBounds(confiniMappa, {padding: [50, 50]});
                                                                            $('#loading').hide();
                                                                        }
                                                                        else {
                                                                            $('#loading').hide();
                                                                            risultatiRicerca(0, 0, 0, 1, null, 0, 0, 0);
                                                                        }
                                                                    },
                                                                    error: function (request, status, error) {
                                                                        $('#loading').hide();
                                                                        console.log(error);
                                                                        alert('Error in searching ' + text + "\n The error is " + error);
                                                                    }
                                                                });
                                                            }
                                                        }
                                                        else if (categorie != "null") { //no text search
                                                            var categorieArray = categorie.split(",");
                                                            var stringaCategorie = categorieArray.join(";");
                                                            stringaCategorie = stringaCategorie.replace("[", "");
                                                            stringaCategorie = stringaCategorie.replace("]", "");
                                                            stringaCategorie = stringaCategorie.replace(/\s+/g, '');
                                                            coordinateSelezione = unescape(msg.coordinateSelezione);
                                                            //$("#embed").show();
                                                            mostraServiziAJAX_new(stringaCategorie, msg.actualSelection, coordinateSelezione, msg.nomeComune, msg.numeroRisultatiServizi, msg.numeroRisultatiSensori, msg.numeroRisultatiBus, msg.raggioServizi, msg.raggioSensori, msg.raggioBus, openPins, null, "categorie");
                                                        }
                                                        else {
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }
                                    function showResultsFreeSearch(textToSearch, limit) {
                                        textToSearch = escape(textToSearch);
                                        $('#loading').show();
                                        $.ajax({
                                            data: {
                                                search: textToSearch,
                                                limit: limit
                                            },
                                            url: ctx + "/ajax/json/free-text-search.jsp",
                                            type: "GET",
                                            dataType: 'json',
                                            async: true,
                                            success: function (data) {
                                                if (data.features.length > 0) {
                                                    servicesLayer = L.geoJson(data, {
                                                        pointToLayer: function (feature, latlng) {
                                                            marker = showmarker(feature, latlng);
                                                            return marker;
                                                        },
                                                        onEachFeature: function (feature, layer) {
                                                            popupContent = "";
                                                            var divId = feature.id + "-" + feature.properties.tipo;
                                                            popupContent = popupContent + "<div id=\"" + divId + "\" ></div>";
                                                            layer.bindPopup(popupContent);
                                                        }
                                                    }).addTo(map);
                                                    if (mode == "embed") {
                                                        for (i in servicesLayer._layers) {
                                                            var uri = servicesLayer._layers[i].feature.properties.serviceUri;
                                                            if (include(openPins, uri))
                                                                servicesLayer._layers[i].openPopup();
                                                        }
                                                    }
                                                    var confiniMappa = servicesLayer.getBounds();
                                                    map.fitBounds(confiniMappa, {padding: [50, 50]});
                                                    $('#loading').hide();
                                                }
                                                else {
                                                    $('#loading').hide();
                                                    risultatiRicerca(0, 0, 0, 1, null, 0, 0, 0);
                                                }
                                            },
                                            error: function (request, status, error) {
                                                $('#loading').hide();
                                                console.log(error);
                                                alert('Error in searching ' + text + "\n The error is " + error);
                                            }
                                        });
                                    }

                                    function showService(serviceUri) {
                                        $('#loading').show();
                                        $.ajax({
                                            data: {
                                                serviceUri: serviceUri
                                            },
                                            url: ctx + "/api/service.jsp",
                                            type: "GET",
                                            async: true,
                                            dataType: 'json',
                                            success: function (msg) {
                                                if("Service" in msg) {
                                                  msg = msg.Service;
                                                }
                                                if("Sensor" in msg) {
                                                  msg = msg.Sensor;
                                                }
                                                if (msg != null && msg.meteo != null) {
                                                    $.ajax({
                                                        url: "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                                        type: "GET",
                                                        async: true,
                                                        data: {
                                                            nomeComune: msg.meteo.location
                                                        },
                                                        success: function (msgMeteo) {
                                                            $('#info-aggiuntive .content').html(msgMeteo);
                                                            $('#loading').hide();
                                                        }
                                                    });
                                                } else if (msg != null && msg.features.length > 0 && msg.features[0].geometry.coordinates != undefined) {
                                                    var longService = msg.features[0].geometry.coordinates[0];
                                                    var latService = msg.features[0].geometry.coordinates[1];
                                                    servicesLayer = L.geoJson(msg, {
                                                        pointToLayer: function (feature, latlng) {
                                                            marker = showmarker(feature, latlng);
                                                            return marker;
                                                        },
                                                        onEachFeature: function (feature, layer) {
                                                            var tipo = feature.properties.tipo;
                                                            if(tipo==undefined)
                                                              tipo = feature.properties.serviceType;
                                                            var divMM = feature.id + "-multimedia";
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
                                                                    } else {
                                                                        htmlDiv = "<div id=\"" + divMM + "\"class=\"multimedia\"><img src=\"" + multimediaResource + "\" width=\"80\" height=\"80\"></div>";
                                                                    }
                                                                }
                                                            }
                                                            if (include(tipo, "@en"))
                                                                tipo = tipo.replace("@en", "");
                                                            else
                                                                tipo = tipo.replace("@it", "");
                                                            var divId = feature.id + "-" + tipo;
                                                            if (feature.properties.tipo != "fermata") {
                                                                contenutoPopup = createContenutoPopup(feature, divId, feature.id);
                                                                layer.addTo(map).bindPopup(contenutoPopup).openPopup();
                                                                if (feature.multimedia != null) {
                                                                    //$(".leaflet-popup-content-wrapper").css("width", "300px"); 
                                                                    $('#' + divId).closest('div.leaflet-popup-content-wrapper').css("width", "300px");
                                                                }
                                                                if(feature.realtime!=undefined) {
                                                                  mostraRealTimeData(divId+"-info", feature.realtime);
                                                                  //popup_fixpos(div);
                                                                }

                                                                popup_fixpos(divId);
                                                            }
                                                            else {
                                                                var contenutoPopup = createContenutoPopup(feature, divId, feature.id);
                                                                layer.addTo(map).bindPopup(contenutoPopup).openPopup();
                                                            }
                                                        }
                                                    });
                                                    map.setView(new L.LatLng(latService, longService), 16);
                                                }
                                                else {
                                                    alert("no info found for service " + serviceUri);
                                                }
                                                $('#loading').hide();
                                            }
                                        });
                                    }

                                    function showEmbedConfiguration(parameters) {
                                        var idConfiguration = parameters['idConf'];
                                        //var scale=parameters['scale'];
                                        //var translate=parameters['translate'];
                                        $.ajax({
                                            data: {
                                                idConfiguration: idConfiguration,
                                                scale: scale,
                                                translate: translate
                                            },
                                            url: "api/embed/configuration.jsp",
                                            type: "GET",
                                            async: true,
                                            dataType: 'json',
                                            success: function (msg) {
                                                if (msg.nomeProvincia != null) {
                                                    $("#elencoprovince").val(msg.nomeProvincia);
                                                    mostraElencoComuni(msg.nomeProvincia, msg.nomeComune);
                                                    $("#ui-id-2").click();
                                                }
                                                if (msg.line != null) {
                                                    //getBusLines("embed");
                                                    mostraElencoAgenzie();
                                                    $("#elencolinee").val(msg.line);
                                                    mostraElencoFermate(msg.line, msg.stop);
                                                    $("#ui-id-1").click();
                                                }
                                                if (msg.pins)
                                                    var pins = JSON.parse(msg.pins);
                                                if (msg.popupOpen)
                                                    var openPopup = JSON.parse(msg.popupOpen);
                                                $("#raggioricerca").val(msg.radius);
                                                $("#numerorisultati").val(msg.numeroRisultati);
                                                $('#selezione').html(msg.actualSelection);
                                                var center = JSON.parse(msg.center);
                                                map.setView(new L.LatLng(center.lat, center.lng), msg.zoom);
                                                var openPins = [];
                                                for (var i = 0; i < openPopup.length; i++) {
                                                    openPins.push(openPopup[i].id);
                                                }
                                                var weatherCity = msg.weatherCity;
                                                if (weatherCity) {
                                                    $.ajax({
                                                        url: "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                                        type: "GET",
                                                        async: true,
                                                        data: {
                                                            nomeComune: weatherCity
                                                        },
                                                        success: function (msg) {
                                                            $('#info-aggiuntive .content').html(msg);
                                                        }
                                                    });
                                                }
                                                var categorie = msg.categorie;
                                                //categorie=categorie.substr(1,categorie.length-1);
                                                $("#categorie :not(:checked)").each(function () {
                                                    var category = $(this).val();
                                                    if (categorie.indexOf(category) > -1)
                                                        $(this).attr("checked", true);
                                                });
                                                servicesLayer = L.geoJson(pins, {
                                                    pointToLayer: function (feature, latlng) {
                                                        marker = showmarker(feature, latlng);
                                                        return marker;
                                                    },
                                                    onEachFeature: function (feature, layer) {

                                                        var divId = feature.id + "-" + feature.properties.tipo;
                                                        if (feature.properties.tipo != "fermata") {
                                                            contenutoPopup = "<h3>" + feature.properties.name + "</h3>";
                                                            contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + feature.properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
                                                            contenutoPopup = contenutoPopup + "Tipologia: " + feature.properties.tipo + "<br />";
                                                            if (feature.properties.email != "" && feature.properties.email)
                                                                contenutoPopup = contenutoPopup + "Email: " + feature.properties.email + "<br />";
                                                            if (feature.properties.indirizzo != "")
                                                                contenutoPopup = contenutoPopup + "Indirizzo: " + feature.properties.indirizzo;
                                                            if (feature.properties.numero != "" && feature.properties.numero)
                                                                contenutoPopup = contenutoPopup + ", " + feature.properties.numero + "<br />";
                                                            else
                                                                contenutoPopup = contenutoPopup + "<br />";
                                                            if (feature.properties.note != "" && feature.properties.note)
                                                                contenutoPopup = contenutoPopup + "Note: " + feature.properties.note + "<br />";
                                                            contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                                            if (include(openPins, feature.id))
                                                                layer.addTo(map).bindPopup(contenutoPopup).openPopup();
                                                            else
                                                                layer.addTo(map).bindPopup(contenutoPopup);
                                                        }
                                                        else {
                                                            var divLinee = divId + "-linee";
                                                            var contenutoPopup = "<h3>FERMATA : " + feature.properties.popupContent + "</h3>";
                                                            contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + feature.properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
                                                            contenutoPopup += "<div id=\"" + divLinee + "\" ></div>";
                                                            contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                                            if (include(openPins, feature.id))
                                                                layer.addTo(map).bindPopup(contenutoPopup).openPopup();
                                                            else
                                                                layer.addTo(map).bindPopup(contenutoPopup);
                                                        }
                                                    }
                                                });
                                                $("#loading").hide();
                                            }
                                        });
                                    }

                                    /***  codice per mantenere aperto più di un popup per volta ***/
                                    L.Map = L.Map.extend({
                                        openPopup: function (popup) {
                                            //        this.closePopup();  // just comment this
                                            this._popup = popup;
                                            return this.addLayer(popup);
                                        }

                                    });
                                    // CREAZIONE MAPPA CENTRATA NEL PUNTO
                                    //commentato marco
                                    //var map = L.map('map').setView([43.3555664, 11.0290384], 8);

                                    // SCELTA DEL TILE LAYER ED IMPOSTAZIONE DEI PARAMETRI DI DEFAULT
                                    /*commentato marco
                                     L.tileLayer('http://c.tiles.mapbox.com/v3/examples.map-szwdot65/{z}/{x}/{y}.png', { // NON MALE
                                     //L.tileLayer('http://{s}.tile.cloudmade.com/{key}/22677/256/{z}/{x}/{y}.png', {
                                     //L.tileLayer('http://a.www.toolserver.org/tiles/bw-mapnik/{z}/{x}/{y}.png', {
                                     //L.tileLayer('http://a.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                     //L.tileLayer('http://tilesworld1.waze.com/tiles/{z}/{x}/{y}.png', {
                                     //		L.tileLayer('http://maps.yimg.com/hx/tl?v=4.4&x={x}&y={y}&z={z}', {
                                     //L.tileLayer('http://a.tiles.mapbox.com/v3/examples.map-bestlap85.h67h4hc2/{z}/{x}/{y}.png', { MAPBOX MA NON FUNZIA
                                     //L.tileLayer('http://{s}.tile.cloudmade.com/1a1b06b230af4efdbb989ea99e9841af/998/256/{z}/{x}/{y}.png', { 
                                     //	L.tileLayer('http://{s}.tile.cloudmade.com/1a1b06b230af4efdbb989ea99e9841af/121900/256/{z}/{x}/{y}.png', { 
                                     attribution: 'Map data &copy; 2011 OpenStreetMap contributors, Imagery &copy; 2012 CloudMade',
                                     key: 'BC9A493B41014CAABB98F0471D759707',
                                     minZoom: 8
                                     }).addTo(map);
                                     
                                     *fine commento marco
                                     */

                                    //codice per gestione layers
                                    //var osm = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'});
                                    //http://otile1.mqcdn.com/tiles/1.0.0/sat/{z}/{x}/{y}.jpg
                                    //http://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png
                                    //http://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png
                                    //http://a.tile.thunderforest.com/outdoors/{z}/{x}/{y}.png
                                    //http://a.tile.stamen.com/toner/{z}/{x}/{y}.png
                                    //http://a.tile.thunderforest.com/landscape/{z}/{x}/{y}.png
                                    //var osm = L.tileLayer('http://a.tile.thunderforest.com/landscape/{z}/{x}/{y}.png', {attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'});

                                    var mbAttr = 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
                                            '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
                                            'Imagery © <a href="http://mapbox.com">Mapbox</a>',
                                            mbUrl = 'https://{s}.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=<%=mapAccessToken%>';
                                    var streets = L.tileLayer(mbUrl, {id: 'mapbox.streets', attribution: mbAttr}),
                                            satellite = L.tileLayer(mbUrl, {id: 'mapbox.streets-satellite', attribution: mbAttr}),
                                            grayscale = L.tileLayer(mbUrl, {id: 'pbellini.f33fdbb7', attribution: mbAttr});
                                    var map = L.map('map', {
                                        center: [<%= ServiceMap.getMapDefaultLatLng(request, "43.3555664, 11.0290384") %>],
                                        zoom: <%= ServiceMap.getMapDefaultZoom(request, "8") %>,
                                        layers: [streets]
                                    });
                                    var baseMaps = {
                                        "Streets": streets,
                                        "Satellite": satellite,
                                        "Grayscale": grayscale
                                    };
                                    var toggleMap = L.control.layers(baseMaps, null, {position: 'bottomright', width: '50px', height: '50px'});
                                    toggleMap.addTo(map);
                                    if (getUrlParameter("map") == "streets") {
                                        map.removeLayer(satellite);
                                        map.addLayer(streets);
                                    }
                                    else if (getUrlParameter("map") == "grayscale") {
                                        map.removeLayer(satellite);
                                        map.addLayer(grayscale);
                                    }

                                    // DEFINIZIONE DEI CONFINI MASSIMI DELLA MAPPA
                                    /*if(mode!="query") {
                                      var bounds = new L.LatLngBounds(new L.LatLng(41.7, 8.4), new L.LatLng(44.930222, 13.4));
                                      map.setMaxBounds(bounds);
                                    }*/
  
                                    // GENERAZIONE DEI LAYER PRINCIPALI
                                    var busStopsLayer = new L.LayerGroup();
                                    var servicesLayer = new L.LayerGroup();
                                    var eventLayer = new L.LayerGroup();
                                    var clickLayer = new L.LayerGroup();
                                    var GPSLayer = new L.LayerGroup();
                                    // AGGIUNTA DEL PLUGIN PER LA GEOLOCALIZZAZIONE
                                    var GPSControl = new L.Control.Gps({
                                        maxZoom: 16,
                                        style: null
                                    });
                                    var oms = new OverlappingMarkerSpiderfier(map, { keepSpiderfied: true });
                                    //map.addControl(GPSControl);
                                    //$("#currentPosition").add(GPSControl);+

                                    // ASSOCIA FUNZIONI AGGIUNTIVE ALL'APERTURA DI UN POPUP SU PARTICOLARI TIPI DI DATI
                                    var last_marker = null;
                                    map.on('popupopen', function (e) {
                                        $('#raggioricerca').prop('disabled', false);
                                        $('#raggioricerca_t').prop('disabled', false);
                                        $('#PublicTransportLine').prop('disabled', false);
                                        $('#nResultsServizi').prop('disabled', false);
                                        $('#nResultsSensori').prop('disabled', false);
                                        $('#nResultsBus').prop('disabled', false);
                                        $('#approximativeAddress').html('');
                                        var markerPopup = e.popup._source;
                                        //aggiunto
                                        var idServizio = markerPopup.feature.id;
                                        currentServiceUri = markerPopup.feature.properties.serviceUri;
                                        var tipoServizio = markerPopup.feature.properties.tipo;
                                        if(tipoServizio==undefined)
                                          tipoServizio=markerPopup.feature.properties.serviceType;
                                        var serviceType = markerPopup.feature.properties.serviceType;
                                        var nome = markerPopup.feature.properties.name;
                                        var divId = idServizio + "-" + tipoServizio;//assegnazione degli id ai div relativi ai servizi
                                        var coordinates = markerPopup.feature.geometry.coordinates;
                                        if (markerPopup.feature.properties.multimedia != "" && markerPopup.feature.properties.multimedia != null) {
                                            $('#' + divId).closest('div.leaflet-popup-content-wrapper').css("width", "300px");
                                        }
                                        popup_fixpos(divId);
                                        if (mode != "embed") {
                                            //selezione = 'Servizio: ' + markerPopup.feature.properties.name;
                                            if (tipoServizio == 'fermata'){
                                                selezione = 'Bus Stop: ' + markerPopup.feature.properties.name;
                                            }else{
                                                selezione = 'Service: ' + markerPopup.feature.properties.name;
                                            }
                                            $('#selezione').html(selezione);
                                            clickLayer.clearLayers();
                                            // CLICK SUL MARKER - Viene evidenziato il SELECTED marker, e settata l'icona di default al precedente selezionato.                                
                                            $(markerPopup._icon).siblings().removeClass('selected');
                                            if (last_marker != null && last_marker.getLatLng() != markerPopup.getLatLng()) {
                                                if (last_marker.feature.properties.serviceType == "") {
                                                    var serviceIcon = "generic";
                                                } else {
                                                    var serviceIcon = last_marker.feature.properties.serviceType;
                                                    //Michela
                                                    if( (serviceType == "TransferServiceAndRenting_BusStop" || serviceType == "Tram_stops" || 
                                                            serviceType == "Train_station" || serviceType == "Ferry_stop" )){
                                                        if(last_marker.feature.properties.agency)
                                                            serviceIcon = serviceIcon +"_"+ last_marker.feature.properties.agency.toLowerCase().replace(/\./g, "").replace(/&/g, "").replace(/ù/g, "u").replace(/à/g, "a").replace(/ /g, ""); 
                                                        else
                                                            serviceIcon = serviceIcon +"_ataflinea"; 
                                                    } 
                                                }
                                                if (last_marker.feature.properties.serviceType != "bus_real_time") {
                                                    
                                                    var def_icon = L.icon({
                                                        
                                                        //iconUrl: ctx + '/img/mapicons/' + last_marker.feature.properties.serviceType + '.png',
                                                        //iconRetinaUrl: ctx + '/img/mapicons/' + last_marker.feature.properties.serviceType + '.png',
                                                        iconUrl: ctx + '/img/mapicons/' + serviceIcon + '.png',
                                                        iconRetinaUrl: ctx + '/img/mapicons/' + serviceIcon + '.png',
                                                        iconSize: [26, 29],
                                                        iconAnchor: [13, 29], });
                                                    //popupAnchor: [0, -27],});
                                                    last_marker.setIcon(def_icon);
                                                }
                                                else {
                                                    var def_icon = L.icon({
                                                        iconUrl: ctx + '/img/mapicons/' + serviceIcon + '.gif',
                                                        iconRetinaUrl: ctx + '/img/mapicons/' + serviceIcon + '.gif',
                                                        iconSize: [15, 15],
                                                        iconAnchor: [7, 15],
                                                        popupAnchor: [0, -7], });
                                                    last_marker.setIcon(def_icon);
                                                }
                                            }
                                            $(markerPopup._icon).addClass('selected');
                                            last_marker = markerPopup;
                                        }
                                        loadServiceInfo(currentServiceUri, divId, idServizio, coordinates);
                                        coordinateSelezione = markerPopup.feature.geometry.coordinates[1] + ";" + markerPopup.feature.geometry.coordinates[0];
                                        listOfPopUpOpen.push(currentServiceUri);
                                    });
                                    map.on('popupclose', function (e) {
                                        var popupToRemove = e.popup._source;
                                        for (var i = listOfPopUpOpen.length - 1; i >= 0; i--) {
                                            if (listOfPopUpOpen[i] === popupToRemove.feature.properties.serviceUri) {
                                                listOfPopUpOpen.splice(i, 1);
                                            }
                                        }
                                    });
                                    // AL CLICK CERCO L'INDIRIZZO APPROSSIMATIVO	
                                    map.on('click', function (e) {
                                      mapLatLngClick(e.latlng);
                                    });
                                    var selezioneAttiva = false;
                                    var ricercaInCorso = false;
                                    var logEndPoint = "<%=logEndPoint%>";
                                    $(document).ready(function () {
                                        // funzione di inizializzazione all'avvio della mappa
                                        init();
                                    });
                                    function init() {
                                        mostraElencoAgenzie(); changeLanguage('ENG');
                                        // CREO LE TABS JQUERY UI NEL MENU IN ALTO
                                        $("#tabs").tabs();
                                        $("#tabs-servizi").tabs();
                                        $("#path_date").datepicker({dateFormat: "yy-mm-dd"});
                                        $("#path_time").timepicker({timeFormat:"H:i", step: 10});
                                        //$("#fancytree").fancytree();
                                        
                                        /* //fancy tree INIZIO
                                        $.ajax({
                                                url: "${pageContext.request.contextPath}/ajax/json/taxonomy.jsp",
                                                type: "GET",
                                                async: true,
                                                dataType: 'json',
                                                data: {
                                                    lang: "ENG",
                                                },
                                                success: function (response) {
                                                  taxonomy_ENG = response;
                                                }
                                            });
                                        $.ajax({
                                                url: "${pageContext.request.contextPath}/ajax/json/taxonomy.jsp",
                                                type: "GET",
                                                async: true,
                                                dataType: 'json',
                                                data: {
                                                    lang: "ITA",
                                                },
                                                success: function (response) {
                                                  taxonomy_ITA = response;
                                                }
                                            });
                                        //fancy tree fine
                                        */
                                        $.widget( "ui.autocomplete", $.ui.autocomplete, {
                                          _renderItem: function( ul, item ) {
                                            var icon = item.properties.serviceType.replace(" ","_");
                                            if(icon=="StreetNumber" || icon=="Municipality")
                                              icon = "generic";
                                            return $( "<li>" )
                                              .attr( "data-value", item.value )
                                              .append( "<img src=\""+ctx+"/img/mapicons/"+icon+".png\" height=\"23\" width=\"20\" align=\"top\">"+item.label )
                                              .appendTo( ul );
                                          }
                                        });
                                        $("#quick-search").autocomplete({source: quickSearch, select: quickSearchSelect });
                                        if (mode == "query" || mode == "embed" || mode == "bus-position") {
                                            var url = document.URL;
                                            var queryString = url.substring(url.indexOf('?') + 1);
                                            var parameters = parseUrlQuery(queryString);
                                            $("#embed.menu").hide();
                                            $("#save").hide();
                                            $("#saveQuery").hide();
                                            $("#saveQuerySearch").hide();
                                            var controls = parameters["controls"];
                                            if (mode == "query" && controls == undefined)
                                                controls = "collapsed";
                                            if (controls == "false" || controls == "hidden" || controls == undefined) {
                                                //michela SE NON vedo il menu DEVO METTERE i logo
                                                
                                                $("#menu-dx").hide();
                                                $("#menu-alto").hide();//al posto di questo devo mettere i logo
                                                $("#menu-alto-hidden").show();
                                                
                                                $("#embed.menu").hide();
                                            }
                                            else if (controls == "collapsed") {
                                                $("#menu-dx .header").click();
                                                $("#menu-alto .header").click();
                                            }
                                            var info = parameters["info"];
                                            if (info == "false" || info == "hidden" /*|| info ==undefined*/){
                                                $("#info-aggiuntive").hide();
                                            }
                                            else if (info == "collapsed") {
                                                $("#info-aggiuntive .header").click();
                                            }
                                            if (parameters["description"] == "false") {
                                                $("#queryBox").hide();
                                            }
                                            setTimeout(function waitMapSize() { 
                                              if(map.getSize().y>0) {
                                                if (parameters["showBusPosition"] == "true" || mode=="bus-position") {
                                                  mostraAutobusRT(true,parameters["agency"],parameters["line"]);
                                                } else {
                                                  showResults(parameters); 
                                                }
                                              } else 
                                                setTimeout(waitMapSize,500); 
                                            },500);
                                        }
                                        $("#raggioricerca").change(function(){
                                          var selected=$(this).val();
                                          $("#geosearch").prop("disabled", selected!="geo");
                                        });
                                        $("#geosearch").change(function(){
                                          clickLayer.clearLayers();
                                          var selected=$(this).val();
                                          if(selected!="select")
                                            $.ajax({
                                                url: "${pageContext.request.contextPath}/ajax/json/get-geometry.jsp",
                                                type: "GET",
                                                async: true,
                                                dataType: 'json',
                                                data: {
                                                    label: selected
                                                },
                                                success: function (response) {
                                                  var wkt = new Wkt.Wkt();
                                                  wkt.read(response.wkt);
                                                  var obj = wkt.toObject();
                                                  obj.addTo(clickLayer);
                                                  clickLayer.addTo(map);
                                                  map.fitBounds(clickLayer.getLayers()[0].getBounds());
                                                }
                                            });                                          
                                        });
                                    }

                                    var comuneChoice;
                                    var selezione;
                                    var coordinateSelezione;
                                    var numeroRisultati;
                                    // MOSTRA ELENCO COMUNI DI UNA PROVINCIA
                                    function mostraElencoComuni(selectOption, nomeComune) {
                                        if ($("#elencoprovince").val() != null) {
                                            //	if (selectOption.options.selectedIndex != 0){	
                                            $('#elencolinee')[0].options.selectedIndex = 0;
                                            $('#elencofermate').html('<option value=""> - Select a Bus Stop - </option>');
                                            //$('#loading').show();
                                            $.ajax({
                                                url: "${pageContext.request.contextPath}/ajax/get-municipality-list.jsp",
                                                type: "GET",
                                                async: true,
                                                //dataType: 'json',
                                                data: {
                                                    nomeProvincia: $("#elencoprovince").val()
                                                            // nomeProvincia: selectOption.options[selectOption.options.selectedIndex].value
                                                },
                                                success: function (msg) {
                                                    $('#elencocomuni').html(msg);
                                                    if (mode == "embed" || mode == "query") {
                                                        $('#elencocomuni').val(nomeComune);
                                                        //$('#loading').hide();
                                                    }
                                                    else
                                                        $('#loading').hide();
                                                }
                                            });
                                        }
                                    }

                                    /*function loadServiceInfo(uri, div) {
                                     $.ajax({
                                     data: {
                                     serviceUri: uri
                                     },
                                     url: "${pageContext.request.contextPath}/api/service.jsp",
                                     type: "GET",
                                     async: true,
                                     dataType: 'json',
                                     success: function (data) {
                                     if (data.features.length > 0) {
                                     var tipo = data.features[0].properties.tipo;
                                     selezione = 'Servizio: ' + data.features[0].properties.nome;
                                     $('#selezione').html(selezione);
                                     var divMM = data.features[0].id + "-multimedia";
                                     var multimediaResource = data.features[0].properties.multimedia;
                                     var htmlDiv;
                                     if (multimediaResource != null)
                                     {
                                     var format = multimediaResource.substring(multimediaResource.length -4);
                                     if (format == ".mp3"){
                                     htmlDiv = "<div id=\"" + divMM + "\"class=\"multimedia\"><audio controls class=\"audio-controls\"><source src=\""+ multimediaResource +"\" type=\"audio/mpeg\"></audio></div>";
                                     }else{
                                     if ((format == ".wav") || (format == ".ogg")){
                                     htmlDiv = "<div id=\"" + divMM + "\"class=\"multimedia\"><audio controls class=\"audio-controls\"><source src=\""+ multimediaResource +"\" type=\"audio/"+ format+"\"></audio></div>";  
                                     }else{
                                     htmlDiv = "<div id=\"" + divMM + "\"class=\"multimedia\"><img src=\"" + multimediaResource + "\" width=\"80\" height=\"80\"></div>";   
                                     }
                                     }
                                     }
                                     if (include(tipo, "@en"))
                                     tipo = tipo.replace("@en", "");
                                     else
                                     tipo = tipo.replace("@it", "");
                                     var divId = data.features[0].id + "-" + tipo;
                                     if (data.features[0].properties.tipo != "fermata") {
                                     contenutoPopup = "<h3>" + data.features[0].properties.nome + "</h3>";
                                     contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + data.features[0].properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
                                     contenutoPopup = contenutoPopup + "<b>Tipologia:</b> " + tipo + "<br />";
                                     var feature = data.features[0];
                                     if (data.features[0].properties.email != "" && data.features[0].properties.email)
                                     contenutoPopup = contenutoPopup + "<b>Email:</b><a href=\"mailto:"+feature.properties.email+"?Subject=information request\" target=\"_top\"> " + feature.properties.email + "</a><br />";
                                     
                                     if (feature.properties.website != "" && feature.properties.website)
                                     contenutoPopup = contenutoPopup + "<b>Website:</b><a href=\"http\://"+ feature.properties.website + "\" target=\"_blank\" title=\""+feature.properties.nome+" - website\"> " + feature.properties.website + "</a><br />";
                                     if (feature.properties.phone != "" && feature.properties.phone)
                                     contenutoPopup = contenutoPopup + "<b>Phone:</b> " + feature.properties.phone + "<br />";
                                     if (feature.properties.fax != "" && feature.properties.fax)
                                     contenutoPopup = contenutoPopup + "<b>Fax:</b> " + feature.properties.fax + "<br />";
                                     if (data.features[0].properties.indirizzo != "")
                                     contenutoPopup = contenutoPopup + "<b>Indirizzo:</b> " + data.features[0].properties.indirizzo;
                                     if (data.features[0].properties.numero != "" && data.features[0].properties.numero)
                                     contenutoPopup = contenutoPopup + ", " + data.features[0].properties.numero + "<br />";
                                     else
                                     contenutoPopup = contenutoPopup + "<br />";
                                     if (feature.properties.cap != "" && feature.properties.cap)
                                     contenutoPopup = contenutoPopup + "<b>Cap:</b> " + feature.properties.cap + "<br />";
                                     if (feature.properties.city != "" && feature.properties.city)
                                     contenutoPopup = contenutoPopup + "<b>City:</b> " + feature.properties.city + "<br />";
                                     if (feature.properties.province != "" && feature.properties.province)
                                     contenutoPopup = contenutoPopup + "<b>Prov.:</b> " + feature.properties.province + "<br />";
                                     if (data.features[0].properties.multimedia != "" && data.features[0].properties.multimedia) {
                                     contenutoPopup = contenutoPopup + "<b>Multimedia Content:</b></br>" +htmlDiv;
                                     }
                                     if (data.features[0].properties.description != "" && data.features[0].properties.description) {
                                     if (include(data.features[0].properties.description, "@it"))
                                     data.features[0].properties.description = data.features[0].properties.description.replace("@it", "");
                                     contenutoPopup = contenutoPopup + "Description: " + data.features[0].properties.description + "<br />";
                                     }
                                     if (data.features[0].properties.note != "" && data.features[0].properties.note)
                                     contenutoPopup = contenutoPopup + "<b>Note:</b> " + data.features[0].properties.note + "<br />";
                                     
                                     contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                     var name = data.features[0].properties.nome;
                                     nameEscaped = escape(name);
                                     var divSavePin = "savePin-" + data.features[0].id;
                                     contenutoPopup = contenutoPopup + "<div id=\"" + divSavePin + "\" class=\"savePin\" onclick=save_handler('" + data.features[0].properties.tipo + "','" + data.features[0].properties.serviceUri + "','" + nameEscaped + "')></div>";
                                     //layer.addTo(map).bindPopup(contenutoPopup).openPopup();
                                     if (tipo == 'parcheggio_auto' || tipo == "parcheggio_Coperto" || tipo == "parcheggi_all'aperto" || tipo == "car_park@en") {
                                     mostraParcheggioAJAX(name, divId);
                                     }
                                     if (tipo == 'sensore') {
                                     mostraSensoreAJAX(name, divId);
                                     }
                                     $("#" + div).html(contenutoPopup);
                                     if (multimediaResource != "" && multimediaResource != null)
                                     $(".leaflet-popup-content-wrapper").css("width", "300px");
                                     popup_fixpos(div);
                                     } else {
                                     var divLinee = divId + "-linee";
                                     var contenutoPopup = "<h3>FERMATA : " + data.features[0].properties.nome + "</h3>";
                                     contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + data.features[0].properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
                                     contenutoPopup = contenutoPopup + "<div id=\"" + divLinee + "\" ></div>";
                                     contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                     var divSavePin = "savePin-" + data.features[0].id;
                                     var name = data.features[0].properties.nome;
                                     nameEscaped = escape(name);
                                     contenutoPopup = contenutoPopup + "<div id=\"" + divSavePin + "\" class=\"savePin\" onclick=save_handler('" + data.features[0].properties.tipo + "','" + data.features[0].properties.serviceUri + "','" + nameEscaped + "')></div>";
                                     $("#" + div).html(contenutoPopup);
                                     popup_fixpos(div);
                                     mostraAVMAJAX(name, divId);
                                     mostraLineeBusAJAX(name, divLinee);
                                     }
                                     } else {
                                     contenutoPopup = "No info sul servizio " + uri;
                                     $("#" + div).html(contenutoPopup);
                                     popup_fixpos(div);
                                     }
                                     }
                                     });
                                     }*/
                                    // FUNZIONI DI RICERCA PRINCIPALI
                                    function mostraLineeBusAJAX(uriFermata, divLinee, divRoute) {
                                        $.ajax({
                                            url: "${pageContext.request.contextPath}/ajax/get-lines-of-stop.jsp",
                                            type: "GET",
                                            async: true,
                                            //dataType: 'json',
                                            data: {
                                                uriFermata: uriFermata,
                                                divRoute: divRoute,
                                            },
                                            success: function (msg) {
                                                $("#" + divLinee).html(msg);
                                                popup_fixpos(divLinee);
                                                //$('#info-aggiuntive .content').html(msg);
                                            }
                                        });
                                    }
                                    function mostraAVMAJAX(nomeFermata, divId) {
                                        $('#loading').show();
                                        $.ajax({
                                            url: "${pageContext.request.contextPath}/ajax/get-avm.jsp",
                                            type: "GET",
                                            async: true,
                                            //dataType: 'json',
                                            data: {
                                                nomeFermata: nomeFermata
                                            },
                                            success: function (msg) {
                                                $("#" + divId).html(msg);
                                                //$('#info-aggiuntive .content').html(msg);
                                                popup_fixpos(divId);
                                                $('#loading').hide();
                                            }
                                        });
                                    }
                                    function mostraParcheggioAJAX(nomeParcheggio, divId) {
                                        $.ajax({
                                            url: "${pageContext.request.contextPath}/ajax/get-parking-status.jsp",
                                            type: "GET",
                                            async: true,
                                            //dataType: 'json',
                                            data: {
                                                nomeParcheggio: nomeParcheggio
                                            },
                                            success: function (msg) {
                                                $("#" + divId).html(msg);
                                                //$('#info-aggiuntive .content').html(msg);
                                                popup_fixpos(divId);
                                            }
                                        });
                                    }
                                    //MICHELA 
                                    function mostraOrariAJAX(TPLStopUri, divId){//uri exnomefermata
                                        $.ajax({
                                            url: "${pageContext.request.contextPath}/ajax/json/get-timetable.jsp",
                                            type: "GET",
                                            async: true,
                                            dataType: 'json',
                                            data: {
                                                TPLStopUri: TPLStopUri
                                            },
                                            success: function (msg) {
                                                //alert(msg);
                                                //$("#" + divId).html(msg);//ok 
                                                msg.idtable = "timetable-"+divId;
                                                ViewManager.render(msg, "#" + divId, "BusStop");
                                                
                                                $.fn.dataTable.moment('HH:MM:ss yyyy-mm-dd');
                                                $("#" +  msg.idtable).DataTable({
                                                    "columnDefs": [
                                                            { "width": "60px", "targets": 0 },
                                                            { "type": "date", "targets": 0 }],
                                                    "autowidth": true,
                                                    "order": [[ 0, "asc" ]],
                                                    "scrollY":  "200px", 
                                                    "scrollCollapse": true,
                                                    "lengthMenu": [[10, 25, 50, 100, 200], [10, 25, 50, 100, 200]],// [[10, 25, 50, 100, 200], [10, 25, 50, 100, 200]],
                                                    "pagingType": "numbers",
                                                    "language": {
                                                        "lengthMenu":   "<span name=\"lbl\" caption=\"timetable_display\">Display </span>"+ "_MENU_" + 
                                                                        "<span name=\"lbl\" caption=\"timetable_records\"> Bus per page</span>",
                                                        "zeroRecords":  "Nothing found - sorry",
                                                        "info":         "<span name=\"lbl\" caption=\"timetable_showing\">Showing page</span>"+" _PAGE_ "+ 
                                                                        "<span name=\"lbl\" caption=\"timetable_of\">of</span>"+" _PAGES_",
                                                        "infoEmpty":    "No records available",
                                                        "infoFiltered": "(filtered from _MAX_ total records)",
                                                        "search":       "<span name=\"lbl\" caption=\"timetable_search\">Search</span>"+":",
                                                    }
                                                });
                                                //$("#" + divId).html(parseJSON(msg));
                                                //$('#info-aggiuntive .content').html(msg);
                                                popup_fixpos(divId);
                                            }
                                        });
                                    }
                                    function mostraSensoreAJAX(nomeSensore, divId) {
                                        $.ajax({
                                            url: "${pageContext.request.contextPath}/ajax/get-sensor-data.jsp",
                                            type: "GET",
                                            async: true,
                                            //dataType: 'json',
                                            data: {
                                                nomeSensore: nomeSensore
                                            },
                                            success: function (msg) {
                                                $("#" + divId).html(msg);
                                                popup_fixpos(divId);
                                                //$('#info-aggiuntive .content').html(msg);

                                            }
                                            // timeout:10000
                                        });
                                    }

                                    function updateSelection() {
                                        comuneChoice = $('#elencocomuni').val();
                                        selezione = "COMUNE di " + comuneChoice;
                                        $('#nResultsServizi').prop('disabled', false);
                                        $('#nResultsSensori').prop('disabled', false);
                                        $('#nResultsBus').prop('disabled', false);
                                        $('#selezione').html(selezione);
                                        coordinateSelezione = "";
                                        $('#raggioricerca')[0].options.selectedIndex = 0;
                                        $('#raggioricerca').prop('disabled', 'disabled');
                                        $('#raggioricerca_t')[0].options.selectedIndex = 0;
                                        $('#raggioricerca_t').prop('disabled', 'disabled');
                                        $('#approximativeAddress').html('');
                                        if ($('#PublicTransportLine').prop('checked')) {
                                            if ($('#PublicTransportLine').prop('checked', false))
                                                ;
                                        }
                                        $('#PublicTransportLine').prop('disabled', 'disabled');
                                        $.ajax({
                                            url: "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                            type: "GET",
                                            async: true,
                                            //dataType: 'json',
                                            data: {
                                                nomeComune: comuneChoice
                                            },
                                            success: function (msg) {
                                                $('#info-aggiuntive .content').html(msg);
                                            }
                                        });
                                    }

                                    function getCategorie(tipo_cat) {
                                        // ESTRAGGO LE CATEGORIE SELEZIONATE
                                        var categorie = [];
            <% if (km4cVersion.equals("old")) {%>
                                        $('#' + tipo_cat + ' :checked').each(function () {
                                            categorie.push($(this).val());
                                        });
            <%} else {%>
                                        var nCatAll = 0;
                                        $('#' + tipo_cat + ' .macrocategory:checked').each(function () {
                                            if ($('#' + tipo_cat + ' .sub_' + $(this).val() + ":not(:checked)").length == 0) {
                                                categorie.push($(this).val());
                                                if ($(this).val() == "TransferServiceAndRenting") {
                                                    categorie.push("BusStop");
                                                    categorie.push("SensorSite");
                                                }
                                                if ($(this).val() == "Path") {
                                                    categorie.pop("Path");
                                                    categorie.push("Cycle_paths");
                                                    categorie.push("Tourist_trail");
                                                    categorie.push("Tramline");
                                                }
                                                if ($(this).val() == "Area") {
                                                    categorie.pop("Area");
                                                    categorie.push("Gardens");
                                                    categorie.push("Green_areas");
                                                    categorie.push("Controlled_parking_zone");
                                                }
                                                // CODICE PER EVENTI NEI TRASVERSALI
                                                if ($(this).val() == "HappeningNow") {
                                                    categorie.pop("HappeningNow");
                                                    categorie.push("Event");
                                                }
                                                nCatAll++;
                                            }
                                            else
                                                $('#' + tipo_cat + ' .sub_' + $(this).val() + ":checked").each(function () {
                                                    categorie.push($(this).val());
                                                });
                                        });
                                        if (nCatAll == $('#' + tipo_cat + ' .macrocategory').length) {
                                            categorie = ["Service"];
                                            /*if (tipo_cat == "categorie") {
                                             categorie.push("BusStop");
                                             categorie.push("SensorSite");
                                             }else{
                                             
                                             if ($('#Bus').is(':checked'))
                                             categorie.push($("#Bus").val());
                                             if ($('#Sensor').is(':checked'))
                                             categorie.push($("#Sensor").val());
                                             }*/
                                            categorie.push("BusStop");
                                            categorie.push("SensorSite");
                                            if (tipo_cat == 'categorie_t') {
                                                categorie.push("Event");
                                                categorie.push("PublicTransportLine");
                                            }

                                        }
            <% }%>


                                        return categorie;
                                    }
                                    function ricercaCategorie(){
                                        var testodacercare = $("#raggioricerca").val();
                                    }
                                    
                                    function ricercaServizi(tipo_categorie, cat, limit) {
                                        mode = "normal";
                                        var tipo_cat = tipo_categorie;
                                        if(cat != null){
                                            /*if(cat == 'TransferServiceAndRenting'){
                                                var stringaCategorie = cat+";BusStop;SensorSite";
                                            }else{
                                                var stringaCategorie = cat;
                                            }*/
                                            var stringaCategorie = cat;
                                            if((cat.indexOf("BusStop") != -1) || (cat.indexOf("SensorSite") != -1)){
                                                //limit = $("#nResultsServizi").val();
                                                //alert("limit: "+limit);
                                                tipo_cat = tipo_cat+":"+$("#nResultsServizi").val();
                                            }
                                        }else{
                                            var stringaCategorie = getCategorie(tipo_cat).join(";");
                                        }
                                        if (selezione == undefined)
                                            selezione = "";
                                        //if (tipo_categorie == "categorie")
                                        if ((tipo_cat.indexOf("categorie:") != -1) || tipo_cat == "categorie"){
                                            var raggioRicerca = $("#raggioricerca").val();
                                        }
                                        else{
                                            var raggioRicerca = $("#raggioricerca_t").val();
                                        }
                                        if (((selezione != '') && (selezione.indexOf('Bus Line') == -1)) || raggioRicerca == "area" || raggioRicerca == "geo") {
                                            if (stringaCategorie == "") {
                                                alert("Select at least one category from top-right menu");
                                            }
                                            else {
                                                $('#loading').show();
                                                // SVUOTO LA MAPPA DAI PUNTI PRECEDENTEMENTE DISEGNATI
                                                if ((selezione.indexOf("Linea Bus:") == -1) || (selezione.indexOf("Bus Line:") == -1)) {
                                                    svuotaLayers();
                                                }
                                                mostraServiziAJAX_new(stringaCategorie, selezione, coordinateSelezione, comuneChoice, limit, null, null, null, null, null, null, null, tipo_cat);
                                            }
                                        }
                                        else {
                                            //alert("Attenzione, non è stata selezionata alcuna risorsa di partenza per la ricerca");
                                            alert("Attention, you did not select any resources base for research");
                                        }
                                    }

                                    function mostraServiziAJAX_new(categorie, selezione, coordinateSelezione, nomeComune, risultatiServizi, risultatiSensori, risultatiBus, raggioServizi, raggioSensori, raggioBus, openPins, textFilter, tipo_categoria) {
                                        //$('#info-aggiuntive .content').html('');
                                        
                                        if (tipo_categoria == undefined)
                                            tipo_categoria = "categorie";
                                        //Michela
                                        /*
                                        if(tipo_categoria == "categorie_t"){
                                            var countCategories = categorie.split(";");
                                            var loading_categories = countCategories.length;
                                            $('#loading').show();
                                        }*/
                                        //michela
                                        
                                        if (mode != "query" && mode != "embed") {
                                            if ((tipo_categoria == "categorie") || (tipo_categoria.indexOf("categorie:") != -1)) {
                                                if (risultatiServizi != null){
                                                    var numeroRisultatiServizi = risultatiServizi;
                                                }else{
                                                    var numeroRisultatiServizi = $('#nResultsServizi').val();
                                                }
                                                //MICHELA
                                                var numeroRisultatiSensori = $('#nResultsSensor').val();
                                                var numeroRisultatiBus = $('#nResultsBus').val();
                                                var raggioServizi = $("#raggioricerca").val();
                                                var areaServizi = $("#geosearch").val();
                                                var raggioSensori = raggioServizi;
                                                var raggioBus = raggioServizi;
                                                var textFilter = $("#serviceTextFilter").val();
                                                var value_type = $("#valueTypeFilter").val();
                                            }
                                            else {
                                                var numeroRisultatiServizi = $('#nResultsServizi_t').val();
                                                var raggioServizi = $("#raggioricerca_t").val();
                                                var textFilter = $("#serviceTextFilter_t").val();
                                            }

                                            //per salvataggio query
                                            var raggi = [];
                                            raggi.push(raggioServizi, raggioSensori, raggioBus);
                                            var numRis = [];
                                            numRis.push(numeroRisultatiServizi, numeroRisultatiSensori, numeroRisultatiBus);
                                        }
                                        else {
                                            //modificare per riprendere tutti i valori del numero risultati (servizi, sensori e bus)
                                            numeroRisultatiServizi = risultatiServizi;
                                            numeroRisultatiSensori = risultatiSensori;
                                            numeroRisultatiBus = risultatiBus;
                                            if(raggioServizi=="area")
                                              raggioServizi=-1;
                                            $('#loading').show();
                                        }
                                        var centroRicerca;
                                        if (pins.length > 0)
                                            pins = "";
                                        //if (((selezione != null && selezione.indexOf("COMUNE di") == -1) && (raggioServizi == "geo" || raggioServizi=="inside")) || (coordinateSelezione != "" && undefined != coordinateSelezione && coordinateSelezione != "null" && coordinateSelezione != null)) {
                                        if (((selezione != null && selezione.indexOf("COMUNE di") == -1) && (raggioServizi == "geo" || raggioServizi=="inside" || raggioServizi=="area")) || (coordinateSelezione != "" && undefined != coordinateSelezione && coordinateSelezione != "null" && coordinateSelezione != null)
                                                /*|| (selezione == "" && categorie == 'BusStop')*/ ) {
                                            if (raggioServizi == "geo") {
                                                centroRicerca = "geo:"+areaServizi;
                                            }
                                            else if (raggioServizi == "area") {
                                                var bnds = map.getBounds()
                                                centroRicerca = bnds.getSouth() + ";" + bnds.getWest() + ";" + bnds.getNorth() + ";" + bnds.getEast();
                                            }
                                            else if (coordinateSelezione == "Posizione Attuale") {
                                                // SE HO RICHIESTO LA POSIZIONE ATTUALE ESTRAGGO LE COORDINATE
                                                centroRicerca = GPSControl._currentLocation.lat + ";" + GPSControl._currentLocation.lng;
                                            }
                                            /*else if ((selezione.indexOf("Fermata Bus:") != -1) || (selezione.indexOf("Bus Stop:") != -1)) {
                                                centroRicerca = coordinateSelezione;
                                            }
                                            else if (selezione.indexOf("Coord:") != -1 || selezione.indexOf("Numero Bus:") != -1) {
                                                centroRicerca = coordinateSelezione;
                                            }
                                            else if ((selezione.indexOf("Servizio:") != -1) || (selezione.indexOf("Service:") != -1)) {
                                                centroRicerca = coordinateSelezione;
                                            }
                                            else if (selezione.indexOf("point") != -1) {
                                                centroRicerca = coordinateSelezione;
                                            }*/
                                            else
                                                centroRicerca = coordinateSelezione;
                                            var text = $('#serviceTextFilter').val();
                                            query = saveQueryServices(centroRicerca, raggi, categorie, numRis, selezione, text);
                                            var coord = centroRicerca.split(";");
                                            if(raggioServizi!="geo")
                                              clickLayer.clearLayers();
                                            if (raggioServizi != "area" && coord.length==2) {
                                                clickLayer.addLayer(L.marker([coord[0], coord[1]]));
                                                if(raggioServizi!="inside")
                                                  clickLayer.addLayer(L.circle([coord[0], coord[1]], raggioServizi * 1000, {className: 'circle'})).addTo(map);
                                            } else if(coord.length==4 && mode=="query") {
                                                clickLayer.addLayer(L.marker([(Number(coord[0])+Number(coord[2]))/2, (Number(coord[1])+Number(coord[3]))/2]));
                                                clickLayer.addLayer(L.rectangle([[coord[0], coord[1]],[coord[2], coord[3]]])).addTo(map);
                                            } else if(coord.length==1 && centroRicerca.startsWith("geo:") && mode=="query") {
                                                var label=centroRicerca.substr(4);
                                                $.ajax({
                                                  url: "${pageContext.request.contextPath}/ajax/json/get-geometry.jsp",
                                                  type: "GET",
                                                  async: true,
                                                  dataType: 'json',
                                                  data: {
                                                      label: label
                                                  },
                                                  success: function (response) {
                                                    var wkt = new Wkt.Wkt();
                                                    wkt.read(response.wkt);
                                                    var obj = wkt.toObject();
                                                    obj.addTo(clickLayer);
                                                    clickLayer.addTo(map);
                                                  }
                                              });                                          
                                            } else if(raggioServizi=="geo") {
                                            }
                                            
                                            var numeroServizi = 0;
                                            var numeroBus = 0;
                                            var numeroSensori = 0;
                                            var numEventi = 0;
                                            var numLineeBus = 0;
                                            var numTotRisultati = 0;
                                            var numTotServices = 0;
                                            var numTotBusStops = 0;
                                            var numTotSensors = 0;
                                            if (centroRicerca!=null && centroRicerca.indexOf("COMUNE di") != -1) {
                                                //getServices in MunicipalityMICHELA INIZIO
                                                $.ajax({
                                                    url: ctx + "/ajax/json/get-services-in-municipality.jsp",
                                                    type: "GET",
                                                    async: true,
                                                    dataType: 'json',
                                                    data: {
                                                        nomeProvincia: provincia,
                                                        nomeComune: nomeComune,
                                                        categorie: categorie,
                                                        textFilter: textFilter,
                                                        numeroRisultatiServizi: numeroRisultatiServizi,
                                                        numeroRisultatiSensori: numeroRisultatiSensori,
                                                        numeroRisultatiBus: numeroRisultatiBus,
                                                        cat_servizi: tipo_categoria
                                                    },
                                                    success: function (msg) {
                                                        //console.log(msg);
                                                        if (mode == "JSON") {
                                                            $("#body").replaceWith(JSON.stringify(msg));
                                                        }
                                                        else {
                                                            if ($("#elencocomuni").val() != 'all') {
                                                            }

                                                            $('#loading').hide();
                                                            if (msg.features.length > 0) {
                                                                var i = 0;
                                                                var count = 0;
                                                                servicesLayer = L.geoJson(msg, {
                                                                    pointToLayer: function (feature, latlng) {
                                                                        marker = showmarker(feature, latlng);
                                                                        return marker;
                                                                    },
                                                                    onEachFeature: function (feature, layer) {
                                                                        var contenutoPopup = "";
                                                                        var divId = feature.id + "-" + feature.properties.tipo;
                                                                        contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                                                        layer.bindPopup(contenutoPopup);
                                                                        if (feature.properties.serviceType == "TransferServiceAndRenting_BusStop") {
                                                                            numeroBus++;
                                                                        }
                                                                        else {
                                                                            if (feature.properties.serviceType == "TransferServiceAndRenting_SensorSite") {
                                                                                numeroSensori++;
                                                                            }
                                                                            else {
                                                                                numeroServizi++;
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                                <%if (clusterResults > 0) {%>
                                                                if (msg.features.length >=<%=clusterResults%>) {
                                                                    markers = new L.MarkerClusterGroup({maxClusterRadius: <%=clusterDistance%>, disableClusteringAtZoom: <%=noClusterAtZoom%>});
                                                                    servicesLayer = markers.addLayer(servicesLayer);
                                                                    //$("#cluster-msg").text("più di <%=clusterResults%> risultati, attivato clustering");
                                                                    $("#cluster-msg").text("more than <%=clusterResults%> results, clustering enabled");
                                                                    $("#cluster-msg").show();
                                                                }
                                                                else
                                                                    $("#cluster-msg").hide();
            <%}%>
                                                                servicesLayer.addTo(map);
                                                                if (mode == "embed") {
                                                                    for (i in servicesLayer._layers) {
                                                                        var uri = servicesLayer._layers[i].feature.properties.serviceUri;
                                                                        if (include(openPins, uri))
                                                                            servicesLayer._layers[i].openPopup();
                                                                    }
                                                                }
                                                                var markerJson = JSON.stringify(msg.features);
                                                                pins = markerJson;
                                                                //if (mode != "embed") {
                                                                var confiniMappa = servicesLayer.getBounds();
                                                                map.fitBounds(confiniMappa, {padding: [50, 50]});
                                                                if (tipo_categoria == "categorie" || tipo_categoria.indexOf("categorie:") != -1) {
                                                                    risultatiRicerca((numeroServizi + numeroBus + numeroSensori), 0, 0, 1, null, 0, 0, 0);
                                                                } else {
                                                                    risultatiRicerca(numeroServizi, numeroBus, numeroSensori, 1, null, 0, 0, 0);
                                                                }
                                                            }
                                                            else {
                                                                risultatiRicerca(0, 0, 0, 0, null, 0, 0, 0);
                                                            }
                                                        }
                                                        if (categorie.indexOf("Event") != -1) {
                                                            numEventi = searchEvent("transverse", null, null, numeroRisultatiServizi, textFilter);
                                                            if (numEventi != 0) {
                                                                risultatiRicerca((numeroServizi + numEventi), numeroBus, numeroSensori, 1, null, 0, 0, 0);
                                                                $("input[name=event_choice][value=day]").attr('checked', 'checked');
                                                            }
                                                        }
                                                    },
                                                    error: function (request, status, error) {
                                                        if ($("#elencocomuni").val() != 'all') {
                                                            $.ajax({
                                                                url: "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                                                type: "GET",
                                                                async: true,
                                                                //dataType: 'json',
                                                                data: {
                                                                    nomeComune: $("#elencocomuni").val()
                                                                },
                                                                success: function (msg) {
                                                                    $('#info-aggiuntive .content').html(msg);
                                                                }
                                                            });
                                                        }
                                                        $('#loading').hide();
                                                        console.log(error);
                                                        alert('Si è verificato un errore');
                                                    }
                                                });
                                                //getServices in municipality FINE
                                                }else
                                            {            
                                            $.ajax({
                                                url: "${pageContext.request.contextPath}/ajax/json/get-services.jsp",
                                                type: "GET",
                                                async: true,
                                                dataType: 'json',
                                                data: {
                                                    centroRicerca: centroRicerca,
                                                    raggioServizi: raggioServizi,
                                                    raggioSensori: raggioSensori,
                                                    raggioBus: raggioBus,
                                                    categorie: categorie,
                                                    textFilter: textFilter,
                                                    numeroRisultatiServizi: numeroRisultatiServizi,
                                                    numeroRisultatiSensori: numeroRisultatiSensori,
                                                    cat_servizi: tipo_categoria,
                                                    numeroRisultatiBus: numeroRisultatiBus,
                                                    value_type: value_type
                                                },
                                                success: function (msg_response) {
                                                    if (mode == "JSON") {
                                                        $("#body").html(JSON.stringify(msg_response));
                                                    }
                                                    else {
                                                        var nServices=0;
                                                        if(msg_response.Services!=undefined)
                                                          nServices=msg_response.Services.features.length                                                        
                                                        $('#loading').hide();//TODO
                                                        var msg = {
                                                            "fullCount": 0,
                                                            "type": "FeatureCollection",
                                                            "features": []
                                                        };
                                                        
                                                        // conversione JSON restituito dalle API a JSON utilizzato in precedenza
                                                        if(msg_response != null){
                                                        if ((msg_response.BusStops != null) &&(msg_response.BusStops.features.length != 0)) {
                                                            msg.features = msg.features.concat(msg_response.BusStops.features);
                                                            msg.fullCount = msg.fullCount + msg_response.BusStops.fullCount;
                                                            if(tipo_categoria == "categorie_t"){
                                                                numTotBusStops = msg_response.BusStops.fullCount;
                                                            }
                                                        }
                                                        if ((msg_response.SensorSites != null) && (msg_response.SensorSites.features.length != 0)) {
                                                            msg.features = msg.features.concat(msg_response.SensorSites.features);
                                                            msg.fullCount = msg.fullCount + msg_response.SensorSites.fullCount;
                                                            if(tipo_categoria == "categorie_t"){
                                                                numTotSensors = msg_response.SensorSites.fullCount;
                                                            }
                                                        }
                                                        if ((msg_response.Services != null) && (msg_response.Services.features.length != 0)) {
                                                            msg.features = msg.features.concat(msg_response.Services.features);
                                                            msg.fullCount = msg.fullCount + msg_response.Services.fullCount;
                                                            if(tipo_categoria == "categorie_t"){
                                                                numTotServices = msg_response.Services.fullCount;
                                                            }
                                                        }
                                                        }
                                                        for (var k=0; k<msg.features.length; k++){
                                                            msg.features[k].id = k+1;
                                                        }    
                                                                
                                                        numTotRisultati = msg.fullCount;
                                                        if (msg != null && msg.features.length > 0) {                                                            
                                                            servicesLayer = L.geoJson(msg, {
                                                                pointToLayer: function (feature, latlng) {
                                                                    marker = showmarker(feature, latlng);
                                                                    return marker;
                                                                },
                                                                onEachFeature: function (feature, layer) {
                                                                    // codice per apertura all'avvio di percorsi e aree.
                                                                    var contenutoPopup = "";
                                                                    var divId = feature.id + "-" + feature.properties.tipo;
                                                                    contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                                                    layer.bindPopup(contenutoPopup);
                                                                    if (feature.properties.serviceType == "TransferServiceAndRenting_BusStop") {
                                                                        numeroBus++;
                                                                    }
                                                                    else {
                                                                        if (feature.properties.serviceType == "TransferServiceAndRenting_SensorSite") {
                                                                            numeroSensori++;
                                                                        }
                                                                        else {
                                                                            numeroServizi++;
                                                                        }
                                                                    }
                                                                }
                                                            })
            <%if (clusterResults > 0) {%>
                                                            if (msg.features.length >=<%=clusterResults%>) {
                                                                markers = new L.MarkerClusterGroup({maxClusterRadius: <%=clusterDistance%>, disableClusteringAtZoom: <%=noClusterAtZoom%>});
                                                                servicesLayer = markers.addLayer(servicesLayer);
                                                                //$("#cluster-msg").text("più di <%=clusterResults%> risultati, attivato clustering");
                                                                $("#cluster-msg").text("more than <%=clusterResults%> results, clustering enabled");
                                                                $("#cluster-msg").show();
                                                            }
                                                            else
                                                                $("#cluster-msg").hide();
            <%}%>
                                                            servicesLayer.addTo(map);
                                                            if (mode == "embed") {
                                                                for (i in servicesLayer._layers) {
                                                                    var uri = servicesLayer._layers[i].feature.properties.serviceUri;
                                                                    if (include(openPins, uri))
                                                                        servicesLayer._layers[i].openPopup();
                                                                }
                                                            }
                                                            var markerJson = JSON.stringify(msg.features);
                                                            pins = markerJson;

                                                            if (raggioServizi != "area") {
                                                                var confiniMappa = servicesLayer.getBounds();
                                                                map.fitBounds(confiniMappa, {padding: [50, 50]});
                                                            }
                                                            //var nResults = numeroRisultatiServizi + numeroRisultatiSensori + numeroRisultatiBus;
                                                            /*if (msg.features.length < nResults || nResults == 0) {
                                                             risultatiRicerca(numeroServizi, numeroBus, numeroSensori, 0);
                                                             }
                                                             else {
                                                             risultatiRicerca(numeroServizi, numeroBus, numeroSensori, 0);
                                                             }*/
                                                            if (tipo_categoria == "categorie" || tipo_categoria.indexOf("categorie:") != -1) {
                                                                risultatiRicerca((numeroServizi + numeroBus + numeroSensori), 0, 0, 1, null, numTotRisultati, numTotBusStops, numTotSensors);
                                                            } else {
                                                                //risultatiRicerca(numeroServizi, numeroBus, numeroSensori, 2); //DA DECOMMENTARE QUANDO SISTEMATI TRANSVERSE SERVICE
                                                                risultatiRicerca(numeroServizi, numeroBus, numeroSensori, 1, null, numTotServices, numTotBusStops, numTotSensors);
                                                            }
                                                        }
                                                        else {
                                                            if (categorie.indexOf("PublicTransportLine") == -1) {
                                                                risultatiRicerca(0, 0, 0, 0, null,0, 0, 0);
                                                            }
                                                            if (raggioServizi != "area") {
                                                                map.fitBounds(clickLayer.getLayers()[1].getBounds());
                                                            }
                                                        }
                                                    }
                                                    if (categorie.indexOf("Event") != -1) {
                                                        numEventi = searchEvent("transverse", raggioServizi, null/*centroRicerca*/, numeroRisultatiServizi, textFilter);
                                                        if (numEventi != 0) {
                                                            risultatiRicerca((numeroServizi + numEventi), numeroBus, numeroSensori, 1, null, (numTotServices + numEventi), numTotBusStops, numTotSensors);
                                                            $("input[name=event_choice][value=day]").attr('checked', 'checked');
                                                        }
                                                    }

                                                },
                                                error: function (request, status, error) {
                                                    $('#loading').hide();//TODO
                                                    console.log(error);
                                                    risultatiRicerca(0, 0, 0, 1, null, 0, 0, 0);
                                                    if(request.responseText.indexOf("exceeds the limit")!=-1) {
                                                      alert("Sorry, the query is too complex please reduce complexity.");
                                                    }
                                                }
                                            });
                                            }//MICHELA
                                            if (categorie.indexOf("PublicTransportLine") != -1) {
                                                var i = 0;
                                                $.ajax({
                                                    url: "${pageContext.request.contextPath}/ajax/json/get-nearTPL.jsp",
                                                    type: "GET",
                                                    async: true,
                                                    dataType: 'json',
                                                    data: {
                                                        centro: centroRicerca,
                                                        raggio: raggioServizi,
                                                        numLinee: numeroRisultatiServizi
                                                    },
                                                    success: function (msg) {
                                                        if (mode == "JSON") {
                                                            $("#body").html(JSON.stringify(msg));
                                                        }
                                                        else {
                                                            $('#loading').hide();//TODO                                                            
                                                            if (msg != null && msg.PublicTransportLine.features.length > 0) {
                                                                numLineeBus = msg.PublicTransportLine.features.length;
                                                                for (i = 0; i < msg.PublicTransportLine.features.length; i++) {
                                                                    var agency = msg.PublicTransportLine.features[i].properties.agency;
                                                                    var nomeLinea = agency + " - Line: "+msg.PublicTransportLine.features[i].properties.lineNumber +" " +msg.PublicTransportLine.features[i].properties.lineName
                                                                    showLinea(msg.PublicTransportLine.features[i].properties.lineNumber, msg.PublicTransportLine.features[i].properties.routeUri, msg.PublicTransportLine.features[i].properties.direction, nomeLinea, "transverse", msg.PublicTransportLine.features[i].properties.polyline,i);
                                                                }
                                                                
                                                                //risultatiRicerca(msg.PublicTransportLine.features.length+numeroServizi, numeroBus, numeroSensori, 1);
                                                                $('#resultTPL').show();
                                                                var template = "{{#features}}" +
                                                                        "<div class=\"tplItem\" id=\"route_ATAF_{{properties.route}}\" style=\"margin-top:5px; border:1px solid #000; padding:6px; overflow:auto;\" onmouseover=\"selectRoute({{properties.routeUri}})\" onmouseout=\"deselectRoute({{properties.routeUri}})\">\n\
                                                                        <div class=\"tplName\"><b style=\"color:#B500B5;\"><b>TPL Line:</b> {{properties.lineName}}</b></div>" +
                                                                        "<div class=\"tplDirection\" style=\"float:left; margin-top:7px; display:block; width:85%;\"><b>Direction:</b> {{properties.direction}}<br></div></div>" +
                                                                        "{{/features}}";
                                                                var output = Mustache.render(template, msg.PublicTransportLine);
                                                                document.getElementById('listTPL').innerHTML = output;
                                                                $(".circle.leaflet-clickable").css({stroke: "#0c0", fill: "#0c0"});
                                                                $('#numTPL').html(numLineeBus + " Public Transport Lines Found.");
                                                                $('#numTPL').show();
                                                                if (($('#msg').text().indexOf('not results') != -1) || ($('#msg').text().indexOf('alcun risultato') != -1) || numeroServizi == 0) {
                                                                    $('#msg').html('');
                                                                    $('#searchOutput').hide();
                                                                }
                                                            } else {
                                                                if (categorie == "PublicTransportLine" || (numeroServizi == 0)) {
                                                                    risultatiRicerca(0, 0, 0, 0, null, 0, 0, 0);
                                                                }
                                                            }
                                                        }
                                                    }
                                                });
                                            }                                            
                                        }
                                        else {
                                            // caso tutte le fermate oppure ricerca per comune
                                            if (pins.lenght > 0)
                                                pins = "";
                                            if (mode == "query" || mode == "embed") {
                                                var nomeComune = nomeComune;
                                                $('#loading').show();
                                            }
                                            else
                                                var nomeComune = $("#elencocomuni").val();
                                            //qui devo cambiare le condizioni   
                                            
                                            //if (selezione == "" || (selezione != null && selezione.indexOf("COMUNE di") != -1)) {
                                            if ((selezione == "" && categorie != 'BusStop') || (selezione != null && selezione.indexOf("COMUNE di") != -1)) {
                                                var provincia = $("#elencoprovince").val();
                                                var comune = $("#elencocomuni").val();
                                                query = saveQueryMunicipality(provincia, comune, categorie, numRis, selezione);
                                                var numeroServizi = 0;
                                                var numeroBus = 0;
                                                var numeroSensori = 0;
                                                var numEventi = 0;
                                                $.ajax({
                                                    url: ctx + "/ajax/json/get-services-in-municipality.jsp",
                                                    type: "GET",
                                                    async: true,
                                                    dataType: 'json',
                                                    data: {
                                                        nomeProvincia: provincia,
                                                        nomeComune: nomeComune,
                                                        categorie: categorie,
                                                        textFilter: textFilter,
                                                        numeroRisultatiServizi: numeroRisultatiServizi,
                                                        numeroRisultatiSensori: numeroRisultatiSensori,
                                                        numeroRisultatiBus: numeroRisultatiBus,
                                                        cat_servizi: tipo_categoria
                                                    },
                                                    success: function (msg) {
                                                        //console.log(msg);
                                                        if (mode == "JSON") {
                                                            $("#body").replaceWith(JSON.stringify(msg));
                                                        }
                                                        else {
                                                            if ($("#elencocomuni").val() != 'all') {
                                                                //if (selectOption.options[selectOption.options.selectedIndex].value != 'all'){
                                                                /*
                                                                 $.ajax({
                                                                 url : "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                                                 type : "GET",
                                                                 async: true,
                                                                 //dataType: 'json',
                                                                 data : {
                                                                 nomeComune: $("#elencocomuni").val()
                                                                 },
                                                                 success : function(msg) {
                                                                 $('#info-aggiuntive .content').html(msg);
                                                                 }
                                                                 });
                                                                 */
                                                            }

                                                            $('#loading').hide();
                                                            if (msg.features.length > 0) {
                                                                servicesLayer = L.geoJson(msg, {
                                                                    pointToLayer: function (feature, latlng) {
                                                                        marker = showmarker(feature, latlng);
                                                                        return marker;
                                                                    },
                                                                    onEachFeature: function (feature, layer) {
                                                                        var contenutoPopup = "";
                                                                        var divId = feature.id + "-" + feature.properties.tipo;
                                                                        // X TEMPI DI CARICAMENTO INFO SCHEDA ALL'APERTURA DEL POPUP LUNGHI, VISUALIZZARE NOME, LOD E TIPOLOGIA DI SERVIZIO
                                                                        /*if (feature.properties.nome != null && feature.properties.nome != "")
                                                                         contenutoPopup = "<h3>" + feature.properties.nome + "</h3>";
                                                                         else {
                                                                         if (feature.properties.identifier != null && feature.properties.identifier != "")
                                                                         contenutoPopup = "<h3>" + feature.properties.identifier + "</h3>";
                                                                         }
                                                                         contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + feature.properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
                                                                         contenutoPopup = contenutoPopup + "<b>Tipologia: </b>" + feature.properties.category +" - "+ feature.properties.subCategory + "<br />";*/
                                                                        contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                                                        layer.bindPopup(contenutoPopup);
                                                                        if (feature.properties.serviceType == "TransferServiceAndRenting_BusStop") {
                                                                            numeroBus++;
                                                                        }
                                                                        else {
                                                                            if (feature.properties.serviceType == "TransferServiceAndRenting_SensorSite") {
                                                                                numeroSensori++;
                                                                            }
                                                                            else {
                                                                                numeroServizi++;
                                                                            }
                                                                        }

                                                                    }
                                                                });
            <%if (clusterResults > 0) {%>
                                                                if (msg.features.length >=<%=clusterResults%>) {
                                                                    markers = new L.MarkerClusterGroup({maxClusterRadius: <%=clusterDistance%>, disableClusteringAtZoom: <%=noClusterAtZoom%>});
                                                                    servicesLayer = markers.addLayer(servicesLayer);
                                                                    //$("#cluster-msg").text("più di <%=clusterResults%> risultati, attivato clustering");
                                                                    $("#cluster-msg").text("more than <%=clusterResults%> results, clustering enabled");
                                                                    $("#cluster-msg").show();
                                                                }
                                                                else
                                                                    $("#cluster-msg").hide();
            <%}%>
                                                                servicesLayer.addTo(map);
                                                                if (mode == "embed") {
                                                                    for (i in servicesLayer._layers) {
                                                                        var uri = servicesLayer._layers[i].feature.properties.serviceUri;
                                                                        if (include(openPins, uri))
                                                                            servicesLayer._layers[i].openPopup();
                                                                    }
                                                                }
                                                                var markerJson = JSON.stringify(msg.features);
                                                                pins = markerJson;
                                                                //if (mode != "embed") {
                                                                var confiniMappa = servicesLayer.getBounds();
                                                                map.fitBounds(confiniMappa, {padding: [50, 50]});
                                                                if (tipo_categoria == "categorie" || tipo_categoria.indexOf("categorie:") != -1) {
                                                                    risultatiRicerca((numeroServizi + numeroBus + numeroSensori), 0, 0, 1, null, 0, 0, 0);
                                                                } else {
                                                                    risultatiRicerca(numeroServizi, numeroBus, numeroSensori, 1, null, 0, 0, 0);
                                                                }
                                                            }
                                                            else {
                                                                risultatiRicerca(0, 0, 0, 0, null, 0, 0, 0);
                                                            }
                                                        }
                                                        if (categorie.indexOf("Event") != -1) {
                                                            numEventi = searchEvent("transverse", null, null, numeroRisultatiServizi, textFilter);
                                                            if (numEventi != 0) {
                                                                risultatiRicerca((numeroServizi + numEventi), numeroBus, numeroSensori, 1, null, 0, 0, 0);
                                                                $("input[name=event_choice][value=day]").attr('checked', 'checked');
                                                            }
                                                        }
                                                    },
                                                    error: function (request, status, error) {
                                                        if ($("#elencocomuni").val() != 'all') {
                                                            $.ajax({
                                                                url: "${pageContext.request.contextPath}/ajax/get-weather.jsp",
                                                                type: "GET",
                                                                async: true,
                                                                //dataType: 'json',
                                                                data: {
                                                                    nomeComune: $("#elencocomuni").val()
                                                                },
                                                                success: function (msg) {
                                                                    $('#info-aggiuntive .content').html(msg);
                                                                }
                                                            });
                                                        }
                                                        $('#loading').hide();
                                                        console.log(error);
                                                        alert('Si è verificato un errore');
                                                    }
                                                });
                                            }/*
                                            else if(selezione == "" && categorie == 'BusStop'){
                                                  risultatiRicerca(0, 0, 0, 0, null, 0, 0, 0);//DA CAMBIARE
                                            }*/
                                            
                                            if (selezione != null && (selezione.indexOf("Linea Bus:") != -1 || selezione.indexOf("Bus Line:") != -1)) {
                                                $.ajax({
                                                    url: "${pageContext.request.contextPath}/ajax/json/get-services-near-stops.jsp",
                                                    type: "GET",
                                                    async: true,
                                                    dataType: 'json',
                                                    data: {
                                                        nomeLinea: $('#elencolinee')[0].options[$('#elencolinee')[0].options.selectedIndex].value,
                                                        raggio: 100,
                                                        categorie: categorie,
                                                        numerorisultati: 100
                                                    },
                                                    success: function (msg) {
                                                        $('#loading').hide();
                                                        var i = 0;
                                                        if (msg.features.length > 0) {
                                                            servicesLayer = L.geoJson(msg, {
                                                                pointToLayer: function (feature, latlng) {
                                                                    marker = showmarker(feature, latlng);
                                                                    return marker;
                                                                },
                                                                onEachFeature: function (feature, layer) {
                                                                    var divId = feature.id + "-" + feature.properties.tipo;
                                                                    var divLinee = divId + "-linee";
                                                                    // contenutoPopup="<div id=\""+divId+"\" >";
                                                                    if (feature.properties.tipo == "fermata")
                                                                        contenutoPopup = "<h3> BUS STOP: " + feature.properties.name + "</h3>";
                                                                    else
                                                                        contenutoPopup = "<h3>" + feature.properties.name + "</h3>";
                                                                    contenutoPopup = contenutoPopup + "<a href='" + logEndPoint + feature.properties.serviceUri + "' title='Linked Open Graph' target='_blank'>LINKED OPEN GRAPH</a><br />";
                                                                    contenutoPopup = contenutoPopup + "Tipology: " + feature.properties.tipo + "<br />";
                                                                    if (feature.properties.email != "")
                                                                        contenutoPopup = contenutoPopup + "Email: " + feature.properties.email + "<br />";
                                                                    if (feature.properties.indirizzo != "")
                                                                        contenutoPopup = contenutoPopup + "Address: " + feature.properties.indirizzo + "<br />";
                                                                    if (feature.properties.note != "")
                                                                        contenutoPopup = contenutoPopup + "Note: " + feature.properties.note + "<br />";
                                                                    contenutoPopup += "<div id=\"" + divLinee + "\" ></div>";
                                                                    contenutoPopup = contenutoPopup + "<div id=\"" + divId + "\" ></div>";
                                                                    layer.bindPopup(contenutoPopup);
                                                                }
                                                            }).addTo(map);
                                                            var markerJson = JSON.stringify(msg.features);
                                                            pins = markerJson;
                                                            var confiniMappa = servicesLayer.getBounds();
                                                            map.fitBounds(confiniMappa, {padding: [50, 50]});
                                                            var nSer = (msg.features.length);
                                                            risultatiRicerca(msg.features.length, 0, 0, 1, null, 0, 0, 0);
                                                        }
                                                        else {
                                                            risultatiRicerca(0, 0, 0, 0, null, 0, 0, 0);
                                                        }
                                                    },
                                                    error: function (request, status, error) {
                                                        $('#loading').hide();
                                                        console.log(error);
                                                        alert('Si è verificato un errore');
                                                    }
                                                });
                                            }
                                        }
                                    }
                                    $('.gps-button').click(function () {
                                        if (GPSControl._isActive == true) {
                                            selezione = 'Posizione Attuale';
                                            $('#selezione').html(selezione);
                                            coordinateSelezione = "Posizione Attuale";
                                            $('#raggioricerca').prop('disabled', false);
                                            $('#raggioricerca_t').prop('disabled', false);
                                            $('#numerorisultati').prop('disabled', false);
                                            $('#PublicTransportLine').prop('disabled', false);
                                        }
                                    });
                                    $('#info img').click(function () {
                                        if ($("#info").hasClass("active") == false) {
                                            $('#info').addClass("active");
                                            selezioneAttiva = true;
                                        }
                                        else {
                                            $('#info').removeClass("active");
                                            selezioneAttiva = false;
                                        }
                                    });
                                    $('#choosePosition').click(function () {
                                        if ($("#choosePosition").hasClass("active") == false) {
                                            $('#choosePosition').addClass("active");
                                            selezioneAttiva = true;
                                        }
                                        else {
                                            $('#choosePosition').removeClass("active");
                                            selezioneAttiva = false;
                                            clickLayer.clearLayers();
                                        }
                                    });
        </script>
    </body><div id="overMap"></div>
</html>