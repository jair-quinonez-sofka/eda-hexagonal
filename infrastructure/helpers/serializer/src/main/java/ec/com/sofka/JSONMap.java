package ec.com.sofka;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.com.sofka.generics.domain.DomainEvent;
import org.springframework.stereotype.Component;

@Component
public class JSONMap implements IJSONMapper {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);


    @Override
    public String writeToJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException | ClassCastException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    @Override
    public Object readFromJson(String json, Class<?> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException | ClassCastException e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}
