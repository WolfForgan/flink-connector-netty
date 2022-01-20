# flink-connector-netty(http)
A netty(http) connector as Flink Source which takes https://github.com/apache/bahir-flink/tree/master/flink-connector-netty as a reference and reimplemented in pure JAVA language.



# Dependencies

## Maven version description

- JDK 1.8
- Flink 1.14.2+
- Nacos client 1.x
- Log4j 2.17.0



# Run in IDEA

Run/Debug Configurations:

![image-20220119174612777](https://github.com/WolfForgan/flink-connector-netty/blob/main/image/image-20220119174612777.png)

Special attention:  

Add option "Add dependencies with 'provided' scope to classpath", because scopes of Flink dependencies are "provided" in pom.xml
