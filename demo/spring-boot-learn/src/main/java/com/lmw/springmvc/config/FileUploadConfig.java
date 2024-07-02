package com.lmw.springmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * 文件上传配置
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-02 22:05
 */
@Configuration
public class FileUploadConfig {
	@Bean
	public MultipartResolver multipartResolver(){
		return new CommonsMultipartResolver();
	}
}
