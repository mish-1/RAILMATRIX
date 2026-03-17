package model;

public class User extends Person {

    private int userId;

    public User(int userId, String name) {
        super(name);
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }
}