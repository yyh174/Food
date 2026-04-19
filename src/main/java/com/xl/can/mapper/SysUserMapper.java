package com.xl.can.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xl.can.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    
    @Select("SELECT * FROM sys_user WHERE tenant_id = #{tenantId} AND username = #{username} AND deleted_at IS NOT NULL LIMIT 1")
    SysUser selectDeletedByTenantAndUsername(@Param("tenantId") Long tenantId, @Param("username") String username);
    
    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    void physicalDeleteById(@Param("id") Long id);
}
