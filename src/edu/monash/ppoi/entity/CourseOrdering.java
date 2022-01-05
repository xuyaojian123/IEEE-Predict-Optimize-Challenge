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
    // 得到每个课程范围
    public void setRCourse(int max_value, List<List<Integer>> postCondition, List<List<Integer>> allCourseSchedule) {
//        List<Recurring> allRecurring = instance.getAllRecurring();
        //课程ID对应的最大范围
        Map<Integer, Integer> courseIdMaxScope = new HashMap<>();
        // 初始化星期五的课程上限和下限都为5
        List<List<Course>> allCourseScope = new ArrayList<>();
        List<Course> oneDayCourseScope = new ArrayList<>();
        OrderedRCourse = new ArrayList<>();
        int L = allCourseSchedule.size();//L = 5
        for (int i = 0; i < L; i++) {
            allCourseScope.add(new ArrayList<>());
        }
        //遍历星期五
        for (int i = 0; i < allCourseSchedule.get(allCourseSchedule.size() - 1).size(); i++) {
            //课程ID
            int courseId = allCourseSchedule.get(L - 1).get(i);
            courseIdMaxScope.put(courseId, max_value);
            Course course = new Course(courseId, L, max_value);
            oneDayCourseScope.add(course);
        }
        // 在allCourseScope存放星期五的课程范围
        allCourseScope.set(L - 1, oneDayCourseScope);

        // 从星期四往前开始遍历
        for (int i = allCourseSchedule.size() - 2; i >= 0; i--) {
            oneDayCourseScope = new ArrayList<>();
            for (int j = 0; j < allCourseSchedule.get(i).size(); j++) {
                Course course = new Course();
                // 当前活动ID
                int courseId = allCourseSchedule.get(i).get(j);
//                if(courseId==7) {
//                	System.out.print("test");
//                }
                course.setId(courseId);
                // 设置他的下线为i+1
                course.setLow(i + 1);
                // 该课程的后置活动IDS
                List<Integer> backIDs = postCondition.get(courseId);
                // 该课程没有后置活动
                if (backIDs.size() == 0) {
                    //设置最大为5，L=5
                    course.setTop(L);
                } else {
                    // 随机初始值，得设置成大于等于5的数
                    int mixn = max_value;
                    for (int k = 0; k < backIDs.size(); k++) {
                        //得到后置活动的最大范围
                        int maxScope = courseIdMaxScope.get(backIDs.get(k));
                        if (maxScope == 0) {
                            throw new RuntimeException("程序有问题");
                        }
                        // 取得后置活动中右边界最小的那一个
                        if (mixn > maxScope) {
                            mixn = maxScope;
                        }
                    }
                    course.setTop(mixn - 1);
                }
                //在map保存此次活动的最大范围，以便在循环中用到
                courseIdMaxScope.put(course.getId(), course.getTop());
                //添加到一天的安排中
                oneDayCourseScope.add(course);
            }
            allCourseScope.set(i, oneDayCourseScope);
        }
        //排序：先按范围大小排序，再按id排序
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
        /*******************查看allCourseScope******************************/
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
 // 得到每个课程范围
    public void setACourse(int max_value, List<List<Integer>> postCondition, List<List<Integer>> allCourseSchedule) {
//        List<Recurring> allRecurring = instance.getAllRecurring();
        //课程ID对应的最大范围
        Map<Integer, Integer> courseIdMaxScope = new HashMap<>();
        // 初始化星期五的课程上限和下限都为5
        List<List<Course>> allCourseScope = new ArrayList<>();
        List<Course> oneDayCourseScope = new ArrayList<>();
        OrderedACourse = new ArrayList<>();
        int L = allCourseSchedule.size();//L = 5
        for (int i = 0; i < L; i++) {
            allCourseScope.add(new ArrayList<>());
        }
        //遍历星期五
        for (int i = 0; i < allCourseSchedule.get(allCourseSchedule.size() - 1).size(); i++) {
            //课程ID
            int courseId = allCourseSchedule.get(L - 1).get(i);
            courseIdMaxScope.put(courseId, max_value);
            Course course = new Course(courseId, L, max_value);
            oneDayCourseScope.add(course);
        }
        // 在allCourseScope存放星期五的课程范围
        allCourseScope.set(L - 1, oneDayCourseScope);

        // 从星期四往前开始遍历
        for (int i = allCourseSchedule.size() - 2; i >= 0; i--) {
            oneDayCourseScope = new ArrayList<>();
            for (int j = 0; j < allCourseSchedule.get(i).size(); j++) {
                Course course = new Course();
                // 当前活动ID
                int courseId = allCourseSchedule.get(i).get(j);
//                if(courseId==7) {
//                	System.out.print("test");
//                }
                course.setId(courseId);
                // 设置他的下线为i+1
                course.setLow(i + 1);
                // 该课程的后置活动IDS
                List<Integer> backIDs = postCondition.get(courseId);
                // 该课程没有后置活动
                if (backIDs.size() == 0) {
                    //设置最大为5，L=5
                    course.setTop(L);
                } else {
                    // 随机初始值，得设置成大于等于5的数
                    int mixn = max_value;
                    for (int k = 0; k < backIDs.size(); k++) {
                        //得到后置活动的最大范围
                        int maxScope = courseIdMaxScope.get(backIDs.get(k));
                        if (maxScope == 0) {
                            throw new RuntimeException("程序有问题");
                        }
                        // 取得后置活动中右边界最小的那一个
                        if (mixn > maxScope) {
                            mixn = maxScope;
                        }
                    }
                    course.setTop(mixn - 1);
                }
                //在map保存此次活动的最大范围，以便在循环中用到
                courseIdMaxScope.put(course.getId(), course.getTop());
                //添加到一天的安排中
                oneDayCourseScope.add(course);
            }
            allCourseScope.set(i, oneDayCourseScope);
        }
        //排序：先按范围大小排序，再按id排序
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
        /*******************查看allCourseScope******************************/
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
    
 // 对排序好的课程进行洗牌（洗牌规则：同一天同一个范围内的课程随机排序）
 	public static List<List<Course>> courseScope_Shuffle(List<List<Course>> allCourseScope) {
 	    //遍历每一天
 	    for (int i = 0; i < allCourseScope.size(); i++) {
 	        // 对该一天的所有课程洗牌（该过程可以理解为也对同一天同一个范围内的课程进行了随机排序））
 	        Collections.shuffle(allCourseScope.get(i));
 	        //对该天课程排序：按范围从小到大排序（相当于只对同一天同一个范围内的课程进行了随机排序）
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
