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
    @State private var instanceToDelete: Instance?
    @State private var instanceToRename: Instance?
    @State private var renameText = ""
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
        .alert(
            "Delete Instance",
            isPresented: Binding(
                get: { instanceToDelete != nil },
                set: { if !$0 { instanceToDelete = nil } }
            ),
            presenting: instanceToDelete
        ) { instance in
            Button("Delete", role: .destructive) {
                store.vm.deleteInstance(instanceId: instance.id)
                instanceToDelete = nil
            }
            Button("Cancel", role: .cancel) {
                instanceToDelete = nil
            }
        } message: { instance in
            Text("Are you sure you want to delete \"\(instance.displayName)\"? This cannot be undone.")
        }
        .alert(
            "Rename Instance",
            isPresented: Binding(
                get: { instanceToRename != nil },
                set: { if !$0 { instanceToRename = nil } }
            ),
            presenting: instanceToRename
        ) { instance in
            TextField("Name", text: $renameText)
            Button("Save") {
                let trimmed = renameText.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty {
                    store.vm.renameInstance(instanceId: instance.id, newName: trimmed)
                }
                instanceToRename = nil
            }
            Button("Cancel", role: .cancel) {
                instanceToRename = nil
            }
        } message: { _ in
            Text("Enter a new display name.")
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            if let uiImage = UIImage(named: "Logo") {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFit()
                    .frame(height: 120)
                    .padding(.bottom, 8)
            }
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
                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                    Button(role: .destructive) {
                        instanceToDelete = instance
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
                .contextMenu {
                    Button {
                        renameText = instance.displayName
                        instanceToRename = instance
                    } label: {
                        Label("Rename", systemImage: "pencil")
                    }
                    Button(role: .destructive) {
                        instanceToDelete = instance
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
    }
}
