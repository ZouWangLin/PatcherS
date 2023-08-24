package com.fun90.idea.thread;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.fun90.idea.constant.PluginConstant;
import com.fun90.idea.util.PatcherUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;

import javax.swing.*;
import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author zouwanglin
 */
public class BootSubmitRunnable implements Runnable {
    private JTextField projectNameTextField;
    private JTextField textField1;
    private JTextField textField2;
    private JTextField textField4;
    private JTextArea textArea1;
    private AnActionEvent event;
    private Module module;
    private JCheckBox svnCheckBox;
    private JTextField textField3;

    public BootSubmitRunnable(JTextField projectNameTextField, JTextField textField1, JTextField textField2, JTextField textField4, JTextArea textArea1, AnActionEvent event, Module module, JCheckBox svnCheckBox, JTextField textField3) {
        this.projectNameTextField = projectNameTextField;
        this.textField1 = textField1;
        this.textField2 = textField2;
        this.textField4 = textField4;
        this.textArea1 = textArea1;
        this.event = event;
        this.module = module;
        this.svnCheckBox = svnCheckBox;
        this.textField3 = textField3;
    }

    @Override
    public void run() {
        //读取target目录下.jar文件
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        String contentRoot = moduleRootManager.getContentRoots()[0].getPath();
        String targetPath = contentRoot + File.separator + "target";
        File targetFile = new File(targetPath);
        File[] files = targetFile.listFiles();
        String jarName = "";
        for (File file : files) {
            if (file.getName().endsWith(".jar")) {
                jarName = file.getName();
            }
        }

        if (StrUtil.isBlank(jarName)) {
            PatcherUtil.showInfo("jarName is null", event.getProject());
            ThreadUtil.sleep(60, TimeUnit.SECONDS);
            return;
        }

        //获取jar包的绝对路径
        String jarPath = targetPath + File.separator + jarName;

        //提交svn
        String text = textField3.getText();


        //beta,E:\HiggsOceanWar\后台\ROOT;pro,E:\OCEANWAR后台[正式]\2023-04;beta
        String[] split = StrUtil.split(text, ";");
        String beta = split[0];
        String pro = split[1];
        String activeEnv = split[2];

        if (StrUtil.equals("beta", activeEnv)) {
            String path = beta.split(",")[1];
            String svnFullPath = path + "/" + jarName;
            //复制文件到svn目录
            FileUtil.copy(new File(jarPath), new File(svnFullPath), true);
            //提交svn
            RuntimeUtil.execForStr("svn add " + svnFullPath);
            RuntimeUtil.execForStr("svn commit " + svnFullPath + " -m \"" + "\"");
            ThreadUtil.sleep(10000);

            PatcherUtil.showInfo("success deploy boot", event.getProject());
        } else if (StrUtil.equals("pro", activeEnv)) {
            String moduleName = projectNameTextField.getText().trim();
            String path = pro.split(",")[1];
            String svnFullPath = path + "/" + jarName;
            //复制文件到svn目录
            FileUtil.copy(new File(jarPath), new File(svnFullPath), true);

            File zip = ZipUtil.zip(jarPath, svnFullPath);
            //重命名
            Date date = new Date();
            String yearMonthDay = DateUtil.format(date, DatePattern.PURE_DATE_PATTERN);
            String time = DateUtil.format(date, "HHmm");
            String authorText = textField1.getText();
            String descText = textField2.getText();

            String newFileName = yearMonthDay + PluginConstant.ZIP_SEPARATOR + time + PluginConstant.ZIP_SEPARATOR + authorText + PluginConstant.ZIP_SEPARATOR + descText + PluginConstant.ZIP_SEPARATOR + moduleName + ".zip";
            FileUtil.rename(zip, newFileName, false, true);

            //打开谷歌浏览器
            String runChrome = "cmd /c start chrome https://devops.pocketcity.com/deploy-approval?workflow_id=1";
            RuntimeUtil.execForStr(runChrome);

            //提交svn
            String newPath = path + "/" + newFileName;

            //提交svn
            RuntimeUtil.execForStr("svn add " + newPath);
            RuntimeUtil.execForStr("svn commit " + newPath + " -m \"" + "\"");
            ThreadUtil.sleep(10000);

        }
    }
}
