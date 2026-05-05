package com.lmw.springaop.service;

import org.springframework.stereotype.Service;

/**
 * 用户服务接口
 *
 * @author LaiMingWei
 * @version 1.1.0
 * @since 2026-05-02 22:21
 */
@Service
public class UserService {

	public void test() {
		System.out.println("执行目标方法");
	}
}
