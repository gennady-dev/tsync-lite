package lite.telestorage.services

abstract class BackgroundJobManager {
  abstract fun scheduleContentObserverJob()
  abstract fun schedulePeriodicJob()
}