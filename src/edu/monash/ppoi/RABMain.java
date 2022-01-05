package edu.monash.ppoi;

import edu.monash.io.FileUtils;
import edu.monash.ppoi.checker.ChronicsHandler;
import edu.monash.ppoi.checker.ChronicsScheduleChecker;
import edu.monash.ppoi.checker.DateHandler;
import edu.monash.ppoi.constant.Parameters;
import edu.monash.ppoi.instance.Battery;
import edu.monash.ppoi.instance.Instance;
import edu.monash.ppoi.solution.Schedule;
import edu.monash.ppoi.utils.PseudoRandom;
import edu.monash.ppoi.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 算法总流程
 * 
 * @author qingling zhu
 *
 */
public class RABMain {
	private static List<Double> prices;
	private static ChronicsHandler chronicsHandler;

	public static void main(String[] args) {
		//args[0] = "large_4";
		String[] s = args[0].split("_");
		String a ="",b="";
		if(s[0].equals("small")){
			a = "0";
		}else if(s[0].equals("large")){
			a = "1";
		}
		b = s[1];
		String directory = "data/results/instance"+ a + b +"/finalOpt_" + args[0];
		File dir;
		int n, range;
		int maxRoom = 10;
		ChronicsScheduleChecker chronicsScheduleChecker;
		Evaluation evaluationRA;
		double bestCost, ttCost, batteryCost, globalCost;
		String instanceFilename = "./data/phase_2_instances/phase2_instance_" + args[0] + ".txt";
		Instance instance = Instance.parseInstance(new File(instanceFilename));
		System.out.println("Parameters.timeSlotsLength:" + Parameters.timeSlotsLength);
		chronicsHandler = new ChronicsHandler(Parameters.year, Parameters.month, Phase.P2.getLoadSeries(),
				Phase.P2.readPriceSeries());
		// Extract electricity prices.
		prices = chronicsHandler.getPrices();
		System.out.println("Begin to optimize instance:" + args[0]);
		int maxRun = Integer.parseInt(args[1]);
		System.out.println("It will run: " + maxRun + " times.!");
		int MaxBatteryIter = Integer.parseInt(args[2]);
		String who = args[3];
		
		int batteryNum = instance.getAllBatteries().size();
		int idx = 0, begin = 0;
		Evaluation evaluation = new Evaluation(instance,
				new int[instance.getAllBatteries().size()][Parameters.timeSlotsLength]);
		double mutation_rateR = 1.0 / (double) evaluation.co.numSameScopeR.size();
		double mutation_rateA = 1.0 / (double) evaluation.co.numSameScopeA.size();
		double mutation_rateRA = 1.0
				/ (double) (evaluation.co.numSameScopeA.size() + evaluation.co.numSameScopeR.size());
		double mutation_rateB = 0.001;// 1.0/(double)Parameters.timeSlotsLength;
		System.out.println("mR:" + mutation_rateR + "\nmA:" + mutation_rateA + "\nmB:" + mutation_rateB);
		int[][] thisBattery = new int[instance.getAllBatteries().size()][Parameters.timeSlotsLength];
		// 从globalBest文件夹中读取
		ArrayList<Integer> bestPermutation = null;
		Schedule bestSchedule = null;
		int[][] bestBattery = null;
		dir = new File(directory + "/globalBest/");
		for (File f : dir.listFiles()) {
			if (f.getName().startsWith("Permutation_")) {
				System.out.println("permutation file:" + f.getPath());
				bestPermutation = FileUtils.readCombination(f.getPath());
			} else if (f.getName().startsWith("phase2_instance_solution_")) {
				System.out.println("solution file:" + f.getPath());
				bestSchedule = Schedule.parseSchedule(f, instance);
			} else if (f.getName().startsWith("bestBatterySchedule_")) {
				System.out.println("battery file:" + f.getPath());
				bestBattery = FileUtils.readBatterySchedule(f.getPath(), batteryNum);
			}
		}
		while (bestSchedule == null) {
			System.out.println("No permutation file in globalBest directory.!!!! RANDOM INIT.!!");
			bestPermutation = new ArrayList<Integer>();
			// permOrderedRCourse,permR随机产生的组合
			begin = 0;
			for (int scopei=0;scopei<evaluation.co.numSameScopeR.size();scopei++) {
				n = evaluation.co.numSameScopeR.get(scopei);
				range = evaluation.co.rangeSameScopeR.get(scopei);
				int[] perm = new int[n];
				Utils.randomPermutation(perm, n);
				for (int i = 0; i < n; i++) {
					bestPermutation.add(perm[i] + begin);
					//bestPermutation.add(PseudoRandom.randInt(0, range*8-1));
					bestPermutation.add(PseudoRandom.randInt(0, Parameters.maxBoundR));
					bestPermutation.add(PseudoRandom.randInt(0,maxRoom));
				}
				begin += n;
			}
			// permOrderedACourse,permA随机产生的组合
			begin = 0;
			for (int scopei=0;scopei< evaluation.co.numSameScopeA.size();scopei++) {
				n = evaluation.co.numSameScopeA.get(scopei);
				range = evaluation.co.rangeSameScopeA.get(scopei);
				int[] perm = new int[n];
				Utils.randomPermutation(perm, n);
				for (int i = 0; i < n; i++) {
					bestPermutation.add(perm[i] + begin);
					//bestPermutation.add(PseudoRandom.randInt(0, range*96));
					bestPermutation.add(PseudoRandom.randInt(0, Parameters.maxBoundA));
					bestPermutation.add(PseudoRandom.randInt(0,maxRoom));
				}
				begin += n;
			}
			evaluation.fitness(bestPermutation);
			bestSchedule = evaluation.getSchedule();
		}
		if (bestBattery == null) {
			System.out.println("No battery with this timetable, then initial it with all zeros.");
			bestBattery = new int[instance.getAllBatteries().size()][Parameters.timeSlotsLength];
		}
		evaluationRA = new Evaluation(bestBattery);
		// 得到bestCost
		chronicsScheduleChecker = new ChronicsScheduleChecker(chronicsHandler, bestSchedule);
		bestCost = chronicsScheduleChecker.getObjective();
		System.out.println(bestCost);
		
		if(bestPermutation.size()==2*(instance.getAllOnceOff().size()+instance.getAllRecurring().size())) {
			//扩展room
			System.out.println("permutation is 2 times number of activities, then should be expanded by zeros..");
			for(idx = 0;idx+2 < 3*(instance.getAllOnceOff().size()+instance.getAllRecurring().size());idx=idx+3) {
				bestPermutation.add(idx+2, 0);
			}
		}else if(bestPermutation.size()==3*(instance.getAllOnceOff().size()+instance.getAllRecurring().size())) {
			System.out.println("permutation is 3 times number of activities.");
		}
//		System.exit(0);
		// 找到evaluationRA对应的permutation？？
		bestPermutation = evaluationRA.getNewPermutation(bestPermutation, bestSchedule);
		// System.out.println("recovery
		// score:"+evaluationRA.fitness(batteryPermutation));
		System.out.println("recovery score:" + evaluationRA.evaluate(evaluationRA.TimeTable1));
		System.out.println("recovery score1:" + evaluationRA.fitness(bestPermutation));
		
		double cost = evaluation.evaluate(evaluationRA.TimeTable1, bestBattery);
		System.out.println("begin with RAB cost:" + cost);
		if (Math.abs(cost - bestCost) > 0.0001) {
			System.out.println("this schedule is not consist with permutation and battery.!!!!!!");
			System.exit(0);
		}

		System.out.println("\n");
		System.out.println(java.time.LocalDateTime.now());
		// System.out.println(java.time.Clock.systemUTC().instant());
		System.out.println("******************************Begin to run ******************");

		int iterRA = 0, maxIterRA = 10;
		int run = 0;
		boolean isImproved = false;
		
		while (run < maxRun) {
			System.out.println(
					"\n======Begin to run :" + (run + 1) + "******************" + java.time.LocalDateTime.now());
			System.out.println("===============fixed battery to optimize timetable================");
			// 固定电池对permutation进行扰动
			evaluationRA = new Evaluation(bestBattery);
			// 找到evaluationRA对应的permutation？？
			ArrayList<Integer> batteryPermutation = evaluationRA.getNewPermutation(bestPermutation, bestSchedule);
			// System.out.println("recovery
			// score:"+evaluationRA.fitness(batteryPermutation));
			bestCost = evaluationRA.evaluate(evaluationRA.TimeTable1);
			System.out.println("recovery score:" + bestCost);
			ttCost = bestCost;//这里为什么需要2个呢？
			// 对bestPermutation进行local search
			iterRA = 0;
			isImproved = false;
			// 找一个新的permutation
			ArrayList<Integer> permutation = new ArrayList<Integer>();

			while (iterRA < maxIterRA) {
				idx = 0;
				begin = 0;
				permutation = new ArrayList<Integer>();

				for (int scopei=0;scopei<evaluation.co.numSameScopeR.size();scopei++) {
					n = evaluation.co.numSameScopeR.get(scopei);
					range = evaluation.co.rangeSameScopeR.get(scopei);
					if (PseudoRandom.randDouble() < mutation_rateRA) {// 刚开始的时候随机的机会很小，慢慢变大。
						// 进行扰动
						int[] perm = new int[n];
						Utils.randomPermutation(perm, n);
						for (int i = 0; i < n; i++) {
							permutation.add(perm[i] + begin);
							permutation.add(PseudoRandom.randInt(0, range*32-1));
							permutation.add(PseudoRandom.randInt(0, maxRoom));
							idx = idx + 3;
						}
					} else {
						// 保持best的不变。
						for (int i = 0; i < n; i++) {
							int bestidx = batteryPermutation.get(idx);
							permutation.add(bestidx);
							permutation.add(batteryPermutation.get(idx + 1));
							permutation.add(batteryPermutation.get(idx + 2));
							idx = idx + 3;
						}
					}
					begin += n;
				}
				// permOrderedACourse,permA随机产生的组合
				begin = 0;

				for (int scopei=0;scopei<evaluation.co.numSameScopeA.size();scopei++) {
					n = evaluation.co.numSameScopeA.get(scopei);
					range = evaluation.co.rangeSameScopeA.get(scopei);
					if (PseudoRandom.randDouble() < mutation_rateRA) {// 刚开始的时候随机的机会很小，慢慢变大。
						int[] perm = new int[n];
						Utils.randomPermutation(perm, n);
						for (int i = 0; i < n; i++) {
							permutation.add(perm[i] + begin);
							permutation.add(PseudoRandom.randInt(0, range*96));
							permutation.add(PseudoRandom.randInt(0, maxRoom));
							idx = idx + 3;
						}
					} else {
						// 保持原来的不变。
						for (int i = 0; i < n; i++) {
							int bestidx = batteryPermutation.get(idx);
							permutation.add(bestidx);
							permutation.add(batteryPermutation.get(idx + 1));
							permutation.add(batteryPermutation.get(idx + 2));
							idx = idx + 3;
						}
					}
					begin += n;
				}
				cost = evaluationRA.fitness(permutation);
				if (cost < ttCost) {
					System.out.println("find a permutation with cost:" + cost + " then find better solution = true.");
					iterRA = 0;
					ttCost = cost;
					batteryPermutation = new ArrayList<Integer>(permutation);
					bestSchedule = evaluationRA.getSchedule();
					bestPermutation = new ArrayList<Integer>(permutation);// 当排课提升了，电池没有提升时，需要更新bestPermutation.
					isImproved = true;
				}
				else if (!isImproved && iterRA > 0.8 * maxIterRA && Math.abs(cost - bestCost) > 0.1
						&& cost < bestCost * 1.001) {
					batteryPermutation = new ArrayList<Integer>(permutation);// 因为这是后退一步，所以不用覆盖bestPermutation.
					System.out.println("find a permutation with cost:" + cost + " then break.");
					break;
				}
				iterRA++;
				System.out.println(iterRA);
				if (iterRA % 10 == 0)
					System.out.print(iterRA + ",");
			}
			System.out.println("===============fixed timetable to optimize battery================");
			// ****************固定batteryPermutation来优化电池。。。
			evaluationRA.fitness(batteryPermutation);
			// 没有电池的排课分数
			double RAscore = evaluation.evaluate(evaluationRA.TimeTable1);
			System.out.println("RA score:" + RAscore + " then remove battery before and get new battery.");
			// 获取有排课，没有电池的baseload，去掉之前的电池
			List<Double> loadAfterTabling = new ArrayList<Double>(evaluationRA.loadAfterTabling);
			for (int batteryi = 0; batteryi < bestBattery.length; batteryi++) {
				Battery battery = instance.getAllBatteries().get(batteryi);
				for (int time = 0; time < Parameters.timeSlotsLength; time++) {
					// 1. 计算这个解对应的能量和load负载
					// -1:DisCharge, 0:hold, 1:Charge
					int actioni = bestBattery[batteryi][time];
					Battery.Act action = Battery.Act.HOLD;
					switch (actioni) {
					case -1:
						action = Battery.Act.DISCHARGE;
						break;
					case 0:
						action = Battery.Act.HOLD;
						break;
					case 1:
						action = Battery.Act.CHARGE;
						break;
					default:
						System.err.println("no this action value.!!!!!!!!!!");
					}
					loadAfterTabling.set(time, loadAfterTabling.get(time) - battery.getLoadKW(action));
				}
			}
			for (int batteryi = 0; batteryi < batteryNum; batteryi++) {
				for (int time = 0; time < Parameters.timeSlotsLength; time++) {
					thisBattery[batteryi][time] = bestBattery[batteryi][time];
				}
			}
			batteryCost = cost(instance.getAllBatteries(), thisBattery, loadAfterTabling);
			System.out.println("optimize battery from best Battery before with cost = "+batteryCost);
			
			int failed = 0;
			while (true) {
				// 对batterySchedule进行扰动
				int[][] newBattery = new int[batteryNum][Parameters.timeSlotsLength];
				for (int batteryi = 0; batteryi < batteryNum; batteryi++) {
					Battery battery = instance.getBattery(batteryi);
					double state = battery.getCapacityKWh();
					// int randi = PseudoRandom.randInt(0,
					// Parameters.timeSlotsLength-1);//使得每个新解跟最好解不一样
					for (int time = 0; time < Parameters.timeSlotsLength; time++) {
						// 1. 计算这个解对应的能量和load负载
						// -1:DisCharge, 0:hold, 1:Charge
						int actioni = 0;
						if (PseudoRandom.randDouble() < mutation_rateB)
							actioni = PseudoRandom.randInt(0, 2) - 1;
						else
							actioni = thisBattery[batteryi][time];
						// boolean valid = true;
						do {
							Battery.Act action = Battery.Act.HOLD;
							switch (actioni) {
							case -1:
								action = Battery.Act.DISCHARGE;
								break;
							case 0:
								action = Battery.Act.HOLD;
								break;
							case 1:
								action = Battery.Act.CHARGE;
								break;
							default:
								System.err.println("no this action value.!!!!!!!!!!");
							}
							// 2. 检查是否满足约束条件
							double energyKWh = battery.getEnergyKWh(action, 15d / 60d);// 15分钟后的电池能量
							state = state + energyKWh;
							// System.out.println(state);
							if (state < 0 || state > battery.getCapacityKWh()) {
								// valid = false;
								state = state - energyKWh;//
								if (PseudoRandom.randDouble() < mutation_rateB)
									actioni = PseudoRandom.randInt(0, 2) - 1;
								else
									actioni = thisBattery[batteryi][time];
							} else {
								break;
							}
						} while (true);
						newBattery[batteryi][time] = actioni;
					}

				}
				cost = cost(instance.getAllBatteries(), newBattery, loadAfterTabling);
				if (cost < batteryCost) {
					failed = 0;
					batteryCost = cost;
					// bestPermutation = new ArrayList<Integer>(permutation);
					for (int batteryi = 0; batteryi < batteryNum; batteryi++) {
						for (int time = 0; time < Parameters.timeSlotsLength; time++) {
							thisBattery[batteryi][time] = newBattery[batteryi][time];
						}
					}

				}
				failed++;
				if (failed % 10000 == 0) {
					System.out.print("("+failed + ", "+batteryCost+"),");
				}
				if (failed > MaxBatteryIter)
					break;
			}
			cost = evaluation.evaluate(evaluationRA.TimeTable1, thisBattery);
			if (cost < bestCost) {//整体有提升，保存batteryPermutation，thisBattery，并把他们赋值给bestPermutation和bestBattery
				System.out.println("RAB successed with cost" + cost);
				bestCost = cost;
				
				bestPermutation = new ArrayList<Integer>(batteryPermutation);
				for (int batteryi = 0; batteryi < batteryNum; batteryi++) {
					for (int time = 0; time < Parameters.timeSlotsLength; time++) {
						bestBattery[batteryi][time] = thisBattery[batteryi][time];
					}
				}
				bestSchedule = evaluation.getSchedule();
				// SAVE
				FileUtils.TimeTableAndBaseload2csv(evaluationRA.TimeTable1, loadAfterTabling,
						directory + "/TTandBaseload_" + args[0] + "_Scored" + String.format("%2.1f", bestCost) + "_"
								+who+ ".csv");
				FileUtils.saveCombination(bestPermutation,
						directory + "/Permutation_Scored" + String.valueOf((int) bestCost) + "_" +who+ ".csv");
				FileUtils.saveBatterySchedule(bestBattery, directory + "/bestBatterySchedule_Scored"
						+ String.format("%2.1f", bestCost) + "_" +who+ ".csv");
				System.out.println("Run:" + run + " get better RAB cost:" + cost);
				FileUtils.output(instance, evaluationRA.TimeTable1, bestBattery, directory
						+ "/phase2_instance_solution_" + args[0] + "_" + String.valueOf((int) bestCost) + "_" +who+ "_(RAB).txt");//
				FileUtils.TimeTable2csv(evaluationRA.TimeTable1,
						directory + "/TimeTable_" + String.valueOf((int) bestCost) +"_"+who+".csv",
						DateHandler.PERIODS_PER_DAY);
			}
			else{
				System.out.println("RAB failed with cost:" + cost + "; best score is:"+bestCost);
			}
			// 每次迭代结束和最好的对比
			ArrayList<Integer> globalPermutation = null;
			Schedule globalSchedule = null;
			int[][] globalBattery = null;
			dir = new File(directory + "/globalBest/");
			for (File f : dir.listFiles()) {
				if (f.getName().startsWith("Permutation_")) {
					globalPermutation = FileUtils.readCombination(f.getPath());
				} else if (f.getName().startsWith("phase2_instance_solution_")) {
					globalSchedule = Schedule.parseSchedule(f, instance);
				} else if (f.getName().startsWith("bestBatterySchedule_")) {
					globalBattery = FileUtils.readBatterySchedule(f.getPath(), batteryNum);
				}
			}
			if(globalSchedule!=null) {
				chronicsScheduleChecker = new ChronicsScheduleChecker(chronicsHandler, globalSchedule);
				globalCost = chronicsScheduleChecker.getObjective();
			}else {
				globalCost=Double.MAX_VALUE;
			}
			System.out.println("the global best score is:"+globalCost);
			if (globalCost < bestCost) {
				System.out.println(
						"=========================I have read a new permutation and battery===============================");
				bestSchedule = globalSchedule;
				bestCost = globalCost;
				// 别的程序找到了最好的解，则从其恢复最好的解，也即是bestPermutation and bestBattery
				bestPermutation = new ArrayList<Integer>(globalPermutation);
				for (int batteryi = 0; batteryi < batteryNum; batteryi++) {
					for (int time = 0; time < Parameters.timeSlotsLength; time++) {
						bestBattery[batteryi][time] = globalBattery[batteryi][time];
					}
				}
			}else if(globalCost>bestCost){
				//先删除globalBest
				dir = new File(directory + "/globalBest/");
				for(File f:dir.listFiles()) {
					if(!f.isDirectory())
						f.delete();
				}
				//保存到globalBest，方便其他程序同步。
				// SAVE
				FileUtils.TimeTableAndBaseload2csv(evaluationRA.TimeTable1, loadAfterTabling,
						directory + "/globalBest/TTandBaseload_" + args[0] + "_Scored" + String.format("%2.1f", bestCost) + "_"
								 +who+ ".csv");
				FileUtils.saveCombination(bestPermutation,
						directory + "/globalBest/Permutation_Scored" + String.valueOf((int) bestCost) + "_" +who+ ".csv");
				FileUtils.saveBatterySchedule(bestBattery, directory + "/globalBest/bestBatterySchedule_Scored"
						+ String.format("%2.1f", bestCost) + "_" +who+ ".csv");
				
				FileUtils.output(instance, evaluationRA.TimeTable1, bestBattery, directory
						+ "/globalBest/phase2_instance_solution_" + args[0] + "_" + String.valueOf((int) bestCost) +who+ "_(RAB).txt");//
				FileUtils.TimeTable2csv(evaluationRA.TimeTable1,
						directory + "/globalBest/TimeTable_" + String.valueOf((int) bestCost) +who+ ".csv",
						DateHandler.PERIODS_PER_DAY);
				System.out.println("This better solution has saved to globalBest directory for others!!!!!");
			}
			System.out.println("======end to run :" + (run + 1) + "******************" + java.time.LocalDateTime.now());
			run = run + 1;
		}
	}

	static double cost(List<Battery> batterys, int[][] batterySchedule, List<Double> baseload) {
		double cost = 0.0;
		int horizon = batterySchedule[0].length;

		ArrayList<Double> baseload_v = new ArrayList<Double>();
		// make a copy of baseload
		double max_load = Double.MIN_VALUE;
		for (Double load : baseload) {
			baseload_v.add(load);
			if (load > max_load) {
				max_load = load;
			}
		}
		double state, load;// [] = new double[batterys.size()];
		// double load[] = new double[batterys.size()];

		for (int batteryi = 0; batteryi < batterys.size(); batteryi++) {
			Battery battery = batterys.get(batteryi);
			state = battery.getCapacityKWh();// 刚开始都充满电。
			for (int time = 0; time < horizon; time++) {
				int actioni = batterySchedule[batteryi][time];
				Battery.Act action = Battery.Act.HOLD;
				switch (actioni) {
				case -1:
					action = Battery.Act.DISCHARGE;
					break;
				case 0:
					action = Battery.Act.HOLD;
					break;
				case 1:
					action = Battery.Act.CHARGE;
					break;
				default:
					System.err.println("no this action value.!!!!!!!!!!");
				}
				// DETERMINE LOAD ON ACTION
				load = battery.getLoadKW(action);
				cost += 0.001d * load * 900d / 3600d * prices.get(time);
				baseload_v.set(time, baseload_v.get(time) + load);

				// determine change in power
				state += battery.getEnergyKWh(action, 15d / 60d);// 15分钟后的电池能量
				if (state < 0 || state > battery.getCapacityKWh()) {
					System.out.println("a invalid solution. this is impossible.!!!!!!!!please check.");
					System.exit(0);
				}
			}
		}
		double max_load_v = Double.MIN_VALUE;
		for (int time = 0; time < horizon; time++) {
			if (baseload_v.get(time) > max_load_v) {
				max_load_v = baseload_v.get(time);
			}
		}
		cost += 0.005 * (max_load_v * max_load_v - max_load * max_load);// 如果超过最大负载，加一个大的惩罚项。
		return cost;
	}
}
