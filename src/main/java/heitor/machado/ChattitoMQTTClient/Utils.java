package heitor.machado.ChattitoMQTTClient;

import java.util.List;

public class Utils {

    public static GroupEnterRequest findAndRemove(List<GroupEnterRequest> requests, Long requestId) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).requestId == (requestId)) {
                return requests.remove(i);
            }
        }
        return null;
    }
}
