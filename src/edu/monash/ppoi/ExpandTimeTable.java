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
		ClassRoomLeft1 = new int[Parameters.timeSlotsLength][nBuilding][2];// 10�·�31��
	}
	
	
	public void Expand32(String[][] TimeTable, int[][][] ClassRoomLeft, Instance instance) {
		int nBuilding = instance.getAllBuildings().size();
		// ***************************************************************
		// ��TimeTable��ClassRoomLeft��չ��32*31
		// ���潫TimeTable��һ��������չ��һ����
		
		//��ʼΪ0
		int current_timeslot = 0;
		// ��һ�����յĽ�ֹtimeslot
		int weekend = DateHandler.PERIODS_PER_DAY * 1 -1 - 11*4;//51
		//�ȴ����һ�죨�����죩
		while (current_timeslot <= weekend){
			for (int i = 0; i < nBuilding; i++) {
				int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
				int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
				ClassRoomLeft1[current_timeslot][i][0] = nsmall;
				ClassRoomLeft1[current_timeslot][i][1] = nlarge;
			}
			current_timeslot++;
		}
		//����11�µ�1��2��3��4����
		for (int weeki = 1; weeki <= 4; weeki++) {
			for (int week = 1; week <= 5; week++) {// ����һ��������
				// ��0�㵽9��
				for (int j = 0; j < 9 * 4; j++) {
					// ��idx�Ǹ�timeslot��6*2���󿽱���ClassRoomLeft1
					for (int i = 0; i < 6; i++) {
						int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
						int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
						ClassRoomLeft1[current_timeslot][i][0] = nsmall;
						ClassRoomLeft1[current_timeslot][i][1] = nlarge;
					}
					current_timeslot++;
				}
				// ÿ�����ڵ�ǰ���죨����һ�������壩��ClassRoomLeft������ClassRoomLeft1��
				for (int timeslot = 32 * (week - 1); timeslot <= 32 * week - 1; timeslot++) {// ��9�㵽17��
					for (int buildi = 0; buildi <= 5; buildi++) {
						ClassRoomLeft1[current_timeslot][buildi][0] = ClassRoomLeft[timeslot][buildi][0];// ʣ��small
						// Room�ĸ���
						ClassRoomLeft1[current_timeslot][buildi][1] = ClassRoomLeft[timeslot][buildi][1];// ʣ��large
						// Room�ĸ���
						TimeTable1[buildi][current_timeslot] = TimeTable[buildi][timeslot];
					}
					current_timeslot++;
				}
				// ��17�㵽24��
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
			// ÿ�����ڵ���ĩ
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

		//���⴦��������죨202-11 00��00-23��45 ��һ��
		for (int week = 1; week <= 1; week++) {
			// ��0�㵽9��
			for (int j = 0; j < 9 * 4; j++) {
				// ��idx�Ǹ�timeslot��6*2���󿽱���ClassRoomLeft1
				for (int i = 0; i < 6; i++) {
					int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][i][0] = nsmall;
					ClassRoomLeft1[current_timeslot][i][1] = nlarge;
				}
				current_timeslot++;
			}
			// 9�㵽17��
			for (int timeslot = 32 * (week - 1); timeslot <= 32 * week - 1; timeslot++) {
				for (int buildi = 0; buildi <= 5; buildi++) {
					int nlarge = instance.getAllBuildings().get(buildi).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(buildi).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][buildi][0] = nsmall;
					ClassRoomLeft1[current_timeslot][buildi][1] = nlarge;
					
					
					
//					ClassRoomLeft1[current_timeslot][buildi][0] = ClassRoomLeft[timeslot][buildi][0];// ʣ��small
					// Room�ĸ���
//					ClassRoomLeft1[current_timeslot][buildi][1] = ClassRoomLeft[timeslot][buildi][1];// ʣ��large
					// Room�ĸ���
//					TimeTable1[buildi][current_timeslot] = TimeTable[buildi][timeslot];
				}
				current_timeslot++;
			}
			// ��17�㵽24��
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
		//���һ�죨2020-12-1 00��00����2020-12-1 10��45 �ܶ���
		for (int week = 2; week <= 2; week++) {
			// ��0�㵽9��
			for (int j = 0; j < 9 * 4; j++) {
				// ��idx�Ǹ�timeslot��6*2���󿽱���ClassRoomLeft1
				for (int i = 0; i < 6; i++) {
					int nlarge = instance.getAllBuildings().get(i).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(i).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][i][0] = nsmall;
					ClassRoomLeft1[current_timeslot][i][1] = nlarge;
				}
				current_timeslot++;
			}
			// 9�㵽10:45��
			for (int timeslot = 32 * (week - 1); timeslot <= 32 * (week - 1) + 2*4 - 1; timeslot++) {
				for (int buildi = 0; buildi <= 5; buildi++) {
					int nlarge = instance.getAllBuildings().get(buildi).getNumRooms(Room.Large);
					int nsmall = instance.getAllBuildings().get(buildi).getNumRooms(Room.Small);
					ClassRoomLeft1[current_timeslot][buildi][0] = nsmall;
					ClassRoomLeft1[current_timeslot][buildi][1] = nlarge;
//					ClassRoomLeft1[current_timeslot][buildi][0] = ClassRoomLeft[timeslot][buildi][0];// ʣ��small
					// Room�ĸ���
//					ClassRoomLeft1[current_timeslot][buildi][1] = ClassRoomLeft[timeslot][buildi][1];// ʣ��large
					// Room�ĸ���
//					TimeTable1[buildi][current_timeslot] = TimeTable[buildi][timeslot];
				}
				current_timeslot++;
			}
		}

		if( current_timeslot != ChronicsHandler.PERIODS_PER_DAY * 30 ){
			throw new RuntimeException("ExpandTimeTable������չ����������������������");
		}

	}
}
