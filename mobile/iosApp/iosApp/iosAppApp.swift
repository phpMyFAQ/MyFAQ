import SwiftUI
import Shared

@main
struct MyFaqApp: App {
    init() {
        SharedModuleKt.doInitKoin(appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
