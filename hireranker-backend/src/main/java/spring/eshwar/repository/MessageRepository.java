package spring.eshwar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.eshwar.entity.Message;
import spring.eshwar.entity.MessageReadStatus;
import spring.eshwar.entity.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySender(User sender);

    List<Message> findByReceiver(User receiver);

    List<Message> findBySenderOrderBySentAtDesc(User sender);

    List<Message> findByReceiverOrderBySentAtDesc(User receiver);

    List<Message> findBySenderOrReceiverOrderBySentAtDesc(User sender, User receiver);

    List<Message> findByReceiverAndReadStatus(User receiver, MessageReadStatus readStatus);

    List<Message> findByReceiverAndReadStatusOrderBySentAtDesc(User receiver, MessageReadStatus readStatus);

    @Query("SELECT m FROM Message m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.sentAt ASC")
    List<Message> findConversationBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT m FROM Message m WHERE (m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR (m.sender.id = :user2Id AND m.receiver.id = :user1Id) ORDER BY m.sentAt ASC")
    List<Message> findConversationBetweenUserIds(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
