package kr.hhplus.be.server.eventHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "orderTaskExecutor")
    public ThreadPoolTaskExecutor orderTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);        // 기본 스레드 수
        executor.setMaxPoolSize(50);         // 최대 스레드 수
        executor.setQueueCapacity(100);      // 큐 용량
        executor.setThreadNamePrefix("Order-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
