package edu.monash.ppoi;

import edu.monash.io.FileUtils;
import edu.monash.io.tsf.TimeSeriesDB;
import edu.monash.ppoi.checker.ChronicsHandler;
import edu.monash.ppoi.checker.ChronicsScheduleChecker;
import edu.monash.ppoi.checker.DateHandler;
import edu.monash.ppoi.checker.ScheduleChecker;
import edu.monash.ppoi.constant.Parameters;
import edu.monash.ppoi.entity.Course;
import edu.monash.ppoi.entity.CourseOrdering;
import edu.monash.ppoi.entity.L;
import edu.monash.ppoi.instance.*;
import edu.monash.ppoi.solution.OnceOffSchedule;
import edu.monash.ppoi.solution.RecurringSchedule;
import edu.monash.ppoi.solution.Schedule;
import edu.monash.ppoi.utils.PseudoRandom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 评价一个组合的好坏，这个组合的维度是nR+nA维的permutation
 *
 * @author qingling zhu 输出得到这个组合的cost，如果更好，则打印
 */
public class Evaluation {
    //注释了
    //static DateHandler date;
    static List<Double> prices;
    private double max_load;
    private double max_load_v;// 用来传出cost中的max_load
    //    private double max_load_afterDischarge;
    private List<Double> baseload;
    private List<Double> baseload_v;
    public List<Double> loadAfterTabling;
    static CourseOrdering co;
    // static List<Course> OrderedRCourse;
    static ArrayList<List<Integer>> postConditionR;
    // static List<Course> OrderedACourse;
    static ArrayList<List<Integer>> postConditionA;
    // static int[] bestBatterySchedule;// 刚开始是全0-HOLD
//    static List<Double> batteryEnergy;// 记录整个过程中，电池的能量，刚开始能量为capacity
//    private String who;
    static final boolean isDebug = false;

    // int instance_type;
    // int instance_idx;
    static Instance instance;
    static int nBuilding;
    static String instanceFilename;
    // String scheduleFilename;
    static ChronicsHandler chronicsHandler;
    //TimeSeriesDB db;
    // 排课结果
    public String[][] TimeTable1;
    private int[][] batterySchedule;
    private Schedule schedule;

    public Evaluation(Instance inst, int[][] batterySchedule) {
//        this.who = who;
        instance = inst;
        nBuilding = instance.getAllBuildings().size();
        chronicsHandler = new ChronicsHandler(Parameters.year, Parameters.month, Phase.P2.getLoadSeries(), Phase.P2.readPriceSeries());
        //我添加的
        prices = chronicsHandler.getPrices();
        baseload = ChronicsScheduleChecker.getBaseLoad(chronicsHandler, instance);
        ChronicsScheduleChecker.updateBaseLoad(baseload);
        assert (baseload.size() == Parameters.timeSlotsLength);// 每个时间点的初始负载
        // baseload加上batterySchedule
        double load;
        boolean valid = true;
        for (int batteryi = 0; batteryi < batterySchedule.length; batteryi++) {
            Battery battery = instance.getAllBatteries().get(batteryi);
            double state = battery.getCapacityKWh();//
            for (int time = 0; time < Parameters.timeSlotsLength; time++) {
                // 1. 计算这个解对应的能量和load负载
                // -1:DisCharge
                // 0:hold
                // 1:Charge
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
                load = battery.getLoadKW(action);// 这个解会给baseload在这个时间点time添加多少负载。
                // 改变baseload
                baseload.set(time, baseload.get(time) + load);
                if (baseload.get(time) > max_load) {
                    max_load = baseload.get(time);
                }
                // 2. 检查是否满足约束条件
                state += battery.getEnergyKWh(action, 15d / 60d);// 15分钟后的电池能量
                // System.out.println(state);
                if (state < 0 || state > battery.getCapacityKWh()) {
                    valid = false;
                }
            }
        }
        if (!valid) {
            System.err.println("the battery Schedule is not valid.!!!!!");
            System.exit(0);
        }
        this.batterySchedule = batterySchedule;
        preProcess(instance);
    }

    public Evaluation(int[][] batterySchedule) {
//        this.who = who;
        baseload = ChronicsScheduleChecker.getBaseLoad(chronicsHandler, instance);
        ChronicsScheduleChecker.updateBaseLoad(baseload);
        assert (baseload.size() == Parameters.timeSlotsLength);// 每个时间点的初始负载
        // baseload加上batterySchedule
        double load;
        boolean valid = true;
        for (int batteryi = 0; batteryi < batterySchedule.length; batteryi++) {
            Battery battery = instance.getAllBatteries().get(batteryi);
            double state = battery.getCapacityKWh();//
            for (int time = 0; time < Parameters.timeSlotsLength; time++) {
                // 1. 计算这个解对应的能量和load负载
                // -1:DisCharge
                // 0:hold
                // 1:Charge
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
                load = battery.getLoadKW(action);// 这个解会给baseload在这个时间点time添加多少负载。
                // 改变baseload
                baseload.set(time, baseload.get(time) + load);
                if (baseload.get(time) > max_load) {
                    max_load = baseload.get(time);
                }
                // 2. 检查是否满足约束条件
                state += battery.getEnergyKWh(action, 15d / 60d);// 15分钟后的电池能量
                // System.out.println(state);
                if (state < 0 || state > battery.getCapacityKWh()) {
                    valid = false;
                }
            }
        }
        if (!valid) {
            System.err.println("the battery Schedule is not valid.!!!!!");
            System.exit(0);
        }
        this.batterySchedule = batterySchedule;
    }

    public double fitness(List<Integer> bestPermutation) {
        // 先保存下baseload和max_load
        loadAfterTabling = new ArrayList<Double>();
        max_load = Double.MIN_VALUE;
        for (Double load : baseload) {
            loadAfterTabling.add(load);
            if (load > max_load)
                max_load = load;
        }
        int[] mapping = new int[32 * 5];// 映射到真实的timeslots上，160->2976
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 32; j++) {
                //第一阶段的mapping
                //mapping[32 * i + j] = 420 + 96 * i + j;
                //第二阶段的mapping
                mapping[32 * i + j] = 88 + 96 * i + j;
            }
        }

        List<Course> permOrderedRCourse = new ArrayList<Course>();
        List<Integer> RCourseBestK = new ArrayList<Integer>();
        List<Course> permOrderedACourse = new ArrayList<Course>();
        List<Integer> ACourseBestK = new ArrayList<Integer>();

        ExpandTimeTable ett;// = new ExpandTimeTable();

        int idx = 0;

        for (Integer n : co.numSameScopeR) {
            for (int i = 0; i < n; i++) {
                int bestidx = bestPermutation.get(idx);
                permOrderedRCourse.add(new Course(co.getOrderedRCourse().get(bestidx)));
                RCourseBestK.add(bestPermutation.get(idx + 1));
                // permutation.add(bestidx);
                idx = idx + 2;
            }
        }
        assert (permOrderedRCourse.size() == instance.getAllRecurring().size());

        for (Integer n : co.numSameScopeA) {
            for (int i = 0; i < n; i++) {
                int bestidx = bestPermutation.get(idx);
                permOrderedACourse.add(new Course(co.getOrderedACourse().get(bestidx)));
                ACourseBestK.add(bestPermutation.get(idx + 1));
                idx = idx + 2;
            }
        }
        assert (permOrderedACourse.size() == instance.getAllOnceOff().size());

        int[][][] ClassRoomLeft = new int[8 * 4 * Parameters.WORKINGDAY][nBuilding][2];
        for (int i = 0; i < nBuilding; i++) {
            int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
            int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
            for (int j = 0; j < 8 * 4 * Parameters.WORKINGDAY; j++) {
                ClassRoomLeft[j][i][0] = nsmall;
                ClassRoomLeft[j][i][1] = nlarge;
            }
        }

        String[][] TimeTable = new String[nBuilding][8 * 4 * Parameters.WORKINGDAY];

        for (int coursei = 0; coursei < permOrderedRCourse.size(); coursei++) {
            Course curCourse = permOrderedRCourse.get(coursei);

            int RActivityi = curCourse.getId();// 对这个活动安排上
            if (isDebug)
                System.out.print("begin schedule Recurring:" + RActivityi);
            Recurring activity = instance.getRecurring(RActivityi);

            int t_start = 32 * (curCourse.getLow() - 1), t_end = 32 * curCourse.getTop() - 1;
            ArrayList<L> allCost = new ArrayList<L>();
            // 计算安排在可行的timeslot的cost
            for (int t = t_start; t <= t_end; t++) {
                if (Math.floorMod(t, 32) + activity.getDuration() - 1 >= 32)
                    continue;// 不能跨天安排
                double cost = cost(activity, mapping[t]);// t时间段对应一个月的时间段
                // 用L记录这个Recurring安排在t时刻的cost
                L l = new L();
                l.setI(RActivityi);
                l.setJ(t);
                l.setCost(Double.parseDouble(String.format("%.4f", cost)));// 如果activityi安排在t，cost是多少？

                if (max_load_v > max_load)
                    l.setMax_load(max_load_v);// 设为较大的那个
                else
                    l.setMax_load(max_load);// 保持不变
                l.setBaseload(baseload_v);// 改变
                allCost.add(l);// 安排在时间段t，
            }
            allCost.sort(new Comparator<L>() {
                // 从小到大进行排序。
                @Override
                public int compare(L l1, L l2) {
                    // TODO Auto-generated method stub
                    double cost1 = l1.getCost();
                    double cost2 = l2.getCost();
                    if (cost1 < cost2)
                        return -1;
                    else if (cost1 > cost2)
                        return 1;
                    else if (l1.getJ() < l1.getJ())
                        return -1;
                    else if (l1.getJ() > l1.getJ())
                        return 1;
                    else
                        return 0;
                }

            });
            // 从小到大遍历allCost,找到合适的timeslot进行插入
            boolean isOK = false;// 在所有timeslot里面是否找到合适的地方安排？
            int bestk = 0;//选择第几个最好的。
            //boolean[] isPFeasible = new boolean[5];

            //for(int weekday=1;weekday<=5;weekday++) {
            //	List<Course> permOrderedRCourse1 = new ArrayList<Course>();
            //	for(Course c: permOrderedRCourse)
            //		permOrderedRCourse1.add(new Course(c));
            //	isPFeasible[weekday-1] = checkPostCondition(permOrderedRCourse1, coursei, RActivityi, weekday);
            //}
            for (int li = 0; li < allCost.size(); li++) {// 对应的是96*4(384)->96*9-1(包含)
                int timeslot = allCost.get(li).getJ();
                if (Math.floorMod(timeslot, 32) + activity.getDuration() - 1 >= 32)
                    continue;// 不能跨天安排

                // 在这些timeslots中，判断哪个timeslot的开销cost最小，记录它，并使用这个timeslot

                // 先看一栋楼的安排

                int nRooms = instance.getRecurring(RActivityi).getNumRooms();
                Room roomType = instance.getRecurring(RActivityi).getRoomSize();
                assert (nRooms < 10);
                // 如果一栋楼可以安排下，则优先一栋楼。如果不可以，则找多栋楼的组合。
                ArrayList<int[]> buildIDs = new ArrayList<int[]>();// 那栋楼，多少个房间。
                // boolean isMixed = true;
                int nRoomsRemaining = nRooms;
                boolean isFeasibleST = false;
                for (int buildi = 0; buildi < nBuilding; buildi++) {
                    int nlargeRoomLeft = Integer.MAX_VALUE;// 这个duration期间这栋楼最多可用的大小房间数
                    int nsmallRoomLeft = Integer.MAX_VALUE;
                    // boolean isOccupied = false;
                    for (int t = 0; t < instance.getRecurring(RActivityi).getDuration(); t++) {
                        // if(TimeTable[buildi][timeslot+t]!=null)
                        // isOccupied=true;
                        if (ClassRoomLeft[timeslot + t][buildi][1] < nlargeRoomLeft)
                            nlargeRoomLeft = ClassRoomLeft[timeslot + t][buildi][1];
                        if (ClassRoomLeft[timeslot + t][buildi][0] < nsmallRoomLeft)
                            nsmallRoomLeft = ClassRoomLeft[timeslot + t][buildi][0];
                    }
                    // if(isOccupied)
                    // continue;
                    if (roomType.equals(Room.Large)) {
                        if (nRooms <= nlargeRoomLeft) {
                            // 需要大房间，且这个时间timeslot的这栋楼buildi有这么多的大房间，那么就安排在这里。
                            buildIDs.clear();
                            int[] temp = {buildi, nRooms};
                            buildIDs.add(temp);
                            isFeasibleST = true;
                            break;
                        } else {
                            // 一栋楼装不下
                            if (nRoomsRemaining > 0 && nlargeRoomLeft > 0) {
                                // 如果剩余需要的房间数>0
                                int nOccupiedRoom = Math.min(nRoomsRemaining, nlargeRoomLeft);// 实际占用的房间数，（需要1，有2），（需要3，有1）
                                int[] temp = {buildi, nOccupiedRoom};// 把剩下的房间都占用
                                buildIDs.add(temp);
                                nRoomsRemaining = nRoomsRemaining - nOccupiedRoom;// 减掉加入的房间数
                                assert (nRoomsRemaining >= 0);
                            }
                        }
                    } else if (roomType.equals(Room.Small)) {
                        if (nRooms <= nsmallRoomLeft) {
                            // 需要小房间，且这个时间timeslot的这栋楼buildi有这么多的小房间，那么就安排在这里。
                            buildIDs.clear();
                            int[] temp = {buildi, nRooms};
                            buildIDs.add(temp);
                            isFeasibleST = true;
                            break;
                        } else {
                            // 一栋楼装不下
                            if (nRoomsRemaining > 0 && nsmallRoomLeft > 0) {
                                // 如果剩余需要的房间数>0
                                int nOccupiedRoom = Math.min(nRoomsRemaining, nsmallRoomLeft);// 实际占用的房间数，（需要1，有2），（需要3，有1）
                                int[] temp = {buildi, nOccupiedRoom};// 把剩下的房间都占用
                                buildIDs.add(temp);
                                nRoomsRemaining = nRoomsRemaining - nOccupiedRoom;// 减掉加入的房间数
                                assert (nRoomsRemaining >= 0);
                            }
                        }
                    } else {
                        System.err.print("not possible!!!!");
                    }
                }
                if (nRoomsRemaining == 0 || isFeasibleST) {//房间足够
                    //判断这个活动的后置活动是否有可行安排的时间段。
                    //检查所有后置活动是否满足！！

                    int weekday = Math.floorDiv(timeslot, 32) + 1;//1,2,3,4,5


                    //if(!isPFeasible[weekday-1]) {
                    //	System.out.println("Activity: "+RActivityi+" cannot put at TimeSlot:"+timeslot+"weekday:"+weekday);
                    //	continue;
                    //}


                    if (RCourseBestK.get(coursei) != bestk) {
                        bestk++;
                        continue;
                    }
                    // 如果在这个时间段能安排下，则安排。如果不能则下一个时间段。
                    for (int buildi = 0; buildi < buildIDs.size(); buildi++) {
                        int[] temp = buildIDs.get(buildi);

                        // 这栋楼的剩余房间更新
                        for (int t = 0; t < instance.getRecurring(RActivityi).getDuration(); t++) {
                            if (TimeTable[temp[0]][timeslot + t] == null)
                                TimeTable[temp[0]][timeslot + t] = "r" + RActivityi + "(" + temp[1] + ")";
                            else
                                TimeTable[temp[0]][timeslot + t] += "|r" + RActivityi + "(" + temp[1] + ")";// 安排在哪栋楼
                            // TimeTableRooms[temp[0]][timeslot+t] = temp[1];
                            if (roomType.equals(Room.Small)) {
                                ClassRoomLeft[timeslot + t][temp[0]][0] -= temp[1];
                                assert (ClassRoomLeft[timeslot + t][temp[0]][0] >= 0);
                            } else {
                                ClassRoomLeft[timeslot + t][temp[0]][1] -= temp[1];
                                assert (ClassRoomLeft[timeslot + t][temp[0]][1] >= 0);
                            }
                        }
                    }
                    // TimeTable2csv(TimeTable);
                    isOK = true;
                    // nScheduled = nScheduled+1;
                    // isScheduled[RActivityi] = Math.floorDiv(timeslot, 32)+1;//安排在星期几
                    assert (RActivityi == instance.getRecurring(RActivityi).getID());
                    // ScheduledID.add(RActivityi);


                    // 如果安排了这个活动，检查它安排在星期几，然后检测所有它的后置活动的范围看是否需要修正。
                    //boolean T = checkPostCondition(permOrderedRCourse, coursei, RActivityi, weekday);
                    permOrderedRCourse.get(coursei).setLow(weekday);
                    //assert (T == true);
//					if (isDebug)
//						System.out.println(" in day:" + weekday);
                    List<Integer> postConditionI = postConditionR.get(RActivityi);
                    assert (postConditionR.size() == instance.getAllRecurring().size());
                    for (Integer posti : postConditionI) {// 对于该活动的每个后置活动
                        for (int ci = coursei + 1; ci < permOrderedRCourse.size(); ci++) {
                            Course c = permOrderedRCourse.get(ci);
                            if (c.getId() == posti) {// 对所有活动的后置活动的下限进行修正
                                if (weekday + 1 > c.getLow()) {
                                    int origin_scope = (c.getTop() - c.getLow() + 1) * 32 - 1;
                                    c.setLow(weekday + 1);
                                    int new_scope = (c.getTop() - c.getLow() + 1) * 32 - 1;
                                    assert (c.getLow() <= c.getTop());
                                    if (isDebug)
                                        System.out.println("r" + c.getId() + " change to [" + c.getLow() + ","
                                                + c.getTop() + "]!!!!!!!!!!!");

                                    //重新设置所选择的位置。
                                    if (RCourseBestK.get(ci) > new_scope) {
                                        int new_bestK = (int) Math.floor((double) RCourseBestK.get(ci) / (double) origin_scope * (double) new_scope);
                                        RCourseBestK.set(ci, new_bestK);
                                    }
                                }
                            }
                        }
                    }

                    // 重新设置max_load
                    max_load = allCost.get(li).getMax_load();
                    loadAfterTabling.clear();
                    for (Double load : allCost.get(li).getBaseload())
                        loadAfterTabling.add(load);
                    break;
                } else {

                }
            }
            if (!isOK) {
//				System.out.println("*** r" + RActivityi + " activity that have not scheduled1.!!");
                return Double.MAX_VALUE;
                // System.exit(0);
            }
        }

        // TimeTable2csv(TimeTable, "TimeTable.csv", 32);

        // System.out.println("扩展完成~！！");
        ett = new ExpandTimeTable(instance);
        ett.Expand32(TimeTable, ClassRoomLeft, instance);
        TimeTable1 = ett.getTimeTable1();
        int[][][] ClassRoomLeft1 = ett.getClassRoomLeft1();

        // System.exit(0);
        // ***************************************************************
        // 继续安排once-off activities到TimeTable1和ClassRoomLeft1中

        // 先安排preceding为0的活动
        if (isDebug)
            System.out.println("------------------------------------------------------");
        // 遍历每个活动，按照固定的先后顺序进行遍历
        for (int coursei = 0; coursei < permOrderedACourse.size(); coursei++) {
            Course curCourse = permOrderedACourse.get(coursei);
            int RActivityi = curCourse.getId();
            if (isDebug)
                System.out.print("begin schedule OnceOff:" + RActivityi);
            OnceOff activity = instance.getOnceOff(RActivityi);
            int t_start = 0, t_end = 0;
            if (curCourse.getLow() == 1)
                t_start = 0;
            else
                t_start = 52 + 96 * (curCourse.getLow() - 2);

            if (curCourse.getTop() == 1)
                t_end = 51;
            else
                t_end = 52 + 96 * (curCourse.getTop() - 1) - 1;

            if (t_start > t_end) {
                //把该不能排排的后置的后置也设置成 下限大于上限
                //System.out.println("*** r" + RActivityi + " activity that have not scheduled1.!!");
                List<Integer> postConditionI = postConditionA.get(RActivityi);
                for (Integer posti : postConditionI) {// 对于该活动的每个后置活动
                    for (int ci = coursei + 1; ci < permOrderedACourse.size(); ci++) {
                        Course c = permOrderedACourse.get(ci);
                        if (c.getId() == posti) {
                            //这样他的后置就不能排了
                            c.setLow(c.getTop() + 1);
                        }
                    }
                }
                continue;
            }

            // 遍历合适的时间段，找到对应的cost来
            ArrayList<L> allCost = new ArrayList<L>();
            // 计算安排在可行的timeslot的cost
            for (int t = t_start; t <= t_end; t++) {
//				if (Math.floorMod(t, DateHandler.PERIODS_PER_DAY) + activity.getDuration()
//						- 1 >= DateHandler.PERIODS_PER_DAY)
//					continue;// 不能跨天安排
                if (t + activity.getDuration() >= Parameters.timeSlotsLength)
                    continue;
                double cost = cost(activity, t);
                // 用L记录这个Recurring安排在t时刻的cost
                L l = new L();
                l.setI(RActivityi);
                l.setJ(t);
//				l.setCost(cost);
                l.setCost(Double.parseDouble(String.format("%.4f", cost)));
                if (max_load_v > max_load)
                    l.setMax_load(max_load_v);// 设为较大的那个
                else
                    l.setMax_load(max_load);// 保持不变
                l.setBaseload(baseload_v);
                allCost.add(l);
            }
            allCost.sort(new Comparator<L>() {
                // 从小到大进行排序。
                @Override
                public int compare(L l1, L l2) {
                    // TODO Auto-generated method stub
                    double cost1 = l1.getCost();
                    double cost2 = l2.getCost();
                    if (cost1 < cost2)
                        return -1;
                    else if (cost1 > cost2)
                        return 1;
                    else if (l1.getJ() < l1.getJ())
                        return -1;
                    else if (l1.getJ() > l1.getJ())
                        return 1;
                    else
                        return 0;
                }

            });
            boolean isOK = false;// 在所有timeslot里面是否找到合适的地方安排？
            int bestk = 0;
            // 从小到大遍历allCost,找到合适的timeslot进行插入
            for (int li = 0; li < allCost.size(); li++) {// 对应的是96*4(384)->96*9-1(包含)
                int timeslot = allCost.get(li).getJ();

                // for(;timeslot<DateHandler.PERIODS_PER_DAY*DAYS;timeslot++) {
                // if(Math.floorMod(timeslot,DateHandler.PERIODS_PER_DAY)+instance.getOnceOff(RActivityi).getDuration()-1>=DateHandler.PERIODS_PER_DAY)
                // continue;//不能跨天安排
                // 先看一栋楼的安排
                int nRooms = instance.getOnceOff(RActivityi).getNumRooms();
                Room roomType = instance.getOnceOff(RActivityi).getRoomSize();
                assert (nRooms < 10);
                // 如果一栋楼可以安排下，则优先一栋楼。如果不可以，则找多栋楼的组合。
                ArrayList<int[]> buildIDs = new ArrayList<int[]>();// 那栋楼，多少个房间。
                // boolean isMixed = true;
                boolean isFeasibleST = false;
                int nRoomsRemaining = nRooms;
                for (int buildi = 0; buildi < nBuilding; buildi++) {

                    // boolean isFeasibleST = false;
                    int nlargeRoomLeft = Integer.MAX_VALUE;// 这个duration期间这栋楼最多可用的大小房间数
                    int nsmallRoomLeft = Integer.MAX_VALUE;
                    // boolean isOccupied = false;
                    for (int t = 0; t < instance.getOnceOff(RActivityi).getDuration(); t++) {
                        // if(TimeTable1[buildi][timeslot+t]!=null) {
                        //// isOccupied=true;
                        // break;//如果被占了，则这个开始时间不合适，退出继续找下一个。
                        // }
                        if (ClassRoomLeft1[timeslot + t][buildi][1] < nlargeRoomLeft)
                            nlargeRoomLeft = ClassRoomLeft1[timeslot + t][buildi][1];
                        if (ClassRoomLeft1[timeslot + t][buildi][0] < nsmallRoomLeft)
                            nsmallRoomLeft = ClassRoomLeft1[timeslot + t][buildi][0];
                    }
                    // if(isOccupied)
                    // continue;
                    if (roomType.equals(Room.Large)) {
                        if (nRooms <= nlargeRoomLeft) {
                            // 需要大房间，且这个时间timeslot的这栋楼buildi有这么多的大房间，那么就安排在这栋楼buildi。
                            buildIDs.clear();
                            int[] temp = {buildi, nRooms};
                            buildIDs.add(temp);
                            isFeasibleST = true;
                            break;
                        } else {
                            // 一栋楼装不下
                            if (nRoomsRemaining > 0 && nlargeRoomLeft > 0) {
                                // 如果剩余需要的房间数>0
                                int nOccupiedRoom = Math.min(nRoomsRemaining, nlargeRoomLeft);// 实际占用的房间数，（需要1，有2），（需要3，有1）
                                int[] temp = {buildi, nOccupiedRoom};// 把剩下的房间都占用
                                buildIDs.add(temp);
                                nRoomsRemaining = nRoomsRemaining - nOccupiedRoom;// 减掉加入的房间数
                                assert (nRoomsRemaining >= 0);
                            }
                        }
                    } else if (roomType.equals(Room.Small)) {
                        if (nRooms <= nsmallRoomLeft) {
                            // 需要小房间，且这个时间timeslot的这栋楼buildi有这么多的小房间，那么就安排在这里。
                            buildIDs.clear();
                            int[] temp = {buildi, nRooms};
                            buildIDs.add(temp);
                            isFeasibleST = true;
                            break;
                        } else {
                            // 一栋楼装不下
                            if (nRoomsRemaining > 0 && nsmallRoomLeft > 0) {
                                // 如果剩余需要的房间数>0
                                int nOccupiedRoom = Math.min(nRoomsRemaining, nsmallRoomLeft);// 实际占用的房间数，（需要1，有2），（需要3，有1）
                                int[] temp = {buildi, nOccupiedRoom};// 把剩下的房间都占用
                                buildIDs.add(temp);
                                nRoomsRemaining = nRoomsRemaining - nOccupiedRoom;// 减掉加入的房间数
                                assert (nRoomsRemaining >= 0);
                            }
                        }
                    } else {
                        System.err.print("not possible!!!!");
                    }
                }
                if (nRoomsRemaining == 0 || isFeasibleST) {

                    //判断这个活动的后置是否有可行区间！！！！

                    if (ACourseBestK.get(coursei) != bestk) {
                        bestk++;
                        continue;
                    }
                    // 如果在这个时间段能安排下，则安排。如果不能则下一个时间段。
                    for (int i = 0; i < buildIDs.size(); i++) {
                        int[] temp = buildIDs.get(i);

                        // 这栋楼的剩余房间更新
                        for (int t = 0; t < instance.getOnceOff(RActivityi).getDuration(); t++) {
                            // assert(TimeTable1[temp[0]][timeslot+t]==null);//确保不会覆盖别的活动。
                            if (TimeTable1[temp[0]][timeslot + t] == null)
                                TimeTable1[temp[0]][timeslot + t] = "a" + RActivityi + "(" + temp[1] + ")";// 安排在哪栋楼
                            else
                                TimeTable1[temp[0]][timeslot + t] += "|a" + RActivityi + "(" + temp[1] + ")";// 安排在哪栋楼
                            // TimeTableRooms[temp[0]][timeslot+t] = temp[1];
                            if (roomType.equals(Room.Small)) {
                                ClassRoomLeft1[timeslot + t][temp[0]][0] -= temp[1];
                                assert (ClassRoomLeft1[timeslot + t][temp[0]][0] >= 0);
                            } else {
                                ClassRoomLeft1[timeslot + t][temp[0]][1] -= temp[1];
                                assert (ClassRoomLeft1[timeslot + t][temp[0]][1] >= 0);
                            }
                        }
                    }
                    // TimeTable2csv(TimeTable);
                    isOK = true;
                    // nScheduled = nScheduled+1;
                    // isScheduled[RActivityi] = Math.floorDiv(timeslot,
                    // DateHandler.PERIODS_PER_DAY)+1;//安排在一个月的第几天
                    // 如果安排了这个活动，则看下安排在第几天，然后将它的所有后置活动的最小的范围检测是否需要缩小。
                    // 如果安排了这个活动，检查它安排在星期几，然后检测所有它的后置活动的范围看是否需要修正。
                    int scheduledDay = chronicsHandler.getDay(timeslot);//Math.floorDiv(timeslot, DateHandler.PERIODS_PER_DAY) + 1;// 安排在一个月的第几天
                    if (isDebug)
                        System.out.println(" in day:" + scheduledDay);
                    List<Integer> postConditionI = postConditionA.get(RActivityi);
                    assert (postConditionA.size() == instance.getAllRecurring().size());
                    for (Integer posti : postConditionI) {// 对于该活动的每个后置活动
                        for (int ci = coursei + 1; ci < permOrderedACourse.size(); ci++) {
                            Course c = permOrderedACourse.get(ci);
                            if (c.getId() == posti) {
                                if (scheduledDay + 1 > c.getLow()) {
//                                	int origin_scope = (c.getTop()-c.getLow()+1)*96-1;
                                    c.setLow(scheduledDay + 1);
//                                    int new_scope = (c.getTop()-c.getLow()+1)*96-1;
                                    assert (c.getLow() <= c.getTop());
                                    if (isDebug)
                                        System.out.println("a" + c.getId() + " change to [" + c.getLow() + ","
                                                + c.getTop() + "]!!!!!!!!!!!");

                                    //重新设置所选择的位置。
//                                    if(ACourseBestK.get(ci)>new_scope) {
//                                    	int new_bestK = (int)Math.floor((double)ACourseBestK.get(ci)/(double)origin_scope*(double)new_scope);
//                                    	ACourseBestK.set(ci, new_bestK);
//                                    }
                                }
                            }
                        }
                    }
                    assert (RActivityi == instance.getOnceOff(RActivityi).getID());
                    // ScheduledID.add(RActivityi);

                    // 重新设置max_load
                    max_load = allCost.get(li).getMax_load();
                    loadAfterTabling.clear();
                    for (Double load : allCost.get(li).getBaseload())
                        loadAfterTabling.add(load);
                    break;
                }
            }
            if (!isOK) {
                //把该活动的后置活动的下限设置比上线大
                //System.out.println("*** r" + RActivityi + " activity that have not scheduled1.!!");
                List<Integer> postConditionI = postConditionA.get(RActivityi);
                for (Integer posti : postConditionI) {// 对于该活动的每个后置活动
                    for (int ci = coursei + 1; ci < permOrderedACourse.size(); ci++) {
                        Course c = permOrderedACourse.get(ci);
                        if (c.getId() == posti) {
                            //这样他的后置就不能排了
                            c.setLow(c.getTop() + 1);
                        }
                    }
                }
            }
        }

        schedule = Schedule.getSchedulefromTTandB(instance, TimeTable1, batterySchedule);
        ChronicsScheduleChecker chronicsScheduleChecker = new ChronicsScheduleChecker(chronicsHandler, schedule);
        double score = chronicsScheduleChecker.getObjective();
        return score;
    }

    public double evaluate(String[][] TimeTable) {

//        FileUtils.output(instance, TimeTable, batterySchedule, "schedule" + who + ".txt");
//        schedule = Schedule.parseSchedule(new File("schedule" + who + ".txt"), instance);
        schedule = Schedule.getSchedulefromTTandB(instance, TimeTable, batterySchedule);
        ChronicsScheduleChecker chronicsScheduleChecker = new ChronicsScheduleChecker(chronicsHandler, schedule);
        double score = chronicsScheduleChecker.getObjective();
        return score;
    }

    public double evaluate(String[][] TimeTable, int[][] batterySchedule) {

//        FileUtils.output(instance, TimeTable, batterySchedule, "schedule" + who + ".txt");
//        schedule = Schedule.parseSchedule(new File("schedule" + who + ".txt"), instance);
        schedule = Schedule.getSchedulefromTTandB(instance, TimeTable, batterySchedule);
        ChronicsScheduleChecker chronicsScheduleChecker = new ChronicsScheduleChecker(chronicsHandler, schedule);
        double score = chronicsScheduleChecker.getObjective();
        return score;
    }

    /**
     * 从TimeTable1指定开始和结束位置查找第一次出现的活动标识，返回标识
     * 0<=start<=end<=2075
     * 没找到返回-1
     *
     * @param timeTable1 安排好的课表
     * @param activity   查找的活动标识（比如：r0,r1,a2等等）
     * @param start      开始位置（包括）
     * @param end        结束位置（包括）
     * @return
     */
    public static int getPosition(String[][] timeTable1, String activity, int start, int end) {
//	    if (start <= end && start >= 0 && end <= 2075) {

        for (int j = start; j <= end; j++) {
            for (int i = 0; i < timeTable1.length; i++) {
                if (timeTable1[i][j] == null)
                    continue;
                String s = timeTable1[i][j];
                // 使用|分割，转义字符
                String[] splits = s.split("\\|");
                for (String t : splits) {
                    // 判断是否是和要找的活动相等
                    if (t.indexOf(activity) == 0 && t.charAt(activity.length()) == '(') {
                        return j;
                    }
                }
            }
        }
//	    }
        return -1;
    }

    static void preProcess(Instance instance) {
        // int nBuilding = instance.getAllBuildings().size();
        // 五天的课表
        List<List<Integer>> allCourseSchedule = new ArrayList<>();
        ArrayList<ArrayList<Integer>> RActivityPrecedenceInfo = new ArrayList<ArrayList<Integer>>();

        int nRecurring = instance.getAllRecurring().size();// 经常性活动的数量
        for (int i = 0; i < nRecurring; i++) {
            ArrayList<Integer> temp = new ArrayList<Integer>();
            temp.add(instance.getRecurring(i).getNumPreceding());// 先插入这个活动的前置活动的数量。
            for (int j = 0; j < instance.getRecurring(i).getNumPreceding(); j++) {// 再插入所有前置活动的id
                temp.add(instance.getRecurring(i).getPreceding().get(j).getID());
            }
            RActivityPrecedenceInfo.add(temp);
        }
        int[] isScheduled = new int[nRecurring];// 是否安排，如果安排了安排在星期几？
        // 存后置活动
        postConditionR = new ArrayList<>();
        for (int i = 0; i < nRecurring; i++) {
            postConditionR.add(new ArrayList<>());
        }
        int nScheduled = 0;// 已经安排的活动数量
        int day = 1;

        while (nScheduled < nRecurring) {
            boolean hasP0 = false;// 是否有precedence为0的课
            ArrayList<Integer> ScheduledID = new ArrayList<Integer>();
            // 一天的课表
            List<Integer> oneCourseSchedule = new ArrayList<>();
            for (int RActivityi = 0; RActivityi < nRecurring; RActivityi++) {
                if (isScheduled[RActivityi] > 0)
                    continue;
                int nPreceding = instance.getRecurring(RActivityi).getNumPreceding();// 这个活动
                if (RActivityPrecedenceInfo.get(RActivityi).size() - 1 == 0) {// 前置活动中没有未安排的活动，
                    hasP0 = true;
                    oneCourseSchedule.add(RActivityi);
                    if (isDebug)
                        System.out.print("r" + RActivityi + "(" + day + "),");
                    nScheduled = nScheduled + 1;
                    isScheduled[RActivityi] = 1;

                    assert (RActivityi == instance.getRecurring(RActivityi).getID());
                    ScheduledID.add(RActivityi);

                }
            }

            if (!hasP0) {
                System.err.println("没有preceding为0！！！");
                System.exit(0);
            }
            day = day + 1;
            allCourseSchedule.add(oneCourseSchedule);
            if (isDebug)
                System.out.println();
            // 删除已经安排的活动的preceding
            for (int RActivityi = 0; RActivityi < nRecurring; RActivityi++) {// isScheduled[RActivityi]==0&&
                int nP = RActivityPrecedenceInfo.get(RActivityi).get(0);
                for (int j = 1; j < RActivityPrecedenceInfo.get(RActivityi).size(); j++) {

                    if (ScheduledID.contains(RActivityPrecedenceInfo.get(RActivityi).get(j))) {
                        int idx = ScheduledID.indexOf(RActivityPrecedenceInfo.get(RActivityi).get(j));
                        postConditionR.get(ScheduledID.get(idx)).add(RActivityi);
                        RActivityPrecedenceInfo.get(RActivityi).remove(j);
                        nP = nP - 1;
                        j = j - 1;
                    }
                }
                RActivityPrecedenceInfo.get(RActivityi).set(0, nP);
            }
        } // while

        co = new CourseOrdering();
        co.setRCourse(Parameters.WORKINGDAY, postConditionR, allCourseSchedule);
        // 根据OrderedRCourse每个范围的个数，比如[1,1]的个数，[1,2]的个数，等
        // co.getNumSameScope(OrderedRCourse);
        // System.exit(0);
        // ***************************************************************
        // 继续安排once-off activities到TimeTable1和ClassRoomLeft1中

        // 先安排preceding为0的活动

        // System.out.print(instance.getAllRecurring().get(0).getNumPreceding());

        // 存后置活动
        postConditionA = new ArrayList<>();
        for (int i = 0; i < nRecurring; i++) {
            postConditionA.add(new ArrayList<>());
        }
        // 31天的课表
        allCourseSchedule = new ArrayList<>();
        nScheduled = 0;// 已经安排的活动数量
        day = 1;
        ArrayList<ArrayList<Integer>> AActivityPrecedenceInfo = new ArrayList<ArrayList<Integer>>();
        int nOnceOff = instance.getAllOnceOff().size();// 一次性活动的数量
        for (int i = 0; i < nOnceOff; i++) {
            ArrayList<Integer> temp = new ArrayList<Integer>();
            temp.add(instance.getOnceOff(i).getNumPreceding());// 先插入这个活动的前置活动的数量。
            for (int j = 0; j < instance.getOnceOff(i).getNumPreceding(); j++) {// 再插入所有前置活动的id
                temp.add(instance.getOnceOff(i).getPreceding().get(j).getID());
            }
            AActivityPrecedenceInfo.add(temp);
        }
        isScheduled = new int[nOnceOff];// 是否安排，如果安排了安排在哪一天？
        // System.out.println(isScheduled[0]);
        while (nScheduled < nOnceOff) {
            boolean hasP0 = false;// 是否有precedence为0的课
            // 一天的课表
            List<Integer> oneCourseSchedule = new ArrayList<>();
            ArrayList<Integer> ScheduledID = new ArrayList<Integer>();// 记录这一轮安排了哪些活动
            for (int RActivityi = 0; RActivityi < nOnceOff; RActivityi++) {
                if (isScheduled[RActivityi] > 0)
                    continue;// 如果这个活动安排了，那么就不再安排了。
                int nPreceding = instance.getAllOnceOff().get(RActivityi).getNumPreceding();// 这个活动
                if (AActivityPrecedenceInfo.get(RActivityi).size() - 1 == 0) {// 前置活动中没有未安排的活动，
                    hasP0 = true;
                    oneCourseSchedule.add(RActivityi);
                    if (isDebug)
                        System.out.print("a" + RActivityi + "(" + day + "),");
                    // 安排这个活动:安排在哪几栋楼的哪个时间。
                    nScheduled = nScheduled + 1;
                    isScheduled[RActivityi] = 1;// 安排在一个月的第几天

                    assert (RActivityi == instance.getOnceOff(RActivityi).getID());
                    ScheduledID.add(RActivityi);

                }
            }
            day = day + 1;
            allCourseSchedule.add(oneCourseSchedule);
            if (isDebug)
                System.out.println();
            // TimeTable2csv(TimeTable, "TimeTable.csv");
            // System.exit(0);
            if (!hasP0) {
                System.err.println("没有preceding为0！！！");

            }

            // 删除已经安排的活动的preceding
            for (int RActivityi = 0; RActivityi < nOnceOff; RActivityi++) {// isScheduled[RActivityi]==0&&
                int nP = AActivityPrecedenceInfo.get(RActivityi).get(0);
                for (int j = 1; j < AActivityPrecedenceInfo.get(RActivityi).size(); j++) {

                    if (ScheduledID.contains(AActivityPrecedenceInfo.get(RActivityi).get(j))) {
                        int idx = ScheduledID.indexOf(AActivityPrecedenceInfo.get(RActivityi).get(j));
                        postConditionA.get(ScheduledID.get(idx)).add(RActivityi);
                        AActivityPrecedenceInfo.get(RActivityi).remove(j);
                        nP = nP - 1;
                        j = j - 1;
                    }
                }
                AActivityPrecedenceInfo.get(RActivityi).set(0, nP);
            }
        } // while

        // OrderedACourse = co.getCourseScope(DAYS, postConditionA, allCourseSchedule);
        co.setACourse(Parameters.DAYS, postConditionA, allCourseSchedule);


    }

    /**
     * 计算某个活动安排在从startIn2796开始的timeslots里面的cost
     *
     * @param activity
     * @param startIn2796
     * @return
     */
    private double cost(Recurring activity, int startIn2796) {
        double cost = 0.0;
        // Determine period during the day.
        int period = chronicsHandler.getPeriod(startIn2796);
        //int period = date.getPeriod(startIn2796);
//		double overMaxLoad = 0.0;
        max_load_v = max_load;
        baseload_v = new ArrayList<Double>();
        // make a copy of baseload
        for (Double load : loadAfterTabling)
            baseload_v.add(load);
        for (int day : chronicsHandler.getAllDays(startIn2796)) {
            //for (int day : date.getAllDays(startIn2796)) {
            // Compute start time on this day.
            //int start = day * DateHandler.PERIODS_PER_DAY + period;
            int start = day * ChronicsHandler.PERIODS_PER_DAY + period - 11 * 4;
            for (int time = 0; time < activity.getDuration(); time++) {
                double load = activity.getNumRooms() * activity.getLoadkW();
                cost += 0.001d * load * 900d / 3600d * prices.get(start + time);// prices[start + time]*
                if (loadAfterTabling.get(start + time) + load > max_load_v) {
                    max_load_v = loadAfterTabling.get(start + time) + load;
                }
                // 改变baseload_v
                baseload_v.set(start + time, loadAfterTabling.get(start + time) + load);
            }
        }
//		if (max_load_v > max_load)
//			overMaxLoad = max_load_v - max_load;
        cost += 0.005 * (max_load_v * max_load_v - max_load * max_load);// 如果超过最大负载，加一个大的惩罚项。
        // cost += 0.005*(max_load_v*max_load_v);
        // cost += 1000.0*overMaxLoad;
        return cost;
    }

    private double cost(OnceOff activity, int startIn2796) {
        double cost = 0.0;
        // Determine period during the day.
        // int period = DateHandler.getPeriod(startIn2796);
//		double overMaxLoad = 0.0;
        max_load_v = max_load;
        baseload_v = new ArrayList<Double>();
        // make a copy of baseload
        for (Double load : loadAfterTabling)
            baseload_v.add(load);
        // for (int day : date.getAllDays(startIn2796)) {
        // // Compute start time on this day.
        // int start = day * DateHandler.PERIODS_PER_DAY + period;

        for (int time = 0; time < activity.getDuration(); time++) {
            double load = activity.getNumRooms() * activity.getLoadkW();
            cost += 0.001d * load * 900d / 3600d * prices.get(startIn2796 + time);// prices[start + time]*
            if (loadAfterTabling.get(startIn2796 + time) + load > max_load_v) {
                max_load_v = loadAfterTabling.get(startIn2796 + time) + load;
            }
            // 改变baseload_v
            baseload_v.set(startIn2796 + time, loadAfterTabling.get(startIn2796 + time) + load);
        }
        // }
//		if (max_load_v > max_load)
//			overMaxLoad = max_load_v - max_load;
        cost += 0.005 * (max_load_v * max_load_v - max_load * max_load);// 如果超过最大负载，加一个大的惩罚项。
        // cost += 0.005*(max_load_v*max_load_v);
        // cost += 1000.0*overMaxLoad;
        // 对于once-off activity还需要考虑其价值
        // 判断是否在工作时间段
        double value = activity.getValue();
        //boolean isWorkingHours = date.isWorkingHours(startIn2796, activity.getDuration());
        boolean isWorkingHours = chronicsHandler.isWorkingHours(startIn2796, activity.getDuration());
        if (!isWorkingHours)
            value = value - activity.getOutOfOfficePenalty();
        // else
        // System.out.println("once off activity can be scheduled in working
        // hour!!!!!!");

        cost = cost - value;
        return cost;
    }


    /**
     * 递归修改后置活动的范围
     * 注意！！！:调用该方法前，事先保存好permOrderedRCourse，该方法会修改permOrderedRCourse的内容，如果返回false的话，无法复原
     *
     * @param permOrderedRCourse
     * @param coursei            permOrderedRCourse中安排的第i个课程
     * @param RActivityi         该活动的id
     * @param weekday            该活动安排在第几天
     * @return
     */
    public static boolean checkPostCondition(List<Course> permOrderedRCourse, int coursei, int RActivityi, int weekday) {

        List<Integer> postConditionI = postConditionR.get(RActivityi);
        for (int i = 0; i < postConditionI.size(); i++) {
            Integer posti = postConditionI.get(i);
            // 从coursei前面的课程已安排好，从coursei后面的课程中找
            for (int j = coursei; j < permOrderedRCourse.size(); j++) {
                Course c = permOrderedRCourse.get(j);
                //课表范围内找到和后置posti相等的
                if (c.getId() == posti) {
                    if (weekday + 1 > c.getLow()) {
                        c.setLow(weekday + 1);
                        if (c.getLow() > c.getTop()) {
                            return false;
                        }
                        if (i == postConditionI.size() - 1) {
                            return checkPostCondition(permOrderedRCourse, coursei, c.getId(), c.getLow());
                        } else {
                            boolean b = checkPostCondition(permOrderedRCourse, coursei, c.getId(), c.getLow());
                            if (!b) {
                                return false;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return true;
    }


    public ArrayList<Integer> getNewPermutation(List<Integer> bestPermutation, Schedule schedule) {

        List<RecurringSchedule> recurringSchedules = schedule.getRecurringSchedule();
        List<OnceOffSchedule> onceOffSchedules = schedule.getOnceOffSchedule();


        loadAfterTabling = new ArrayList<Double>();
        max_load = Double.MIN_VALUE;
        for (Double load : baseload) {
            loadAfterTabling.add(load);
            if (load > max_load)
                max_load = load;
        }
        int[] mapping = new int[32 * 5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 32; j++) {
                mapping[32 * i + j] = 88 + 96 * i + j;
            }
        }

        List<Course> permOrderedRCourse = new ArrayList<Course>();
        List<Integer> RCourseBestK = new ArrayList<Integer>();
        List<Course> permOrderedACourse = new ArrayList<Course>();
        List<Integer> ACourseBestK = new ArrayList<Integer>();

        ExpandTimeTable ett;// = new ExpandTimeTable();

        int idx = 0;

        for (Integer n : co.numSameScopeR) {
            for (int i = 0; i < n; i++) {
                int bestidx = bestPermutation.get(idx);
                permOrderedRCourse.add(new Course(co.getOrderedRCourse().get(bestidx)));
                RCourseBestK.add(bestPermutation.get(idx + 1));
                // permutation.add(bestidx);
                idx = idx + 2;
            }
        }
        assert (permOrderedRCourse.size() == instance.getAllRecurring().size());

        for (Integer n : co.numSameScopeA) {
            for (int i = 0; i < n; i++) {
                int bestidx = bestPermutation.get(idx);
                permOrderedACourse.add(new Course(co.getOrderedACourse().get(bestidx)));
                ACourseBestK.add(bestPermutation.get(idx + 1));
                idx = idx + 2;
            }
        }
        assert (permOrderedACourse.size() == instance.getAllOnceOff().size());

        int[][][] ClassRoomLeft = new int[8 * 4 * Parameters.WORKINGDAY][nBuilding][2];
        for (int i = 0; i < nBuilding; i++) {
            int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
            int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
            for (int j = 0; j < 8 * 4 * Parameters.WORKINGDAY; j++) {
                ClassRoomLeft[j][i][0] = nsmall;
                ClassRoomLeft[j][i][1] = nlarge;
            }
        }

        String[][] TimeTable = new String[nBuilding][8 * 4 * Parameters.WORKINGDAY];

        for (int coursei = 0; coursei < permOrderedRCourse.size(); coursei++) {
            Course curCourse = permOrderedRCourse.get(coursei);

            int RActivityi = curCourse.getId();
            if (isDebug)
                System.out.print("begin schedule Recurring:" + RActivityi);
            Recurring activity = instance.getRecurring(RActivityi);

            int t_start = 32 * (curCourse.getLow() - 1), t_end = 32 * curCourse.getTop() - 1;
            ArrayList<L> allCost = new ArrayList<L>();
            // 璁＄畻瀹夋帓鍦ㄥ彲琛岀殑timeslot鐨刢ost
            for (int t = t_start; t <= t_end; t++) {
                if (Math.floorMod(t, 32) + activity.getDuration() - 1 >= 32)
                    continue;// 涓嶈兘璺ㄥぉ瀹夋帓
                double cost = cost(activity, mapping[t]);// t鏃堕棿娈靛搴斾竴涓湀鐨勬椂闂存
                // 鐢↙璁板綍杩欎釜Recurring瀹夋帓鍦╰鏃跺埢鐨刢ost
                L l = new L();
                l.setI(RActivityi);
                l.setJ(t);
                l.setCost(Double.parseDouble(String.format("%.4f", cost)));// 濡傛灉activityi瀹夋帓鍦╰锛宑ost鏄灏戯紵

                if (max_load_v > max_load)
                    l.setMax_load(max_load_v);// 璁句负杈冨ぇ鐨勯偅涓�
                else
                    l.setMax_load(max_load);// 淇濇寔涓嶅彉
                l.setBaseload(baseload_v);// 鏀瑰彉
                allCost.add(l);// 瀹夋帓鍦ㄦ椂闂存t锛�
            }
            allCost.sort(new Comparator<L>() {
                @Override
                public int compare(L l1, L l2) {
                    // TODO Auto-generated method stub
                    double cost1 = l1.getCost();
                    double cost2 = l2.getCost();
                    if (cost1 < cost2)
                        return -1;
                    else if (cost1 > cost2)
                        return 1;
                    else if (l1.getJ() < l1.getJ())
                        return -1;
                    else if (l1.getJ() > l1.getJ())
                        return 1;
                    else
                        return 0;
                }

            });
            boolean isOK = false;
            int bestk = 0;
            for (int li = 0; li < allCost.size(); li++) {
                int timeslot = allCost.get(li).getJ();
                if (Math.floorMod(timeslot, 32) + activity.getDuration() - 1 >= 32)
                    continue;
                int nRooms = instance.getRecurring(RActivityi).getNumRooms();
                Room roomType = instance.getRecurring(RActivityi).getRoomSize();
                assert (nRooms < 10);
                ArrayList<int[]> buildIDs = new ArrayList<int[]>();
                int nRoomsRemaining = nRooms;
                boolean isFeasibleST = false;
                for (int buildi = 0; buildi < nBuilding; buildi++) {
                    int nlargeRoomLeft = Integer.MAX_VALUE;
                    int nsmallRoomLeft = Integer.MAX_VALUE;
                    for (int t = 0; t < instance.getRecurring(RActivityi).getDuration(); t++) {
                        if (ClassRoomLeft[timeslot + t][buildi][1] < nlargeRoomLeft)
                            nlargeRoomLeft = ClassRoomLeft[timeslot + t][buildi][1];
                        if (ClassRoomLeft[timeslot + t][buildi][0] < nsmallRoomLeft)
                            nsmallRoomLeft = ClassRoomLeft[timeslot + t][buildi][0];
                    }
                    if (roomType.equals(Room.Large)) {
                        if (nRooms <= nlargeRoomLeft) {
                            buildIDs.clear();
                            int[] temp = {buildi, nRooms};
                            buildIDs.add(temp);
                            isFeasibleST = true;
                            break;
                        } else {
                            if (nRoomsRemaining > 0 && nlargeRoomLeft > 0) {
                                int nOccupiedRoom = Math.min(nRoomsRemaining, nlargeRoomLeft);
                                int[] temp = {buildi, nOccupiedRoom};
                                buildIDs.add(temp);
                                nRoomsRemaining = nRoomsRemaining - nOccupiedRoom;
                                assert (nRoomsRemaining >= 0);
                            }
                        }
                    } else if (roomType.equals(Room.Small)) {
                        if (nRooms <= nsmallRoomLeft) {
                            buildIDs.clear();
                            int[] temp = {buildi, nRooms};
                            buildIDs.add(temp);
                            isFeasibleST = true;
                            break;
                        } else {
                            if (nRoomsRemaining > 0 && nsmallRoomLeft > 0) {
                                int nOccupiedRoom = Math.min(nRoomsRemaining, nsmallRoomLeft);
                                int[] temp = {buildi, nOccupiedRoom};
                                buildIDs.add(temp);
                                nRoomsRemaining = nRoomsRemaining - nOccupiedRoom;
                                assert (nRoomsRemaining >= 0);
                            }
                        }
                    } else {
                        System.err.print("not possible!!!!");
                    }
                }
                if (nRoomsRemaining == 0 || isFeasibleST) {
                    int weekday = Math.floorDiv(timeslot, 32) + 1;//1,2,3,4,5
                    RecurringSchedule r_oneSchedule = recurringSchedules.get(RActivityi);
                    int before_day = (weekday - 1) > 0 ? (weekday - 1) : 0;
                    int start = 52 + before_day * 24 * 4 + 9 * 4 + timeslot % 32;
                    if (start != r_oneSchedule.getStartTime()) {
                        bestk++;
                        continue;
                    }
                    RCourseBestK.set(coursei, bestk);
                    for (int buildi = 0; buildi < buildIDs.size(); buildi++) {
                        int[] temp = buildIDs.get(buildi);
                        for (int t = 0; t < instance.getRecurring(RActivityi).getDuration(); t++) {
                            if (TimeTable[temp[0]][timeslot + t] == null)
                                TimeTable[temp[0]][timeslot + t] = "r" + RActivityi + "(" + temp[1] + ")";
                            else
                                TimeTable[temp[0]][timeslot + t] += "|r" + RActivityi + "(" + temp[1] + ")";// 瀹夋帓鍦ㄥ摢鏍嬫ゼ
                            if (roomType.equals(Room.Small)) {
                                ClassRoomLeft[timeslot + t][temp[0]][0] -= temp[1];
                                assert (ClassRoomLeft[timeslot + t][temp[0]][0] >= 0);
                            } else {
                                ClassRoomLeft[timeslot + t][temp[0]][1] -= temp[1];
                                assert (ClassRoomLeft[timeslot + t][temp[0]][1] >= 0);
                            }
                        }
                    }
                    isOK = true;
                    assert (RActivityi == instance.getRecurring(RActivityi).getID());
                    permOrderedRCourse.get(coursei).setLow(weekday);
                    List<Integer> postConditionI = postConditionR.get(RActivityi);
                    assert (postConditionR.size() == instance.getAllRecurring().size());
                    for (Integer posti : postConditionI) {
                        for (int ci = coursei + 1; ci < permOrderedRCourse.size(); ci++) {
                            Course c = permOrderedRCourse.get(ci);
                            if (c.getId() == posti) {
                                if (weekday + 1 > c.getLow()) {
                                    c.setLow(weekday + 1);
                                    assert (c.getLow() <= c.getTop());
                                    if (isDebug)
                                        System.out.println("r" + c.getId() + " change to [" + c.getLow() + ","
                                                + c.getTop() + "]!!!!!!!!!!!");
                                }
                            }
                        }
                    }
                    max_load = allCost.get(li).getMax_load();
                    loadAfterTabling.clear();
                    for (Double load : allCost.get(li).getBaseload())
                        loadAfterTabling.add(load);
                    break;
                } else {

                }
            }
            if (!isOK) {
                throw new RuntimeException("重复性活动复原不了!!!!");
            }
        }
        ett = new ExpandTimeTable(instance);
        ett.Expand32(TimeTable, ClassRoomLeft, instance);
        TimeTable1 = ett.getTimeTable1();
        int[][][] ClassRoomLeft1 = ett.getClassRoomLeft1();
        if (isDebug)
            System.out.println("------------------------------------------------------");
        for (int coursei = 0; coursei < permOrderedACourse.size(); coursei++) {
            Course curCourse = permOrderedACourse.get(coursei);
            int RActivityi = curCourse.getId();
            OnceOffSchedule once_oneSchedule = null;
            for (OnceOffSchedule temp : onceOffSchedules) {
                OnceOff activity1 = temp.getActivity();
                if (activity1.getID() == RActivityi) {
                    once_oneSchedule = temp;
                    break;
                }
            }
            if (once_oneSchedule == null) {
                ACourseBestK.set(coursei, Parameters.timeSlotsLength);
                //this activity is not scheduled.
                continue;
            }
            if (isDebug)
                System.out.print("begin schedule OnceOff:" + RActivityi);
            OnceOff activity = instance.getOnceOff(RActivityi);
            int t_start = 0, t_end = 0;
            if (curCourse.getLow() == 1)
                t_start = 0;
            else
                t_start = 52 + 96 * (curCourse.getLow() - 2);

            if (curCourse.getTop() == 1)
                t_end = 51;
            else
                t_end = 52 + 96 * (curCourse.getTop() - 1) - 1;
            ArrayList<L> allCost = new ArrayList<L>();
            for (int t = t_start; t <= t_end; t++) {
                if (t + activity.getDuration() >= Parameters.timeSlotsLength)
                    continue;
                double cost = cost(activity, t);
                L l = new L();
                l.setI(RActivityi);
                l.setJ(t);
                l.setCost(Double.parseDouble(String.format("%.4f", cost)));
                if (max_load_v > max_load)
                    l.setMax_load(max_load_v);// 璁句负杈冨ぇ鐨勯偅涓�
                else
                    l.setMax_load(max_load);// 淇濇寔涓嶅彉
                l.setBaseload(baseload_v);
                allCost.add(l);
            }
            allCost.sort(new Comparator<L>() {
                @Override
                public int compare(L l1, L l2) {
                    // TODO Auto-generated method stub
                    double cost1 = l1.getCost();
                    double cost2 = l2.getCost();
                    if (cost1 < cost2)
                        return -1;
                    else if (cost1 > cost2)
                        return 1;
                    else if (l1.getJ() < l1.getJ())
                        return -1;
                    else if (l1.getJ() > l1.getJ())
                        return 1;
                    else
                        return 0;
                }

            });
            int bestk = 0;
            boolean isOK = false;
            for (int li = 0; li < allCost.size(); li++) {
                int timeslot = allCost.get(li).getJ();
                int nRooms = instance.getOnceOff(RActivityi).getNumRooms();
                Room roomType = instance.getOnceOff(RActivityi).getRoomSize();
                assert (nRooms < 10);
                ArrayList<int[]> buildIDs = new ArrayList<int[]>();
                // boolean isMixed = true;
                boolean isFeasibleST = false;
                int nRoomsRemaining = nRooms;
                for (int buildi = 0; buildi < nBuilding; buildi++) {
                    int nlargeRoomLeft = Integer.MAX_VALUE;
                    int nsmallRoomLeft = Integer.MAX_VALUE;
                    for (int t = 0; t < instance.getOnceOff(RActivityi).getDuration(); t++) {
                        if (ClassRoomLeft1[timeslot + t][buildi][1] < nlargeRoomLeft)
                            nlargeRoomLeft = ClassRoomLeft1[timeslot + t][buildi][1];
                        if (ClassRoomLeft1[timeslot + t][buildi][0] < nsmallRoomLeft)
                            nsmallRoomLeft = ClassRoomLeft1[timeslot + t][buildi][0];
                    }
                    if (roomType.equals(Room.Large)) {
                        if (nRooms <= nlargeRoomLeft) {
                            buildIDs.clear();
                            int[] temp = {buildi, nRooms};
                            buildIDs.add(temp);
                            isFeasibleST = true;
                            break;
                        } else {
                            if (nRoomsRemaining > 0 && nlargeRoomLeft > 0) {
                                int nOccupiedRoom = Math.min(nRoomsRemaining, nlargeRoomLeft);
                                int[] temp = {buildi, nOccupiedRoom};
                                buildIDs.add(temp);
                                nRoomsRemaining = nRoomsRemaining - nOccupiedRoom;
                                assert (nRoomsRemaining >= 0);
                            }
                        }
                    } else if (roomType.equals(Room.Small)) {
                        if (nRooms <= nsmallRoomLeft) {
                            buildIDs.clear();
                            int[] temp = {buildi, nRooms};
                            buildIDs.add(temp);
                            isFeasibleST = true;
                            break;
                        } else {
                            if (nRoomsRemaining > 0 && nsmallRoomLeft > 0) {
                                int nOccupiedRoom = Math.min(nRoomsRemaining, nsmallRoomLeft);
                                int[] temp = {buildi, nOccupiedRoom};
                                buildIDs.add(temp);
                                nRoomsRemaining = nRoomsRemaining - nOccupiedRoom;
                                assert (nRoomsRemaining >= 0);
                            }
                        }
                    } else {
                        System.err.print("not possible!!!!");
                    }
                }
                if (nRoomsRemaining == 0 || isFeasibleST) {
                    if (timeslot != once_oneSchedule.getStartTime()) {
                        bestk++;
                        continue;
                    }
                    ACourseBestK.set(coursei, bestk);
                    isOK = true;
                    for (int i = 0; i < buildIDs.size(); i++) {
                        int[] temp = buildIDs.get(i);
                        for (int t = 0; t < instance.getOnceOff(RActivityi).getDuration(); t++) {
                            if (TimeTable1[temp[0]][timeslot + t] == null)
                                TimeTable1[temp[0]][timeslot + t] = "a" + RActivityi + "(" + temp[1] + ")";// 瀹夋帓鍦ㄥ摢鏍嬫ゼ
                            else
                                TimeTable1[temp[0]][timeslot + t] += "|a" + RActivityi + "(" + temp[1] + ")";// 瀹夋帓鍦ㄥ摢鏍嬫ゼ
                            if (roomType.equals(Room.Small)) {
                                ClassRoomLeft1[timeslot + t][temp[0]][0] -= temp[1];
                                assert (ClassRoomLeft1[timeslot + t][temp[0]][0] >= 0);
                            } else {
                                ClassRoomLeft1[timeslot + t][temp[0]][1] -= temp[1];
                                assert (ClassRoomLeft1[timeslot + t][temp[0]][1] >= 0);
                            }
                        }
                    }
                    int scheduledDay = chronicsHandler.getDay(timeslot);
                    if (isDebug)
                        System.out.println(" in day:" + scheduledDay);
                    List<Integer> postConditionI = postConditionA.get(RActivityi);
                    assert (postConditionA.size() == instance.getAllRecurring().size());
                    for (Integer posti : postConditionI) {// 瀵逛簬璇ユ椿鍔ㄧ殑姣忎釜鍚庣疆娲诲姩
                        for (int ci = coursei + 1; ci < permOrderedACourse.size(); ci++) {
                            Course c = permOrderedACourse.get(ci);
                            if (c.getId() == posti) {
                                if (scheduledDay + 1 > c.getLow()) {
                                    c.setLow(scheduledDay + 1);
                                    assert (c.getLow() <= c.getTop());
                                    if (isDebug)
                                        System.out.println("a" + c.getId() + " change to [" + c.getLow() + "," + c.getTop() + "]!!!!!!!!!!!");

                                }
                            }
                        }
                    }
                    assert (RActivityi == instance.getOnceOff(RActivityi).getID());
                    max_load = allCost.get(li).getMax_load();
                    loadAfterTabling.clear();
                    for (Double load : allCost.get(li).getBaseload())
                        loadAfterTabling.add(load);
                    break;
                }
            }
            if (!isOK) {
                throw new RuntimeException("*** a" + RActivityi + " 活动不能够复原.!!");
            }
        }
        int ans = 0;
        ArrayList<Integer> new_perm = new ArrayList<>();
        for (int i = 0; i < RCourseBestK.size(); i++) {
            new_perm.add(bestPermutation.get(ans));
            new_perm.add(RCourseBestK.get(i));
            ans += 2;
        }
        for (int i = 0; i < ACourseBestK.size(); i++) {
            new_perm.add(bestPermutation.get(ans));
            new_perm.add(ACourseBestK.get(i));
            ans += 2;
        }
        return new_perm;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    /**
     * baseLoad_tansfer是从文件从读取的某个baseload赋值给Evaluation1，使用这种方法修改他的默认baseload
     *
     * @param baseLoad_tansfer
     * @see ChronicsScheduleChecker#updateBaseLoad(List) 默认为baseload1文件夹下面的baseload
     */
    public void setBaseload(List<Double> baseLoad_tansfer) {
        baseload = baseLoad_tansfer;
        max_load = 0.0;
        double load;
        boolean valid = true;
        for (int batteryi = 0; batteryi < batterySchedule.length; batteryi++) {
            Battery battery = instance.getAllBatteries().get(batteryi);
            double state = battery.getCapacityKWh();//
            for (int time = 0; time < Parameters.timeSlotsLength; time++) {
                // 1. 计算这个解对应的能量和load负载
                // -1:DisCharge
                // 0:hold
                // 1:Charge
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
                load = battery.getLoadKW(action);// 这个解会给baseload在这个时间点time添加多少负载。
                // 改变baseload
                baseload.set(time, baseload.get(time) + load);
                if (baseload.get(time) > max_load) {
                    max_load = baseload.get(time);
                }
                // 2. 检查是否满足约束条件
                state += battery.getEnergyKWh(action, 15d / 60d);// 15分钟后的电池能量
                // System.out.println(state);
                if (state < 0 || state > battery.getCapacityKWh()) {
                    valid = false;
                }
            }
        }
    }

    public List<Double> getBaseload() {
        return baseload;
    }

    public double evaluate(Schedule schedule) {
        ChronicsScheduleChecker chronicsScheduleChecker = new ChronicsScheduleChecker(chronicsHandler, schedule);
        //修改baseload为evaluation对象的baseload
        chronicsScheduleChecker.setBaseLoad(baseload);
        double score = chronicsScheduleChecker.getObjective();
        return score;
    }


    /**
     * 判断一个活动是否可以安排在timeslot开始的时间段。
     * 如果可以，则返回安排在哪几栋楼。
     *
     * @param activity
     * @param timeslot
     * @param ClassRoomLeft
     * @return
     */
    private List<int[]> isFeasibleTime(Activity activity, int timeslot, int[][][] ClassRoomLeft) {
        int nRooms = activity.getNumRooms();
        Room roomType = activity.getRoomSize();
        ArrayList<int[]> buildIDs = new ArrayList<int[]>();// 那栋楼，多少个房间。
        // boolean isMixed = true;
        int nRoomsRemaining = nRooms;
        //检查每栋楼这段时间最多可用房间数。
        int[] nRoomLeft = new int[nBuilding];//每栋楼最多可用的房间数，那么可以用哪几栋，或者哪一栋楼来安排这个活动呢？有哪些可能性？
        //我需要nRooms个房间，先找一栋楼就可以安排下的。再找2栋楼安排的，再找3栋楼安排的。
        for (int buildi = 0; buildi < nBuilding; buildi++) {
            nRoomLeft[buildi] = Integer.MAX_VALUE;// 这个duration期间这栋楼最多可用的大小房间数
            for (int t = 0; t < activity.getDuration(); t++) {
                if (activity.getRoomSize().equals(Room.Large) && ClassRoomLeft[timeslot + t][buildi][1] < nRoomLeft[buildi])
                    nRoomLeft[buildi] = ClassRoomLeft[timeslot + t][buildi][1];
                if (activity.getRoomSize().equals(Room.Small) && ClassRoomLeft[timeslot + t][buildi][0] < nRoomLeft[buildi])
                    nRoomLeft[buildi] = ClassRoomLeft[timeslot + t][buildi][0];
            }
        }
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < nRoomLeft.length; i++) {
            int i1 = nRoomLeft[i];
            for (int j = 0; j < i1; j++) {
                all.add(i);
            }
        }

        List<List<Integer>> combination_results = new ArrayList<>();
        HashMap<String, Integer> check = new HashMap<>();
        combinationSelect(all, nRooms, combination_results,check);

        return null;

    }


    /**
     * 组合选择（从列表中选择n个组合）
     * @param dataList 待选列表
     * @param n 选择个数
     */
    public static void combinationSelect(List<Integer> dataList, int n , List<List<Integer>> results,HashMap<String, Integer> check) {
        combinationSelect(dataList, 0, new int[n], 0,results,check);
    }

    /**
     * 组合选择
     * @param dataList 待选列表
     * @param dataIndex 待选开始索引
     * @param resultList 前面（resultIndex-1）个的组合结果
     * @param resultIndex 选择索引，从0开始
     */
    private static void combinationSelect(List<Integer> dataList, int dataIndex, int[] resultList, int resultIndex,List<List<Integer>> results,HashMap<String, Integer> check) {
        int resultLen = resultList.length;
        int resultCount = resultIndex + 1;
        if (resultCount > resultLen) { // 全部选择完时，输出组合结果
            for (int i = 0; i < resultList.length; i++) {
                System.out.print(resultList[i]+" ");
            }
            System.out.println();
            List<Integer> one_result = new ArrayList<>();
            String str = "";
            for (int i = 0; i < resultList.length; i++) {
                str += resultList[i];
                one_result.add(resultList[i]);
            }
            if( !check.containsKey(str) ){
                check.put(str,1);
                results.add(one_result);
            }
            return;
        }
        // 递归选择下一个
        for (int i = dataIndex; i < dataList.size() + resultCount - resultLen; i++) {
            resultList[resultIndex] = dataList.get(i);
            combinationSelect(dataList, i + 1, resultList, resultIndex + 1,results,check);
        }
    }

}
