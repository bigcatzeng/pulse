package com.trxs.commons.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileTools
{
    private enum Singleton
    {
        INSTANCE;
        private FileTools singleton;
        Singleton()
        {
            singleton = new FileTools();
        }
        public FileTools getInstance()
        {
            return singleton;
        }
    }

    private FileTools(){}

    public static FileTools getInstance()
    {
        return Singleton.INSTANCE.getInstance();
    }

    public Path createDirectories(String dir) throws IOException
    {
        Path path = Paths.get(dir);
        if ( Files.exists(path) ) return path;

        return Files.createDirectories(path);
    }

    public List<Path> find(String dir, String regex) throws IOException
    {
        List<Path> fileList = Files.list(Paths.get(dir)).filter( path -> matcher(path.getFileName().toString(), regex) ).collect(Collectors.toList());
        return fileList;
    }

    private boolean matcher( String value, String regex)
    {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(value);
        return m.find();
    }

    public String readFile(Path path)
    {
        StringBuffer stringBuffer = new StringBuffer();
        readLines(path).forEach( line -> stringBuffer.append(line) );
        return stringBuffer.toString();
    }

    public Stream<String> readLines(Path path)
    {
        List<String> lines = new ArrayList<>(0);
        if ( path == null || path!=null && Files.notExists(path) ) return lines.stream();

        try
        {
            return Files.lines(path);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return lines.stream();
    }

    public Path openFile(String filePath)
    {
        Path path = Paths.get(filePath);
        if ( Files.notExists(path) )
        {
            Path dir = path.getParent();
            try
            {
                if ( Files.notExists(dir) ) Files.createDirectories(dir);

                Files.createFile(path);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return path;
    }

    public BufferedWriter getBufferWriteByPath(Path path)
    {
        BufferedWriter bufferedWriter = null;
        try
        {
            bufferedWriter = Files.newBufferedWriter(path);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return bufferedWriter;
    }

}
