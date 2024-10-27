package br.com.ufu.iot_router_backend.service.impl;

import br.com.ufu.iot_router_backend.service.SimulatedDevicesIPsService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SimulatedDevicesIPsServiceImpl implements SimulatedDevicesIPsService {

    /*
     * Coletar IPS
     * Pingar cada IP
     * Validar o resultado e implementar o ajuste...
     * */


    @Override
    public List<String> getSimulatedDevicesIPs(List<String> containerNames) throws IOException {
        List<String> containerIps = new ArrayList<>();

        for (String containerName : containerNames) {
            String command = "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' " + containerName;
            log.info("Command : {}", command);
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String ip = reader.readLine();
            containerIps.add(ip != null ? ip : "IP não encontrado");
        }

        return containerIps;
    }
}
