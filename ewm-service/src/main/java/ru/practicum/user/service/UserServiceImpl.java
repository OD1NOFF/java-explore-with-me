package ru.practicum.user.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.util.OffsetPageRequest;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserDto addUser(NewUserRequest request) {
        User saved = repository.save(mapper.toEntity(request));
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        List<User> users = (ids == null || ids.isEmpty())
                ? repository.findAll(OffsetPageRequest.of(from, size)).getContent()
                : repository.findAllByIdIn(ids, OffsetPageRequest.of(from, size));
        return users.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        if (!repository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        repository.deleteById(userId);
    }
}
