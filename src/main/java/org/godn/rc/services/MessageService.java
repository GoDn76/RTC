package org.godn.rc.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.godn.rc.auth.model.User;
import org.godn.rc.auth.repository.UserRepository;
import org.godn.rc.websocket.dto.Chat;
import org.godn.rc.websocket.dto.JoinResult;
import org.godn.rc.entity.ChatParticipants;
import org.godn.rc.entity.ChatRoom;
import org.godn.rc.entity.Message;
import org.godn.rc.entity.MessageType;
import org.godn.rc.repository.ChatParticipantsRepository;
import org.godn.rc.repository.ChatRoomRepository;
import org.godn.rc.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatParticipantsRepository chatParticipantsRepository;

    @Transactional
    public Message saveMessage(String userId, String roomId, String content) {

        ChatRoom room = chatRoomRepository.findById(UUID.fromString(roomId))
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User sender = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = new Message();
        message.setChatRoom(room);
        message.setSender(sender);
        message.setContent(content);
        message.setCreatedAt(Instant.now());
        message.setMessageType(MessageType.TEXT);

        return messageRepository.save(message);
    }

    public List<Chat> getChatHistory(String roomId, int limit, int offset) {

        Pageable pageable = PageRequest.of(offset / limit, limit);

        Page<Message> page = messageRepository
                .findByChatRoomIdOrderByCreatedAtDesc(
                        UUID.fromString(roomId),
                        pageable
                );

        return page.getContent()
                .stream()
                .map(this::convertToChatDto)
                .toList();
    }


    @Transactional
    public JoinResult handleUserJoin(UUID roomId,
                                     UUID userId,
                                     int limit) {

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        boolean alreadyParticipant =
                chatParticipantsRepository
                        .existsByChatRoomIdAndUserId(roomId, userId);

        if (!alreadyParticipant) {

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            ChatParticipants participant = new ChatParticipants();
            participant.setChatRoom(room);
            participant.setUser(user);

            chatParticipantsRepository.save(participant);
        }

        Pageable pageable = PageRequest.of(0, limit);

        List<Message> messages =
                messageRepository
                        .findTopByChatRoomOrderByCreatedAtDesc(room, pageable);

        List<Chat> history = messages.stream()
                .map(this::convertToChatDto)
                .toList();

        return new JoinResult(history);
    }

    public Chat convertToChatDto(Message message) {

        Chat chat = new Chat();
        chat.setId(message.getId().toString());
        chat.setRoomId(message.getChatRoom().getId().toString());
        chat.setUserId(message.getSender().getId().toString());
        chat.setName(message.getSender().getName());
        chat.setMessage(message.getContent());
        chat.setTimestamp(message.getCreatedAt().toEpochMilli());
        chat.setUpvotes(0L);

        return chat;
    }
}