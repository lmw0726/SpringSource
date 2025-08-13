/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.beans;

import org.springframework.asm.ClassVisitor;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.ReflectUtils;

import java.security.ProtectionDomain;
import java.util.*;

/**
 * 基于 <code>Map</code> 的 JavaBean 视图。默认的键集合是所有属性名（getter 或 setter）的并集。
 * 试图设置只读属性的操作会被忽略，只写属性的值将返回 <code>null</code>。
 * 不支持移除对象（键集合是固定的）。
 * @author Chris Nokleberg
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class BeanMap implements Map {
	/**
	 * 限制映射的键集合中仅包含可读属性。
	 * @see BeanMap.Generator#setRequire
	 */
	public static final int REQUIRE_GETTER = 1;

	/**
	 * 限制映射的键集合中仅包含可写属性。
	 * @see BeanMap.Generator#setRequire
	 */
	public static final int REQUIRE_SETTER = 2;

	/**
	 * 辅助方法，用于创建新的 <code>BeanMap</code> 实例。
	 * 如果需要更细粒度的控制，请使用 <code>BeanMap.Generator</code> 的新实例，而非此静态方法。
	 * @param bean 作为映射底层的 JavaBean 对象
	 * @return 一个新的 <code>BeanMap</code> 实例
	 */
	public static BeanMap create(Object bean) {
		Generator gen = new Generator();
		gen.setBean(bean);
		return gen.create();
	}

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE = new Source(BeanMap.class.getName());

        private static final BeanMapKey KEY_FACTORY =
          (BeanMapKey)KeyFactory.create(BeanMapKey.class, KeyFactory.CLASS_BY_NAME);

        interface BeanMapKey {
            public Object newInstance(Class type, int require);
        }
        
        private Object bean;
        private Class beanClass;
        private int require;
        
        public Generator() {
            super(SOURCE);
        }

		/**
		 * 设置生成的映射应反映的 bean。可以使用 {@link #setBean} 用同一类型的另一个 bean 进行替换。
		 * 调用此方法会覆盖之前通过 {@link #setBeanClass} 设置的值。
		 * 在调用 {@link #create} 之前，必须调用此方法或 {@link #setBeanClass} 之一。
		 * @param bean 初始的 bean 对象
		 */
		public void setBean(Object bean) {
			this.bean = bean;
			if (bean != null) {
				beanClass = bean.getClass();
				// SPRING 修补开始
				setContextClass(beanClass);
				// SPRING 修补结束
			}
        }

		/**
		 * 设置生成的映射所支持的 bean 的类。
		 * 在调用 {@link #create} 之前，必须调用此方法或 {@link #setBean} 之一。
		 * @param beanClass bean 的类
		 */
		public void setBeanClass(Class beanClass) {
			this.beanClass = beanClass;
		}

		/**
		 * 限制生成的映射所反映的属性范围。
		 * @param require 可使用 {@link #REQUIRE_GETTER} 和 {@link #REQUIRE_SETTER} 的任意组合；
		 *                默认为零（允许任何属性）
		 */
		public void setRequire(int require) {
			this.require = require;
		}

		protected ClassLoader getDefaultClassLoader() {
			return beanClass.getClassLoader();
		}

		protected ProtectionDomain getProtectionDomain() {
			return ReflectUtils.getProtectionDomain(beanClass);
		}

		/**
		 * 创建一个新的 <code>BeanMap</code> 实例。如果可能，将重用已有的生成类。
		 */
		public BeanMap create() {
            if (beanClass == null)
                throw new IllegalArgumentException("Class of bean unknown");
            setNamePrefix(beanClass.getName());
            return (BeanMap)super.create(KEY_FACTORY.newInstance(beanClass, require));
        }

        public void generateClass(ClassVisitor v) throws Exception {
            new BeanMapEmitter(v, getClassName(), beanClass, require);
        }

        protected Object firstInstance(Class type) {
            return ((BeanMap)ReflectUtils.newInstance(type)).newInstance(bean);
        }

        protected Object nextInstance(Object instance) {
            return ((BeanMap)instance).newInstance(bean);
        }
    }

	/**
	 * 使用指定的 bean 创建一个新的 <code>BeanMap</code> 实例。
	 * 这种方式比使用 {@link #create} 静态方法更快。
	 * @param bean 作为 Map 底层数据的 JavaBean 对象
	 * @return 一个新的 <code>BeanMap</code> 实例
	 */
	abstract public BeanMap newInstance(Object bean);

	/**
	 * 获取属性的类型。
	 * @param name JavaBean 属性的名称
	 * @return 属性的类型，如果属性不存在则返回 null
	 */
	abstract public Class getPropertyType(String name);

    protected Object bean;

    protected BeanMap() {
    }

    protected BeanMap(Object bean) {
        setBean(bean);
    }

    public Object get(Object key) {
        return get(bean, key);
    }

    public Object put(Object key, Object value) {
        return put(bean, key, value);
    }

	/**
	 * 获取指定 bean 的属性值。这使得一个 <code>BeanMap</code>
	 * 可以静态地用于多个 bean —— 忽略与映射关联的 bean 实例，而使用此方法传入的 bean。
	 * @param bean 要查询的 bean；必须与该 <code>BeanMap</code> 的类型兼容
	 * @param key 必须是一个 String
	 * @return 当前属性值，如果没有匹配的属性则返回 null
	 */
	abstract public Object get(Object bean, Object key);

	/**
	 * 设置指定 bean 的属性值。这使得一个 <code>BeanMap</code>
	 * 可以静态地用于多个 bean —— 忽略与映射关联的 bean 实例，而使用此方法传入的 bean。
	 * @param bean 要设置属性的 bean
	 * @param key 必须是一个 String
	 * @param value 要设置的新值
	 * @return 旧的属性值（如果存在），否则返回 null
	 */
	abstract public Object put(Object bean, Object key, Object value);

	/**
	 * 更改此映射当前使用的底层 bean。
	 * @param bean 新的 JavaBean
	 * @see #getBean
	 */
	public void setBean(Object bean) {
		this.bean = bean;
	}

	/**
	 * 返回当前由该映射使用的 bean。
	 * @return 当前的 JavaBean
	 * @see #setBean
	 */
	public Object getBean() {
		return bean;
	}

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object key) {
        return keySet().contains(key);
    }

    public boolean containsValue(Object value) {
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object v = get(it.next());
            if (((value == null) && (v == null)) || (value != null && value.equals(v)))
                return true;
        }
        return false;
    }

    public int size() {
        return keySet().size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map t) {
        for (Iterator it = t.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            put(key, t.get(key));
        }
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof Map)) {
            return false;
        }
        Map other = (Map)o;
        if (size() != other.size()) {
            return false;
        }
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            if (!other.containsKey(key)) {
                return false;
            }
            Object v1 = get(key);
            Object v2 = other.get(key);
            if (!((v1 == null) ? v2 == null : v1.equals(v2))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int code = 0;
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object value = get(key);
            code += ((key == null) ? 0 : key.hashCode()) ^
                ((value == null) ? 0 : value.hashCode());
        }
        return code;
    }

    // TODO: optimize
    public Set entrySet() {
        HashMap copy = new HashMap();
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            copy.put(key, get(key));
        }
        return Collections.unmodifiableMap(copy).entrySet();
    }

    public Collection values() {
        Set keys = keySet();
        List values = new ArrayList(keys.size());
        for (Iterator it = keys.iterator(); it.hasNext();) {
            values.add(get(it.next()));
        }
        return Collections.unmodifiableCollection(values);
    }

    /*
     * @see java.util.AbstractMap#toString
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append('{');
        for (Iterator it = keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            sb.append(key);
            sb.append('=');
            sb.append(get(key));
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
