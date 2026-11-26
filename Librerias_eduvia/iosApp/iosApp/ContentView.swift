import UIKit
import SwiftUI
import Shared

/// Puente SwiftUI → Compose: muestra el UIViewController generado por el código compartido (`App()`), sin lógica propia.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}