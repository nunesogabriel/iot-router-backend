package br.com.ufu.iot_router_backend.service;

import java.io.IOException;
import java.util.List;

public interface SimulatedDevicesIPsService {
    List<String> getSimulatedDevicesIPs(List<String> containerNames) throws IOException;
}
