package org.godn.rc.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.godn.rc.auth.model.User;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "messages", indexes = {@Index(name = "idx_chatroom_created", columnList = "chat_room_id, createdAt")})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column
    @NotBlank(message = "Repository URL is required")
    @Pattern(regexp = "^(https?://).+", message = "Invalid Repository URL format")
    private String fileURL;

    private MessageType messageType;

    private Instant createdAt;
}
