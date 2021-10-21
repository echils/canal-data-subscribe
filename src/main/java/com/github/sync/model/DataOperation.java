package com.github.sync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据信息
 *
 * @author echils
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataOperation {

    /**
     * 数据信息(Json)
     */
    private String value;

    /**
     * 数据变动类型
     */
    private OperationType type;


    public enum OperationType {

        /**
         * 新增
         */
        INSERT,

        /**
         * 删除
         */
        DELETE,

        /**
         * 更新
         */
        UPDATE
    }

}

