/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jmx.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.management.MBeanServer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.lang.Nullable;

/**
 * 通过 WebSphere 专有的 {@code AdminServiceFactory} API 获取
 * WebSphere {@link javax.management.MBeanServer} 引用的 {@link FactoryBean}，
 * 该 API 在 WebSphere 5.1 及更高版本中可用。
 *
 * <p>将 {@code MBeanServer} 暴露为 bean 引用。
 *
 * <p>此 {@code FactoryBean} 是 {@link MBeanServerFactoryBean} 的直接替代方案，
 * 后者使用标准 JMX 1.2 API 来访问平台的 {@link MBeanServer}。
 *
 * <p>参阅 WebSphere 的
 * <a href="https://www.ibm.com/support/knowledgecenter/SSEQTJ_9.0.0/com.ibm.websphere.javadoc.doc/web/apidocs/com/ibm/websphere/management/AdminServiceFactory.html">{@code AdminServiceFactory}</a>
 * 和
 * <a href="https://www.ibm.com/support/knowledgecenter/SSEQTJ_9.0.0/com.ibm.websphere.javadoc.doc/web/apidocs/com/ibm/websphere/management/MBeanFactory.html">{@code MBeanFactory}</a> 的 Javadoc。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0.3
 * @see javax.management.MBeanServer
 * @see MBeanServerFactoryBean
 */
public class WebSphereMBeanServerFactoryBean implements FactoryBean<MBeanServer>, InitializingBean {

	private static final String ADMIN_SERVICE_FACTORY_CLASS = "com.ibm.websphere.management.AdminServiceFactory";

	private static final String GET_MBEAN_FACTORY_METHOD = "getMBeanFactory";

	private static final String GET_MBEAN_SERVER_METHOD = "getMBeanServer";


	@Nullable
	private MBeanServer mbeanServer;


	@Override
	public void afterPropertiesSet() throws MBeanServerNotFoundException {
		try {
			/*
			 * this.mbeanServer = AdminServiceFactory.getMBeanFactory().getMBeanServer();
			 */
			Class<?> adminServiceClass = getClass().getClassLoader().loadClass(ADMIN_SERVICE_FACTORY_CLASS);
			Method getMBeanFactoryMethod = adminServiceClass.getMethod(GET_MBEAN_FACTORY_METHOD);
			Object mbeanFactory = getMBeanFactoryMethod.invoke(null);
			Method getMBeanServerMethod = mbeanFactory.getClass().getMethod(GET_MBEAN_SERVER_METHOD);
			this.mbeanServer = (MBeanServer) getMBeanServerMethod.invoke(mbeanFactory);
		}
		catch (ClassNotFoundException ex) {
			throw new MBeanServerNotFoundException("Could not find WebSphere's AdminServiceFactory class", ex);
		}
		catch (InvocationTargetException ex) {
			throw new MBeanServerNotFoundException(
					"WebSphere's AdminServiceFactory.getMBeanFactory/getMBeanServer method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new MBeanServerNotFoundException(
					"Could not access WebSphere's AdminServiceFactory.getMBeanFactory/getMBeanServer method", ex);
		}
	}


	@Override
	@Nullable
	public MBeanServer getObject() {
		return this.mbeanServer;
	}

	@Override
	public Class<? extends MBeanServer> getObjectType() {
		return (this.mbeanServer != null ? this.mbeanServer.getClass() : MBeanServer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
