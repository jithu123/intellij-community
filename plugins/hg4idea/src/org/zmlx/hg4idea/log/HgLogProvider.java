/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package org.zmlx.hg4idea.log;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.VcsLogTextFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zmlx.hg4idea.HgNameWithHashInfo;
import org.zmlx.hg4idea.HgUpdater;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.repo.HgConfig;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.repo.HgRepositoryManager;
import org.zmlx.hg4idea.util.HgHistoryUtil;
import org.zmlx.hg4idea.util.HgUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Nadya Zabrodina
 */
public class HgLogProvider implements VcsLogProvider {

  private static final Logger LOG = Logger.getInstance(HgLogProvider.class);

  @NotNull private final Project myProject;
  @NotNull private final HgRepositoryManager myRepositoryManager;
  @NotNull private final VcsLogRefManager myRefSorter;
  @NotNull private final VcsLogObjectsFactory myVcsObjectsFactory;

  public HgLogProvider(@NotNull Project project, @NotNull HgRepositoryManager repositoryManager) {
    myProject = project;
    myRepositoryManager = repositoryManager;
    myRefSorter = new HgRefManager();
    myVcsObjectsFactory = ServiceManager.getService(project, VcsLogObjectsFactory.class);
  }

  @NotNull
  @Override
  public List<? extends VcsFullCommitDetails> readFirstBlock(@NotNull VirtualFile root,
                                                             boolean ordered, int commitCount) throws VcsException {
    String[] params = ordered ? ArrayUtil.EMPTY_STRING_ARRAY : new String[]{"-r", "0:tip"};
    return HgHistoryUtil.history(myProject, root, commitCount, params);
  }

  @NotNull
  @Override
  public List<TimedVcsCommit> readAllHashes(@NotNull VirtualFile root, @NotNull Consumer<VcsUser> userRegistry) throws VcsException {
    return HgHistoryUtil.readAllHashes(myProject, root, userRegistry);
  }

  @NotNull
  @Override
  public List<? extends VcsShortCommitDetails> readShortDetails(@NotNull VirtualFile root, @NotNull List<String> hashes)
    throws VcsException {
    return HgHistoryUtil.readMiniDetails(myProject, root, hashes);
  }

  @NotNull
  @Override
  public List<? extends VcsFullCommitDetails> readFullDetails(@NotNull VirtualFile root, @NotNull List<String> hashes) throws VcsException {
    return HgHistoryUtil.history(myProject, root, -1, HgHistoryUtil.prepareHashes(hashes));
  }

  @NotNull
  @Override
  public Collection<VcsRef> readAllRefs(@NotNull VirtualFile root) throws VcsException {
    myRepositoryManager.waitUntilInitialized();
    HgRepository repository = myRepositoryManager.getRepositoryForRoot(root);
    if (repository == null) {
      LOG.error("Repository not found for root " + root);
      return Collections.emptyList();
    }

    repository.update();
    Collection<HgNameWithHashInfo> branches = repository.getBranches();
    Collection<HgNameWithHashInfo> bookmarks = repository.getBookmarks();
    Collection<HgNameWithHashInfo> tags = repository.getTags();
    Collection<HgNameWithHashInfo> localTags = repository.getLocalTags();

    Collection<VcsRef> refs = new ArrayList<VcsRef>(branches.size() + bookmarks.size());

    for (HgNameWithHashInfo branchInfo : branches) {
      refs.add(myVcsObjectsFactory.createRef(myVcsObjectsFactory.createHash(branchInfo.getHash()), branchInfo.getName(), HgRefManager.BRANCH, root));
    }
    for (HgNameWithHashInfo bookmarkInfo : bookmarks) {
      refs.add(myVcsObjectsFactory.createRef(myVcsObjectsFactory.createHash(bookmarkInfo.getHash()), bookmarkInfo.getName(),
                         HgRefManager.BOOKMARK, root));
    }
    String currentRevision = repository.getCurrentRevision();
    if (currentRevision != null) { // null => fresh repository
      refs.add(myVcsObjectsFactory.createRef(myVcsObjectsFactory.createHash(currentRevision), "tip", HgRefManager.HEAD, root));
    }
    for (HgNameWithHashInfo tagInfo : tags) {
      refs.add(myVcsObjectsFactory.createRef(myVcsObjectsFactory.createHash(tagInfo.getHash()), tagInfo.getName(), HgRefManager.TAG, root));
    }
    for (HgNameWithHashInfo localTagInfo : localTags) {
      refs.add(myVcsObjectsFactory.createRef(myVcsObjectsFactory.createHash(localTagInfo.getHash()), localTagInfo.getName(),
                              HgRefManager.LOCAL_TAG, root));
    }
    return refs;
  }

  @NotNull
  @Override
  public VcsKey getSupportedVcs() {
    return HgVcs.getKey();
  }

  @NotNull
  @Override
  public VcsLogRefManager getReferenceManager() {
    return myRefSorter;
  }

  @Override
  public void subscribeToRootRefreshEvents(@NotNull final Collection<VirtualFile> roots, @NotNull final VcsLogRefresher refresher) {
    myProject.getMessageBus().connect(myProject).subscribe(HgVcs.STATUS_TOPIC, new HgUpdater() {
      @Override
      public void update(Project project, @Nullable VirtualFile root) {
        if (root != null && roots.contains(root)) {
          refresher.refresh(root);
        }
      }
    });
  }

  @NotNull
  @Override
  public List<? extends VcsFullCommitDetails> getFilteredDetails(@NotNull final VirtualFile root,
                                                                 @NotNull Collection<VcsLogBranchFilter> branchFilters,
                                                                 @NotNull Collection<VcsLogUserFilter> userFilters,
                                                                 @NotNull Collection<VcsLogDateFilter> dateFilters,
                                                                 @NotNull Collection<VcsLogTextFilter> textFilters,
                                                                 @NotNull Collection<VcsLogStructureFilter> structureFilters,
                                                                 int maxCount) throws VcsException {
    List<String> filterParameters = ContainerUtil.newArrayList();

    // branch filter and user filter may be used several times without delimiter
    if (!branchFilters.isEmpty()) {
      HgRepository repository = myRepositoryManager.getRepositoryForRoot(root);
      if (repository == null) {
        LOG.error("Repository not found for root " + root);
        return Collections.emptyList();
      }

      boolean atLeastOneBranchExists = false;
      for (VcsLogBranchFilter branchFilter : branchFilters) {
        String branchName = branchFilter.getBranchName();
        if (branchExists(repository, branchName)) {
          filterParameters.add(prepareParameter("branch", branchName));
          atLeastOneBranchExists = true;
        }
      }
      if (!atLeastOneBranchExists) { // no such branches => filter matches nothing
        return Collections.emptyList();
      }
    }

    if (!userFilters.isEmpty()) {
      for (VcsLogUserFilter authorFilter : userFilters) {
        filterParameters.add(prepareParameter("user", authorFilter.getUserName(root)));
      }
    }

    if (!dateFilters.isEmpty()) {
      StringBuilder args = new StringBuilder();
      final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      filterParameters.add("-r");
      VcsLogDateFilter filter = dateFilters.iterator().next();
      if (filter.getAfter() != null) {
        args.append("date('>").append(dateFormatter.format(filter.getAfter())).append("')");
      }

      if (filter.getBefore() != null) {
        if (args.length() > 0) {
          args.append(" and ");
        }

        args.append("date('<").append(dateFormatter.format(filter.getBefore())).append("')");
      }
      filterParameters.add(args.toString());
    }

    if (textFilters.size() > 1) {
      LOG.warn("Expected only one text filter: " + textFilters);
    }
    else if (!textFilters.isEmpty()) {
      String textFilter = textFilters.iterator().next().getText();
      filterParameters.add(prepareParameter("keyword", textFilter));
    }

    if (!structureFilters.isEmpty()) {
      for (VcsLogStructureFilter filter : structureFilters) {
        for (VirtualFile file : filter.getFiles(root)) {
          filterParameters.add(file.getPath());
        }
      }
    }

    return HgHistoryUtil.history(myProject, root, maxCount, ArrayUtil.toStringArray(filterParameters));
  }

  @Nullable
  @Override
  public VcsUser getCurrentUser(@NotNull VirtualFile root) throws VcsException {
    String userName = HgConfig.getInstance(myProject, root).getNamedConfig("ui", "username");
    if (userName == null) {
      userName = System.getenv("HGUSER");
    }
    List<String> userArgs = HgUtil.parseUserNameAndEmail(userName);
    return userName == null ? null : myVcsObjectsFactory.createUser(userArgs.get(0), userArgs.get(1));
  }

  @NotNull
  @Override
  public Collection<String> getContainingBranches(@NotNull VirtualFile root, @NotNull Hash commitHash) throws VcsException {
    return HgHistoryUtil.getDescendingHeadsOfBranches(myProject, root, commitHash);
  }

  private static String prepareParameter(String paramName, String value) {
    return "--" + paramName + "=" + value; // no value escaping needed, because the parameter itself will be quoted by GeneralCommandLine
  }

  private static boolean branchExists(@NotNull HgRepository repository, @NotNull String branchName) {
    return HgUtil.getNamesWithoutHashes(repository.getBranches()).contains(branchName) ||
           HgUtil.getNamesWithoutHashes(repository.getBookmarks()).contains(branchName);
  }

}
