import java.util.ArrayList;
import java.util.List;

/**
 * @author Hppppppf
 * @date 2022/4/24 15:42
 * @description
 */
public class CodeTest {
    public static void main(String[] args) {
        int[] power = {1, 1, 1, 24};
        System.out.println(damage(power));
    }

    public static boolean damage(int[] power) {
        ArrayList<Integer> arr = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            arr.add(power[i]);
        //全排列
        ArrayList<ArrayList<Integer>> arrlist = quanpailie(arr);
        //判断
        for (int i = 0; i < arrlist.size(); i++) {
            ArrayList<Integer> arrTemp = arrlist.get(i);
            if (isOK(arrTemp, arrTemp.get(0), 1) || isOK2(arrTemp)) return true;
        }
        return false;
    }

    public static boolean isOK(ArrayList<Integer> arr, Integer re, int index) {
        for (int i = index; i < arr.size(); i++) {
            if (re + arr.get(i) < 24) {
                return isOK(arr, re + arr.get(i), index + 1);
            }
            if (re - arr.get(i) < 24) {
                return isOK(arr, re + arr.get(i), index + 1);
            }
            if (re * arr.get(i) < 24) {
                return isOK(arr, re + arr.get(i), index + 1);
            }
            if (re > arr.get(i) && re % arr.get(i) == 0 && re / arr.get(i) < 24) {
                return isOK(arr, re + arr.get(i), index + 1);
            }
        }
        if (index == 4 && re + arr.get(3) == 24 || re - arr.get(3) == 24 || re * arr.get(3) == 24 || re / arr.get(3) == 24)
            return true;
        else return false;
    }

    public static boolean isOK2(ArrayList<Integer> arr) {
        List<Integer> l1 = compute(arr.get(0), arr.get(1));
        List<Integer> l2 = compute(arr.get(2), arr.get(3));
        for (int i = 0; i < l1.size(); i++) {
            for (int j = 0; j < l2.size(); j++) {
                if (l1.get(i) + l2.get(j) == 24) return true;
                if (l1.get(i) - l2.get(j) == 24) return true;
                if (l1.get(i) * l2.get(j) == 24) return true;
                if (l2.get(j) != 0 &&  l1.get(i) > l2.get(j) && l1.get(i) % l2.get(j) == 0 && l1.get(i) / l2.get(j) == 24) return true;
            }
        }
        return false;
    }

    public static List<Integer> compute(int a, int b) {
        List<Integer> ret = new ArrayList<>();
        ret.add(a + b);
        ret.add(a - b);
        ret.add(a * b);
        if (a > b && a % b == 0) ret.add(a / b);
        return ret;
    }

    public static ArrayList<ArrayList<Integer>> quanpailie(ArrayList<Integer> arr) {
        ArrayList<ArrayList<Integer>> ret = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int temp = arr.get(i);
            ArrayList<Integer> arr_t = (ArrayList<Integer>) arr.clone();
            arr_t.remove(i);
            ArrayList<ArrayList<Integer>> ret_t = qpl(arr_t, temp);
            for (int j = 0; j < ret_t.size(); j++) {
                ret_t.get(j).add(0, temp);
            }
            ret.addAll(ret_t);
        }
        return ret;
    }

    public static ArrayList<ArrayList<Integer>> qpl(ArrayList<Integer> arr, Integer a) {
        ArrayList<ArrayList<Integer>> ret = new ArrayList<>();
        if (arr.size() == 1) {
            ret.add(arr);
            return ret;
        }
        for (int i = 0; i < arr.size(); i++) {
            int temp = arr.get(i);
            ArrayList<Integer> arr_t = (ArrayList<Integer>) arr.clone();
            arr_t.remove(i);
            ArrayList<ArrayList<Integer>> ret_t = qpl(arr_t, temp);
            for (int j = 0; j < ret_t.size(); j++) {
                ret_t.get(j).add(0, temp);
            }
            ret.addAll(ret_t);
        }
        return ret;
    }
}
