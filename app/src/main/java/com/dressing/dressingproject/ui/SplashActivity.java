package com.dressing.dressingproject.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.dressing.dressingproject.R;
import com.dressing.dressingproject.gcm.RegistrationIntentService;
import com.dressing.dressingproject.manager.NetworkManager;
import com.dressing.dressingproject.manager.PropertyManager;
import com.dressing.dressingproject.ui.models.SignInResult;
import com.dressing.dressingproject.ui.models.UserItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class SplashActivity extends AppCompatActivity
{

    private final int SPLASH_DISPLAY_LENGHT = 1000; //Splash 시간

    Handler mHandler = new Handler(Looper.getMainLooper());
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                doRealStart();
            }
        };
        setUpIfNeeded();


//        findViewById(R.id.splash).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
//                finish();
//            }
//        }, 1000);

    }

    private void goMain() {
        //여기서 네트워크 요청 미리 처리한다음에...
        //블라블라
        startActivity(new Intent(this, MainActivity.class));
        SplashActivity.this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        finish();
//        Intent intent = new Intent(this, StyleActivity.class);
//        intent.putExtra("isFirst",true);
//        startActivity(intent);
//        SplashActivity.this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
//        finish();
    }

    private void goLogin() {

        startActivity(new Intent(this, LoginActivity.class));
        SplashActivity.this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(RegistrationIntentService.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            setUpIfNeeded();
        }
    }

    private void setUpIfNeeded() {
        if (checkPlayServices()) {
            String regId = PropertyManager.getInstance().getRegistrationToken();
            if (!regId.equals("")) {
                doRealStart();
            } else {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }
    }

    private void doRealStart() {

        //로그인 타입 정보와 유저 ID를 가져옴.
        final PropertyManager propertyManager = PropertyManager.getInstance();
        String loginType = propertyManager.getLoginType();
        final String userId = PropertyManager.getInstance().getUserId();

        /**
         * 로그인 한적이 없을 경우 혹은 로그아웃했을 경우 → 로그인 액티비티로 이동
         */
        if(TextUtils.isEmpty(loginType)){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goLogin();
                }
            },SPLASH_DISPLAY_LENGHT);
        }
        /**
         * 이미 로그인 했다면 로그인타입 Check!
         */
        else
        {
            switch (loginType)
            {
                //일반로그인
                case PropertyManager.LOGIN_TYPE_NORMAL:

                    if(!TextUtils.isEmpty(userId))
                    {
                        UserItem item = new UserItem();
                        item.setEmail(userId);
                        item.setPassword(propertyManager.getUserPassword());

                        NetworkManager.getInstance().requestPostSignin(SplashActivity.this, item, new NetworkManager.OnResultListener<SignInResult>() {
                            @Override
                            public void onSuccess(SignInResult result) {
                                int code = result.code;
                                String msg = result.msg;

                                //!TextUtils.isEmpty(result._id)
                                if (msg.equals("Success")) {
                                    // activity start...
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            goMain();
                                        }
                                    }, SPLASH_DISPLAY_LENGHT);
                                }
                                else
                                {
                                    Toast.makeText(SplashActivity.this, "로그인에 실패하였습니다!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFail(int code) {
                                Toast.makeText(SplashActivity.this, "아이디/패스워드를 확인해 주세요!", Toast.LENGTH_SHORT).show();
                                goLogin();//로그인 화면으로 보냄!
                            }
                        });
                    }

                    break;
                //페이스북 로그인
                case PropertyManager.LOGIN_TYPE_FACEBOOK:

                    break;
                //구글 로그인
                case PropertyManager.LOGIN_TYPE_GOOGLE:

                    break;
            }
        }

    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Dialog dialog = apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
                dialog.show();
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

}
