package se.citerus.dddsample.domain.model.cargo;

import org.apache.commons.lang.Validate;
import se.citerus.dddsample.domain.model.handling.HandlingEvent;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.voyage.Voyage;
import se.citerus.dddsample.domain.shared.ValueObject;

import java.util.*;

/**
 * An itinerary.
 */
public class Itinerary implements ValueObject<Itinerary> {

  private List<Leg> legs = Collections.emptyList();

  static final Itinerary EMPTY_ITINERARY = new Itinerary();
  private static final Date END_OF_DAYS = new Date(Long.MAX_VALUE);

  /**
   * Constructor.
   *
   * @param legs List of legs for this itinerary.
   */
  public Itinerary(final List<Leg> legs) {
    Validate.notEmpty(legs);
    Validate.noNullElements(legs);

    // TODO
    // Validate that legs are in proper order, connected
    // and that load/unload times are doable?

    this.legs = legs;
  }

  public Itinerary(Leg... legs) {
    this(Arrays.asList(legs));
  }

  /**
   * @return the legs of this itinerary, as an <b>immutable</b> list.
   */
  public List<Leg> legs() {
    return Collections.unmodifiableList(legs);
  }

  /**
   * Test if the given handling event is expected when executing this itinerary.
   *
   * @param event Event to test.
   * @return <code>true</code> if the event is expected
   */
  public boolean isExpected(final HandlingEvent event) {
    if (isEmpty()) {
      return false;
    }

    if (event.type() == HandlingEvent.Type.RECEIVE) {
      return (firstLeg().loadLocation().equals(event.location()));
    }

    if (event.type() == HandlingEvent.Type.LOAD) {
      //Check that the there is a leg with same load location and voyage
      for (Leg leg : legs) {
        if (leg.loadLocation().sameIdentityAs(event.location()) &&
          leg.voyage().sameIdentityAs(event.voyage()))
          return true;
      }
      return false;
    }

    if (event.type() == HandlingEvent.Type.UNLOAD) {
      //Check that the there is a leg with same unload location and voyage
      for (Leg leg : legs) {
        if (leg.unloadLocation().sameIdentityAs(event.location()) &&
          leg.voyage().sameIdentityAs(event.voyage()))
          return true;
      }
      return false;
    }

    if (event.type() == HandlingEvent.Type.CLAIM) {
      //Check that the last leg's destination is from the event's location
      final Leg leg = lastLeg();
      return (leg.unloadLocation().equals(event.location()));
    }

    if (event.type() == HandlingEvent.Type.CUSTOMS) {
      //Check that the customs location fits the rule of the customs zone
      //TODO Answering this properly requires Cargo's destination. Can't be answered at itinerary level.
    }
    return false;

  }

  /**
   * @return The initial departure location.
   */
  Location initialDepartureLocation() {
    if (legs.isEmpty()) {
      return Location.UNKNOWN;
    } else {
      return legs.get(0).loadLocation();
    }
  }

  /**
   * @return The final arrival location.
   */
  Location finalArrivalLocation() {
    if (legs.isEmpty()) {
      return Location.UNKNOWN;
    } else {
      return lastLeg().unloadLocation();
    }
  }

  /**
   * @return Date when cargo arrives at final destination.
   */
  Date finalArrivalDate() {
    if (isEmpty()) return new Date(END_OF_DAYS.getTime());
    return new Date(lastLeg().unloadTime().getTime());
  }

  private boolean isEmpty() {
    return legs.isEmpty();
  }

  /**
   * @return The first leg on the itinerary.
   */
  public Leg firstLeg() {
    if (isEmpty()) return null;
    return legs.get(0);
  }

  /**
   * @return The last leg on the itinerary.
   */
  public Leg lastLeg() {
    if (isEmpty()) return null;
    return legs.get(legs.size() - 1);
  }

  /**
   * @param rescheduledVoyage the voyage that has been rescheduled
   * @return A new itinerary which is a copy of the old one, adjusted for the delay of the given voyage.
   */
  public Itinerary withRescheduledVoyage(final Voyage rescheduledVoyage) {
    final List<Leg> newLegsList = new ArrayList<Leg>(this.legs.size());

    Leg lastAdded = null;
    for (Leg leg : this.legs) {
      if (leg.voyage().sameIdentityAs(rescheduledVoyage)) {
        Leg modifiedLeg = leg.withRescheduledVoyage(rescheduledVoyage);
        // This truncates the itinerary if the voyage rescheduling makes
        // it impossible to maintain the old unload-load chain.
        if (lastAdded != null && modifiedLeg.loadTime().before(lastAdded.unloadTime())) {
          break;
        }
        newLegsList.add(modifiedLeg);
      } else {
        newLegsList.add(leg);
      }
      lastAdded = leg;
    }

    return new Itinerary(newLegsList);
  }

  /**
   * @return A list of all locations on this itinerary.
   */
  public List<Location> locations() {
    final List<Location> result = new ArrayList<Location>();
    result.add(firstLeg().loadLocation());
    for (Leg leg : legs) {
      result.add(leg.unloadLocation());
    }
    return result;
  }

  /**
   * @param location a location
   * @return Load time at this location, or null if the location isn't on this itinerary.
   */
  public Date loadTimeAt(final Location location) {
    for (Leg leg : legs) {
      if (leg.loadLocation().sameIdentityAs(location)) {
        return leg.loadTime();
      }
    }
    return null;
  }

  /**
   * @param location a location
   * @return Unload time at this location, or null if the location isn't on this itinerary.
   */
  public Date unloadTimeAt(final Location location) {
    for (Leg leg : legs) {
      if (leg.unloadLocation().sameIdentityAs(location)) {
        return leg.unloadTime();
      }
    }
    return null;
  }


  /**
   * @param other itinerary to compare
   * @return <code>true</code> if the legs in this and the other itinerary are all equal.
   */
  @Override
  public boolean sameValueAs(final Itinerary other) {
    return other != null && legs.equals(other.legs);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final Itinerary itinerary = (Itinerary) o;

    return sameValueAs(itinerary);
  }

  @Override
  public int hashCode() {
    return legs.hashCode();
  }

  Itinerary() {
    // Needed by Hibernate
  }

  // Auto-generated surrogate key
  private Long id;
}
