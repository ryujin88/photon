package de.komoot.photon.utils;

import com.google.common.collect.Lists;

import de.komoot.photon.Constants;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Convert a elasticsearch {@link SearchResponse} into a list of {@link JSONObject}s
 * <p/>
 * Created by Sachin Dole on 2/20/2015.
 */
@Slf4j
public class ConvertToJson implements OneWayConverter<SearchResponse, List<JSONObject>> {

  private static final String[] KEYS_LANG_UNSPEC = {Constants.OSM_ID, Constants.OSM_VALUE, Constants.OSM_KEY, Constants.POSTCODE, Constants.HOUSENUMBER, Constants.OSM_TYPE};
  private static final String[] KEYS_LANG_SPEC = {Constants.NAME, Constants.COUNTRY, Constants.CITY, Constants.STREET, Constants.STATE};
  private final String lang;

  public ConvertToJson(final String lang) {
    this.lang = lang;
  }

  @Override
  public List<JSONObject> convert(final SearchResponse searchResponse) {
    final SearchHit[] hits = searchResponse.getHits().getHits();
    final List<JSONObject> list = Lists.newArrayListWithExpectedSize(hits.length);
    for (final SearchHit hit : hits) {
      final Map<String, Object> source = hit.getSourceAsMap();

      final JSONObject feature = new JSONObject();
      feature.put(Constants.TYPE, Constants.FEATURE);
      feature.put(Constants.GEOMETRY, getPoint(source));

      // populate properties
      final JSONObject properties = new JSONObject();

      // language unspecific properties
      for (final String key : KEYS_LANG_UNSPEC) {
        if (source.containsKey(key)) {
          properties.put(key, source.get(key));
        }
      }

      // language specific properties
      for (final String key : KEYS_LANG_SPEC) {
        if (source.containsKey(key)) {
          properties.put(key, getLocalised(source, key, lang));
        }
      }

      // add extent of geometry
      final Map<String, Object> extent = (Map<String, Object>) source.get("extent");
      if (extent != null) {
        final List<List<Double>> coords = (List<List<Double>>) extent.get("coordinates");
        final List<Double> nw = coords.get(0);
        final List<Double> se = coords.get(1);
        properties.put("extent", new JSONArray(Lists.newArrayList(nw.get(0), nw.get(1), se.get(0), se.get(1))));
      }

      feature.put(Constants.PROPERTIES, properties);

      list.add(feature);
    }
    return list;
  }

  private String getLocalised(final Map<String, Object> source, final String fieldName, final String lang) {
    final Map<String, String> map = (Map<String, String>) source.get(fieldName);
    if (map == null) {
      return null;
    }

    if (map.get(lang) != null) {
      // language specific field
      return map.get(lang);
    }

    return map.get("default");
  }

  private JSONObject getPoint(final Map<String, Object> source) {
    final JSONObject point = new JSONObject();

    final Map<String, Double> coordinate = (Map<String, Double>) source.get("coordinate");
    if (coordinate != null) {
      point.put(Constants.TYPE, Constants.POINT);
      point.put(Constants.COORDINATES, new JSONArray("[" + coordinate.get(Constants.LON) + "," + coordinate.get(Constants.LAT) + "]"));
    } else {
      log.error(String.format("invalid data [id=%s, type=%s], coordinate is missing!", source.get(Constants.OSM_ID), source.get(Constants.OSM_VALUE)));
    }

    return point;
  }
}
