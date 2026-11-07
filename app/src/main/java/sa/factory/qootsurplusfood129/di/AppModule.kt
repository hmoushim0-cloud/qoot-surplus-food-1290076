package sa.factory.qootsurplusfood129.di

import sa.factory.qootsurplusfood129.data.repository.QootSurplusRepositoryImpl
import sa.factory.qootsurplusfood129.domain.repository.QootSurplusRepository
import sa.factory.qootsurplusfood129.domain.usecase.*
import sa.factory.qootsurplusfood129.hardware.HardwareSensorDriver
import sa.factory.qootsurplusfood129.ui.viewmodel.QootSurplusViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: QootSurplusRepository = QootSurplusRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): QootSurplusViewModel = QootSurplusViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
