package com.example.demo.config;

import com.example.demo.dto.chat.ChatRoomMessage; // 채팅 메시지 DTO 클래스
import com.example.demo.service.impl.ChatServiceImpl; // 채팅 서비스 구현체 클래스

// Jackson 라이브러리 관련 임포트
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 처리의 핵심 클래스
import com.fasterxml.jackson.databind.SerializationFeature; // JSON 직렬화 시 특징 설정 (예: 날짜 포맷)
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Java 8 날짜/시간(LocalDate, LocalDateTime 등) 직렬화를 위한 모듈
import com.fasterxml.jackson.databind.type.CollectionType; // List, Set 등 컬렉션 타입의 제네릭 정보를 얻기 위함

// 스프링 프레임워크 및 Redis 관련 임포트
import org.springframework.beans.factory.annotation.Value; // application.properties 등에서 설정 값을 주입받기 위함
import org.springframework.context.annotation.Bean; 
import org.springframework.context.annotation.Configuration; // 스프링 설정 클래스임을 명시
import org.springframework.data.redis.connection.RedisConnectionFactory; // Redis 서버와의 연결을 관리하는 팩토리 인터페이스
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory; // Lettuce 기반 Redis 연결 팩토리 구현체
import org.springframework.data.redis.core.RedisTemplate; // Redis 데이터 작업을 위한 핵심 템플릿
import org.springframework.data.redis.listener.RedisMessageListenerContainer; // Redis Pub/Sub 메시지를 리스닝하는 컨테이너
import org.springframework.data.redis.listener.ChannelTopic; // Redis Pub/Sub 채널을 나타내는 클래스
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter; // 메시지 리스너를 특정 메서드에 연결하는 어댑터
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer; // 모든 자바 객체를 JSON으로 직렬화/역직렬화 (범용)
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer; // 특정 클래스나 제네릭 타입을 JSON으로 직렬화/역직렬화
import org.springframework.data.redis.serializer.StringRedisSerializer; // 문자열을 Redis에 직렬화/역직렬화

import java.util.List;

/**
 * Redis 관련 설정을 정의하는 스프링 설정 클래스입니다.
 * Redis 연결, 데이터 직렬화 방식, Pub/Sub(발행/구독) 기능 등을 설정합니다.
 */
@Configuration
public class RedisConfig {

    // application.properties에서 Redis 호스트 값을 주입받습니다.
    @Value("${spring.data.redis.host}")
    private String redisHost;

    // application.properties에서 Redis 포트 값을 주입받습니다.
    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * RedisConnectionFactory 빈을 정의합니다.
     * 이 팩토리는 애플리케이션과 Redis 서버 간의 연결을 생성하고 관리합니다.
     * 여기서는 Lettuce 클라이언트를 사용하여 Redis에 연결합니다.
     *
     * @return RedisConnectionFactory 인스턴스
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * Pub/Sub 메시지 전송 및 수신에 사용될 RedisTemplate 빈을 정의합니다.
     * 이 템플릿은 단일 'ChatRoomMessage' 객체를 Redis 메시지로 처리합니다.
     * 키는 문자열로, 값(ChatRoomMessage 객체)은 JSON 형태로 직렬화됩니다.
     *
     * @param connectionFactory Redis 서버와의 연결을 관리하는 팩토리
     * @return 설정된 RedisTemplate 인스턴스 (ChatRoomMessage용)
     */
    @Bean(name = "chatMessageRedisTemplate") // Pub/Sub 메시징에 사용될 템플릿임을 명확히 하기 위한 이름
    public RedisTemplate<String, ChatRoomMessage> chatMessageRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ChatRoomMessage> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer()); // Redis 키를 문자열로 직렬화

        // ObjectMapper를 생성하여 Java 8 날짜/시간 객체(LocalDateTime 등)를 JSON으로 변환할 수 있도록 설정합니다.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Java 8 날짜/시간 모듈 등록
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 날짜를 ISO 8601 문자열(예: "2023-01-01T10:30:00")로 저장하도록 설정

        // 값(ChatRoomMessage 객체)을 JSON으로 직렬화하기 위해 GenericJackson2JsonRedisSerializer를 사용합니다.
        // objectMapper를 생성자에 직접 전달하여 커스터마이징된 설정을 적용합니다.
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)); // 해시 타입의 값도 동일하게 설정

        template.afterPropertiesSet(); // 모든 속성이 설정된 후 템플릿을 초기화합니다.
        return template;
    }

    /**
     * 채팅 히스토리 캐싱에 사용될 RedisTemplate 빈을 정의합니다.
     * 이 템플릿은 'List<ChatRoomMessage>' 객체를 Redis에 캐시할 때 사용됩니다.
     * 키는 문자열로, 값(List<ChatRoomMessage> 객체)은 JSON 형태로 직렬화됩니다.
     *
     * @param connectionFactory Redis 서버와의 연결을 관리하는 팩토리
     * @return 설정된 RedisTemplate 인스턴스 (List<ChatRoomMessage>용)
     */
    @Bean(name = "chatHistoryRedisTemplate") // 채팅 히스토리 캐싱에 사용될 템플릿임을 명확히 하기 위한 이름
    public RedisTemplate<String, List<ChatRoomMessage>> chatHistoryRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, List<ChatRoomMessage>> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer()); // Redis 키를 문자열로 직렬화

        // ObjectMapper를 생성하여 Java 8 날짜/시간 객체(LocalDateTime 등)를 JSON으로 변환할 수 있도록 설정합니다.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Java 8 날짜/시간 모듈 등록
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 날짜를 ISO 8601 문자열로 저장하도록 설정

        // List<ChatRoomMessage>와 같은 제네릭 컬렉션 타입을 정확히 직렬화/역직렬화하기 위해 Jackson2JsonRedisSerializer를 사용합니다.
        // ObjectMapper의 getTypeFactory()를 통해 List<ChatRoomMessage>의 정확한 JavaType을 생성합니다.
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, ChatRoomMessage.class);

        // Jackson2JsonRedisSerializer의 생성자에 ObjectMapper와 생성한 JavaType을 함께 전달하여 초기화합니다.
        // 이 방식은 Deprecated된 setObjectMapper() 메서드를 사용하지 않는 최신 권장 방법입니다.
        Jackson2JsonRedisSerializer<List<ChatRoomMessage>> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, collectionType);

        template.setValueSerializer(jsonSerializer); // Redis 값을 JSON으로 직렬화
        template.setHashValueSerializer(jsonSerializer); // 해시 타입의 값도 동일하게 설정 (필요시)

        template.afterPropertiesSet(); // 모든 속성이 설정된 후 템플릿을 초기화합니다.
        return template;
    }

    /**
     * Redis Pub/Sub 메시지를 수신하기 위한 컨테이너 빈을 정의합니다.
     * 이 컨테이너는 특정 Redis 채널에서 메시지가 오는지 감시하고,
     * 메시지가 오면 등록된 리스너 어댑터로 전달합니다.
     *
     * @param connectionFactory Redis 서버와의 연결 팩토리
     * @param listenerAdapter   메시지 리스너 어댑터 (메시지를 처리할 메서드에 연결)
     * @param channelTopic      메시지를 구독할 Redis 채널 토픽
     * @return 설정된 RedisMessageListenerContainer 인스턴스
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListener(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic channelTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory); // Redis 연결 팩토리 설정
        container.addMessageListener(listenerAdapter, channelTopic); // 특정 채널의 메시지를 리스너 어댑터로 전달하도록 등록
        return container;
    }

    /**
     * Redis Pub/Sub 메시지를 수신했을 때 특정 메서드를 호출하도록 연결하는 어댑터 빈을 정의합니다.
     * 여기서는 Redis 채널로 들어오는 메시지를 'ChatServiceImpl'의 'sendMessage' 메서드로 전달합니다.
     *
     * @param chatService 메시지를 처리할 ChatServiceImpl 인스턴스
     * @return 설정된 MessageListenerAdapter 인스턴스
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(ChatServiceImpl chatService) {
        // chatService 빈의 "sendMessage" 메서드를 Redis 메시지 리스너로 등록합니다.
        // Redis로부터 메시지가 오면 chatService.sendMessage()가 호출됩니다.
        return new MessageListenerAdapter(chatService, "sendMessage");
    }

    /**
     * Redis Pub/Sub 채널의 토픽을 정의하는 빈입니다.
     * 메시지 발행자와 구독자가 동일한 토픽을 사용해야 메시지가 정상적으로 전달됩니다.
     *
     * @return "chatroom"이라는 이름의 ChannelTopic 인스턴스
     */
    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("chatroom"); // 채팅 메시지가 발행/구독될 채널 이름
    }
}