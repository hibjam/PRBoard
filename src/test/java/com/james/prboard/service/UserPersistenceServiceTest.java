package com.james.prboard.service;

import com.james.prboard.domain.User;
import com.james.prboard.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPersistenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserPersistenceService userPersistenceService;

    @Test
    void resolveOrCreate_returnsExistingUser_whenAuth0IdMatches() {
        // Given
        String auth0Id = "auth0|existing";
        User existingUser = new User();
        existingUser.setAuth0Id(auth0Id);
        existingUser.setEmail("existing@example.com");

        when(userRepository.findByAuth0Id(auth0Id)).thenReturn(Optional.of(existingUser));

        // When
        User result = userPersistenceService.resolveOrCreate(auth0Id, "existing@example.com");

        // Then
        assertThat(result).isEqualTo(existingUser);
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveOrCreate_linksAuth0Id_whenEmailMatchesExistingUser() {
        // Given
        String auth0Id = "auth0|new-provider";
        String email = "existing@example.com";

        User existingUser = new User();
        existingUser.setEmail(email);

        when(userRepository.findByAuth0Id(auth0Id)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        // When
        User result = userPersistenceService.resolveOrCreate(auth0Id, email);

        // Then
        assertThat(result.getAuth0Id()).isEqualTo(auth0Id);
        verify(userRepository).save(existingUser);
    }

    @Test
    void resolveOrCreate_createsNewUser_whenNoMatchExists() {
        // Given
        String auth0Id = "auth0|brand-new";
        String email = "new@example.com";

        when(userRepository.findByAuth0Id(auth0Id)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        User result = userPersistenceService.resolveOrCreate(auth0Id, email);

        // Then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAuth0Id()).isEqualTo(auth0Id);
        assertThat(captor.getValue().getEmail()).isEqualTo(email);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void resolveOrCreate_createsUserWithAuth0IdAsEmail_whenEmailIsNull() {
        // Given
        String auth0Id = "auth0|no-email";

        when(userRepository.findByAuth0Id(auth0Id)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        User result = userPersistenceService.resolveOrCreate(auth0Id, null);

        // Then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAuth0Id()).isEqualTo(auth0Id);
        assertThat(captor.getValue().getEmail()).isEqualTo(auth0Id);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void resolveOrCreate_doesNotSaveExistingUser_whenAuth0IdAlreadyMatches() {
        // Given
        String auth0Id = "auth0|existing";
        User existingUser = new User();
        existingUser.setAuth0Id(auth0Id);

        when(userRepository.findByAuth0Id(auth0Id)).thenReturn(Optional.of(existingUser));

        // When
        userPersistenceService.resolveOrCreate(auth0Id, "any@example.com");

        // Then
        verify(userRepository, never()).save(any());
    }
}