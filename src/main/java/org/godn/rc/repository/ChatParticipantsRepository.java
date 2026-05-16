package org.godn.rc.repository;

import org.godn.rc.entity.ChatParticipants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatParticipantsRepository extends JpaRepository<ChatParticipants, UUID> {
    boolean existsByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

}
