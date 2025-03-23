package br.com.microservices.orchestrated.orderservice.core.repository;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends MongoRepository<Event, String> {

    // This method is used to find all events by order by created at desc.
    List<Event> findAllByOrderByCreatedAtDesc();

    // This method is used to find a last event by order id.
    Optional<Event> findTop1ByOrderByCreatedAtDesc(String orderId);

    // This method is used to find a last event by transaction id.
    Optional<Event> findTop1ByTransactionIdOrderByCreatedAtDesc(String transactionId);

}
