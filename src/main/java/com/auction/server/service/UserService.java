package com.auction.server.service;

import java.util.List;

import com.auction.server.model.user.User;
import com.auction.server.repository.UserRepository;
import com.auction.shared.dto.UserDTO;
import com.auction.shared.enums.Role;
import com.auction.shared.exception.AuthenticationException;
import com.auction.shared.exception.AuthorizationException;
import com.auction.shared.exception.DuplicateResourceException;
import com.auction.shared.exception.ResourceNotFoundException;
import com.auction.shared.exception.ValidationException;

/**
 * Service chịu trách nhiệm xử lý các nghiệp vụ liên quan đến người dùng.
 *
 * <p>Lớp này nằm ở tầng Service, làm nhiệm vụ trung gian giữa controller
 * và {@link UserRepository}. Các nghiệp vụ chính bao gồm:</p>
 *
 * <ul>
 *     <li>Đăng nhập người dùng</li>
 *     <li>Đăng ký tài khoản mới</li>
 *     <li>Truy vấn người dùng theo ID hoặc email</li>
 *     <li>Cập nhật thông tin người dùng</li>
 *     <li>Kiểm tra và xử lý trạng thái bị khóa tài khoản</li>
 *     <li>Chuyển đổi {@link User} sang {@link UserDTO}</li>
 * </ul>
 *
 * <p>Lớp này sử dụng Singleton Pattern để đảm bảo chỉ có một instance
 * của {@code UserService} trong toàn bộ chương trình.</p>
 *
 * <p>Lưu ý: Service này trả về entity {@link User} cho các nghiệp vụ nội bộ.
 * Khi cần gửi dữ liệu về client, controller có thể gọi {@link #mapUserToDTO(User)}
 * để chuyển sang DTO.</p>
 */
public class UserService {

    /**
     * Instance duy nhất của {@code UserService}.
     *
     * <p>Dùng {@code volatile} để đảm bảo an toàn khi sử dụng
     * double-checked locking trong môi trường đa luồng.</p>
     */
    private static volatile UserService instance;

    /**
     * Repository dùng để truy cập dữ liệu người dùng trong database.
     */
    private final UserRepository userRepository = UserRepository.getInstance();

    /**
     * Constructor private để ngăn khởi tạo trực tiếp từ bên ngoài.
     */
    private UserService() {
    }

    /**
     * Lấy instance duy nhất của {@code UserService}.
     *
     * @return instance singleton của {@code UserService}
     */
    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) {
                if (instance == null) {
                    instance = new UserService();
                }
            }
        }

        return instance;
    }
    /**
     * Tìm người dùng theo ID.
     *
     * @param id ID của người dùng cần tìm
     * @return entity {@link User} tương ứng
     * @throws ValidationException       nếu ID không hợp lệ
     * @throws ResourceNotFoundException nếu không tìm thấy người dùng
     * @throws Exception                 nếu có lỗi phát sinh từ tầng repository
     */
    public User getUserById(int id) throws Exception {
        if (id <= 0) {
            throw new ValidationException("ID người dùng không hợp lệ!");
        }

        User user = userRepository.getUserById(id);

        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        return user;
    }

    /**
     * Tìm người dùng theo email.
     *
     * <p>Email sẽ được chuẩn hóa bằng cách trim và chuyển về chữ thường
     * trước khi truy vấn.</p>
     *
     * @param email email của người dùng cần tìm
     * @return entity {@link User} tương ứng
     * @throws ValidationException       nếu email bị bỏ trống
     * @throws ResourceNotFoundException nếu không tìm thấy người dùng
     * @throws Exception                 nếu có lỗi phát sinh từ tầng repository
     */
    public User getUserByEmail(String email) throws Exception {
        String cleanEmail = normalizeEmail(email);

        User user = userRepository.getUserByEmail(cleanEmail);

        if (user == null) {
            throw new ResourceNotFoundException("User", 0);
        }

        return user;
    }

    /**
     * Lấy danh sách tất cả người dùng trong hệ thống.
     *
     * <p>Chỉ người dùng có quyền {@link Role#ADMIN} mới được phép gọi
     * nghiệp vụ này.</p>
     *
     * @param requester người dùng đang thực hiện yêu cầu
     * @return danh sách tất cả {@link User}
     * @throws AuthenticationException nếu requester chưa đăng nhập
     * @throws AuthorizationException  nếu requester không có quyền admin
     * @throws Exception               nếu có lỗi phát sinh từ tầng repository
     */
    public List<User> getAllUsers(User requester) throws Exception {
        requireAdmin(requester);
        return userRepository.getAllUsers();
    }

    /**
     * Cập nhật thông tin người dùng.
     *
     * @param user entity người dùng cần cập nhật
     * @throws ValidationException nếu user bị null
     * @throws Exception           nếu có lỗi phát sinh từ tầng repository
     */
    public void updateUser(User user) throws Exception {
        if (user == null) {
            throw new ValidationException("Người dùng không hợp lệ!");
        }

        userRepository.updateUser(user);
    }

    /**
     * Kiểm tra người dùng có đang bị khóa tài khoản hay không.
     *
     * @param user người dùng cần kiểm tra
     * @return {@code true} nếu người dùng đang bị khóa;
     *         {@code false} nếu không bị khóa hoặc user null
     */
    public boolean isBanned(User user) {
        return user != null && user.getBanEndTime() > System.currentTimeMillis();
    }

    /**
     * Khóa tài khoản một người dùng trong một khoảng thời gian nhất định.
     *
     * <p>Chỉ admin mới có quyền thực hiện thao tác này.</p>
     *
     * @param requester      admin đang thực hiện thao tác
     * @param target         người dùng bị khóa
     * @param durationMillis thời gian khóa, tính bằng mili-giây
     * @throws AuthenticationException nếu requester chưa đăng nhập
     * @throws AuthorizationException  nếu requester không phải admin
     * @throws ValidationException     nếu target không hợp lệ, thời gian khóa không hợp lệ,
     *                                 hoặc admin tự khóa chính mình
     * @throws Exception               nếu có lỗi phát sinh từ tầng repository
     */
    public void applyBan(User requester, User target, long durationMillis) throws Exception {
        requireAdmin(requester);

        if (target == null) {
            throw new ValidationException("Người dùng bị ban không hợp lệ!");
        }

        if (durationMillis <= 0) {
            throw new ValidationException("Thời gian ban phải lớn hơn 0!");
        }

        if (requester.getId() == target.getId()) {
            throw new ValidationException("Admin không thể tự ban chính mình!");
        }

        long now = System.currentTimeMillis();

        target.setBanStartTime(now);
        target.setBanEndTime(now + durationMillis);

        userRepository.updateUser(target);
    }

    /**
     * Gỡ trạng thái khóa tài khoản của một người dùng.
     *
     * <p>Chỉ admin mới có quyền thực hiện thao tác này.</p>
     *
     * @param requester admin đang thực hiện thao tác
     * @param target    người dùng cần được gỡ khóa
     * @throws AuthenticationException nếu requester chưa đăng nhập
     * @throws AuthorizationException  nếu requester không phải admin
     * @throws ValidationException     nếu target không hợp lệ
     * @throws Exception               nếu có lỗi phát sinh từ tầng repository
     */
    public void removeBan(User requester, User target) throws Exception {
        requireAdmin(requester);

        if (target == null) {
            throw new ValidationException("Người dùng cần gỡ ban không hợp lệ!");
        }

        target.setBanStartTime(0);
        target.setBanEndTime(0);

        userRepository.updateUser(target);
    }
    /**
     * Chuyển đổi entity {@link User} sang DTO {@link UserDTO}.
     *
     * <p>DTO được dùng để gửi dữ liệu người dùng về client.
     * Không nên đưa mật khẩu hoặc dữ liệu nhạy cảm vào DTO.</p>
     *
     * @param user entity người dùng cần chuyển đổi
     * @return {@link UserDTO} tương ứng, hoặc {@code null} nếu user null
     */
    public UserDTO mapUserToDTO(User user) {
        if (user == null) {
            return null;
        }

        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getBanStartTime(),
                user.getBanEndTime()
        );
    }

    /**
     * Kiểm tra email hoặc username đã tồn tại trong hệ thống hay chưa.
     *
     * @param username username đã được chuẩn hóa
     * @param email    email đã được chuẩn hóa
     * @throws DuplicateResourceException nếu email hoặc username đã tồn tại
     * @throws Exception                  nếu có lỗi phát sinh từ tầng repository
     */
    private void checkUserExistence(String username, String email) throws Exception {
        if (userRepository.getUserByEmail(email) != null) {
            throw new DuplicateResourceException("Email này đã được đăng ký!");
        }

        if (userRepository.getUserByUsername(username) != null) {
            throw new DuplicateResourceException("Tên đăng nhập đã tồn tại!");
        }
    }

    /**
     * Kiểm tra người dùng hiện tại có quyền admin hay không.
     *
     * @param requester người dùng đang thực hiện thao tác
     * @throws AuthenticationException nếu requester chưa đăng nhập
     * @throws AuthorizationException  nếu requester không có quyền admin
     */
    private void requireAdmin(User requester) throws Exception {
        if (requester == null) {
            throw new AuthenticationException("Bạn cần đăng nhập!");
        }

        if (!requester.isAdmin()) {
            throw new AuthorizationException("Chỉ Admin mới có quyền thực hiện thao tác này!");
        }
    }

    /**
     * Chuẩn hóa email đầu vào.
     *
     * <p>Email sau khi chuẩn hóa sẽ được trim và chuyển về chữ thường.</p>
     *
     * @param email email đầu vào
     * @return email đã được chuẩn hóa
     * @throws ValidationException nếu email null hoặc rỗng
     */
    private String normalizeEmail(String email) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email không được để trống!");
        }

        return email.trim().toLowerCase();
    }

    /**
     * Chuẩn hóa một chuỗi bắt buộc không được rỗng.
     *
     * @param value     giá trị đầu vào
     * @param fieldName tên trường dùng trong thông báo lỗi
     * @return chuỗi đã được trim
     * @throws ValidationException nếu giá trị null hoặc rỗng
     */
    private String normalizeRequiredText(String value, String fieldName) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " không được để trống!");
        }

        return value.trim();
    }

    /**
     * Kiểm tra mật khẩu đầu vào.
     *
     * <p>Hiện tại chỉ kiểm tra mật khẩu không được để trống.
     * Nếu cần, có thể mở rộng thêm các điều kiện như độ dài tối thiểu,
     * chữ hoa, chữ thường, số, ký tự đặc biệt.</p>
     *
     * @param password mật khẩu cần kiểm tra
     * @throws ValidationException nếu mật khẩu null hoặc rỗng
     */
    private void validatePassword(String password) throws Exception {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Mật khẩu không được để trống!");
        }
    }
}