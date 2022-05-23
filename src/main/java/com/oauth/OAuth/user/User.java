package com.oauth.OAuth.user;

import com.oauth.OAuth.login.dto.GitHubUserInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String username;

    public User(GitHubUserInfo gitHubUserInfo) {
        this.username = gitHubUserInfo.getId();
    }
}
