package com.abdul_wahid.chatapplication;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.abdul_wahid.chatapplication.Model.UserModel;
import com.abdul_wahid.chatapplication.adapter.SearchUserRecyclerAdapter;
import com.abdul_wahid.chatapplication.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    EditText searchInput;
    RecyclerView recyclerView;
    FrameLayout mainFrameLayout;
    ProgressBar progressBar;

    ChatFragment chatFragment;
    ProfileFragment profileFragment;
    SearchUserRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        chatFragment = new ChatFragment();
        profileFragment = new ProfileFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        searchInput = findViewById(R.id.search_username_input);
        recyclerView = findViewById(R.id.recycler_view);
        mainFrameLayout = findViewById(R.id.main_frame_layout);
        progressBar = findViewById(R.id.progress_bar); // Assuming you have a ProgressBar in your layout

        recyclerView.setVisibility(View.GONE);
        mainFrameLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE); // Initially hide the ProgressBar

        final View rootView = findViewById(android.R.id.content).getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) {
                bottomNavigationView.setVisibility(View.GONE);
            } else {
                bottomNavigationView.setVisibility(View.VISIBLE);
                searchInput.clearFocus();
            }
        });

        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                bottomNavigationView.setVisibility(View.GONE);
            } else {
                bottomNavigationView.setVisibility(View.VISIBLE);
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String searchTerm = charSequence.toString();
                if (searchTerm.isEmpty() || searchTerm.length() < 3) {
                    if (adapter != null) {
                        adapter.stopListening();
                    }
                    recyclerView.setVisibility(View.GONE);
                    mainFrameLayout.setVisibility(View.VISIBLE);
                    return;
                }
                mainFrameLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                setupSearchRecyclerView(searchTerm);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_chat) {
                showChatFragment();
            } else if (item.getItemId() == R.id.menu_profile) {
                showProfileFragment();
            }
            return true;
        });

        bottomNavigationView.setSelectedItemId(R.id.menu_chat);

        getFCMToken();
    }

    @Override
    public void onBackPressed() {
        if (recyclerView.getVisibility() == View.VISIBLE) {
            recyclerView.setVisibility(View.GONE);
            mainFrameLayout.setVisibility(View.VISIBLE);
            if (adapter != null) {
                adapter.stopListening();
            }
            searchInput.setText("");
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void showChatFragment() {
        mainFrameLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, chatFragment).commit();
        searchInput.setVisibility(View.VISIBLE);
    }

    private void showProfileFragment() {
        mainFrameLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, profileFragment).commit();
        searchInput.setVisibility(View.GONE);
    }

    void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.i("My token", token);
                FirebaseUtil.currentUserDetails().update("fcmToken", token);
            }
        });
    }

    void setupSearchRecyclerView(String searchTerm) {
        Query query = FirebaseUtil.allUserCollectionReference()
                .whereGreaterThanOrEqualTo("username", searchTerm)
                .whereLessThanOrEqualTo("username", searchTerm + '\uf8ff');

        FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class).build();

        adapter = new SearchUserRecyclerAdapter(options, getApplicationContext());
        adapter.setOnDataChangedListener(() -> {
            // Hide the ProgressBar when data changes
            progressBar.setVisibility(View.GONE);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
