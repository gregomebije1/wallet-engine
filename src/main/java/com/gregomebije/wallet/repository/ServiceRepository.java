package com.gregomebije.wallet.repository;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.gregomebije.wallet.model.Service;

@Repository
public interface ServiceRepository extends JpaRepository <Service, Long> {

    /*
    @EntityGraph(attributePaths = "serviceProvider")
    List<Service> findAll();
    */

    //@Query(value = "SELECT s.* FROM services s JOIN service_providers sp ON s.service_provider_id = sp.id", nativeQuery = true)
    @Query("SELECT s FROM Service s JOIN FETCH s.serviceProvider") //JPQL
    List<Service> findAllWithProvider();
}
