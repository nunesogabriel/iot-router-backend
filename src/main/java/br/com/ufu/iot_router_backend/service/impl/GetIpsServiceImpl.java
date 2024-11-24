package br.com.ufu.iot_router_backend.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.service.GetIpsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GetIpsServiceImpl implements GetIpsService {

    @Override
    public List<Device> updateDeviceList(String jsonResponse) throws IOException {
        List<Device> devices = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        for (JsonNode node : root) {
            String name = node.path("name").asText();
            
            if (isMqttDevice(name) || isBroker(name)) {
            	String ip = node.path("ip").asText();
            	var device = new Device();
            	device.setIp(ip);
            	device.setName(name);
                devices.add(device);
            }
        }

        return devices;
    }

	private boolean isBroker(String name) {
		return name.contains("mosq");
	}

	private boolean isMqttDevice(String name) {
		return name.contains("mqtt");
	}
}
