package com.auction.server.controller;

import com.auction.dto.UserDTO;
import com.auction.exception.AuctionAppException;
import com.auction.protocol.ActionType;
import com.auction.protocol.Request;
import com.auction.protocol.Response;
import com.auction.protocol.auth.AuthResponse;
import com.auction.protocol.auth.LoginRequest;
import com.auction.protocol.auth.RegisterRequest;
import com.auction.server.ClientHandler;
import com.auction.service.AuthService;

/**
 * Controller chịu trách nhiệm xử lý các luồng liên quan đến Xác thực người dùng
 * (Đăng nhập, Đăng ký). Giao tiếp trực tiếp với AuthService.
 */
public class AuthController {

    private final AuthService authService = AuthService.getInstance();

    /**
     * Xử lý yêu cầu đăng nhập từ Client.
     *
     * @param request Gói tin Request chứa LoginRequest payload (email, password).
     * @param client  Tham chiếu đến luồng ClientHandler để lưu trữ Session nếu đăng nhập thành công.
     * @return Response chứa UserDTO nếu thành công, hoặc chứa chuỗi báo lỗi nếu thất bại.
     */
    public Response<?> handleLogin(Request<?> request, ClientHandler client) {
        try {
            LoginRequest loginData = (LoginRequest) request.getPayload();

            // Gọi Service xử lý. Nếu lỗi (sai pass, bị ban...), nó sẽ ném ra AuctionAppException
            UserDTO userDTO = authService.login(loginData.getEmail(), loginData.getPassword());

            // Lưu thông tin người dùng vào Session của luồng Socket này
            client.setCurrentUser(userDTO);

            return Response.success(ActionType.LOGIN, new AuthResponse(userDTO, "Đăng nhập thành công"));

        } catch (AuctionAppException e) {
            // Bắt các lỗi nghiệp vụ: Sai mật khẩu, User bị khóa (Banned)...
            return Response.error(ActionType.LOGIN, e.getMessage());
        } catch (Exception e) {
            // Lỗi hệ thống bất ngờ (Mất kết nối DB...)
            e.printStackTrace();
            return Response.error(ActionType.LOGIN, "Lỗi máy chủ khi xử lý đăng nhập!");
        }
    }

    /**
     * Xử lý yêu cầu đăng ký tài khoản mới từ Client.
     *
     * @param request Gói tin Request chứa RegisterRequest payload.
     * @param client  Tham chiếu đến luồng ClientHandler.
     * @return Response chứa UserDTO (người dùng mới) nếu thành công, hoặc chuỗi lỗi.
     */
    public Response<?> handleRegister(Request<?> request, ClientHandler client) {
        try {
            RegisterRequest regData = (RegisterRequest) request.getPayload();

            UserDTO newUserDTO = authService.register(
                    regData.getUsername(),
                    regData.getEmail(),
                    regData.getPassword()
            );

            // Có thể tự động đăng nhập luôn sau khi đăng ký thành công (Tuỳ logic dự án)
            client.setCurrentUser(newUserDTO);

            return Response.success(ActionType.REGISTER, new AuthResponse(newUserDTO, "Đăng ký thành công"));

        } catch (AuctionAppException e) {
            // Bắt lỗi: Trùng Email, Trùng Username, Thiếu trường dữ liệu...
            return Response.error(ActionType.REGISTER, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error(ActionType.REGISTER, "Lỗi máy chủ khi xử lý đăng ký!");
        }
    }
}