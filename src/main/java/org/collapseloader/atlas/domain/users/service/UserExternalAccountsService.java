package org.collapseloader.atlas.domain.users.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.coyote.BadRequestException;
import org.collapseloader.atlas.domain.users.dto.request.UserExternalAccountRequest;
import org.collapseloader.atlas.domain.users.dto.response.UserExternalAccountResponse;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.entity.UserExternalAccount;
import org.collapseloader.atlas.domain.users.repository.UserExternalAccountRepository;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserExternalAccountsService {
    private final UserRepository userRepository;
    private final UserExternalAccountRepository userExternalAccountRepository;
    private final ObjectMapper objectMapper;

    public UserExternalAccountsService(
            UserRepository userRepository,
            UserExternalAccountRepository userExternalAccountRepository,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userExternalAccountRepository = userExternalAccountRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<UserExternalAccountResponse> getExternalAccounts(User principal) {
        return userExternalAccountRepository.findByUserId(principal.getId()).stream()
                .map(this::mapExternalAccount)
                .toList();
    }

    @Transactional
    public UserExternalAccountResponse addExternalAccount(User principal, UserExternalAccountRequest request)
            throws NotFoundException, BadRequestException {
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException());
        if (request == null) {
            throw new BadRequestException("External account payload is required");
        }

        var account = UserExternalAccount.builder()
                .user(user)
                .displayName(normalizeDisplayName(request.displayName()))
                .metadata(request.metadata())
                .build();
        return mapExternalAccount(userExternalAccountRepository.save(account));
    }

    @Transactional
    public void deleteExternalAccount(User principal, Long accountId) throws NotFoundException, BadRequestException {
        var account = userExternalAccountRepository.findByIdAndUserId(accountId, principal.getId())
                .orElseThrow(() -> new NotFoundException());
        userExternalAccountRepository.delete(account);
    }

    private UserExternalAccountResponse mapExternalAccount(UserExternalAccount account) {
        return new UserExternalAccountResponse(
                account.getId(),
                account.getDisplayName(),
                normalizeMetadata(account.getMetadata()),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }

    private Object normalizeMetadata(JsonNode metadata) {
        if (metadata == null || metadata.isNull()) {
            return null;
        }
        return objectMapper.convertValue(metadata, Object.class);
    }

    private String normalizeDisplayName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
