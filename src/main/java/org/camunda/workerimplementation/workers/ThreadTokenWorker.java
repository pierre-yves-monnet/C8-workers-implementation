package org.camunda.workerimplementation.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.workerimplementation.WorkerConfig;
import org.camunda.workerimplementation.monitor.MonitorWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;

public class ThreadTokenWorker implements JobHandler {

  private final MonitorWorker monitorWorker;
  private final WorkerConfig workerConfig;
  Logger logger = LoggerFactory.getLogger(ThreadTokenWorker.class);
  private final Semaphore semaphore;

  public ThreadTokenWorker(WorkerConfig workerConfig, MonitorWorker monitorWorker) {
    this.workerConfig = workerConfig;
    this.monitorWorker = monitorWorker;
    semaphore = new Semaphore(workerConfig.getNumberOfSemaphores());
  }

  @Override
  public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {
    logger.debug("------------- Worker: ThreadTokenWorker " + Thread.currentThread().getName());
    monitorWorker.startHandle(this);
    try {
      semaphore.acquire();
    } catch (Exception e) {
      return;
    }
    doWorkInDifferentThread(jobClient, activatedJob);
    monitorWorker.stopHandle(this);
  }

  private void doWorkInDifferentThread(JobClient jobClient, ActivatedJob activatedJob) {
    Thread thread = new Thread(() -> {
      WorkToComplete workToComplete = new WorkToComplete();
      workToComplete.executeJob(this, activatedJob, monitorWorker);
      jobClient.newCompleteCommand(activatedJob.getKey()).send().join();
      semaphore.release();

    });
    thread.start();

  }
}
