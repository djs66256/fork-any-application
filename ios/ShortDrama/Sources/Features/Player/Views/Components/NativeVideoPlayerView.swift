import AVKit
import SwiftUI

struct NativeVideoPlayerView: View {
    let url: URL?
    let playbackRate: Float
    let onProgressChange: (Double) -> Void

    @State private var player = AVPlayer()
    @State private var timeObserverToken: Any?

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
}
