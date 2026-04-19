package com.xl.can.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("shop")
public class Shop implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String shopCode;
    private String shopName;
    private String province;
    private String city;
    private String district;
    private String address;
    private String managerName;
    private String managerPhone;
    @TableField("meituan_id")
    private String meituanId;
    @TableField("ele_id")
    private String eleId;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deletedAt;
}
