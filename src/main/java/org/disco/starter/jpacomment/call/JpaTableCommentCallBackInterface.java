package org.disco.starter.jpacomment.call;

import org.disco.starter.jpacomment.pojo.dto.TableCommentDTO;
import org.hibernate.persister.entity.SingleTableEntityPersister;

/**
 *  暴露TableComment钩子
 *
 * @author douit
 * @version 3.0.0
 * @date 2023-04-07
 * @intro
 */
public interface JpaTableCommentCallBackInterface {
  public String getTableComment(SingleTableEntityPersister persister, TableCommentDTO table, Class targetClass);
}
