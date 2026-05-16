package org.godn.rc.repository;

import org.godn.rc.entity.ChatRoom;
import org.godn.rc.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByChatRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

    @Query("""
       SELECT m FROM Message m
       WHERE m.chatRoom = :room
       ORDER BY m.createdAt DESC
       """)
    List<Message> findTopByChatRoomOrderByCreatedAtDesc(
            @Param("room") ChatRoom room,
            Pageable pageable
    );
}
