import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;


/**
 * @author Hppppppf
 * @date 2022/4/9 15:55
 * @description https://zte.hina.com/zte/network
 */
public class oldMain {


    public static Double v = 5.0;             //无人机飞行速度
    public static Double H = 10.0;            //无人机飞行高度
    public static Double dIntraOrbit = 90.0;  //轨道内无人机的距离
    public static Double dInterOrbit = 80.0;  //轨道间距
    public static Double d = 115.0;           //无人机之间最大通讯距离
    public static Double D = 70.0;            //无人机与地面基站之间的最大通讯距离
    public static Integer s = 10;             //信号量
    public static Integer c = 3;              //链路容量
    public static Integer numberOfTimesSent = (int) Math.ceil((double) s / c);
    public static Double lastTimeDelay = 0.0; //上一次转发信号的时延
    public static Integer aerialPlatformsMark = 6666;
    public static Double tf = 0.1;            //转发时延常数
    public static String path = "result.txt"; //保存路径
    static FileWriter fileWriter;
    static String output = "";

    static Double eps = 0.0;

    static {
        try {
            fileWriter = new FileWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //基站位置表
    static List<List<Double>> posList = new ArrayList<>();
    //高空平台位置表
    static List<List<Double>> aerialPlatforms = new ArrayList<>();

    public oldMain() throws IOException {
    }

    public static void main(String[] args) throws IOException {

        posList.add(Arrays.asList(45.73, 45.26, 0.0));
        posList.add(Arrays.asList(1200.0, 700.0, 0.0));
        posList.add(Arrays.asList(-940.0, 1100.0, 0.0));


        /*aerialPlatforms.add(Arrays.asList(-614.0, 1059.0, 24.0));
        aerialPlatforms.add(Arrays.asList(-943.0, 715.0, 12.0));
        aerialPlatforms.add(Arrays.asList(1073.0, 291.0, 37.0));
        aerialPlatforms.add(Arrays.asList(715.0, 129.0, 35.0));
        aerialPlatforms.add(Arrays.asList(186.0, 432.0, 21.0));
        aerialPlatforms.add(Arrays.asList(-923.0, 632.0, 37.0));
        aerialPlatforms.add(Arrays.asList(833.0, 187.0, 24.0));
        aerialPlatforms.add(Arrays.asList(-63.0, 363.0, 11.0));*/

        //开始转发时刻表
        List<Double> timeList = new ArrayList<>();
        timeList.addAll(Arrays.asList(0.0, 4.7, 16.4));


        Double DelayResult = 0.0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    if (j == k) continue;
                    String cc = "3";
                    Double temp = 0.0;
                    Double time = 0.0;
                    for (int Times = 0; Times < numberOfTimesSent; Times++) {
                        time = timeList.get(i);
                        if (lastTimeDelay < time)   lastTimeDelay = time;
                        temp += computeTotalDelay(posList.get(j), posList.get(k), lastTimeDelay);
                        temp = dataProcessing(temp);
                        //DelayResult += temp;
                        if (Times == numberOfTimesSent - 1) cc = "1";
                        String out = timeList.get(i) + "," + j + "," + k + "," + temp + "," + cc + "\n";
                        System.out.print(out);
                        String output_t = output.substring(0, output.length() - 1);
                        System.out.println(output_t);
                        fileWriter.append(out);
                        fileWriter.append(output_t + "\n");
                        fileWriter.flush();
                        output = "";
                        lastTimeDelay += temp;
                    }
                }
            }
        }
        //System.out.print("DelayResultL:" + DelayResult);
    }


    /**
     * 计算time时刻基站start到基站end的转发时延
     *
     * @param start 起点基站位置
     * @param end   重点点基站位置
     * @param time  当前时刻
     * @return 时延
     */
    public static Double computeTotalDelay(List<Double> start, List<Double> end, Double time) {

        Double delay = 0.0;
        //可用无人机位置
        List<List<Integer>> availableUAV = new ArrayList<>(computeAvailableUAVsforBase(time, D, start));
        //List<Integer> selectedUAV = new ArrayList<>(selectUAV2(availableUAV,start, end, time,computeDistance(start,end)));
        List<Integer> selectedUAV = new ArrayList<>(selectUAV(availableUAV, end, time));
        Double delayTemp = computeDelay(computePos(time, selectedUAV.get(0), selectedUAV.get(1)), start);
        delay += delayTemp;
        time += delayTemp;
        String out = "";
        out = "(" + dataProcessing(time) + "," + selectedUAV.get(0) + "," + selectedUAV.get(1) + "),";
        output += out;
        while (computeDistance(computePos(time + computeDelay(computePos(time, selectedUAV.get(0), selectedUAV.get(1)), end), selectedUAV.get(0), selectedUAV.get(1)), end) > D) {
            List<List<Integer>> availableUAV_temp = new ArrayList<>(computeAvailableUAVsforUAV(time, d, Arrays.asList(selectedUAV.get(0), selectedUAV.get(1))));
            //List<Integer> selectedUAV_temp = new ArrayList<>(selectUAV2(availableUAV_temp, start, end, time,computeDistance(computePos(time,selectedUAV.get(0),selectedUAV.get(1)),end)));
            List<Integer> selectedUAV_temp = new ArrayList<>(selectUAV(availableUAV_temp, end, time));
            List<Integer> preUAV = new ArrayList<>(selectedUAV);
            selectedUAV.clear();
            selectedUAV.addAll(selectedUAV_temp);
            Double delay_temp = computeDelay(computePos(time, selectedUAV.get(0), selectedUAV.get(1)), computePos(time, preUAV.get(0), preUAV.get(1)));
            delay += delay_temp;
            time += delay_temp;
            if (selectedUAV.get(0) == aerialPlatformsMark) {
                out = "(" + dataProcessing(time) + "," + selectedUAV.get(1) + "),";
            } else out = "(" + dataProcessing(time) + "," + selectedUAV.get(0) + "," + selectedUAV.get(1) + "),";
            output += out;
            //System.out.println(computeDistance(computePos(time, selectedUAV.get(0), selectedUAV.get(1)), end));
        }
        delay += computeDelay(computePos(time, selectedUAV.get(0), selectedUAV.get(1)), end);
        return delay;
    }

    /**
     * 为当前无人机计算覆盖范围内的无人机
     *
     * @param time     当前时刻
     * @param distance 覆盖范围
     * @param startUAV 当前无人机序号
     * @return 无人机序号表
     */
    public static List<List<Integer>> computeAvailableUAVsforUAV(Double time, Double distance, List<Integer> startUAV) {
        List<List<Integer>> ret = new ArrayList<>();
        List<Double> posOfStartUAV;
        if (startUAV.get(0) == aerialPlatformsMark) {
            posOfStartUAV = aerialPlatforms.get(startUAV.get(1));
            return computeAvailableUAVsforBase(time, distance, posOfStartUAV);
        } else {
            posOfStartUAV = computePos(time, startUAV.get(0), startUAV.get(1));
            //加入高空平台
            for (int i = 0; i < aerialPlatforms.size(); i++) {
                if (i != startUAV.get(1) && computeDistance(aerialPlatforms.get(i), posOfStartUAV) < distance) {
                    ret.add(Arrays.asList(aerialPlatformsMark, i));
                }
            }
        }


        //求m,n范围
        int mMax = 0, nMax = 0, mMin = 0, nMin = 0;

        int m = startUAV.get(0);
        int n = startUAV.get(1);

        mMin = m - 1 - (int) (dIntraOrbit / d);
        mMax = m + 1 + (int) (dIntraOrbit / d);
        nMin = n - 1 - (int) (dInterOrbit / d);
        nMax = n + 1 + (int) (dInterOrbit / d);


        //遍历所有可能的无人机
        for (int i = mMin - 1; i <= mMax; i++) {
            for (int j = nMin - 1; j <= nMax; j++) {
                Double delayTemp = computeDelay(computePos(time, i, j), posOfStartUAV);
                if (computeDistance(computePos(time + delayTemp, i, j), posOfStartUAV) < distance) {
                    ret.add(Arrays.asList(i, j));
                }
            }
        }
        return ret;
    }

    /**
     * 为基站计算覆盖范围内的无人机
     *
     * @param time     当前时刻
     * @param distance 覆盖范围
     * @param pos      基站/无人机位置
     * @return 返回在pos处的基站/无人机覆盖范围distance内的无人机编号表
     */
    public static List<List<Integer>> computeAvailableUAVsforBase(Double time, Double distance, List<Double> pos) {
        List<List<Integer>> ret = new ArrayList<>();

        //加入高空平台
        for (int i = 0; i < aerialPlatforms.size(); i++) {
            if (computeDistance(aerialPlatforms.get(i), pos) < distance) {
                ret.add(Arrays.asList(aerialPlatformsMark, i));
            }
        }

        //求m,n范围
        int mMax = 0, nMax = 0, mMin = 0, nMin = 0;

        int m = (int) ((pos.get(0) - v * time) / dIntraOrbit);
        int n = (int) (pos.get(1) / dInterOrbit);

        mMin = m - 1 - (int) (dIntraOrbit / D);
        mMax = m + 1 + (int) (dIntraOrbit / D);
        nMin = n - 1 - (int) (dInterOrbit / D);
        nMax = n + 1 + (int) (dInterOrbit / D);

        //遍历所有可能的无人机
        for (int i = mMin - 1; i <= mMax; i++) {
            for (int j = nMin - 1; j <= nMax; j++) {
                if (computeDistance(computePos(time + computeDelay(computePos(time, i, j), pos), i, j), pos) < distance) {
                    final int ii = i, jj = j;
                    ret.add(new ArrayList() {
                        {
                            add(ii);
                            add(jj);
                        }
                    });
                }
            }
        }
        return ret;
    }

    /**
     * 计算编号为(m,n)的无人机位置
     *
     * @param time 时刻
     * @param m    序号m
     * @param n    序号n
     * @return 无人机位置(x, y, z)
     */
    public static List<Double> computePos(Double time, Integer m, Integer n) {
        if (m == aerialPlatformsMark) return aerialPlatforms.get(n);
        Double x = v * time + m * dIntraOrbit;
        Double y = n * dInterOrbit;
        Double z = H;
        List<Double> pos = new ArrayList<>();
        pos.addAll(Arrays.asList(x, y, z));
        return pos;
    }

    /**
     * 计算两点之间距离
     *
     * @param pos1 位置1
     * @param pos2 位置2
     * @return 距离
     */
    public static Double computeDistance(List<Double> pos1, List<Double> pos2) {
        Double distance = 0.0;
        for (int i = 0; i < 3; i++) {
            distance += Math.pow((pos1.get(i) - pos2.get(i)), 2);
        }
        distance = Math.pow(distance, 0.5);
        return distance;
    }

    /**
     * 贪心策略，选择距离目的地最近的无人机
     *
     * @param availableUAVs 可用的无人机序号表
     * @param pos           目的地位置
     * @param time          当前时刻
     * @return 无人机序号(m, n)
     */
    public static List<Integer> selectUAV(List<List<Integer>> availableUAVs, List<Double> pos, Double time) {
        return availableUAVs.stream()
                .min((Comparator.comparing(o -> computeDistance(computePos(time, o.get(0), o.get(1)), pos))))
                .get();
    }

    /**
     * 计算转发时延
     *
     * @param pos1 位置1
     * @param pos2 位置2
     * @return 转发时延
     */
    public static Double computeDelay(List<Double> pos1, List<Double> pos2) {
        Double ret = tf + computeDistance(pos1, pos2) / 10000.0;
        return ret;
    }

    public static double dataProcessing(Double x) {
        BigDecimal bigDecimal = new BigDecimal(x);
        BigDecimal ret = bigDecimal.setScale(4, BigDecimal.ROUND_DOWN);
        return ret.doubleValue();

    }


/*
 * 动态规划解单源最短路径
 *
 * @param start 起点基站位置
 * @param end   终点基站位置
 * @param time  任务起始时间
 * @return 总转发时延
 */
    public static Double computeTotalDelay2(List<Double> start, List<Double> end, Double time) {// *
      /*1.划范围
      2.设置初始无人机
      3.迭代->判断是否进入end范围
     */
        List<Integer> delineation = delineation(start, end, time);
        int m_min = delineation.get(0);
        int m_max = delineation.get(1);
        int n_min = delineation.get(2);
        int n_max = delineation.get(3);

        if (start.get(0) > end.get(0)) {
            m_min -= 2;
        }

        //time时刻，以Start Base为起点的各无人机时延表
        DelayTable delayTable = new DelayTable(time, m_min - 1, m_max + 1, n_min - 1, n_max + 1);

        Queue<List<Integer>> UAVqueue = new ArrayDeque<>();

        List<List<Integer>> availableUAVsForStartBase = computeAvailableUAVsforBase(time, D, start);
        availableUAVsForStartBase.forEach(uav -> {
            Double delay = computeDelay(computePos(time, uav.get(0), uav.get(1)), start);
            //delay -= eps;
            delayTable.setDelay(uav.get(0), uav.get(1), delay, Arrays.asList(Integer.MAX_VALUE, Integer.MAX_VALUE));
            UAVqueue.offer(uav);
        });
        Double totalDelay = Double.MAX_VALUE;
        List<Integer> UAVsforEnd = new ArrayList<>();
        while (!UAVqueue.isEmpty()) {
            List<Integer> uav = UAVqueue.poll();
            int m = uav.get(0);
            int n = uav.get(1);
            Double lastDelay = delayTable.getDelay(m, n);
            Double newTime = time + lastDelay;
            List<List<Integer>> availableUAVs = computeAvailableUAVsforUAV(newTime, d, uav);
            for (int i = 0; i < availableUAVs.size(); i++) {
                List<Integer> availableUav = availableUAVs.get(i);
                if (delayTable.isValid(availableUav.get(0), availableUav.get(1))) {
                    int _m = availableUav.get(0);
                    int _n = availableUav.get(1);
                    Double newDelay = computeDelay(computePos(newTime, _m, _n), computePos(newTime, m, n));
                    if (newDelay + delayTable.getDelay(m, n) < delayTable.getDelay(_m, _n)) {
                        UAVqueue.offer(Arrays.asList(_m, _n));
                        delayTable.setDelay(_m, _n, newDelay + delayTable.getDelay(m, n), uav);
                        if (computeDistance(end, computePos(newTime + computeDelay(computePos(newTime, _m, _n), end), _m, _n)) < D) {
                            Double delayTemp = delayTable.getDelay(_m, _n) + computeDelay(computePos(newTime, _m, _n), end);
                            if (delayTemp < totalDelay) {
                                totalDelay = delayTemp;
                                UAVsforEnd.clear();
                                UAVsforEnd.add(_m);
                                UAVsforEnd.add(_n);
                                //System.out.println(Arrays.asList(_m,_n)+""+newTime+computeDelay(computePos(newTime,_m,_n),end));
                            }

                        }
                    }
                }
            }
        }

        //输出转发路径
        List<Integer> ret = UAVsforEnd;

        Stack<List<Integer>> route = new Stack<>();
        route.push(ret);
        while (delayTable.getPre(ret).get(0) < 100) {
            route.push(delayTable.getPre(ret));
            ret = delayTable.getPre(ret);
        }
        while (!route.isEmpty()) {
            List<Integer> uav = route.pop();
            output += "(" + dataProcessing(delayTable.getDelayByList(uav) + time - eps) + "," + uav.get(0) + "," + uav.get(1) + "),";
        }
        return totalDelay;
    }


    static class DelayTable {
        Double startTime;
        Double[][] delayTable;
        HashMap<List<Integer>, List<Integer>> preUAV = new HashMap<>();
        int m_min, m_max, n_min, n_max;

        public DelayTable(Double startTime, int m_min, int m_max, int n_min, int n_max) {
            this.startTime = startTime;
            this.delayTable = new Double[m_max - m_min + 1][n_max - n_min + 1];
            this.m_min = m_min;
            this.n_min = n_min;
            this.m_max = m_max;
            this.n_max = n_max;
            for (int i = 0; i < delayTable.length; i++) {
                for (int j = 0; j < delayTable[0].length; j++) {
                    delayTable[i][j] = Double.MAX_VALUE;
                }
            }
        }

        public Double getDelay(int m, int n) {
            return delayTable[m - m_min][n - n_min];
        }

        public Double getDelayByList(List<Integer> mn) {
            return delayTable[mn.get(0) - m_min][mn.get(1) - n_min];
        }

        public void setDelay(int m, int n, Double delay, List<Integer> pre) {
            this.delayTable[m - m_min][n - n_min] = delay;
            this.preUAV.put(Arrays.asList(m, n), pre);
        }

        public List<Integer> getPre(List<Integer> mn) {
            return this.preUAV.get(mn);
        }

        public boolean isValid(int m, int n) {
            if (m >= m_min && m <= m_max && n >= n_min && n <= n_max) return true;
            else return false;
        }
    }


    /*
     * 划定范围
     * ________ymax________
     * | S              ...
     * xmin               .xmax
     * |               ...
     * |              E ..
     * -------ymin-------
     *
     * @param start 起点基站位置
     * @param end   终点基站位置
     * @param time  任务起始时间
     * @return 边界的m，n编号,m_min,m_max,n_min,n_max
     */
    public static List<Integer> delineation(List<Double> start, List<Double> end, Double time) {
        List<List<Integer>> availableUAVforStatrBase = computeAvailableUAVsforBase(time, D, start);
        List<List<Integer>> availableUAVforEndBase = computeAvailableUAVsforBase(time, D, end);
        final int[] x_min = {Integer.MAX_VALUE};
        final int[] m_max = {0};
        final int[] n_min = {Integer.MAX_VALUE};
        final int[] n_max = {0};
        availableUAVforStatrBase.forEach(num -> {
            if (num.get(0) < x_min[0]) x_min[0] = num.get(0);
            else if (num.get(0) > m_max[0]) m_max[0] = num.get(0);
            if (num.get(1) < n_min[0]) n_min[0] = num.get(1);
            else if (num.get(1) > n_max[0]) n_max[0] = num.get(1);
        });
        availableUAVforEndBase.forEach(num -> {
            if (num.get(0) < x_min[0]) x_min[0] = num.get(0);
            else if (num.get(0) > m_max[0]) m_max[0] = num.get(0);
            if (num.get(1) < n_min[0]) n_min[0] = num.get(1);
            else if (num.get(1) > n_max[0]) n_max[0] = num.get(1);
        });
        List<Integer> ret = new ArrayList<>();
        ret.add(x_min[0]);
        ret.add(m_max[0]);
        ret.add(n_min[0]);
        ret.add(n_max[0]);
        return ret;
    }
    /*
    /**
     * 蚁群算法
     *
     * @param start     起点基站
     * @param end       终点基站
     * @param startTime 起始时间
     * @param signal    信号量
     * @return 最优解的时延
     *//*
    public static Double antColonyOptimization(Base start, Base end, Double startTime, Integer signal) {

    }
    static class Ant{
        List<UAVandAerialPlatform> Tabu;
        List<UAVandAerialPlatform> Allowed;
        double[][] Delta;
        UAVandAerialPlatform current;
        Double delay;
        Random random;
        double alpha;
        double beta;

        Ant(double alpha,double beta,Base start,Double time){
            this.alpha = alpha;
            this.beta = beta;
            this.delay = 0.0;
            Tabu = new ArrayList<>();
            Allowed = new ArrayList<>();
            random = new Random();
            Allowed.addAll(start.getAvailableUAVs(time));
            Delta = new double[Allowed.size()][Allowed.size()];
        }

        public void chooseNext(Double time){
            while (Allowed.size() > 0){
                List<UAVandAerialPlatform> next = current.getAvailable(time);
                int temp = next.size() - 1;
                double all_p = 0.0;
                for (int i = 0; i < next.size(); i++) {
                    all_p +=
                }
            }
        }
    }

    static class Graph{

    }
    */
}
