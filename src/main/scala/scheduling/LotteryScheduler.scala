package scheduling

import scheduling.jobs.LotteryDrawJob
import org.quartz._
import org.quartz.impl.StdSchedulerFactory
import services.LotteryService

import scala.concurrent.ExecutionContext


class LotteryScheduler(lotteryService: LotteryService)(implicit ec: ExecutionContext) {

  def startScheduler(): Unit = {
    val jobDataMap = new JobDataMap()
    jobDataMap.put("lotteryService", lotteryService)
    jobDataMap.put("executionContext", ec)

    val job = JobBuilder.newJob(classOf[LotteryDrawJob])
      .withIdentity("lotteryJob", "group1")
      .usingJobData(jobDataMap)
      .build()

    val trigger = TriggerBuilder.newTrigger()
      .withIdentity("lotteryTrigger", "group1")
      .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
      .build()

    val scheduler = StdSchedulerFactory.getDefaultScheduler()
    scheduler.start()
    scheduler.scheduleJob(job, trigger)
  }
}
