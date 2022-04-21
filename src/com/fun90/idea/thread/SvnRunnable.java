package com.fun90.idea.thread;

import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.ZipUtil;

/**
 * @author zouwanglin
 */
public class SvnRunnable implements Runnable {

    private String svnFullPath;
    private String dirName;

    public SvnRunnable(String svnFullPath, String dirName) {
        this.svnFullPath = svnFullPath;
        this.dirName = dirName;
    }

    @Override
    public void run() {
        ZipUtil.zip(dirName, svnFullPath, true);
        RuntimeUtil.exec("svn add " + svnFullPath);
        RuntimeUtil.exec("svn commit " + svnFullPath + " -m \"" + "\"");
    }


}
