package edu.monash.ppoi.entity;

import java.util.List;

public class L {
	private int i;
	private int j;
	private int k;
	
	private double cost;
	private double max_load;
	private List<Double> baseload;
	public int getI() {
		return i;
	}
	public void setI(int i) {
		this.i = i;
	}
	public int getJ() {
		return j;
	}
	public void setJ(int j) {
		this.j = j;
	}
	public int getK() {
		return k;
	}
	public void setK(int k) {
		this.k = k;
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	public double getMax_load() {
		return max_load;
	}
	public void setMax_load(double max_load) {
		this.max_load = max_load;
	}
	public List<Double> getBaseload() {
		return baseload;
	}
	public void setBaseload(List<Double> baseload) {
		this.baseload = baseload;
	}
	
}
