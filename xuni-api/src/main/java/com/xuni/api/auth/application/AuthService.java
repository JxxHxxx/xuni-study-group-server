package com.xuni.api.auth.application;

import com.xuni.api.auth.dto.request.AuthCodeForm;
import com.xuni.api.auth.dto.request.EmailForm;
import com.xuni.api.auth.dto.request.LoginForm;
import com.xuni.api.auth.dto.request.SignupForm;
import com.xuni.api.auth.infra.AuthCodeRepository;
import com.xuni.api.auth.infra.MemberRepository;
import com.xuni.api.auth.dto.response.CreateAuthCodeEvent;
import com.xuni.api.auth.dto.response.VerifyAuthCodeEvent;
import com.xuni.core.auth.domain.AuthCode;
import com.xuni.core.auth.domain.Member;
import com.xuni.core.auth.domain.UsageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.xuni.core.auth.domain.AuthProvider.*;
import static com.xuni.api.auth.dto.response.AuthResponseMessage.EXISTED_EMAIL;
import static com.xuni.api.auth.dto.response.AuthResponseMessage.LOGIN_FAIL;
import static com.xuni.core.auth.domain.exception.ExceptionMessage.ALREADY_EXIST_EMAIL;
import static com.xuni.core.auth.domain.exception.ExceptionMessage.NOT_EXIST_AUTH_CODE;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final AuthCodeRepository authCodeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreateAuthCodeEvent createAuthCode(EmailForm form) {
        Optional<AuthCode> optionalAuthCode = authCodeRepository.findByEmailAndUsageType(form.email(), UsageType.SIGNUP);
        if (optionalAuthCode.isPresent()) {
            AuthCode authCode = optionalAuthCode.get();
            authCode.regenerate();
            return new CreateAuthCodeEvent(authCode.getAuthCodeId(), authCode.getEmail(), authCode.getValue());
        }

        AuthCode authCode = authCodeRepository.save(new AuthCode(form.email(), UsageType.SIGNUP));
        return new CreateAuthCodeEvent(authCode.getAuthCodeId(), authCode.getEmail(), authCode.getValue());
    }

    public void checkEmailAndAuthProvider(EmailForm form) {
        Optional<Member> oMember = memberRepository.findByLoginInfoEmail(form.email());
        if (oMember.isPresent()) {
            Member member = oMember.get();
            if (member.isAuthProvider(XUNI)) {
                throw new IllegalArgumentException(ALREADY_EXIST_EMAIL);
            }
        }
    }

    @Transactional
    public VerifyAuthCodeEvent verifyAuthCode(AuthCodeForm form) {
        AuthCode authCode = authCodeRepository.findById(form.authCodeId()).orElseThrow(
                () -> new IllegalArgumentException(NOT_EXIST_AUTH_CODE));

        authCode.verifyAuthCode(form.value());

        return new VerifyAuthCodeEvent(authCode.getAuthCodeId());
    }

    @Transactional
    public void signup(SignupForm form) {
        AuthCode authCode = authCodeRepository.findById(form.getAuthCodeId()).orElseThrow(() -> new IllegalArgumentException(NOT_EXIST_AUTH_CODE));
        checkDuplicationOfEmail(authCode.getEmail());

        Member member = authCode.createMember(authCode.getEmail(), passwordEncoder.encrypt(form.getPassword()), form.getName());
        Member saveMember = memberRepository.save(member);
        authCode.setMember(saveMember);
    }

    public MemberDetails login(LoginForm loginForm) {
        Member member = memberRepository.findByLoginInfoEmail(loginForm.getEmail())
                .orElseThrow(() -> new IllegalArgumentException(LOGIN_FAIL));

        member.matches(passwordEncoder.encrypt(loginForm.getPassword()));

        return new SimpleMemberDetails(member.getId(), member.receiveEmail(), member.getName(), member.getAuthority());
    }

    private void checkDuplicationOfEmail(String email) {
        if (memberRepository.findByLoginInfoEmail(email).isPresent()) {
            throw new IllegalArgumentException(EXISTED_EMAIL);
        }
    }
}
