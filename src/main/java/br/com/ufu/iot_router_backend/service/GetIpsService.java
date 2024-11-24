package br.com.ufu.iot_router_backend.service;

import java.io.IOException;
import java.util.List;

import br.com.ufu.iot_router_backend.model.Device;

public interface GetIpsService {
    List<Device> updateDeviceList(String jsonResponse) throws IOException;
}
