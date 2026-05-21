package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatJpaRepository extends JpaRepository<Chat, Long> {}
