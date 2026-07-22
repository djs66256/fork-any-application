import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "play.tv")
                .font(.system(size: 60))
                .foregroundColor(.accentColor)
            Text("ShortDrama")
                .font(.largeTitle)
                .fontWeight(.bold)
            Text("Version 0.1.0")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
