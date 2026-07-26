package com.djs66256.short_drama.feature.player.player

interface NativePlayerAdapter {
    fun attach(sourceUrl: String)
    fun play()
    fun pause()
    fun seekTo(positionSeconds: Double)
    fun setPlaybackSpeed(speed: Float)
    fun currentPositionSeconds(): Double
    fun release()
}
