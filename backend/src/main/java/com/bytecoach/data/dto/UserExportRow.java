package com.bytecoach.data.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExportRow {
    @ExcelProperty("ID")
    private Long id;

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("昵称")
    private String nickname;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("角色")
    private String role;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("注册时间")
    private String createTime;

    @ExcelProperty("最后登录")
    private String lastLoginTime;
}
