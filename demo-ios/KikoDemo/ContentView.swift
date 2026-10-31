import DemoKit
import SwiftUI
import UniformTypeIdentifiers

/// The demo's Compose screen, plus the system file importer behind its "Open cart" button.
struct ContentView: View {
    @State private var importing = false

    var body: some View {
        ComposeView(onOpen: { importing = true })
            .ignoresSafeArea()
            .fileImporter(isPresented: $importing, allowedContentTypes: [.item]) { result in
                guard case .success(let url) = result else { return }
                let scoped = url.startAccessingSecurityScopedResource()
                defer { if scoped { url.stopAccessingSecurityScopedResource() } }
                if let data = try? Data(contentsOf: url) {
                    MainViewControllerKt.playCart(data: data)
                }
            }
    }
}

/// Hosts the Kotlin side (demo/src/iosMain), which draws the game and its controls.
struct ComposeView: UIViewControllerRepresentable {
    let onOpen: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController(onOpen: onOpen)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
