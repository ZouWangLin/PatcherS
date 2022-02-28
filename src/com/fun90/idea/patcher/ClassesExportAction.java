package com.fun90.idea.patcher;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiJavaFile;

import java.io.File;

/**
 * Created by serical on 2017/3/17.
 */
public class ClassesExportAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            DataContext dataContext = e.getDataContext();
            PsiJavaFile javaFile = (PsiJavaFile) LangDataKeys.PSI_FILE.getData(dataContext).getContainingFile();
            String sourceName = javaFile.getName();
            Module module = LangDataKeys.MODULE.getData(dataContext);
            String compileRoot = CompilerModuleExtension.getInstance(module).getCompilerOutputPath().getPath();
            getVirtualFile(sourceName, CompilerModuleExtension.getInstance(module).getCompilerOutputPath().getChildren(), compileRoot);
            VirtualFileManager.getInstance().syncRefresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            Messages.showErrorDialog("Please build your module or project!!!", "error");
        }
    }

    private void getVirtualFile(String sourceName, VirtualFile virtualFile[], String compileRoot)
            throws Exception {
        if (!ArrayUtil.isEmpty(virtualFile)) {
            VirtualFile arr$[] = virtualFile;
            int len$ = arr$.length;
            for (int i$ = 0; i$ < len$; i$++) {
                VirtualFile vf = arr$[i$];
                String srcName;
                if (StrUtil.indexOf(vf.toString(), '$') != -1) {
                    srcName = StrUtil.sub(vf.toString(), StrUtil.lastIndexOfIgnoreCase(vf.toString(), "/") + 1, StrUtil.indexOf(vf.toString(), '$'));
                } else {
                    srcName = StrUtil.sub(vf.toString(), StrUtil.lastIndexOfIgnoreCase(vf.toString(), "/") + 1,
                            StrUtil.length(vf.toString()) - 6);
                }
                String dstName = StrUtil.sub(sourceName, 0, StrUtil.length(sourceName) - 5);
                if (StrUtil.equals(srcName, dstName)) {
                    String outRoot = (new StringBuilder()).append(StrUtil.sub(compileRoot, 0, StrUtil.lastIndexOfIgnoreCase(compileRoot, "/"))).append("/out").toString();
                    String packagePath = StrUtil.sub(vf.getPath(), StrUtil.length(compileRoot), StrUtil.length(vf.getPath()));
                    File s = new File(vf.getPath());
                    File t = new File((new StringBuilder()).append(outRoot).append(packagePath).toString());
                    FileUtil.copy(s, t);
                }
                if (!ArrayUtil.isEmpty(virtualFile)) {
                    getVirtualFile(sourceName, vf.getChildren(), compileRoot);
                }

            }

        }
    }
}
