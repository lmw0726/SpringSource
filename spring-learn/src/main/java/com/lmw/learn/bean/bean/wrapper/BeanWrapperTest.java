package com.lmw.learn.bean.bean.wrapper;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;

/**
 * BeanWrapper测试类
 *
 * @author LMW
 * @version 1.0
 * @date 2024-02-29 23:21
 */
public class BeanWrapperTest {
	public static void main(String[] args) {
		BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(Fruit.class);
		beanWrapper.setPropertyValue("color", "红色");
		beanWrapper.setPropertyValue(new PropertyValue("size", "大号"));
	}


}
