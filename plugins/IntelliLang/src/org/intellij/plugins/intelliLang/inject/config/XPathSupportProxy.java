/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.plugins.intelliLang.inject.config;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.IdentityCharTable;
import com.intellij.psi.impl.source.xml.XmlTokenImpl;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlElementType;
import org.intellij.lang.xpath.context.ContextProvider;
import org.intellij.lang.xpath.context.ContextType;
import org.intellij.lang.xpath.context.NamespaceContext;
import org.intellij.lang.xpath.context.VariableContext;
import org.intellij.lang.xpath.psi.XPathExpression;
import org.intellij.lang.xpath.psi.XPathType;
import org.intellij.plugins.xpathView.support.XPathSupport;
import org.intellij.plugins.xpathView.util.Namespace;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.Set;

/**
 * Proxy class that allows to avoid a hard compile time dependency on the XPathView plugin.
 */
public abstract class XPathSupportProxy {
  private static final Logger LOG = Logger.getInstance("org.intellij.plugins.intelliLang.inject.config.XPathSupportProxy");

  public static final Object UNSUPPORTED = "UNSUPPORTED";
  public static final Object INVALID = "INVALID";

  @NotNull
  public abstract XPath createXPath(String expression) throws JaxenException;

  public abstract void attachContext(@NotNull PsiFile file);

  private static XPathSupportProxy ourInstance;
  private static boolean isInitialized;

  @Nullable
  public static synchronized XPathSupportProxy getInstance() {
    if (isInitialized) {
      return ourInstance;
    }
    try {
      return ourInstance = ServiceManager.getService(XPathSupportProxy.class);
    } finally {
      if (ourInstance == null) {
        LOG.info("XPath Support is not available");
      }
      isInitialized = true;
    }
  }

  public static class Impl extends XPathSupportProxy {
    private static class Provider extends ContextProvider {
      private final XmlTokenImpl myDummyContext = new XmlTokenImpl(XmlElementType.XML_CONTENT_EMPTY, "", 0, 0, new IdentityCharTable()) {
        @Override
        public boolean isValid() {
          return true;
        }
      };

      @NotNull
      public ContextType getContextType() {
        return XPathSupport.TYPE;
      }

      @NotNull
      @Override
      public XPathType getExpectedType(XPathExpression expr) {
        return XPathType.BOOLEAN;
      }

      public XmlElement getContextElement() {
        // needed because the static method ContextProvider.isValid() checks this to determine if the provider
        // is still valid - refactor this into an instance method ContextProvider.isValid()?
        return myDummyContext;
      }

      public NamespaceContext getNamespaceContext() {
        return null;
      }

      public VariableContext getVariableContext() {
        return null;
      }

      public Set<QName> getAttributes(boolean forValidation) {
        return null;
      }

      public Set<QName> getElements(boolean forValidation) {
        return null;
      }
    }

    private final ContextProvider myProvider = new Provider();
    private final XPathSupport mySupport = XPathSupport.getInstance();

    @NotNull
    public XPath createXPath(String expression) throws JaxenException {
      return mySupport.createXPath(null, expression, Collections.<Namespace>emptyList());
    }

    public void attachContext(@NotNull PsiFile file) {
      myProvider.attachTo(file);
    }
  }
}