// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package git4idea.index.actions

import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionExtensionProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.changes.ui.ChangeDiffRequestChain
import com.intellij.util.containers.isEmpty
import git4idea.index.createThreeSidesDiffRequestProducer
import git4idea.index.createTwoSidesDiffRequestProducer
import git4idea.index.ui.GIT_FILE_STATUS_NODES_STREAM
import git4idea.index.ui.GIT_STAGE_TREE

class GitStageDiffAction : AnActionExtensionProvider {
  override fun isActive(e: AnActionEvent): Boolean = e.getData(GIT_STAGE_TREE) != null

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project != null &&
                               !e.getData(GIT_FILE_STATUS_NODES_STREAM).isEmpty()
    e.presentation.isVisible = e.presentation.isEnabled || e.isFromActionToolbar
  }

  override fun actionPerformed(e: AnActionEvent) {
    val producers = e.getRequiredData(GIT_STAGE_TREE).statusNodesListSelection(true)
      .map { createTwoSidesDiffRequestProducer(e.project!!, it) }
    DiffManager.getInstance().showDiff(e.project, ChangeDiffRequestChain(producers.list, producers.selectedIndex), DiffDialogHints.DEFAULT)
  }
}

class GitStageThreeSideDiffAction : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project != null &&
                               e.getData(GIT_STAGE_TREE) != null &&
                               !e.getData(GIT_FILE_STATUS_NODES_STREAM).isEmpty()
    e.presentation.isVisible = e.presentation.isEnabled || e.isFromActionToolbar
  }

  override fun actionPerformed(e: AnActionEvent) {
    val producers = e.getRequiredData(GIT_STAGE_TREE).statusNodesListSelection(false)
      .map { createThreeSidesDiffRequestProducer(e.project!!, it) }
    DiffManager.getInstance().showDiff(e.project, ChangeDiffRequestChain(producers.list, producers.selectedIndex), DiffDialogHints.DEFAULT)
  }
}