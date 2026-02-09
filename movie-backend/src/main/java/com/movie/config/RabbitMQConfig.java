package com.movie.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // --- 点赞队列 (不变) ---
    public static final String LIKE_COUNT_QUEUE = "like.count.queue.v2";
    @Bean
    public Queue likeCountQueue() {
        return new Queue(LIKE_COUNT_QUEUE, true);
    }
    
    // --- 新增：订单超时处理 ---
    public static final String ORDER_TTL_QUEUE = "order.delay.queue"; // 延迟队列
    public static final String ORDER_DLX_EXCHANGE = "order.dlx.exchange"; // 死信交换机
    public static final String ORDER_RELEASE_QUEUE = "order.release.queue"; // 死信接收队列
    public static final String ORDER_DLX_ROUTING_KEY = "order.release.key"; // 死信路由键

    // 1. 定义死信交换机
    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(ORDER_DLX_EXCHANGE);
    }
    
    // 2. 定义延迟队列 (消息过期后会进入死信交换机)
    @Bean

    public Queue orderTtlQueue() {
        return QueueBuilder.durable(ORDER_TTL_QUEUE)
                .ttl(900000) // 15分钟 TTL (15 * 60 * 1000)
                .deadLetterExchange(ORDER_DLX_EXCHANGE) // 绑定死信交换机
                .deadLetterRoutingKey(ORDER_DLX_ROUTING_KEY) // 绑定死信路由键
                .build();
    }
    
    // 3. 定义死信队列 (真正消费消息的队列)
    @Bean
    public Queue orderReleaseQueue() {
        return new Queue(ORDER_RELEASE_QUEUE, true);
    }
    
    // 4. 绑定死信队列和死信交换机
    @Bean
    public Binding orderDlxBinding() {
        return BindingBuilder.bind(orderReleaseQueue()).to(orderDlxExchange()).with(ORDER_DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}