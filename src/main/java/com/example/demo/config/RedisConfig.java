package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.example.demo.dto.chat.ChatRoomMessage;
import com.example.demo.service.impl.ChatServiceImpl;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    /**
     * RedisTemplate Bean : Redis 서버와 상호작용 Config
     * 
     * key와 value의 직렬화 방식을 지정하여 Redis에 데이터를 저장하고 읽을 때 일관성을 유지합니다.
     *
     * @param connectionFactory Redis 서버와의 연결을 관리하는 팩토리
     * @return 설정된 RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, ChatRoomMessage> chatRoomMessageRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ChatRoomMessage> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListener(RedisConnectionFactory connectionFactory,
                                                            MessageListenerAdapter listenerAdapter,
                                                            ChannelTopic channelTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, channelTopic);
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(ChatServiceImpl chatService) {
        return new MessageListenerAdapter(chatService, "sendMessage");
    }

    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("chatroom");
    }
}