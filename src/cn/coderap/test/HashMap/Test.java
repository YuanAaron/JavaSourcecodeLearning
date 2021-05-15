package cn.coderap.test.HashMap;

import java.util.HashMap;

public class Test {
    public static void main(String[] args) {
        HashMap<String, Double> map = new HashMap<>(1);
        map.put("k1",1.0);
        map.put("k2",2.0);
        map.put("k3",3.0);
        HashMap<String, Double> map1 = new HashMap<>(1);
//        map1.put("k4",4.0);
        map1.putAll(map);
    }

}
