/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.debugger.engine.evaluation.expression;

import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.sun.jdi.LocalVariable;

/**
 * User: lex
 * Date: Oct 15, 2003
 * Time: 11:39:04 PM
 *
 * todo [lex] does this interface really required?
 */
public interface InspectLocal extends InspectEntity{
  StackFrameProxyImpl getStackFrame();
  LocalVariable   getLocal     ();
}
