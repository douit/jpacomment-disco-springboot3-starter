# jpacomment-disco-springboot3-starter
JPA创建或修改数据库的表注释和字段注释(仅支持springboot3)


## 说明：
JPA 比较方便，让开发免于手动创建表操作，但有一个问题表中字段无注释，虽然JPA有提供方法，但无法适应所有主流数据库。
JPA 自身提供方法如下：
```java
public class MyEntity {

 @Column(nullable = false,columnDefinition = "int(2) comment '我是年龄注释...'")
 private Integer age;

}
```
其中 **columnDefinition** 其实就是写 Native Sql，这样违背了JPA的初衷“屏蔽底层数据库差异”。

jpacomment-disco-springboot3-starter 目前适配了三种数据库 Mysql Sqlserver oracle，后期可以添加其他数据库。

jpacomment-disco-springboot3-starter 的方法很简单将 java属性上的注解注释内容 修改到表字段里面。

用法如下：

在yaml文件中添加
```yaml
disco:
  jpa:
    comment:
      enable: true
```
```实现如下接口可以使用其它接口文档注解替代@ColumnComment和@TableComment,以knife4j为例
import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.disco.starter.jpacomment.call.JpaCommentCallBackInterface;
import org.disco.starter.jpacomment.pojo.dto.TableCommentDTO;
import org.hibernate.persister.entity.SingleTableEntityPersister;
@Slf4j
public class JpaCommentCallBackImpl implements JpaCommentCallBackInterface {
@Override
public String getColumnComment(TableCommentDTO table, Class targetClass, String propertyName, String columnName) {

        Schema schema = AnnotationUtil.getAnnotation(
                ClassUtil.getDeclaredField(targetClass, propertyName), Schema.class);
        if (schema != null && schema.description()!= null) {
            return schema.description();
        }

        return null;
    }


    @Override
    public String getTableComment(SingleTableEntityPersister persister, TableCommentDTO table, Class targetClass) {
        Schema schema  = AnnotationUtil.getAnnotation(targetClass, Schema.class);
        if (schema != null && schema.description()!= null) {
            return    schema.description();
        }
        return null;
    }
}

package org.disco.core.config.jpa;

import org.disco.core.service.impl.JpaCommentCallBackImpl;
import org.disco.starter.jpacomment.call.JpaCommentCallBackInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JpaAutoConfig {

    @Bean
    public JpaCommentCallBackInterface jpaCommentCallBackInterface() {
        JpaCommentCallBackInterface service = new JpaCommentCallBackImpl();
        return service;
    }
}

```

Entity 实体类里面添加注解 **@TableComment** 和  **@ColumnComment**

```java

package org.example.entity;

import org.disco.starter.jpacomment.annotation.ColumnComment;
import org.disco.starter.jpacomment.annotation.TableComment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.base.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author admin
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "T_SYS_ORG")
@TableComment("组织信息表")
public class SysOrgEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ColumnComment("编号")
    @Column(unique = true)
    private String orgCode;

    @ColumnComment("组织id")
    private String orgId;

    @ColumnComment("组织名称")
    private String orgName;


}
```

调用 service 方法 更新全库或 单表字段注释，这里没有采用启动自动更新字段注释，而采用手动方式调用，
主要考虑表注释一般不会频繁更新。

```java
@RestController
@RequestMapping("api/sys")
public class SetCcommentController {

    @Resource
    JpacommentService jpacommentService;

   /**
    * 更新全库字段注释
    */
    @GetMapping("alterAllTableAndColumn")
    public String alterAllTableAndColumn() {
        jpacommentService.alterAllTableAndColumn();
        return "success";
    }

}
```

开启日志打印 application.ymal 中添加
```yaml
logging:
  level:
    root: INFO
    org.disco: DEBUG  # jpacomment-disco-springboot3-starter日志打印
```
控制台可以打印如下信息：


```
。。。

```