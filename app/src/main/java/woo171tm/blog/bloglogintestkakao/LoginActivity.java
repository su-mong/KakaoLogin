package woo171tm.blog.bloglogintestkakao;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.ApiErrorCode;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.callback.UnLinkResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.OptionalBoolean;
import com.kakao.util.exception.KakaoException;

/*
  2019.02.06 ~ 2019.02.19 made by Candykick(KR)
  카카오 로그인 API 블로그 예제

  로그인 Activity이자 앱 시작 시 맨 처음 나오는 Activity.
  사용한 API: 카카오 로그인 API 1.16.0

  이 소스 코드는 개발중이신 앱에 그대로 가져다가 쓰셔도 무방합니다.
  단, 이 소스 코드를 가지고 그대로 본인의 블로그 및 책 등에 사용하는 것은 허용하지 않습니다.
 */

public class LoginActivity extends AppCompatActivity {

    //카카오 로그인 콜백 선언
    private SessionCallback sessionCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //카카오 로그인 콜백 초기화
        sessionCallback = new SessionCallback();
        Session.getCurrentSession().addCallback(sessionCallback);
        //앱 실행 시 로그인 토큰이 있으면 자동으로 로그인 수행
        Session.getCurrentSession().checkAndImplicitOpen();

        //커스텀 카카오 로그인 버튼
        Button btnLoginKakao = findViewById(R.id.kakaoLoginButton2);
        btnLoginKakao.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Session.getCurrentSession().open(AuthType.KAKAO_LOGIN_ALL, LoginActivity.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //카카오 로그인 화면에서 값이 넘어온 경우 처리
        if(Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
    }

    @Override
    protected void onDestroy() {
        //Activity Destroy 시 카카오 로그인 콜백 제거
        //이 코드가 없으면 타 로그인 플랫폼과 연동 시 오류가 발생할 가능성이 높다.
        super.onDestroy();
        Session.getCurrentSession().removeCallback(sessionCallback);
    }

    //카카오 로그인 콜백
    private class SessionCallback implements ISessionCallback {
        @Override
        public void onSessionOpened() { //세션이 성공적으로 열린 경우
            UserManagement.getInstance().me(new MeV2ResponseCallback() { //유저 정보를 가져온다.
                @Override
                public void onFailure(ErrorResult errorResult) { //유저 정보를 가져오는 데 실패한 경우
                    int result = errorResult.getErrorCode(); //오류 코드를 받아온다.

                    if(result == ApiErrorCode.CLIENT_ERROR_CODE) { //클라이언트 에러인 경우: 네트워크 오류
                        Toast.makeText(getApplicationContext(), "네트워크 연결이 불안정합니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else { //클라이언트 에러가 아닌 경우: 기타 오류
                        Toast.makeText(getApplicationContext(),"로그인 도중 오류가 발생했습니다: "+errorResult.getErrorMessage(),Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) { //세션이 도중에 닫힌 경우
                    Toast.makeText(getApplicationContext(),"세션이 닫혔습니다. 다시 시도해 주세요: "+errorResult.getErrorMessage(),Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(MeV2Response result) { //유저 정보를 가져오는데 성공한 경우
                    String needsScopeAutority = ""; //이메일, 성별, 연령대, 생일 정보 가져오는 권한 체크용

                    if(result.getKakaoAccount().needsScopeAccountEmail()) { //이메일 정보를 가져오는 데 사용자가 동의하지 않은 경우
                        needsScopeAutority = needsScopeAutority + "이메일";
                    }
                    if(result.getKakaoAccount().needsScopeGender()) { //성별 정보를 가져오는 데 사용자가 동의하지 않은 경우
                        needsScopeAutority = needsScopeAutority + ", 성별";
                    }
                    if(result.getKakaoAccount().needsScopeAgeRange()) { //연령대 정보를 가져오는 데 사용자가 동의하지 않은 경우
                        needsScopeAutority = needsScopeAutority + ", 연령대";
                    }
                    if(result.getKakaoAccount().needsScopeBirthday()) { //생일 정보를 가져오는 데 사용자가 동의하지 않은 경우
                        needsScopeAutority = needsScopeAutority + ", 생일";
                    }

                    if(needsScopeAutority.length() != 0) { //거절된 권한이 있는 경우
                        //거절된 권한을 허용해달라는 Toast 메세지 출력
                        if(needsScopeAutority.charAt(0) == ',') {
                            needsScopeAutority = needsScopeAutority.substring(2);
                        }
                        Toast.makeText(getApplicationContext(), needsScopeAutority+"에 대한 권한이 허용되지 않았습니다. 개인정보 제공에 동의해주세요.", Toast.LENGTH_SHORT).show();

                        //회원탈퇴 수행
                        //회원탈퇴에 대한 자세한 내용은 MainActivity의 회원탈퇴 버튼 참고
                        UserManagement.getInstance().requestUnlink(new UnLinkResponseCallback() {
                            @Override
                            public void onFailure(ErrorResult errorResult) {
                                int result = errorResult.getErrorCode();

                                if(result == ApiErrorCode.CLIENT_ERROR_CODE) {
                                    Toast.makeText(getApplicationContext(), "네트워크 연결이 불안정합니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), "오류가 발생했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onSessionClosed(ErrorResult errorResult) {
                                Toast.makeText(getApplicationContext(), "로그인 세션이 닫혔습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onNotSignedUp() {
                                Toast.makeText(getApplicationContext(), "가입되지 않은 계정입니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onSuccess(Long result) { }
                        });
                    } else { //모든 정보를 가져오도록 허락받았다면
                        //MainActivity로 넘어가면서 유저 정보를 같이 넘겨줌
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("name", result.getNickname()); //유저 이름(String)
                        intent.putExtra("profile", result.getProfileImagePath()); //유저 프로필 사진 주소(String)

                        if (result.getKakaoAccount().hasEmail() == OptionalBoolean.TRUE)
                            intent.putExtra("email", result.getKakaoAccount().getEmail()); //이메일이 있다면 -> 이메일 값 넘겨줌(String)
                        else
                            intent.putExtra("email", "none"); //이메일이 없다면 -> 이메일 자리에 none 집어넣음.
                        if (result.getKakaoAccount().hasAgeRange() == OptionalBoolean.TRUE)
                            intent.putExtra("ageRange", result.getKakaoAccount().getAgeRange().getValue()); //연령대 정보 있다면 -> 연령대 정보를 String으로 변환해서 넘겨줌
                        else
                            intent.putExtra("ageRange", "none");
                        if (result.getKakaoAccount().hasGender() == OptionalBoolean.TRUE)
                            intent.putExtra("gender", result.getKakaoAccount().getGender().getValue()); //성별 정보가 있다면 -> 성별 정보를 String으로 변환해서 넘겨줌
                        else
                            intent.putExtra("gender", "none");
                        if (result.getKakaoAccount().hasBirthday() == OptionalBoolean.TRUE)
                            intent.putExtra("birthday", result.getKakaoAccount().getBirthday()); //생일 정보가 있다면 -> 생일 정보를 String으로 변환해서 넘겨줌
                        else
                            intent.putExtra("birthday", "none");

                        startActivity(intent);
                        finish();
                    }
                }
            });
        }

        @Override
        public void onSessionOpenFailed(KakaoException e) { //세션을 여는 도중 오류가 발생한 경우 -> Toast 메세지를 띄움.
            Toast.makeText(getApplicationContext(), "로그인 도중 오류가 발생했습니다. 인터넷 연결을 확인해주세요: "+e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
