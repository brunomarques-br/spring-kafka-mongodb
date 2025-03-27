package br.com.microservices.orchestrated.orchestratorservice.core.saga;

import static br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource.*;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus.*;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics.*;

public final class SagaHandler {

    private SagaHandler() {
    }

    public static final Object[][] SAGA_HANDLER = {

            /* ORCHESTRATOR */
            {ORCHESTRATOR, SUCCESS, PRODUCT_VALIDATION_SUCCESS},
            {ORCHESTRATOR, FAIL, FINISH_FAIL},

            /* PRODUCT_VALIDATION_SERVICE */
            {PRODUCT_VALIDATION_SERVICE, ROLLBACK_PENDING, PRODUCT_VALIDATION_FAIL},
            {PRODUCT_VALIDATION_SERVICE, FAIL, FINISH_FAIL},
            {PRODUCT_VALIDATION_SERVICE, SUCCESS, PAYMENT_SUCCESS},

            /* PAYMENT_SERVICE */
            {PAYMENT_SERVICE, ROLLBACK_PENDING, PAYMENT_FAIL},
            {PAYMENT_SERVICE, FAIL, PRODUCT_VALIDATION_FAIL},
            {PAYMENT_SERVICE, SUCCESS, INVENTORY_SUCCESS},

            /* INVENTORY_SERVICE */
            {INVENTORY_SERVICE, ROLLBACK_PENDING, INVENTORY_FAIL},
            {INVENTORY_SERVICE, FAIL, PAYMENT_FAIL},
            {INVENTORY_SERVICE, SUCCESS, FINISH_SUCCESS},

    };

    // The index of the EVENT_SOURCE in the SAGA_HANDLER array
    public static final int EVENT_SOURCE_INDEX = 0;
    // The index of the SAGA_STATUS in the SAGA_HANDLER array
    public static final int SAGA_STATUS_INDEX = 1;
    // The index of the TOPIC in the SAGA_HANDLER array
    public static final int TOPIC_INDEX = 2;

}
