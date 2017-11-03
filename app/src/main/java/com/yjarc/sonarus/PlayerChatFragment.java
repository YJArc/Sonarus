package com.yjarc.sonarus;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.yjarc.sonarus.AppConstants.CASTROOM_ID;


public class PlayerChatFragment extends Fragment {

    TextView chat, chat_hint;
    EditText input_msg;
    Button btn_send_msg;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_player_chat, container, false);

        chat = (TextView) v.findViewById(R.id.player_chat);
        chat_hint = (TextView) v.findViewById(R.id.player_chat_hint);
        btn_send_msg = (Button) v.findViewById(R.id.btn_chat_send);
        input_msg = (EditText) v.findViewById(R.id.input_chat_msg);

        return v;
    }

    ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            appendChatConversation(dataSnapshot);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {}

        };

    DatabaseReference root;
    boolean isChatting;

    public void getChat() {
        Log.e("getChat", "getchat");
        if (CASTROOM_ID == null || CASTROOM_ID.isEmpty()) {
            Log.e("castroom", "NOT SET");
        } else {
            isChatting = true;
            Log.e("castroom", "SET");

            chat_hint.setVisibility(View.INVISIBLE);

            root = FirebaseDatabase.getInstance().getReference().child("Roombase").child(CASTROOM_ID).child("5Messages");
            root.limitToLast(20).addValueEventListener(valueEventListener);

            btn_send_msg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map<String, Object> map = new HashMap<>();
                    String temp_key = root.push().getKey();
                    root.updateChildren(map);

                    DatabaseReference message_root = root.child(temp_key);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("from", "Yohanan");//((MainActivity) getActivity()).getUserName());
                    map2.put("msg", input_msg.getText().toString());

                    message_root.updateChildren(map2);
                    input_msg.setText("");
                }
            });

        }
    }

    public void stopChat(){
        if(isChatting)
            root.removeEventListener(valueEventListener);
        Log.e("chatlisten", "removed");
    }


    String chatName, chatMessage;
    private void appendChatConversation (DataSnapshot dataSnapshot){
        chat.setText("");
        Iterator i = dataSnapshot.getChildren().iterator();

        while (i.hasNext()){
            Iterator j = ((DataSnapshot) i.next()).getChildren().iterator();
            chatName = (String) ((DataSnapshot)j.next()).getValue();
            chatMessage = (String) ((DataSnapshot)j.next()).getValue();

            if (Build.VERSION.SDK_INT >= 24) {
                chat.append(Html.fromHtml(ColorizeTexts(chatName) + ": " + chatMessage + "<br>",Html.FROM_HTML_MODE_LEGACY));
                Log.e(chatName, chatMessage);

            }
            else
            {
                chat.append((Html.fromHtml(ColorizeTexts(chatName)) + ": " + chatMessage + "<br>"));
            }

        }
    }

    // TODO: 5/21/2017 replace html coloring with spannable
    public String ColorizeTexts(String Text){
        int colorchoice = Text.hashCode() % 2;

        switch (colorchoice){
            case 1:
                return "<font color=\"#CCB400\">" + Text + "</font>";
            case 2:
                return "<font color=\"#CCB400\">" + Text + "</font>";
            default:
                return "<font color=\"#CCB400\">" + Text + "</font>";
        }

    }
}
