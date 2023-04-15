package org.disco.starter.jpacomment.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.disco.starter.jpacomment.call.JpaCommentCallBackInterface;
import org.disco.starter.jpacomment.enums.DbTypeEnum;
import org.disco.starter.jpacomment.pojo.dto.TableCommentDTO;
import org.disco.starter.jpacomment.properties.JpacommentProperties;
import org.disco.starter.jpacomment.service.AlterCommentService;
import org.disco.starter.jpacomment.service.JpacommentService;
import org.disco.starter.jpacomment.service.impl.MysqlAlterCommentServiceImpl;
import org.disco.starter.jpacomment.service.impl.OracleAlterCommentServiceImpl;
import org.disco.starter.jpacomment.service.impl.PgSqlAlterCommentServiceImpl;
import org.disco.starter.jpacomment.service.impl.SqlServerAlterCommentServiceImpl;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * JpacommentAutoConfig 配置
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
@Configuration
@EnableConfigurationProperties({JpacommentProperties.class})
@AutoConfigureAfter({EntityManager.class, JdbcTemplate.class, JpaProperties.class})
@ConditionalOnProperty(prefix = "disco.jpa.comment", value = "enable", havingValue = "true")
public class JpacommentAutoConfig {

    public static Logger logger = LoggerFactory.getLogger(JpacommentAutoConfig.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    DataSource dataSource;

    @Resource
    JpaProperties jpaProperties;

    @Bean
    @ConditionalOnMissingBean
    public AlterCommentService alterCommentService() throws SQLException {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        String databaseType = metaData.getDatabaseProductName().toUpperCase();
        String schema = "";
        AlterCommentService service;
        if (databaseType.contains(DbTypeEnum.MYSQL.getValue())) {
            schema = jdbcTemplate.queryForObject("select database() from dual", String.class);
            service = new MysqlAlterCommentServiceImpl();
        } else if (databaseType.contains(DbTypeEnum.SQLSERVER.getValue())) {
            schema = "dbo";
            String jpaDefaultSchema = "default_schema";
            Map<String, String> params = jpaProperties.getProperties();
            if (params != null && StrUtil.isNotBlank(params.get(jpaDefaultSchema))) {
                schema = params.get(jpaDefaultSchema);
            }
            service = new SqlServerAlterCommentServiceImpl();
        } else if (databaseType.contains(DbTypeEnum.ORACLE.getValue())) {
            schema = jdbcTemplate.queryForObject("select SYS_CONTEXT('USERENV','CURRENT_SCHEMA') CURRENT_SCHEMA from dual", String.class);
            service = new OracleAlterCommentServiceImpl();
        } else if (databaseType.contains(DbTypeEnum.POSTGRESQL.getValue())) {
            schema = jdbcTemplate.queryForObject(" SELECT CURRENT_SCHEMA ", String.class);
            service = new PgSqlAlterCommentServiceImpl();
        } else {
            service = null;
            logger.error("can not find DatabaseProductName {}", databaseType);
        }

        if (service != null) {
            service.setSchema(schema);
            service.setJdbcTemplate(jdbcTemplate);
            logger.debug("当前数据库schema为 {}", service.getSchema());
        }

        return service;
    }


    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    public JpacommentService jpacommentService() throws SQLException {
        JpacommentService service = new JpacommentService();
        service.setEntityManager(entityManager);
        service.setAlterCommentService(alterCommentService());
        return service;
    }

    @Bean
    @ConditionalOnMissingBean
    public JpaCommentCallBackInterface jpaCommentCallBackInterface() {
        JpaCommentCallBackInterface service = new JpaCommentCallBackInterface(){
            @Override
            public String getTableComment(SingleTableEntityPersister persister, TableCommentDTO table, Class targetClass) {
                return null;
            }

            @Override
            public String getColumnComment(TableCommentDTO table, Class targetClass, String propertyName, String columnName) {
                return null;
            }

            //**/
        };
        return service;
    }


}
