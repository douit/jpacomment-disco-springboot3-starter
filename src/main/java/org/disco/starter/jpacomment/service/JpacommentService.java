package org.disco.starter.jpacomment.service;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.disco.starter.jpacomment.annotation.ColumnComment;
import org.disco.starter.jpacomment.annotation.TableComment;
import org.disco.starter.jpacomment.call.JpaColumnCommentCallBackInterface;
import org.disco.starter.jpacomment.call.JpaCommentCallBackInterface;
import org.disco.starter.jpacomment.call.JpaTableCommentCallBackInterface;
import org.disco.starter.jpacomment.pojo.dto.ColumnCommentDTO;
import org.disco.starter.jpacomment.pojo.dto.TableCommentDTO;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

/**
 * JPA 字段注释处理类
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class JpacommentService {

    public static Logger logger = LoggerFactory.getLogger(JpacommentService.class);

    private EntityManager entityManager;




    @Autowired
    private  JpaCommentCallBackInterface jpaCommentCallBackInterface;



    private JpaColumnCommentCallBackInterface getJpaColumnCommentCallBackInterface() {
        return jpaCommentCallBackInterface;
    }

    private JpaTableCommentCallBackInterface getJpaTableCommentCallBackInterface() {
        return jpaCommentCallBackInterface;
    }

    AlterCommentService alterCommentService;

    Map<String, TableCommentDTO> dtoMap;

    public void init() {
        dtoMap = findAllTableAndColumn();
        logger.info("JpacommentService 初始化成功...");
    }

    /**
     * 设置当前 schema 用于中途修改schema
     *
     * @param schema 模式 mysql来说就是database
     */
    public void setCurrentSchema(String schema) {
        alterCommentService.setSchema(schema);
    }

    /**
     * 用于中途修改 数据源的可能
     *
     * @param jdbcTemplate jdbcTemplate
     */
    public void setCurrentJdbcTemplate(JdbcTemplate jdbcTemplate) {
        alterCommentService.setJdbcTemplate(jdbcTemplate);
    }

    /**
     * 更新整个数据库的表注释和字段注释，非空情况下才更新
     */
    public void alterAllTableAndColumn() {
        Map<String, TableCommentDTO> dtoMap = findAllTableAndColumn();
        dtoMap.forEach((k, v) -> {
            try {
                alterSingleTableAndColumn(k);
            } catch (Exception e) {
                logger.warn("=============================tableName '{}' ", k);
            }
        });
    }

    /**
     * 更新单个数据库的表注释和字段注释，非空情况下才更新
     *
     * @param tableName 数据库表名称
     */
    public void alterSingleTableAndColumn(String tableName) {
        TableCommentDTO commentDTO = dtoMap.get(tableName);
        if (commentDTO != null) {
            if (StrUtil.isNotBlank(commentDTO.getComment())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("修改表 {} 的注释为 '{}'", commentDTO.getName(), commentDTO.getComment());
                }
                alterCommentService.alterTableComment(commentDTO.getName(), commentDTO.getComment());
            }
            commentDTO.getColumnCommentDTOList().forEach(
                    item -> {
                        if (StrUtil.isNotBlank(item.getComment())) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("修改表 {} 字段 {} 的注释为 '{}'", commentDTO.getName(), item.getName(), item.getComment());
                            }
                            if(commentDTO.getName().equals("disco_conf_item_release")){
                                logger.debug("修改表 {} 字段 {} 的注释为 '{}'", commentDTO.getName(), item.getName(), item.getComment());
                            }
                            alterCommentService.alterColumnComment(commentDTO.getName(), item.getName(), item.getComment());
                        }
                    });
        } else {
            logger.warn("tableName '{}' not find in JPA ", tableName);
        }
    }


    @SuppressWarnings("rawtypes")
    public Map<String, TableCommentDTO> findAllTableAndColumn() {
        Map<String, TableCommentDTO> tableCommentMap = new HashMap<>(256);
        //通过EntityManager获取factory
        EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        Map<String, EntityPersister> persisterMap = sessionFactory.getMetamodel().entityPersisters();
        for (Map.Entry<String, EntityPersister> entity : persisterMap.entrySet()) {
            SingleTableEntityPersister persister = (SingleTableEntityPersister) entity.getValue();
            Class targetClass = entity.getValue().getMappedClass();
            TableCommentDTO table = new TableCommentDTO();
            // 表注释
            getTableInfo(persister, table, targetClass);
            //除主键外的属性注释
            getColumnInfo(persister, table, targetClass);
            // 主键字段注释
            getKeyColumnInfo(persister, table, targetClass);

            tableCommentMap.put(table.getName(), table);
        }

        return tableCommentMap;
    }

    @SuppressWarnings("rawtypes")
    private void getTableInfo(SingleTableEntityPersister persister, TableCommentDTO table, Class targetClass) {
        table.setColumnCommentDTOList(new ArrayList<>(32));
        table.setName(persister.getTableName());

        TableComment tableComment = AnnotationUtil.getAnnotation(targetClass, TableComment.class);
        if (tableComment != null) {
            table.setComment(tableComment.value());
        } else {
            String comment = getJpaTableCommentCallBackInterface().getTableComment(persister,table,targetClass);
            table.setComment(comment != null ? comment : "");
        }
    }

    /**
     * 递归获取所有父类的类对象 包括自己
     * 最后的子类在第一个
     *
     * @param targetClass targetClass
     * @param list        list
     */
    @SuppressWarnings("rawtypes")
    private void getAllClass(Class targetClass, List<Class> list) {
        list.add(targetClass);

        if (!Object.class.equals(targetClass.getSuperclass())) {
            getAllClass(targetClass.getSuperclass(), list);
        }
    }

    @SuppressWarnings("rawtypes")
    private void getColumnInfo(SingleTableEntityPersister persister, TableCommentDTO table, Class targetClass) {
        // 情况比较复杂，必须还要判断是否有父类，存在父类则还要取父类的字段信息，优先取得子类字段为依据
        List<Class> classList = new ArrayList<>(2);
        getAllClass(targetClass, classList);

        Set<String> alreadyDealField = new HashSet<>(32);
        Set<String> allColumnField = new HashSet<>(32);

        String[] attributes = persister.getPropertyNames();
        //属性
        for (String attr : attributes) {
            allColumnField.add(attr);
        }

        classList.forEach(classItem -> Arrays.stream(ClassUtil.getDeclaredFields(classItem)).forEach(field -> {
            if (allColumnField.contains(field.getName())) {
                // 判断是否已经处理过
                if (!alreadyDealField.contains(field.getName())) {
                    //对应数据库表中的字段名
                    String[] columnName = persister.getPropertyColumnNames(field.getName());
                    getColumnComment(table, classItem, field.getName(), columnName);
                    alreadyDealField.add(field.getName());
                }
            }
        }));
    }

    @SuppressWarnings("rawtypes")
    private void getKeyColumnInfo(SingleTableEntityPersister persister, TableCommentDTO table, Class targetClass) {
        String idName = persister.getIdentifierPropertyName();
        String[] idColumns = persister.getIdentifierColumnNames();
        getColumnComment(table, targetClass, idName, idColumns);
    }

    @SuppressWarnings("rawtypes")
    private void getColumnComment(TableCommentDTO table, Class targetClass, String propertyName, String[] columnName) {
        ColumnComment idColumnComment = AnnotationUtil.getAnnotation(
                ClassUtil.getDeclaredField(targetClass, propertyName), ColumnComment.class);
        Arrays.stream(columnName).forEach(item -> {
            ColumnCommentDTO column = new ColumnCommentDTO();
            column.setName(item);
            if (idColumnComment != null) {
                column.setComment(idColumnComment.value());
            } else {
                String comment = getJpaColumnCommentCallBackInterface().getColumnComment(table, targetClass, propertyName, item);
                column.setComment(comment != null ? comment : "");
            }
            table.getColumnCommentDTOList().add(column);
        });
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setAlterCommentService(AlterCommentService alterCommentService) {
        this.alterCommentService = alterCommentService;
    }
}
