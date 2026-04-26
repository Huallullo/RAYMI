package com.raymi.app.domain.usecase.notifications

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.raymi.app.core.workers.CheckOverdueRentalsWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Caso de uso para programar verificaciones periódicas de alquileres vencidos
 */
class ScheduleOverdueCheckUseCase @Inject constructor(
    private val workManager: WorkManager
) {
    operator fun invoke() {
        val workRequest = PeriodicWorkRequestBuilder<CheckOverdueRentalsWorker>(
            repeatInterval = 1, // Cada día
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "check_overdue_rentals",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
