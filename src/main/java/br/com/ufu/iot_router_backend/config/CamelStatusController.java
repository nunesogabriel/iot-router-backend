package br.com.ufu.iot_router_backend.config;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CamelStatusController {

    @Autowired
    private CamelContext camelContext;

    @GetMapping("/camel/status")
    public String getStatus() {
        return camelContext.getStatus().toString();
    }
}
