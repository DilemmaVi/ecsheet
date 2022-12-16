# 基于Luckysheet实现的协同编辑在线表格

------

这两年，在线表格协作工具越来越火，但开源界一直没有相关的实现，被垄断在几个大厂手上，随着**Luckysheet** 的横空出世，开源界终于也有一个漂亮能打的在线表格，而且仔细研究后发现Luckysheet与excel已经特别接近，也实现了协同编辑，故基于Luckysheet，本项目实现了一个协同编辑的demo：

本项目后端的语言是Java，主要技术栈如下：

> * 框架：SpringBoot + Websocket
> * 数据库：MongoDB 4.4.0
> * 前端核心：Luckysheet


### [友情链接：Luckysheet](https://github.com/mengshukeji/Luckysheet)

> 🚀Luckysheet is an online spreadsheet like excel that is powerful, simple to configure, and completely open source.

------
## 演示地址: [点击访问](http://ecsheet.szxctech.cn/index)


## QuickStart

由于后端是基于Java和mongo的，需要提前安装相关环境，如想快速体验，建议使用Docker，本项目也经docker环境验证可以正常运行。

### 1. 安装Docker环境

参考阿里云方案：https://developer.aliyun.com/article/110806

### 2. 使用Docker 安装mongodb并启动

参考菜鸟教程方案：https://www.runoob.com/docker/docker-install-mongodb.html
代码里面的mongo账户密码为 admin/123456，请保持一致

### 3. 打包程序docker镜像

* 在服务器新建一个docker文件夹，将maven打包好的jar包和Dockerfile文件复制到服务器的docker文件夹下,执行以下语句

```shell
docker build -t ecsheet .
```
* 启动容器
```shell
docker run -d --name ecsheet --link mongo:mongo -p 9999:9999 ecsheet
```

### 4. 访问地址
http://{你服务器的ip地址}:9999/index
