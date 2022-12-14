package kr.co.reverse.auth.api.service;

import kr.co.reverse.auth.api.request.LoginReq;
import kr.co.reverse.auth.api.request.SignupReq;
import kr.co.reverse.auth.api.request.UserReq;
import kr.co.reverse.auth.api.response.AuthRes;
import kr.co.reverse.auth.api.response.UserRes;
import kr.co.reverse.auth.common.exception.EmailDuplicateException;
import kr.co.reverse.auth.common.exception.IncorrectEmailOrPasswordException;
import kr.co.reverse.auth.common.jwt.JwtTokenProvider;
import kr.co.reverse.auth.common.util.CookieUtil;
import kr.co.reverse.auth.db.entity.Auth;
import kr.co.reverse.auth.db.entity.Authority;
import kr.co.reverse.auth.db.entity.StatusCode;
import kr.co.reverse.auth.db.entity.UserStatus;
import kr.co.reverse.auth.db.repository.AuthRepository;
import kr.co.reverse.auth.db.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final AuthRepository authRepository;
    private final CookieUtil cookieUtil;
    private final UserStatusRepository userStatusRepository;

    @Value("${user.redirect-uri}")
    private String USER_REDIRECT_URI;
    private final String REFRESH_TOKEN = "refreshToken";
    private final String ACCESS_TOKEN = "accessToken";
    private final String AUTHORIZATION = "Authorization";

    @Transactional
    public void signUp(SignupReq signInfo){

        if(authRepository.existsByEmail(signInfo.getEmail())){
            throw new IllegalArgumentException();
        }else{

            UserStatus status = UserStatus.builder()
                    .userStatusCode(StatusCode.ACTIVE)
                    .build();
            userStatusRepository.save(status);

            Auth auth = Auth.builder()
                    .email(signInfo.getEmail())
                    .password(passwordEncoder.encode(signInfo.getPassword()))
                    .authority(Authority.ROLE_USER)
                    .userStatus(status)
                    .build();
            authRepository.save(auth);

            //user ????????? ????????? ??????
            UserReq req = UserReq.builder()
                    .authId(auth.getId().toString())
                    .nickname(signInfo.getNickname())
                    .build();

            connectCreateUser(req);

        }
    }

    public AuthRes login(LoginReq loginInfo, HttpServletResponse response) {

        if(authRepository.findByEmail(loginInfo.getEmail()).orElse(null) == null){
            throw new IncorrectEmailOrPasswordException();
        }else{

            //????????? ???????????? ????????? ?????????
            Auth auth = authRepository.findByEmail(loginInfo.getEmail()).get();
            if(auth.getUserStatus().getUserStatusCode().equals(StatusCode.DELETED)){
                throw new IncorrectEmailOrPasswordException();
            }

            //1. email, password??? ???????????? authentication ?????? ??????
            UsernamePasswordAuthenticationToken authenticationToken = loginInfo.toAuthentication();

            //2. ?????? ??????(???????????? ??????)
            // authenticate ???????????? ????????? ??? CustomUserDetailsService ?????? ?????? loadUserByUsername ???????????? ??????
            Authentication authentication = null;
            try{
                authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            }catch (AuthenticationException e){
                throw new IncorrectEmailOrPasswordException();
            }

            //3. ?????? ??????
            AuthRes tokenInfo = jwtTokenProvider.generateTokenDto(authentication);


            String userId = connectGetUserId(auth.getId().toString());


            //4. redis??? refresh token ??????
            redisService.setValues(tokenInfo.getRefreshToken(), userId);
            redisService.setValues(tokenInfo.getAccessToken(), userId);

            //cookie??? ??????
//            response.addHeader(AUTHORIZATION, "Bearer " + tokenInfo.getAccessToken());
//            Cookie cookie = new Cookie("accessToken", tokenInfo.getAccessToken());
            // Cookie cookie = new Cookie(REFRESH_TOKEN, tokenInfo.getRefreshToken());

            Cookie accessCookie = cookieUtil.addAccessCookie(tokenInfo.getAccessToken());
            response.addCookie(accessCookie);

            Cookie refreshCookie = cookieUtil.addRefreshCookie(tokenInfo.getRefreshToken());
            response.addCookie(refreshCookie);

            return tokenInfo;
        }
    }

    @Transactional
    public AuthRes reissue(HttpServletRequest request, HttpServletResponse response) {

        String accessToken = null;
        Cookie[] cookies = request.getCookies();

        for(Cookie cookie : cookies){
            if(cookie.getName().equals(ACCESS_TOKEN)){
                accessToken = cookie.getValue();
                break;
            }
        }
        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);


        cookies = request.getCookies();
        String refreshToken = null;
        for(Cookie cookie : cookies){
            if(cookie.getName().equals(REFRESH_TOKEN)){
                refreshToken = cookie.getValue();
                break;
            }
        }

        String userId = redisService.getValues(refreshToken);

        Auth auth = authRepository.findById(UUID.fromString(connectGetAuthId(userId))).get();

        redisService.checkRefreshToken(authentication.getName(), auth.getEmail());

        // ?????? ?????? ????????? ?????? ?????????
        AuthRes token = jwtTokenProvider.generateTokenDto(authentication);

        //redis??? ?????? accesstoken ?????? ??? ???????????? ??????
        redisService.deleteValues(accessToken);
        redisService.setValues(token.getAccessToken(), userId);

//        response.addHeader(AUTHORIZATION, "Bearer " + token.getAccessToken());

        Cookie accessCookie = cookieUtil.addAccessCookie(token.getAccessToken());
        response.addCookie(accessCookie);

        return token;
    }

    @Transactional
    public void deleteUser(HttpServletRequest request, HttpServletResponse response){

        String accessToken = null;
        Cookie[] cookies = request.getCookies();

        for(Cookie cookie : cookies){
            if(cookie.getName().equals(ACCESS_TOKEN)){
                accessToken = cookie.getValue();
                break;
            }
        }
        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);


        cookies = request.getCookies();
        String refreshToken = null;
        for(Cookie cookie : cookies){
            if(cookie.getName().equals(REFRESH_TOKEN)){
                refreshToken = cookie.getValue();
                break;
            }
        }

        Auth auth = authRepository.findByEmail(authentication.getName()).get();

        auth.getUserStatus().setUserStatusCode(StatusCode.DELETED);

//        connectDeleteUser();

        //???????????? ?????? ?????? ??????
        redisService.deleteValues(accessToken);
        redisService.deleteValues(refreshToken);

        // connectDeleteUser(auth.getId().toString());

        Cookie accessCookie = new Cookie(REFRESH_TOKEN, null);
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN, null);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        // connectDeleteUser(auth.getId().toString());


    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = null;
        Cookie[] cookies = request.getCookies();

        for(Cookie cookie : cookies){
            if(cookie.getName().equals(ACCESS_TOKEN)){
                accessToken = cookie.getValue();
                break;
            }
        }

        cookies = request.getCookies();
        String refreshToken = null;
        for(Cookie cookie : cookies){
            if(cookie.getName().equals(REFRESH_TOKEN)){
                refreshToken = cookie.getValue();
                break;
            }
        }

        //???????????? ?????? ?????? ??????
        redisService.deleteValues(accessToken);
        redisService.deleteValues(refreshToken);

        //?????? ??????
        Cookie accessCookie = new Cookie(REFRESH_TOKEN, null);
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN, null);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

    }

    public void checkDuplicateEmail(String email) {

        try{
            authRepository.findByEmail(email).get();
            throw new EmailDuplicateException();
        }catch (NoSuchElementException e){
            return;
        }
    }


    public ResponseEntity connectCreateUser(UserReq req){
        RestTemplate restTemplate = new RestTemplate();

        URI uri = UriComponentsBuilder.fromUriString(USER_REDIRECT_URI)
                .path("/create")
                .encode().build().toUri();

        ResponseEntity result = restTemplate.postForEntity(uri, req, ResponseEntity.class);
        return result;
    }

    public String connectGetUserId(String authId){

        RestTemplate restTemplate = new RestTemplate();

        URI uri = UriComponentsBuilder.fromUriString(USER_REDIRECT_URI)
                .path("/uid/{auth_id}")
                .encode().build()
                .expand(authId)
                .toUri();

        ResponseEntity<UserRes> result = restTemplate.getForEntity(uri,UserRes.class);

        return result.getBody().getId();

    }

    public String connectGetAuthId(String userId){

        RestTemplate restTemplate = new RestTemplate();

        URI uri = UriComponentsBuilder.fromUriString(USER_REDIRECT_URI)
                .path("/aid/{user_id}")
                .encode().build()
                .expand(userId)
                .toUri();

        ResponseEntity<UserRes> result = restTemplate.getForEntity(uri,UserRes.class);

        return result.getBody().getId();


    }

    // public void connectDeleteUser(String authId) {
    //     RestTemplate restTemplate = new RestTemplate();

    //     URI uri = UriComponentsBuilder.fromUriString(USER_REDIRECT_URI)
    //             .path("/delete/{auth_id}")
    //             .encode().build()
    //             .expand(authId)
    //             .toUri();

    //     restTemplate.delete(uri);
    // }
}
