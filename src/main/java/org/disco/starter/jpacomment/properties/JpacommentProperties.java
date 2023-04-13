package org.disco.starter.jpacomment.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JpacommentProperties 配置文件
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
@ConfigurationProperties(prefix = "disco.jpa.comment")
public class JpacommentProperties {

    private boolean enable = true;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
