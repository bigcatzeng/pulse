package com.trxs.commons.bean;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.beans.BeanMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class GeneralBean
{
    /**
     * 实体Object
     */
    private  Object object = null;

    /**
     * 属性map
     */
    private  BeanMap beanMap = null;

    public GeneralBean() {
        super();
    }

    @SuppressWarnings("unchecked")
    public GeneralBean(Map propertyMap) {
        this.object = generateBean(propertyMap);
        this.beanMap = BeanMap.create(this.object);
    }

    /**
     * 给bean属性赋值
     * @param property 属性名
     * @param value 值
     */
    public void setValue(String property, Object value) {
        beanMap.put(property, value);
    }

    /**
     * 通过属性名得到属性值
     * @param property 属性名
     * @return 值
     */
    public Object getValue(String property) {
        return beanMap.get(property);
    }

    /**
     * 得到该实体bean对象
     * @return
     */
    public Object getObject()
    {
        return this.object;
    }


    @SuppressWarnings("unchecked")
    private Object generateBean(Map propertyMap)
    {
        BeanGenerator generator = new BeanGenerator();
        Set keySet = propertyMap.keySet();
        for (Iterator i = keySet.iterator(); i.hasNext();)
        {
            String key = (String) i.next();
            generator.addProperty(key, (Class) propertyMap.get(key));
        }
        return generator.create();
    }

    public static Object genBean(Map<String,Class<?>>  propertyClassMap, Map<String,Object> values)
    {
        // 生成动态 Bean
        GeneralBean bean = new GeneralBean(propertyClassMap);
        // 设置 Bean 属性值
        values.forEach((key,value)-> bean.setValue(key,value));
        // 获得bean的实体
        Object object = bean.getObject();
        return object;
    }
}