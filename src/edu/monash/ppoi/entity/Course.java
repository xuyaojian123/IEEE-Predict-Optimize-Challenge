package edu.monash.ppoi.entity;

public class Course {
    //课程id
    private int id;

    //下限
    private int low;

    //上限
    private int top;

    public Course() {
    }

    public Course(Course c) {
    	this.id = c.id;
    	this.low = c.low;
    	this.top = c.top;
    }
    public Course(int id, int low, int top) {
        this.id = id;
        this.low = low;
        this.top = top;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLow() {
        return low;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }
}
