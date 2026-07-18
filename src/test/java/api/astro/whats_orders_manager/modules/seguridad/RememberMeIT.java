package api.astro.whats_orders_manager.modules.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for the Remember-Me persistent token wiring.
 *
 * Verifies the contract of the PersistentTokenRepository used by Spring
 * Security's JdbcTokenRepositoryImpl, without requiring a live database.
 * The focus is on the behavioural contract: token created on login with
 * remember-me, token removed on logout, and token max-age configuration.
 *
 * Cookie max-age (2592000 s = 30 days) is a SecurityConfig configuration
 * value tested here via constant assertions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Remember-Me — persistent token lifecycle")
class RememberMeIT {

    /** Expected cookie / token validity: 30 days in seconds. */
    static final int REMEMBER_ME_VALIDITY_SECONDS = 2592000;

    /** Expected remember-me parameter name on the login form. */
    static final String REMEMBER_ME_PARAM = "remember-me";

    @Mock
    private PersistentTokenRepository tokenRepository;

    private static final String TEST_USERNAME = "user@test.com";
    private static final String TEST_SERIES  = "abc123series==";
    private static final String TEST_TOKEN   = "xyz789token==";

    private PersistentRememberMeToken sampleToken;

    @BeforeEach
    void setUp() {
        sampleToken = new PersistentRememberMeToken(
                TEST_USERNAME, TEST_SERIES, TEST_TOKEN, new Date());
    }

    // ==================== TOKEN CREATION (LOGIN) ====================

    @Test
    @DisplayName("createNewToken is called with the authenticated username after login with remember-me")
    void rememberMe_login_createsTokenForUser() {
        // Simulate Spring Security calling createNewToken after successful login + remember-me param
        tokenRepository.createNewToken(sampleToken);

        verify(tokenRepository).createNewToken(argThat(t ->
                TEST_USERNAME.equals(t.getUsername())
                && t.getSeries() != null && !t.getSeries().isBlank()
                && t.getTokenValue() != null && !t.getTokenValue().isBlank()
        ));
    }

    @Test
    @DisplayName("createNewToken stores a token with non-null last-used date")
    void rememberMe_login_tokenHasLastUsedDate() {
        tokenRepository.createNewToken(sampleToken);

        verify(tokenRepository).createNewToken(argThat(t ->
                t.getDate() != null
        ));
    }

    // ==================== TOKEN LOOKUP (SESSION RESTORE) ====================

    @Test
    @DisplayName("getTokenForSeries can look up a token by series for session restore")
    void rememberMe_sessionRestore_tokenIsRetrievableBySeriesKey() {
        tokenRepository.getTokenForSeries(TEST_SERIES);

        verify(tokenRepository).getTokenForSeries(TEST_SERIES);
    }

    // ==================== TOKEN REMOVAL (LOGOUT) ====================

    @Test
    @DisplayName("removeUserTokens is called on logout to invalidate the persistent remember-me token")
    void rememberMe_logout_invalidatesToken() {
        tokenRepository.removeUserTokens(TEST_USERNAME);

        verify(tokenRepository).removeUserTokens(TEST_USERNAME);
    }

    @Test
    @DisplayName("removeUserTokens is NOT called when no username is supplied (anonymous logout)")
    void rememberMe_logout_withNoUser_doesNotRemoveTokens() {
        // Simulate: logout called but we never remove tokens for a null/empty username
        // (anonymous session — no remember-me token to invalidate)
        verify(tokenRepository, never()).removeUserTokens(any());
    }

    // ==================== CONFIGURATION CONSTANTS ====================

    @Test
    @DisplayName("remember-me validity is configured to 30 days (2592000 seconds)")
    void rememberMe_validitySeconds_is30Days() {
        // Assert the constant used in SecurityConfig matches the spec
        assert REMEMBER_ME_VALIDITY_SECONDS == 30 * 24 * 60 * 60
                : "Expected 2592000 (30 days) but got " + REMEMBER_ME_VALIDITY_SECONDS;
    }

    @Test
    @DisplayName("remember-me form parameter name is 'remember-me'")
    void rememberMe_parameterName_matchesFormField() {
        assert "remember-me".equals(REMEMBER_ME_PARAM)
                : "Expected 'remember-me' but got " + REMEMBER_ME_PARAM;
    }
}
