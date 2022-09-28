package com.wpx.nacosconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

@EnableDiscoveryClient(autoRegister = false) // 设置不自动注册到注册中心Nacos，因为这个项目本身是消费者，不向外暴露服务
@SpringBootApplication
public class NacosReactiveConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NacosReactiveConsumerApplication.class, args);
    }

    @RestController
    class HelloController {

        @Resource   // 在HelloController中注入DiscoveryClient，这是Spring Cloud Commons模块提供的一个服务发现接口，
        // Spring Cloud Alibaba Nacos Discovery 模块内部会初始化一个它的实现类————NacosDiscoveryClient，
        // 用于后续的服务发现操作
        private DiscoveryClient discoveryClient;

        @Resource //解释参考discoveryClient
        private ReactiveDiscoveryClient reactiveDiscoveryClient;

        private final String serviceName = "NacosProvider";

        @GetMapping("/services") // 注意，这里返回的是Flux<ServiceInstance>
        public Flux<ServiceInstance> services() {
            // 使用 reactiveDiscoveryClient 获取注册中心下所有的服务列表
            return reactiveDiscoveryClient.getInstances(serviceName);
        }

        @GetMapping("/instance")
        public Flux<String> instance() {
            // 使用 reactiveDiscoveryClient 获取NacosProvider 服务对应的所有实例
            return reactiveDiscoveryClient.getInstances(serviceName).map(instance ->
                    "[ serviceId:" + instance.getServiceId()
                            + " , host:" + instance.getHost()
                            + " , port:" + instance.getPort()
                            + "]");
        }

        @GetMapping("/hello")
        public Mono<String> hello() {
            Flux<ServiceInstance> instances = reactiveDiscoveryClient.getInstances(serviceName);
            // 从服务实例中取第一个，这个方法不知道对不对，如果不对帮我在评论区指出来
            ServiceInstance serviceInstance = instances.blockFirst();
            // 使用WebClient调用服务实例对应的节点信息中的“/echo”方法
            return WebClient.create("http://" + serviceInstance.getHost() + ":" + serviceInstance.getPort())
                    .get()
                    .uri("/echo?name=nacos")
                    .retrieve()
                    .bodyToMono(String.class);
        }

    }

}
