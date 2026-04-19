package com.xl.can.context;

import com.xl.can.entity.SysUser;

public class UserContext {
    private static final ThreadLocal<SysUser> userThreadLocal = new ThreadLocal<>();

    public static void setUser(SysUser user) {
        userThreadLocal.set(user);
    }

    public static SysUser getUser() {
        return userThreadLocal.get();
    }

    public static Long getUserId() {
        SysUser user = getUser();
        return user != null ? user.getId() : null;
    }

    public static String getUsername() {
        SysUser user = getUser();
        return user != null ? user.getUsername() : null;
    }

    public static String getRoleCode() {
        SysUser user = getUser();
        return user != null ? user.getRoleCode() : null;
    }

    public static Long getTenantId() {
        SysUser user = getUser();
        return user != null ? user.getTenantId() : null;
    }

    public static void remove() {
        userThreadLocal.remove();
    }
}
