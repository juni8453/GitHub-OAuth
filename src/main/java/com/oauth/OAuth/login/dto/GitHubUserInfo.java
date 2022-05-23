package com.oauth.OAuth.login.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GitHubUserInfo {
    private final String id;
    private final String idNumber;
    private final String nickname;

    @JsonCreator
    public GitHubUserInfo(
            @JsonProperty("login") String id,
            @JsonProperty("id") String idNumber,
            @JsonProperty("name") String nickname) {
        this.id = id;
        this.idNumber = idNumber;
        this.nickname = nickname;
    }
}
