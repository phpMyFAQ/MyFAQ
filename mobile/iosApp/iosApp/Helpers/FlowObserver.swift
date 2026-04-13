import SwiftUI
import Shared
import Combine

/// Wraps a KMP `StateFlow` so SwiftUI views can observe it via `@StateObject`.
/// Usage: `@StateObject var items = FlowObserver(flow: vm.items, initial: [])`
final class FlowObserver<T>: ObservableObject {
    @Published var value: T

    private var collector: Kotlinx_coroutines_coreJob?

    init(flow: Kotlinx_coroutines_coreStateFlow, initial: T) {
        self.value = (flow.value as? T) ?? initial
        self.collector = FlowCollectorKt.collectFlow(flow: flow, onEach: { [weak self] newValue in
            DispatchQueue.main.async {
                if let v = newValue as? T {
                    self?.value = v
                }
            }
        })
    }

    deinit {
        collector?.cancel(cause: nil)
    }
}
