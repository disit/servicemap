/*
 * Leaflet Gps Control 1.1.0
 * http://labs.easyblog.it/maps/leaflet-gps
 *
 * https://github.com/stefanocudini/leaflet-gps
 * https://bitbucket.org/zakis_/leaflet-gps 
 *
 * Copyright 2013, Stefano Cudini - stefano.cudini@gmail.com
 * Licensed under the MIT license.
 */

(function() {

L.Control.Gps = L.Control.extend({

	includes: L.Mixin.Events, 
	//
	//Managed Events:
	//	Event			Data passed			Description
	//	gpslocated		{latlng, marker}	fired after gps marker is located
	//	gpsdisabled							fired after gps is disabled
	//
	//Methods exposed:
	//	method 			Description
	//  getLocation		return Latlng and marker of current position
	//  activate		active tracking on runtime
	//  deactivate		deactive tracking on runtime
	//
	options: {		
		autoActive: false,		//activate control at startup
		autoTracking: false,	//move map when gps location change
		maxZoom: null,			//max zoom for autoTracking
		marker: null,			//L.Marker used for location, default use a L.CircleMarker
		textErr: null,			//error message on alert notification
		callErr: null,			//function that run on gps error activating
		style: {radius: 16,		//marker circle style
				weight:3,
				color: '#e03',
				fill: false},
		title: 'Center map on your location',
		position: 'topleft'
		//TODO add gpsLayer
		//TODO timeout autoTracking		
	},

	initialize: function(options) {
		if(options && options.style)
			options.style = L.Util.extend({}, this.options.style, options.style); 
		L.Util.setOptions(this, options);
		this._errorFunc = this.options.callErr || this.showAlert;
		this._isActive = false;//global state of gps
		this._currentLocation = null;	//store last location
	},
	
    onAdd: function (map) {
    
    	this._map = map;	
        	
        var container = L.DomUtil.create('div', 'leaflet-control-gps');

        this._button = L.DomUtil.create('a', 'gps-button', container);
        this._button.href = '#';
        this._button.title = this.options.title;
		L.DomEvent
			.on(this._button, 'click', L.DomEvent.stop, this)
			.on(this._button, 'click', this._switchGps, this);

		this._alert = L.DomUtil.create('div', 'gps-alert', container);
		this._alert.style.display = 'none';

		this._gpsMarker = this.options.marker ? this.options.marker : new L.CircleMarker([0,0], this.options.style);
		this._map.addLayer( this._gpsMarker );
		
		this._map
			.on('locationfound', this._drawGps, this)
			.on('locationerror', this._errorGps, this);	
			
		if(this.options.autoActive)
			this.activate();

        return container;
    },
    
	onRemove: function(map) {
		this.deactivate();
	},
	
	_switchGps: function() {
		if(this._isActive)
			this.deactivate();
		else
			this.activate();
	},
	
	getLocation: function() {	//get last location
		return this._currentLocation;
	},
    
    activate: function() {
	    this._isActive = true;
	    this._map.locate({
	        enableHighAccuracy: true,
			watch: this.options.autoTracking,
			//maximumAge:s
	        setView: false,	//automatically sets the map view to the user location
			maxZoom: this.options.maxZoom   
	    });	    
    },
    
    deactivate: function() {
   		this._isActive = false;    
		this._map.stopLocate();
    	L.DomUtil.removeClass(this._button, 'active');
		this._gpsMarker.setLatLng([-90,0]);  //move to antarctica!
		//TODO make method .hide() using _icon.style.display = 'none'
		this.fire('gpsdisabled');
    },
    
    _drawGps: function(e) {
    	//TODO use e.accuracy for gps circle radius/color
    	this._currentLocation = e.latlng;
    	
    	//TODO add new event here
    	
    	if(this.options.autoTracking || this._isActive)
			this._moveTo(e.latlng);
			
    	this._gpsMarker.setLatLng(e.latlng);
//    	if(this._gpsMarker.accuracyCircle)
//    		this._gpsMarker.accuracyCircle.setRadius((e.accuracy / 2).toFixed(0));
    		
    	this.fire('gpslocated', {latlng: e.latlng, marker: this._gpsMarker});
    	
    	L.DomUtil.addClass(this._button, 'active');	
    },
    
    _moveTo: function(latlng) {
    	
		if(this.options.maxZoom)
			this._map.setView(latlng, Math.max(this._map.getZoom(), this.options.maxZoom) );
		else
			this._map.panTo(latlng);    
    },
    
    _errorGps: function(e) {
    	this.deactivate();
    	this._errorFunc.call(this, this.options.textErr || e.message);
    },
    
	showAlert: function(text) {
		this._alert.style.display = 'block';
		this._alert.innerHTML = text;
		var that = this;
		clearTimeout(this.timerAlert);
		this.timerAlert = setTimeout(function() {
			that._alert.style.display = 'none';
		}, 2000);
	}
});

L.control.gps = function (options) {
    return new L.Control.Gps(options);
};

}).call(this);
