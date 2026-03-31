package com.diplome.game_recommendation.helpers.models;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import java.util.Collections;
import java.util.Objects;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final String token;

    public ApiKeyAuthentication(String token) {
        super(Collections.emptyList());
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return "ApiKeyUser";
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ApiKeyAuthentication other)) {
            return false;
        }
        return Objects.equals(getPrincipal(), other.getPrincipal())
            && Objects.equals(getCredentials(), other.getCredentials());
}

    @Override
    public int hashCode() {
        return Objects.hash(getPrincipal(), getCredentials());
    }

}
