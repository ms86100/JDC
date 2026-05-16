package com.jira.auth.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupMembershipId implements Serializable {
    private UUID user;
    private UUID group;
}