/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://forgerock.org/license/CDDLv1.0.html
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at http://forgerock.org/license/CDDLv1.0.html
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
*/

package org.forgerock.openidm.quartz.impl;

import java.util.Date;
import java.util.Set;

import junit.framework.TestCase;

import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.Resources;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

/**
 * Test class that contains unit tests for the RepoJobStore class.
 *
 *
 */
public class TestRepoJobstore extends TestCase {

    private RepoJobStore jobStore;
    private SimpleSignaler signaler;

    public void setUp() {
        signaler = new SimpleSignaler();
        jobStore = new RepoJobStore();
        jobStore.setRepositoryService(Resources.newCollection(new MemoryBackend()));
        jobStore.setSchedulerSignaler(signaler);
    }

    public void tearDown() {
        jobStore = null;
    }

    public void testStoreRetrieveRemoveTrigger() throws Exception {
        Trigger trigger = new SimpleTrigger("trigger1", "group1", new Date());
        Trigger retrievedTrigger1 = null;
        Trigger retrievedTrigger2 = null;

        jobStore.storeTrigger(null, trigger, false);
        retrievedTrigger1 = jobStore.retrieveTrigger(null, trigger.getName(), trigger.getGroup());
        jobStore.removeTrigger(null, trigger.getName(), trigger.getGroup());
        retrievedTrigger2 = jobStore.retrieveTrigger(null, trigger.getName(), trigger.getGroup());

        assertEquals(trigger, retrievedTrigger1);
        assertNull(retrievedTrigger2);
    }

    public void testStoreRetrieveRemoveJob() throws Exception {
        JobDetail job = new JobDetail("job1", "group1", SimpleJob.class);
        JobDetail retrievedJob1 = null;
        JobDetail retrievedJob2 = null;

        jobStore.storeJob(null, job, false);
        retrievedJob1 = jobStore.retrieveJob(null, job.getName(), job.getGroup());
        jobStore.removeJob(null, job.getName(), job.getGroup());
        retrievedJob2 = jobStore.retrieveJob(null, job.getName(), job.getGroup());

        assertEquals(job, retrievedJob1);
        assertNull(retrievedJob2);
    }

    public void testStoreJobAndTrigger() throws Exception {
        Trigger trigger = new SimpleTrigger("trigger1", "group1", new Date());
        JobDetail job = new JobDetail("job1", "group1", SimpleJob.class);
        Trigger retrievedTrigger1 = null;
        JobDetail retrievedJob = null;

        jobStore.storeJobAndTrigger(null, job, trigger);
        retrievedJob = jobStore.retrieveJob(null, job.getName(), job.getGroup());
        retrievedTrigger1 = jobStore.retrieveTrigger(null, trigger.getName(), trigger.getGroup());
        jobStore.removeTrigger(null, trigger.getName(), trigger.getGroup());
        jobStore.removeJob(null, job.getName(), job.getGroup());

        assertEquals(job, retrievedJob);
        assertEquals(trigger, retrievedTrigger1);
        assertNull(jobStore.retrieveTrigger(null, trigger.getName(), trigger.getGroup()));
        assertNull(jobStore.retrieveJob(null, job.getName(), job.getGroup()));
    }

    public void testJobState() throws Exception {
        Trigger trigger = new SimpleTrigger("trigger1", "group1", new Date());
        JobDetail job = new JobDetail("job1", "group1", SimpleJob.class);

        jobStore.storeJobAndTrigger(null, job, trigger);

        assertEquals(0, jobStore.getPausedTriggerGroups(null).size());

        jobStore.pauseTriggerGroup(null, "group1");
        Set<String> paused = jobStore.getPausedTriggerGroups(null);
        assertEquals(1, paused.size());

        String pausedGroup = paused.iterator().next();
        assertEquals("group1", pausedGroup);

        jobStore.resumeAll(null);
        assertEquals(0, jobStore.getPausedTriggerGroups(null).size());
    }

    /*public void testAcquireNextTrigger() throws Exception {
        long currentTime = System.currentTimeMillis();
        Date start1 = new Date(currentTime + 10000);
        Date start2 = new Date(currentTime + 20000);
        Date start3 = new Date(currentTime - 3600000);
        Date end1 = new Date(currentTime + 11000);
        Date end2 = new Date(currentTime + 21000);
        Date end3 = new Date(currentTime - 3500000);

        JobDetail job = new JobDetail("job1", "group1", SimpleJob.class);
        Trigger trigger1 = new SimpleTrigger("trigger1", "group1", job.getName(), job.getGroup(), start1, end1, 2, 2000);
        Trigger trigger2 = new SimpleTrigger("trigger2", "group1", job.getName(), job.getGroup(), start2, end2, 2, 2000);
        Trigger trigger3 = new SimpleTrigger("trigger3", "group1", job.getName(), job.getGroup(), start3, end3, 2, 2000);

        trigger1.computeFirstFireTime(null);
        trigger2.computeFirstFireTime(null);
        trigger3.computeFirstFireTime(null);

        jobStore.storeTrigger(null, trigger1, false);
        jobStore.storeTrigger(null, trigger2, false);
        jobStore.storeTrigger(null, trigger3, false);

        assertEquals(trigger1, jobStore.acquireNextTrigger(null, currentTime + 2000));
        assertEquals(trigger2, jobStore.acquireNextTrigger(null, currentTime + 2000));
        assertEquals(trigger3, jobStore.acquireNextTrigger(null, currentTime + 2000));
        assertNull(jobStore.acquireNextTrigger(null, currentTime + 3600000));

        assertEquals(1, signaler.getMisfiredCount());

        jobStore.releaseAcquiredTrigger(null, trigger2);
        jobStore.releaseAcquiredTrigger(null, trigger1);
        assertEquals(trigger1, jobStore.acquireNextTrigger(null, currentTime + 3600000));
        assertEquals(trigger2, jobStore.acquireNextTrigger(null, currentTime + 3600000));
        assertNull(jobStore.acquireNextTrigger(null, currentTime + 3600000));

        jobStore.removeTrigger(null, trigger1.getName(), trigger1.getGroup());
        jobStore.removeTrigger(null, trigger2.getName(), trigger2.getGroup());
        jobStore.removeTrigger(null, trigger3.getName(), trigger3.getGroup());

        signaler.clear();
    }*/


}
