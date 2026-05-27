# TODO - Admin riêng & quản lý qua shared

## Steps
- [ ] Step 1: Thêm protocol admin (ActionType + DTO/Request/Response)
- [x] Step 1.1: Thêm ActionType admin và shared protocol files

- [ ] Step 1.2: Update TODO sau khi xong
- [x] Step 2: Tạo Admin UI (AdminScreen.fxml + AdminMenuController.java)


- [x] Step 2: Routing theo role trong LoginController (ADMIN -> AdminScreen, USER -> HomeScreen)

- [x] Step 3: Tạo protocol ActionType/Request/Response cho admin: 

  - [ ] Lấy danh sách users (GET_ALL_USERS)
  - [ ] Ban user (APPLY_BAN)
  - [ ] Gỡ ban (REMOVE_BAN)
- [x] Step 4: Tạo server controller cho admin (AdminController)

- [x] Step 5: Mở rộng ClientHandler.dispatch để route các ActionType mới tới AdminController

- [x] Step 6: Tạo client-side requests (AdminClientService hoặc gọi Client.sendMessage trực tiếp) để implement nút admin

- [x] Step 7: Update UI: gọi danh sách users, hiển thị, ban/unban

- [x] Step 8: Kiểm tra

  - [ ] Login USER không vào AdminScreen
  - [ ] Login ADMIN vào AdminScreen và bấm nút hoạt động đúng phân quyền


