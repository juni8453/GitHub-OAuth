package com.oauth.OAuth.login;

import com.oauth.OAuth.login.dto.AccessToken;
import com.oauth.OAuth.login.dto.GitHubUserInfo;
import com.oauth.OAuth.user.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/* OAuth2 검증 순서
 * TODO
 *  ---- 1, 2번은 [getCode] 메서드에서 진행한다 ---
 *  1. Client 는 OAuth 에게 Authorization Code 를 요청한다.
 *  2. 요청을 받은 OAuth 는, Redirect URL 을 통해 Client 에게 Code 를 보낸다. (state 등 여러가지 정보도 보낼 수 있다)
 *  ---- 현재 Client 에 Code 가 부여된 상태 -----
 *  ---- 3,4,5 번은 [getToken] 메서드에서 진행한다 ---
 *  3. Client 는 부여받은 Code 를 서버에 보낸다.
 *  4. 서버는 다시 OAuth 에게 Code 를 보내며 OAuth 는 자신이 최초로 Client 에게 발급한 Code 와 동일한지 확인한다.
 *  5. 맞다면 서버에 Access Token 을 부여하고, 서버는 Client 에게 Access Token 을 전달한다.
 * */

@RestController
public class LoginController {

    private final LoginService loginService;
    private final UserRepository userRepository;

    public LoginController(LoginService loginService, UserRepository userRepository) {
        this.loginService = loginService;
        this.userRepository = userRepository;
    }

    @GetMapping("/loginPage")
    public ResponseEntity<String> loginPage() {
        return ResponseEntity.ok("login page 입니다.");
    }

    @GetMapping("/")
    // HttpSession 이 매개변수라면 SessionId 는 브라우저에 무조건 생성된다.
    public ResponseEntity<String> home(HttpServletResponse response, HttpSession session) {
        GitHubUserInfo checkSession = (GitHubUserInfo) session.getAttribute("userInfo");

        // TODO :
        //  첫 번째 검증 (세션이 존재하는가 ?)
        //  두 번째 검증 (Session 내부에 gitHubUserInfo 객체가 있지만, 내부 데이터를 확인해서 DB 와 일치하는 값이 있는가 ?)
        if ((checkSession == null) || userRepository.findByUsername(checkSession.getId()).isEmpty()) {
            // 로그인 화면으로 보낸다.
            try {
                response.sendRedirect("http://localhost:8080/loginPage");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ResponseEntity.ok("Home Page 입니다.");
    }

    @GetMapping("/login")
    public RedirectView getCode(RedirectAttributes redirectAttributes) {
        return loginService.requestCode(redirectAttributes);
    }

    // 임시 코드를 리다이렉트 받아 처리하는 컨트롤러
    // 앞 과정에 의해 임시 Code, state 가 파라미터 값으로 넘어오는데, 이걸 이용해 Access Token 값을 구하고 저장한다.
    // 넘어온 임시 Code 를 OAuth 에서 검증하고, 맞다면 Access Token 을 Server 에 부여한 뒤 다시 Client 로 전달한다.
    @GetMapping("/login/oauth")
    public ResponseEntity<HttpHeaders> getToken(@RequestParam String code, @RequestParam String state, HttpServletRequest request) {

        // OAuth 에서 받아온 임시 Code 와 state 매개변수를 사용해 AccessToken 을 가져와 AccessToken 클래스에 저장한다.
        AccessToken accessTokenInfo = loginService.getAccessToken(code, state);
        System.out.println("AccessToken 저장 완료 - " + accessTokenInfo);

        // 저장된 AccessToken 값을 활용해 유저에 대한 정보를 요청하자.
        GitHubUserInfo gitHubUserInfo = loginService.getGitHubUserInfo(accessTokenInfo);
        System.out.println("User 정보 - " + gitHubUserInfo);

        HttpSession session = request.getSession();
        session.setAttribute("userInfo", gitHubUserInfo);

        // 이제 DB 에 유저를 저장하고 해당 데이터를 가지고 Header 에 Location 셋팅한 뒤 홈페이지로 리다이렉트
        return loginService.saveMember(gitHubUserInfo);
    }
}
