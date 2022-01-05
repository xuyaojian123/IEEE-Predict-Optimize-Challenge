package edu.monash.ppoi.checker;

import edu.monash.ppoi.constant.Parameters;
import edu.monash.ppoi.instance.Building;
import edu.monash.ppoi.instance.Instance;
import edu.monash.ppoi.instance.OnceOff;
import edu.monash.ppoi.instance.Recurring;
import edu.monash.ppoi.solution.BatterySchedule;
import edu.monash.ppoi.solution.Schedule;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChronicsScheduleChecker implements IScheduleChecker {

	private static final Double peakTariff = 0.005;

	private final RoomUsage roomUsage;
	private final List<Double> baseload;
	private final List<Double> prices;
	private final List<BatteryCheck> batteryChecks;
	private final ActivityScheduleCheck<OnceOff> onceoffCheck;
	private final ActivityScheduleCheck<Recurring> recurringCheck;

	private final boolean isValid;
	private double[] sumload;

	public ChronicsScheduleChecker(ChronicsHandler db, Schedule schedule) {

		// Get the instance to discover which loads are 'active'.
		Instance instance = schedule.getInstance();

		// Get base load.
		this.baseload = getBaseLoad(db, instance);
		updateBaseLoad(baseload);
		
		this.prices = db.getPrices();

		// Create room usage tracker.
		roomUsage = new RoomUsage(instance, db.getHorizon());

		// Check the battery schedules.
		batteryChecks = new ArrayList<>();
		for (BatterySchedule battSchedule : schedule.getBatterySchedule()) {
			batteryChecks.add(new BatteryCheck(battSchedule, db.getHorizon()));
		}
		
		// Check the precedences.
		onceoffCheck = new ActivityScheduleCheck<OnceOff>(db, roomUsage, instance.getAllOnceOff(), schedule.getOnceOffSchedule());
		recurringCheck = new ActivityScheduleCheck<Recurring>(db, roomUsage, instance.getAllRecurring(), schedule.getRecurringSchedule());

		// Check full schedule validity.
		isValid = checkAllValid();
		sumload = accumulateLoads();
	}


	public double getObjective() {
		return isValid() ? getScore() : Double.NaN;
	}

	public double getScore() {
		double energyCost = getEnergyCost();
		double peakCost = getPeakCost();
		double onceOffProfit = getOnceOffProfit();

		//System.out.println(energyCost+ " " + peakCost + " " + onceOffProfit);
		return energyCost + peakCost - onceOffProfit;
	}

	public double getEnergyCost() {

		double sumCost = 0d;
		
		for (int t = 0; t < baseload.size(); t++) {
			
			double load = getTotalLoad(t);
			double cost = 0.001d * load * 900d / 3600d * prices.get(t);
			
			sumCost += cost;
		}
		
		return sumCost;
	}

	public double getPeakCost() {

		double maxLoad = 0d;
		for (int t = 0; t < sumload.length; t++)
			maxLoad = Math.max(maxLoad, getTotalLoad(t));

		return peakTariff * maxLoad * maxLoad;
	}

	public double getOnceOffProfit() {
		return onceoffCheck.getValue();
	}

	public boolean isValid() {
		return isValid;
	}

	public double getBaseLoad(int t) {
		return baseload.get(t);
	}

	public double getScheduleLoad(int t) {
		return getTotalLoad(t) - getBaseLoad(t);
	}

	public double getTotalLoad(int t) {
		return sumload[t];
	}

	public RoomUsage getRoomUsage() {
		return roomUsage;
	}

	private boolean checkAllValid() {

		boolean isValid = true;
	
		isValid = isValid && roomUsage.isValid();
		isValid = isValid && onceoffCheck.isValid();
		isValid = isValid && recurringCheck.isValid();
		
		for (BatteryCheck check : batteryChecks) {
			isValid = isValid && check.isValid();
		}

		return isValid;
	}

	private double[] accumulateLoads() {

		double[] sumload = new double[baseload.size()];

		// Base load.
		for (int i = 0; i < baseload.size(); i++) {
			sumload[i] += baseload.get(i);
		}

		// Battery load.
		for (BatteryCheck check : batteryChecks) {
			accumulate(sumload, check.getPowerKW());
		}

		// Activity load.
		accumulate(sumload, onceoffCheck.getPowerKW());
		accumulate(sumload, recurringCheck.getPowerKW());
		
		return sumload;
	}

	private static void accumulate(double[] accumulator, double[] values) {
		for (int i = 0; i < accumulator.length; i++) {
			accumulator[i] += values[i];
		}
	}

	public static List<Double> getBaseLoad(ChronicsHandler db, Instance instance) {

		// Collect all active building series.
		Map<Building, List<Double>> buildingLoad = new LinkedHashMap<>();
		for (Building building : instance.getAllBuildings())
			buildingLoad.put(building, building.getBaseLoad(db));

		return combine(buildingLoad);
	}

	public static List<Double> combine(Map<Building, List<Double>> buildingLoad) {

		List<Double> baseload = new ArrayList<>();
		
		int step = 0;
		while (true) {
			
			double sumLoad = 0;
			
			for (List<Double> entry : buildingLoad.values()) {
				if (step >= entry.size())
					return baseload;
				
				sumLoad += entry.get(step);
			}

			baseload.add(sumLoad);
			step = step + 1;
		}
	}
	

	public static void updateBaseLoad(List<Double> baseLoad){

		List<Double> buildingTotal = new ArrayList<>();
		List<Double> solarTotal = new ArrayList<>();
		String line1 = "", line2 = "";
		BufferedReader reader1 = null,reader2 = null;
		try {
			reader1 = new BufferedReader(new FileReader("data/buildingTotalFinal.csv"));
			reader2 = new BufferedReader(new FileReader("data/solarTotalFinal.csv"));
			while ( (line1 = reader1.readLine())!=null ){
				buildingTotal.add( Double.parseDouble(line1));
			}
			while ( (line2 = reader2.readLine())!= null){
				solarTotal.add( Double.parseDouble(line2));
			}
			//int ans = 0;
			for (int i = 0; i < Parameters.timeSlotsLength; i++) {
				baseLoad.set(i, buildingTotal.get(i) - solarTotal.get(i) );
				//ans++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader2.close();
				reader1.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	

	/**
	 * baseLoad_evaluation鏄粠Evalutation1涓殑baseload璧嬪�艰繃鏉ョ殑,鍥犱负Evalutation1涓殑baseload鏄竴鐩翠笉鍙樼殑锛屽搴旂殑灏辨槸baseload鏂囦欢澶逛笅鏌愪釜baseload
	 * 浣跨敤杩欑鏂规硶淇敼浠栫殑榛樿baseload
	 * @see #updateBaseLoad(List) 榛樿涓篵aseload1涓嬮潰鐨刡aseload
	 * @param baseLoad_evaluation
	 */
	public void setBaseLoad(List<Double> baseLoad_evaluation){
		if (this.baseload.size()!=baseLoad_evaluation.size()){
			throw new RuntimeException("璧嬪�肩殑baseload鍑洪敊浜嗭紒锛侊紒锛侊紒");
		}
		//this.baseload涓篺inal鍨嬶紝涓嶈兘鏀瑰彉浠栫殑鎸囧悜锛屼絾鍙互淇敼浠栫殑鍐呴儴鍏冪礌銆�
		for (int i = 0; i < baseLoad_evaluation.size(); i++) {
			this.baseload.set(i,baseLoad_evaluation.get(i));
		}
		//sumload去掉了final类型，重新计算sumload
		sumload = accumulateLoads();
	}

	public double[] getSumload() {
		return sumload;
	}
}
