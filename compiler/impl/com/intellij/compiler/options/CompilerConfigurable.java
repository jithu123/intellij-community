package com.intellij.compiler.options;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.CompilerSettingsFactory;
import com.intellij.compiler.RmicSettings;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.options.ExcludedEntriesConfigurable;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.FileStatusManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CompilerConfigurable extends SearchableConfigurable.Parent.Abstract {
  private Project myProject;
  private static final Icon ICON = IconLoader.getIcon("/general/configurableCompiler.png");
  private CompilerUIConfigurable myCompilerUIConfigurable;

  public static CompilerConfigurable getInstance(Project project) {
    return ShowSettingsUtil.getInstance().findProjectConfigurable(project, CompilerConfigurable.class);
  }

  public CompilerConfigurable(Project project) {
    myProject = project;
    myCompilerUIConfigurable = new CompilerUIConfigurable(myProject);
  }

  public String getDisplayName() {
    return CompilerBundle.message("compiler.configurable.display.name");
  }

  public Icon getIcon() {
    return ICON;
  }

  public String getHelpTopic() {
    return "project.propCompiler";
  }

  public String getId() {
    return getHelpTopic();
  }

  @Nullable
  public Runnable enableSearch(String option) {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return myCompilerUIConfigurable.createComponent();
  }

  @Override
  public boolean hasOwnContent() {
    return true;
  }

  @Override
  public boolean isModified() {
    return myCompilerUIConfigurable.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myCompilerUIConfigurable.apply();
  }

  @Override
  public void reset() {
    myCompilerUIConfigurable.reset();
  }

  @Override
  public void disposeUIResources() {
    myCompilerUIConfigurable.disposeUIResources();
  }

  protected Configurable[] buildConfigurables() {
    List<Configurable> result = new ArrayList<Configurable>();

    CompilerConfigurationImpl compilerConfiguration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true);

    final ExcludedEntriesConfigurable excludes = new ExcludedEntriesConfigurable(myProject, descriptor, compilerConfiguration.getExcludedEntriesConfiguration()) {
      public void apply() {
        super.apply();
        FileStatusManager.getInstance(myProject).fileStatusesChanged(); // refresh exclude from compile status
        //ProjectView.getInstance(myProject).refresh();
      }
    };

    result.add(createExcludesWrapper(excludes));

    ArrayList<Configurable> additional = new ArrayList<Configurable>();

    final CompilerSettingsFactory[] factories = Extensions.getExtensions(CompilerSettingsFactory.EP_NAME, myProject);
    if (factories.length > 0) {
      for (CompilerSettingsFactory factory : factories) {
        additional.add(factory.create(myProject));
      }
      Collections.sort(additional, new Comparator<Configurable>() {
        public int compare(final Configurable o1, final Configurable o2) {
          return Comparing.compare(o1.getDisplayName(), o2.getDisplayName());
        }
      });
    }

    additional.add(0, new RmicConfigurable(RmicSettings.getInstance(myProject)));
    additional.add(0, new JavaCompilersTab(myProject, compilerConfiguration.getRegisteredJavaCompilers(), compilerConfiguration.getDefaultCompiler()));

    result.addAll(additional);

    return result.toArray(new Configurable[result.size()]);

  }

  private Configurable createExcludesWrapper(final ExcludedEntriesConfigurable excludes) {
    return new Configurable(){
        @Nls
        public String getDisplayName() {
          return "Excludes";
        }

        public Icon getIcon() {
          return null;
        }

        public String getHelpTopic() {
          return null;
        }

        public JComponent createComponent() {
          return excludes.createComponent();
        }

        public void apply() throws ConfigurationException {
          excludes.apply();
        }

        public boolean isModified() {
          return excludes.isModified();
        }

        public void reset() {
          excludes.reset();
        }

        public void disposeUIResources() {
          excludes.disposeUIResources();
        }
      };
  }
}