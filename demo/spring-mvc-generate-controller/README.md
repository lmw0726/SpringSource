该项目主要有两个功能：
1、运行时生成Controller类，并能对该Controller所有的URL发起请求。
2、将第一步生成的Controller类配置文件，在启动时，加载进SpringMVC中。并能访问这些Controller中的URL。

实现方案如下：

1、通过Groovy脚本生成相应的Controller代码，进而加载这些groovy脚本，从而生成Controller。
    可以查看：
    https://blog.csdn.net/zhao_god/article/details/132083904
    
如何演示?

- 请求 http://127.0.0.1:8080/groovy/generate （POST请求）</br>
  此时，URL /groovy/hello将会被加载进Spring MVC中。
- 发起GET请求，访问 http://127.0.0.1:8080/groovy/hello 。可以看到显示 hello,groovy。
- 发起DELETE请求，访问 http://127.0.0.1:8080/groovy/unload 。 </br>
  此时，Spring MVC 会卸载掉 URL /groovy/hello
- 再次访问 http://127.0.0.1:8080/groovy/hello ，就会显示 404 状态码。

    
2、通过FreeMarker生成Controller的java文件，然后编译成class文件后，加载进SpringMVC中。</br>
3、通过字节码生成工具如Cglib等，直接生成相应的Controller。
