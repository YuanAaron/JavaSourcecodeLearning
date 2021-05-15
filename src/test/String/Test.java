package test.String;

public class Test {
    public static void main(String[] args) {
        /*默认情况下，两者都是与对象的内存地址有关的，因此o.hashCode()和System.identityHashCode(o)的结果是相同的。
        但是一般来说，我们都会重载hashCode函数，比如String的hashCode重载后，hashcode与字符串的内容有关系。如果我们new
        两个内容相同的String，那么内存地址肯定是不相同的，那么此时怎么才能得到原生的hashCode，这个函数就是干这个事情的。*/
        Object o=new Object();
        //相等
        System.out.println(o.hashCode());
        System.out.println(System.identityHashCode(o));

        String s1=new String("zhangsan");
        String s2=new String("zhangsan");
        //相等
        System.out.println(s1.hashCode());
        System.out.println(s2.hashCode());
        //不相等
        System.out.println(System.identityHashCode(s1));
        System.out.println(System.identityHashCode(s2));
    }
}
