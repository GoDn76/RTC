package org.godn.rc.auth.service;

import org.godn.rc.auth.model.User;
import org.godn.rc.auth.payload.UpdateProfileDto;
import org.godn.rc.auth.payload.UserProfileDto;
import org.godn.rc.auth.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements  ProfileService {

    private final UserRepository userRepository;

    public ProfileServiceImpl (UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public UserProfileDto getUserProfile(String id) {
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDto(user);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(String userId, UpdateProfileDto updateProfileDto) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(updateProfileDto.getName());

        User updatedUser = userRepository.save(user);
        return mapToDto(updatedUser);
    }

    private UserProfileDto mapToDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setUuid(user.getId().toString());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setEmailVerified(user.getEmailVerified());
        return dto;
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public List<UserProfileDto> getUsers(String currUserEmail, String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        return userRepository.searchUsers(currUserEmail, query.trim(), pageable)
                .stream()
                .map(this::mapToDto)
                .toList();
    }
}
