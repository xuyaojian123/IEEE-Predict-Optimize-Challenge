package edu.monash.io;

import edu.monash.ppoi.checker.DateHandler;
import edu.monash.ppoi.constant.Parameters;
import edu.monash.ppoi.instance.*;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static void writeStringToFile(String filename, String contents) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(contents);
        } catch (IOException ex) {
            throw new RuntimeException("Unhandled IOException.", ex);
        }
    }

    public static List<Double> readPriceCSV(String pathname) {

        List<Double> prices = new ArrayList<>();

        String contents = readFileAsString(pathname);
        String[] lines = contents.split("[\r\n]+");
        for (int i = 1; i < lines.length; i++) {

            String[] parts = lines[i].split(",");
            double price = Double.valueOf(parts[3]);

            // Twice because prices in CSV are every 30min and we schedule every 15min.
            prices.add(price);
            prices.add(price);
        }

        return prices;
    }

    /**
     * Reads in multiple CSVs in the order that they are provided.
     * <p>
     * Assumptions:
     * - each CSV is newline and comma separated,
     * - each CSV file contains a _single_ header row (which is discarded)
     * <p>
     * Files are not assumed to be equal width, but note that file
     * separators are also discarded.
     */
    public static List<String[]> readCSVs(List<String> filepaths) {

        List<String[]> rows = new ArrayList<>();

        for (String filepath : filepaths) {

            String contents = readFileAsString(filepath);
            String[] lines = contents.split("[\r\n]+");

            // First row is header
            for (int i = 1; i < lines.length; i++)
                rows.add(lines[i].split(","));
        }

        return rows;
    }

    /**
     * @see readFileAsString(String)
     */
    public static String readFileAsString(File file) {
        return readFileAsString(file.getPath());
    }

    /**
     * Convenience function to read the file (indicated by pathname), in its entirety, into
     * one String (assuming the default encoding, UTF-8). The String probably contains multiple
     * lines.
     * <p>
     * Not recommended for files 100+ MBs large.
     */
    public static String readFileAsString(String pathname) {
        try {
            return tryReadFileaAsString(pathname);
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Unhandled IOException when reading file %s.", pathname), ex);
        }
    }

    private static String tryReadFileaAsString(String pathname) throws IOException {
        return new String(Files.readAllBytes(FileSystems.getDefault().getPath(pathname)));
    }

    /**
     * 读取TimeTabe1表
     */
    public static ArrayList<Integer> readCombination(String fileName) {
        // String timeTable1CSV = "bestPerm.csv";
        ArrayList<Integer> perm = new ArrayList<Integer>();
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        // bestTimeTable = new String[nBuilding][DateHandler.PERIODS_PER_DAY * DAYS];
        try {
            br = new BufferedReader(new FileReader(fileName));
            // line = br.readLine();// 去除表头
            int bi = 0;
            while ((line = br.readLine()) != null) {
                String[] timeTable1Line = line.split(cvsSplitBy);
                for (int j = 1; j < timeTable1Line.length; j++) {
                    // bestTimeTable[bi][j - 1] = timeTable1Line[j];
                    perm.add(Integer.parseInt(timeTable1Line[j]));
                }
                bi++;
            }
            // TimeTable2csv(bestTimeTable, "TimeTable2.csv", DateHandler.PERIODS_PER_DAY);
            // System.out.println();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return perm;
    }

    public static void saveCombination(ArrayList<Integer> perm, String file) {
        BufferedWriter br;
        try {
            br = new BufferedWriter(new FileWriter(file));

            StringBuilder sb = new StringBuilder();
            // Append strings from array
            sb.append("permutation");
            for (Integer p : perm) {
                sb.append(",");
                sb.append(p);
                // System.out.print(p);
            }
            br.write(sb.toString());
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 按特定格式输出到output.txt文件
     *
     * @param instance
     * @param timeTable
     * @param allBest
     * @param outputFile
     */
    public static void output(Instance instance, String[][] timeTable, int[][] allBest, String outputFile) {
        List<Building> allBuildings = instance.getAllBuildings();
        BufferedWriter br;
        int schedR, schedA;
        boolean isScheduled = false;
        ArrayList<String> outFormate = new ArrayList<String>();
        String oneLine = "";
        int recurringNums = instance.getAllRecurring().size();
        int onceOffNums = instance.getAllOnceOff().size();
        List<Recurring> allRecurring = instance.getAllRecurring();
        List<OnceOff> allOnceOff = instance.getAllOnceOff();
        outFormate.add("ppoi " + instance.getAllBuildings().size() + " " + instance.getAllSolar().size() + " "
                + instance.getAllBatteries().size() + " " + recurringNums + " " + onceOffNums + "\n");
        outFormate.add("sched " + recurringNums + " " + onceOffNums + "\n");
        schedR = recurringNums;
        schedA = onceOffNums;
        for (int i = 0; i < recurringNums; i++) {
            Recurring recurring = allRecurring.get(i);
            String activityLabel = recurring.getActivityLabel();
            String findStr = activityLabel + recurring.getID();
            oneLine = activityLabel + " ";
            oneLine = oneLine + recurring.getID() + " ";
            isScheduled = false;
            int flag = 0;
            // 查找活动的安排时间段
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
                    // 使用|分割，转义字符
                    String[] splits = s.split("\\|");
                    for (String t : splits) {
                        // 判断是否是和要找的活动相等
                        if (t.indexOf(findStr) == 0 && t.charAt(findStr.length()) == '(') {
                            // 找到活动开始出现的时间段
                            int startTime = k;
                            oneLine = oneLine + startTime + " " + recurring.getNumRooms();
                            // 保存该活动分布在哪些建筑物（ID）的上
                            List<Integer> buildIDs = new ArrayList<>();
                            // 活动在该建筑上需要的房间数
                            int roomNums = Integer.parseInt(t.substring(t.indexOf("(") + 1, t.indexOf(")")));
                            for (int l = 0; l < roomNums; l++) {
                                // 取出房间对应的建筑id
                                int id = allBuildings.get(j).getID();
                                buildIDs.add(id);
                            }
                            // 查看其它建筑物是否有该活动（竖着向下查找）
                            for (int l = j + 1; l < timeTable.length; l++) {
                                if (timeTable[l][startTime] == null)
                                    continue;
                                String another = timeTable[l][startTime];
                                // 使用|分割，转义字符
                                String[] anotherSplits = another.split("\\|");
                                for (String t1 : anotherSplits) {
                                    if (t1.indexOf(findStr) == 0 && t1.charAt(findStr.length()) == '(') {
                                        roomNums = Integer
                                                .parseInt(t1.substring(t1.indexOf("(") + 1, t1.indexOf(")")));
                                        for (int m = 0; m < roomNums; m++) {
                                            // 取出房间对应的建筑id
                                            int id = allBuildings.get(l).getID();
                                            buildIDs.add(id);
                                        }
                                    }
                                }
                            }
                            flag = 1;
                            for (int l = 0; l < buildIDs.size(); l++) {
                                oneLine = oneLine + " " + buildIDs.get(l);
                            }
                            oneLine += "\n";
                            outFormate.add(oneLine);
                            isScheduled = true;
                            break;
                        }
                    }
                }
            }
            if (!isScheduled)
                schedR = schedR - 1;
        }
        for (int i = 0; i < onceOffNums; i++) {
			isScheduled = false;
            OnceOff onceOff = allOnceOff.get(i);
            String activityLabel = onceOff.getActivityLabel();
            String findStr = activityLabel + onceOff.getID();
            oneLine = activityLabel + " " + onceOff.getID() + " ";
            int flag = 0;
            // 查找一次性活动的安排时间段
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
                    // 使用|分割，转义字符
                    String[] splits = s.split("\\|");
                    for (String t : splits) {
                        // 判断是否是和要找的活动相等
                        if (t.indexOf(findStr) == 0 && t.charAt(findStr.length()) == '(') {
                            // 找到一次性活动开始出现的时间段
                            int startTime = k;
//								br.write(startTime + " " + onceOff.getNumRooms());
                            oneLine = oneLine + startTime + " " + onceOff.getNumRooms();
                            // 保存该一次性活动分布在哪些建筑物（ID）的上
                            List<Integer> buildIDs = new ArrayList<>();
                            // 活动在该建筑上需要的房间数
                            int roomNums = Integer.parseInt(t.substring(t.indexOf("(") + 1, t.indexOf(")")));
                            for (int l = 0; l < roomNums; l++) {
                                // 取出房间对应的建筑id
                                int id = allBuildings.get(j).getID();
                                buildIDs.add(id);
                            }
                            // 查看其它建筑物是否有该活动（竖着向下查找）
                            for (int l = j + 1; l < timeTable.length; l++) {
                                if (timeTable[l][startTime] == null)
                                    continue;
                                String another = timeTable[l][startTime];
                                // 使用|分割，转义字符
                                String[] anotherSplits = another.split("\\|");
                                for (String t1 : anotherSplits) {
                                    if (t1.indexOf(findStr) == 0 && t1.charAt(findStr.length()) == '(') {
                                        roomNums = Integer
                                                .parseInt(t1.substring(t1.indexOf("(") + 1, t1.indexOf(")")));
                                        for (int m = 0; m < roomNums; m++) {
                                            // 取出房间对应的建筑id
                                            int id = allBuildings.get(l).getID();
                                            buildIDs.add(id);
                                        }
                                    }
                                }
                            }
                            flag = 1;
                            for (int l = 0; l < buildIDs.size(); l++) {
                                oneLine = oneLine + " " + buildIDs.get(l);
                            }
                            oneLine += "\n";
                            outFormate.add(oneLine);
                            isScheduled = true;
                            break;
                        }
                    }
                }
            }
            if (!isScheduled)
                schedA = schedA - 1;
        }
        outFormate.set(1, "sched " + schedR + " " + schedA + "\n");
        // 电池的label标签 = “c"
        String batteryLabel = Battery.LABEL;
        List<Battery> allBatteries = instance.getAllBatteries();
        for (int bi = 0; bi < allBatteries.size(); bi++) {

            for (int t = 0; t < allBest[bi].length; t++) {
                if (allBest[bi][t] == -1) {
//						br.write(batteryLabel + " " + bi + " " + t + " " + 2 + "\n");
                    outFormate.add(batteryLabel + " " + bi + " " + t + " " + 2 + "\n");
                }
                if (allBest[bi][t] == 1) {
//						br.write(batteryLabel + " " + bi + " " + t + " " + 0 + "\n");
                    outFormate.add(batteryLabel + " " + bi + " " + t + " " + 0 + "\n");
                }
            }
            // br.write(batteryLabel + " " + b.getBatteryID() + " "+0 + " " + 1 + "\n");
        }
        try {
            br = new BufferedWriter(new FileWriter(outputFile));
            for (String s : outFormate)
                br.write(s);
            br.flush();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param TimeTable
     * @param filename
     */
    public static void TimeTable2csv(String[][] TimeTable, String filename, int timeslots_in_one_day) {
        BufferedWriter br;
        try {
            br = new BufferedWriter(new FileWriter(filename));

            StringBuilder sb = new StringBuilder();
            sb.append("timeslots");
            for (int dayi = 0; dayi < (TimeTable[0].length) / timeslots_in_one_day; dayi++) {
                for (int timesloti = 0; timesloti < timeslots_in_one_day; timesloti++) {
                    sb.append(",");
                    sb.append("day" + (dayi + 1) + "-t" + (timesloti + 1));
                }
            }
            sb.append("\n");
            // Append strings from array
            for (int i = 0; i < TimeTable.length; i++) {
                sb.append("b" + i);
                for (int j = 0; j < TimeTable[0].length; j++) {
                    sb.append(",");
                    sb.append(TimeTable[i][j]);
                }
                sb.append("\n");
            }

            br.write(sb.toString());
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 读取TimeTabe1表
     */
    public static String[][] readTimeTableAndBaseload(String file) {
//		String timeTable1CSV = "./results/TimeTable1.csv";
        String[][] TTandBaseload = new String[7][Parameters.timeSlotsLength];
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
//		bestTimeTable = new String[nBuilding][DateHandler.PERIODS_PER_DAY * DAYS];
        try {
            br = new BufferedReader(new FileReader(file));
            line = br.readLine();// 去除表头
            int bi = 0;
            while ((line = br.readLine()) != null) {
                String[] timeTable1Line = line.split(cvsSplitBy);
                for (int j = 1; j < timeTable1Line.length; j++) {
                    TTandBaseload[bi][j - 1] = timeTable1Line[j];
                }
                bi++;
            }
//			TimeTable2csv(TTandBaseload, "TimeTable2.csv", DateHandler.PERIODS_PER_DAY);
//			System.out.println();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return TTandBaseload;
    }

    public static void saveBatterySchedule(int[][] batterySchedule, String file) {
        BufferedWriter br;
        try {
            br = new BufferedWriter(new FileWriter(file));

            StringBuilder sb = new StringBuilder();
            // Append strings from array
            sb.append("batterySchedule");
            for (int i = 0; i < batterySchedule.length; i++) {
                for (int j = 0; j < batterySchedule[0].length; j++) {
                    sb.append(",");
                    sb.append(batterySchedule[i][j]);
                    // System.out.print(p);
                }
            }
            br.write(sb.toString());
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * 保存排课结束后的baseload到baseload.csv供电池充放电使用
     */
    public static void saveBaseload(String file, List<Double> baseload) {
        BufferedWriter br;
        try {
            br = new BufferedWriter(new FileWriter(file));

            StringBuilder sb = new StringBuilder();
            // Append strings from array
            sb.append("baseload");
            for (Double load : baseload) {
                sb.append(",");
                sb.append(load);
            }
            br.write(sb.toString());
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static void TimeTableAndBaseload2csv(String[][] TimeTable, List<Double> baseload, String filename) {
        BufferedWriter br;
        try {
            br = new BufferedWriter(new FileWriter(filename));

            StringBuilder sb = new StringBuilder();
            sb.append("timeslots");
            for (int dayi = 0; dayi < (TimeTable[0].length) / DateHandler.PERIODS_PER_DAY; dayi++) {
                for (int timesloti = 0; timesloti < DateHandler.PERIODS_PER_DAY; timesloti++) {
                    sb.append(",");
                    sb.append("day" + (dayi + 1) + "-t" + (timesloti + 1));
                }
            }
            sb.append("\n");
            // Append strings from array
            for (int i = 0; i < TimeTable.length; i++) {
                sb.append("b" + i);
                for (int j = 0; j < TimeTable[0].length; j++) {
                    sb.append(",");
                    sb.append(TimeTable[i][j]);
                }
                sb.append("\n");
            }
            sb.append("baseload");
            for (int j = 0; j < baseload.size(); j++) {
                sb.append(",");
                sb.append(baseload.get(j));
            }
            sb.append("\n");
            br.write(sb.toString());
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * 读取batterySchedule表
     */
    public static int[][] readBatterySchedule(String fileName, int batteryNum) {
        // String timeTable1CSV = "bestPerm.csv";
        int[][] newSolution = new int[batteryNum][Parameters.timeSlotsLength];
        ArrayList<Integer> perm = new ArrayList<Integer>();
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        // bestTimeTable = new String[nBuilding][DateHandler.PERIODS_PER_DAY * DAYS];
        try {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                String[] timeTable1Line = line.split(cvsSplitBy);
//				System.out.println("battery slots的总个数" + timeTable1Line.length);
                int num = 1;
                for (int i = 0; i < batteryNum; i++) {
                    for (int j = 0; j < Parameters.timeSlotsLength; j++) {
                        newSolution[i][j] = Integer.parseInt(timeTable1Line[num++]);
                    }
                }
            }
            // TimeTable2csv(bestTimeTable, "TimeTable2.csv", DateHandler.PERIODS_PER_DAY);
            // System.out.println();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return newSolution;
    }

    public static List<String[]> readWeatherCSV(String pathname) {

        List<String[]> weathers = new ArrayList<>();

        String contents = readFileAsString(pathname);
        String[] lines = contents.split("[\r\n]+");
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split(",");
            weathers.add(parts);
        }

        return weathers;
    }
}
