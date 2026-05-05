package com.auction.test;

import com.auction.dao.UserDAO;
import com.auction.model.user.User;
import com.auction.model.user.Admin;

public class MainTest {
    public static void main(String[] args) {
        System.out.println("===KIỂM TRA LƯU TRỮ DỮ LIỆU===");
        UserDAO dao = UserDAO.getInstance();
        System.out.println("[n1] Dang tao du lieu gia lap");
        User user1 = new User(001, "abc", "ABC", "nhat24271@gmail.com");
        Admin admin1 = new Admin(001, "NHATDEPZAI", "ABC", "nhatnguyen24271@gmail.com");
        dao.addUser(user1);
        dao.addUser(admin1);

        System.out.println("===DANH SACH NGUOI DUNG HIEN TAI===");
        if (dao.getAllUser().isEmpty()) {
            System.out.println("He thong chua co nguoi dung nao");
        } else {
            for (User user: dao.getAllUser()) {
                System.out.println("-Ten: "+user.getUsername()+
                                    "-Email: "+user.getEmail()+
                                    "-ID: "+user.getId()+
                                    "-PWD: "+user.getPwd());
            }
        }
    }
}
