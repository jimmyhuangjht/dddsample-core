package se.citerus.dddsample.infrastructure.persistence.hibernate;

import org.springframework.stereotype.Repository;
import se.citerus.dddsample.domain.model.cargo.Cargo;
import se.citerus.dddsample.domain.model.cargo.CargoRepository;
import se.citerus.dddsample.domain.model.cargo.TrackingId;

import java.util.List;
import java.util.UUID;

/**
 * Hibernate implementation of CargoRepository.
 */
@Repository
public class CargoRepositoryHibernate extends HibernateRepository implements CargoRepository {

  public Cargo find(TrackingId tid) {
    return (Cargo) getSession().
      createQuery("from Cargo where trackingId = :tid").
      setParameter("tid", tid).
      uniqueResult();
  }

  public void store(Cargo cargo) {
    getSession().persist(cargo);
    getSession().createSQLQuery("delete from Leg where cargo_id = null").executeUpdate();
  }

  public TrackingId nextTrackingId() {
    final String random = UUID.randomUUID().toString().toUpperCase();
    return new TrackingId(
      random.substring(0, random.indexOf("-"))
    );
  }

  public List<Cargo> findAll() {
    return getSession().createQuery("from Cargo").list();
  }

}
