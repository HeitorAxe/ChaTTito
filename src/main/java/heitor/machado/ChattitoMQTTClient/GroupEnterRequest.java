package heitor.machado.ChattitoMQTTClient;

public class GroupEnterRequest {
    public String groupId;
    public String userId;
    public long requestId;

    private static long counter = 0;

    public GroupEnterRequest(String groupId, String userId) {
        this.groupId = groupId;
        this.userId  = userId;

        this.requestId = getNextRequestId();
    }

    private static synchronized long getNextRequestId() {
        return ++counter;
    }

    public static long getTotalRequests() {
        return counter;
    }
}
