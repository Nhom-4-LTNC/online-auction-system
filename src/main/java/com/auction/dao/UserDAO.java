package com.auction.dao;

import com.auction.model.user.User;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
        private static UserDAO instance;
        private List<User> users;
        private static final String FILE_PATH = "users.dat";

        private UserDAO() {
                users = new ArrayList<>();
                loadData();
        }

        public static synchronized UserDAO getInstance() {
                if (instance == null) {
                        instance = new UserDAO();
                }
                return instance;
        }

        public synchronized void saveData() {
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
                        out.writeObject(users);
                        System.out.println("Da luu du lieu nguoi dung vao file: "+FILE_PATH);
                } catch (IOException e) {
                        System.out.println("Loi khi luu file du lieu nguoi dung: "+e.getMessage());
                }
        }

        @SuppressWarnings("unchecked")
        private void loadData() {
                File file = new File(FILE_PATH);
                if (file.exists()) {
                        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
                                users = (List<User>) in.readObject();
                                System.out.println("Da doc "+users.size()+" tu he thong");
                        } catch (Exception e) {
                                System.out.println("Gap loi khi doc file "+e.getMessage());
                        }
                }
        }

        public synchronized void addUser(User user) {
                users.add(user);
                saveData();
        }

        public synchronized void updateUser(User updateUser) {
                for (int i=0; i< users.size(); i++) {
                        if (users.get(i).getId() == updateUser.getId()) {
                                users.set(i, updateUser);
                                saveData();
                                return;
                        }
                }
        }

        public synchronized User login(String email, String pwd) {
                for (User user: users) {
                        if (user.getEmail().equals(email) && user.getPwd().equals(pwd)) {
                                return user;
                        }
                }
                return null;
        }

        public synchronized User getUserByEmail(String email) {
                for (User user: users) {
                        if (user.getEmail().equals(email)) {
                                return user;
                        }
                }
                return null;
        }

        public synchronized User getUserById(int id) {
                for (User user: users) {
                        if (user.getId() == id) {
                                return user;
                        }
                }
                return null;
        }


        public synchronized List<User> getAllUser() {
                return users;
        }
}
