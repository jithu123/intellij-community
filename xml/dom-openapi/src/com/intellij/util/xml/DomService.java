/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util.xml;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
public abstract class DomService {
  private static DomService ourCachedInstance = null;

  public static DomService getInstance() {
    if (ourCachedInstance == null) {
      ourCachedInstance = ServiceManager.getService(DomService.class);
    }
    return ourCachedInstance;
  }

  /**
   * @param rootElementClass class of root (file-level) element in DOM model
   * @param scope            search scope
   * @return files containing given root element
   * @see #getFileElements(Class, Project, GlobalSearchScope)
   */
  @NotNull
  public abstract Collection<VirtualFile> getDomFileCandidates(@NotNull Class<? extends DomElement> rootElementClass,
                                                               @NotNull GlobalSearchScope scope);

  // used externally
  @NotNull
  public Collection<VirtualFile> getDomFileCandidates(@NotNull Class<? extends DomElement> rootElementClass,
                                                               Project project,
                                                               @NotNull GlobalSearchScope scope) {
    return getDomFileCandidates(rootElementClass, scope);
  }

  /**
   * @param rootElementClass class of root (file-level) element in DOM model
   * @param project          current project
   * @param scope            search scope
   * @return DOM file elements containing given root element
   */
  @NotNull
  public abstract <T extends DomElement> List<DomFileElement<T>> getFileElements(@NotNull Class<T> rootElementClass,
                                                                                 @NotNull Project project,
                                                                                 @Nullable GlobalSearchScope scope);

  @NotNull
  public abstract ModelMerger createModelMerger();

  public abstract <T extends DomElement> DomAnchor<T> createAnchor(T domElement);

  @NotNull
  public abstract XmlFile getContainingFile(@NotNull DomElement domElement);

  @NotNull
  public abstract EvaluatedXmlName getEvaluatedXmlName(@NotNull DomElement element);

  @NotNull
  public abstract XmlFileHeader getXmlFileHeader(@NotNull XmlFile file);

  public enum StructureViewMode {
    SHOW, SHOW_CHILDREN, SKIP
  }

  @NotNull
  public abstract StructureViewBuilder createSimpleStructureViewBuilder(@NotNull XmlFile file, @NotNull Function<DomElement, StructureViewMode> modeProvider);
}
