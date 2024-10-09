package br.com.ufu.iot_router_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "br.com.ufu.iot_router_backend")
public class IotRouterBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(IotRouterBackendApplication.class, args);
	}

}
