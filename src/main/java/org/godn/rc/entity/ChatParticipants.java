package org.godn.rc.entity;

import jakarta.persistence.*;
import org.godn.rc.auth.model.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_room_id", "user_id"}))
public class ChatParticipants {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Instant JoinedAt = Instant.now();
}
