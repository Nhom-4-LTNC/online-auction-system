package com.auction.server.controller;

import java.util.List;

import com.auction.server.handler.ClientHandler;
import com.auction.server.model.user.User;
import com.auction.server.service.UserService;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.exception.AuctionAppException;
import com.auction.shared.protocol.ActionType;
import com.auction.shared.protocol.Request;
import com.auction.shared.protocol.Response;
import com.auction.shared.protocol.admin.ApplyBanRequest;
import com.auction.shared.protocol.admin.ApplyBanResponse;
import com.auction.shared.protocol.admin.GetAllUsersResponse;
import com.auction.shared.protocol.admin.RemoveBanRequest;
import com.auction.shared.protocol.admin.RemoveBanResponse;

public class AdminController {

    private final UserService userService = UserService.getInstance();

    public Response<?> handleGetAllUsers(Request<?> request, ClientHandler client) {
        try {
            if (client == null || !client.isLoggedIn()) {
                return Response.error(ActionType.GET_ALL_USERS, "Người dùng chưa đăng nhập.");
            }

            UserDTO requesterDTO = client.getCurrentUser();
            User requester = userService.getUserById(requesterDTO.getId());

            List<User> users = userService.getAllUsers(requester);
            List<com.auction.shared.dto.UserDTO> dtoList = users.stream()
                    .map(userService::mapUserToDTO)
                    .toList();

            return Response.success(ActionType.GET_ALL_USERS, new GetAllUsersResponse(dtoList));
        } catch (AuctionAppException e) {
            return Response.error(ActionType.GET_ALL_USERS, e.getMessage());
        } catch (Exception e) {
            return Response.error(ActionType.GET_ALL_USERS, e.getMessage());
        }
    }

    public Response<?> handleApplyBan(Request<?> request, ClientHandler client) {
        try {
            if (client == null || !client.isLoggedIn()) {
                return Response.error(ActionType.APPLY_BAN, "Người dùng chưa đăng nhập.");
            }

            if (!(request.getPayload() instanceof ApplyBanRequest payload)) {
                return Response.error(ActionType.APPLY_BAN, "Payload APPLY_BAN không hợp lệ.");
            }

            UserDTO requesterDTO = client.getCurrentUser();
            User requester = userService.getUserById(requesterDTO.getId());

            User target = userService.getUserById(payload.getTargetUserId());
            userService.applyBan(requester, target, payload.getDurationMillis());

            return Response.success(ActionType.APPLY_BAN, new ApplyBanResponse("Ban user thành công"));
        } catch (AuctionAppException e) {
            return Response.error(ActionType.APPLY_BAN, e.getMessage());
        } catch (Exception e) {
            return Response.error(ActionType.APPLY_BAN, e.getMessage());
        }
    }

    public Response<?> handleRemoveBan(Request<?> request, ClientHandler client) {
        try {
            if (client == null || !client.isLoggedIn()) {
                return Response.error(ActionType.REMOVE_BAN, "Người dùng chưa đăng nhập.");
            }

            if (!(request.getPayload() instanceof RemoveBanRequest payload)) {
                return Response.error(ActionType.REMOVE_BAN, "Payload REMOVE_BAN không hợp lệ.");
            }

            UserDTO requesterDTO = client.getCurrentUser();
            User requester = userService.getUserById(requesterDTO.getId());

            User target = userService.getUserById(payload.getTargetUserId());
            userService.removeBan(requester, target);

            return Response.success(ActionType.REMOVE_BAN, new RemoveBanResponse("Gỡ ban user thành công"));
        } catch (AuctionAppException e) {
            return Response.error(ActionType.REMOVE_BAN, e.getMessage());
        } catch (Exception e) {
            return Response.error(ActionType.REMOVE_BAN, e.getMessage());
        }
    }
}

