/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import sun.misc.SharedSecrets;

//翻译参考自：
//https://zhuanlan.zhihu.com/p/38141571
//https://blog.csdn.net/Luyanc/article/details/100760920
/**
 * Hash table based implementation of the <tt>Map</tt> interface.  This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  (The <tt>HashMap</tt>
 * class is roughly equivalent to <tt>Hashtable</tt>, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 * 基于哈希表实现了Map接口，这个实现类提供了map接口所有的方法，而且它允许key和values都为空。
 * (也就是说HashMap和HashTable大致相当，除了它是线程不安全的且允许key和values值为空)
 * 这个类不能保证映射的顺序；通常，它也不能保证随着时间的变化，顺序保持不变。
 * ==========================
 * 这段的意思是说我们往hashmap集合里通过put方法依次存入5个键值对的时候
 * （简化起见，用1、2、3、4、5来表示），显示结果可能是2,1,4,5,3(不能保证映射顺序)。
 * 在继续往集合添加元素的时候，当数组长度因扩容而改变时，原来数据的会重新分配位置，
 * 这个时候原来的2,1,4,5,3的顺序很可能会改变。
 *
 * 要点：允许key及value为空；由于扩容后的rehash计算，原来的结构顺序会被改变
 *
 * <p>This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * <tt>HashMap</tt> instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 * 假设哈希函数能够把元素合理的分配在各个桶中，HashMap对这些基本操作(get和put)的实现能达到常量级的性能。
 * 迭代整个容器需要的时间正比与HashMap实例的capacity(桶的数量)与它的size(键值对的数量)之和(???)。因此，如果
 * 注重迭代性能，切记不要将初始容量设太高(或将负载因子设太低)。
 *
 * 要点：初始化容量及加载因子要按实际情况设定。（一般不会改变加载因子的大小）
 *
 * <p>An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets.
 * 一个HashMap实例的性能受两个参数的影响：初始容量和负载因子。容量是哈希表中桶的数量，初始容量
 * 仅仅是哈希表被创建时的容量。负载因子是允许哈希表达到多满才可以自动扩容的一个衡量标准。当哈希表中
 * entries的数量超过了负载因子和当前容量的乘积时，哈希表会进行rehash(也就是说，内部数据结构会重建)，
 * 导致哈希表中桶的数量翻倍。
 *
 * 要点：介绍扩容机制，触发扩容条件后，数组长度扩容为原来的两倍。
 *
 * <p>As a general rule, the default load factor (.75) offers a good
 * tradeoff between time and space costs.  Higher values decrease the
 * space overhead but increase the lookup cost (reflected in most of
 * the operations of the <tt>HashMap</tt> class, including
 * <tt>get</tt> and <tt>put</tt>).  The expected number of entries in
 * the map and its load factor should be taken into account when
 * setting its initial capacity, so as to minimize the number of
 * rehash operations.  If the initial capacity is greater than the
 * maximum number of entries divided by the load factor, no rehash
 * operations will ever occur.
 * 通常来说，默认负载因子0.75是时间和空间开销上的一个很好的折衷。较高的值虽然会减少空间的开销，
 * 但是会增加查询成本(反映在HashMap的大多数操作上，包括get和put)。在设置HashMap的初始容量时，
 * 应该考虑将要放入map中的entries数量和负载因子，以便最小化rehash操作的次数。如果初始容量大于预计
 * 的entries数量除以负载因子，就不会发生rehash操作。
 *
 * 要点：介绍加载因子为什么默认取值为0.75，时间和空间上的折衷。
 *
 * <p>If many mappings are to be stored in a <tt>HashMap</tt>
 * instance, creating it with a sufficiently large capacity will allow
 * the mappings to be stored more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.  Note that using
 * many keys with the same {@code hashCode()} is a sure way to slow
 * down performance of any hash table. To ameliorate impact, when keys
 * are {@link Comparable}, this class may use comparison order among
 * keys to help break ties.
 * 如果有大量的映射要存储到HashMap中，在创建实例时指定足够大的容量比按需自动扩容更高效。
 * 需要注意的是，当很多keys有相同的hashcode值时，这无疑会降低哈希表的性能。为了降低影响，
 * 当key实现了Comparable接口时，这个类就能在这些key之间通过比较来降低这种影响。(？？？)
 *
 * 要点：如果我们创建的HashMap对象要存储大量的数据，应该给定一个初始容量，不然内部会一直扩容，降低性能；
 * 另外，如果有很多key的哈希值相同，有必要让这些key值实现Comparable接口。
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 * 注意，HashMap是线程不安全的。如果多个线程并发访问hashmap，且至少一个线程结构性地修改了map，
 * 必须在外部实现同步(结构修改指的是添加或删除一个或多个映射，仅仅更改已经存在的key的映射值并
 * 不是结构修改)。这通常是通过对封装了这个map的某个对象同步来实现的。
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 * 如果不存在这样的对象，那么就应该使用Collections.synchronizedMap方法来包装这个map对象。为了防止
 * 偶然异步访问这个map,最好在创建的时候就使用Map m=Collections.synchronizedMap(new HashMap(...));
 *
 * 要点：HashMap是一个线程不安全的类，如果要保证它的线程安全，应该使用方法
 * Collections.synchronizedMap(new HashMap(...))来进行包装
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 * 该类的集合遍历方法返回的迭代器是fail-fast机制的：在迭代器创建之后，除了通过迭代器自己的remove方法，
 * 无论任何对map进行结构性修改，迭代器都会抛出并发修改异常(expectedModCount和modCount对不上的时候
 * 抛出的异常)。当面对并发修改的时候，迭代器会快速明了的报错，而不会冒着在未来的某个不确定时间的做任何不确定行为的风险。
 *
 * fail-fast就是“快速失败”，它是Java集合中的一种错误检测机制。某个线程在对集合进行迭代时，不允许其他线程对该集合进行结构上的修改。
 * 例如：假设存在两个线程（线程1、线程2），线程1通过Iterator在遍历集合A中的元素，在某个时候线程2对集合A进行了结构上的修改，那么这个
 * 时候程序就会抛出ConcurrentModificationException 异常，从而产生fail-fast。
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 * 注意，迭代器的快速失败行为不能得到保证，一般来说，在异步并发修改时，不可能作出任何坚决的保证。
 * 快速失败迭代器尽最大努力抛出并发修改异常。因此，编写一个依赖于此异常达到正确性的程序的做法是错误的：
 * 迭代器的快速失败行为应该仅用于检测程序bugs。
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
 */
// 实现了Cloneable,可以被克隆；
// 实现了Serializable,可以被序列化;
// 继承自AbstractMap,实现了Map接口，具有Map的所有功能
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable { //在语法层面继承接口Map是多余的，这么做仅仅是为了让阅读代码的人明确知道HashMap是属于Map体系的，起到了文档的作用

    private static final long serialVersionUID = 362498820763181265L;

    /*
     * Implementation notes.
     *
     * This map usually acts as a binned (bucketed) hash table, but
     * when bins get too large, they are transformed into bins of
     * TreeNodes, each structured similarly to those in
     * java.util.TreeMap. Most methods try to use normal bins, but
     * relay to TreeNode methods when applicable (simply by checking
     * instanceof a node).  Bins of TreeNodes may be traversed and
     * used like any others, but additionally support faster lookup
     * when overpopulated. However, since the vast majority of bins in
     * normal use are not overpopulated, checking for existence of
     * tree bins may be delayed in the course of table methods.
     * 这个map通常是一个以桶为基础的哈希表，但是当桶中的元素变得太多时，它会转变成以TreeNodes
     * 为结构的桶，每个桶的结构类似于TreeMap。大部分方法尝试使用普通桶，但合适的时候(仅仅检查
     * 是不是node的实例)，这些方法会转变成TreeNode方法。以TreeNodes为结构的桶像其他的一样
     * 被遍历和使用，但是当桶中的元素过多时，该桶还支持更快速的查找。然而，由于大部分桶在通常情况
     * 下元素不会过多，因此在使用哈希表方法的过程中，检查树状桶的存在可能会被延后。
     *
     * Tree bins (i.e., bins whose elements are all TreeNodes) are
     * ordered primarily by hashCode, but in the case of ties, if two
     * elements are of the same "class C implements Comparable<C>",
     * type then their compareTo method is used for ordering. (We
     * conservatively check generic types via reflection to validate
     * this -- see method comparableClassFor).  The added complexity
     * of tree bins is worthwhile in providing worst-case O(log n)
     * operations when keys either have distinct hashes or are
     * orderable, Thus, performance degrades gracefully under
     * accidental or malicious usages in which hashCode() methods
     * return values that are poorly distributed, as well as those in
     * which many keys share a hashCode, so long as they are also
     * Comparable. (If neither of these apply, we may waste about a
     * factor of two in time and space compared to taking no
     * precautions. But the only known cases stem from poor user
     * programming practices that are already so slow that this makes
     * little difference.)
     * 同一个树状桶（桶中的元素都是TreeNodes）中的树节点主要按hashCode排序(算出自己在数组中的下标)，但是在hashCode相同
     * 的情况下，如果两个元素所属的类都实现了Comparable接口，他们的compareTo方法会被用来排序。
     * （我们会谨慎地通过反射检查泛型信息(即键所属的类)来进行验证——详见方法comparableClassFor）
     * 在keys具有不同的哈希值或可以排序的情况下，都保证各操作最坏情形的复杂度O(log n)，所以树状桶增加的复杂度是值得的。
     * 因此，在意外或者恶意使用的情况下，hashCode()方法返回值分布很糟糕，甚至许多keys共享一个hashCode，只要Keys具有可比性，性能的下降会是平滑的。
     * (如果这两种方法都不适用(不同的哈希码或者可以排序)，与不采取预防措施相比，我们可能会浪费大约两倍的时间和空间。但目前所知的唯一案例来自于
     * 糟糕的用户编程实践，这些实践已经非常缓慢，以至于没有什么区别)
     *
     * Because TreeNodes are about twice the size of regular nodes, we
     * use them only when bins contain enough nodes to warrant use
     * (see TREEIFY_THRESHOLD). And when they become too small (due to
     * removal or resizing) they are converted back to plain bins.  In
     * usages with well-distributed user hashCodes, tree bins are
     * rarely used.  Ideally, under random hashCodes, the frequency of
     * nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average for the default resizing
     * threshold of 0.75, although with a large variance because of
     * resizing granularity. Ignoring variance, the expected
     * occurrences of list size k are (exp(-0.5) * pow(0.5, k) /
     * factorial(k)). The first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     * 因为TreeNode的大小约为普通节点的两倍，所以只有当桶包含足够多的节点时才会使用TreeNodes
     * (是否足够多由TREEIFY_THRESHOLD决定)。当桶中的节点数变得太少(由remove或resize操作导致)时，
     * 又会转成普通桶。使用离散性好的用户hashCode，树型桶很少使用到(我的理解：元素均匀分布在每个桶中，
     * 几乎不会有桶中的链表长度达到阈值)。理想情况下，在随机hashCode下，对于默认的负载因子0.75，所有桶中
     * 节点的分布频率服从参数约为0.5的泊松分布，尽管粒度调整会产生较大方差。忽略方差，列表大小k(桶中元素个数)的
     * 预期出现概率是（exp（-0.5）* pow（0.5，k）/ factorial（k））。
     *
     * 从上面的表中可以看到，链表中元素个数超过８个的概率非常非常小，所以链表转换红黑树的阈值选择了8。
     *
     * The root of a tree bin is normally its first node.  However,
     * sometimes (currently only upon Iterator.remove), the root might
     * be elsewhere, but can be recovered following parent links
     * (method TreeNode.root()).
     *
     * All applicable internal methods accept a hash code as an
     * argument (as normally supplied from a public method), allowing
     * them to call each other without recomputing user hashCodes.
     * Most internal methods also accept a "tab" argument, that is
     * normally the current table, but may be a new or old one when
     * resizing or converting.
     *
     * When bin lists are treeified, split, or untreeified, we keep
     * them in the same relative access/traversal order (i.e., field
     * Node.next) to better preserve locality, and to slightly
     * simplify handling of splits and traversals that invoke
     * iterator.remove. When using comparators on insertion, to keep a
     * total ordering (or as close as is required here) across
     * rebalancings, we compare classes and identityHashCodes as
     * tie-breakers.
     *
     * The use and transitions among plain vs tree modes is
     * complicated by the existence of subclass LinkedHashMap. See
     * below for hook methods defined to be invoked upon insertion,
     * removal and access that allow LinkedHashMap internals to
     * otherwise remain independent of these mechanics. (This also
     * requires that a map instance be passed to some utility methods
     * that may create new nodes.)
     *
     * The concurrent-programming-like SSA-based coding style helps
     * avoid aliasing errors amid all of the twisty pointer operations.
     */

    /**
     * The default initial capacity - MUST be a power of two.
     * 默认的初始容量为16-必须是2的幂次(让不同hash值通过hash & (n-1)尽量计算出不同的下标，
     * 比如对于hash值1011、1101，使用1111就可以区分，而使用1001就不能区分)
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     * 最大容量为2的30次方(必须是2的幂次)
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     * 默认的负载因子
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     * 树化条件一：当一个桶中的元素个数大于8时进行树化
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     * 当一个桶中的元素个数小于6时把树转化为链表
     * 在扩容时，如果桶中元素个数小于UNTREEIFY_THRESHOLD，就会把树形的桶元素还原为链表结构。
     * 这个值应该比TREEIFY_THRESHOLD小，至多为6以便remove时与降低检测相啮合。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     * 树化条件二：当桶的个数达到64的时候才进行树化
     * MIN_TREEIFY_CAPACITY是最小的桶可能树化的哈希表容量(否则，如果桶内节点过多时，哈希表扩容)。
     * 为了避免扩容阈值、树化阈值之间的冲突，这个值不能小于4 * TREEIFY_THRESHOLD
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
     */
    //典型的单链表节点
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash; //用来存储key计算得来的hash值
        final K key;
        V value;
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
    static final int hash(Object key) {
        int h;
        //如果key为null，则hash值为0；否则，调用key的hashCode方法，
        //并让高16位与整个hash值异或，这样做是为了使计算出的hash更分散(
        //比如两个hash值的低16位相同，而高16位不同，那么他们通过hash & (n-1)
        //大概率会计算出两个相同的下标；但是如果让这两个hash值的低高16位进行异或后，
        //再通过hash & (n-1)大概率会计算两个不同的下标)
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */ //对象x的类是C,如果C实现了Comparable<C>接口，那么返回x的Class，否则返回null
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) { //如果x是Comparable类的对象(直接或间接子类、接口实现类)
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            //继续判断x的类是否实现了Comparable<x的类>接口
            //如果x是个字符串，直接返回c。原因是查看String类的定义可知，String实现了Comparable<String>接口
            if ((c = x.getClass()) == String.class) // bypass checks 如果对象x是个字符串
                return c; //返回String.class
            if ((ts = c.getGenericInterfaces()) != null) { //如果对象x不是字符串，通过反射获取对象x的类直接实现的接口（如果是泛型接口,附带泛型信息）
                for (int i = 0; i < ts.length; ++i) { //遍历接口数组
                    //如果当前接口t是个泛型接口，且该泛型接口的原始类型是Comparable，且该泛型接口只有一个泛型参数，且该泛型参数的类是c
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                        ((p = (ParameterizedType)t).getRawType() ==
                         Comparable.class) &&
                        (as = p.getActualTypeArguments()) != null &&
                        as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */ //如果x所属的类是kc，返回k.compareTo(x)的比较结果；如果x为null或者其所属的类不是kc，返回0
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    //扩容门槛为传入的初始容量往上取最近的2的n次方。
    //比如，6(110)->8(111+1) 或 9(1001)->16(1111+1) 规律：从二进制左边第一个不为0的位开始全部变成1，然后再加1
    static final int tableSizeFor(int cap) {
        int n = cap - 1; //如果不减1，传入16会得到32
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /* ---------------- Fields -------------- */

    /**
     * The table, initialized on first use, and resized as
     * necessary. When allocated, length is always a power of two.
     * (We also tolerate length zero in some operations to allow
     * bootstrapping mechanics that are currently not needed.)
     * 数组，又叫做桶(buckets)
     */
    transient Node<K,V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     * 作为entrySet()的缓存
     */
    transient Set<Map.Entry<K,V>> entrySet;

    /**
     * The number of key-value mappings contained in this map.
     * 元素的数量
     */
    transient int size;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     * 修改次数，用于在迭代的时候执行快速失败策略
     * 参考：
     * https://blog.csdn.net/u012926924/article/details/50452411
     * https://www.cnblogs.com/nevermorewang/p/7808197.html
     * https://juejin.im/post/5e74413cf265da574c569935
     */
    transient int modCount;

    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
    // 当size达到多少时进行扩容(threshold=capacity * loadFactor)
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    // 负载因子
    final float loadFactor;

    /* ---------------- Public operations -------------- */

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    //判断传入的初始容量和负载因子是否合法，并计算扩容门槛(扩容门槛为传入的初始容量往上取最近的2的n次方。)
    public  HashMap(int initialCapacity, float loadFactor) {
        //检查传入的初始容量是否合法
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        //检查负载因子是否合法
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        //计算扩容门槛
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        //将参数m中的所有数据存入到当前的HashMap中去
        putMapEntries(m, false);
    }

    /**
     * Implements Map.putAll and Map constructor.
     *
     * @param m the map
     * @param evict false when initially constructing this map, else
     * true (relayed to method afterNodeInsertion).
     *
     * 参考：https://blog.csdn.net/anlian523/article/details/103639094
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        int s = m.size();
        if (s > 0) {
            //table未初始化,说明是拷贝构造方法来调用的putMapEntries，或者其他构造方法调用后还没放任何元素就直接调用putAll
            if (table == null) { // pre-size
                //用s计算保存m需要的容量(括号内可能计算出小数，而容量必须向上取整，因此要加1.0F)
                float ft = ((float)s / loadFactor) + 1.0F;
                //如果计算得到的容量小于最大容量，就进行截断；否则就赋值为最大容量
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                         (int)ft : MAXIMUM_CAPACITY);
                //如果计算得到的容量大于当前容量(threshold实际存放的是capacity的值)，重新计算扩容门槛
                //table还没有初始化时，用户给定的capacity会暂存到threshold，因为HashMap没有成员叫做capacity，
                //capacity是作为table数组的大小而隐式存在的
                if (t > threshold)
                    threshold = tableSizeFor(t);
            }
            //table已初始化，说明是其他构造方法调用后，加入过元素，然后调用了putAll
            //如果m元素个数大于扩容门槛，进行扩容处理(这种情况属于预先扩容，然后put元素，体现了HashMap的"懒汉模式")
            else if (s > threshold)
                resize();
            //将m中的所有元素添加至HashMap中
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * Implements Map.get and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @return the node, or null if none
     */
    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k; //first: 桶中的第一个元素
        //如果桶的数量大于0并且待查找的key所在的桶的第一个元素不为空
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            //看第一个元素是不是要查找的元素，如果是直接返回
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            //该条件成立，表示该桶中不止一个元素（存在哈希碰撞）
            if ((e = first.next) != null) {
                //如果第一个元素是树节点，则按树的方式查找
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                //否则，遍历整个链表查找该元素
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    //添加元素的入口
    public V put(K key, V value) {
        //调用hash(key)计算出key的hash值
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Implements Map.put and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        //如果桶的数量为0，则初始化桶
        if ((tab = table) == null || (n = tab.length) == 0)
            //第一次调用putVal时才调用resize()初始化(懒初始化)
            n = (tab = resize()).length;
        //(n-1) & hash用于计算元素在哪个桶中
        //如果这个桶中还没有元素，则把这个元素放在桶中的第一个位置(没有发生哈希碰撞)
        if ((p = tab[i = (n - 1) & hash]) == null)
            //新建一个节点放在桶中
            tab[i] = newNode(hash, key, value, null);
        else {
            //如果桶中已经存在元素了(发生了哈希碰撞)
            Node<K,V> e; K k;
            //如果这个桶中第一个元素的key与待插入元素的key相同，将该元素保存到e中，用于后续修改value值
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            else if (p instanceof TreeNode)
                //如果第一个元素是树节点(判断链表是否已经转变成了红黑树)，则调用树节点的putTreeVal插入元素
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                //遍历这个桶对应的链表，binCount用于存储链表中元素的个数
                for (int binCount = 0; ; ++binCount) {
                    //如果链表遍历完了都没有找到相同key的元素，说明该key对应的元素不存在，则在链表最后插入一个新节点。
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        //如果插入新节点后链表长度大于8，则判断是否需要树化(因为第一个元素没有加入到binCount中，所以这里-1)
                        //其实是第9个元素添加后，会暂时出现9个节点的链表，然后才会调用treeifyBin方法
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            //树化：链表转化成红黑树
                            treeifyBin(tab, hash);
                        break;
                    }
                    //如果待插入的key在链表中找到了，退出循环
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            //如果找到了对应key的元素
            if (e != null) { // existing mapping for key
                //记录下旧值
                V oldValue = e.value;
                //判断是否需要替换旧值
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                //在节点被访问后做点什么事，在LinkedHashMap中用到
                afterNodeAccess(e);
                //返回旧值
                return oldValue;
            }
        }
        //到这里说明没有找到对应key的元素(新插入了元素)
        //添加节点次数加1
        ++modCount;
        //元素数量加1，判断是否需要扩容
        if (++size > threshold)
            //扩容
            resize();
        //在节点插入后做点什么事，在LinkedHashMap中用到
        afterNodeInsertion(evict);
        //没有找到对应key的元素(插入了元素)，返回null
        return null;
    }

    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */
    final Node<K,V>[] resize() {
        //旧数组
        Node<K,V>[] oldTab = table;
        //旧容量
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        //旧扩容门槛
        int oldThr = threshold;
        int newCap, newThr = 0;
        //该条件成立表示table已经初始化过了，即这是一次正常的扩容
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                //如果旧容量达到了最大容量，则不再进行扩容
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                //如果旧容量的两倍小于最大容量且旧容量大于默认初始容量(16)，则容量扩大为2倍，扩容门槛也扩大为2倍
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            //调用非默认构造方法创建map,第一次插入元素会走到这里
            //如果旧容量为0且旧扩容门槛大于0，则把新容量赋值为旧扩容门槛
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            //调用默认构造方法创建map，第一次插入元素会走到这里
            //如果旧容量、旧扩容门槛都是0，说明还未初始化，则初始化容量为默认容量，扩容门槛为默认容量*默认负载因子
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        //当oldCap < DEFAULT_INITIAL_CAPACITY || oldThr > 0时，新扩容门槛为0
        //如果新扩容门槛为0，则计算为容量*装载因子，但不能超过最大容量
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        //赋值扩容门槛为新门槛
        threshold = newThr;

        //新建一个新容量的数组
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        //把新数组赋值给旧数组
        table = newTab;
        //如果旧数组不为空，则搬移元素
        if (oldTab != null) {
            //遍历旧数组
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                //如果这个桶中第一个元素不为空(一个元素、链表、树)，赋值给e
                if ((e = oldTab[j]) != null) {
                    //清空这个旧桶，便于gc
                    oldTab[j] = null;
                    //如果这个桶中只有一个元素(没有发生哈希碰撞)，则计算它在新桶中的位置并把它搬移到新桶中
                    //因为每次都扩容两倍，所以这个桶中的元素搬移到新桶的时候，新桶肯定还没有元素(即扩容只会缓解哈希碰撞，而不会导致新的哈希碰撞)
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        //如果这个桶的第一个元素是树节点，则把这棵树打散成两棵树插入到新数组中去
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        //如果链表不止一个元素且不是一棵树，则分化成两个链表插入到新的桶中去
                        //假如原来容量为4，hash值为3、7、11、15这四个元素都在三号桶中；
                        //现在扩容到8，则3和11还是在三号桶，7和15要搬移到七号桶中去
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            //(e.hash & oldCap)==0的元素放在低位链表中(存储在原来的桶中)
                            //比如，3 & 4==0 11 & 4=0
                            //深入分析e.hash & oldCap：它其实是在判断e.hash中oldCap的最高位的对应位是0还是1。如果该位上是0，
                            //那么根据e.hash & (newCap-1)==0，就认为扩容不会改变e.hash所在的桶；如果该位是1，那么认为扩容
                            //会改变e.hash所在的桶(变为原来所在的桶+旧容量)
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                //其他元素放在高位链表(存储在原来桶的位置加旧容量的桶中)
                                //比如7 & 4=4，15 & 4=4
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        //遍历完成后，分化成两个链表
                        //低位链表在新桶中的位置与旧桶一样(3和11还在三号桶中)
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        //高位链表在新桶中的位置正好是原来的位置加上旧容量(7和15搬移到七号桶了）
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

    /**
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     * 树化
     */
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        //如果桶数量小于64，直接扩容而不用树化。因为扩容之后，链表会分化成两个链表，达到减少元素的作用。
        //当然也不一定，比如初始容量为16，桶1中存放的全是除以32余1的元素(比如33，65等)，这样即使扩容为
        //32也无法减少链表的长度，直到再次扩容为64后，树化后效率就上去了
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        //tab不为null且其长度大于等于64(实际测试发现，极端情况下（前面添加的所有元素都进入了同一个桶），添加完第11个节点才会转红黑树)
        else if ((e = tab[index = (n - 1) & hash]) != null) { //链表第一个节点
            TreeNode<K,V> hd = null, tl = null; //定义头节点，尾节点
            //把Node节点单链表转换成TreeNode节点双向链表
            do {
                TreeNode<K,V> p = replacementTreeNode(e, null); //将该节点转换为树节点
                if (tl == null) //如果尾节点为空，即空链表插入节点
                    hd = p; //当前节点赋值给头节点
                else { //尾节点不为空，即双向链表尾部插入节点
                    p.prev = tl; //当前节点的前一个节点指向尾节点
                    tl.next = p; //尾节点的下一个节点指向当前节点
                }
                tl = p; //当前节点赋值给尾节点
            } while ((e = e.next) != null);
            //用转换后的双向链表替换原来的单向链表
            if ((tab[index] = hd) != null)
                hd.treeify(tab); //如果进入过上面的循环，则把以hd为头节点的双向链表树化
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V remove(Object key) {
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
    }

    /**
     * Implements Map.remove and related methods.
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     */
    final Node<K,V> removeNode(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;
        //如果桶的数量大于0且待删除元素所在的桶的第一个元素不为空
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {
            Node<K,V> node = null, e; K k; V v;
            //第一种情况：如果第一个元素正好就是要找的元素，赋值给node变量，后续删除使用
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            //如果该桶中不止一个元素（发生了哈希碰撞）
            else if ((e = p.next) != null) {
                //第二种情况：如果第一个元素是树节点，则以树的方式查找节点
                if (p instanceof TreeNode)
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    //第三种情况：否则，遍历整个链表查找元素
                    do {
                        if (e.hash == hash &&
                            ((k = e.key) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            //如果找到了键对象为key的元素，则看是否需要匹配value值。如果不需要匹配，直接删除；如果需要匹配，则看找到的节点的value值是否与传入的value相等
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {
                //第一种情况：如果是树节点，调用树的删除方法（以node调用的，是删除自己）
                if (node instanceof TreeNode)
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                //第二种情况：如果待删除的元素是该桶的第一个元素，则把第二个元素移到第一的位置
                else if (node == p)
                    tab[index] = node.next;
                else //第三种情况：删除中的node节点(p是node的前一个节点)
                    p.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        Node<K,V>[] tab;
        modCount++;
        if ((tab = table) != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i)
                tab[i] = null;
        }
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    public boolean containsValue(Object value) {
        Node<K,V>[] tab; V v;
        if ((tab = table) != null && size > 0) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    if ((v = e.value) == value ||
                        (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     *
     * @return a set view of the keys contained in this map
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet();
            keySet = ks;
        }
        return ks;
    }

    final class KeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<K> iterator()     { return new KeyIterator(); }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super K> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.key);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a view of the values contained in this map
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    final class Values extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<V> iterator()     { return new ValueIterator(); }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super V> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    // Overrides of JDK8 Map extension methods

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Node<K,V> e; V v;
        if ((e = getNode(hash(key), key)) != null &&
            ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        V v = mappingFunction.apply(key);
        if (v == null) {
            return null;
        } else if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        }
        else if (t != null)
            t.putTreeVal(this, tab, hash, key, v);
        else {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        Node<K,V> e; V oldValue;
        int hash = hash(key);
        if ((e = getNode(hash, key)) != null &&
            (oldValue = e.value) != null) {
            V v = remappingFunction.apply(key, oldValue);
            if (v != null) {
                e.value = v;
                afterNodeAccess(e);
                return v;
            }
            else
                removeNode(hash, key, null, false, true);
        }
        return null;
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        V oldValue = (old == null) ? null : old.value;
        V v = remappingFunction.apply(key, oldValue);
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
        }
        else if (v != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, v);
            else {
                tab[i] = newNode(hash, key, v, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        if (old != null) {
            V v;
            if (old.value != null)
                v = remappingFunction.apply(old.value, value);
            else
                v = value;
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
            return v;
        }
        if (value != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, value);
            else {
                tab[i] = newNode(hash, key, value, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K,V>[] tab;
        if (function == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /* ------------------------------------------------------------ */
    // Cloning and serialization

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     * 浅拷贝
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        HashMap<K,V> result;
        try {
            result = (HashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    // These methods are also used when serializing HashSets
    final float loadFactor() { return loadFactor; }
    final int capacity() {
        return (table != null) ? table.length :
            (threshold > 0) ? threshold :
            DEFAULT_INITIAL_CAPACITY;
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }

    /**
     * Reconstitutes this map from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                                             loadFactor);
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                                             mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float)mappings / lf + 1.0f;
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                       DEFAULT_INITIAL_CAPACITY :
                       (fc >= MAXIMUM_CAPACITY) ?
                       MAXIMUM_CAPACITY :
                       tableSizeFor((int)fc));
            float ft = (float)cap * lf;
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                         (int)ft : Integer.MAX_VALUE);

            // Check Map.Entry[].class since it's the nearest public type to
            // what we're actually creating.
            SharedSecrets.getJavaOISAccess().checkArray(s, Map.Entry[].class, cap);
            @SuppressWarnings({"rawtypes","unchecked"})
            Node<K,V>[] tab = (Node<K,V>[])new Node[cap];
            table = tab;

            // Read the keys and values, and put the mappings in the HashMap
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                    K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                    V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

    /* ------------------------------------------------------------ */
    // iterators

    abstract class HashIterator {
        Node<K,V> next;        // next entry to return
        Node<K,V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K,V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K,V> nextNode() {
            Node<K,V>[] t;
            Node<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class KeyIterator extends HashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().key; }
    }

    final class ValueIterator extends HashIterator
        implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class EntryIterator extends HashIterator
        implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }

    /* ------------------------------------------------------------ */
    // spliterators

    static class HashMapSpliterator<K,V> {
        final HashMap<K,V> map;
        Node<K,V> current;          // current node
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        int expectedModCount;       // for comodification checks

        HashMapSpliterator(HashMap<K,V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                HashMap<K,V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K,V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                                        expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<V> {
        ValueSpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    static final class EntrySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K,V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support


    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */

    // Create a regular (non-tree) node
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> next) {
        return new Node<>(hash, key, value, next);
    }

    // For conversion from TreeNodes to plain nodes
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        return new Node<>(p.hash, p.key, p.value, next);
    }

    // Create a tree bin node
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        return new TreeNode<>(hash, key, value, next);
    }

    // For treeifyBin
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }

    // Called only from writeObject, to ensure compatible ordering.
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        Node<K,V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    // Tree bins

    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
    // 典型的树型节点
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        //prev是链表中的节点，用于在删除元素的时候可以快速找到它的前置节点(???)
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }

        /**
         * Returns root of tree containing this node.
         * 遍历红黑树，最终找到根节点
         */
        final TreeNode<K,V> root() {
            for (TreeNode<K,V> r = this, p;;) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * Ensures that the given root is the first node of its bin.
         */
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
                if (root != first) {
                    Node<K,V> rn;
                    tab[index] = root;
                    TreeNode<K,V> rp = root.prev;
                    if ((rn = root.next) != null)
                        ((TreeNode<K,V>)rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }

        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         *
         * find是TreeNode类的方法，调用该方法的是一个TreeNode对象，该对象就是树上的某个节点，
         * 以该节点作为根节点，查找其所有子孙节点，看看哪个节点能够匹配上给定的键对象。
         *
         * kc是k的类型，该类应该是实现了Comparable<K>接口的，否则应该是null
         */
        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            TreeNode<K,V> p = this;
            do {
                //当前节点的hash值，方向(左边还是右边)，当前节点的键对象
                int ph, dir; K pk;
                TreeNode<K,V> pl = p.left, pr = p.right, q; //q:用来存储并返回找到的对象
                //如果k的hash值h小于当前节点的hash值，后续让k和左孩子节点进行比较
                if ((ph = p.hash) > h)
                    p = pl;
                //如果k的hash值h大于当前节点的hash值，后续让k和右孩子节点进行比较
                else if (ph < h)
                    p = pr;
                //如果k的hash值h和当前节点的hash值相同，并且当前节点的键对象pk和k相等(地址相同或equals相等)，直接返回
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                //到这里，说明hash相同，但是pk和k不相等
                //如果左孩子为空，后续让k和右孩子节点进行比较
                else if (pl == null)
                    p = pr;
                //如果右孩子为空，后续让k和左孩子节点进行比较
                else if (pr == null)
                    p = pl;
                //如果左、右孩子都不为空，需要再进行一轮对比来确定到底该往哪个方向去深入对比
                //这一轮的对比主要是想通过comparable方法来比较k和pk的大小
                else if ((kc != null ||
                          (kc = comparableClassFor(k)) != null) &&
                         (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                // 执行到这里,说明无法通过comparable比较 或者 比较之后还是相等
                // 从右孩子节点递归循环查找，如果找到了匹配的键对象则返回
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                //如果从右孩子节点递归查找后仍未找到，那么从左孩子节点进行下一轮循环
                else
                    p = pl;
            } while (p != null);
            //未找到匹配的节点，返回null
            return null;
        }

        /**
         * Calls find for root node.
         */
        final TreeNode<K,V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         *
         * 用这个方法来比较两个对象，返回值要么大于0，要么小于0，不会为0。也就是说，该方法
         * 一定能确定要插入的节点是在树的左节点还是右节点，不然就无法继续满足二叉树结构了
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                //先比较两个对象的类名，类名是字符串对象，按字符串的比较规则。如果成立，字符串相等(类型相同)；否则，字符串不等(类型不同)
                (d = a.getClass().getName().
                 compareTo(b.getClass().getName())) == 0)
                //如果两个对象是相同类型，比较两者的原生hashcode(比较他们根据内存地址计算得到的hashcode值)
                //如果hashcode相等，返回-1
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                     -1 : 1);
            return d;
        }

        /**
         * Forms tree of the nodes linked from this node.
         * 真正树化的方法:将TreeNode节点双向链表转换成树结构
         */
        final void treeify(Node<K,V>[] tab) {
            TreeNode<K,V> root = null; //树的根节点
            //遍历TreeNode节点双向链表
            for (TreeNode<K,V> x = this, next; x != null; x = next) { //x：当前链表节点，next：下一个节点
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null; //将当前链表节点的左、右节点设为空
                if (root == null) { //如果红黑树还没有根节点
                    x.parent = null; //当前链表节点的父节点设为空
                    x.red = false; //当前链表节点设为黑色
                    root = x; //根节点指向当前链表节点(双向链表的第一个节点)
                }
                else { //如果已存在根节点，插入到红黑树中再自平衡
                    K k = x.key; //获取当前链表节点的key
                    int h = x.hash; //获取当前链表节点的hash值
                    Class<?> kc = null; //key所属的Class
                    //从根节点开始遍历红黑树，查找当前链表节点的插入位置
                    for (TreeNode<K,V> p = root;;) {
                        int dir, ph; //dir: 标识方向(左右)，ph: 当前树节点的hash值
                        K pk = p.key; //当前树节点的key
                        if ((ph = p.hash) > h) //如果当前链表节点的hash值小于当前树节点的hash值
                            dir = -1; //表示当前链表节点会放到当前树节点的左侧
                        else if (ph < h)
                            dir = 1; //...右侧
                        //当前链表节点的hash值等于当前树节点的hash值，尝试是否能够通过Comparable比较两个对象（当前链表节点的键对象和当前树节点的键对象）
                        //要想看能否基于Comparable进行比较，首先要看当前链表节点的键是否实现了Comparable接口，此时就需要用到comparableClassFor方法获取该键的Class(如果实现Comparable接口的话，否则返回null)，然后再通过compareComparables方法来比较两个对象的大小。
                        //如果两个对象不具有compare的资格，或者compare之后仍然没有比较出大小。那么就要通过一个决胜局再比一次，这个决胜局就是tieBreakOrder方法。
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);

                        TreeNode<K,V> xp = p;  //定义父节点，保存当前树节点
                        //如果dir小于等于0：当前链表节点一定放置在当前树节点的左侧，但不一定是该树节点的左子节点，也可能是左子节点的右子节点或者更深层次的节点；
                        //如果dir大于0 ：当前链表节点一定放置在当前树节点的右侧，但不一定是该树节点的右子节点，也可能是右子节点的左子节点或者更深层次的节点。
                        //根据dir的值判断把当前链表节点挂载到当前树节点的左侧或者右侧，如果对应侧的子节点为空，那么进行挂载且重新把树进行平衡，然后就可以对下一个链表节点进行处理了；如果对应侧子节点不为空，那么会以当前树节点的对应侧子节点作为当前树节点，重新确定当前链表节点在当前树节点的左侧还是右侧。
                        if ((p = (dir <= 0) ? p.left : p.right) == null) { //dir==0存在？？？
                            x.parent = xp; //当前链表节点作为当前树节点的子节点(孩子认爹)
                            if (dir <= 0)
                                xp.left = x; //当前链表节点作为当前树节点的左子节点(爹认孩子)
                            else
                                xp.right = x; //当前链表节点作为当前树节点的右子节点
                            //插入后自平衡（默认插入的是红节点）
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            //把最终的红黑树的根节点放在tab[N]位置，因为红黑树经过多次自平衡后，原来双向链表的第一个元素不一定是红黑树根节点了
            moveRootToFront(tab, root);
        }

        /**
         * Returns a list of non-TreeNodes replacing those linked from
         * this node.
         */
        final Node<K,V> untreeify(HashMap<K,V> map) {
            Node<K,V> hd = null, tl = null;
            for (Node<K,V> q = this; q != null; q = q.next) {
                Node<K,V> p = map.replacementNode(q, null);
                if (tl == null)
                    hd = p;
                else
                    tl.next = p;
                tl = p;
            }
            return hd;
        }

        /**
         * Tree version of putVal.
         */
        final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                                       int h, K k, V v) {
            //k的class对象
            Class<?> kc = null;
            //标记是否找到这个key的节点
            boolean searched = false;
            //找到树的根节点(如果父节点不为空，查找红黑树的根节点;否则，当前节点本身就是根节点)
            TreeNode<K,V> root = (parent != null) ? root() : this;
            //从树的根节点开始遍历
            for (TreeNode<K,V> p = root;;) {
                // dir(direction):标记在左边还是右边；ph(p.hash):当前节点的hash值; pk(p.key):当前节点的key对象
                int dir, ph; K pk;
                if ((ph = p.hash) > h) //待插入节点的hash比当前节点hash小
                    dir = -1; //待插入节点应该放置在当前节点的左侧
                else if (ph < h) //待插入节点的hash比当前节点hash大
                    dir = 1; //待插入节点应该放置在当前节点的右侧
                //待插入节点的hash与当前节点的hash相等
                else if ((pk = p.key) == k || (k != null && k.equals(pk))) //如果待插入节点k与当前节点的key相等
                    return p; //说明找到了节点，返回到putVal判断是否需要修改其value值
                //走到这里,说明待插入节点的hash和当前节点的hash相等但是k不相等 待插入节点key(k)和当前节点的key(pk)类型不同
                else if ((kc == null &&
                        //如果k的类实现了Comparable接口，则返回k的Class；否则返回null
                          (kc = comparableClassFor(k)) == null) ||
                        //待插入节点的key(k)和当前节点的key(pk)之间不是同样的类型或比较之后相等
                         (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        //遍历左右子树，找到了直接返回
                        if (((ch = p.left) != null &&
                             (q = ch.find(h, k, kc)) != null) ||
                            ((ch = p.right) != null &&
                             (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    //到这里，说明还没有在红黑树中找到与待插入节点的key(k)相等的节点
                    //用这个方法来比较两个对象(可能类型相同，可能类型不同)，返回值要么大于0，要么小于0，不会为0。
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K,V> xp = p;
                //如果最后确实没找到对应key的元素，则新建一个节点
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    Node<K,V> xpn = xp.next;
                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((TreeNode<K,V>)xpn).prev = x;
                    //插入树节点后平衡
                    //把root节点移动到链表的第一个节点
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         */
        final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            int index = (n - 1) & hash;
            TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
            TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
            if (pred == null)
                tab[index] = first = succ;
            else
                pred.next = succ;
            if (succ != null)
                succ.prev = pred;
            if (first == null)
                return;
            if (root.parent != null)
                root = root.root();
            if (root == null
                || (movable
                    && (root.right == null
                        || (rl = root.left) == null
                        || rl.left == null))) {
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            TreeNode<K,V> p = this, pl = left, pr = right, replacement;
            if (pl != null && pr != null) {
                TreeNode<K,V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                    s = sl;
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors
                TreeNode<K,V> sr = s.right;
                TreeNode<K,V> pp = p.parent;
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                }
                else {
                    TreeNode<K,V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;
                if (sr != null)
                    replacement = sr;
                else
                    replacement = p;
            }
            else if (pl != null)
                replacement = pl;
            else if (pr != null)
                replacement = pr;
            else
                replacement = p;
            if (replacement != p) {
                TreeNode<K,V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }

            TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);

            if (replacement == p) {  // detach
                TreeNode<K,V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            if (movable)
                moveRootToFront(tab, r);
        }

        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *
         * @param map the map
         * @param tab the table for recording bin heads
         * @param index the index of the table being split
         * @param bit the bit of hash to split on
         */
        final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
            TreeNode<K,V> b = this;
            // Relink into lo and hi lists, preserving order
            TreeNode<K,V> loHead = null, loTail = null;
            TreeNode<K,V> hiHead = null, hiTail = null;
            int lc = 0, hc = 0;
            for (TreeNode<K,V> e = b, next; e != null; e = next) {
                next = (TreeNode<K,V>)e.next;
                e.next = null;
                if ((e.hash & bit) == 0) {
                    if ((e.prev = loTail) == null)
                        loHead = e;
                    else
                        loTail.next = e;
                    loTail = e;
                    ++lc;
                }
                else {
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }

            if (loHead != null) {
                if (lc <= UNTREEIFY_THRESHOLD)
                    tab[index] = loHead.untreeify(map);
                else {
                    tab[index] = loHead;
                    if (hiHead != null) // (else is already treeified)
                        loHead.treeify(tab);
                }
            }
            if (hiHead != null) {
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    tab[index + bit] = hiHead;
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR
        //参考：www.cnblogs.com/finite/p/8251587.html
        // 左旋 root:根节点，p:旋转节点
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                              TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl; //r:旋转节点的右子节点 pp：旋转节点的父节点 rl:旋转节点的右子节点的左子节点
            if (p != null && (r = p.right) != null) { //旋转节点非空且旋转节点的右子节点非空
                if ((rl = p.right = r.left) != null) //将旋转节点的右子节点的左子节点 赋值给 旋转节点的右子节点【这步只完成了爹认孩子】
                    rl.parent = p; //设置rl和左旋节点的父子关系【这步完成了孩子认爹】

                if ((pp = r.parent = p.parent) == null) //将旋转节点的父节点 赋值给 旋转节点的右孩子的父节点【这步完成了孩子认爹】，相当于右孩子提升了一层；如果旋转节点的父节点为空，说明r已是顶层节点，应该作为root且改为黑色
                    (root = r).red = false;
                else if (pp.left == p) //如果pp不为空，并且旋转节点为其左子节点
                    pp.left = r; //设置pp和r的父子关系【这步完成了爹认孩子】
                else //pp不为空，并且旋转节点是其右孩子
                    pp.right = r; //设置pp和r的父子关系【这步完成了爹认孩子】
                r.left = p; //旋转节点作为他的右子节点的左子节点 【这步完成了爹认孩子】
                p.parent = r; //旋转节点的右子节点作为旋转节点的父节点 【这步完成了孩子认爹】
            }
            return root;
        }
        //右旋
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                               TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }
        //红黑树插入平衡算法
        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            x.red = true; //新插入的节点默认为红色
            //xp:当前节点的父节点，xpp:当前节点的爷爷节点，xppl:爷爷节点的左子节点，xppr:爷爷节点的右子节点
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                //如果父节点为空，说明当前节点就是根节点，将其染成黑色即可
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                //父节点不为空
                //如果父节点为黑色 或 父节点为红色且爷爷节点为空(存在？？？)
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                //父节点存在且为红色
                if (xp == (xppl = xpp.left)) {//如果父节点为爷爷节点的左子节点
                    if ((xppr = xpp.right) != null && xppr.red) { //爷爷节点的右子节点(叔叔节点)不为空且为红色
                        xppr.red = false; //叔叔节点改为黑色
                        xp.red = false; //父亲节点改为黑色
                        xpp.red = true; //爷爷节点改为红色
                        x = xpp; //运行到这里之后，就又会进行下一轮的循环了，将爷爷节点设成当前节点继续插入后的自平衡
                    }
                    else { //叔叔节点不存在或为黑色
                        if (x == xp.right) { //如果当前节点是父节点的右子节点(LR红色情况)
                            root = rotateLeft(root, x = xp); //对父节点进行左旋，将父节点设置成当前节点(得到LL红色情况)
                            xpp = (xp = x.parent) == null ? null : xp.parent; //获取爷爷节点
                        }
                        //LL红色情况
                        if (xp != null) { //如果父节点不为空
                            xp.red = false; //将父节点改成黑色
                            if (xpp != null) { //如果爷爷节点不为空
                                xpp.red = true; //将爷爷节点改成红色
                                root = rotateRight(root, xpp); //对爷爷节点右旋
                            }
                        }
                    }
                }
                else {//如果父节点为爷爷节点的右子节点
                    if (xppl != null && xppl.red) { //爷爷节点的左子节点(叔叔节点)不为空且为红色
                        xppl.red = false; //叔叔节点改为黑色
                        xp.red = false; //父亲节点改为黑色
                        xpp.red = true; //爷爷节点改为红色
                        x = xpp; //运行到这里之后，就又会进行下一轮的循环了，将爷爷节点设成当前节点继续插入后的自平衡
                    }
                    else { //叔叔节点不存在或为黑色
                        if (x == xp.left) { //如果当前节点是父节点的左子节点(RL红色情况)
                            root = rotateRight(root, x = xp); //对父节点进行右旋，将父节点设置成当前节点(得到RR红色情况)
                            xpp = (xp = x.parent) == null ? null : xp.parent; //获取爷爷节点
                        }
                        //RR红色情况
                        if (xp != null) { //如果父节点不为空
                            xp.red = false; //将父节点改成黑色
                            if (xpp != null) { //如果爷爷节点不为空
                                xpp.red = true; //将爷爷节点改成红色
                                root = rotateLeft(root, xpp); //对爷爷节点左旋
                            }
                        }
                    }
                }
            }
        }

        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                                   TreeNode<K,V> x) {
            for (TreeNode<K,V> xp, xpl, xpr;;) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }
                else if (x.red) {
                    x.red = false;
                    return root;
                }
                else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                            (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        }
                        else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                    null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                }
                else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                            (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        }
                        else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                    null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                tb = t.prev, tn = (TreeNode<K,V>)t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }
    }

}
