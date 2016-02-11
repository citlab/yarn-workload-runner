package Core;

import util.JobList;
import util.JobTime;
import util.Schedule;

import java.io.IOException;

/**
 * Created by Johannes on 01/02/16.
 */
public class Yarn {
    private Schedule schedule;
    private JobList jobs;

    // TODO: abstract, flink runner, yarn runner, etc.
    public Yarn(Schedule schedule, JobList jobs) {
        this.schedule = schedule;
        this.jobs = jobs;
    }

    public void runJobs() {
        for (JobTime jobTime: schedule) {
                Job job = jobs.getJobWithName(jobTime.getJobName());
                (new Thread(new JobRunner(job, jobTime))).start();
        }
    }

    private class JobRunner implements Runnable {

        private Job job;
        private JobTime jobTime;

        public JobRunner(Job job, JobTime jobTime) {
            this.job = job;
            this.jobTime = jobTime;
        }

        public void run() {
            try {
                System.out.println("Waiting " + jobTime.getDelay() + " to execute " + jobTime.getJobName());
                Thread.sleep(jobTime.getDelay());
                System.out.println("Executing " + job);
                Runtime.getRuntime().exec(job.getCommand());
//                Runtime.getRuntime().exec("mkdir /Users/Johannes/arbeit/yarn-timed-workload-generator/madeIt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
