package lite.tsync.services

abstract class BackgroundJobManager {
  abstract fun scheduleContentObserverJob()
  abstract fun schedulePeriodicJob()
}