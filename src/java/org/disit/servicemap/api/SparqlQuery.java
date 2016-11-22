/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.disit.servicemap.api;

/**
 *
 * @author bellini
 */
public interface SparqlQuery {
  String query(String type) throws Exception;
}
