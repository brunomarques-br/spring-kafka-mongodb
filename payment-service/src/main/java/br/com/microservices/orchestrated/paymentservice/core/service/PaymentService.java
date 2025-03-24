package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.History;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus.SUCCESS;
import static br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus.FAIL;
import static br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus.ROLLBACK_PENDING;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_AMOUNT_VALUE = 0.1;

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final PaymentRepository paymentRepository;

    /**
     * Method to realize the payment
     *
     * @param event
     */
    public void realizePayment(Event event) {
        try {
            checkCurrentValidation(event);
            createPendingPayment(event);
            var payment = findByOrderIdAndTransactionId(event);
            validateAmount(payment.getTotalAmount());
            changePaymentToSuccess(payment);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to make payment", e);
            handleFailCurrentNotExecuted(event, e.getMessage());
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    /**
     * Method to check if the current validation is valid
     *
     * @param event
     */
    private void checkCurrentValidation(Event event) {

        if (paymentRepository.existsByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())) {
            throw new ValidationException("There's another transactionId for this validation.");
        }

    }

    /**
     * Method to create a pending payment
     *
     * @param event
     */
    private void createPendingPayment(Event event) {
        var totalAmount = calculateAmount(event);
        var totalItems = calculateTotalItems(event);
        var payment = Payment.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
        save(payment);
        setEventAmountItems(event, payment);
    }

    /**
     * Method to save the payment in the database
     *
     * @param payment
     */
    private void save(Payment payment) {
        paymentRepository.save(payment);
    }

    /**
     * Method to calculate the total amount in the event
     *
     * @param event
     * @return double
     */
    private double calculateAmount(Event event) {
        return event.getPayload().getProducts().stream()
                .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    /**
     * Method to calculate the total items in the event
     *
     * @param event
     * @return int
     */
    private int calculateTotalItems(Event event) {
        return event.getPayload().getProducts().stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    /**
     * Method to set the total amount and total items in the event
     *
     * @param event
     * @param payment
     */
    private void setEventAmountItems(Event event, Payment payment) {
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }

    /**
     * Method to find a payment by orderId and transactionId
     *
     * @param event
     * @return Payment
     */
    private Payment findByOrderIdAndTransactionId(Event event) {
        return paymentRepository.findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .orElseThrow(() -> new ValidationException("Payment not found"));
    }

    /**
     * Method to validate the amount must be greater than 0.1
     *
     * @param amount
     */
    private void validateAmount(double amount) {
        if (amount < MIN_AMOUNT_VALUE) {
            throw new ValidationException("Amount must be greater than: ".concat(MIN_AMOUNT_VALUE.toString()));
        }
    }

    /**
     * Method to change the payment status to SUCCESS
     *
     * @param payment
     */
    private void changePaymentToSuccess(Payment payment) {
        payment.setStatus(SUCCESS);
        save(payment);
    }

    /**
     * Method to handle the success of the payment
     *
     * @param event
     */
    private void handleSuccess(Event event) {
        event.setStatus(ESagaStatus.SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Payment realized successfully!");
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
        addHistory(event, "Fail to realize payment".concat(message));
    }

    /**
     * Method to realize the refund // rollback
     * @param event
     */
    public void realizeRefund(Event event){
        changePaymentStatusToRefund(event);
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback / Refund realized for payment!");
    }

    /**
     * Method to change the payment status to REFUND
     * @param event
     */
    private void changePaymentStatusToRefund(Event event) {
        var payment = findByOrderIdAndTransactionId(event);
        payment.setStatus(EPaymentStatus.REFUND);
        setEventAmountItems(event, payment);
        save(payment);
    }

}
