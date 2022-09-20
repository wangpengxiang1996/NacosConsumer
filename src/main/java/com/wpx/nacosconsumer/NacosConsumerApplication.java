package com.wpx.nacosconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;

@EnableDiscoveryClient(autoRegister = false) // 设置不自动注册到注册中心Nacos，因为这个项目本身是消费者，不向外暴露服务
@SpringBootApplication
public class NacosConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NacosConsumerApplication.class, args);
    }

    @Bean  // 构建一个RestTemplate，后续用于访问HTTP服务
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @RestController
    class HelloController {

        @Resource   // 在HelloController中注入DiscoveryClient，这是Spring Cloud Commons模块提供的一个服务发现接口，
        // Spring Cloud Alibaba Nacos Discovery 模块内部会初始化一个它的实现类————NacosDiscoveryClient，
        // 用于后续的服务发现操作
        private DiscoveryClient discoveryClient;

        @Resource   // 在HelloController中注入前面构造的RestTemplate
        private RestTemplate restTemplate;

        private final String serviceName = "NacosProvider";

        @GetMapping("/info")
        public String info() {
            // 使用 DiscoveryClient 获取NacosProvider 服务对应的所有实例
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
            StringBuilder sb = new StringBuilder();
            sb.append("all service:").append(discoveryClient.getServices()).append(" <br/>");
            sb.append("NacosConsumer instance list:<br/>");
            // 遍历拿到的所有服务实例，获取对应的host和port
            serviceInstances.forEach(instance -> {
                sb.append("[ serviceId: ")
                        .append(instance.getServiceId())
                        .append(", host: ")
                        .append(instance.getHost())
                        .append(", port: ")
                        .append(instance.getPort())
                        .append(" ]<br/>");
            });
            return sb.toString();
        }

        @GetMapping("/hello")
        public String hello() {
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
            // 从服务实例中任意取出一个，没有则抛异常
            ServiceInstance serviceInstance = serviceInstances.stream()
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("no " + serviceName + " instance available"));
            // 使用RestTemplate调用服务实例对应的节点信息中的“/echo”方法
            return restTemplate.getForObject(
                    "http://" + serviceInstance.getHost() + ":" + serviceInstance.getPort() + "/echo?name=nacos", String.class);
        }

    }

}
