package com.auction.shared.protocol.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.auction.shared.dto.UserDTO;

public class GetAllUsersResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<UserDTO> users;

    public GetAllUsersResponse(List<UserDTO> users) {
        this.users = users;
    }

    public List<UserDTO> getUsers() {
        return users;
    }
}

