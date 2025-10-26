package heitor.machado.ChattitoMQTTClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.apache.catalina.Group;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class MqttClientService implements MqttCallback {

    @Value("${broker.connectionString}")
    private       String                        broker;
    private       MqttClient                    client;
    private       MqttConnectOptions            opts;
    private final Map<String, Consumer<String>> sessionListeners = new ConcurrentHashMap<>();
    private final ObjectMapper                  objectMapper     = new ObjectMapper();

    @Setter
    @Value("${client.id}")
    private       String                  clientId;
    @Getter
    private final Map<String, String>     activeSessions        = new ConcurrentHashMap<>();
    private final List<SessionRequest>    sessionRequests       = new ArrayList<>();
    private       List<GroupData>         groups                = new ArrayList<>();
    private       List<GroupEnterRequest> groupEnterRequestList = new ArrayList<>();
    private final List<GroupData>         activeGroups          = new ArrayList<>();
    private       Map<String, Boolean>    usersStatus           = new ConcurrentHashMap<>();
    @Getter
    private       Boolean                 clientStatus          = true;

    @PostConstruct
    public void init() throws MqttException {
        //if (clientId == null) clientId = "client_" + System.currentTimeMillis();
        client = new MqttClient(broker, clientId, new MemoryPersistence());
        opts = new MqttConnectOptions();
        opts.setCleanSession(false);
        client.setCallback(this);
        client.connect(opts);

        client.subscribe("users");
        client.subscribe("users/" + clientId + "/control");
        client.subscribe("groups/list");

        System.out.println("Cliente " + clientId + " conectou ao Broker MQTT em " + broker);

        try{
            this.setStatusOnline(true);
        }catch (Exception e){
            System.out.println("Ocorreu um erro e o Client" + clientId + " não pôde publicar seu status.");
        }
    }


    public String requestSession(String toId) throws MqttException {
        String sessionId = toId + "_" + this.clientId + "_" + System.currentTimeMillis();
        var message = new MessageTemplate("session_request", this.clientId, toId, sessionId);
        client.publish("users/" + toId + "/control", message.getMqttMessage(objectMapper));
        return sessionId;
    }

    public Map<String, Boolean> listUsersStatus(){
        return this.usersStatus;
    }

    public String enterOrCreateGroup(String groupId) throws Exception{

        for (var group: this.groups){
            if (Objects.equals(group.getGroupId(), groupId)) {

                if (group.getGroupOwnerId().equals(this.clientId))
                    return "Você é o dono do grupo";
                else if (group.getParticipants().contains(this.clientId))
                    return "você já participa desse grupo";

                var message = new MessageTemplate("group_enter_request", this.clientId, group.getGroupOwnerId(), "");
                message.setGroup(groupId);
                client.publish("users/" + group.getGroupOwnerId() + "/control", message.getMqttMessage(objectMapper));
                return "Pediu para entrar no grupo " + groupId + ".";
            }
        }

        var newGroup = new GroupData(groupId, this.clientId, List.of(this.clientId));
        client.subscribe("groups/" + groupId + "/control");
        groups.add(newGroup);
        activeGroups.add(newGroup);
        publishUpdateGroupList();

        return "Grupo "+groupId+" Criado!";

    }

    public void publishUpdateGroupList() throws Exception{
        String payload = objectMapper.writeValueAsString(this.groups);
        var message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setRetained(true);
        client.publish("groups/list", message);
    }

    public void setStatusOnline(Boolean online) throws Exception{
        var message = new MessageTemplate("user_status_update", this.clientId, "", online ? "ONLINE" : "OFFLINE");
        if(online == this.clientStatus){
            client.publish("users", message.getMqttMessage(objectMapper));
            return;
        }

        if(online) {
            client.connect(opts);
            client.publish("users", message.getMqttMessage(objectMapper));
        }else {
            client.publish("users", message.getMqttMessage(objectMapper));
            client.disconnect();
        }

        this.clientStatus = online;
    }


    public void acceptSession(String sessionId) throws Exception {
        String toId = "";
        for(var sessionRequest : sessionRequests){
            if(sessionRequest.getProposedSessionId().equals(sessionId)){
                toId = sessionRequest.getFromId();
                sessionRequest.setAccepted(true);
                break;
            }
        }
        if(toId.isEmpty()){
            throw new RuntimeException("Não foi possível aceitar a sessão "+sessionId);
        }

        client.subscribe("sessions/" + sessionId);

        var message = new MessageTemplate("session_accept", this.clientId, toId, sessionId);
        message.setSession(sessionId);
        client.publish("users/" + toId + "/control", message.getMqttMessage(objectMapper));

        activeSessions.put(sessionId, toId);
    }

    public void acceptUserInGroup(long requestId) throws Exception {
        var enterRequest = Utils.findAndRemove(groupEnterRequestList, requestId);
        var message = new MessageTemplate("group_enter_accept", this.clientId, enterRequest.userId, enterRequest.groupId);
        message.setGroup(enterRequest.groupId);

        for(var group : this.groups){
            if(group.getGroupId().equals(enterRequest.groupId) && !group.getParticipants().contains(enterRequest.userId)
               && group.getGroupOwnerId().equals(this.clientId))
            {
                group.addParticipants(enterRequest.userId);
                break;
            }
        }

        client.publish("users/" + enterRequest.userId + "/control", message.getMqttMessage(objectMapper));
        publishUpdateGroupList();
    }


    public void sendMessage(String sessionId, String text) throws Exception {
        var message = new MessageTemplate("message", this.clientId, "", text);
        client.publish("sessions/" + sessionId, message.getMqttMessage(objectMapper));
        System.out.println("[" + this.clientId + "] -> (" + sessionId + "): " + text);
    }


    public void registerSessionListener(String sessionId, Consumer<String> callback) throws MqttException {
        sessionListeners.put(sessionId, callback);
        client.subscribe("sessions/" + sessionId);
    }


    @Override
    public void connectionLost(Throwable cause) {
        //setar os status offline?
        System.err.println("Conexão perdida com broker: " + cause.getMessage());
    }

    public List<SessionRequest> getPendingSessionRequests(){
        return this.sessionRequests.stream()
                .filter(r -> Boolean.FALSE.equals(r.getAccepted()))
                .toList();
    }


    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String payload = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
        var message = new MessageTemplate(mqttMessage, objectMapper);
        String type = message.getType();

        if (topic.startsWith("sessions/")) {
            String sessionId = topic.substring("sessions/".length());
            Consumer<String> cb = sessionListeners.get(sessionId);
            if (cb != null) {
                cb.accept(payload);
            } else {
                System.out.println("Mensagem recebida em [" + sessionId + "]: " + payload);
            }
        } else if (topic.startsWith("users/") && topic.endsWith("/control")) {
            if(type.equals("session_request")){
                var newSessionRequest = new SessionRequest(message.getFrom(), message.getText(), false);
                sessionRequests.add(newSessionRequest);
            }
            if(type.equals("session_accept")){
                activeSessions.put(message.getText(), message.getFrom());
            }
            if(type.equals("group_enter_accept")){
                for(var group : groups){
                    if (group.getGroupId().equals(message.getGroup())){
                        activeGroups.add(group);
                        break;
                    }
                }
            }
            if(type.equals("group_enter_request")){
                var newGroupEnterRequest = new GroupEnterRequest(message.getGroup(), message.getFrom());
                groupEnterRequestList.add(newGroupEnterRequest);
            }
        }else if (topic.endsWith("groups/list")) {
            //houve mudança  na lista de grupos
            updateLocalGroups(payload);
        }else if(type.equals("user_status_update")){
            this.usersStatus.put(message.getFrom(), message.getText().equals("ONLINE"));
        }
    }

    public void updateLocalGroups(String messageString){
        try {
            List<GroupData> groups = objectMapper.readValue(
                messageString, new TypeReference<List<GroupData>>() {}
            );
            this.groups = groups;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.activeGroups.clear();

        for(var group : this.groups){
            if(group.getGroupOwnerId().equals(this.clientId)){
                this.activeGroups.add(group);
                continue;
            }
            for(var participant : group.getParticipants()){
                if(participant.equals(this.clientId)){
                    this.activeGroups.add(group);
                    break;
                }
            }
        }
    }

    public List<GroupData> listGroups(){
        return this.groups;
    }

    public List<GroupData> listActiveGroups(){
        return this.activeGroups;
    }

    public List<GroupEnterRequest> listGroupEnterRequests(){
        return this.groupEnterRequestList;
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        return;
    }
}


