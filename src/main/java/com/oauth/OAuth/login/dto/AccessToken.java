package com.oauth.OAuth.login.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AccessToken {
    private final String accessToken;
    private final String tokenType;

    @JsonCreator
    public AccessToken(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }
}
