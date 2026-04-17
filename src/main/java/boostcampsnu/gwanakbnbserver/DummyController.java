package boostcampsnu.gwanakbnbserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DummyController {

    @GetMapping("/check")
    public String dummyResponse() {
        return "ok";
    }
}
