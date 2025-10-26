package heitor.machado.ChattitoMQTTClient;

import java.time.Instant;
import java.util.List;

public class GroupData {
    private String groupId;
    private String groupOwnerId;
    private String lastAddition;
    private List<String> participants;

    public GroupData() {
    }

    public GroupData(String groupId, String groupOwnerId, List<String> participants) {
        this.groupId = groupId;
        this.groupOwnerId = groupOwnerId;
        this.lastAddition = Instant.now().toString();
        this.participants = participants;
    }

    // Getters and setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupOwnerId() { return groupOwnerId; }
    public void setGroupOwnerId(String groupOwnerId) { this.groupOwnerId = groupOwnerId; }

    public String getLastAddition() { return lastAddition; }
    public void setLastAddition(String lastAddition) { this.lastAddition = lastAddition; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    public void addParticipants(String user) {this.participants.add(user);}
}
