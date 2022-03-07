package com.github.wolfforgan.job;

import com.github.wolfforgan.common.registry.Registry;
import com.github.wolfforgan.common.registry.RegistryFactory;
import com.github.wolfforgan.datasource.HttpReceiverSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class HttpReceiverJob {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(3);   //try 3 instances
        String serverName = "flink-connector-http";
        Registry registry = RegistryFactory.fromNacos("ip:port", serverName, "group-name","cluster-name", "namespace-id");
        DataStream<String> inputStream = env.addSource(new HttpReceiverSource("/flink/source", 1978, registry, serverName));   //if no need to register, parameter 'registry' could be null
        inputStream.print();
        env.execute("HttpReceiverJob");
    }
}
