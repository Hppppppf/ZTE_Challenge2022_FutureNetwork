import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;


/**
 * @author Hppppppf
 * @date 2022/4/21 15:26
 * @description
 */
public class NewMain {
    public static Double v = 5.0;             //无人机飞行速度
    public static Double H = 10.0;            //无人机飞行高度
    public static Double dIntraOrbit = 90.0;  //轨道内无人机的距离
    public static Double dInterOrbit = 80.0;  //轨道间距
    public static Double d = 115.0;           //无人机之间最大通讯距离
    public static Double D = 70.0;            //无人机与地面基站之间的最大通讯距离
    public static Integer s = 10;             //信号量
    public static Integer c = 3;              //链路容量
    public static Integer numberOfTimesSent = (int) Math.ceil((double) s / c);
    public static Double tf = 0.1;            //转发时延常数
    public static String path = "result.txt"; //保存路径
    public static FileWriter fileWriter;
    public static String output = "";

    public static Double horizontalDelay = dataProcessing(tf + 0.009);
    public static Double verticalDelay = dataProcessing(tf + 0.008);
    //无人机表
    public static HashMap<List<Integer>, UAV> uavList;
    //基站位置表
    public static List<Base> baseList;
    //高空平台位置表
    public static List<AerialPlatform> aerialPlatforms;
    //开始转发时刻表
    public static List<Double> timeList;
    //冲突表
    public static Conflicts conflicts;

    public static Double DelayResult;

    static {
        try {
            fileWriter = new FileWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Init();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    if (j == k) continue;
                    for (int l = 0; l < 4; l++) {
                        //TODO 时延累加；信号量
                        greedyAlgorithm(baseList.get(j), baseList.get(k), timeList.get(i));
                    }
                }
            }
        }
    }

    public static void greedyAlgorithm(Base start, Base end, Double startTime) {
        String ret = "";
        Double delay = 0.0;
        UAVandAerialPlatform last = select(start.getAvailableUAVs(startTime), end, startTime);
        delay += computeDelayByPos(last.getPos(startTime), start.getPos());
        Double currentTime = startTime + delay;
        conflicts.add(start, last, startTime, currentTime);
        ret += "(" + dataProcessing(currentTime) + "," + last + "),";
        while (computeDistance(last.getPos(currentTime), end.getPos()) > D) {
            UAVandAerialPlatform pre = last;
            last = select(last.getAvailable(currentTime), end, currentTime);
            Double delay_temp = computeDelayBetweenUAVs(pre, last);
            Double time_s = currentTime;
            Double time_e = currentTime + delay_temp;
            while (conflicts.isConflicted(pre, last, time_s, time_e)) {
                time_s = time_e + 0.01;
                time_e = time_s + delay_temp;
            }
            conflicts.add(pre,last,time_s,time_e);
            delay += time_e - currentTime;
            currentTime = time_e;
            ret += "(" + dataProcessing(currentTime) + "," + last + "),";
        }
        ret = ret.substring(0, ret.length() - 1);
        System.out.println(startTime + "," + start.ID + "," + end.ID + "," + dataProcessing(delay) + "\n" + ret);
    }


    /**
     * 贪心策略选择离终点最近的无人机/高空平台
     *
     * @param list
     * @param end
     * @param time
     * @return
     */
    public static UAVandAerialPlatform select(List<UAVandAerialPlatform> list, Base end, Double time) {
        return list.stream().min((o1, o2) -> (int) (computeDistance(o1.getPos(time), end.getPos()) - computeDistance(o2.getPos(time), end.getPos()))).get();
    }

    interface UAVandAerialPlatform {
        public String getID();

        public List<Double> getPos(Double time);

        List<UAVandAerialPlatform> getAvailable(Double time);
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
     * 通过位置计算转发时延
     *
     * @param pos1 位置1
     * @param pos2 位置2
     * @return 转发时延
     */
    public static Double computeDelayByPos(List<Double> pos1, List<Double> pos2) {
        Double ret = tf + computeDistance(pos1, pos2) / 10000.0;
        return ret;
    }

    /**
     * 计算相邻无人机间信号传输时延
     *
     * @param u1
     * @param u2
     * @return
     */
    public static Double computeDelayBetweenUAVs(UAVandAerialPlatform u1, UAVandAerialPlatform u2) {
        return (u1.getID().charAt(0) != u2.getID().charAt(0)) ? horizontalDelay : verticalDelay;
    }

    /**
     * 处理数据
     *
     * @param x
     * @return
     */
    public static double dataProcessing(Double x) {
        BigDecimal bigDecimal = new BigDecimal(x);
        BigDecimal ret = bigDecimal.setScale(4, BigDecimal.ROUND_DOWN);
        return ret.doubleValue();
    }


    static class Conflicts {
        public HashMap<String, Integer> conflictMap;

        public Conflicts() {
            this.conflictMap = new HashMap<>();
        }

        public void add(Object u1, Object u2, Double time_s, Double time_e) {
            String key = u1 + ":" + u2 + "|" + dataProcessing(time_s) + "->" + dataProcessing(time_e);
            conflictMap.put(key, conflictMap.getOrDefault(key, 0) + 1);
        }

        public boolean isConflicted(Object u1, Object u2, Double time_s, Double time_e) {
            String key = u1 + ":" + u2 + "|" + dataProcessing(time_s) + "->" + dataProcessing(time_e);
            if (this.conflictMap.containsKey(key)) {
                return conflictMap.get(key) >= 3;
            }
            return false;
        }
    }

    static class UAV implements UAVandAerialPlatform {

        public List<Integer> ID;

        public UAV(Integer m, Integer n) {
            this.ID = Arrays.asList(m, n);
            uavList.put(Arrays.asList(m, n), this);
        }

        @Override
        public List<Double> getPos(Double time) {
            Double x = v * time + ID.get(0) * dIntraOrbit;
            Double y = ID.get(1) * dInterOrbit;
            Double z = H;
            List<Double> pos = new ArrayList<>();
            pos.addAll(Arrays.asList(x, y, z));
            return pos;
        }

        @Override
        public List<UAVandAerialPlatform> getAvailable(Double time) {
            List<UAVandAerialPlatform> list = new ArrayList<>();
            list.add(new UAV(ID.get(0), ID.get(1) + 1));
            list.add(new UAV(ID.get(0), ID.get(1) - 1));
            list.add(new UAV(ID.get(0) - 1, ID.get(1)));
            list.add(new UAV(ID.get(0) + 1, ID.get(1)));
            aerialPlatforms.forEach(aerialPlatform -> {
                if (computeDistance(this.getPos(time), aerialPlatform.pos) < d) list.add(aerialPlatform);
            });
            return list;
        }

        public String getID() {
            return ID.toString();
        }

        @Override
        public String toString() {
            return ID.get(0) + "," + ID.get(1);
        }
    }

    static class AerialPlatform implements UAVandAerialPlatform {
        public Integer ID;
        public List<Double> pos;

        public AerialPlatform(Integer ID, List<Double> pos) {
            this.ID = ID;
            this.pos = pos;
        }

        @Override
        public String getID() {
            return ID.toString();
        }

        @Override
        public List<Double> getPos(Double time) {
            return pos;
        }

        @Override
        public List<UAVandAerialPlatform> getAvailable(Double time) {
            List<UAVandAerialPlatform> ret = new ArrayList<>();
            //TODO 是否有其他高空平台在覆盖范围内
            //求m,n范围

            int m = (int) ((this.pos.get(0) - v * time) / dIntraOrbit);
            int n = (int) (this.pos.get(1) / dInterOrbit);

            int mMin = m - 1 - (int) (dIntraOrbit / D);
            int mMax = m + 1 + (int) (dIntraOrbit / D);
            int nMin = n - 1 - (int) (dInterOrbit / D);
            int nMax = n + 1 + (int) (dInterOrbit / D);

            //遍历所有可能的无人机
            for (int i = mMin - 1; i <= mMax; i++) {
                for (int j = nMin - 1; j <= nMax; j++) {
                    List<Integer> uavID = Arrays.asList(i, j);
                    if (!uavList.containsKey(uavID)) uavList.put(uavID, new UAV(uavID.get(0), uavID.get(1)));
                    UAV uav = uavList.get(uavID);
                    Double newTime = time + computeDelayByPos(this.pos, uav.getPos(time));
                    if (computeDistance(uav.getPos(newTime), this.pos) < D) {
                        ret.add(uav);
                    }
                }
            }
            return ret;
        }

        @Override
        public String toString() {
            return ID.toString();
        }
    }

    static class Base {
        public Integer ID;
        public List<Double> pos;

        public Base(Integer ID, List<Double> pos) {
            this.ID = ID;
            this.pos = pos;
        }

        @Override
        public String toString() {
            return "Base" + this.ID.toString();
        }

        public List<Double> getPos() {
            return this.pos;
        }

        public List<UAVandAerialPlatform> getAvailableUAVs(Double time) {
            List<UAVandAerialPlatform> ret = new ArrayList<>();
            //TODO 是否有高空平台在基站覆盖范围内
            //求m,n范围

            int m = (int) ((this.pos.get(0) - v * time) / dIntraOrbit);
            int n = (int) (this.pos.get(1) / dInterOrbit);

            int mMin = m - 1 - (int) (dIntraOrbit / D);
            int mMax = m + 1 + (int) (dIntraOrbit / D);
            int nMin = n - 1 - (int) (dInterOrbit / D);
            int nMax = n + 1 + (int) (dInterOrbit / D);

            //遍历所有可能的无人机
            for (int i = mMin - 1; i <= mMax; i++) {
                for (int j = nMin - 1; j <= nMax; j++) {
                    List<Integer> uavID = Arrays.asList(i, j);
                    if (!uavList.containsKey(uavID)) {
                        uavList.put(uavID, new UAV(i, j));
                    }
                    UAV uav = uavList.get(uavID);
                    Double newTime = time + computeDelayByPos(this.pos, uav.getPos(time));
                    if (computeDistance(uav.getPos(newTime), this.pos) < D) {
                        ret.add(uav);
                    }
                }
            }
            return ret;
        }
    }

    public static void Init() {
        baseList = new ArrayList<>();
        aerialPlatforms = new ArrayList<>();
        timeList = new ArrayList<>();
        uavList = new HashMap<>();
        conflicts = new Conflicts();

        baseList.add(new Base(0, Arrays.asList(45.73, 45.26, 0.0)));
        baseList.add(new Base(1, Arrays.asList(1200.0, 700.0, 0.0)));
        baseList.add(new Base(2, Arrays.asList(-940.0, 1100.0, 0.0)));

        aerialPlatforms.add(new AerialPlatform(0, Arrays.asList(-614.0, 1059.0, 24.0)));
        aerialPlatforms.add(new AerialPlatform(1, Arrays.asList(-943.0, 715.0, 12.0)));
        aerialPlatforms.add(new AerialPlatform(2, Arrays.asList(1073.0, 291.0, 37.0)));
        aerialPlatforms.add(new AerialPlatform(3, Arrays.asList(715.0, 129.0, 35.0)));
        aerialPlatforms.add(new AerialPlatform(4, Arrays.asList(186.0, 432.0, 21.0)));
        aerialPlatforms.add(new AerialPlatform(5, Arrays.asList(-923.0, 632.0, 37.0)));
        aerialPlatforms.add(new AerialPlatform(6, Arrays.asList(833.0, 187.0, 24.0)));
        aerialPlatforms.add(new AerialPlatform(7, Arrays.asList(-63.0, 363.0, 11.0)));

        timeList.addAll(Arrays.asList(0.0, 4.7, 16.4));

        DelayResult = 0.0;
    }
}
