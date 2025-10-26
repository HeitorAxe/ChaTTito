package heitor.machado.ChattitoMQTTClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

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

    // Getters
    public String getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getText() {
        return text;
    }

    public String getTs() {
        return ts;
    }

    public String getTo() {
        return to;
    }

    public String getGroup() {
        return group;
    }

    public String getSession() {
        return session;
    }

    // Setters
    public void setType(String type) {
        this.type = type;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setSession(String session) {
        this.session = session;
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
