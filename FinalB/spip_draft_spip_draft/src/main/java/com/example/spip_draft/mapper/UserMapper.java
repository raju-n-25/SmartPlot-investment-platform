package com.example.spip_draft.mapper;

import com.example.spip_draft.entity.User;

public class UserMapper {

    public static User toResponse(User user) {
        return user;
    }

    public static User toEntity(User user) {
        return user;
    }
}