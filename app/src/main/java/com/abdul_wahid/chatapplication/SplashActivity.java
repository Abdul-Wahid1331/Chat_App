package com.abdul_wahid.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.abdul_wahid.chatapplication.Model.UserModel;
import com.abdul_wahid.chatapplication.utils.AndroidUtil;
import com.abdul_wahid.chatapplication.utils.FirebaseUtil;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getIntent().getExtras() != null) {
            String userId = getIntent().getExtras().getString("userId");

            // Check if userId is not null or empty
            if (userId != null && !userId.isEmpty()) {
                FirebaseUtil.allUserCollectionReference().document(userId).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                UserModel model = task.getResult().toObject(UserModel.class);

                                // Proceed to MainActivity and ChatActivity
                                Intent mainIntent = new Intent(this, MainActivity.class);
                                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(mainIntent);

                                Intent chatIntent = new Intent(this, ChatActivity.class);
                                AndroidUtil.passUserModelAsIntent(chatIntent, model);
                                chatIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(chatIntent);
                                finish();
                            } else {
                                Log.e(TAG, "Failed to retrieve user document or document not found.");
                                handleAuthentication();
                            }
                        });
            } else {
                Log.e(TAG, "User ID is null or empty.");
                handleAuthentication();
            }
        } else {
            new Handler().postDelayed(() -> {
                handleAuthentication();
            }, 1000);
        }
    }

    private void handleAuthentication() {
        if (FirebaseUtil.isLoggedIn()) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginPhoneNumber_Activity.class));
        }
        finish();
    }
}
