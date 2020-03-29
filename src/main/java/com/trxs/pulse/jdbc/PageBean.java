package com.trxs.pulse.jdbc;

import java.util.List;

public class PageBean<T>
{
    private List<T> elements; //内容列表
    private int         size; //每页大小
    private int         page; //当前页数
    private int    totalPage; //总的页数
    private int    totalSize; //总共的数量
    private int  elementSize; //list中元素有多少个

    public PageBean()
    {
        size = 0;
        page = 0;
        totalPage = 0;
        totalSize = 0;
        elementSize = 0;
    }

    public static <T> PageBean<T> build( List<T> data, int totalSize, int totalPage, int page, int size )
    {
        PageBean<T> pageBean = new PageBean<>();;
        try
        {
            if ( data != null && data.size() > 0 )
            {
                Class<T> clz = (Class<T>) data.get(0).getClass();
                pageBean = (PageBean<T>) clz.newInstance();
                pageBean.init(data,totalPage,totalPage,page,size);
            }
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        return pageBean;
    }

    public void init(List<T> data, int totalSize, int totalPage, int page, int size)
    {
        this.page = page;
        this.size = size;
        this.elements = data;
        this.elementSize = data.size();
        this.totalSize = totalSize;
        this.totalPage = totalPage;
    }

    public List<T> getElements() {
        return elements;
    }

    public void setElements(List<T> elements) {
        this.elements = elements;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getElementSize() {
        return elementSize;
    }

    public void setElementSize(int elementSize) {
        this.elementSize = elementSize;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
}