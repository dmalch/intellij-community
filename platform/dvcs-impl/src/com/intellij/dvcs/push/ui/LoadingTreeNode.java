/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.dvcs.push.ui;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.JBImageIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.net.URL;

public class LoadingTreeNode extends DefaultMutableTreeNode implements CustomRenderedTreeNode {
  @NotNull protected ImageIcon myLoadingIcon;
  private static final String LOADING_ICON = "/icons/loading.gif";

  @NotNull
  public ImageIcon getIcon() {
    return myLoadingIcon;
  }

  public LoadingTreeNode() {
    super(null, false);
    URL loadingIconUrl = getClass().getResource(LOADING_ICON);
    Image image = ImageLoader.loadFromUrl(loadingIconUrl);
    myLoadingIcon = new JBImageIcon(image);
  }

  @Override
  public void render(@NotNull ColoredTreeCellRenderer renderer) {
    renderer.setIcon(myLoadingIcon);
    // loading icon should be on the left and do not inherit parent config
    renderer.setIconOnTheRight(false);
    renderer.append("Loading Commits...", new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.GRAY));
  }
}
