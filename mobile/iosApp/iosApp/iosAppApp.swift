import SwiftUI
import Shared

@main
struct MyFaqApp: App {
    init() {
        IosPlatformModuleKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
