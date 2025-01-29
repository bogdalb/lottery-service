package jobs

import com.typesafe.scalalogging.LazyLogging
import models.dto.ErrorResponse
import models.{Lottery, LotteryStatus}
import org.quartz._
import org.quartz.impl.StdSchedulerFactory
import services.LotteryService

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}


class LotteryScheduler(lotteryService: LotteryService)(implicit ex: ExecutionContext) {

  def startScheduler(): Unit = {
    val jobDataMap = new JobDataMap()
    jobDataMap.put("lotteryService", lotteryService)
    jobDataMap.put("executionContext", ex)

    val job = JobBuilder.newJob(classOf[LotteryJob])
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

class LotteryJob extends Job with LazyLogging {

  override def execute(context: JobExecutionContext): Unit = {
    val lotteryService = context.getMergedJobDataMap.get("lotteryService").asInstanceOf[LotteryService]
    implicit val ec = context.getMergedJobDataMap.get("executionContext").asInstanceOf[ExecutionContext]

    val today = LocalDate.now()

    val lotteriesFuture: Future[Either[ErrorResponse, Seq[Lottery]]] = lotteryService.listLotteries(
      Some(LotteryStatus.Active),
      Some(today)
    )

    val processLotteryResult = (lottery: Lottery) =>
      for {
        _ <- lotteryService.updateLotteryStatus(lottery.id, LotteryStatus.Closing).map {
          case Right(_) => ()
          case Left(error) => throw new RuntimeException(s"Error updating status for lottery ${lottery.id}: ${error.error}")
        }
        _ <- lotteryService.calculateLotteryResult(lottery.id).map {
          case Right(_) => ()
          case Left(error) => throw new RuntimeException(s"Error calculating result for lottery ${lottery.id}: ${error.error}")
        }
      } yield ()

    val handleError: Throwable => Unit = (exception: Throwable) =>
      logger.error(s"Error: ${exception.getMessage}")

    lotteriesFuture.onComplete {
      case scala.util.Success(result) =>
        result match {
          case Right(lotteries) =>
            val futures = lotteries.map(processLotteryResult)
            Future.sequence(futures).onComplete {
              case scala.util.Failure(exception) => handleError(exception)
              case _ => ()
            }
          case Left(error) =>
            logger.error(s"Error retrieving lotteries: ${error.error}")
        }
      case scala.util.Failure(exception) =>
        handleError(exception)
    }

  }
}

