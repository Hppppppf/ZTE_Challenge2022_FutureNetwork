import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Hppppppf
 * @date 2022/4/9 15:55
 * @description
 */
public class Main {

    public static Double v = 5.0;             //无人机飞行速度
    public static Double H = 10.0;            //无人机飞行高度
    public static Double dIntraOrbit = 90.0;  //轨道内无人机的距离
    public static Double dInterOrbit = 80.0;  //轨道间距
    public static Double d = 125.0;           //无人机之间最大通讯距离
    public static Double D = 70.0;            //无人机与地面基站之间的最大通讯距离
    public static Double tf = 0.1;            //转发时延常数
    public static String path = "result.txt"; //保存路径
    static FileWriter fileWriter;
    static String output = "";

    static {
        try {
            fileWriter = new FileWriter(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Main() throws IOException {
    }

    public static void main(String[] args) throws IOException {
        //基站位置表
        List<List<Double>> posList = new ArrayList<>();
        posList.add(Arrays.asList(45.73, 45.26, 0.0));
        posList.add(Arrays.asList(1200.0, 700.0, 0.0));
        posList.add(Arrays.asList(-940.0, 1100.0, 0.0));
        //开始转发时刻表
        List<Double> timeList = new ArrayList<>();
        timeList.addAll(Arrays.asList(0.0, 4.7, 16.4));


        Double DelayResult = 0.0;
        for (int i = 0; i < 3; i++) {
            for (int j = 1; j < 3; j++) {
                Double temp = computeTotalDelay(posList.get(0), posList.get(j), timeList.get(i));
                DelayResult += temp;
                DecimalFormat decimalFormat = new DecimalFormat("#.0000");
                String out = timeList.get(i) + "," + 0 + "," + j + "," + decimalFormat.format(temp) + "\n";
                System.out.print(out);
                String output_t = output.substring(0, output.length() - 1);
                System.out.println(output_t);
                fileWriter.append(out);
                fileWriter.append(output_t + "\n");
                fileWriter.flush();
                output = "";
            }
        }
        //System.out.print("DelayResultL:"+DelayResult);
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
        List<Integer> selectedUAV = new ArrayList<>(selectUAV(availableUAV, end, time));
        Double delayTemp = computeDelay(computePos(time, selectedUAV.get(0), selectedUAV.get(1)), start);
        delay += delayTemp;
        time += delayTemp;
        DecimalFormat df = new DecimalFormat("0.0000");
        String out = "";
        out = "(" + df.format(time) + "," + selectedUAV.get(0) + "," + selectedUAV.get(1) + "),";
        output += out;
        while (computeDistance(
                computePos(time + computeDelay(computePos(time, selectedUAV.get(0), selectedUAV.get(1)), end), selectedUAV.get(0), selectedUAV.get(1))
                , end) > D) {
            List<List<Integer>> availableUAV_temp = new ArrayList<>(computeAvailableUAVsforUAV(time, d, Arrays.asList(selectedUAV.get(0), selectedUAV.get(1))));
            List<Integer> selectedUAV_temp = new ArrayList<>(selectUAV(availableUAV_temp, end, time));
            List<Integer> preUAV = new ArrayList<>(selectedUAV);
            selectedUAV.clear();
            selectedUAV.addAll(selectedUAV_temp);
            Double delay_temp = computeDelay(computePos(time, selectedUAV.get(0), selectedUAV.get(1)), computePos(time, preUAV.get(0), preUAV.get(1)));
            delay += delay_temp;
            time += delay_temp;
            out = "(" + df.format(time) + "," + selectedUAV.get(0) + "," + selectedUAV.get(1) + "),";
            output += out;
            //System.out.println(computeDistance(computePos(time,selectedUAV.get(0),selectedUAV.get(1)),end));


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
        //求m,n范围
        int mMax = 0, nMax = 0, mMin = 0, nMin = 0;

        int m = startUAV.get(0);
        int n = startUAV.get(1);

        mMin = m - 1 - (int) (dIntraOrbit / d);
        mMax = m + 1 + (int) (dIntraOrbit / d);
        nMin = n - 1 - (int) (dInterOrbit / d);
        nMax = n + 1 + (int) (dInterOrbit / d);


        List<Double> posOfStartUAV = computePos(time, startUAV.get(0), startUAV.get(1));
        //遍历所有可能的无人机
        for (int i = mMin - 1; i <= mMax; i++) {
            for (int j = nMin - 1; j <= nMax; j++) {
                Double delayTemp = computeDelay(computePos(time, i, j), posOfStartUAV);
                if (computeDistance(
                        computePos(time + delayTemp, i, j)
                        , posOfStartUAV) < distance) {
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
                if (computeDistance(
                        computePos(time + computeDelay(computePos(time, i, j), pos), i, j)
                        , pos) < distance) {
                    ret.add(Arrays.asList(i, j));
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
        final Double[] minDistance = {Double.MAX_VALUE};
        List<Integer> ret = new ArrayList<>();
        availableUAVs.forEach(index -> {
            if (computeDistance(computePos(time, index.get(0), index.get(1)), pos) < minDistance[0]) {
                ret.clear();
                ret.add(index.get(0));
                ret.add(index.get(1));
                minDistance[0] = computeDistance(computePos(time, index.get(0), index.get(1)), pos);
            }
        });
        return ret;
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
}
