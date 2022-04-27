package Judge;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Judge {
    static double t = 0.0;

    public static void main(String[] args) {
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
        Scanner sc = new Scanner(System.in);
        for (int i = 0; i < 72; i++) {
            String[] split = sc.nextLine().split(",");

            Base start = bases.get(Integer.parseInt(split[1]));
            Base end = bases.get(Integer.parseInt(split[2]));
            t = Double.parseDouble(split[0]);

            String s = sc.nextLine();
            String[] strings = s.toString().split("\\)" + "," + "\\(");
            Location before = start.getLocation();
            Location after = null;
            double delay = 0;
            for (int j = 0; j < strings.length; j++) {
                String ss = strings[j];
                ss = ss.replaceAll("\\(|\\)", "");
                String[] aa = ss.split(",");
                if (aa.length == 3)
                    after = new Location(Integer.parseInt(aa[1]), Integer.parseInt(aa[2]), Double.parseDouble(aa[0]));
                else {
                    after = highPlatformList.get(Integer.parseInt(aa[1])).location;
                }

                if (Location.calDistance(before, after) > 115
                        || computeDelay(before, after) - (Double.parseDouble(aa[0]) - delay) >0.001) {
                    System.out.println(computeDelay(before, after) + "  " + (Double.parseDouble(aa[0]) - delay)+"   "+delay);
                    System.out.println(s);
                    break;
                }
                if (after.delay != 100.0) {
                    after = new Location(Integer.parseInt(aa[1]), Integer.parseInt(aa[2]), Double.parseDouble(aa[0]));
                }
                before = after;
                delay = Double.parseDouble(aa[0]);

            }
        }


    }

    public static Double computeDelay(Location pos1, Location pos2) {
        Double ret = 0.1 + Location.calDistance(pos1, pos2) / 10000.0;
        return ret;
    }

    public static boolean equals(double a, double b) {
        if (Math.abs(a - b) < Math.pow(1, -4)) {
            return true;
        }
        return false;
    }

}
