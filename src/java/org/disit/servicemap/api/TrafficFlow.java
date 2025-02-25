/* ServiceMap.
   Copyright (C) 2023 DISIT Lab http://www.disit.org - University of Florence

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.disit.servicemap.Configuration;
import org.disit.servicemap.ServiceMap;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.json.JSONObject;
import org.json.JSONArray;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 *
 * @author pierf
 */
public class TrafficFlow {

    public String trafficFlowSearch(String polygon, String dateObservedStart, String dateObservedEnd, String scenarioName,
            String roadElement, String kind) throws Exception {

        long startTime = System.currentTimeMillis();
        Configuration conf = Configuration.getInstance();
        RestHighLevelClient client = ServiceMap.createElasticSearchClient(conf);
        String indexName = conf.get("elasticSearchRoadElementIndex", "roadelement4");
        String sizeString = conf.get("elasticSearchRoadElementCount", "2000");

        int size = Integer.parseInt(sizeString);

        String query = constructQuery(size, polygon, dateObservedStart, dateObservedEnd, scenarioName, roadElement, kind);
        Response response = executeElasticsearchQuery(client, query, indexName);

        //System.out.println(query);
        if (response.getStatusLine().getStatusCode() == 200) {
            // Visualizza la risposta
            String responseBody = convertStreamToString(response.getEntity().getContent());
            long executionTime = System.currentTimeMillis() - startTime;
            String parsedResponse = parseResponse(responseBody, executionTime);
            // Visualizza la stringa della risposta
            return parsedResponse;
        } else {
            System.err.println("Errore nella risposta Elasticsearch: " + response.getStatusLine().getReasonPhrase());
            JSONObject tmp = new JSONObject("{time_query: 0, time_total: 0, status: 'ko', error: " + response.getStatusLine().getReasonPhrase() + ", result: []}");
            return tmp.toString();
        }

    }

    private String constructQuery(int size, String polygon, String dateObservedStart, String dateObservedEnd, String scenarioName,
            String roadElement, String kind) {
        // Costruisco progressivamente il json della query Elastic

        String query = "{"
                + "\"size\": " + size + ","
                + "\"query\": {"
                + "\"bool\": {"
                + "\"must\": [";

        if (dateObservedStart != null) {

            if (scenarioName == null && roadElement == null && kind == null) { // se è l'unico chiudo la clausola "must"
                query += "{"
                        + "\"range\": {"
                        + "\"dateObserved\": {"
                        + "\"gte\": \"" + dateObservedStart + "\","
                        + "\"lte\": \"" + dateObservedEnd + "\""
                        + "}"
                        + "}"
                        + "}]";
            } else {
                query += "{"
                        + "\"range\": {"
                        + "\"dateObserved\": {"
                        + "\"gte\": \"" + dateObservedStart + "\","
                        + "\"lte\": \"" + dateObservedEnd + "\""
                        + "}"
                        + "}"
                        + "},";
            }
        }

        if (scenarioName != null) {
            if (roadElement == null && kind == null) {
                query += "{"
                        + "\"match\": {"
                        + "\"scenario\": \"" + scenarioName + "\""
                        + "}"
                        + "}]";
            } else {
                query += "{"
                        + "\"match\": {"
                        + "\"scenario\": \"" + scenarioName + "\""
                        + "}"
                        + "},";
            }
        }

        if (kind != null) {
            if (roadElement == null) {
                query += "{"
                        + "\"match\": {"
                        + "\"kind\": \"" + kind + "\""
                        + "}"
                        + "}]";
            } else {
                query += "{"
                        + "\"match\": {"
                        + "\"kind\": \"" + kind + "\""
                        + "}"
                        + "},";
            }
        }

        if (roadElement != null) {
            query += "{"
                    + "\"match\": {"
                    + "\"roadElements\": \"" + roadElement + "\""
                    + "}}"
                    + "]";
        }

        if (polygon != null) {
            if (dateObservedStart == null && scenarioName == null && roadElement == null && kind == null) { // se è l'unicio non uso la
                // clausola "must"
                query = "{"
                        + "\"size\": " + size + ","
                        + "\"query\": {"
                        + "\"bool\": {"
                        + "\"filter\": ["
                        + "{"
                        + "\"geo_shape\": {"
                        + "\"line\": {"
                        + "\"shape\": \"" + polygon + "\","
                        + "\"relation\": \"intersects\""
                        + "}}}"
                        + "]";
            } else {
                query += ",\"filter\": ["
                        + "{"
                        + "\"geo_shape\": {"
                        + "\"line\": {"
                        + "\"shape\": \"" + polygon + "\","
                        + "\"relation\": \"intersects\""
                        + "}}}"
                        + "]";
            }
        }

        query += "}}}";

        return query;
    }

    private Response executeElasticsearchQuery(RestHighLevelClient client, String query,
            String indexName)
            throws IOException {
        Response response = null;

        String endpoint = "/" + indexName + "/_search";
        // Esegui la richiesta personalizzata POST
        Request request = new Request("POST", endpoint);
        request.setJsonEntity(query);

        // Esegui la richiesta Elasticsearch
        response = client.getLowLevelClient().performRequest(request);
        System.out.println("TF query: "+query);

        // Chiudi il client
        client.close();

        return response;
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                //System.out.println("linea: " + line);
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            ServiceMap.notifyException(e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                ServiceMap.notifyException(e);
            }
        }
        return sb.toString();
    }

    private String parseResponse(String response, long executionTime) throws Exception {

        JSONObject responseJson = new JSONObject(response);
        String template = "{time_query: 0, time_total: 0, status: 'ok', error: '', result: []}";
        JSONObject parsedResponse = new JSONObject(template);

        // estraggo i risultati
        JSONArray hitsArray = responseJson.getJSONObject("hits").getJSONArray("hits");
        JSONArray array = new JSONArray();

        // prendo solo i json contenenti i dati
        for (int i = 0; i < hitsArray.length(); i++) {
            JSONObject sourceObject = ((JSONObject) hitsArray.get(i)).getJSONObject("_source");
            array.put(sourceObject);
        }

        parsedResponse.put("time_query", responseJson.getInt("took"));
        parsedResponse.put("time_total", executionTime);
        parsedResponse.put("result", array);
        return parsedResponse.toString();
    }

    public static boolean wktValidator(String wktToValidate) {

        try {
            WKTReader reader = new WKTReader();

            Geometry geometry = reader.read(wktToValidate);

            return true;
        } catch (Exception e) {
            System.err.println("WKT is not valid: " + e.getMessage());
            return false;
        }
    }

    public static boolean isoDateValidator(String date) {
        DateTimeFormatter isoDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        Configuration conf = Configuration.getInstance();

        try {
            isoDateTimeFormatter.parse(date);
            return true;
        } catch (DateTimeParseException e) {
            // Se si verifica un'eccezione, la stringa non è nel formato ISO
            if(conf.get("trafficflowUseISOLocalDateTime", "false").equals("true")) {
                isoDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                try {
                    isoDateTimeFormatter.parse(date);
                    return true;
                } catch(DateTimeParseException ee) {
                    return false;
                }
            }
            return false;
        }
    }

    public static String isoDateDefault(String date) {
        Configuration conf = Configuration.getInstance();
        if (date != null  && conf.get("trafficflowUseISOLocalDateTime", "false").equals("false") && !date.contains("+") && !date.contains("Z")) {
            TimeZone timeZone = TimeZone.getTimeZone("Europe/Rome");
            
            String[] d = date.split("-");
            String year = d[0];
            String month = d[1];
            String day = d[2].split("T")[0];
                        
            Calendar calendar = Calendar.getInstance(timeZone);
            calendar.set(Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day), 0, 0, 0); // -1 al mese perchè gennaio corrisponde allo 0
            calendar.setTimeZone(timeZone);
       
            
            if (timeZone.inDaylightTime(calendar.getTime())){
                date = date + "+02:00";
                return date;
            }
            else{
                date = date + "+01:00";
                return date;
            }
        }
        return date;
    }

}
