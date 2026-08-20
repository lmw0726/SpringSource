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

package org.springframework.scheduling.quartz;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;

/**
 * {@link org.springframework.beans.factory.FactoryBean} 实现，暴露一个
 * {@link org.quartz.JobDetail} 对象，将任务执行委托给指定的（静态或非静态）方法。
 * 避免了为仅仅调用 Spring 管理的目标 Bean 上的现有服务方法而实现一行代码的 Quartz Job 的需要。
 *
 * <p>从 {@link MethodInvoker} 基类继承通用配置属性，如 {@link #setTargetObject "targetObject"} 和
 * {@link #setTargetMethod "targetMethod"}，并通过 {@link #setTargetBeanName "targetBeanName"} 属性
 * 支持按名称查找目标 Bean（作为直接指定 "targetObject" 的替代方案，允许使用非单例目标对象）。
 *
 * <p>通过 "concurrent" 属性支持并发运行的任务和非并发运行的任务。
 * 由此 MethodInvokingJobDetailFactoryBean 创建的任务默认是临时的（volatile）和持久的
 * （根据 Quartz 术语）。
 *
 * <p><b>注意：通过此 FactoryBean 创建的 JobDetails <i>不可</i>序列化，
 * 因此不适用于持久化任务存储。</b>
 * 对于希望将持久化任务委托给特定服务方法的每种情况，需要实现自己的 Quartz Job 作为薄包装器。
 *
 * <p>自 Spring 4.1 起，兼容 Quartz 2.1.4 及更高版本。
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @since 18.02.2004
 * @see #setTargetBeanName
 * @see #setTargetObject
 * @see #setTargetMethod
 * @see #setConcurrent
 */
public class MethodInvokingJobDetailFactoryBean extends ArgumentConvertingMethodInvoker
		implements FactoryBean<JobDetail>, BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, InitializingBean {

	@Nullable
	private String name;

	private String group = Scheduler.DEFAULT_GROUP;

	private boolean concurrent = true;

	@Nullable
	private String targetBeanName;

	@Nullable
	private String beanName;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private JobDetail jobDetail;


	/**
	 * 设置任务的名称。
	 * <p>默认值为该 FactoryBean 的 Bean 名称。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 设置任务的分组。
	 * <p>默认值为调度器的默认分组。
	 * @see org.quartz.Scheduler#DEFAULT_GROUP
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * 指定是否应以并发方式运行多个任务。
	 * 当不希望并发执行任务时，通过添加 {@code @PersistJobDataAfterExecution} 和
	 * {@code @DisallowConcurrentExecution} 标记来实现。
	 * 关于有状态与无状态任务的更多信息，请参阅
	 * <a href="https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-03.html">此处</a>。
	 * <p>默认设置为并发运行任务。
	 */
	public void setConcurrent(boolean concurrent) {
		this.concurrent = concurrent;
	}

	/**
	 * 设置 Spring BeanFactory 中目标 Bean 的名称。
	 * <p>这是指定 {@link #setTargetObject "targetObject"} 的替代方案，
	 * 允许调用非单例 Bean。请注意，指定的 "targetObject" 和 {@link #setTargetClass "targetClass"} 值
	 * 将覆盖此 "targetBeanName" 设置的相应效果
	 * （即静态预定义 Bean 类型甚至 Bean 对象）。
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}


	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		prepare();

		// 如果指定了名称则使用指定名称，否则回退到 Bean 名称。
		String name = (this.name != null ? this.name : this.beanName);

		// 根据 concurrent 标志选择有状态任务或无状态任务。
		Class<? extends Job> jobClass = (this.concurrent ? MethodInvokingJob.class : StatefulMethodInvokingJob.class);

		// 构建 JobDetail 实例。
		JobDetailImpl jdi = new JobDetailImpl();
		jdi.setName(name != null ? name : toString());
		jdi.setGroup(this.group);
		jdi.setJobClass(jobClass);
		jdi.setDurability(true);
		jdi.getJobDataMap().put("methodInvoker", this);
		this.jobDetail = jdi;

		postProcessJobDetail(this.jobDetail);
	}

	/**
	 * 用于后处理将由此 FactoryBean 暴露的 JobDetail 的回调。
	 * <p>默认实现为空。可在子类中覆盖。
	 * @param jobDetail 由此 FactoryBean 准备的 JobDetail
	 */
	protected void postProcessJobDetail(JobDetail jobDetail) {
	}


	/**
	 * 重写以支持 {@link #setTargetBeanName "targetBeanName"} 功能。
	 */
	@Override
	public Class<?> getTargetClass() {
		Class<?> targetClass = super.getTargetClass();
		if (targetClass == null && this.targetBeanName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set when using 'targetBeanName'");
			targetClass = this.beanFactory.getType(this.targetBeanName);
		}
		return targetClass;
	}

	/**
	 * 重写以支持 {@link #setTargetBeanName "targetBeanName"} 功能。
	 */
	@Override
	public Object getTargetObject() {
		Object targetObject = super.getTargetObject();
		if (targetObject == null && this.targetBeanName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set when using 'targetBeanName'");
			targetObject = this.beanFactory.getBean(this.targetBeanName);
		}
		return targetObject;
	}


	@Override
	@Nullable
	public JobDetail getObject() {
		return this.jobDetail;
	}

	@Override
	public Class<? extends JobDetail> getObjectType() {
		return (this.jobDetail != null ? this.jobDetail.getClass() : JobDetail.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 调用指定方法的 Quartz Job 实现。
	 * 由 MethodInvokingJobDetailFactoryBean 自动应用。
	 */
	public static class MethodInvokingJob extends QuartzJobBean {

		protected static final Log logger = LogFactory.getLog(MethodInvokingJob.class);

		@Nullable
		private MethodInvoker methodInvoker;

		/**
		 * 设置要使用的 MethodInvoker。
		 */
		public void setMethodInvoker(MethodInvoker methodInvoker) {
			this.methodInvoker = methodInvoker;
		}

		/**
		 * 通过 MethodInvoker 调用方法。
		 */
		@Override
		protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
			Assert.state(this.methodInvoker != null, "No MethodInvoker set");
			try {
				context.setResult(this.methodInvoker.invoke());
			}
			catch (InvocationTargetException ex) {
				if (ex.getTargetException() instanceof JobExecutionException) {
					// -> JobExecutionException，将由 Quartz 以 info 级别记录日志
					throw (JobExecutionException) ex.getTargetException();
				}
				else {
					// -> "未处理异常"，将由 Quartz 以 error 级别记录日志
					throw new JobMethodInvocationFailedException(this.methodInvoker, ex.getTargetException());
				}
			}
			catch (Exception ex) {
				// -> "未处理异常"，将由 Quartz 以 error 级别记录日志
				throw new JobMethodInvocationFailedException(this.methodInvoker, ex);
			}
		}
	}


	/**
	 * MethodInvokingJob 的扩展，实现了 StatefulJob 接口。
	 * Quartz 会检查任务是否有状态，如果有，则不会让任务之间相互干扰。
	 */
	@PersistJobDataAfterExecution
	@DisallowConcurrentExecution
	public static class StatefulMethodInvokingJob extends MethodInvokingJob {

		// 没有实现，只是添加了 StatefulJob 标记接口
		// 以允许有状态的方法调用任务。
	}

}
