package heitor.machado.ChattitoMQTTClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SessionRequest {
    private String  fromId;
    private String  proposedSessionId;
    private Boolean accepted;
}
