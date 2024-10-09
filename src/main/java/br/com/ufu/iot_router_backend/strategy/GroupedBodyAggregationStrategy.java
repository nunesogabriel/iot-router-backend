package br.com.ufu.iot_router_backend.strategy;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import java.util.ArrayList;
import java.util.List;

public class GroupedBodyAggregationStrategy implements AggregationStrategy {
    /**
     * @param oldExchange 
     * @param newExchange
     * @return
     */
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        List<Object> list;
        if (oldExchange == null) {
            list = new ArrayList<>();
            oldExchange = newExchange;
            oldExchange.getIn().setBody(list);
        } else {
            list = oldExchange.getIn().getBody(List.class);
        }
        list.add(newExchange.getIn().getBody());
        return oldExchange;
    }
}
