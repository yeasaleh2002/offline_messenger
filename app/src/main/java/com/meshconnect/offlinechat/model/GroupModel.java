package com.meshconnect.offlinechat.model;

import java.io.Serializable;

/**
 * Model representing an offline P2P chat group.
 */
public class GroupModel implements Serializable {

    private final String id;
    private final String name;
    private final String createdBy;
    private final long createdAt;
    private int memberCount;

    public GroupModel(String id, String name, String createdBy, long createdAt) {
        this(id, name, createdBy, createdAt, 1);
    }

    public GroupModel(String id, String name, String createdBy, long createdAt, int memberCount) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.memberCount = memberCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int count) {
        this.memberCount = count;
    }
}
