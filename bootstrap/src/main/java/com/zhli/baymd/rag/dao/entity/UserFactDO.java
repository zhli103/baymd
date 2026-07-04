package com.zhli.baymd.rag.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_user_fact")
public class UserFactDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    /** fact类型: health/behavior/preference/goal */
    private String factType;

    private String factText;

    private Float confidence;

    private String sourceMsgId;

    /** SHA-256 of fact_text, for dedup */
    private String contentHash;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
