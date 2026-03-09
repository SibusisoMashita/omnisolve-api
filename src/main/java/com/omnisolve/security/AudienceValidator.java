package com.omnisolve.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error ERROR =
            new OAuth2Error("invalid_token", "The required audience is missing", null);

    private final String expectedAudience;
    private final List<String> claimCandidates;

    public AudienceValidator(String expectedAudience, List<String> claimCandidates) {
        this.expectedAudience = expectedAudience;
        this.claimCandidates = claimCandidates;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        for (String claim : claimCandidates) {
            Object value = token.getClaim(claim);
            if (value instanceof String stringValue && expectedAudience.equals(stringValue)) {
                return OAuth2TokenValidatorResult.success();
            }
            if (value instanceof Collection<?> values && values.contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        return OAuth2TokenValidatorResult.failure(ERROR);
    }
}

