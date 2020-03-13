package com.trxs.commons.jdbc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageBean<T>
{
    private List<T> elements; //内容列表
    private int         size; //每页大小
    private int  elementSize; //list中元素有多少个
    private int         page; //当前页数
    private int    totalPage; //总的页数
    private int    totalSize; //总共的数量
}