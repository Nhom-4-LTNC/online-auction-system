package com.auction.client.controller;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

// REVIEW: Tránh import wildcard như com.auction.client.* và com.auction.shared.util.*.
// Nên import rõ class cần dùng để dễ kiểm soát dependency và tránh import nhầm server-side class.
import com.auction.shared.dto.AuctionSummaryDTO;
import com.auction.shared.enums.AuctionStatus;
import com.auction.shared.enums.ItemType;

import com.auction.shared.util.SceneUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class AuctionMenuController {

    @FXML private RadioButton electronicsButton;
    @FXML private RadioButton artButton;
    @FXML private RadioButton vehicleButton;

    @FXML private ListView<AuctionSummaryDTO> auctionListView;

    @FXML private Button refreshButton;
    @FXML private Button backButton;

    private ItemType currentType = ItemType.ELECTRONICS;

    /*
     * REVIEW - VẤN ĐỀ KIẾN TRÚC NGHIÊM TRỌNG:
     *
     * Class này thuộc package client.controller, nên chỉ được làm việc với:
     * - JavaFX UI
     * - DTO
     * - protocol Request/Response
     * - client-side service/network layer
     *
     * Tuyệt đối không được gọi trực tiếp service phía server như:
     * - com.auction.server.service.AuctionService
     * - com.auction.server.repository.*
     * - com.auction.server.model.*
     *
     * Nếu controller client gọi thẳng AuctionService của server, flow sẽ thành:
     *
     * JavaFX Controller -> server.service.AuctionService -> Repository -> Database
     *
     * Như vậy là phá kiến trúc Client-Server, vì client đã bỏ qua:
     * - Socket
     * - Request/Response protocol
     * - ClientHandler
     * - session currentUser trên server
     * - authentication/authorization trên server
     *
     * Flow đúng phải là:
     *
     * JavaFX Controller
     *   -> AuctionClientService hoặc NetworkClient phía client
     *   -> gửi Request qua socket
     *   -> Server/ClientHandler
     *   -> AuctionController phía server
     *   -> AuctionService phía server
     *   -> Repository
     *   -> Database
     *
     * Hướng sửa:
     * - Tạo com.auction.client.service.AuctionClientService.
     * - AuctionClientService gửi Request(ActionType.GET_AUCTIONS_BY_TYPE, payload) qua socket.
     * - Server xử lý request và trả Response chứa List<AuctionSummaryDTO>.
     * - AuctionMenuController chỉ gọi AuctionClientService, không gọi server.service.
     */

    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        electronicsButton.setToggleGroup(group);
        artButton.setToggleGroup(group);
        vehicleButton.setToggleGroup(group);
        electronicsButton.setSelected(true);

        electronicsButton.setUserData(ItemType.ELECTRONICS);
        artButton.setUserData(ItemType.ART);
        vehicleButton.setUserData(ItemType.VEHICLE);

        group.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;

            /*
             * REVIEW:
             * Đoạn này hơi vòng:
             *
             * ((RadioButton) newV.getToggleGroup().getSelectedToggle()).getUserData()
             *
             * Vì newV chính là selected toggle mới rồi, có thể viết ngắn hơn:
             *
             * Object ud = newV.getUserData();
             *
             * Tuy nhiên đây chỉ là vấn đề nhỏ về readability, không phải bug nghiêm trọng.
             */
            Object ud = ((RadioButton) newV.getToggleGroup().getSelectedToggle()).getUserData();

            if (ud instanceof ItemType t) {
                currentType = t;
                load();
            }
        });

        auctionListView.setCellFactory(lv -> {
            ListCell<AuctionSummaryDTO> cell = new ListCell<>() {
                @Override
                protected void updateItem(AuctionSummaryDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        AuctionStatus st = item.getStatus();

                        /*
                         * REVIEW:
                         * Hiển thị currentPrice dạng double trực tiếp sẽ xấu trên UI.
                         * Nên format tiền bằng NumberFormat hoặc utility riêng.
                         *
                         * Ví dụ:
                         * NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(item.getCurrentPrice())
                         *
                         * Nhưng nếu đang ưu tiên demo, có thể sửa sau.
                         */
                        setText("#" + item.getAuctionId() + " | " + item.getItemName() + " | "
                                + item.getCurrentPrice() + " | " + (st == null ? "" : st));
                    }
                }
            };

            // Click trực tiếp lên cell (không phụ thuộc selection change)
            cell.setOnMouseClicked(e -> {
                if (cell.isEmpty()) return;

                AuctionSummaryDTO selected = cell.getItem();
                if (selected == null) return;

                try {
                    var url = getClass().getResource("/fxml/ItemAuction.fxml");
                    if (url == null) {
                        System.err.println("[AuctionMenuController] Resource not found: /fxml/ItemAuction.fxml");
                        return;
                    }

                    FXMLLoader loader = new FXMLLoader(url);
                    Parent root = loader.load();

                    /*
                     * REVIEW:
                     * Cách truyền auctionId qua controller là chấp nhận được.
                     * Nhưng cần đảm bảo ItemAuctionController sau khi nhận auctionId phải gọi server qua client-side service,
                     * không được gọi trực tiếp server.service.AuctionService.
                     */
                    ItemAuctionController controller = loader.getController();
                    if (controller != null) {
                        controller.setAuctionId(selected.getAuctionId());
                    }

                    Stage stage = (Stage) auctionListView.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.show();

                } catch (IOException ex) {
                    /*
                     * REVIEW:
                     * Hiện tại chỉ printStackTrace. Với UI nên hiển thị Alert cho người dùng.
                     * Ví dụ: AlertUtils.showError("Không thể mở màn hình chi tiết đấu giá.");
                     */
                    ex.printStackTrace();

                } catch (RuntimeException ex) {
                    /*
                     * REVIEW:
                     * Bắt RuntimeException rộng như thế này có thể che giấu bug lập trình.
                     * Nếu cần bắt để tránh crash UI thì nên log rõ và hiển thị thông báo.
                     */
                    ex.printStackTrace();
                }
            });

            return cell;
        });

        /*
         * REVIEW:
         * load() được gọi trực tiếp trong initialize().
         * Nếu load() gửi request qua socket và chờ server phản hồi, UI có thể bị đứng.
         *
         * Với demo nhỏ có thể tạm chấp nhận.
         * Nhưng đúng hơn nên load dữ liệu bằng JavaFX Task/background thread,
         * sau đó Platform.runLater(...) để update ListView.
         */
        load();

        refreshButton.setOnAction(this::handleRefresh);
        backButton.setOnAction(this::handleBack);
    }

    public void handleRefresh(ActionEvent event) {
        load();
    }

    public void handleBack(ActionEvent event) {
        try {
            SceneUtils.switchScene(event, "/fxml/HomeScreen.fxml");

        } catch(IOException e) {
            /*
             * REVIEW:
             * Không nên nuốt exception im lặng.
             * Nếu chuyển scene lỗi mà không log, rất khó debug.
             *
             * Nên ít nhất:
             * e.printStackTrace();
             *
             * Tốt hơn:
             * AlertUtils.showError("Không thể quay về màn hình chính.");
             */
        }
    }

    private void load() {
        /*
         * REVIEW - VẤN ĐỀ KIẾN TRÚC NGHIÊM TRỌNG:
         *
         * auctionService ở đây không được là service phía server.
         * Nếu nó là com.auction.server.service.AuctionService thì sai kiến trúc Client-Server.
         *
         * Cách đúng:
         *
         * private final AuctionClientService auctionService = AuctionClientService.getInstance();
         *
         * Trong đó AuctionClientService thuộc package client, ví dụ:
         *
         * com.auction.client.service.AuctionClientService
         *
         * Và method getAuctionListByType(...) phải gửi Request qua socket:
         *
         * Request<GetAuctionsByTypeRequest> request =
         *     new Request<>(ActionType.GET_AUCTIONS_BY_TYPE, new GetAuctionsByTypeRequest(currentType));
         *
         * Response<GetAuctionsByTypeResponse> response = client.sendRequest(request);
         *
         * return response.getPayload().getAuctions();
         *
         * Controller JavaFX không được truy cập trực tiếp database/server service.
         */
        List<AuctionSummaryDTO> list = auctionService.getAuctionListByType(currentType);

        /*
         * REVIEW:
         * Tên method getEndTimeMilis có vẻ sai chính tả.
         * Đúng nên là getEndTimeMillis.
         *
         * Nếu DTO hiện tại đang là getEndTimeMilis thì nên rename lại cho chuẩn,
         * nhưng cần refactor đồng bộ các chỗ đang gọi.
         */
        list.sort(Comparator.comparingLong(AuctionSummaryDTO::getEndTimeMillis).reversed());

        /*
         * REVIEW:
         * Sort reversed nghĩa là auction kết thúc muộn hơn sẽ hiện trước.
         * Với UX đấu giá, thường nên hiển thị auction sắp kết thúc trước:
         *
         * Comparator.comparingLong(AuctionSummaryDTO::getEndTimeMillis)
         *
         * Tuy nhiên đây là quyết định sản phẩm, không phải lỗi kiến trúc.
         */
        ObservableList<AuctionSummaryDTO> data = FXCollections.observableArrayList(list);
        auctionListView.setItems(data);
    }
}