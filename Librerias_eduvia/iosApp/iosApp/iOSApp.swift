import SwiftUI

/// Punto de entrada de iOS: delega la interfaz en el código compartido (`MainViewController()` de Compose Multiplatform, presentado por ContentView).
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}