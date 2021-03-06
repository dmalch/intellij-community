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

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.issueLinks.IssueLinkHtmlRenderer;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.vcs.log.VcsFullCommitDetails;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

public class VcsFullCommitDetailsNode extends DefaultMutableTreeNode implements CustomRenderedTreeNode, TooltipNode {

  @NotNull private final Project myProject;
  private final VcsFullCommitDetails myCommit;

  public VcsFullCommitDetailsNode(@NotNull Project project, @NotNull VcsFullCommitDetails commit) {
    super(commit, false);
    myProject = project;
    myCommit = commit;
  }

  @Override
  public VcsFullCommitDetails getUserObject() {
    return myCommit;
  }

  @Override
  public void render(@NotNull ColoredTreeCellRenderer renderer) {
    String subject = StringUtil.shortenTextWithEllipsis(myCommit.getSubject(), 80, 0);
    renderer.append(subject, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, renderer.getForeground()));
  }

  public String getTooltip() {
    return DvcsUtil.getShortHash(myCommit.getId().toString()) +
           "  " +
           DvcsUtil.getDateString(myCommit) +
           "  by " +
           myCommit.getAuthor().getName() +
           "\n\n" +
           IssueLinkHtmlRenderer.formatTextWithLinks(myProject, myCommit.getFullMessage());
  }
}
