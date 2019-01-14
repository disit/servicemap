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

package org.disit.servicemap.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.disit.servicemap.ServiceMap;

import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class TplLocation {

	String tripUri;
	RepositoryConnection con;

	public TplLocation(String TripURI, RepositoryConnection Con) {
		this.tripUri = TripURI;
		this.con = Con;
	}

	// This method returns the segment length list given the geometry between
	// them
	private ArrayList<String> getSegLen(String wkt) throws ParseException {

		Coordinate[] coords;
		double lon1, lat1, lon2, lat2;
		ArrayList<String> lst = new ArrayList<>();

		try {
			coords = convertWktToCoords(wkt);
			for (int j = 0; j < coords.length - 1; j++) {
				// get lon lat of each two consecutive points in geometry
				lon1 = coords[j].x;
				lat1 = coords[j].y;

				lon2 = coords[j + 1].x;
				lat2 = coords[j + 1].y;

				double t1 = Math.toRadians(lat1);
				double t2 = Math.toRadians(lat2);
				double delLon = Math.toRadians(lon2 - lon1);
				lst.add(Double.toString(
						Math.acos(Math.sin(t1) * Math.sin(t2) + Math.cos(t1) * Math.cos(t2) * Math.cos(delLon))
								* 6371000));
			}

		} catch (Exception e) {
			System.out.println("interStopGeo: " + wkt);
		}
		return lst;
	}

	// This method returns the wkt between two consecutive stops
	private String getInterStopWkt(Coordinate s1, Coordinate s2, Coordinate[] c) throws Exception {

		int i = 0;
		String wkt = "";
		try {

			int foundFirst = 0;
			int foundLast = 0;

			foundFirst = getMinFirst(s1, c);

//				System.out.println("coords.length: " + coords.length);
			if (foundFirst == c.length - 2) {
				wkt = s1.x + " " + s1.y + "," + c[c.length - 1].x + " " + c[c.length - 1].y + "," + s2.x + " " + s2.y;
				// checkWkt(convertWktToCoords(wkt), getSegLen(wkt), foundFirst, foundLast,
				// first, last);
			} else if (foundFirst == c.length - 1) {
				wkt = s1.x + " " + s1.y + "," + s2.x + " " + s2.y;
			} else {

				foundLast = getMinLast(s2, getTncCoords(foundFirst, c));

				wkt = s1.x + " " + s1.y + ",";
				for (i = 0; i < foundLast; i++) {
					wkt += getTncCoords(foundFirst, c)[i].x + " " + getTncCoords(foundFirst, c)[i].y + ",";
				}
				wkt += s2.x + " " + s2.y;
				ArrayList<String> lstSegLen = getSegLen(convertCoordsToWkt(c));

				wkt = checkWkt(c, lstSegLen, foundFirst, foundFirst + foundLast + 1, s1, s2, wkt);
			}
		} catch (Exception e) {
			throw new Exception(e.toString());
		}
		return wkt;
	}

	// This method checks if the wkt between two stops is correct
	private String checkWkt(Coordinate[] c, ArrayList<String> lst, int foundFirst, int foundLast, Coordinate s1,
			Coordinate s2, String OrigWkt) throws ParseException {

		String wkt = "";
		for (int i = 0; i < lst.size() - 1; i++) {
			if ((Double.parseDouble(lst.get(i))) > 200) {
//				System.out.println("SegLen>200!");
				if (isBelong(s2, c[i], c[i + 1])) {
//					System.out.println("belong: true");

//					System.out.println("getDist(stop, getClosestPntLine(p1, p2, stop))"
//							+ getDist(s2, getClosestPntLine(c[i], c[i + 1], s2)));
//					System.out.println("getDist(stop, coords[foundIndx])" + getDist(s2, c[foundLast]));
					Coordinate p = getClosestPntLine(c[i], c[i + 1], s2);
					if (getDist(s2, p) < getDist(s2, c[foundLast])) {
//						System.out.println("Coords should be updated!");
						wkt = updateCoords(c, s1, foundFirst, p, i);
					} else {
						return OrigWkt;
					}
				} else {
					return OrigWkt;
				}
			} else {
				return OrigWkt;
			}
		}
		return wkt;
	}

	// This method returns the the coordination of a given point
	private Coordinate convertPointToCoord(String p) {

		return new Coordinate(Double.parseDouble(p.substring(6, p.indexOf(' '))),
				Double.parseDouble(p.substring(p.indexOf(' ') + 1, p.length() - 1)));

	}

	// This method returns the coordination of a given wkt
	private Coordinate[] convertWktToCoords(String wkt) throws ParseException {

		String lineStrWkt = "LINESTRING(" + wkt + ")";
		// System.out.println("Route: " + route);
		WKTReader wktReader = new WKTReader();
		Geometry g = wktReader.read(lineStrWkt);
		Coordinate[] coords = g.getCoordinates();

		return coords;
	}

	// This method returns the wkt of a given linestring coordination
	private String convertCoordsToWkt(Coordinate[] c) {

		String wkt = "";

		for (int i = 0; i < c.length; i++) {
			wkt += c[i].x + " " + c[i].y + ",";
		}

		wkt = wkt.substring(0, wkt.length() - 1);
		return wkt;
	}

	// This method checks if a stop belongs to a segment start and end with p1 and
	// p2
	private boolean isBelong(Coordinate stop, Coordinate p1, Coordinate p2) {

		Coordinate pointOnLine = new Coordinate();

		pointOnLine = getClosestPntLine(p1, p2, stop);

//		System.out.println(
//				"LINESTRING (" + p1.x + " " + p1.y + "," + stop.x + " " + stop.y + "," + p2.x + " " + p2.y + ")");

//		System.out.println("MULTIPOLYGON (" + p1.x + " " + p1.y + "," + pointOnLine.x + " " + pointOnLine.y + ","
//				+ stop.x + " " + stop.y + "," + p2.x + " " + p2.y + ")");
		double dist = getDist(pointOnLine, stop);
		if (dist < 10) {
//			System.out.println("getDist(pointOnLine, stop):" + getDist(pointOnLine, stop));
			return true;

		} else {
//			System.out.println("getDist(pointOnLine, stop):" + getDist(pointOnLine, stop));
			return false;
		}

	}

	// This method returns the coordinate of the closest point on the line which
	// connects p1 and p2 to stopGeo
	private Coordinate getClosestPntLine(Coordinate p1, Coordinate p2, Coordinate stop) {

		double xDelta = p2.x - p1.x;
		double yDelta = p2.y - p1.y;

		if (p1 == p2) {
			System.out.println("points are teh same!");
		}
		double u = ((stop.x - p1.x) * xDelta + (stop.y - p1.y) * yDelta) / (xDelta * xDelta + yDelta * yDelta);
		Coordinate closestPoint;
		if (u < 0) {
			closestPoint = new Coordinate(p1.x, p1.y);
		} else if (u > 1) {
			closestPoint = new Coordinate(p2.x, p2.y);
		} else {
			closestPoint = new Coordinate(p1.x + u * xDelta, p1.y + u * yDelta);
			// System.out.println("closestPoint:" + closestPoint);
		}
		// System.out.println("closestPoint:" + closestPoint);
		return closestPoint;
	}

	// This method returns the distance between p1 and p2
	private double getDist(Coordinate p1, Coordinate p2) {

		double lon1 = p1.x;
		double lat1 = p1.y;

		double lon2 = p2.x;
		double lat2 = p2.y;

		double t1 = Math.toRadians(lat1);
		double t2 = Math.toRadians(lat2);
		double delLon = Math.toRadians(lon2 - lon1);
		double dist = 0;
		dist = Math.acos(Math.sin(t1) * Math.sin(t2) + Math.cos(t1) * Math.cos(t2) * Math.cos(delLon)) * 6371000;
		// System.out.println("getDist:"
		// + Math.acos(Math.sin(t1) * Math.sin(t2) + Math.cos(t1) * Math.cos(t2) *
		// Math.cos(delLon)) * 6371000);
		// System.out.println("dist:" + dist);
		return dist;
	}

	// This method returns the updates the wkt between two stops
	private String updateCoords(Coordinate[] c, Coordinate stop, int foundFirst, Coordinate pntOnline, int lastPoint)
			throws ParseException {

		String wkt = stop.x + " " + stop.y + ",";
		for (int i = foundFirst + 1; i <= lastPoint; i++) {
			wkt += c[i].x + " " + c[i].y + ",";
		}
		wkt += pntOnline.x + " " + pntOnline.y;
		return wkt;
	}

	// This method returns the point in coords with minimum distance to a stop
	// (first)
	private int getMinFirst(Coordinate stop, Coordinate[] c) {

		int foundFirst = 0;
		double dFirst = 0;
		double firstMinDist;
		firstMinDist = getDist(c[0], stop);

		for (int i = 1; i < c.length; i++) {
			dFirst = getDist(c[i], stop);
			// System.out.println(dFirst);
			if (dFirst < firstMinDist) {
				foundFirst = i;
				firstMinDist = dFirst;
			}
		}
		return foundFirst;
	}

	// This method returns the truncated coordinate of trip starting from indx
	private Coordinate[] getTncCoords(int indx, Coordinate[] c) throws ParseException {

		String wktTrunc = "";
		// wktTrunc += first.x + " " + first.y + ",";
		for (int i = indx + 1; i < c.length; i++) {
			wktTrunc += c[i].x + " " + c[i].y + ",";
		}

		String wktTruncLineString = "LINESTRING(" + wktTrunc.substring(0, wktTrunc.length() - 1) + ")";
//		System.out.println("wktOriginal length: " + coords.length);
//		System.out.println("wktTrunc: " + wktTruncLineString);
//		System.out.println("foundFirst: " + foundFirst);
//		System.out.println("first: " + first.x + " " + first.y);
//		System.out.println("last: " + last.x + " " + last.y);
//		System.out.println("j: " + j);

		WKTReader wktReader = new WKTReader();
		Geometry g = wktReader.read(wktTruncLineString);
		Coordinate[] coords = g.getCoordinates();

		return coords;

	}

	// This method returns a point in coords with minimum distance to stop last
	private int getMinLast(Coordinate p, Coordinate[] c) throws ParseException {

		int foundLast = 0;
		double lastMinDist = getDist(c[0], p);

		for (int i = 1; i < c.length; i++) {
			double dLast = getDist(c[i], p);

			if (dLast < lastMinDist) {
				foundLast = i;
				lastMinDist = dLast;
			}
		}
		return foundLast;
	}

	// This method returns the distance between two stop given the geometry between
	// them

	private double getInterStopDist(String wkt) throws ParseException {

		double lon1, lat1, lon2, lat2;
		double segLen;
		double t1, t2;
		Coordinate[] coords = null;

		double interStopDist = 0;
		// get the stop route between two stops

		coords = convertWktToCoords(wkt);

		for (int j = 0; j < coords.length - 1; j++) {
			// get lon lat of each two consecutive points in geometry
			lon1 = coords[j].x;
			lat1 = coords[j].y;

			lon2 = coords[j + 1].x;
			lat2 = coords[j + 1].y;

			t1 = Math.toRadians(lat1);
			t2 = Math.toRadians(lat2);
			double delLon = Math.toRadians(lon2 - lon1);
			segLen = Math.acos(Math.sin(t1) * Math.sin(t2) + Math.cos(t1) * Math.cos(t2) * Math.cos(delLon)) * 6371000;
			interStopDist += segLen;
		}
		return interStopDist;
	}

	// this method checks if consecutive stops have the same arrival time set
	private ArrayList<String> checkArrTime(ArrayList<String> lst) throws java.text.ParseException, ParseException {

		for (int i = 0; i < lst.size() - 3; i++) {
			// if the arrival time for four consecutive stops are the same
			if ((getElapsedTime(lst.get(i), lst.get(i + 1)) == 0)
					&& (getElapsedTime(lst.get(i + 1), lst.get(i + 2)) == 0)
					&& (getElapsedTime(lst.get(i + 2), lst.get(i + 3)) == 0)) {
				// set the second stop arrival time to 15 seconds later and the third to 30
				// seconds later and the fourth to 45 seconds later

				lst.set(i + 1, setTime(lst.get(i + 1), 15000));
				lst.set(i + 2, setTime(lst.get(i + 2), 30000));
				lst.set(i + 3, setTime(lst.get(i + 3), 45000));
				// if the arrival time for two consecutive stops are the same
			}
		}

		for (int i = 0; i < lst.size() - 2; i++) {
			// if the arrival time for tree consecutive stops are the same
			if ((getElapsedTime(lst.get(i), lst.get(i + 1)) == 0)
					&& (getElapsedTime(lst.get(i + 1), lst.get(i + 2)) == 0)) {
				// set the second stop arrival time to 20 seconds later and the third to 40
				// seconds later
				lst.set(i + 1, setTime(lst.get(i + 1), 20000));
				lst.set(i + 2, setTime(lst.get(i + 2), 40000));
				// if the arrival time for two consecutive stops are the same
			}
		}

		for (int i = 0; i < lst.size() - 1; i++) {
			// if the arrival time for tree consecutive stops are the same
			if (getElapsedTime(lst.get(i), lst.get(i + 1)) == 0) {
				// set the second stop arrival time to 30 seconds later
				lst.set(i + 1, setTime(lst.get(i + 1), 30000));
			}
		}
		return lst;
	}

	private String setTime(String time, long plus) throws java.text.ParseException {

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Date t = sdf.parse(time);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(t);
		// Set arrival time to 30 seconds later
		t.setTime(calendar.getTimeInMillis() + plus);

		return t.toString().substring(11, 19);
	}

	// This method creates the time list which takes that the bus moves between two
	// stops
	private double getInterStopTime(ArrayList<String> arrTime, int nxtStop)
			throws ParseException, java.text.ParseException {

		// get the time which takes that the bus moves between two stops
		double interStopTime = getElapsedTime(arrTime.get(nxtStop - 1), arrTime.get(nxtStop));

		if (interStopTime == 0) {
			return 30;
		} else {

			return interStopTime;
		}
	}

	// This method returns the elapsed time between t2 and t1
	private double getElapsedTime(String t1, String t2) throws ParseException, java.text.ParseException {

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Date d1 = sdf.parse(t1);
		Date d2 = sdf.parse(t2);
		// milliseconds
		long elapsed = d2.getTime() - d1.getTime();

		return elapsed / 1000;
	}

// This method returns the bearing between two points
	private double getBearing(Coordinate p1, Coordinate p2) {

		double lat1, lat2;
		double y, x;

		lat1 = Math.toRadians(p1.y);
		lat2 = Math.toRadians(p2.y);

		double lonDiff = Math.toRadians(p2.x - p1.x);

		y = Math.sin(lonDiff) * Math.cos(lat2);
		x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lonDiff);
		return Math.toDegrees(Math.atan2(y, x)) + 360;
	}

	// This method returns the next stop which the bus is going to reach to given
	// lst which is the arrival time of the bus at stops and current time t
	private int getNextStop(ArrayList<String> lst, String t) throws java.text.ParseException, ParseException {

		int i;
		for (i = 0; i < lst.size(); i++) {
			// find the just passed stop
			if (getElapsedTime(lst.get(i), t) < 0) {
				break;
			}
		}
		return i;
	}

	public Coordinate currentLoc(String t) throws Exception, ParseException, java.text.ParseException,
//	public String currentLoc(String t) throws Exception, ParseException, java.text.ParseException,
			RepositoryException, MalformedQueryException, QueryEvaluationException {
		// try {
		String interStopWkt;
		Coordinate currLoc = new Coordinate();
		int nxtStop, j = 0;
		// moved length
		double mov;
		double EARTH_RADIUS = 6371000;
		double latDest, lonDest;
		// elapsed time from the just passed stop
		double tx;
		String tripWkt = null;
		Geometry g = null;
		Coordinate[] coords = null;

		ArrayList<String> lstStopWkt = new ArrayList<>();
		double interStopDist;
		ArrayList<String> lstArrTime = new ArrayList<>();
		double interStopTime;
		double spd;
		ArrayList<String> lstSegLen = new ArrayList<>();

		String tripName = tripUri.substring(tripUri.indexOf("resource/") + 9, tripUri.length());

		String query = "select  ?geom  ?arrTime str(?wkt) as ?wkt {{" + "select  ?geom  ?arrTime ?wkt {" + 
            "<" + tripUri + "> <http://www.opengis.net/ont/geosparql#hasGeometry>/geo:geometry ?wkt."
            + "?x gtfs:trip ?t."
            + "?x gtfs:stop ?stop."
            + "?x gtfs:arrivalTime ?arrTime."
            + "?stop geo:geometry  ?geom."
				    + "?t dcterms:identifier" + " \"" + tripName + "\"." + "}order by ?arrTime}}";

    long ss=System.nanoTime();
		TupleQuery tqq = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
		TupleQueryResult data = (TupleQueryResult) tqq.evaluate();

		if (data.hasNext()) {
			BindingSet binding = data.next();
			tripWkt = binding.getValue("wkt").stringValue();
		}

		if (tripWkt != null) {
			WKTReader wktReader = new WKTReader();
			g = wktReader.read(tripWkt);
			coords = g.getCoordinates();
		}

		data = (TupleQueryResult) tqq.evaluate();

		while (data.hasNext()) {
			BindingSet binding = data.next();
			lstStopWkt.add(binding.getValue("geom").stringValue());
			lstArrTime.add(binding.getValue("arrTime").stringValue());
		}
		data.close();
    long se = System.nanoTime();
    ServiceMap.println("tpllocation "+tripUri+" "+t+" q: "+(se-ss)/1000000+"ms");

		// check arrival times
		lstArrTime = checkArrTime(lstArrTime);

		// find the next stop
		nxtStop = getNextStop(lstArrTime, t);
		interStopWkt = getInterStopWkt(convertPointToCoord(lstStopWkt.get(nxtStop - 1)),
				convertPointToCoord(lstStopWkt.get(nxtStop)), coords);
		Coordinate[] interStopCoords = convertWktToCoords(interStopWkt);

		lstSegLen = getSegLen(interStopWkt);
		interStopDist = getInterStopDist(interStopWkt);
		interStopTime = getInterStopTime(lstArrTime, nxtStop);
		spd = interStopDist / interStopTime;

		// if the current time is applicable to the route time table
		// if (IsCurrTimeApplicable(lstArrTime, currTime)) {

//		System.out.println("Stop1: " + (nextStop - 1));
//		System.out.println("Stop2: " + nextStop);
//		System.out.println("s1 geo: " + lstStopGeo.get(nextStop - 1));
//		System.out.println("s2 geo: " + lstStopGeo.get(nextStop));
//		System.out.println("wkt Original:  LINESTRING(" + convertCoordsToWkt(coords) + ")");
		// System.out.println("route between s1 and s2: LINESTRING(" + interStopWkt +
		// ")");

//		System.out.println("Distance: " + lstInterStopDist.get(nextStop - 1));
//		System.out.println("Speed: " + lstInterStopSpeed.get(nextStop - 1));
		// System.out.println("Speed: " + lstInterStopSpeed.get(nextStop - 1));

		// find the elapsed time from the just passed stop
		tx = getElapsedTime(lstArrTime.get(nxtStop - 1), t);
		// moved meters during the elapsed time, considering the bus speed
		mov = spd * tx;

//		for (int i = 0; i < lstInterStopSegLen.size(); i++) {
//			System.out.println("segLen " + i + ":" + lstInterStopSegLen.get(i));
//		}

		// get current location
		while (j < lstSegLen.size()) {
			// if the length of segment is more than the moved length
			if (Double.parseDouble(lstSegLen.get(j)) > mov) {
				// calculate bearing
				double b = getBearing(interStopCoords[j], interStopCoords[j + 1]);
				// get the current location

				// convert to radians
				double lat = Math.toRadians(interStopCoords[j].y);
				double lon = Math.toRadians(interStopCoords[j].x);
				b = Math.toRadians(b);
				latDest = Math.asin(Math.sin(lat) * Math.cos(mov / EARTH_RADIUS)
						+ Math.cos(lat) * Math.sin(mov / EARTH_RADIUS) * Math.cos(b));

				lonDest = lon + Math.atan2(Math.sin(b) * Math.sin(mov / EARTH_RADIUS) * Math.cos(lat),
						Math.cos(mov / EARTH_RADIUS) - Math.sin(lat) * Math.sin(latDest));

				currLoc.x = lonDest * (180 / Math.PI);
				currLoc.y = latDest * (180 / Math.PI);
				break;
			} else {
				// otherwise substract the segment length from the moved length
				mov -= Double.parseDouble(lstSegLen.get(j));
			}
			j++;
		}
		return currLoc;
//		return interStopWkt;
//		} catch (Exception e) {
//			throw new Exception("TplLocation: " + tripURI + " " + currTime + " " + e.toString() + "wkt: ");
//		}
	}

//	private int getClosestPnt(Coordinate[] coords, Coordinate point) {
//		double closest = getDist(coords[0], point);
//		int closestInd = 0;
//
//		for (int i = 0; i < coords.length; i++) {
//			if (getDist(coords[i], point) < closest) {
//				closest = getDist(coords[i], point);
//				closestInd = i;
//			}
//		}
//		// System.out.println("closestInd:" + closestInd);
//		return closestInd;
//	}

//	public int getSecClosestPnt(Coordinate[] coords, Coordinate point) {
//		int secClosestInd = 0;
//		double closest = 0;
//		double secClosest = 0;
//
//		for (int i = 0; i < coords.length; i++) {
//			for (int j = 0; j < coords.length; j++) {
//				if (getDist(coords[i], point) < closest) {
//					closest = getDist(coords[i], point);
//
//					if (getDist(coords[j], point) < secClosest) {
//						secClosest = getDist(coords[j], point);
//					}
//				}
//			}
//		}
//
//		for (int i = 0; i < coords.length; i++) {
//
//			if (secClosest == getDist(coords[i], point)) {
//				secClosestInd = i;
//			}
//		}
//		// System.out.println("secClosestInd:" + secClosestInd);
//		return secClosestInd;
//	}

//	private boolean IsCurrTimeApplicable(ArrayList<String> lstStopArrTime, String currTime)
//			throws ParseException, java.text.ParseException {
//
//		if ((getElapsedTime(lstStopArrTime.get(0), currTime) > 0)
//				&& (getElapsedTime(currTime, lstStopArrTime.get(lstStopArrTime.size() - 1)) > 0))
//			return true;
//		else
//			return false;
//	}

}