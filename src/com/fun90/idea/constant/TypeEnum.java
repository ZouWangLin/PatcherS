package com.fun90.idea.constant;

import cn.hutool.core.util.StrUtil;

public enum TypeEnum {
    HTML("HTML"),
    CSS("CSS"),
    JS("JS");
    private String type;

    TypeEnum(String type) {
        this.type = type;
    }

    public static Boolean contains(String type) {
        boolean flag = false;
        for (TypeEnum ele : TypeEnum.values()) {
            if (StrUtil.equalsIgnoreCase(ele.getType(), type)) {
                flag = true;
            }
        }
        return flag;
    }

    public String getType() {
        return type;
    }
}
