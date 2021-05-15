package cn.coderap.test.Float;

public class Test {
    public static void main(String[] args) {
        System.out.println(Float.isFinite(1.0f/0.0f)); //有限的
        System.out.println(Float.isInfinite(1.0f/0.0f)); //无限的
        System.out.println(Float.isNaN(0.0f/0.0f)); //Not-a-Number,数学上0/0是一种未确定
    }
}
