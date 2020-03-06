package com.trxs.commons.jdbc;

import com.trxs.commons.io.FileTools;
import com.trxs.commons.util.LoopPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SQLProvider extends LoopPointer
{
    protected static Logger logger = LoggerFactory.getLogger(SQLProvider.class.getName());

    private int status;

    private String idWithSQL;
    private List<String> sqlLines;

    private Map<String, String> statementMap;

    private String[] keyArrary;
    private String[] sqlArrary;

    public SQLProvider()
    {
        status = 0;
        sqlLines = new ArrayList<>(16);
        statementMap = new ConcurrentHashMap<>(32);
    }

    public void forEach(BiConsumer<? super String, ? super String> action)
    {
        statementMap.forEach( action );
    }

    public String getSQL( String key )
    {
        String sql = statementMap.get(key);
        if ( sql != null ) return sql;
        logger.error("\nCan`t find the SQL with key->{}!!!", key);
        return null;
    }

    private void accept(String line)
    {
        if ( line.startsWith("/*") )
        {
            status = 0;
            return;
        }

        if ( line.endsWith("*/") )
        {
            status = 1;
            return;
        }

        if (status == 0 && line.trim().length() == 0 ) return;
        Objects.requireNonNull(line);

        try
        {
            parsing(line.trim());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void parsing(String line)
    {
        switch (status)
        {
            case 0:
                if ( sqlLines.size() > 0 ) addSQL();
                if ( line.indexOf("ID") == -1  ) break;
                List<String> options = Arrays.asList(line.split(":")).stream().map(option->option.trim()).collect(Collectors.toList());
                if ( options.size() == 2 && options.get(0).toUpperCase().endsWith("ID") )
                    idWithSQL = options.get(1);
                break;
            case 1:
                sqlLines.add(line);
                break;
        }
    }

    private String[] getOptions( String line )
    {
        line.split(":");

        List<String> options = Arrays.asList(line.split(":")).stream().map(option->option.trim()).collect(Collectors.toList());
        if ( options.size() == 2 && options.get(0).toUpperCase().endsWith("ID") )
            idWithSQL = options.get(1);
        return null;
    }

    public void init(String source)
    {
        if ( statementMap.size() > 0 ) return;
        List<String> sqlLines = FileTools.getInstance().readStaticResource(source);
        sqlLines.forEach( line -> { next(); accept(line); });
        addSQL();
    }

    private void addSQL()
    {
        if ( idWithSQL == null || sqlLines.size() == 0 ) return;

        if ( statementMap.containsKey(idWithSQL) )
        {
            System.err.println("\nSQL-ID -> " + idWithSQL + " 重复!!! 在" + currentIndex + "行！！！");
            System.err.println("\nSQL-ID -> " + idWithSQL + " 重复!!! 在" + currentIndex + "行！！！");
            System.err.println("\nSQL-ID -> " + idWithSQL + " 重复!!! 在" + currentIndex + "行！！！");
            return;
        }
        statementMap.put(idWithSQL,String.join("\n",sqlLines));
        sqlLines.clear(); idWithSQL = null;
    }

    public List<String> sortKeys()
    {
        List<String> keys = new ArrayList<>(statementMap.keySet());
        Collections.sort(keys,new ComparatorKey());
        keyArrary = new String[keys.size()];
        sqlArrary = new String[keys.size()];

        keys.toArray(keyArrary);
        LoopPointer loopPointer = new LoopPointer();
        keys.forEach( key -> sqlArrary[loopPointer.next()] = statementMap.get(key));

        return keys;
    }

    private class ComparatorKey implements Comparator<String>
    {

        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.<p>
         * <p>
         * In the foregoing description, the notation
         * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
         * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
         * <tt>0</tt>, or <tt>1</tt> according to whether the value of
         * <i>expression</i> is negative, zero or positive.<p>
         * <p>
         * The implementor must ensure that <tt>sgn(compare(x, y)) ==
         * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
         * implies that <tt>compare(x, y)</tt> must throw an exception if and only
         * if <tt>compare(y, x)</tt> throws an exception.)<p>
         * <p>
         * The implementor must also ensure that the relation is transitive:
         * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies
         * <tt>compare(x, z)&gt;0</tt>.<p>
         * <p>
         * Finally, the implementor must ensure that <tt>compare(x, y)==0</tt>
         * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
         * <tt>z</tt>.<p>
         * <p>
         * It is generally the case, but <i>not</i> strictly required that
         * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
         * any comparator that violates this condition should clearly indicate
         * this fact.  The recommended language is "Note: this comparator
         * imposes orderings that are inconsistent with equals."
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         * first argument is less than, equal to, or greater than the
         * second.
         * @throws NullPointerException if an argument is null and this
         *                              comparator does not permit null arguments
         * @throws ClassCastException   if the arguments' types prevent them from
         *                              being compared by this comparator.
         */
        @Override
        public int compare(String o1, String o2)
        {
            return o1.compareTo(o2);
        }
    }
}
