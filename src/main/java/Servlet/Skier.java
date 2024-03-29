package Servlet;

public class Skier {
    private int season;

    private int year;

    private int day;

    private int skierId;

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getSkierId() {
        return skierId;
    }

    public void setSkierId(int skierId) {
        this.skierId = skierId;
    }

    @Override
    public String toString() {
        return "Skier{" +
                "season=" + season +
                ", year=" + year +
                ", day=" + day +
                ", skierId=" + skierId +
                '}';
    }
}
