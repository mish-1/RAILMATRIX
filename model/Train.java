package model;

public class Train {

    private int trainId;
    private String trainName;
    private String source;
    private String destination;

    public Train(int trainId, String trainName, String source, String destination) {
        this.trainId = trainId;
        this.trainName = trainName;
        this.source = source;
        this.destination = destination;
    }

    public int getTrainId() {
        return trainId;
    }

    public String getTrainName() {
        return trainName;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return trainId + " - " + trainName + " (" + source + " to " + destination + ")";
    }
}