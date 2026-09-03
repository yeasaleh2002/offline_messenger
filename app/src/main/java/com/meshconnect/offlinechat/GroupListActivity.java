package com.meshconnect.offlinechat;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.meshconnect.offlinechat.adapter.GroupAdapter;
import com.meshconnect.offlinechat.db.ChatDatabaseHelper;
import com.meshconnect.offlinechat.model.GroupModel;

import java.util.List;
import java.util.UUID;

/**
 * Activity for viewing, creating, and joining offline P2P chat groups.
 */
public class GroupListActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView recyclerViewGroups;
    private LinearLayout layoutEmptyState;
    private ExtendedFloatingActionButton fabCreateGroup;

    private GroupAdapter groupAdapter;
    private ChatDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);

        dbHelper = ChatDatabaseHelper.getInstance(this);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadGroups();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroups();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerViewGroups = findViewById(R.id.recyclerViewGroups);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        fabCreateGroup = findViewById(R.id.fabCreateGroup);
    }

    private void setupRecyclerView() {
        groupAdapter = new GroupAdapter(this::openGroupChat);
        recyclerViewGroups.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGroups.setAdapter(groupAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        fabCreateGroup.setOnClickListener(v -> showCreateGroupDialog());
    }

    private void loadGroups() {
        List<GroupModel> groups = dbHelper.getAllGroups();
        if (groups.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewGroups.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewGroups.setVisibility(View.VISIBLE);
            groupAdapter.setGroups(groups);
        }
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Offline Group");

        final EditText input = new EditText(this);
        input.setHint("e.g. Emergency Team, Family Group");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(64, 32, 64, 16);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String groupName = input.getText().toString().trim();
            if (!groupName.isEmpty()) {
                String groupId = "GRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                dbHelper.createGroup(groupId, groupName, android.os.Build.MODEL);
                Toast.makeText(this, "Group \"" + groupName + "\" created!", Toast.LENGTH_SHORT).show();
                loadGroups();

                GroupModel createdGroup = new GroupModel(groupId, groupName, android.os.Build.MODEL, System.currentTimeMillis());
                openGroupChat(createdGroup);
            } else {
                Toast.makeText(this, "Please enter a valid group name.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openGroupChat(GroupModel group) {
        Intent intent = new Intent(this, GroupChatActivity.class);
        intent.putExtra("EXTRA_GROUP_ID", group.getId());
        intent.putExtra("EXTRA_GROUP_NAME", group.getName());
        startActivity(intent);
    }
}
