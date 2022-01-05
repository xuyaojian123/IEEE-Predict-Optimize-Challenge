package edu.monash.ppoi;

import edu.monash.ppoi.checker.ChronicsHandler;
import edu.monash.ppoi.checker.DateHandler;
import edu.monash.ppoi.constant.Parameters;
import edu.monash.ppoi.instance.Instance;
import edu.monash.ppoi.instance.Room;

public class ExpandTimeTable {
	private String[][] TimeTable1;
	private int[][][] ClassRoomLeft1;
	
	public String[][] getTimeTable1(){
		return TimeTable1;
	}
	public int[][][] getClassRoomLeft1(){
		return ClassRoomLeft1;
	}
	
	public ExpandTimeTable(Instance instance){
		int nBuilding = instance.getAllBuildings().size();
		TimeTable1 = new String[nBuilding][Parameters.timeSlotsLength];
		ClassRoomLeft1 = new int[Parameters.timeSlotsLength][nBuilding][2];// 10月份31天
	}
	
	
	public void Expand32(String[][] TimeTable, int[][][] ClassRoomLeft, Instance instance) {
		int nBuilding = instance.getAllBuildings().size();
		// ***************************************************************
		// 将TimeTable和ClassRoomLeft扩展成32*31
		// 下面将TimeTable从一个星期扩展到一个月
		
		//初始为0
		int current_timeslot = 0;
		// 第一天周日的截止timeslot
		int weekend = DateHandler.PERIODS_PER_DAY * 1 -1 - 11*4;//51
		//先处理第一天（星期天）
		while (current_timeslot <= weekend){
			for (int i = 0; i < nBuilding; i++) {
				int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
				int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
				ClassRoomLeft1[current_timeslot][i][0] = nsmall;
				ClassRoomLeft1[current_timeslot][i][1] = nlarge;
			}
			current_timeslot++;
		}
		//处理11月的1、2、3、4星期
		for (int weeki = 1; weeki <= 4; weeki++) {
			for (int week = 1; week <= 5; week++) {// 星期一到星期五
				// 从0点到9点
				for (int j = 0; j < 9 * 4; j++) {
					// 把idx那个timeslot的6*2矩阵拷贝到ClassRoomLeft1
					for (int i = 0; i < 6; i++) {
						int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
						int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
						ClassRoomLeft1[current_timeslot][i][0] = nsmall;
						ClassRoomLeft1[current_timeslot][i][1] = nlarge;
					}
					current_timeslot++;
				}
				// 每个星期的前五天（星期一到星期五）从ClassRoomLeft拷贝到ClassRoomLeft1中
				for (int timeslot = 32 * (week - 1); timeslot <= 32 * week - 1; timeslot++) {// 从9点到17点
					for (int buildi = 0; buildi <= 5; buildi++) {
						ClassRoomLeft1[current_timeslot][buildi][0] = ClassRoomLeft[timeslot][buildi][0];// 剩余small
						// Room的个数
						ClassRoomLeft1[current_timeslot][buildi][1] = ClassRoomLeft[timeslot][buildi][1];// 剩余large
						// Room的个数
						TimeTable1[buildi][current_timeslot] = TimeTable[buildi][timeslot];
					}
					current_timeslot++;
				}
				// 从17点到24点
				for (int j = 0; j < 7 * 4; j++) {
					for (int i = 0; i < 6; i++) {
						int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
						int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
						ClassRoomLeft1[current_timeslot][i][0] = nsmall;
						ClassRoomLeft1[current_timeslot][i][1] = nlarge;
					}
					current_timeslot++;
				}
			}
			// 每个星期的周末
			weekend = current_timeslot + DateHandler.PERIODS_PER_DAY * 2 - 1;
			while (current_timeslot <= weekend) {
				for (int i = 0; i < nBuilding; i++) {
					int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][i][0] = nsmall;
					ClassRoomLeft1[current_timeslot][i][1] = nlarge;
				}
				current_timeslot++;
			}
		}

		//特殊处理最后两天（202-11 00：00-23：45 周一）
		for (int week = 1; week <= 1; week++) {
			// 从0点到9点
			for (int j = 0; j < 9 * 4; j++) {
				// 把idx那个timeslot的6*2矩阵拷贝到ClassRoomLeft1
				for (int i = 0; i < 6; i++) {
					int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][i][0] = nsmall;
					ClassRoomLeft1[current_timeslot][i][1] = nlarge;
				}
				current_timeslot++;
			}
			// 9点到17点
			for (int timeslot = 32 * (week - 1); timeslot <= 32 * week - 1; timeslot++) {
				for (int buildi = 0; buildi <= 5; buildi++) {
					int nlarge = instance.getAllBuildings().get(buildi).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(buildi).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][buildi][0] = nsmall;
					ClassRoomLeft1[current_timeslot][buildi][1] = nlarge;
					
					
					
//					ClassRoomLeft1[current_timeslot][buildi][0] = ClassRoomLeft[timeslot][buildi][0];// 剩余small
					// Room的个数
//					ClassRoomLeft1[current_timeslot][buildi][1] = ClassRoomLeft[timeslot][buildi][1];// 剩余large
					// Room的个数
//					TimeTable1[buildi][current_timeslot] = TimeTable[buildi][timeslot];
				}
				current_timeslot++;
			}
			// 从17点到24点
			for (int j = 0; j < 7 * 4; j++) {
				for (int i = 0; i < 6; i++) {
					int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][i][0] = nsmall;
					ClassRoomLeft1[current_timeslot][i][1] = nlarge;
				}
				current_timeslot++;
			}
		}
		//最后一天（2020-12-1 00：00——2020-12-1 10：45 周二）
		for (int week = 2; week <= 2; week++) {
			// 从0点到9点
			for (int j = 0; j < 9 * 4; j++) {
				// 把idx那个timeslot的6*2矩阵拷贝到ClassRoomLeft1
				for (int i = 0; i < 6; i++) {
					int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][i][0] = nsmall;
					ClassRoomLeft1[current_timeslot][i][1] = nlarge;
				}
				current_timeslot++;
			}
			// 9点到10:45点
			for (int timeslot = 32 * (week - 1); timeslot <= 32 * (week - 1) + 2*4 - 1; timeslot++) {
				for (int buildi = 0; buildi <= 5; buildi++) {
					int nlarge = instance.getAllBuildings().get(buildi).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(buildi).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][buildi][0] = nsmall;
					ClassRoomLeft1[current_timeslot][buildi][1] = nlarge;
//					ClassRoomLeft1[current_timeslot][buildi][0] = ClassRoomLeft[timeslot][buildi][0];// 剩余small
					// Room的个数
//					ClassRoomLeft1[current_timeslot][buildi][1] = ClassRoomLeft[timeslot][buildi][1];// 剩余large
					// Room的个数
//					TimeTable1[buildi][current_timeslot] = TimeTable[buildi][timeslot];
				}
				current_timeslot++;
			}
		}

		if( current_timeslot != ChronicsHandler.PERIODS_PER_DAY * 30 ){
			throw new RuntimeException("ExpandTimeTable类中扩展出错啦！！！！！！！！");
		}

	}
}
