package com.fun90.idea.thread;

import cn.hutool.core.util.RuntimeUtil;

/**
 * @author zouwanglin
 */
public class SecureFxRunnable implements Runnable {

    @Override
    public void run() {
        RuntimeUtil.exec("SecureFX");
    }
}
