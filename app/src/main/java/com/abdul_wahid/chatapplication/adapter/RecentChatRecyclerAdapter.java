package com.abdul_wahid.chatapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.abdul_wahid.chatapplication.ChatActivity;
import com.abdul_wahid.chatapplication.Model.ChatroomModel;
import com.abdul_wahid.chatapplication.Model.UserModel;
import com.abdul_wahid.chatapplication.R;
import com.abdul_wahid.chatapplication.utils.AndroidUtil;
import com.abdul_wahid.chatapplication.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options,Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {
        FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean latMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseUtil.currrentUserId());

                        UserModel otherUserModel = task.getResult().toObject(UserModel.class);

                        if (otherUserModel != null) {
                            FirebaseUtil.getOtherProfilePicStorageRef(otherUserModel.getUserId()).getDownloadUrl()
                                    .addOnCompleteListener(t -> {
                                        if (t.isSuccessful() && t.getResult() != null) {
                                            Uri uri = t.getResult();
                                            AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                                        } else {
                                            holder.profilePic.setImageResource(R.drawable.person_icon);
                                        }
                                    });

                            holder.usernameText.setText(otherUserModel.getUsername());
                            if (latMessageSentByMe)
                                holder.lastMessageText.setText("You: " + model.getLastMessage());
                            else
                                holder.lastMessageText.setText(model.getLastMessage());
                            holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));

                            holder.itemView.setOnClickListener(view -> {
                                Intent intent = new Intent(context, ChatActivity.class);
                                AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            });
                        } else {
                            holder.usernameText.setText("Unknown User");
                            holder.lastMessageText.setText("No message available");
                            holder.profilePic.setImageResource(R.drawable.person_icon);
                        }
                    } else {
                        holder.usernameText.setText("Unknown User");
                        holder.lastMessageText.setText("No message available");
                        holder.profilePic.setImageResource(R.drawable.person_icon);
                    }
                });

    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chats_recycler_row,parent,false);
        return new ChatroomModelViewHolder(view);
    }

    static class ChatroomModelViewHolder extends RecyclerView.ViewHolder{

        TextView usernameText;
        TextView lastMessageText;
        TextView lastMessageTime;
        ImageView profilePic;

        public ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);

            usernameText = itemView.findViewById(R.id.username_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.recent_chats_profile_pic_image_view);
        }
    }
}
