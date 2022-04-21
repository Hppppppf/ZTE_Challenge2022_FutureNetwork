import javafx.beans.binding.DoubleExpression;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
    //w无人机表
    public static HashMap<List<Integer>, UAV> uavList;
    //基站位置表
    public static List<Base> baseList;
    //高空平台位置表
    public static List<AerialPlatform> aerialPlatforms;
    //开始转发时刻表
    public static List<Double> timeList;

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
    }

    /**
     * 贪心策略，选择距离目的地最近的无人机
     *
     * @param availableUAVs 可用的无人机序号表
     * @param end         目的地位置
     * @param time          当前时刻
     * @return
     */
    public static UAV selectUAV(List<UAV> availableUAVs, Base end, Double time) {
        return availableUAVs
                .stream()
                .min((u1, u2) -> (int) (computeDistance(u1.getPos(time), end.getPos()) - computeDistance(u2.getPos(time), end.getPos())))
                .get();
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
    public static Double computeDelayBetweenUAVs(UAV u1, UAV u2) {
        if (u1.ID.get(0) != u2.ID.get(0)) return horizontalDelay;
        else return verticalDelay;
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

    static class UAV {

        public List<Integer> ID;

        public UAV(Integer m, Integer n) {
            this.ID = Arrays.asList(m, n);
            uavList.put(Arrays.asList(m, n), this);
        }

        public List<Double> getPos(Double time) {
            Double x = v * time + ID.get(0) * dIntraOrbit;
            Double y = ID.get(1) * dInterOrbit;
            Double z = H;
            List<Double> pos = new ArrayList<>();
            pos.addAll(Arrays.asList(x, y, z));
            return pos;
        }

        public List<Integer> getID() {
            return ID;
        }

        @Override
        public String toString() {
            return ID.get(0) + "," + ID.get(1);
        }

        public List<UAV> getAvailableUAVs() {
            List<UAV> uavs = new ArrayList<>();
            uavs.add(new UAV(ID.get(0), ID.get(1) + 1));
            uavs.add(new UAV(ID.get(0), ID.get(1) - 1));
            uavs.add(new UAV(ID.get(0) - 1, ID.get(1)));
            uavs.add(new UAV(ID.get(0) + 1, ID.get(1)));
            return uavs;
        }
    }

    static class AerialPlatform {
        public Integer ID;
        public List<Double> pos;

        public AerialPlatform(Integer ID, List<Double> pos) {
            this.ID = ID;
            this.pos = pos;
        }
        public List<UAV> getAvailableUAVs(Double time) {
            List<UAV> ret = new ArrayList<>();
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
                    if (!uavList.containsKey(uavID)) uavList.put(uavID,new UAV(uavID.get(0),uavID.get(1)));
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

    static class Base {
        public Integer ID;
        public List<Double> pos;

        public Base(Integer ID, List<Double> pos) {
            this.ID = ID;
            this.pos = pos;
        }

        public List<Double> getPos() {
            return this.pos;
        }

        public List<UAV> getAvailableUAVs(Double time) {
            List<UAV> ret = new ArrayList<>();
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

        UAV u1 = new UAV(0, 0);
        System.out.println(u1.getAvailableUAVs());
        UAV u2 = uavList.get(Arrays.asList(0, 1));
        System.out.println(u2);
    }
}
