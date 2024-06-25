package com.lmw.learn.bean.bean.wrapper;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.propertyeditors.CharsetEditor;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;

/**
 * BeanWrapper测试类
 *
 * @author LMW
 * @version 1.0
 * @date 2024-02-29 23:21
 */
public class BeanWrapperTest {
	public static void main(String[] args) {
		Fruit fruit = new Fruit();
		BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(fruit);
		// PropertyAccessor的相关接口
		boolean isRead = beanWrapper.isReadableProperty("color");
		System.out.println(isRead);
		boolean isWrite = beanWrapper.isWritableProperty("size");
		System.out.println(isWrite);
		Class<?> color = beanWrapper.getPropertyType("color");
		System.out.println(color);
		beanWrapper.setPropertyValue("color", "红色");
		beanWrapper.setPropertyValue(new PropertyValue("size", "大号"));

		beanWrapper.registerCustomEditor(String.class, new CharsetEditor());
		PropertyEditor customEditor = beanWrapper.findCustomEditor(String.class, "color");
		String text = customEditor.getAsText();
		System.out.println(text);

		Integer integer = beanWrapper.convertIfNecessary("1", Integer.class);
		System.out.println(integer);

		Class<?> wrappedClass = beanWrapper.getWrappedClass();
		System.out.println(wrappedClass);

		PropertyDescriptor descriptor = beanWrapper.getPropertyDescriptor("color");
		System.out.println(descriptor.getName());
		System.out.println(descriptor.getDisplayName());


		Fruit instance = (Fruit) beanWrapper.getWrappedInstance();
		System.out.println(instance.getColor());
		System.out.println(instance.getSize());
	}


}
