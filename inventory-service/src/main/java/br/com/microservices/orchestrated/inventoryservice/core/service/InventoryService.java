package br.com.microservices.orchestrated.inventoryservice.core.service;

import br.com.microservices.orchestrated.inventoryservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Event;
import br.com.microservices.orchestrated.inventoryservice.core.dto.History;
import br.com.microservices.orchestrated.inventoryservice.core.dto.Order;
import br.com.microservices.orchestrated.inventoryservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.inventoryservice.core.model.Inventory;
import br.com.microservices.orchestrated.inventoryservice.core.model.OrderInventory;
import br.com.microservices.orchestrated.inventoryservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.inventoryservice.core.repository.InventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.repository.OrderInventoryRepository;
import br.com.microservices.orchestrated.inventoryservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus.FAIL;
import static br.com.microservices.orchestrated.inventoryservice.core.enums.ESagaStatus.ROLLBACK_PENDING;

@Slf4j
@Service
@AllArgsConstructor
public class InventoryService {

    private static final String CURRENT_SOURCE = "INVENTORY_SERVICE";

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final InventoryRepository inventoryRepository;
    private final OrderInventoryRepository orderInventoryRepository;

    /**
     * Method to update the inventory
     *
     * @param event
     */
    public void updateInventory(Event event) {
        try {
            checkCurrentValidation(event);
            createOrderInventory(event);
            updateInventory(event.getPayload());
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to update inventory", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     * Method to rollback the inventory
     *
     * @param event
     */
    public void rollbackInventory(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            returnInventoryToPreviousValue(event);
            addHistory(event, "Rollback realized for inventory!");
        } catch (Exception e) {
            addHistory(event, "Rollback not executed for inventory: ".concat(e.getMessage()));
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     * Method to create a pending payment
     *
     * @param event
     */
    private void returnInventoryToPreviousValue(Event event) {
        orderInventoryRepository.findByOrderIdAndTransactionId(
                event.getPayload().getId(),
                event.getTransactionId()
        ).forEach(orderInventory -> {
            var inventory = orderInventory.getInventory();
            inventory.setAvaliable(orderInventory.getOldQuantity());
            inventoryRepository.save(inventory);
            log.info("Restored inventory for order: {} from: {} to {}",
                    event.getPayload().getId(),
                    orderInventory.getNewQuantity(),
                    inventory.getAvaliable());
        });
    }

    /**
     * Method to check if the current validation is valid
     *
     * @param event
     */
    private void checkCurrentValidation(Event event) {
        if (orderInventoryRepository.existsByOrderIdAndTransactionId(
                event.getPayload().getId(),
                event.getTransactionId())
        ) {
            throw new ValidationException("There's another transactionId for this validation.");
        }
    }

    /**
     * Method to create a pending payment
     *
     * @param event
     */
    private void createOrderInventory(Event event) {
        event.getPayload().getProducts().forEach(product -> {
            var inventory = findInventoryByProductCode(product.getProduct().getCode());
            var orderInventory = createOrderInventory(event, product, inventory);
            orderInventoryRepository.save(orderInventory);
        });
    }

    /**
     * Method to create a order inventory
     *
     * @param event
     * @param product
     * @param inventory
     * @return
     */
    private OrderInventory createOrderInventory(Event event, OrderProducts product, Inventory inventory) {
        return OrderInventory.builder()
                .inventory(inventory)
                .oldQuantity(inventory.getAvaliable())
                .orderQuantity(product.getQuantity())
                .newQuantity(inventory.getAvaliable() - product.getQuantity())
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .build();
    }

    /**
     * Method to find the inventory by product code
     *
     * @param productCode
     * @return
     */
    private Inventory findInventoryByProductCode(String productCode) {
        return inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ValidationException("Inventory not found for product_code: " + productCode));
    }

    /**
     * Method to update the inventory for each product
     *
     * @param order
     */
    private void updateInventory(Order order) {
        order.getProducts().forEach(product -> {
            var inventory = findInventoryByProductCode(product.getProduct().getCode());
            checkInventory(inventory.getAvaliable(), product.getQuantity());
            inventory.setAvaliable(inventory.getAvaliable() - product.getQuantity());
            inventoryRepository.save(inventory);
        });
    }

    /**
     * Method to check if the inventory is available
     *
     * @param avaliable
     * @param orderQuantity
     */
    private void checkInventory(int avaliable, int orderQuantity) {
        if (orderQuantity > avaliable) {
            throw new ValidationException("Product is out of stock!");
        }
    }

    /**
     * Method to handle the success of the payment
     *
     * @param event
     */
    private void handleSuccess(Event event) {
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Inventory updated successfully!");
    }

    /**
     * Method to add a history to the event
     *
     * @param event
     * @param message
     */
    private void addHistory(Event event, String message) {
        var history = History.builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        event.addToHistory(history);
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to update inventory: ".concat(message));
    }


}
