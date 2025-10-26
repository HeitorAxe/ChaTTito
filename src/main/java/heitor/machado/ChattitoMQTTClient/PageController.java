package heitor.machado.ChattitoMQTTClient;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @Value("${server.port}")
    private String serverPort;
    @Value("${client.id}")
    private String clientId;

    @GetMapping("/")
    public String index(Model model) {
        var apiBaseUrl = "http://localhost:"+serverPort+"/api";
        model.addAttribute("apiBaseUrl", apiBaseUrl);
        model.addAttribute("clientId", clientId);
        return "index";
    }
}