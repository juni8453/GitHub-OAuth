package com.oauth.OAuth.login;

import com.oauth.OAuth.login.dto.AccessToken;
import com.oauth.OAuth.login.dto.GitHubUserInfo;
import com.oauth.OAuth.user.User;
import com.oauth.OAuth.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class LoginService {

    private final UserRepository userRepository;
    private final String clientId;
    private final String redirectUrl;
    private final String loginUrl;
    private final String state;
    private final String clientSecret;
    private final String tokenUrl;
    private final String userUrl;

    public LoginService(
            UserRepository userRepository,
            @Value("${oauth2.user.github.client-id}") String clientId,
            @Value("${oauth2.user.github.redirect-url}") String redirectUrl,
            @Value("${oauth2.user.github.login-url}") String loginUrl,
            @Value("${oauth2.user.github.client-secret}") String clientSecret,
            @Value("${oauth2.user.github.token-url}") String tokenUrl,
            @Value("${oauth2.user.github.user-url}") String userUrl) {

        this.userRepository = userRepository;
        this.clientId = clientId;
        this.redirectUrl = redirectUrl;
        this.loginUrl = loginUrl;
        this.state = UUID.randomUUID().toString();
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.userUrl = userUrl;
    }

    // TODO 필수 매개변수 clientId, redirectUrl(callback URL) 등을 넣고 https://github.com/login/oauth/authorize 로 요청 보내기
    public RedirectView requestCode(RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("client_id", clientId);
        redirectAttributes.addAttribute("redirect_url", redirectUrl);
        redirectAttributes.addAttribute("state", state);

        return new RedirectView(loginUrl);
    }

    // TODO 필수 매개변수 client_id, client_secret, code, redirect_uri 를 담고 https://github.com/login/oauth/access_token 로 요청 보내기
    public RedirectView requestToken(String code, String state, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("client_id", clientId);
        redirectAttributes.addAttribute("redirect_url", redirectUrl);
        redirectAttributes.addAttribute("client_secret", clientSecret);
        redirectAttributes.addAttribute("code", code);
        redirectAttributes.addAttribute("state", state);

        return new RedirectView(tokenUrl);
    }

    // TODO
    //  필수 매개변수를 POST https://github.com/login/oauth/access_token 로 보내고 AccessToken 을 받아온 뒤, 내 Token 클래스에 저장하는 과정이다.
    //  1. header 에 Accept / body 에 필수 매개변수 client_id, client_secret, code 를 넣는다.
    //  2. 셋팅한 값을 HttpEntity 를 통해 합치고, POST https://github.com/login/oauth/access_token 으로 보낸다.
    public AccessToken getAccessToken(String code, String state) {
        Map<String, String> bodies = new HashMap<>();
        bodies.put("client_id", clientId);
        bodies.put("client_secret", clientSecret);
        bodies.put("code", code);
        bodies.put("state", state);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // RestTemplate -> 지정된 URL, HttpMethod, HttpEntity 로 Http 서버와 통신한 뒤, 응답을 JSON 형식으로 지정한 클래스에 파싱하는 클래스
        // 파싱되는 클래스 변수에 @JsonProperty 어노테이션이 달려있어야 한다.
        HttpEntity<Object> request = new HttpEntity<>(bodies, headers);
        ResponseEntity<AccessToken> response = new RestTemplate().postForEntity(tokenUrl, request, AccessToken.class);

        // 지정한 클래스에 Json 타입 데이터를 파싱한 뒤 body 를 리턴
        return response.getBody();
    }

    // TODO
    //  AccessToken 을 활용해 사용자 정보를 가져온다.
    public GitHubUserInfo getGitHubUserInfo(AccessToken accessTokenInfo) {

        // 메뉴얼에 나온 그대로 Header 정보를 넣어줘야 한다.
        // Authorization: token OAUTH-TOKEN
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + accessTokenInfo.getAccessToken());

        HttpEntity<Object> request = new HttpEntity<>(headers);
        ResponseEntity<GitHubUserInfo> response = new RestTemplate().exchange(userUrl, HttpMethod.GET, request, GitHubUserInfo.class);

        return response.getBody();
    }

    public ResponseEntity<HttpHeaders> saveMember(GitHubUserInfo gitHubUserInfo) {
        userRepository.save(new User(gitHubUserInfo));

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("http://localhost:8080/"));

        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
}
