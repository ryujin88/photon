package de.komoot.photon.nominatim.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.Arrays;
import java.util.Map;

import lombok.Data;

import static de.komoot.photon.Constants.STATE;

/**
 * representation of an address as returned by nominatim's get_addressdata PL/pgSQL function
 *
 * @author christoph
 */
@Data
public class AddressRow {

  private static final String[] CITY_PLACE_VALUES = {"city", "hamlet", "town", "village"}; // must be in alphabetic order to speed up lookup
  private static final String[] USEFUL_CONTEXT_KEYS = {"boundary", "landuse", "place"}; // must be in alphabetic order to speed up lookup
  // relation ids that are known to be cities, but cannot be deduced by mapping as they are mapped in a special way
  // see https://github.com/komoot/photon/issues/138
  private static final long[] CURATED_CITY_RELATION_IDS;

  static {
    final long[] ids = {62772l, 62654l, 62532l, 62407l, 62340l, 62525l, 62640l, 62646l, 62644l, 62508l, 62634l, 62470l, 62531l, 62658l, 62594l, 62717l, 62430l, 62581l, 62414l, 62526l, 1829065l, 191645l, 62456l, 62539l, 62370l, 62562l, 62745l, 62403l, 62713l, 62675l, 62573l, 62523l, 62400l, 62768l, 62374l, 62522l, 62671l, 1800297l, 62638l, 62499l, 285864l, 62751l, 62396l, 62589l, 62381l, 62693l, 62652l, 62518l, 62598l, 62349l, 62701l, 27021l, 62512l, 62578l, 62748l, 62391l, 62484l, 62649l, 62449l, 27027l, 62347l, 62481l, 62630l, 62691l, 62590l, 62410l, 62385l, 62428l, 62591l, 62528l, 62724l, 62780l, 62734l, 62695l, 62409l, 62631l, 62629l, 62471l, 62642l, 62369l, 62411l, 62455l, 2168233l, 62405l, 62659l, 62720l, 62534l, 62685l, 62699l, 62352l, 62636l, 2793104l, 62450l, 172679l, 62495l, 62554l, 62493l, 62496l, 62444l, 62418l, 62453l, 62478l, 62464l, 62719l, 62422l, 62559l, 62782l};
    Arrays.sort(ids);
    CURATED_CITY_RELATION_IDS = ids;
  }

  final Integer adminLevel;
  private final long placeId;
  private final Map<String, String> name;
  private final String osmKey;
  private final String osmValue;
  private final int rankAddress;
  private final String postcode;
  private final String place;
  private final String osmType;
  private final long osmId;

  public boolean isStreet() {
    return 26 <= rankAddress && rankAddress < 28;
  }

  public boolean isCity() {
    if ("place".equals(osmKey) && Arrays.binarySearch(CITY_PLACE_VALUES, osmValue) >= 0) {
      return true;
    }

    if (place != null && Arrays.binarySearch(CITY_PLACE_VALUES, place) >= 0) {
      return true;
    }

    return adminLevel != null && adminLevel == 8 && "boundary".equals(osmKey) && "administrative".equals(osmValue);

  }

  /**
   * whether address row was manually marked as city
   */
  public boolean isCuratedCity() {
    return "R".equals(osmType) && Arrays.binarySearch(CURATED_CITY_RELATION_IDS, osmId) >= 0;
  }

  public boolean isPostcode() {
    if ("place".equals(osmKey) && "postcode".equals(osmValue)) {
      return true;
    }

    return "boundary".equals(osmKey) && "postal_code".equals(osmValue);

  }

  public boolean hasPostcode() {
    return postcode != null; // TODO really null?
  }

  public boolean hasPlace() {
    return place != null;
  }

  public boolean isUsefulForContext() {
    if (name.isEmpty()) {
      return false;
    }

    if (isPostcode()) {
      return false;
    }

    if (isCuratedCity()) {
      // was already added to city
      return false;
    }

    if (rankAddress < 4) {
      // continent, sea, ...
      return false;
    }

    return Arrays.binarySearch(USEFUL_CONTEXT_KEYS, osmKey) >= 0;

  }

  public boolean isCountry() {
    if (adminLevel != null && adminLevel == 2 && "boundary".equals(osmKey) && "administrative".equals(osmValue)) {
      return true;
    }

    return "place".equals(osmKey) && "country".equals(osmValue);

  }

  public boolean isState() {
    if ("place".equals(osmKey) && STATE.equals(osmValue)) {
      return true;
    }

    return adminLevel != null && adminLevel == 4 && "boundary".equals(osmKey) && "administrative".equals(osmValue);

  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("placeId", placeId)
        .add("name", name)
        .add("osmKey", osmKey)
        .add("osmValue", osmValue)
        .toString();
  }
}
