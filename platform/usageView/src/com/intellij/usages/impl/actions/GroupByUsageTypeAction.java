// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.usages.impl.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.usageView.UsageViewBundle;
import org.jetbrains.annotations.NotNull;

class GroupByUsageTypeAction extends RuleAction {
  GroupByUsageTypeAction() {
    super(UsageViewBundle.messagePointer("action.group.by.usage.type"), AllIcons.General.Filter); //TODO: special icon
  }

  @Override
  protected boolean getOptionValue(@NotNull AnActionEvent e) {
    return getUsageViewSettings(e).isGroupByUsageType();
  }

  @Override
  protected void setOptionValue(@NotNull AnActionEvent e, boolean value) {
    getUsageViewSettings(e).setGroupByUsageType(value);
  }
}
