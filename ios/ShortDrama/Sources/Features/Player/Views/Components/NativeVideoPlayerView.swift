import AVKit
import SwiftUI

struct NativeVideoPlayerView: View {
    let url: URL?
    let playbackRate: Float
    let onProgressChange: (Double) -> Void
    let onPlaybackEnded: () -> Void
    let onPlaybackFailed: (String) -> Void

    @State private var player = AVPlayer()
    @State private var timeObserverToken: Any?
    @State private var playbackEndObserver: NSObjectProtocol?
    @State private var playbackFailureObserver: NSObjectProtocol?

    var body: some View {
        VideoPlayer(player: player)
            .onAppear {
                configurePlayer()
            }
            .onChange(of: url) { _, _ in
                configurePlayer()
            }
            .onChange(of: playbackRate) { _, newValue in
                if player.currentItem != nil {
                    player.rate = newValue
                }
            }
            .onDisappear {
                removePlaybackObservers()
                if let timeObserverToken {
                    player.removeTimeObserver(timeObserverToken)
                    self.timeObserverToken = nil
                }
                player.pause()
            }
    }

    private func configurePlayer() {
        guard let url else {
            player.replaceCurrentItem(with: nil)
            return
        }

        let item = AVPlayerItem(url: url)
        player.replaceCurrentItem(with: item)
        attachObserverIfNeeded()
        attachPlaybackObservers(for: item)
        player.playImmediately(atRate: playbackRate)
    }

    private func attachObserverIfNeeded() {
        guard timeObserverToken == nil else { return }
        timeObserverToken = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 1, preferredTimescale: 600),
            queue: .main
        ) { time in
            onProgressChange(max(time.seconds, 0))
        }
    }

    private func attachPlaybackObservers(for item: AVPlayerItem) {
        removePlaybackObservers()
        playbackEndObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: item,
            queue: .main
        ) { _ in
            onPlaybackEnded()
        }
        playbackFailureObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime,
            object: item,
            queue: .main
        ) { notification in
            let error = notification.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? Error
            onPlaybackFailed(error?.localizedDescription ?? "播放失败")
        }
    }

    private func removePlaybackObservers() {
        if let playbackEndObserver {
            NotificationCenter.default.removeObserver(playbackEndObserver)
            self.playbackEndObserver = nil
        }
        if let playbackFailureObserver {
            NotificationCenter.default.removeObserver(playbackFailureObserver)
            self.playbackFailureObserver = nil
        }
    }
}
