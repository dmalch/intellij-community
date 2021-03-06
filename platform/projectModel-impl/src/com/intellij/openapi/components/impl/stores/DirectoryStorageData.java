/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.components.impl.stores;

import com.intellij.application.options.PathMacrosCollector;
import com.intellij.openapi.components.StateSplitter;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PairConsumer;
import gnu.trove.THashMap;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DirectoryStorageData {
  private static final Logger LOG = Logger.getInstance(DirectoryStorageData.class);

  private Map<String, Map<File, Element>> myStates = new THashMap<String, Map<File, Element>>();
  private long myLastTimestamp = 0;
  private DirectoryStorageData myOriginalData;

  public Set<String> getComponentNames() {
    return myStates.keySet();
  }

  public void loadFrom(final @Nullable VirtualFile dir, TrackingPathMacroSubstitutor pathMacroSubstitutor) {
    if (dir == null || !dir.exists()) {
      return;
    }

    for (VirtualFile file : dir.getChildren()) {
      if (!StringUtil.endsWithIgnoreCase(file.getName(), ".xml")) {
        //do not load system files like .DS_Store on Mac
        continue;
      }

      try {
        final Document document = JDOMUtil.loadDocument(file.contentsToByteArray());
        final Element element = document.getRootElement();
        if (!element.getName().equals(StorageData.COMPONENT)) {
          LOG.error("Incorrect root tag name (" + element.getName() + ") in " + file.getPresentableUrl());
          continue;
        }

        String componentName = element.getAttributeValue(StorageData.NAME);
        if (componentName == null) {
          LOG.error("Component name isn't specified in " + file.getPresentableUrl());
          continue;
        }

        if (pathMacroSubstitutor != null) {
          pathMacroSubstitutor.expandPaths(element);

          final Set<String> unknownMacros = PathMacrosCollector.getMacroNames(element);
          pathMacroSubstitutor.addUnknownMacros(componentName, unknownMacros);
        }

        put(componentName, new File(file.getPath()), element, true);
      }
      catch (IOException e) {
        LOG.info("Unable to load state", e);
      }
      catch (JDOMException e) {
        LOG.info("Unable to load state", e);
      }
    }
  }

  public void put(final String componentName, File file, final Element element, final boolean updateTimestamp) {
    LOG.assertTrue(componentName != null, String.format("Component name should not be null for file: %s", file == null ? "NULL!" : file.getPath()));

    Map<File, Element> stateMap = myStates.get(componentName);
    if (stateMap == null) {
      stateMap = new THashMap<File, Element>();
      myStates.put(componentName, stateMap);
    }

    stateMap.put(file, element);
    if (updateTimestamp) {
      updateLastTimestamp(file);
    }
  }

  public void updateLastTimestamp(File file) {
    myLastTimestamp = Math.max(myLastTimestamp, file.lastModified());
    if (myOriginalData != null) {
      myOriginalData.myLastTimestamp = myLastTimestamp;
    }
  }

  public long getLastTimeStamp() {
    return myLastTimestamp;
  }

  public Map<File, Long> getAllStorageFiles() {
    final Map<File, Long> allStorageFiles = new THashMap<File, Long>();
    process(new StorageDataProcessor() {
      @Override
      public void process(final String componentName, final File file, final Element element) {
        allStorageFiles.put(file, file.lastModified());
      }
    });

    return allStorageFiles;
  }

  public void processComponent(@NotNull final String componentName, @NotNull final PairConsumer<File, Element> consumer) {
    final Map<File, Element> map = myStates.get(componentName);
    if (map != null) {
      for (File file : map.keySet()) {
        consumer.consume(file, map.get(file));
      }
    }
  }

  public void process(@NotNull final StorageDataProcessor processor) {
    for (final String componentName : myStates.keySet()) {
      processComponent(componentName, new PairConsumer<File, Element>() {
        @Override
        public void consume(File file, Element element) {
          processor.process(componentName, file, element);
        }
      });
    }
  }

  @Override
  protected DirectoryStorageData clone() {
    final DirectoryStorageData result = new DirectoryStorageData();
    result.myStates = new HashMap<String, Map<File, Element>>(myStates);
    result.myLastTimestamp = myLastTimestamp;
    result.myOriginalData = this;
    return result;
  }

  public void clear() {
    myStates.clear();
    myOriginalData = null;
  }

  public boolean containsComponent(final String componentName) {
    return myStates.get(componentName) != null;
  }

  public void removeComponent(final String componentName) {
    myStates.remove(componentName);
  }

  @Nullable
  public <T> T getMergedState(String componentName, Class<T> stateClass, StateSplitter splitter, @Nullable T mergeInto) {
    final List<Element> subElements = new ArrayList<Element>();
    processComponent(componentName, new PairConsumer<File, Element>() {
      @Override
      public void consume(File file, Element element) {
        final List children = element.getChildren();
        assert children.size() == 1 : JDOMUtil.writeElement(element, File.separator);
        final Element subElement = (Element)children.get(0);
        subElement.detach();
        subElements.add(subElement);
      }
    });

    final Element state = new Element(StorageData.COMPONENT);
    splitter.mergeStatesInto(state, subElements.toArray(new Element[subElements.size()]));
    removeComponent(componentName);

    return DefaultStateSerializer.deserializeState(state, stateClass, mergeInto);
  }

  interface StorageDataProcessor {
    void process(String componentName, File file, Element element);
  }
}
