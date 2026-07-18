package com.cycling.domain.model

data class ScanProgress(
    val isScanning: Boolean = false,
    val currentStep: ScanStep = ScanStep.IDLE,
    val songsProcessed: Int = 0,
    val totalSongs: Int = 0
)

enum class ScanStep {
    IDLE,
    QUERYING_MEDIASTORE,
    SAVING_TO_DATABASE,
    COMPLETED,
    ERROR
}
