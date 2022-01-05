package edu.monash.ppoi.entity;

import java.util.*;

public class CourseOrdering {

	public List<Integer> numSameScopeR;
	public List<Integer> rangeSameScopeR;
	public List<Integer> numSameScopeA;
	public List<Integer> rangeSameScopeA;
	private List<Course> OrderedRCourse;
	private List<Course> OrderedACourse;
	private boolean isDebug = true;
    // �õ�ÿ���γ̷�Χ
    public void setRCourse(int max_value, List<List<Integer>> postCondition, List<List<Integer>> allCourseSchedule) {
//        List<Recurring> allRecurring = instance.getAllRecurring();
        //�γ�ID��Ӧ�����Χ
        Map<Integer, Integer> courseIdMaxScope = new HashMap<>();
        // ��ʼ��������Ŀγ����޺����޶�Ϊ5
        List<List<Course>> allCourseScope = new ArrayList<>();
        List<Course> oneDayCourseScope = new ArrayList<>();
        OrderedRCourse = new ArrayList<>();
        int L = allCourseSchedule.size();//L = 5
        for (int i = 0; i < L; i++) {
            allCourseScope.add(new ArrayList<>());
        }
        //����������
        for (int i = 0; i < allCourseSchedule.get(allCourseSchedule.size() - 1).size(); i++) {
            //�γ�ID
            int courseId = allCourseSchedule.get(L - 1).get(i);
            courseIdMaxScope.put(courseId, max_value);
            Course course = new Course(courseId, L, max_value);
            oneDayCourseScope.add(course);
        }
        // ��allCourseScope���������Ŀγ̷�Χ
        allCourseScope.set(L - 1, oneDayCourseScope);

        // ����������ǰ��ʼ����
        for (int i = allCourseSchedule.size() - 2; i >= 0; i--) {
            oneDayCourseScope = new ArrayList<>();
            for (int j = 0; j < allCourseSchedule.get(i).size(); j++) {
                Course course = new Course();
                // ��ǰ�ID
                int courseId = allCourseSchedule.get(i).get(j);
//                if(courseId==7) {
//                	System.out.print("test");
//                }
                course.setId(courseId);
                // ������������Ϊi+1
                course.setLow(i + 1);
                // �ÿγ̵ĺ��ûIDS
                List<Integer> backIDs = postCondition.get(courseId);
                // �ÿγ�û�к��û
                if (backIDs.size() == 0) {
                    //�������Ϊ5��L=5
                    course.setTop(L);
                } else {
                    // �����ʼֵ�������óɴ��ڵ���5����
                    int mixn = max_value;
                    for (int k = 0; k < backIDs.size(); k++) {
                        //�õ����û�����Χ
                        int maxScope = courseIdMaxScope.get(backIDs.get(k));
                        if (maxScope == 0) {
                            throw new RuntimeException("����������");
                        }
                        // ȡ�ú��û���ұ߽���С����һ��
                        if (mixn > maxScope) {
                            mixn = maxScope;
                        }
                    }
                    course.setTop(mixn - 1);
                }
                //��map����˴λ�����Χ���Ա���ѭ�����õ�
                courseIdMaxScope.put(course.getId(), course.getTop());
                //��ӵ�һ��İ�����
                oneDayCourseScope.add(course);
            }
            allCourseScope.set(i, oneDayCourseScope);
        }
        //�����Ȱ���Χ��С�����ٰ�id����
        for (int i = 0; i < allCourseScope.size(); i++) {
            Collections.sort(allCourseScope.get(i), new Comparator<Course>() {
                @Override
                public int compare(Course o1, Course o2) {
                    int a1 = o1.getTop() - o1.getLow();
                    int a2 = o2.getTop() - o2.getLow();
//                    if (a1 != a2) {
//                        return a1 - a2;
//                    } else {
//                        return o1.getId() - o2.getId();
//                    }
                    if(a1<a2)
                    	return -1;
                    else if(a1>a2)
                    	return 1;
                    else
                    	if(o1.getId()<o2.getId())
                    		return -1;
                    	else if(o1.getId()>o2.getId())
                    		return 1;
                    	else
                    		return 0;
                }
            });
        }
        /*******************�鿴allCourseScope******************************/
        if(isDebug)
		for (int i = 0; i < allCourseScope.size(); i++) {
			for (int j = 0; j < allCourseScope.get(i).size(); j++) {
				Course c = allCourseScope.get(i).get(j);
				System.out.print("r" + c.getId() + "[" + c.getLow() + "," + c.getTop() + "],");
			}
			System.out.println();
		}
		/*************************************************/
        
        numSameScopeR = new ArrayList<Integer>();
        rangeSameScopeR = new ArrayList<Integer>();
        Course lastCourse = allCourseScope.get(0).get(0);
        int thisNum = 0;
        for (int i = 0; i < allCourseScope.size(); i++) {
			// List<Course> aPost= new ArrayList<Course>();
			for (int j = 0; j < allCourseScope.get(i).size(); j++) {
				Course thisCourse = allCourseScope.get(i).get(j);
				if(thisCourse.getTop()==lastCourse.getTop()) {
					thisNum ++;
				}else {
					numSameScopeR.add(thisNum);
					rangeSameScopeR.add(lastCourse.getTop()-lastCourse.getLow()+1);
					thisNum=1;
				}
				OrderedRCourse.add(new Course(thisCourse));
				lastCourse = thisCourse;
			}
			numSameScopeR.add(thisNum);
			rangeSameScopeR.add(lastCourse.getTop()-lastCourse.getLow()+1);
			thisNum=0;
			if(i+1<allCourseScope.size())
			lastCourse = allCourseScope.get(i+1).get(0);
		}
        /***********************************************/
        if(isDebug) {
        for(int i=0;i<numSameScopeR.size();i++) {System.out.print(numSameScopeR.get(i)+"("+rangeSameScopeR.get(i)+")"+",");}
        System.out.println();
        }
//        return allCourseScope1;
    }
 // �õ�ÿ���γ̷�Χ
    public void setACourse(int max_value, List<List<Integer>> postCondition, List<List<Integer>> allCourseSchedule) {
//        List<Recurring> allRecurring = instance.getAllRecurring();
        //�γ�ID��Ӧ�����Χ
        Map<Integer, Integer> courseIdMaxScope = new HashMap<>();
        // ��ʼ��������Ŀγ����޺����޶�Ϊ5
        List<List<Course>> allCourseScope = new ArrayList<>();
        List<Course> oneDayCourseScope = new ArrayList<>();
        OrderedACourse = new ArrayList<>();
        int L = allCourseSchedule.size();//L = 5
        for (int i = 0; i < L; i++) {
            allCourseScope.add(new ArrayList<>());
        }
        //����������
        for (int i = 0; i < allCourseSchedule.get(allCourseSchedule.size() - 1).size(); i++) {
            //�γ�ID
            int courseId = allCourseSchedule.get(L - 1).get(i);
            courseIdMaxScope.put(courseId, max_value);
            Course course = new Course(courseId, L, max_value);
            oneDayCourseScope.add(course);
        }
        // ��allCourseScope���������Ŀγ̷�Χ
        allCourseScope.set(L - 1, oneDayCourseScope);

        // ����������ǰ��ʼ����
        for (int i = allCourseSchedule.size() - 2; i >= 0; i--) {
            oneDayCourseScope = new ArrayList<>();
            for (int j = 0; j < allCourseSchedule.get(i).size(); j++) {
                Course course = new Course();
                // ��ǰ�ID
                int courseId = allCourseSchedule.get(i).get(j);
//                if(courseId==7) {
//                	System.out.print("test");
//                }
                course.setId(courseId);
                // ������������Ϊi+1
                course.setLow(i + 1);
                // �ÿγ̵ĺ��ûIDS
                List<Integer> backIDs = postCondition.get(courseId);
                // �ÿγ�û�к��û
                if (backIDs.size() == 0) {
                    //�������Ϊ5��L=5
                    course.setTop(L);
                } else {
                    // �����ʼֵ�������óɴ��ڵ���5����
                    int mixn = max_value;
                    for (int k = 0; k < backIDs.size(); k++) {
                        //�õ����û�����Χ
                        int maxScope = courseIdMaxScope.get(backIDs.get(k));
                        if (maxScope == 0) {
                            throw new RuntimeException("����������");
                        }
                        // ȡ�ú��û���ұ߽���С����һ��
                        if (mixn > maxScope) {
                            mixn = maxScope;
                        }
                    }
                    course.setTop(mixn - 1);
                }
                //��map����˴λ�����Χ���Ա���ѭ�����õ�
                courseIdMaxScope.put(course.getId(), course.getTop());
                //��ӵ�һ��İ�����
                oneDayCourseScope.add(course);
            }
            allCourseScope.set(i, oneDayCourseScope);
        }
        //�����Ȱ���Χ��С�����ٰ�id����
        for (int i = 0; i < allCourseScope.size(); i++) {
            Collections.sort(allCourseScope.get(i), new Comparator<Course>() {
                @Override
                public int compare(Course o1, Course o2) {
                    int a1 = o1.getTop() - o1.getLow();
                    int a2 = o2.getTop() - o2.getLow();
//                    if (a1 != a2) {
//                        return a1 - a2;
//                    } else {
//                        return o1.getId() - o2.getId();
//                    }
                    if(a1<a2)
                    	return -1;
                    else if(a1>a2)
                    	return 1;
                    else
                    	if(o1.getId()<o2.getId())
                    		return -1;
                    	else if(o1.getId()>o2.getId())
                    		return 1;
                    	else
                    		return 0;
                }
            });
        }
        /*******************�鿴allCourseScope******************************/
        if(isDebug)
		for (int i = 0; i < allCourseScope.size(); i++) {
			for (int j = 0; j < allCourseScope.get(i).size(); j++) {
				Course c = allCourseScope.get(i).get(j);
				System.out.print("a" + c.getId() + "[" + c.getLow() + "," + c.getTop() + "],");
			}
			System.out.println();
		}
		/*************************************************/
        
        numSameScopeA = new ArrayList<Integer>();
        rangeSameScopeA = new ArrayList<Integer>();
        Course lastCourse = allCourseScope.get(0).get(0);
        int thisNum = 0;
        for (int i = 0; i < allCourseScope.size(); i++) {
			// List<Course> aPost= new ArrayList<Course>();
			for (int j = 0; j < allCourseScope.get(i).size(); j++) {
				Course thisCourse = allCourseScope.get(i).get(j);
				if(thisCourse.getTop()==lastCourse.getTop()) {
					thisNum ++;
				}else {
					numSameScopeA.add(thisNum);
					rangeSameScopeA.add(lastCourse.getTop()-lastCourse.getLow()+1);
					thisNum=1;
				}
				OrderedACourse.add(new Course(thisCourse));
				lastCourse = thisCourse;
			}
			numSameScopeA.add(thisNum);
			rangeSameScopeA.add(lastCourse.getTop()-lastCourse.getLow()+1);
			thisNum=0;
			if(i+1<allCourseScope.size())
			lastCourse = allCourseScope.get(i+1).get(0);
		}
        
        /***********************************************/
        if(isDebug) {
        	for(int i=0;i<numSameScopeA.size();i++) {System.out.print(numSameScopeA.get(i)+"("+rangeSameScopeA.get(i)+")"+",");}
        System.out.println();
        }
//        return allCourseScope1;
    }
    
 // ������õĿγ̽���ϴ�ƣ�ϴ�ƹ���ͬһ��ͬһ����Χ�ڵĿγ��������
 	public static List<List<Course>> courseScope_Shuffle(List<List<Course>> allCourseScope) {
 	    //����ÿһ��
 	    for (int i = 0; i < allCourseScope.size(); i++) {
 	        // �Ը�һ������пγ�ϴ�ƣ��ù��̿������ΪҲ��ͬһ��ͬһ����Χ�ڵĿγ̽�����������򣩣�
 	        Collections.shuffle(allCourseScope.get(i));
 	        //�Ը���γ����򣺰���Χ��С���������൱��ֻ��ͬһ��ͬһ����Χ�ڵĿγ̽������������
 	        allCourseScope.get(i).sort((o1, o2) -> {
 	            int a1 = o1.getTop() - o1.getLow();
 	            int a2 = o2.getTop() - o2.getLow();
 	            return a1 - a2;
 	        });
 	    }
 	    return allCourseScope;
 	}


	public List<Course> getOrderedRCourse() {
		return OrderedRCourse;
	}


//	public void setOrderedRCourse(List<Course> orderedRCourse) {
//		OrderedRCourse = orderedRCourse;
//	}


	public List<Course> getOrderedACourse() {
		return OrderedACourse;
	}



 	
}
