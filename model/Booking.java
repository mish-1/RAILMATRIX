package model;

public class Booking {

    private int bookingId;
    private User user;
    private Train train;

    public Booking(int bookingId, User user, Train train) {
        this.bookingId = bookingId;
        this.user = user;
        this.train = train;
    }

    @Override
    public String toString() {
        return "Booking ID: " + bookingId +
               "\nUser: " + user.getName() +
               "\nTrain: " + train.getTrainName();
    }
}