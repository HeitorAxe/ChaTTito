package heitor.machado.ChattitoMQTTClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class MessageTemplate {
    private String type;
    private String from;
    private String text;
    private String to;
    private String ts;
    private String group;
    private String session;

    public MessageTemplate() {}

    public MessageTemplate(MqttMessage message, ObjectMapper objectMapper) {
        try {
            String payload = new String(message.getPayload());

            MessageTemplate temp = objectMapper.readValue(payload, MessageTemplate.class);

            this.type     = temp.type;
            this.from     = temp.from;
            this.text     = temp.text;
            this.to       = temp.to;
            this.group    = temp.group;
            this.session  = temp.session;

            this.ts = Instant.now().toString();

        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public MessageTemplate(String type, String from, String to, String text) {
        this.type = type;
        this.from = from;
        this.text = text;
        this.to   = to;
        this.ts   = Instant.now().toString();
    }


    public MqttMessage getMqttMessage(ObjectMapper objectMapper){
        String payload = null;
        try {
            payload = objectMapper.writeValueAsString(this);
            return new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

        } catch (JsonProcessingException e) {
            //throw new RuntimeException(e);
            return null;
        }
    }
}
