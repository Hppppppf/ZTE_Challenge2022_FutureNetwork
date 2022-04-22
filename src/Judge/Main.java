package Judge;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ROUND_DOWN;

public class Main {

    static double t = 0;

    static final double v = 5;


    static final int H = 10;

    static final int dIntraOrbit = 90;

    static final int dInterOrbit = 80;

    static final int d = 115;

    static final int D = 70;

    static final double tf = 0.1;

    static FileWriter fileWriter;

    static DecimalFormat decimalFormat = new DecimalFormat("0.0000");


    static {
        try {
            fileWriter = new FileWriter("result.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static StringBuilder output = new StringBuilder();


    public static void main(String[] args) throws IOException {
        Base base1 = new Base(new Location(45.73, 45.26, 0.0), 0);
        Base base2 = new Base(new Location(1200.0, 700.0, 0.0), 1);
        Base base3 = new Base(new Location(-940.0, 1100.0, 0.0), 2);
        ArrayList<Base> bases = new ArrayList() {{
            add(base1);
            add(base2);
            add(base3);
        }};
        //开始转发时刻表
        List<Double> timeList = new ArrayList() {{
            add(0.0);
            add(4.7);
            add(16.4);
        }};

        HighPlatform highPlatform1 = new HighPlatform(new Location(-614, 1059, 24, 100));
        HighPlatform highPlatform2 = new HighPlatform(new Location(-934, 715, 12, 100));
        HighPlatform highPlatform3 = new HighPlatform(new Location(1073, 291, 37, 100));
        HighPlatform highPlatform4 = new HighPlatform(new Location(715, 129, 35, 100));
        HighPlatform highPlatform5 = new HighPlatform(new Location(186, 432, 21, 100));
        HighPlatform highPlatform6 = new HighPlatform(new Location(-923, 632, 37, 100));
        HighPlatform highPlatform7 = new HighPlatform(new Location(833, 187, 24, 100));
        HighPlatform highPlatform8 = new HighPlatform(new Location(-63, 363, 11, 100));
        List<HighPlatform> highPlatformList = new ArrayList() {{
            add(highPlatform1);
            add(highPlatform2);
            add(highPlatform3);
            add(highPlatform4);
            add(highPlatform5);
            add(highPlatform6);
            add(highPlatform7);
            add(highPlatform8);
        }};


        Double DelayResult = 0.0;
        for (int i = 0; i < 3; i++) {
            for (int z = 0; z < 3; z++) {
                for (int j = 0; j < 3; j++) {
                    if (z == j) continue;
                    t = timeList.get(i);
                    UAV uav = calDelayBetweenBase3(bases.get(z), bases.get(j), highPlatformList);
                }
            }
        }
//        t = timeList.get(0);
//        UAV uav = calDelayBetweenBase3(base1, base2, highPlatformList);
    }

    /**
     * 计算基站周围可用无人机
     *
     * @param base
     * @return
     */
    public static List<UAV> computeAvailableUAVsForBase(Base base, HashMap<String, List<Double>> hm) {
        //求m,n范围
        int mMax, nMax, mMin, nMin;

        int m = (int) ((base.getLocation().getX() - v * t) / dIntraOrbit);
        int n = (int) (base.getLocation().getY() / dInterOrbit);

        mMin = m - 1 - (int) (dIntraOrbit / D);
        mMax = m + 1 + (int) (dIntraOrbit / D);
        nMin = n - 1 - (int) (dInterOrbit / D);
        nMax = n + 1 + (int) (dInterOrbit / D);


        List<UAV> result = new ArrayList<>();

        for (int i = mMin; i <= mMax; i++)
            for (int j = nMin; j <= nMax; j++) {
                UAV uav = new UAV(j, i, 0);
                String s = "base:" + uav.getM() + "," + uav.getN();
                if (hm.containsKey(s)) {
                    uav = new UAV(j, i, hm.get(s).get(hm.get(s).size() - 1));
                }
                //当前无人机与基站距离
                Double distance = Location.calDistance(uav.location, base.getLocation());
                //计算时延
                Double delay = computeDelay(uav.location, base.getLocation());

                //更新无人机位置
                uav.updateLocation(delay + (hm.containsKey(s) ? hm.get(s).get(hm.get(s).size() - 1) : 0));
                //无人机位置更新后的位置
                Double distance1 = Location.calDistance(uav.location, base.getLocation());

                //传播前和传播后位置都符合
                if (distance <= D && distance1 <= D) {
                    result.add(uav);
                    uav.sb.append("(" + decimalFormat.format(uav.location.delay) + "," + uav.getM() + "," + uav.getN() + ")");
                }
            }

        return result;
    }


    /**
     * 二阶段题解
     *
     * @param start
     * @param end
     * @param platformList
     * @return
     */
    public static UAV calDelayBetweenBase3(Base start, Base end, List<HighPlatform> platformList) {
        HashMap<String, List<Double>> hm = new HashMap<>();
        List<UAV> uavs = computeAvailableUAVsForBase(start, hm);

        Double totalDelay = 0.0;
        StringBuilder sb = new StringBuilder();

        UAV u = new UAV(0, 0, 1000);
        for (UAV uav : uavs) {
            UAV minDelayUAV = findMinDelayUAV(uav, start, end, platformList, hm);
            if (minDelayUAV.location.delay < u.location.delay) {
                u = minDelayUAV;
            }
        }
        totalDelay += u.location.delay;
        sb.append(t + "," + start.index + "," + end.index + "," + decimalFormat.format(totalDelay) + ",3" + "\n");
        sb.append(u.sb + "\n");
        handleString(hm, u);
        u = new UAV(0, 0, 1000);
        uavs = computeAvailableUAVsForBase(start, hm);
        for (UAV uav : uavs) {
            UAV minDelayUAV = findMinDelayUAV(uav, start, end, platformList, hm);
            if (minDelayUAV.location.delay < u.location.delay) {
                u = minDelayUAV;
            }
        }
        totalDelay += u.location.delay;
        sb.append(t + "," + start.index + "," + end.index + "," + decimalFormat.format(totalDelay) + ",3" + "\n");
        sb.append(u.sb + "\n");
        handleString(hm, u);
        uavs = computeAvailableUAVsForBase(start, hm);
        u = new UAV(0, 0, 1000);
        for (UAV uav : uavs) {
            UAV minDelayUAV = findMinDelayUAV(uav, start, end, platformList, hm);
            if (minDelayUAV.location.delay < u.location.delay) {
                u = minDelayUAV;
            }
        }
        totalDelay += u.location.delay;
        sb.append(t + "," + start.index + "," + end.index + "," + decimalFormat.format(totalDelay) + ",3" + "\n");
        sb.append(u.sb + "\n");
        handleString(hm, u);
        uavs = computeAvailableUAVsForBase(start, hm);
        u = new UAV(0, 0, 1000);
        for (UAV uav : uavs) {
            UAV minDelayUAV = findMinDelayUAV(uav, start, end, platformList, hm);
            if (minDelayUAV.location.delay < u.location.delay) {
                u = minDelayUAV;
            }
        }
        totalDelay += u.location.delay;
        sb.append(t + "," + start.index + "," + end.index + "," + decimalFormat.format(totalDelay) + ",1" + "\n");
        sb.append(u.sb + "\n");

        System.out.print(sb);

        return u;
    }

    public static void handleString(HashMap<String, List<Double>> hm, UAV u) {
        String[] split = u.sb.toString().split("\\)" + "," + "\\(");
        String s = "base";
        double previousDouble = 0.0;
        for (int i = 0; i < split.length; i++) {
            String r = split[i].replaceAll("\\(|\\)", "");
            String[] split1 = r.split(",");
            ArrayList<Double> lists = new ArrayList<>();
            lists.add(previousDouble);
            lists.add(Double.valueOf(split1[0]));
            if (split1.length == 2) {
                if (hm.containsKey(s + ":" + split1[1])) {
                    List<Double> doubles = hm.get(s + ":" + split1[1]);
                    doubles.add(previousDouble);
                    doubles.add(Double.valueOf(split1[0]));
                    s = split1[1];
                    previousDouble = doubles.get(doubles.size() - 1);
                } else {
                    hm.put(s + ":" + split1[1], lists);
                    s = split1[1];
                    previousDouble = lists.get(1);
                }
            } else {
                if (hm.containsKey(s + ":" + split1[1] + "," + split1[2])) {
                    List<Double> doubles = hm.get(s + ":" + split1[1] + "," + split1[2]);
                    doubles.add(previousDouble);
                    doubles.add(Double.valueOf(split1[0]));
                    s = split1[1] + "," + split1[2];
                    previousDouble = doubles.get(doubles.size() - 1);
                } else {
                    hm.put(s + ":" + split1[1] + "," + split1[2], lists);
                    s = split1[1] + "," + split1[2];
                    previousDouble = lists.get(1);
                }
            }

        }
    }

    /**
     * 找到耗时最短的无人机
     *
     * @param uav
     * @param start
     * @param end
     * @param platformList
     * @return
     */
    public static UAV findMinDelayUAV(UAV uav, Base start, Base end, List<HighPlatform> platformList, HashMap<String, List<Double>> timeHm) {
        for (HighPlatform highPlatform : platformList) {
            highPlatform.location.delay = 100.0;
            highPlatform.sb = new StringBuilder();
        }
        int x = 1, y = 1;
        if (start.getLocation().getX() > end.getLocation().getX()) {
            x = -1;
        }
        if (start.getLocation().getY() > end.getLocation().getY()) {
            y = -1;
        }

        UAV result = new UAV(0, 0, 1000);
        Queue<UAV> queue = new ArrayDeque<>();
        queue.add(uav);
        HashMap<String, UAV> hm = new HashMap<>();
        hm.put(uav.getM() + " " + uav.getN(), uav);
        while (!queue.isEmpty()) {
            UAV poll = queue.poll();
            poll = hm.get(poll.getM() + " " + poll.getN());

            //更新结果
            result = updateResult(end, result, poll);
            //判断是否超出计算范围
            if (outOfBase(start, end, poll)) break;
            //更新高空平台
            updateHighPlatform(platformList, poll, timeHm);
            //更新无人机
            updateUAV(poll, start, end, platformList, poll.getM() + x, poll.getN(), queue, hm, timeHm);
            updateUAV(poll, start, end, platformList, poll.getM(), poll.getN() + y, queue, hm, timeHm);
            updateUAV(poll, start, end, platformList, poll.getM() + x, poll.getN() + y, queue, hm, timeHm);

        }
        return result;

    }

    /**
     * 更新空中平台
     *
     * @param platformList
     * @param poll
     */
    private static void updateHighPlatform(List<HighPlatform> platformList, UAV poll, HashMap<String, List<Double>> timeHm) {
        boolean flag = false;
        for (int i = 0; i < platformList.size(); i++) {
            HighPlatform highPlatform = platformList.get(i);
            if (Location.calDistance(poll.location, highPlatform.location) < d
                    && highPlatform.location.delay > poll.location.delay + computeDelay(poll.location, highPlatform.location)) {

                String key = poll.getM() + "," + poll.getN() + ":" + i;

                if (!timeHm.containsKey(key)
                        || timeHm.get(key).get(timeHm.get(key).size() - 1) <= poll.location.delay
                        || timeHm.get(key).get(timeHm.get(key).size() - 2) >= poll.location.delay + computeDelay(poll.location, highPlatform.location)) {
                    highPlatform.location.delay = poll.location.delay + computeDelay(poll.location, highPlatform.location);
                    highPlatform.sb = new StringBuilder(poll.sb + ",(" + decimalFormat.format(highPlatform.location.delay) + "," + i + ")");
                    flag = true;
                } else {
                    UAV uav = new UAV(poll.getM(), poll.getN(), timeHm.get(key).get(timeHm.get(key).size() - 1));
                    if (highPlatform.location.delay > timeHm.get(key).get(timeHm.get(key).size() - 1) + computeDelay(uav.location, highPlatform.location)) {
                        highPlatform.location.delay = timeHm.get(key).get(timeHm.get(key).size() - 1) + computeDelay(uav.location, highPlatform.location);
                        highPlatform.sb = new StringBuilder(poll.sb + ",(" + decimalFormat.format(highPlatform.location.delay) + "," + i + ")");
                        flag = true;
                    }
                }
            }
        }

        if (flag) {
            for (HighPlatform highPlatform : platformList) {
                if (highPlatform.location.delay >= 100) continue;
                for (int i = 0; i < platformList.size(); i++) {
                    HighPlatform platform = platformList.get(i);
                    if (platform == highPlatform) continue;
                    if (Location.calDistance(platform.location, highPlatform.location) < d
                            && platform.location.delay > highPlatform.location.delay + computeDelay(highPlatform.location, platform.location)) {
                        String key = platformList.indexOf(highPlatform) + ":" + i;
                        if (!timeHm.containsKey(key)
                                || timeHm.get(key).get(1) < highPlatform.location.delay
                                || timeHm.get(key).get(0) > highPlatform.location.delay + computeDelay(highPlatform.location, platform.location)) {
                            platform.location.delay = highPlatform.location.delay + computeDelay(highPlatform.location, platform.location);
                            platform.sb = new StringBuilder(poll.sb + ",(" + decimalFormat.format(highPlatform.location.delay) + "," + i + ")");
                        }
                    }
                }
            }
        }
    }

    /**
     * 更新结果
     *
     * @param end
     * @param result
     * @param poll
     * @return
     */
    private static UAV updateResult(Base end, UAV result, UAV poll) {
        if (Location.calDistance(poll.location, end.getLocation()) < d && poll.location.delay + computeDelay(poll.location, end.getLocation()) < result.location.delay) {
            result = poll;
            poll.location.delay += computeDelay(poll.location, end.getLocation());
        }
        return result;
    }

    /**
     * 无人机已经远远飞出了基站的范围
     *
     * @param start
     * @param end
     * @param poll
     * @return
     */
    private static boolean outOfBase(Base start, Base end, UAV poll) {
        boolean b = (end.getLocation().getX() - start.getLocation().getX()) * (poll.location.getX() - end.getLocation().getX()) > 0;
        boolean b1 = (end.getLocation().getY() - start.getLocation().getY()) * (poll.location.getY() - end.getLocation().getY()) > 0;
        if (b && b1 && Location.calDistance(poll.location, end.getLocation()) > 2 * d) {
            return true;
        }
        return false;
    }


    public static void updateUAV(UAV uav, Base start, Base end, List<HighPlatform> platformList, int M, int N, Queue<UAV> queue, HashMap<String, UAV> hm, HashMap<String, List<Double>> timeHm) {
        String key = M + " " + N;
        if (!(Math.abs(uav.getM() - M) >= 1 && Math.abs(uav.getN() - N) >= 1)) {
            UAV uav1 = new UAV(N, M, uav.location.delay);
            uav1.updateLocation(computeDelay(uav1.location, uav.location) + uav.location.delay);
            if (!hm.containsKey(key) || hm.get(key).location.delay > uav1.location.delay) {
                String s = uav.getM() + "," + uav.getN() + ":" + uav1.getM() + "," + uav1.getN();
                if (!timeHm.containsKey(s) || timeHm.get(s).get(timeHm.get(s).size() - 1) < uav.location.delay) {
                    hm.put(key, uav1);
                    uav1.sb = new StringBuilder(uav.sb + ",(" + decimalFormat.format(uav1.location.delay) + "," + uav1.getM() + "," + uav1.getN() + ")");
                    queue.add(uav1);
                } else {
                    if (!hm.containsKey(key) || hm.get(key).location.delay > timeHm.get(s).get(timeHm.get(s).size() - 1) + computeDelay(uav.location, uav1.location)) {
                        uav1.updateLocation(timeHm.get(s).get(timeHm.get(s).size() - 1) + computeDelay(uav.location, uav1.location));
                        hm.put(key, uav1);
                        uav1.sb = new StringBuilder(uav.sb + ",(" + decimalFormat.format(uav1.location.delay) + "," + uav1.getM() + "," + uav1.getN() + ")");
                        queue.add(uav1);
                    }
                }
            }
        }

        for (HighPlatform highPlatform : platformList) {
            if (highPlatform.location.delay >= 100) continue;
            UAV uav1 = new UAV(N, M, highPlatform.location.delay);
            if (Location.calDistance(highPlatform.location, uav1.location) < d) {
                uav1.location = new Location(M, N, highPlatform.location.delay + computeDelay(highPlatform.location, uav1.location));
                if (!hm.containsKey(key) || hm.get(key).location.delay > uav1.location.delay) {
                    String s = platformList.indexOf(platformList) + ":" + uav1.getM() + "," + uav1.getN();

                    if (!timeHm.containsKey(s) || timeHm.get(s).get(timeHm.get(s).size() - 1) < highPlatform.location.delay || timeHm.get(s).get(timeHm.get(s).size() - 2) > uav1.location.delay) {
                        uav1.sb = new StringBuilder(highPlatform.sb + ",(" + decimalFormat.format(uav1.location.delay) + "," + uav1.getM() + "," + uav1.getN() + ")");
                        hm.put(key, uav1);
                        queue.add(uav1);
                    } else {
                        UAV uav2 = new UAV(N, M, timeHm.get(s).get(timeHm.get(s).size() - 1));
                        if (!hm.containsKey(key) || hm.get(key).location.delay > timeHm.get(s).get(timeHm.get(s).size() - 1) + computeDelay(highPlatform.location, uav2.location)) {
                            uav2.updateLocation(timeHm.get(s).get(timeHm.get(s).size() - 1) + computeDelay(highPlatform.location, uav2.location));
                            hm.put(key, uav2);
                            uav2.sb = new StringBuilder(highPlatform.sb + ",(" + decimalFormat.format(uav2.location.delay) + "," + uav2.getM() + "," + uav2.getN() + ")");
                            queue.add(uav1);
                        }
                    }
                }
            }
        }

    }


    /**
     * 找到可用的高空平台
     *
     * @param start
     * @param end
     * @param platformList
     * @return
     */
    public static List<HighPlatform> findAvailablePlatform(Base start, Base end, List<HighPlatform> platformList) {
        return platformList.stream().filter(o -> (o.location.getX() - start.getLocation().getX()) * (end.getLocation().getX() - o.location.getX()) > 0
                &&
                (o.location.getY() - start.getLocation().getY()) * (end.getLocation().getY() - o.location.getY()) >= 0).collect(Collectors.toList());
    }


    /**
     * 生成字符串
     *
     * @param first
     * @param last
     */
    public static void genString(UAV first, UAV last) {
        int firstM = first.getM();
        int firstN = first.getN();
        int lastM = last.getM();
        int lastN = last.getN();
        int x = 1, y = 1;
        if (firstM > lastM) {
            x = -1;
        }
        if (firstN > lastN) {
            y = -1;
        }

        double delay1 = 90 / 10000.0;
        double delay2 = 80 / 10000.0;
        double delay3 = Math.pow(90 * 90 + 80 * 80, 0.5) / 10000.0;

        last.sb.append("(" + decimalFormat.format(first.location.delay + t) + "," + first.getM() + "," + first.getN() + ")");
        double delay = first.location.delay;
        while ((lastM - firstM) * x > 0 && (lastN - firstN) * y > 0) {
            firstM += x;
            firstN += y;
            delay = delay + delay3 + 0.1;
            last.sb.append(",(" + decimalFormat.format(delay + t) + "," + firstM + "," + firstN + ")");
        }

        while ((lastM - firstM) * x > 0) {
            firstM += x;
            delay = delay + delay1 + 0.1;
            last.sb.append(",(" + decimalFormat.format(delay + t) + "," + firstM + "," + firstN + ")");
        }

        while ((lastN - firstN) * y > 0) {
            firstN += y;
            delay = delay + delay2 + 0.1;
            last.sb.append(",(" + decimalFormat.format(delay + t) + "," + firstM + "," + firstN + ")");
        }

    }


    /**
     * 初试化延迟表
     *
     * @param start
     * @param end
     * @return
     */
    private static double[][] initArray(Base start, Base end) {
        List<Integer> nBetweenBases = findNBetweenBases(start, end);
        List<Integer> mBetweenBases = findMBetweenBases(start, end);
        int minN = nBetweenBases.get(0);
        int maxN = nBetweenBases.get(1);
        int minM = mBetweenBases.get(0);
        int maxM = mBetweenBases.get(1);

        double[][] delay = new double[maxM - minM + 1][maxN - minN + 1];
        for (int i = 0; i < delay.length; i++) {
            Arrays.fill(delay[i], 100);
        }
        double delay1 = 90 / 10000.0;
        double delay2 = 80 / 10000.0;
        double delay3 = Math.pow(90 * 90 + 80 * 80, 0.5) / 10000.0;


        delay[0][0] = 0;
        for (int i = 0; i < delay.length; i++)
            for (int j = 0; j < delay[0].length; j++) {
                if (i > 0) {
                    BigDecimal bigDecimal = new BigDecimal(delay[i - 1][j] + 0.1 + delay1);
                    bigDecimal = bigDecimal.setScale(6, ROUND_DOWN);
                    delay[i][j] = Math.min(bigDecimal.doubleValue(), delay[i][j]);
                }
                if (j > 0) {
                    BigDecimal bigDecimal = new BigDecimal(delay[i][j - 1] + 0.1 + delay2);
                    bigDecimal = bigDecimal.setScale(6, ROUND_DOWN);
                    delay[i][j] = Math.min(bigDecimal.doubleValue(), delay[i][j]);
                }
                if (i > 0 && j > 0) {
                    BigDecimal bigDecimal = new BigDecimal(delay[i - 1][j - 1] + 0.1 + delay3);
                    bigDecimal = bigDecimal.setScale(6, ROUND_DOWN);
                    delay[i][j] = Math.min(bigDecimal.doubleValue(), delay[i][j]);
                }
            }
        return delay;
    }


    /**
     * 两个基站为角形成一个长方形，找出无人机能与其通信的最大N与最小N
     *
     * @param start
     * @param end
     * @return list(0)为最小  list(1)为最大
     */
    public static List<Integer> findNBetweenBases(Base start, Base end) {
        Double max = Math.max(start.getLocation().getY() + D, end.getLocation().getY() + D);
        Double min = Math.min(start.getLocation().getY() - D, end.getLocation().getY() - D);

        //找到最下面的N
        int N = (int) (min / dInterOrbit) - 1;

        List<Integer> temp = new ArrayList<>();
        while (N * dInterOrbit <= max) {
            if (N * dInterOrbit >= min) {
                temp.add(N);
            }
            N++;
        }

        ArrayList<Integer> result = new ArrayList<>();
        result.add(temp.get(0));
        result.add(temp.get(temp.size() - 1));

        return result;
    }


    /**
     * 两个基站为角形成一个长方形，找出无人机能与其通信的最大M与最小M
     *
     * @param start
     * @param end
     * @return
     */
    public static List<Integer> findMBetweenBases(Base start, Base end) {
        double max = Math.max(start.getLocation().getX() + 2 * D, end.getLocation().getX() + 2 * D);
        double min = Math.min(start.getLocation().getX() - 2 * D, end.getLocation().getX() - 2 * D);
        int M = (int) (min / dIntraOrbit) - 1;
        List<Integer> temp = new ArrayList<>();
        while (M * dIntraOrbit <= max) {
            if (M * dIntraOrbit >= min) {
                temp.add(M);
            }
            M++;
        }

        ArrayList<Integer> result = new ArrayList<>();
        result.add(temp.get(0));
        result.add(temp.get(temp.size() - 1));

        return result;
    }


    /**
     * 计算传输时延
     *
     * @param M1
     * @param N1
     * @param M2
     * @param N2
     * @return
     */
    public static double calDelay(int M1, int N1, int M2, int N2) {
        UAV uav = new UAV(N1, M1, 0);
        UAV uav1 = new UAV(N2, M2, 0);
        return computeDelay(uav.location, uav1.location);
    }


    /**
     * 计算无人机可通信范围内的所有无人机
     *
     * @param u
     * @param delay 总时延
     * @return
     */
    public static List<UAV> findAvailableUAVSForUAV(UAV u, double delay) {
        //求m,n范围
        int mMax = 0, nMax = 0, mMin = 0, nMin = 0;
        List<UAV> result = new ArrayList<>();

        int m = u.getM();
        int n = u.getN();

        mMin = m - 1 - (int) (dIntraOrbit / d);
        mMax = m + 1 + (int) (dIntraOrbit / d);
        nMin = n - 1 - (int) (dInterOrbit / d);
        nMax = n + 1 + (int) (dInterOrbit / d);

        for (int i = mMin - 1; i <= mMax; i++) {
            for (int j = nMin - 1; j <= nMax; j++) {
                UAV uav = new UAV(j, i, delay);
                if (Location.calDistance(uav.location, u.location) < d) {
                    result.add(uav);
                }
            }
        }
        return result;
    }

    /**
     * 计算转发时延
     *
     * @param pos1
     * @param pos2
     * @return
     */
    public static Double computeDelay(Location pos1, Location pos2) {
        Double ret = tf + Location.calDistance(pos1, pos2) / 10000.0;
        return ret;
    }


}


class UAV {
    private int m;
    private int n;


    StringBuilder sb = new StringBuilder();

    /**
     * 传播前的距离
     */
    Location location;


    public UAV(int n, int m, double delay) {
        this.n = n;
        this.m = m;
        this.location = new Location(Main.v * (Main.t + delay) + this.m * Main.dIntraOrbit, this.n * Main.dInterOrbit, Main.H);
        location.delay = delay;
    }

    public int getM() {
        return m;
    }

    public int getN() {
        return n;
    }


    public void setM(int m) {
        this.m = m;
    }

    public void setN(int n) {
        this.n = n;
    }


    /**
     * @param delay 总延迟
     * @Param 当前时间
     */
    void updateLocation(double delay) {
        this.location = new Location(Main.v * (Main.t + delay) + this.m * Main.dIntraOrbit, this.n * Main.dInterOrbit, Main.H);
        location.delay = delay;
    }


}


class Base {
    /**
     * 位置
     */
    private Location location;

    int index;

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Location getLocation() {
        return location;
    }

    public int getIndex() {
        return index;
    }

    public Base(Location location, int index) {
        this.location = location;
        this.index = index;
    }
}

class Location {

    private double x;

    private double y;

    private double z;

    /**
     * calDelayBetweenBases2中使用，信号能够传达到该位置时最早的时延（delay）
     */
    Double delay = 0.0;


    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Location(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location(double x, double y, double z, double delay) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.delay = delay;
    }

    public Location(int M, int N, double delay) {
        this.x = Main.v * (Main.t + delay) + M * Main.dIntraOrbit;
        this.y = N * Main.dInterOrbit;
        this.z = Main.H;
        this.delay = delay;
    }

    /**
     * 计算距离
     *
     * @param l1
     * @param l2
     * @return
     */
    public static Double calDistance(Location l1, Location l2) {
        double x = Math.pow(l1.getX() - l2.getX(), 2);
        double y = Math.pow(l1.getY() - l2.getY(), 2);
        double z = Math.pow(l1.getZ() - l2.getZ(), 2);
        return Math.pow(x + y + z, 1.0 / 2.0);
    }

    /**
     * des是否在l1,l2所组成长方体的范围内
     *
     * @param l1
     * @param l2
     * @param des
     * @return
     */
    public static boolean isInRange(Location l1, Location l2, Location des) {
        double a = (l1.getX() - des.getX()) * (des.getX() - l2.getX());
        double b = (l1.getY() - des.getY()) * (des.getY() - l2.getY());
        return a >= 0 && b >= 0;
    }
}

/**
 * 高空平台
 */
class HighPlatform {

    public HighPlatform(Location location) {
        this.location = location;
    }

    StringBuilder sb = new StringBuilder();
    Location location;
}