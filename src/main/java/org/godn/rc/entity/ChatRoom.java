package org.godn.rc.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private ChatRoomType type;

    private String name;

    @NotBlank(message = "Join ID is required")
    @Pattern(
            regexp = "^[A-Z0-9]{3}-[A-Z0-9]{3}$",
            message = "Join ID must be 6 uppercase letters or digits."
    )
    @Column(unique = true, nullable = false, length = 6)
    private String joinId;

    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<ChatParticipants> participants;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private List<Message> messages;
}
