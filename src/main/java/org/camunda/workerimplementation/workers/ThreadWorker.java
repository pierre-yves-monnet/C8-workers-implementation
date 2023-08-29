package org.camunda.workerimplementation.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.WorkerConfig;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadWorker implements JobHandler {

  private final WorkerConfig workerConfig;
  private final MonitorWorker monitorWorker;
  Logger logger = LoggerFactory.getLogger(ThreadWorker.class);

  public ThreadWorker(WorkerConfig workerConfig, MonitorWorker monitorWorker) {
    this.workerConfig = workerConfig;
    this.monitorWorker = monitorWorker;
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {
    logger.debug("------------- Worker: threadExecutor " + Thread.currentThread().getName());

    monitorWorker.startHandle(this);

    doWorkInDifferentThread(jobClient, activatedJob);

    monitorWorker.stopHandle(this);
  }

  private void doWorkInDifferentThread(JobClient jobClient, ActivatedJob activatedJob) {
    Thread thread = new Thread(() -> {

      WorkToComplete workToComplete = new WorkToComplete();
      workToComplete.executeJob(this, activatedJob, monitorWorker);

      jobClient.newCompleteCommand(activatedJob.getKey()).send().join();
    });
    thread.start();
  }

}