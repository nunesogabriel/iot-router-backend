package br.com.ufu.iot_router_backend.service;

import br.com.ufu.iot_router_backend.enums.QoSAdjustmentTypeEnum;
import br.com.ufu.iot_router_backend.model.Device;

public interface QoSService {
    void applyQoS(Device device, QoSAdjustmentTypeEnum adjustmentType) throws Exception;
    String fetchHostTopology() throws Exception;
}
