package org.editorconfig.configmanagement;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.FileIndentOptionsProvider;
import org.editorconfig.Utils;
import org.editorconfig.core.EditorConfig;
import org.editorconfig.plugincomponents.SettingsProviderComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public class EditorConfigIndentOptionsProvider extends FileIndentOptionsProvider {
  // Handles the following EditorConfig settings:
  private static final String indentSizeKey = "indent_size";
  private static final String tabWidthKey = "tab_width";
  private static final String indentStyleKey = "indent_style";

  @Nullable
  @Override
  public CommonCodeStyleSettings.IndentOptions getIndentOptions(@NotNull PsiFile psiFile) {
    final VirtualFile file = psiFile.getVirtualFile();
    if (file == null || !file.isInLocalFileSystem()) return null;

    final Project project = psiFile.getProject();
    final CodeStyleSettings currentSettings = CodeStyleSettingsManager.getInstance(project).getCurrentSettings();
    if (!Utils.isEnabled(currentSettings)) return null;

    // Get editorconfig settings
    final String filePath = file.getCanonicalPath();
    final SettingsProviderComponent settingsProvider = SettingsProviderComponent.getInstance();
    final List<EditorConfig.OutPair> outPairs = settingsProvider.getOutPairs(filePath);
    // Apply editorconfig settings for the current editor
    return applyCodeStyleSettings(project, outPairs, file);
  }

  private static CommonCodeStyleSettings.IndentOptions applyCodeStyleSettings(Project project,
                                                                              final List<EditorConfig.OutPair> outPairs,
                                                                              final VirtualFile file) {
    // Apply indent options
    final String indentSize = Utils.configValueForKey(outPairs, indentSizeKey);
    final String tabWidth = Utils.configValueForKey(outPairs, tabWidthKey);
    final String indentStyle = Utils.configValueForKey(outPairs, indentStyleKey);
    final CommonCodeStyleSettings.IndentOptions indentOptions = new CommonCodeStyleSettings.IndentOptions();
    if (applyIndentOptions(project, indentOptions, indentSize, tabWidth, indentStyle, file.getCanonicalPath())) {
      return indentOptions;
    }
    return null;
  }

  private static boolean applyIndentOptions(Project project, CommonCodeStyleSettings.IndentOptions indentOptions,
                                            String indentSize, String tabWidth, String indentStyle, String filePath) {
    boolean changed = false;
    final String calculatedIndentSize = calculateIndentSize(tabWidth, indentSize);
    final String calculatedTabWidth = calculateTabWidth(tabWidth, indentSize);
    if (!calculatedIndentSize.isEmpty()) {
      if (applyIndentSize(indentOptions, calculatedIndentSize)) {
        Utils.appliedConfigMessage(project, calculatedIndentSize, indentSizeKey, filePath);
        changed = true;
      } else {
        Utils.invalidConfigMessage(project, calculatedIndentSize, indentSizeKey, filePath);
      }
    }
    if (!calculatedTabWidth.isEmpty()) {
      if (applyTabWidth(indentOptions, calculatedTabWidth)) {
        Utils.appliedConfigMessage(project, calculatedTabWidth, tabWidthKey, filePath);
        changed = true;
      }
      else {
        Utils.invalidConfigMessage(project, calculatedTabWidth, tabWidthKey, filePath);
      }
    }
    if (!indentStyle.isEmpty()) {
      if (applyIndentStyle(indentOptions, indentStyle)) {
        Utils.appliedConfigMessage(project, indentStyle, indentStyleKey, filePath);
        changed = true;
      }
      else {
        Utils.invalidConfigMessage(project, indentStyle, indentStyleKey, filePath);
      }
    }
    return changed;
  }

  private static String calculateIndentSize(final String tabWidth, final String indentSize) {
    return indentSize.equals("tab") ? tabWidth : indentSize;
  }

  private static String calculateTabWidth(final String tabWidth, final String indentSize) {
    if (tabWidth.isEmpty() && indentSize.equals("tab")) {
      return "";
    }
    else if (tabWidth.isEmpty()) {
      return indentSize;
    }
    else {
      return tabWidth;
    }
  }

  private static boolean applyIndentSize(final CommonCodeStyleSettings.IndentOptions indentOptions, final String indentSize) {
    try {
      int indent = Integer.parseInt(indentSize);
      indentOptions.INDENT_SIZE = indent;
      indentOptions.CONTINUATION_INDENT_SIZE = indent;
      return true;
    }
    catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean applyTabWidth(final CommonCodeStyleSettings.IndentOptions indentOptions, final String tabWidth) {
    try {
      indentOptions.TAB_SIZE = Integer.parseInt(tabWidth);
      return true;
    }
    catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean applyIndentStyle(CommonCodeStyleSettings.IndentOptions indentOptions, String indentStyle) {
    if (indentStyle.equals("tab") || indentStyle.equals("space")) {
      indentOptions.USE_TAB_CHARACTER = indentStyle.equals("tab");
      return true;
    }
    return false;
  }
}
