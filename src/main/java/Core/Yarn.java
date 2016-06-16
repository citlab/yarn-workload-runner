package Core;

import Core.modules.Freamon;
import org.apache.log4j.Logger;
import util.Config;
import util.Schedule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class Yarn {
    private Schedule schedule;
    private PrintStream summaryLog;
    final static Logger LOG = Logger.getLogger(Yarn.class);

    public Yarn(Schedule schedule, PrintStream summaryLog) {
        this.schedule = schedule;
        this.summaryLog = summaryLog;
    }

    public void initiateJobExecution() {

        Thread[] jobThreads = new Thread[schedule.size()];
        for (int i = 0; i < schedule.size(); i++) {
            JobSequence jobSequence = schedule.get(i);
            Thread jobThread = new Thread(new JobRunner(jobSequence));
            jobThreads[i] = jobThread;
            jobThreads[i].start();
        }

        // wait for all threads' run() methods to complete before continuing
        try {
            for (Thread thread : jobThreads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class JobRunner implements Runnable {
        private JobSequence jobSequence;
        private String[] envp;
        private Config config;

        public JobRunner(JobSequence jobSequence) {
            this.jobSequence = jobSequence;
            config = Config.getInstance();
            envp = new String[]{"HADOOP_CONF_DIR=" + config.getHadoopConfDir()};
        }

        public void run() {
            for (Job job : jobSequence) {
                executeJob(job);
            }
        }

        private void executeJob(Job job) {
            try {

                LOG.info("Waiting " + job.getDelay() + "seconds to execute " + job.getJobName());
                summaryLog.println("Waiting " + job.getDelay() + "seconds to execute " + job.getJobName());
                Thread.sleep(job.getDelay() * 1000, 0);

                PrintStream logPrintStream = job.getLogPrintStream(Config.getLogDir(job.getExperimentName()));

                LOG.info("Executing " + job + '+' + job.getDelay() + "sec with command: " + job.getCommand());

                Process process = Runtime.getRuntime().exec(job.getCommand(), envp);

                BufferedReader buff = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader buffErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                String line;
                long startTime = 0;
                boolean jobStarted = false;
                while ((line = buff.readLine()) != null) {
                    logPrintStream.println(line);
                    summaryLog.println("Analyzing job output.");
                    if (!jobStarted && job.getCmdBuilder().isSubmittedLine(line)) {
                        String jobID = line.substring(line.indexOf("Submitted application")).replace("Submitted application", "").trim();
                        job.setJobID(jobID);
                        LOG.info("Submitted " + job + '+' + job.getDelay() + "sec as " + jobID);
                        if (config.notifyFreamon()) {
                            Freamon.onSubmit(job.getJobID());
                        }
                    }

                    // once the job is marked as started don't check for the started line anymore
                    if (!jobStarted && job.getCmdBuilder().isStartLine(line)) {
                        startTime = System.currentTimeMillis();
                        if (config.notifyFreamon()) {
                            Freamon.onStart(job.getJobID(), startTime);
                        }
                        jobStarted = true;
                    }

                    if (jobStarted && job.getCmdBuilder().isStopLine(line)) {
                        long endTime = System.currentTimeMillis();
                        long duration = (endTime - startTime);
                        LOG.info("Took " + duration / 1000
                                + " seconds to complete executing " + job + '+' + job.getDelay() + "sec");
                        summaryLog.println("[" + Thread.currentThread().getName() + "]" + job.getJobName() + " - " + duration);
                        if (config.notifyFreamon()) {
                            Freamon.onStop(job.getJobID(), endTime);
                        }
                        jobStarted = false;
                    }
                }


                // TODO: create experiment summary file/output
                logPrintStream.flush();
                logPrintStream.close();

                summaryLog.flush();
                while ((line = buffErr.readLine()) != null) System.out.println("[STDERR] " + line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
