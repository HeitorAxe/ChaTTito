package heitor.machado.ChattitoMQTTClient;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final MqttClientService mqttService;

    public ChatController(MqttClientService mqttService) {
        this.mqttService = mqttService;
    }

    @PostMapping("/requestSession")
    public String requestSession(@RequestParam String toId) throws Exception {
        mqttService.requestSession(toId);
        return "Session request sent";
    }

    @PostMapping("/sendMessage")
    public String sendMessage(@RequestParam String sessionId, @RequestParam String message) throws Exception {
        mqttService.sendMessage(sessionId, message);
        return "Message sent";
    }

    @GetMapping("/listPendingRequests")
    public List<SessionRequest> getPendingRequests()
    {
        return mqttService.getPendingSessionRequests();
    }

    @GetMapping("/listActiveSessions")
    //Session ID - participante
    public Map<String, String> getActiveSessions(){
        return mqttService.getActiveSessions();
    }

    @PostMapping("/acceptSession")
    public String acceptSession(@RequestParam String sessionId) throws Exception {
        mqttService.acceptSession(sessionId);
        return "Session accepted: " + sessionId;
    }

    @GetMapping("/groups/listGroups")
    public List<GroupData> listGroups(){
        return mqttService.listGroups();
    }

    @GetMapping("/groups/listMyGroups")
    public List<GroupData> listMyGroups(){
        return mqttService.listActiveGroups();
    }

    @GetMapping("/groups/listGroupRequests")
    public List<GroupEnterRequest> listGroupRequests(){
        return mqttService.listGroupEnterRequests();
    }

    @PostMapping("/groups/enterOrCreateGroup")
    public String enterOrCreateGroup(@RequestParam String groupId) throws Exception{
        return mqttService.enterOrCreateGroup(groupId);
    }

    @PostMapping("/groups/acceptUserInGroup")
    public String acceptUserInGroup(@RequestParam Long requestId) throws Exception{
        mqttService.acceptUserInGroup(requestId);
        return "OK";
    }

    @GetMapping("/users")
    public Map<String, Boolean> listUsersStatus() throws Exception{
        return mqttService.listUsersStatus();
    }

    @GetMapping("/user/status")
    public Boolean getClientStatus() throws Exception{
        return mqttService.getClientStatus();
    }

    @PostMapping("/user/status")
    public String setClientStatus(@RequestParam Boolean online) throws Exception{
        mqttService.setStatusOnline(online);
        return "OK";
    }

    @GetMapping(value = "/messages/{sessionId}", produces = "text/event-stream")
    public SseEmitter streamMessages(@PathVariable String sessionId) throws Exception {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        mqttService.registerSessionListener(sessionId, msg -> {
            try {
                emitter.send(SseEmitter.event().data(msg));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

}
