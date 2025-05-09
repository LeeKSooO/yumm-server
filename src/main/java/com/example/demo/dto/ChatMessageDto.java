package com.example.demo.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private String roomId;
    private String sender; // username( or nickname )
    private String content;
    private MessageType Type; // Enter, Talk, Leave ..

}
