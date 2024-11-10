package br.com.ufu.iot_router_backend.service.impl;

import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.service.GetIpsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GetIpsServiceImpl implements GetIpsService {

    @Override
    public List<Device> updateDeviceList(String jsonResponse) throws IOException {
        List<Device> devices = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        log.info("Devices Docker : {}", jsonResponse);
        JsonNode root = mapper.readTree(jsonResponse);
        for (JsonNode node : root) {
            String name = node.path("name").asText();
            String ip = node.path("ip").asText();
            devices.add(new Device(name, ip));
        }

        return devices;
    }
}
