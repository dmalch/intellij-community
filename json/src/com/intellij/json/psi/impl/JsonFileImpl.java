package com.intellij.json.psi.impl;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import com.intellij.json.psi.JsonFile;
import com.intellij.json.psi.JsonValue;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonFileImpl extends PsiFileBase implements JsonFile {

  public JsonFileImpl(FileViewProvider fileViewProvider) {
    super(fileViewProvider, JsonLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return JsonFileType.INSTANCE;
  }

  @Nullable
  @Override
  public JsonValue getTopLevelValue() {
    return PsiTreeUtil.getChildOfType(this, JsonValue.class);
  }

  @Override
  public String toString() {
    final VirtualFile virtualFile = getVirtualFile();
    return String.format("JsonFile: " + (virtualFile != null? virtualFile.getName() : "<unknown>"));
  }
}
