package org.disco.starter.jpacomment.call;

import org.disco.starter.jpacomment.pojo.dto.TableCommentDTO;

/**
 *  暴露ColumnComment钩子
 *
 * @author douit
 * @version 3.0.0
 * @date 2023-04-07
 * @intro
 */
public interface JpaColumnCommentCallBackInterface {
  public String getColumnComment(TableCommentDTO table, Class targetClass, String propertyName, String columnName);
}
