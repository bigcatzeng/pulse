package com.trxs.pulse;

public class Foo
{
    private String name;
    private Integer size;

    private Foo children;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Foo getChildren() {
        return children;
    }

    public void setChildren(Foo children) {
        this.children = children;
    }
}
