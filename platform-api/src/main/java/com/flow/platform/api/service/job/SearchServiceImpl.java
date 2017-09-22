/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.service.job;

import com.flow.platform.api.domain.SearchCondition;
import com.flow.platform.api.domain.envs.EnvKey;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.domain.job.Job;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author gyfirim
 */

@Service(value = "searchService")
public class SearchServiceImpl implements SearchService {

    private static List<Condition> conditions = new ArrayList<>(3);

    static {
        conditions.add(new KeywordCondition());
        conditions.add(new BranchCondition());
        conditions.add(new GitCondition());
    }

    @Autowired
    private JobService jobService;

    @Override
    public List<Job> search(SearchCondition searchCondition, List<String> paths) {
        List<Job> jobs = jobService.list(paths, false);
        return match(searchCondition, jobs);
    }

    private List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
        for (Condition condition : conditions) {
            jobs = condition.match(searchCondition, jobs);
        }
        return jobs;
    }

    interface Condition {

        List<Job> match(SearchCondition searchCondition, List<Job> jobs);
    }

    static class KeywordCondition implements Condition {

        @Override
        public List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
            String words = searchCondition.getKeyword();

            if (Strings.isNullOrEmpty(words)) {
                return jobs;
            }

            List<Job> copyJobs = new LinkedList<>();

            for (Job job : jobs) {
                if (job.getNumber().toString().equals(words)) { // compare job number
                    copyJobs.add(job);
                } else if (Strings.isNullOrEmpty(job.getEnv(GitEnvs.FLOW_GIT_BRANCH))) { //compare branch
                    copyJobs.add(job);
                }
            }
            return copyJobs;
        }
    }

    static class BranchCondition implements Condition {

        @Override
        public List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
            String branch = searchCondition.getBranch();
            if (Strings.isNullOrEmpty(branch)) {
                return jobs;
            }

            List<Job> copyJobs = new LinkedList<>();
            for (Job job : jobs) {
                if (job.getEnv(GitEnvs.FLOW_GIT_BRANCH).equals(branch)) {
                    copyJobs.add(job);
                }
            }

            return copyJobs;
        }
    }

    static class GitCondition implements Condition {

        @Override
        public List<Job> match(SearchCondition searchCondition, List<Job> jobs) {
            if (searchCondition.getGitEventType() == null) {
                return jobs;
            }

            List<Job> copyJobs = new LinkedList<>();
            for (Job job : jobs) {
                if (job.getCategory() == searchCondition.getGitEventType()) {
                    copyJobs.add(job);
                }
            }

            return copyJobs;
        }
    }
}
