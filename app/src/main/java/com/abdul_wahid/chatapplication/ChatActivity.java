package com.abdul_wahid.chatapplication;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abdul_wahid.chatapplication.Model.ChatMessageModel;
import com.abdul_wahid.chatapplication.Model.ChatroomModel;
import com.abdul_wahid.chatapplication.Model.UserModel;
import com.abdul_wahid.chatapplication.adapter.ChatRecyclerAdapter;
import com.abdul_wahid.chatapplication.utils.AndroidUtil;
import com.abdul_wahid.chatapplication.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class ChatActivity extends AppCompatActivity {

    private UserModel otherUser;
    private EditText messageInput;
    private ImageButton sendMessageBtn;
    private ImageButton backBtn;
    private TextView otherUsername;
    private RecyclerView recyclerView;
    private String chatroomId;
    private ChatroomModel chatroomModel;
    private ChatRecyclerAdapter adapter;
    private ImageView profilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        initializeViews();
        setupListeners();
        getOrCreateChatroomModel(); // Ensure chatroom model is created or fetched first
    }

    private void initializeViews() {
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currrentUserId(), otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        profilePic = findViewById(R.id.chat_profile_pic_image_view);

        otherUsername.setText(otherUser.getUsername());

        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(this, uri, profilePic);
                    } else {
                        Log.e("ChatActivity", "Error getting profile pic URL", t.getException());
                    }
                });
    }

    private void setupListeners() {
        backBtn.setOnClickListener(view -> onBackPressed());

        sendMessageBtn.setOnClickListener(view -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                if (chatroomModel != null) {
                    sendMessageToUser(message);
                } else {
                    Log.e("ChatActivity", "Cannot send message. Chatroom model is null.");
                }
            }
        });
    }

    private void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        manager.setStackFromEnd(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void sendMessageToUser(String message) {
        if (chatroomModel == null) {
            Log.e("ChatActivity", "Cannot send message. Chatroom model is null.");
            return;
        }

        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currrentUserId());
        chatroomModel.setLastMessage(message);

        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("ChatActivity", "Error updating chatroom", task.getException());
                    }
                });

        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currrentUserId(), Timestamp.now());
        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        messageInput.setText("");
                    } else {
                        Log.e("ChatActivity", "Error sending message", task.getException());
                    }
                });
    }

    private void getOrCreateChatroomModel() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    ChatroomModel chatroom = new ChatroomModel(
                            "chatroom123",
                            Arrays.asList("user1", "user2"),
                            Timestamp.now(),
                            "user1",
                            ""
                    );

                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                            .addOnCompleteListener(setTask -> {
                                if (!setTask.isSuccessful()) {
                                    Log.e("ChatActivity", "Error creating chatroom", setTask.getException());
                                } else {
                                    setupChatRecyclerView(); // Setup RecyclerView after creating the chatroom
                                }
                            });
                } else {
                    setupChatRecyclerView(); // Setup RecyclerView if chatroom already exists
                }
            } else {
                Log.e("ChatActivity", "Error getting chatroom model", task.getException());
            }
        });
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
