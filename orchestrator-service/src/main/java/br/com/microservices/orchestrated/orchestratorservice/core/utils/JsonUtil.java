package br.com.microservices.orchestrated.orchestratorservice.core.utils;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class JsonUtil {

    private final ObjectMapper objectMapper;

    /**
     * @param object
     * @return String
     * Converte um objeto para uma string JSON
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @param json
     * @return Event
     * Converte uma string JSON para um objeto Event
     */
    public Event toEvent(String json) {
        try {
            return objectMapper.readValue(json, Event.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
