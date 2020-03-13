package com.fel.compile;

import com.fel.Expression;
import com.fel.FelEngine;
import com.fel.context.FelContext;
import com.sun.tools.javac.Main;

import java.io.*;
import java.util.List;

/**
 * jdk1.5环境的编译器实现类
 *
 * @author yuqingsong
 */
public class FelCompiler15 extends AbstCompiler
{
    @Override
    Class<Expression> compileToClass(JavaSource src) throws ClassNotFoundException
    {
        String className = src.getSimpleName();
        String pack = src.getPackageName();
        String srcPackageDir = getSrcPackageDir(pack);
        String file = srcPackageDir + className + ".java";
        new File(srcPackageDir).mkdirs();
        String source = src.getSource();
        writeJavaFile(file, source);

        List<String> opt = getCompileOption();
        opt.add(file);
        String[] arg = opt.toArray(new String[0]);

        int compile = Main.compile(arg);
        if (compile != 0)
        {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<Expression> c = (Class<Expression>) loader.loadClass(src.getName());
        return c;
    }

    void writeJavaFile(String file, String source)
    {
        OutputStreamWriter write = null;
        try
        {
            BufferedOutputStream os;
            os = new BufferedOutputStream(new FileOutputStream(file), 500);
            write = new OutputStreamWriter(os, "utf-8");
            write.write(source);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (write != null)
            {
                try
                {
                    write.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void main(String[] args) throws InstantiationException,
            IllegalAccessException {
        FelEngine engine = FelEngine.instance;
        Integer num = new Integer(5);
        FelContext jc = engine.getContext();
        jc.set("num", num);
        System.out.println(engine.compile("num+1", jc).eval(jc));
    }
}
