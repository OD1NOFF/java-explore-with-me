package ru.practicum.user.service;

import java.util.List;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

public interface UserService {

    UserDto addUser(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);
}
