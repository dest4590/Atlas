package org.collapseloader.atlas.util.passwords;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.collapseloader.atlas.domain.users.entity.User;
import org.collapseloader.atlas.domain.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PasswordUpgradeSuccessHandler
        extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final HybridPasswordEncoder encoder;

    public PasswordUpgradeSuccessHandler(UserRepository userRepository,
                                         @Qualifier("hybridPasswordEncoder") HybridPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void onAuthenticationSuccess(
            @NonNull
            HttpServletRequest request,
            @NonNull
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        User user = (User) authentication.getPrincipal();

        if (user == null) return;

        if (encoder.isDjangoHash(user.getPassword())) {
            String rawPassword = request.getParameter("password");

            if (rawPassword != null) {
                user.setPassword(encoder.encode(rawPassword));
                userRepository.save(user);
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
