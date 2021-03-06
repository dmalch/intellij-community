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

package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.ide.util.gotoByName.GotoActionItemProvider;
import com.intellij.ide.util.gotoByName.GotoActionModel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class GotoActionAction extends GotoActionBase implements DumbAware {

  @Override
  public void gotoActionPerformed(final AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);

    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.action");
    final GotoActionModel model = new GotoActionModel(project, component, editor, file);
    final GotoActionCallback<Object> callback = new GotoActionCallback<Object>() {
      @Override
      public void elementChosen(ChooseByNamePopup popup, final Object element) {
        final String enteredText = popup.getEnteredText();
        openOptionOrPerformAction(((GotoActionModel.MatchedValue)element).value, enteredText, project, component, e);
      }
    };

    Pair<String, Integer> start = getInitialText(false, e);
    showNavigationPopup(callback, null, createPopup(project, model, start.first, start.second), false);
  }

  private static ChooseByNamePopup createPopup(final Project project, final GotoActionModel model, final String initialText, final int initialIndex) {
    final ChooseByNamePopup oldPopup = project == null ? null : project.getUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY);
    if (oldPopup != null) {
      oldPopup.close(false);
    }
    final ChooseByNamePopup popup = new ChooseByNamePopup(project, model, new GotoActionItemProvider(model), oldPopup, initialText, false, initialIndex) {
      @NotNull
      @Override
      protected Set<Object> filter(@NotNull Set<Object> elements) {
        return super.filter(model.sort(elements));
      }

      @Override
      protected boolean closeForbidden(boolean ok) {
        if (!ok) return false;
        Object element = getChosenElement();
        return element instanceof GotoActionModel.MatchedValue && processOptionInplace(((GotoActionModel.MatchedValue)element).value, this)
               || super.closeForbidden(true);
      }
    };

    if (project != null) {
      project.putUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, popup);
    }
    popup.addMouseClickListener(new MouseAdapter() {
      @Override
      public void mouseClicked(@NotNull MouseEvent e) {
        Object element = popup.getSelectionByPoint(e.getPoint());
        if (element instanceof GotoActionModel.MatchedValue) {
          if (processOptionInplace(((GotoActionModel.MatchedValue)element).value, popup)) {
            e.consume();
          }
        }
      }
    });
    return popup;
  }

  private static boolean processOptionInplace(Object value, ChooseByNamePopup popup) {
    if (value instanceof BooleanOptionDescription) {
      final BooleanOptionDescription option = (BooleanOptionDescription)value;
      option.setOptionState(!option.isOptionEnabled());
      if (popup != null) {
        popup.repaintList();
      }
      return true;
    }
    return false;
  }

  public static void openOptionOrPerformAction(Object element,
                                               final String enteredText,
                                               final Project project,
                                               final Component component,
                                               @Nullable final AnActionEvent e) {
    if (processOptionInplace(element, null)) {
      return;
    }
    if (element instanceof OptionDescription) {
      final String configurableId = ((OptionDescription)element).getConfigurableId();
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          ShowSettingsUtilImpl.showSettingsDialog(project, configurableId, enteredText);
        }
      });
    }
    else {
      //element could be AnAction (SearchEverywhere)
      final AnAction action = element instanceof AnAction ? (AnAction)element : ((GotoActionModel.ActionWrapper)element).getAction();
      if (action != null) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            if (component == null || !component.isShowing()) {
              return;
            }
            final Presentation presentation = action.getTemplatePresentation().clone();
            final DataContext context = DataManager.getInstance().getDataContext(component);
            final AnActionEvent event = new AnActionEvent(e == null ? null : e.getInputEvent(),
                                                          context,
                                                          ActionPlaces.ACTION_SEARCH,
                                                          presentation,
                                                          ActionManager.getInstance(),
                                                          e == null ? 0 : e.getModifiers());

            if (ActionUtil.lastUpdateAndCheckDumb(action, event, true)) {
              if (action instanceof ActionGroup) {
                JBPopupFactory.getInstance()
                  .createActionGroupPopup(presentation.getText(), (ActionGroup)action, context,
                                          JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false)
                  .showInBestPositionFor(context);
              } else {
                ActionUtil.performActionDumbAware(action, event);
              }
            }
          }
        }, ModalityState.NON_MODAL);
      }
    }
  }

  @Override
  protected boolean requiresProject() {
    return false;
  }
}