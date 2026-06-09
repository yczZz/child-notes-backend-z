package com.ycz.childnotesbackend.context;

import com.ycz.childnotesbackend.model.entity.AdminAccount;

public class AdminAuthContext {

    private static final ThreadLocal<AdminAccount> CURRENT_ADMIN = new ThreadLocal<>();

    private AdminAuthContext() {
    }

    public static void setCurrentAdmin(AdminAccount admin) {
        CURRENT_ADMIN.set(admin);
    }

    public static AdminAccount getCurrentAdmin() {
        return CURRENT_ADMIN.get();
    }

    public static Long getCurrentAdminId() {
        AdminAccount admin = CURRENT_ADMIN.get();
        return admin == null ? null : admin.getId();
    }

    public static void clear() {
        CURRENT_ADMIN.remove();
    }
}
