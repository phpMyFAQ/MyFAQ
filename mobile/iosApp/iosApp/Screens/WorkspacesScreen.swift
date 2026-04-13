import SwiftUI
import Shared

/// Observable wrapper around the shared WorkspacesViewModel.
final class WorkspacesStore: ObservableObject {
    let vm: WorkspacesViewModel

    @Published var instances: [Instance] = []
    @Published var addState: AddInstanceState = AddInstanceStateIdle()

    private var instancesJob: Kotlinx_coroutines_coreJob?
    private var addStateJob: Kotlinx_coroutines_coreJob?

    init() {
        vm = WorkspacesViewModel(
            db: KoinHelper.database,
            activeInstanceManager: KoinHelper.activeInstanceManager,
            scope: defaultScope()
        )
        startObserving()
    }

    private func startObserving() {
        instancesJob = FlowCollectorKt.collectFlow(flow: vm.instances) { [weak self] value in
            DispatchQueue.main.async {
                self?.instances = (value as? [Instance]) ?? []
            }
        }
        addStateJob = FlowCollectorKt.collectFlow(flow: vm.addState) { [weak self] value in
            DispatchQueue.main.async {
                if let state = value as? AddInstanceState {
                    self?.addState = state
                }
            }
        }
    }

    deinit {
        instancesJob?.cancel(cause: nil)
        addStateJob?.cancel(cause: nil)
    }
}

struct WorkspacesScreen: View {
    @StateObject private var store = WorkspacesStore()
    @State private var showAddSheet = false
    let onInstanceSelected: () -> Void

    var body: some View {
        Group {
            if store.instances.isEmpty {
                emptyState
            } else {
                instanceList
            }
        }
        .navigationTitle("Workspaces")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showAddSheet = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            AddInstanceSheet(store: store) {
                showAddSheet = false
                onInstanceSelected()
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            Text("No instances yet")
                .font(.title2)
            Text("Add your phpMyFAQ instance to get started.")
                .font(.body)
                .foregroundStyle(.secondary)
            Button("Add Instance") {
                showAddSheet = true
            }
            .buttonStyle(.borderedProminent)
            .padding(.top, 8)
            Spacer()
        }
        .padding()
    }

    private var instanceList: some View {
        List {
            ForEach(store.instances, id: \.id) { instance in
                Button {
                    store.vm.selectInstance(instance: instance)
                    onInstanceSelected()
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(instance.displayName)
                            .font(.headline)
                        Text(instance.baseUrl)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .foregroundStyle(.primary)
            }
            .onDelete { offsets in
                for index in offsets {
                    let instance = store.instances[index]
                    store.vm.deleteInstance(instanceId: instance.id)
                }
            }
        }
    }
}
