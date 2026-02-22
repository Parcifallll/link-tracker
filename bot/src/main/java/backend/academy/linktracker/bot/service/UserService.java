package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.model.User;
import backend.academy.linktracker.bot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public boolean isNewUser(long chatId) {
        return !userRepository.exists(chatId);
    }

    public void registerUser(long chatId) {
        userRepository.save(new User(chatId));
    }
}
