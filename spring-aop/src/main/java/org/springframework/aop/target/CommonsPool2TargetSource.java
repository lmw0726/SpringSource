/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.target;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.aop.TargetSource} 的实现，
 * 将对象保存在可配置的 Apache Commons2 Pool 中。
 *
 * <p>默认情况下，会创建一个 {@code GenericObjectPool} 实例。
 * 子类可以通过重写 {@code createObjectPool()} 方法来更改所使用的
 * {@code ObjectPool} 类型。
 *
 * <p>提供了许多配置属性，与 Commons Pool 的
 * {@code GenericObjectPool} 类中的属性相对应；这些属性在构建期间传递给
 * {@code GenericObjectPool}。如果创建此类的子类来更改 {@code ObjectPool}
 * 的实现类型，请传入与所选实现相关的配置属性值。
 *
 * <p>{@code testOnBorrow}、{@code testOnReturn} 和 {@code testWhileIdle}
 * 属性被明确排除，因为此类使用的 {@code PoolableObjectFactory}
 * 的实现没有实现有意义的验证。所有暴露的 Commons Pool 属性
 * 都使用了相应的 Commons Pool 默认值。
 *
 * <p>从 Spring 4.2 起，与 Apache Commons Pool 2.4 兼容。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @since 4.2
 * @see GenericObjectPool
 * @see #createObjectPool()
 * @see #setMaxSize
 * @see #setMaxIdle
 * @see #setMinIdle
 * @see #setMaxWait
 * @see #setTimeBetweenEvictionRunsMillis
 * @see #setMinEvictableIdleTimeMillis
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class CommonsPool2TargetSource extends AbstractPoolingTargetSource implements PooledObjectFactory<Object> {

	private int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

	private int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;

	private long maxWait = GenericObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS;

	private long timeBetweenEvictionRunsMillis = GenericObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

	private long minEvictableIdleTimeMillis = GenericObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

	private boolean blockWhenExhausted = GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

	/**
	 * 用于池化目标对象的 Apache Commons {@code ObjectPool}。
	 */
	@Nullable
	private ObjectPool pool;


	/**
	 * 使用默认设置创建 CommonsPoolTargetSource。
	 * 池的默认最大大小为 8。
	 * @see #setMaxSize
	 * @see GenericObjectPoolConfig#setMaxTotal
	 */
	public CommonsPool2TargetSource() {
		setMaxSize(GenericObjectPoolConfig.DEFAULT_MAX_TOTAL);
	}


	/**
	 * 设置池中空闲对象的最大数量。
	 * 默认值为 8。
	 * @see GenericObjectPool#setMaxIdle
	 */
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	/**
	 * 返回池中空闲对象的最大数量。
	 */
	public int getMaxIdle() {
		return this.maxIdle;
	}

	/**
	 * 设置池中空闲对象的最小数量。
	 * 默认值为 0。
	 * @see GenericObjectPool#setMinIdle
	 */
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	/**
	 * 返回池中空闲对象的最小数量。
	 */
	public int getMinIdle() {
		return this.minIdle;
	}

	/**
	 * 设置从池中获取对象的最大等待时间。
	 * 默认值为 -1，表示永久等待。
	 * @see GenericObjectPool#setMaxWaitMillis
	 */
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	/**
	 * 返回从池中获取对象的最大等待时间。
	 */
	public long getMaxWait() {
		return this.maxWait;
	}

	/**
	 * 设置驱逐运行之间的时间间隔，以检查空闲对象是否
	 * 空闲过久或已变为无效。
	 * 默认值为 -1，表示不执行任何驱逐。
	 * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
	 */
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	/**
	 * 返回检查空闲对象的驱逐运行之间的时间间隔。
	 */
	public long getTimeBetweenEvictionRunsMillis() {
		return this.timeBetweenEvictionRunsMillis;
	}

	/**
	 * 设置空闲对象在被驱逐前可以在池中保留的最短时间。
	 * 默认值为 1800000（30 分钟）。
	 * <p>注意，需要执行驱逐运行才能使此设置生效。
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @see GenericObjectPool#setMinEvictableIdleTimeMillis
	 */
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	/**
	 * 返回空闲对象在被驱逐前可以在池中保留的最短时间。
	 */
	public long getMinEvictableIdleTimeMillis() {
		return this.minEvictableIdleTimeMillis;
	}

	/**
	 * 设置当池耗尽时调用是否应阻塞。
	 */
	public void setBlockWhenExhausted(boolean blockWhenExhausted) {
		this.blockWhenExhausted = blockWhenExhausted;
	}

	/**
	 * 指定当池耗尽时调用是否应阻塞。
	 */
	public boolean isBlockWhenExhausted() {
		return this.blockWhenExhausted;
	}


	/**
	 * 创建并持有一个 ObjectPool 实例。
	 * @see #createObjectPool()
	 */
	@Override
	protected final void createPool() {
		logger.debug("Creating Commons object pool");
		this.pool = createObjectPool();
	}

	/**
	 * 子类可以重写此方法以返回特定的 Commons Pool。
	 * 它们应该在此处对池应用任何配置属性。
	 * <p>默认是具有给定池大小的 GenericObjectPool 实例。
	 * @return 一个空的 Commons {@code ObjectPool}。
	 * @see GenericObjectPool
	 * @see #setMaxSize
	 */
	protected ObjectPool createObjectPool() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(getMaxSize());
		config.setMaxIdle(getMaxIdle());
		config.setMinIdle(getMinIdle());
		config.setMaxWaitMillis(getMaxWait());
		config.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		config.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		config.setBlockWhenExhausted(isBlockWhenExhausted());
		return new GenericObjectPool(this, config);
	}


	/**
	 * 从 {@code ObjectPool} 中借出一个对象。
	 */
	@Override
	public Object getTarget() throws Exception {
		Assert.state(this.pool != null, "No Commons ObjectPool available");
		return this.pool.borrowObject();
	}

	/**
	 * 将指定对象返回到底层的 {@code ObjectPool}。
	 */
	@Override
	public void releaseTarget(Object target) throws Exception {
		if (this.pool != null) {
			this.pool.returnObject(target);
		}
	}

	@Override
	public int getActiveCount() throws UnsupportedOperationException {
		return (this.pool != null ? this.pool.getNumActive() : 0);
	}

	@Override
	public int getIdleCount() throws UnsupportedOperationException {
		return (this.pool != null ? this.pool.getNumIdle() : 0);
	}


	/**
	 * 在销毁此对象时关闭底层的 {@code ObjectPool}。
	 */
	@Override
	public void destroy() throws Exception {
		if (this.pool != null) {
			logger.debug("Closing Commons ObjectPool");
			this.pool.close();
		}
	}


	//----------------------------------------------------------------------------
	// org.apache.commons.pool2.PooledObjectFactory 接口的实现
	//----------------------------------------------------------------------------

	@Override
	public PooledObject<Object> makeObject() throws Exception {
		return new DefaultPooledObject<>(newPrototypeInstance());
	}

	@Override
	public void destroyObject(PooledObject<Object> p) throws Exception {
		destroyPrototypeInstance(p.getObject());
	}

	@Override
	public boolean validateObject(PooledObject<Object> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<Object> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<Object> p) throws Exception {
	}

}
