import SwiftUI

/// Punto de entrada de iOS: delega la interfaz en el código compartido (`App()` de Compose Multiplatform, presentado por ContentView).
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}