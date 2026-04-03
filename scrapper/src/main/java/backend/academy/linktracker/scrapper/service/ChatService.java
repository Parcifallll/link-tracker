package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public void register(long chatId) {
        if (chatRepository.exists(chatId)) {
            throw new ChatAlreadyExistsException(chatId);
        }
        chatRepository.save(new Chat(chatId));
    }

    public void delete(long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        chatRepository.delete(chatId);
    }

    public boolean exists(long chatId) {
        return chatRepository.exists(chatId);
    }
}
