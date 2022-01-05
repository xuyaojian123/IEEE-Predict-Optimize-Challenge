package edu.monash.ppoi.utils;


/**
 * Utilities methods to used by MOEA/D
 */
public class Utils {


    /**
     * 从TimeTable1指定开始和结束位置查找第一次出现的活动标识，返回标识
     * 0<=start<=end<=2075
     * 没找到返回-1
     * @param timeTable1 安排好的课表
     * @param activity   查找的活动标识（比如：r0,r1,a2等等）
     * @param start      开始位置（包括）
     * @param end        结束位置（包括）
     * @return
     */
    public static int getPosition(String[][] timeTable1, String activity, int start, int end) {
        if (start <= end && start >= 0 && end <= 2075) {
            for (int i = 0; i < timeTable1.length; i++) {
                for (int j = start; j <= end; j++) {

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
        }
        return -1;
    }

    public static double distVector(double[] vector1, double[] vector2) {
        int dim = vector1.length;
        double sum = 0;
        for (int n = 0; n < dim; n++) {
            sum += (vector1[n] - vector2[n]) * (vector1[n] - vector2[n]);
        }
        return Math.sqrt(sum);
    } // distVector

    public static void minFastSort(double x[], int idx[], int n, int m) {
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < n; j++) {
                if (x[i] > x[j]) {
                    double temp = x[i];
                    x[i] = x[j];
                    x[j] = temp;
                    int id = idx[i];
                    idx[i] = idx[j];
                    idx[j] = id;
                } // if
            }
        } // for

    } // minFastSort

    public static void randomPermutation(int[] perm, int size) {
        int[] index = new int[size];
        boolean[] flag = new boolean[size];

        for (int n = 0; n < size; n++) {
            index[n] = n;
            flag[n] = true;
        }

        int num = 0;
        while (num < size) {
            int start = PseudoRandom.randInt(0, size - 1);
            //int start = int(size*nd_uni(&rnd_uni_init));
            while (true) {
                if (flag[start]) {
                    perm[num] = index[start];
                    flag[start] = false;
                    num++;
                    break;
                }
                if (start == (size - 1)) {
                    start = 0;
                } else {
                    start++;
                }
            }
        } // while
    } // randomPermutation
}

