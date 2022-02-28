package com.fun90.idea.patcher;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.swing.clipboard.ClipboardUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.fun90.idea.constant.PluginConstant;
import com.fun90.idea.thread.SecureFxRunnable;
import com.fun90.idea.util.FilesUtil;
import com.fun90.idea.util.PatcherUtil;
import com.fun90.idea.util.PathResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PatcherDialog extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;

    private JTextField textField;
    private JButton fileChooseBtn;
    private JPanel filePanel;
    private JTextField projectNameTextField;
    private JComboBox<String> moduleComboBox;
    private JCheckBox openSecureFX;
    private JCheckBox sourceCheckBox;
    private JTextField textField1;
    private JTextField textField2;
    private AnActionEvent event;
    private JBList<VirtualFile> fileList;
    private Module module;
    private final PatcherConfig config;

    PatcherDialog(final AnActionEvent event) {
        this.event = event;
        this.config = PatcherConfig.getInstance(event.getProject());
        setTitle("Export Patcher Dialog");
        setContentPane(contentPane);
        setModal(true);
        textField1.setText(config.getOtherMap().get("author"));
        textField2.setText(config.getOtherMap().get("desc"));
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final ModuleManager moduleManager = ModuleManager.getInstance(Objects.requireNonNull(event.getProject()));
        Module[] modules = moduleManager.getModules();
        // 获取当前文件所属模块
        module = PatcherUtil.getModule(modules, event);
        final String userDir = System.getProperty("user.home");
        String exportPath = userDir + File.separator + "Desktop";
        if (config != null && config.getExportPathMap().containsKey(module.getName())) {
            exportPath = config.getExportPathMap().get(module.getName());
        }
        textField.setText(exportPath);
        // 保存路径按钮事件
        fileChooseBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(userDir);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            int flag = fileChooser.showOpenDialog(null);
            if (flag == JFileChooser.APPROVE_OPTION) {
                textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        // 增加空选项，防止第一项无法选中
        for (Module module : modules) {
            moduleComboBox.addItem(module.getName());
        }
        if (module != null) {
            moduleComboBox.setSelectedItem(module.getName());
            String projectName = config.getOtherMap().get("projectName");
            if (projectName != null) {
                projectNameTextField.setText(projectName);
            } else {
                projectNameTextField.setText(module.getName());
            }

        }
        moduleComboBox.addItemListener(e -> {
            if (StrUtil.isNotBlank((String) e.getItem())) {
                module = moduleManager.findModuleByName((String) e.getItem());
                projectNameTextField.setText(module.getName());
            }
        });
    }

    private void createUIComponents() {
        VirtualFile[] data = event.getData(LangDataKeys.VIRTUAL_FILE_ARRAY);
        if (data != null) {
            fileList = new JBList<>(data);
            fileList.setEmptyText("No File Selected!");
            ToolbarDecorator decorator = ToolbarDecorator.createDecorator(fileList);
            filePanel = decorator.createPanel();
        }
    }

    private void onOK() {
        // 条件校验
        if (null == textField.getText() || "".equals(textField.getText())) {
            Messages.showErrorDialog(this, "Please select save path!", "Error");
            return;
        }
        VirtualFile[] selectedFiles = event.getData(LangDataKeys.VIRTUAL_FILE_ARRAY);
        if (selectedFiles == null || selectedFiles.length == 0) {
            Messages.showErrorDialog("Please select at least one file!", "Error");
            return;
        }
        if (module == null) {
            Messages.showErrorDialog(this, "Please select module!", "Error");
            return;
        }
        Map<String, String> exportPathMap = config.getExportPathMap();
        if (StringUtil.isNotEmpty(textField.getText())) {
            exportPathMap.put(module.getName(), textField.getText());
        } else {
            exportPathMap.remove(module.getName());
        }
        if (sourceCheckBox.isSelected()) {
            this.execute(null);
            this.dispose();
        } else {
            CompileExecutor compileExecutor = new CompileExecutor(module, event);
            compileExecutor.run(this::execute, this::dispose);
        }

        //启动SecureFx
        if (openSecureFX.isSelected()) {
            Thread secureFxThread = new Thread(new SecureFxRunnable());
            secureFxThread.start();
        }

    }

    private void onCancel() {
        dispose();
    }

    private void execute(CompileContext compileContext) {
        String moduleName = projectNameTextField.getText().trim();
        // 设置导出目录
        String exportPath = textField.getText();
        if (exportPath.endsWith(File.separator)) {
            exportPath += moduleName + File.separator;
        } else {
            exportPath += File.separator + moduleName + File.separator;
        }
        Date date = new Date();
        String yearMonthDay = DateUtil.format(date, DatePattern.PURE_DATE_PATTERN);
        String time = DateUtil.format(date, "HHmm");

        String dirName = exportPath + yearMonthDay + time + File.separator + "ROOT" + File.separator;
        FileUtil.mkdir(dirName);

        ListModel<VirtualFile> selectedFiles = fileList.getModel();
        PathResult result = PatcherUtil.getPathResult(module, selectedFiles, dirName, compileContext);
        // 删除原有文件
//        if (openSecureFX.isSelected()) {
//            FilesUtil.delete(dirName);
//        }
        // 导出
        result.getFromTo().forEach(FilesUtil::copy);

        // 压缩文件
        String authorText = textField1.getText();
        String descText = textField2.getText();
        config.getOtherMap().put("author", authorText);
        config.getOtherMap().put("desc", descText);
        config.getOtherMap().put("projectName", moduleName);
        String zipName = exportPath + yearMonthDay + time + File.separator + yearMonthDay + PluginConstant.ZIP_SEPARATOR + time +
                PluginConstant.ZIP_SEPARATOR + authorText + PluginConstant.ZIP_SEPARATOR + descText + PluginConstant.ZIP_SEPARATOR + moduleName + ".zip";
        ZipUtil.zip(dirName, zipName, true);

        // 复制文件到剪切板
        ClipboardUtil.set(new Transferable() {
            DataFlavor[] dataFlavors = new DataFlavor[]{DataFlavor.javaFileListFlavor};

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return dataFlavors;
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                for (int i = 0; i < dataFlavors.length; i++) {
                    if (dataFlavors[i].equals(flavor)) {
                        return true;
                    }
                }
                return false;
            }

            @NotNull
            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                List<File> files = new ArrayList<File>();
                files.add(new File(dirName));
                return files;
            }
        });

        // 提示信息
        StringBuilder message = new StringBuilder();
        int notExportSize = result.getUnsettledList().size();
        int fileCount = selectedFiles.getSize() - notExportSize;
        message.append("Export ").append(fileCount).append(" files. ");
        if (fileCount != 0) {
            message.append("(<a href=\"file://").append(exportPath + yearMonthDay + time).append("\" target=\"blank\">open</a>)<br>");
        }
        if (notExportSize > 0) {
            message.append("<b>Warning:</b>");
            for (int i = 0; i < notExportSize; i++) {
                message.append(result.getUnsettledList().get(i));
                if (i < notExportSize - 1) {
                    message.append(",<br>");
                }
            }
            message.append(" <b>is not exported!</b><br><b>Please make sure web path is right and these files are not tests.</b>");
        }
        PatcherUtil.showInfo(message.toString(), event.getProject());
    }
}
