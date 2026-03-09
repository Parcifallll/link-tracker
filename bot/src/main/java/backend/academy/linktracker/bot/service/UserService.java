package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.model.User;
import backend.academy.linktracker.bot.model.UserSession;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.repository.UserRepository;
import backend.academy.linktracker.bot.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    public boolean isNewUser(long chatId) {
        return !userRepository.exists(chatId);
    }

    public void registerUser(long chatId) {
        userRepository.save(new User(chatId));
    }

    public UserState getState(long chatId) {
        return userSessionRepository.findById(chatId).getState();
    }

    public void setState(long chatId, UserState state) {
        UserSession session = userSessionRepository.findById(chatId);
        session.setState(state);
        userSessionRepository.save(chatId, session);
    }

    public void setPendingLink(long chatId, URI link) {
        UserSession session = userSessionRepository.findById(chatId);
        session.setPendingLink(link);
        userSessionRepository.save(chatId, session);
    }

    public URI getPendingLink(long chatId) {
        return userSessionRepository.findById(chatId).getPendingLink();
    }

    public void setTags(long chatId, List<String> tags) {
        UserSession session = userSessionRepository.findById(chatId);
        session.setTags(tags);
        userSessionRepository.save(chatId, session);
    }

    public List<String> getTags(long chatId) {
        return userSessionRepository.findById(chatId).getTags();
    }

    public void setFilters(long chatId, List<String> filters) {
        UserSession session = userSessionRepository.findById(chatId);
        session.setFilters(filters);
        userSessionRepository.save(chatId, session);
    }

    public List<String> getFilters(long chatId) {
        return userSessionRepository.findById(chatId).getFilters();
    }

    public void resetSession(long chatId) {
        userSessionRepository.save(chatId, new UserSession());
    }
}
