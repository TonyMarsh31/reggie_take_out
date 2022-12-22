package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工表
 */
@Data
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String name;
    private String password;
    private String phone;
    private String sex;
    /**
     * 身份证号
     */
    private String idNumber;
    private Integer status;
    @TableField(fill = FieldFill.INSERT) // 在插入时由mybatisPlus自动填充
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) // 在插入和更新时由mybatisPlus自动填充
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
