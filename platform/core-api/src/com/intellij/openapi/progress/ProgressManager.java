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
package com.intellij.openapi.progress;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.containers.ConcurrentHashSet;
import com.intellij.util.containers.SmartHashSet;
import gnu.trove.THashMap;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ProgressManager extends ProgressIndicatorProvider {
  private static class ProgressManagerHolder {
    private static final ProgressManager ourInstance = ServiceManager.getService(ProgressManager.class);
  }

  @NotNull
  public static ProgressManager getInstance() {
    return ProgressManagerHolder.ourInstance;
  }

  public abstract boolean hasProgressIndicator();
  public abstract boolean hasModalProgressIndicator();
  public abstract boolean hasUnsafeProgressIndicator();

  public abstract void runProcess(@NotNull Runnable process, ProgressIndicator progress) throws ProcessCanceledException;
  public abstract <T> T runProcess(@NotNull Computable<T> process, ProgressIndicator progress) throws ProcessCanceledException;

  @Override
  public ProgressIndicator getProgressIndicator() {
    return myThreadIndicator.get();
  }

  public static void progress(@NotNull String text) throws ProcessCanceledException {
    progress(text, "");
  }

  public static void progress2(@NotNull final String text) throws ProcessCanceledException {
    final ProgressIndicator pi = getInstance().getProgressIndicator();
    if (pi != null) {
      pi.checkCanceled();
      pi.setText2(text);
    }
  }

  public static void progress(@NotNull String text, @Nullable String text2) throws ProcessCanceledException {
    final ProgressIndicator pi = getInstance().getProgressIndicator();
    if (pi != null) {
      pi.checkCanceled();
      pi.setText(text);
      pi.setText2(text2 == null ? "" : text2);
    }
  }

  public abstract void executeNonCancelableSection(@NotNull Runnable runnable);

  public abstract void setCancelButtonText(String cancelButtonText);


  /**
   * Runs the specified operation in a background thread and shows a modal progress dialog in the
   * main thread while the operation is executing.
   *
   * @param process       the operation to execute.
   * @param progressTitle the title of the progress window.
   * @param canBeCanceled whether "Cancel" button is shown on the progress window.
   * @param project       the project in the context of which the operation is executed.
   * @return true if the operation completed successfully, false if it was cancelled.
   */
  public abstract boolean runProcessWithProgressSynchronously(@NotNull Runnable process,
                                                              @NotNull @Nls String progressTitle,
                                                              boolean canBeCanceled,
                                                              @Nullable Project project);

  /**
   * Runs the specified operation in a background thread and shows a modal progress dialog in the
   * main thread while the operation is executing.
   *
   * @param process       the operation to execute.
   * @param progressTitle the title of the progress window.
   * @param canBeCanceled whether "Cancel" button is shown on the progress window.
   * @param project       the project in the context of which the operation is executed.
   * @return true result of operation
   * @throws E exception thrown by process
   */
  public abstract <T, E extends Exception> T runProcessWithProgressSynchronously(@NotNull ThrowableComputable<T, E> process,
                                                                                 @NotNull @Nls String progressTitle,
                                                                                 boolean canBeCanceled,
                                                                                 @Nullable Project project) throws E;

  /**
   * Runs the specified operation in a background thread and shows a modal progress dialog in the
   * main thread while the operation is executing.
   *
   * @param process         the operation to execute.
   * @param progressTitle   the title of the progress window.
   * @param canBeCanceled   whether "Cancel" button is shown on the progress window.
   * @param project         the project in the context of which the operation is executed.
   * @param parentComponent the component which will be used to calculate the progress window ancestor
   * @return true if the operation completed successfully, false if it was cancelled.
   */
  public abstract boolean runProcessWithProgressSynchronously(@NotNull Runnable process,
                                                              @NotNull @Nls String progressTitle,
                                                              boolean canBeCanceled,
                                                              @Nullable Project project,
                                                              @Nullable JComponent parentComponent);

  /**
   * Runs a specified <code>process</code> in a background thread and shows a progress dialog, which can be made non-modal by pressing
   * background button. Upon successful termination of the process a <code>successRunnable</code> will be called in Swing UI thread and
   * <code>canceledRunnable</code> will be called if terminated on behalf of the user by pressing either cancel button, while running in
   * a modal state or stop button if running in background.
   *
   * @param project          the project in the context of which the operation is executed.
   * @param progressTitle    the title of the progress window.
   * @param process          the operation to execute.
   * @param successRunnable  a callback to be called in Swing UI thread upon normal termination of the process.
   * @param canceledRunnable a callback to be called in Swing UI thread if the process have been canceled by the user.
   * @deprecated use {@link #run(com.intellij.openapi.progress.Task)}
   */
  public abstract void runProcessWithProgressAsynchronously(@NotNull Project project,
                                                            @NotNull @Nls String progressTitle,
                                                            @NotNull Runnable process,
                                                            @Nullable Runnable successRunnable,
                                                            @Nullable Runnable canceledRunnable);
  /**
   * Runs a specified <code>process</code> in a background thread and shows a progress dialog, which can be made non-modal by pressing
   * background button. Upon successful termination of the process a <code>successRunnable</code> will be called in Swing UI thread and
   * <code>canceledRunnable</code> will be called if terminated on behalf of the user by pressing either cancel button, while running in
   * a modal state or stop button if running in background.
   *
   * @param project          the project in the context of which the operation is executed.
   * @param progressTitle    the title of the progress window.
   * @param process          the operation to execute.
   * @param successRunnable  a callback to be called in Swing UI thread upon normal termination of the process.
   * @param canceledRunnable a callback to be called in Swing UI thread if the process have been canceled by the user.
   * @param option           progress indicator behavior controller.
   * @deprecated use {@link #run(com.intellij.openapi.progress.Task)}
   */
  public abstract void runProcessWithProgressAsynchronously(@NotNull Project project,
                                                            @NotNull @Nls String progressTitle,
                                                            @NotNull Runnable process,
                                                            @Nullable Runnable successRunnable,
                                                            @Nullable Runnable canceledRunnable,
                                                            @NotNull PerformInBackgroundOption option);

  /**
   * Runs a specified <code>task</code> in either background/foreground thread and shows a progress dialog.
   *
   * @param task task to run (either {@link com.intellij.openapi.progress.Task.Modal}
   *             or {@link com.intellij.openapi.progress.Task.Backgroundable}).
   */
  public abstract void run(@NotNull Task task);

  public abstract void runProcessWithProgressAsynchronously(@NotNull Task.Backgroundable task, @NotNull ProgressIndicator progressIndicator);

  protected static final ThreadLocal<ProgressIndicator> myThreadIndicator = new ThreadLocal<ProgressIndicator>();

  // indicator -> threads which are running under this indicator
  private static final Map<ProgressIndicator, Set<Thread>> threadsUnderIndicator = new THashMap<ProgressIndicator, Set<Thread>>();
  // threads which are running under canceled indicator
  private static final Set<Thread> threadsUnderCanceledIndicator = new ConcurrentHashSet<Thread>();

  public static void canceled(@NotNull ProgressIndicator indicator) {
    // mark threads running under this indicator as canceled
    synchronized (threadsUnderIndicator) {
      Set<Thread> threads = threadsUnderIndicator.get(indicator);
      if (threads != null) {
        threadsUnderCanceledIndicator.addAll(threads);
      }
    }
  }

  public static void checkCanceled() throws ProcessCanceledException {
    boolean thereIsCanceledIndicator = !threadsUnderCanceledIndicator.isEmpty();
    if (thereIsCanceledIndicator) {
      getInstance().doCheckCanceled();
    }
  }

  private static final Collection<ProgressIndicator> nonStandardIndicators = new ConcurrentHashSet<ProgressIndicator>();
  protected static void callCheckCancelForNonStandardIndicators() {
    for (ProgressIndicator indicator : nonStandardIndicators) {
      try {
        indicator.checkCanceled();
      }
      catch (ProcessCanceledException e) {
        indicator.cancel();
      }
    }
  }


  public void executeProcessUnderProgress(@NotNull Runnable process,
                                          @Nullable("null means reuse current progress") ProgressIndicator progress)
    throws ProcessCanceledException {
    ProgressIndicator oldIndicator = null;
    boolean set = progress != null && progress != (oldIndicator = getProgressIndicator());
    if (set) {
      myThreadIndicator.set(progress);
      try {
        registerIndicatorAndRun(progress, Thread.currentThread(), process);
      }
      finally {
        myThreadIndicator.set(oldIndicator);
      }
    }
    else {
      process.run();
    }
  }

  private static void registerIndicatorAndRun(@NotNull ProgressIndicator progress, @NotNull Thread currentThread, @NotNull Runnable process) {
    Set<Thread> underIndicator;
    boolean alreadyUnder;
    boolean addedToPerverse;
    synchronized (threadsUnderIndicator) {
      underIndicator = threadsUnderIndicator.get(progress);
      if (underIndicator == null) {
        underIndicator = new SmartHashSet<Thread>();
        threadsUnderIndicator.put(progress, underIndicator);
      }
      alreadyUnder = !underIndicator.add(currentThread);
      addedToPerverse = !(progress instanceof StandardProgressIndicator) && nonStandardIndicators.add(progress);
    }

    try {
      if (progress instanceof WrappedProgressIndicator) {
        registerIndicatorAndRun(((WrappedProgressIndicator)progress).getOriginalProgressIndicator(), currentThread, process);
      }
      else {
        process.run();
      }
    }
    finally {
      synchronized (threadsUnderIndicator) {
        boolean removed = alreadyUnder || underIndicator.remove(currentThread);
        if (removed && underIndicator.isEmpty()) {
          threadsUnderIndicator.remove(progress);
        }
        threadsUnderCanceledIndicator.remove(currentThread);
        if (addedToPerverse) {
          nonStandardIndicators.remove(progress);
        }
      }
    }
  }
}