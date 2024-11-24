package br.com.ufu.iot_router_backend.model;

import br.com.ufu.iot_router_backend.enums.QoSPriority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Device {
    private String name;
    private String ip;
    private double rxBytes;
    private double txBytes;
    private QoSPriority priority;
    private boolean available;
    
    public boolean isMqtt() {
    	return (this.name != null) ? this.name.contains("mqtt") : false;
    }
}
