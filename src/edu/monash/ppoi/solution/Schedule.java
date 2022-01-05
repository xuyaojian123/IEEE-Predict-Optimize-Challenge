package edu.monash.ppoi.solution;

import edu.monash.io.FileUtils;
import edu.monash.ppoi.instance.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Schedule {

	private final Instance instance;

	private final List<RecurringSchedule> recurStart;
	private final List<OnceOffSchedule> onceOffStart;
	private final List<BatterySchedule> batterySchedule;

	public Schedule(Instance scheduled) {
		
		this.instance = scheduled;
		
		this.recurStart = new ArrayList<>();
		this.onceOffStart = new ArrayList<>();
		this.batterySchedule = new ArrayList<>();
	}

	public Instance getInstance() {
		return instance;
	}

	public void add(RecurringSchedule schedule) {
		recurStart.add(schedule);
	}

	public void add(OnceOffSchedule schedule) {
		onceOffStart.add(schedule);
	}

	public void add(BatterySchedule schedule) {
		batterySchedule.add(schedule);
	}

	public void addAll(List<BatterySchedule> schedule) {
		batterySchedule.addAll(schedule);
	}

	public List<RecurringSchedule> getRecurringSchedule() {
		return recurStart;
	}

	public List<OnceOffSchedule> getOnceOffSchedule() {
		return onceOffStart;
	}

	public List<BatterySchedule> getBatterySchedule() {
		return batterySchedule;
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		builder.append(instance.getHeaderString());
		builder.append('\n');

		builder.append(String.format("sched %d %d", recurStart.size(), onceOffStart.size()));
		builder.append('\n');

		appendSchedules(recurStart, builder);
		appendSchedules(onceOffStart, builder);
		appendSchedules(batterySchedule, builder);

		return builder.toString();
	}

	private static void appendSchedules(List<?> schedules, StringBuilder builder) {
		for (Object schedule : schedules)
			builder.append(schedule.toString());
	}

	public static Schedule parseSchedule(File file, Instance instance) {

		if (!file.exists()) {
			throw new RuntimeException("Missing file " + file.getAbsolutePath());
		}

		Schedule schedule = new Schedule(instance);
		String[] lines = FileUtils.readFileAsString(file.getPath()).split("[\r\n]+");

		int lineNum = 0;
		if (!instance.getHeaderString().equals(lines[0])) {
			throw new RuntimeException("Mismatch between schedule and instance.");
		}

		String[] header = lines[1].split(" ");
		int numRecurring = Integer.parseInt(header[1]);
		int numOnceOff = Integer.parseInt(header[2]);
		lineNum = 2;

		for (int i = 0; i < numRecurring; i++) {
			schedule.add(RecurringSchedule.parse(instance, lines[lineNum++]));
		}

		for (int i = 0; i < numOnceOff; i++) {
			schedule.add(OnceOffSchedule.parse(instance, lines[lineNum++]));
		}

		schedule.addAll(BatterySchedule.parse(instance, lines, lineNum));

		return schedule;
	}

	public static Schedule getSchedulefromTTandB(Instance instance, String[][] timeTable, int[][] battery) {
//		ArrayList<ArrayList<String>> outFormate = new ArrayList<ArrayList<String>>();
		ArrayList<String> outFormate = new ArrayList<String>();
		String oneLine = "";
		boolean isScheduled = false;
		List<Building> allBuildings = instance.getAllBuildings();
		int schedR, schedA;
//		BufferedWriter br;
//		try {
//			br = new BufferedWriter(new FileWriter(outputFile));
		int recurringNums = instance.getAllRecurring().size();
		int onceOffNums = instance.getAllOnceOff().size();
		List<Recurring> allRecurring = instance.getAllRecurring();
		List<OnceOff> allOnceOff = instance.getAllOnceOff();
//			ArrayList<String> oneLine = new ArrayList<String>();
//			oneLine.add("ppoi");
//			oneLine.add(String.format("%d", instance.getAllBuildings().size()));
//			oneLine.add(String.format("%d", instance.getAllSolar().size()));
//			oneLine.add(String.format("%d", instance.getAllBatteries().size()));
//			oneLine.add(String.format("%d", recurringNums));
//			oneLine.add(String.format("%d", onceOffNums));
		outFormate.add("ppoi " + instance.getAllBuildings().size() + " " + instance.getAllSolar().size() + " "
				+ instance.getAllBatteries().size() + " " + recurringNums + " " + onceOffNums);

//			br.write("ppoi " + instance.getAllBuildings().size() + " " + instance.getAllSolar().size() + " "
//					+ instance.getAllBatteries().size() + " " + recurringNums + " " + onceOffNums + "\n");
//			oneLine = new ArrayList<String>();
//			oneLine.add("sched");
//			oneLine.add(String.format("%d", "0"));
//			oneLine.add(String.format("%d", "0"));
		outFormate.add("sched " + recurringNums + " " + onceOffNums + "\n");
		schedR = recurringNums;
		schedA = onceOffNums;
//			br.write("sched " + recurringNums + " " + onceOffNums + "\n");
		for (int i = 0; i < recurringNums; i++) {
			Recurring recurring = allRecurring.get(i);
			String activityLabel = recurring.getActivityLabel();
			String findStr = activityLabel + recurring.getID();
//				oneLine = new ArrayList<String>();
//				br.write(activityLabel + " ");
			oneLine = activityLabel + " ";
//				oneLine.add(activityLabel);
//				oneLine.add(String.format("%d", recurring.getID()));
//				br.write(recurring.getID() + " ");
			oneLine = oneLine + recurring.getID() + " ";
			isScheduled = false;
			int flag = 0;
			// ���һ�İ���ʱ���
			for (int j = 0; j < timeTable.length; j++) {
				if (flag == 1) {
					break;
				}
				for (int k = 0; k < timeTable[j].length; k++) {
					if (flag == 1)
						break;
					if (timeTable[j][k] == null)
						continue;
					String s = timeTable[j][k];
					// ʹ��|�ָת���ַ�
					String[] splits = s.split("\\|");

					for (String t : splits) {
						// �ж��Ƿ��Ǻ�Ҫ�ҵĻ���
						if (t.indexOf(findStr) == 0 && t.charAt(findStr.length()) == '(') {
							// �ҵ����ʼ���ֵ�ʱ���
							int startTime = k;
							// �ҵ�finStr(�����:r11)��һ�γ��ֵ��ſ�ʱ��,�жϻ�Ƿ���383����863ʱ�����
//								if (startTime < 384) {
//									// ���������Χ�ڣ�����ҵڶ��γ��ָû��ʱ��Σ���һ����ݵ�ʱ��startTime = startTime + 96*7��Ҳ�������ڶ��γ��ֵ�ʱ��Σ�
//									startTime += 96 * 7;
//								}
//								br.write(startTime + " " + recurring.getNumRooms());
//								oneLine.add(String.format("%d", startTime));
//								oneLine.add(String.format("%d", recurring.getNumRooms()));
							oneLine = oneLine + startTime + " " + recurring.getNumRooms();
							// ����û�ֲ�����Щ�����ID������
							List<Integer> buildIDs = new ArrayList<>();
							// ��ڸý�������Ҫ�ķ�����
							int roomNums = Integer.parseInt(t.substring(t.indexOf("(") + 1, t.indexOf(")")));
							for (int l = 0; l < roomNums; l++) {
								// ȡ�������Ӧ�Ľ���id
								int id = allBuildings.get(j).getID();
								buildIDs.add(id);
							}
							// �鿴�����������Ƿ��иû���������²��ң�
							for (int l = j + 1; l < timeTable.length; l++) {
								if (timeTable[l][startTime] == null)
									continue;
								String another = timeTable[l][startTime];
								// ʹ��|�ָת���ַ�
								String[] anotherSplits = another.split("\\|");
								for (String t1 : anotherSplits) {
									if (t1.indexOf(findStr) == 0 && t1.charAt(findStr.length()) == '(') {
										roomNums = Integer
												.parseInt(t1.substring(t1.indexOf("(") + 1, t1.indexOf(")")));
										for (int m = 0; m < roomNums; m++) {
											// ȡ�������Ӧ�Ľ���id
											int id = allBuildings.get(l).getID();
											buildIDs.add(id);
										}
									}
								}
							}
							flag = 1;
							for (int l = 0; l < buildIDs.size(); l++) {
//									br.write(" " + buildIDs.get(l));
								oneLine = oneLine + " " + buildIDs.get(l);
//									oneLine.add(String.format("%d", buildIDs.get(l)));
							}
//								br.write("\n");
//								if(oneLine.size()>2) {
							outFormate.add(oneLine);
							isScheduled = true;
//								}else {
//									schedR = schedR -1;
////									Integer.parseInt(outFormate.get(1).get(1)) =
////									outFormate.set(1, )
//								}
							break;
						}
					}
				}
			}
			if(!isScheduled)
				schedR = schedR -1;
		}

		for (int i = 0; i < onceOffNums; i++) {
			OnceOff onceOff = allOnceOff.get(i);
			String activityLabel = onceOff.getActivityLabel();
			String findStr = activityLabel + onceOff.getID();
//				oneLine = new ArrayList<String>();
//				oneLine.add(activityLabel);
//				oneLine.add(String.format("%d", onceOff.getID()));
//				br.write(activityLabel + " ");
//				br.write(onceOff.getID() + " ");
			oneLine = activityLabel + " " + onceOff.getID() + " ";
			isScheduled = false;
			int flag = 0;
			// ����һ���Ի�İ���ʱ���
			for (int j = 0; j < timeTable.length; j++) {
				if (flag == 1) {
					break;
				}
				for (int k = 0; k < timeTable[j].length; k++) {
					if (flag == 1)
						break;
					if (timeTable[j][k] == null)
						continue;
					String s = timeTable[j][k];
					// ʹ��|�ָת���ַ�
					String[] splits = s.split("\\|");
					for (String t : splits) {
						// �ж��Ƿ��Ǻ�Ҫ�ҵĻ���
						if (t.indexOf(findStr) == 0 && t.charAt(findStr.length()) == '(') {
							// �ҵ�һ���Ի��ʼ���ֵ�ʱ���
							int startTime = k;
//								br.write(startTime + " " + onceOff.getNumRooms());
//								oneLine.add(String.format("%d", startTime));
							oneLine = oneLine + startTime + " " + onceOff.getNumRooms();
							// �����һ���Ի�ֲ�����Щ�����ID������
							List<Integer> buildIDs = new ArrayList<>();
							// ��ڸý�������Ҫ�ķ�����
							int roomNums = Integer.parseInt(t.substring(t.indexOf("(") + 1, t.indexOf(")")));
							for (int l = 0; l < roomNums; l++) {
								// ȡ�������Ӧ�Ľ���id
								int id = allBuildings.get(j).getID();
								buildIDs.add(id);
							}
							// �鿴�����������Ƿ��иû���������²��ң�
							for (int l = j + 1; l < timeTable.length; l++) {
								if (timeTable[l][startTime] == null)
									continue;
								String another = timeTable[l][startTime];
								// ʹ��|�ָת���ַ�
								String[] anotherSplits = another.split("\\|");
								for (String t1 : anotherSplits) {
									if (t1.indexOf(findStr) == 0 && t1.charAt(findStr.length()) == '(') {
										roomNums = Integer
												.parseInt(t1.substring(t1.indexOf("(") + 1, t1.indexOf(")")));
										for (int m = 0; m < roomNums; m++) {
											// ȡ�������Ӧ�Ľ���id
											int id = allBuildings.get(l).getID();
											buildIDs.add(id);
										}
									}
								}
							}
							flag = 1;
							for (int l = 0; l < buildIDs.size(); l++) {
//									br.write(" " + buildIDs.get(l));
//									oneLine.add(String.format("%d", buildIDs.size()));
								oneLine = oneLine + " " + buildIDs.get(l);
							}
//								if(oneLine.size()>2) {
							outFormate.add(oneLine);
							isScheduled = true;
//								}else {
//									schedA = schedA - 1;
//								}
//								br.write("\n");
							break;
						}
					}
				}
			}
			if(!isScheduled)
				schedA = schedA - 1;
		}
		outFormate.set(1, "sched " + schedR + " " + schedA);
		// ��ص�label��ǩ = ��c"
		String batteryLabel = Battery.LABEL;
		List<Battery> allBatteries = instance.getAllBatteries();
		for (int bi = 0; bi < allBatteries.size(); bi++) {

			for (int t = 0; t < battery[bi].length; t++) {
				if (battery[bi][t] == -1) {
//						br.write(batteryLabel + " " + bi + " " + t + " " + 2 + "\n");
//						oneLine = new ArrayList<String>();
//						oneLine.add(batteryLabel);
//						oneLine.add(String.format("%d", bi));
//						oneLine.add(String.format("%d", t));
//						oneLine.add(String.format("%d", 2));
					outFormate.add(batteryLabel + " " + bi + " " + t + " " + 2);
				}
				if (battery[bi][t] == 1) {
//						br.write(batteryLabel + " " + bi + " " + t + " " + 0 + "\n");
//						oneLine = new ArrayList<String>();
//						oneLine.add(batteryLabel);
//						oneLine.add(String.format("%d", bi));
//						oneLine.add(String.format("%d", t));
//						oneLine.add(String.format("%d", 0));
					outFormate.add(batteryLabel + " " + bi + " " + t + " " + 0);
				}
			}
			// br.write(batteryLabel + " " + b.getBatteryID() + " "+0 + " " + 1 + "\n");
		}

//			br.flush();
//			br.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	public static Schedule parseSchedule(File file, Instance instance) {

//		if (!file.exists()) {
//			throw new RuntimeException("Missing file " + file.getAbsolutePath());
//		}

		Schedule schedule = new Schedule(instance);
//		String[] lines = FileUtils.readFileAsString(file.getPath()).split("[\r\n]+");

		int lineNum = 0;
//		if (!instance.getHeaderString().equals(lines[0])) {
//			throw new RuntimeException("Mismatch between schedule and instance.");
//		}

		String[] header = outFormate.get(1).split(" ");
		int numRecurring = Integer.parseInt(header[1]);
		int numOnceOff = Integer.parseInt(header[2]);
		lineNum = 2;

		for (int i = 0; i < numRecurring; i++) {
			schedule.add(RecurringSchedule.parse(instance, outFormate.get(lineNum++)));
		}

		for (int i = 0; i < numOnceOff; i++) {
			schedule.add(OnceOffSchedule.parse(instance, outFormate.get(lineNum++)));
		}

		schedule.addAll(BatterySchedule.parse(instance, outFormate, lineNum));

		return schedule;
	}

}
