package br.com.ufu.iot_router_backend.service;

import br.com.ufu.iot_router_backend.model.Device;

import java.io.IOException;
import java.util.List;

public interface GetIpsService {
    List<Device> updateDeviceList(String jsonResponse) throws IOException;
}
