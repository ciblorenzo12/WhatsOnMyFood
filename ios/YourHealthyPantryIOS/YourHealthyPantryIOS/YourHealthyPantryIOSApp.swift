import SwiftUI
import AVFoundation
import Charts
import UIKit
import Foundation

private let defaultBackendBaseURL = "https://x7amycb9govesb-8787.proxy.runpod.net/"
private let bitwiseAppToken = "R7qK2mZ9vP4xT0aLN6cY1sD8wF3hJ5bG"
private let recommendedDailyCalories = 2000

@main
struct YourHealthyPantryIOSApp: App {
    @StateObject private var pantry = PantryStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(pantry)
        }
    }
}

struct RootView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem { Label("Home", systemImage: "house") }
            ScanView()
                .tabItem { Label("Scan", systemImage: "barcode.viewfinder") }
            PantryView()
                .tabItem { Label("Pantry", systemImage: "cabinet") }
            IngredientDatabaseView()
                .tabItem { Label("Database", systemImage: "list.bullet.rectangle") }
            ProfileView()
                .tabItem { Label("Profile", systemImage: "person.crop.circle") }
        }
        .tint(.purple)
    }
}

struct HomeView: View {
    @EnvironmentObject private var pantry: PantryStore
    @State private var showScanner = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Your Healthy Pantry")
                            .font(.largeTitle.bold())
                        Text("Scan products, explain health ratings, compare options, and track pantry risk from user, AI, and nutrition data.")
                            .foregroundStyle(.secondary)
                    }
                    .padding(.top, 8)

                    HStack(spacing: 12) {
                        NavigationLink {
                            ScanView()
                        } label: {
                            ActionTile(title: "Scan Barcode", icon: "barcode.viewfinder", color: .purple)
                        }

                        NavigationLink {
                            PantryInsightsView()
                        } label: {
                            ActionTile(title: "Risk Insights", icon: "chart.bar.xaxis", color: .cyan)
                        }
                    }

                    ScoreSummaryGrid(stats: PantryRiskScorer.stats(for: pantry.products))

                    SectionHeader(title: "Recent Pantry")
                    if pantry.products.isEmpty {
                        EmptyStateView(title: "No pantry products yet", message: "Scan or enter a barcode to start building the pantry score.")
                    } else {
                        ForEach(pantry.products.prefix(5)) { product in
                            ProductRow(product: product)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("Healthy Pantry")
        }
    }
}

struct ScanView: View {
    @State private var barcode = ""
    @State private var product: Product?
    @State private var errorMessage: String?
    @State private var isLoading = false
    @State private var scannerEnabled = true

    private let client = APIClient()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if scannerEnabled {
                        BarcodeScannerView { code in
                            barcode = code
                            scannerEnabled = false
                            Task { await lookupBarcode(code) }
                        }
                        .frame(height: 260)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .overlay(alignment: .center) {
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(.green, lineWidth: 3)
                                .frame(width: 230, height: 110)
                        }
                    }

                    HStack(spacing: 10) {
                        TextField("Barcode", text: $barcode)
                            .textInputAutocapitalization(.never)
                            .keyboardType(.numberPad)
                            .textFieldStyle(.roundedBorder)

                        Button {
                            Task { await lookupBarcode(barcode) }
                        } label: {
                            if isLoading {
                                ProgressView()
                            } else {
                                Label("Search", systemImage: "magnifyingglass")
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(barcode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                    }

                    Button {
                        scannerEnabled.toggle()
                    } label: {
                        Label(scannerEnabled ? "Pause Camera" : "Resume Camera", systemImage: scannerEnabled ? "pause.circle" : "camera")
                    }
                    .buttonStyle(.bordered)

                    if let errorMessage {
                        Text(errorMessage)
                            .foregroundStyle(.red)
                            .font(.callout)
                    }

                    if let product {
                        ProductDetailView(product: product)
                    }
                }
                .padding()
            }
            .navigationTitle("Scan Product")
        }
    }

    @MainActor
    private func lookupBarcode(_ code: String) async {
        let clean = code.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        isLoading = true
        errorMessage = nil
        do {
            product = try await client.fetchProduct(barcode: clean)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}

struct ProductDetailView: View {
    @EnvironmentObject private var pantry: PantryStore
    @State private var product: Product
    @State private var aiText: String?
    @State private var aiError: String?
    @State private var aiLoading = false
    @State private var showMarketplace = false

    private let client = APIClient()

    init(product: Product) {
        _product = State(initialValue: product)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 14) {
                AsyncImage(url: product.imageURL) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFit()
                    case .failure:
                        Image(systemName: "photo").font(.largeTitle)
                    default:
                        ProgressView()
                    }
                }
                .frame(width: 110, height: 130)
                .background(.thinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))

                VStack(alignment: .leading, spacing: 8) {
                    Text(product.displayName)
                        .font(.title3.bold())
                    Text(product.brands ?? "Unknown brand")
                        .foregroundStyle(.secondary)
                    Text(product.quantity ?? "")
                        .font(.callout)
                        .foregroundStyle(.secondary)
                    CertificationBadgesView(labels: product.allLabels)
                }
            }

            let analysis = ProductAnalyzer.analyze(product)
            Text(analysis.verdict)
                .font(.title2.bold())
                .foregroundStyle(analysis.score >= 70 ? .green : .red)
                .frame(maxWidth: .infinity)

            ScoreChips(product: product)

            BitwiseCard(product: product, aiText: aiText, error: aiError, loading: aiLoading) {
                Task { await loadBitwiseExplanation() }
            }

            IngredientClassificationView(analysis: analysis)

            NutritionFactsView(nutriments: product.nutriments)

            HStack {
                Button {
                    pantry.upsert(product.withScore(analysis.score))
                } label: {
                    Label(pantry.contains(product) ? "Update Pantry" : "Add to Pantry", systemImage: "plus.circle.fill")
                }
                .buttonStyle(.borderedProminent)

                Button {
                    showMarketplace.toggle()
                } label: {
                    Label("Marketplace", systemImage: "cart")
                }
                .buttonStyle(.bordered)
            }

            if showMarketplace {
                MarketplaceView(product: product)
            }
        }
        .padding()
        .background(.background)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(.purple.opacity(0.2)))
        .onAppear {
            product = product.withScore(ProductAnalyzer.analyze(product).score)
        }
    }

    @MainActor
    private func loadBitwiseExplanation() async {
        aiLoading = true
        aiError = nil
        do {
            aiText = try await client.askBitwise(product: product)
        } catch {
            aiError = error.localizedDescription
        }
        aiLoading = false
    }
}

struct PantryView: View {
    @EnvironmentObject private var pantry: PantryStore

    var body: some View {
        NavigationStack {
            List {
                if pantry.products.isEmpty {
                    EmptyStateView(title: "Pantry is empty", message: "Scanned products will appear here with their health and risk scores.")
                } else {
                    Section {
                        NavigationLink {
                            PantryInsightsView()
                        } label: {
                            Label("Open Pantry Risk Insights", systemImage: "chart.bar.xaxis")
                        }
                    }

                    ForEach(pantry.products) { product in
                        PantryProductCell(product: product) { updatedRisk in
                            pantry.updateUserRisk(product, risk: updatedRisk)
                        }
                    }
                    .onDelete { offsets in
                        pantry.delete(at: offsets)
                    }
                }
            }
            .navigationTitle("Pantry")
            .toolbar { EditButton() }
        }
    }
}

struct PantryInsightsView: View {
    @EnvironmentObject private var pantry: PantryStore

    private var items: [RiskItem] {
        PantryRiskScorer.score(products: pantry.products)
    }

    private var stats: RiskStats {
        PantryRiskScorer.stats(for: pantry.products)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text("Pantry Risk Insights")
                    .font(.largeTitle.bold())

                ScoreSummaryGrid(stats: stats)

                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader(title: "What This Shows")
                    Text(insightSummary)
                        .font(.callout)
                        .foregroundStyle(.secondary)
                }
                .insightPanel()

                VStack(alignment: .leading, spacing: 10) {
                    SectionHeader(title: "Pantry Health Profile")
                    HealthProfileBar(label: "Health", value: stats.averageHealthScore, color: .green)
                    HealthProfileBar(label: "Risk control", value: max(0, 100 - stats.averageCombinedRisk), color: .cyan)
                    HealthProfileBar(label: "User confidence", value: stats.averageUserRisk == 0 ? 0 : max(0, 100 - stats.averageUserRisk), color: .orange)
                    HealthProfileBar(label: "Calorie budget", value: max(0, 100 - stats.dailyCaloriesPercent), color: .purple)
                    HealthProfileBar(label: "Data coverage", value: dataCoverage, color: .blue)
                }
                .insightPanel()

                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader(title: "Product Risk Graph")
                    Chart(items) { item in
                        BarMark(
                            x: .value("Risk", item.combinedRisk),
                            y: .value("Product", item.product.shortName)
                        )
                        .foregroundStyle(item.combinedRisk >= 70 ? .red : item.combinedRisk >= 40 ? .orange : .green)
                        .annotation(position: .trailing) {
                            Text("\(item.combinedRisk)")
                                .font(.caption.bold())
                        }
                    }
                    .chartXScale(domain: 0...100)
                    .frame(minHeight: max(220, items.count * 52))
                }
                .insightPanel()

                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader(title: "Risk Distribution")
                    Chart(stats.distribution) { entry in
                        BarMark(
                            x: .value("Category", entry.label),
                            y: .value("Count", entry.count)
                        )
                        .foregroundStyle(entry.color)
                    }
                    .frame(height: 220)
                }
                .insightPanel()

                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader(title: "Calorie Contribution")
                    Chart(calorieItems) { item in
                        BarMark(
                            x: .value("Calories", item.product.nutriments?.energy ?? 0),
                            y: .value("Product", item.product.shortName)
                        )
                        .foregroundStyle(.purple)
                        .annotation(position: .trailing) {
                            Text("\(Int((item.product.nutriments?.energy ?? 0).rounded())) kcal")
                                .font(.caption.bold())
                        }
                    }
                    .frame(minHeight: max(180, calorieItems.count * 48))
                    Text("Total pantry calories: \(stats.totalCalories) kcal, \(stats.dailyCaloriesPercent)% of a \(recommendedDailyCalories) kcal daily target.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .insightPanel()

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: "Product Breakdown")
                    ForEach(items) { item in
                        let analysis = ProductAnalyzer.analyze(item.product)
                        VStack(alignment: .leading, spacing: 5) {
                            Text("\(item.product.displayName) - Score \(analysis.score)/100 | Risk \(item.combinedRisk)/100")
                                .font(.headline)
                            LabeledText(label: "Healthy ingredients", value: analysis.healthyIngredients.joined(separator: ", "), color: .green)
                            LabeledText(label: "Concern ingredients", value: analysis.concernIngredients.joined(separator: ", "), color: .red)
                            LabeledText(label: "Classifications", value: analysis.classifications.joined(separator: ", "), color: .purple)
                            if let calories = item.product.nutriments?.energy {
                                Text("Calories: \(Int(calories.rounded())) kcal per 100g")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 8)
                    }
                }
                .insightPanel()
            }
            .padding()
        }
        .navigationTitle("Insights")
    }

    private var calorieItems: [RiskItem] {
        items
            .filter { $0.product.nutriments?.energy != nil }
            .sorted { ($0.product.nutriments?.energy ?? 0) > ($1.product.nutriments?.energy ?? 0) }
    }

    private var insightSummary: String {
        guard let highest = items.first else {
            return "Add products to compare health score, user concern, calories, and combined pantry risk."
        }
        return "\(highest.product.displayName) has the highest combined risk at \(highest.combinedRisk)/100. The pantry currently has \(stats.lowRiskCount) low-risk, \(stats.moderateRiskCount) moderate-risk, and \(stats.highRiskCount) high-risk products."
    }

    private var dataCoverage: Int {
        guard stats.itemCount > 0 else { return 0 }
        let availableSignals = stats.healthScoreCount + stats.calorieCount + stats.userRiskCount
        return min(100, max(0, Int((Double(availableSignals) / Double(stats.itemCount * 3) * 100).rounded())))
    }
}

struct HealthProfileBar: View {
    let label: String
    let value: Int
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack {
                Text(label)
                    .font(.caption.bold())
                Spacer()
                Text("\(value)/100")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.gray.opacity(0.18))
                    Capsule()
                        .fill(color.gradient)
                        .frame(width: proxy.size.width * CGFloat(max(0, min(100, value))) / 100)
                }
            }
            .frame(height: 10)
        }
    }
}

struct IngredientDatabaseView: View {
    @State private var query = ""

    private var filtered: [AdditiveInfo] {
        guard !query.isEmpty else { return additiveDatabase }
        return additiveDatabase.filter {
            $0.code.localizedCaseInsensitiveContains(query)
            || $0.name.localizedCaseInsensitiveContains(query)
            || $0.risk.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        NavigationStack {
            List(filtered) { additive in
                VStack(alignment: .leading, spacing: 5) {
                    HStack {
                        Text(additive.code).font(.headline)
                        Spacer()
                        Text(additive.risk)
                            .font(.caption.bold())
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(additive.riskColor.opacity(0.15))
                            .foregroundStyle(additive.riskColor)
                            .clipShape(Capsule())
                    }
                    Text(additive.name).font(.subheadline)
                    Text(additive.note).font(.caption).foregroundStyle(.secondary)
                }
            }
            .searchable(text: $query, prompt: "Search additives")
            .navigationTitle("Ingredient Database")
        }
    }
}

struct ProfileView: View {
    @AppStorage("backendBaseURL") private var backendBaseURL = defaultBackendBaseURL
    @EnvironmentObject private var pantry: PantryStore

    var body: some View {
        NavigationStack {
            Form {
                Section("Backend") {
                    TextField("Backend URL", text: $backendBaseURL)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.URL)
                    Text("Used for Bitwise AI and marketplace endpoints.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Section("Pantry") {
                    Text("\(pantry.products.count) saved products")
                    Button(role: .destructive) {
                        pantry.clear()
                    } label: {
                        Label("Clear Pantry", systemImage: "trash")
                    }
                }
            }
            .navigationTitle("Profile")
        }
    }
}

struct MarketplaceView: View {
    let product: Product
    @State private var availability: [RetailerAvailability] = []
    @State private var alternatives: [RetailerAlternative] = []
    @State private var loading = false
    @State private var errorMessage: String?

    private let client = APIClient()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "Marketplace Comparison")

            if loading {
                ProgressView("Loading marketplace data")
            } else if let errorMessage {
                Text(errorMessage).foregroundStyle(.red)
            } else {
                if availability.isEmpty && alternatives.isEmpty {
                    Text("No marketplace data returned yet.")
                        .foregroundStyle(.secondary)
                }

                ForEach(availability) { item in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(item.retailer).font(.headline)
                            Text(item.status).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(item.priceDisplay)
                    }
                }

                ForEach(alternatives) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.name).font(.headline)
                        Text(item.reasonText).font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
        }
        .task { await load() }
    }

    @MainActor
    private func load() async {
        loading = true
        errorMessage = nil
        do {
            availability = try await client.fetchAvailability(for: product)
            alternatives = try await client.fetchAlternatives(for: product)
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }
}

struct BarcodeScannerView: UIViewControllerRepresentable {
    let onCode: (String) -> Void

    func makeUIViewController(context: Context) -> ScannerViewController {
        let controller = ScannerViewController()
        controller.onCode = onCode
        return controller
    }

    func updateUIViewController(_ uiViewController: ScannerViewController, context: Context) {}
}

final class ScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onCode: ((String) -> Void)?
    private let session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var lastCode = ""

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        configureSession()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if !session.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { self.session.startRunning() }
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if session.isRunning {
            session.stopRunning()
        }
    }

    private func configureSession() {
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else {
            showCameraUnavailable()
            return
        }
        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else {
            showCameraUnavailable()
            return
        }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = [.ean13, .ean8, .upce, .qr]

        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        view.layer.insertSublayer(layer, at: 0)
        previewLayer = layer
    }

    private func showCameraUnavailable() {
        let label = UILabel()
        label.text = "Camera unavailable. Enter barcode manually."
        label.textColor = .white
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            label.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let code = object.stringValue,
              code != lastCode else { return }
        lastCode = code
        onCode?(code)
    }
}

final class PantryStore: ObservableObject {
    @Published private(set) var products: [Product] = [] {
        didSet { save() }
    }

    private let key = "pantryProducts"

    init() {
        load()
    }

    func contains(_ product: Product) -> Bool {
        products.contains { $0.barcode == product.barcode }
    }

    func upsert(_ product: Product) {
        if let index = products.firstIndex(where: { $0.barcode == product.barcode }) {
            products[index] = product
        } else {
            products.insert(product, at: 0)
        }
    }

    func updateUserRisk(_ product: Product, risk: Int) {
        guard let index = products.firstIndex(where: { $0.barcode == product.barcode }) else { return }
        products[index].userIngredientRiskScore = max(0, min(100, risk))
    }

    func delete(at offsets: IndexSet) {
        products.remove(atOffsets: offsets)
    }

    func clear() {
        products.removeAll()
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([Product].self, from: data) else { return }
        products = decoded
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(products) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

final class APIClient {
    private var backendBaseURL: String {
        UserDefaults.standard.string(forKey: "backendBaseURL") ?? defaultBackendBaseURL
    }

    func fetchProduct(barcode: String) async throws -> Product {
        let fields = [
            "code", "product_name", "brands", "quantity", "image_url", "labels", "labels_en",
            "labels_tags", "packaging", "categories", "serving_size", "nutriscore_grade",
            "nova_group", "ecoscore_grade", "ingredients_text", "nutriments"
        ].joined(separator: ",")
        guard let url = URL(string: "https://world.openfoodfacts.org/api/v2/product/\(barcode).json?fields=\(fields)") else {
            throw AppError.invalidURL
        }
        let (data, response) = try await URLSession.shared.data(from: url)
        try validate(response)
        let decoded = try JSONDecoder().decode(OpenFoodFactsResponse.self, from: data)
        guard decoded.status == 1, let dto = decoded.product else {
            throw AppError.productNotFound
        }
        return dto.toProduct(fallbackBarcode: barcode)
    }

    func askBitwise(product: Product) async throws -> String {
        let prompt = """
        Explain why this product is healthy or not healthy for a consumer. Use these sections exactly: Why this rating, What to watch, Recommendation, Scientific sources. Be specific to the product, ingredients, Nutri-Score, NOVA group, Eco-Score, and nutrition data. Product: \(product.displayName). Ingredients: \(product.ingredients.joined(separator: ", ")). Nutri-Score: \(product.nutriscoreGrade ?? "unknown"). NOVA: \(product.novaGroup.map(String.init) ?? "unknown"). Calories per 100g: \(product.nutriments?.energy.map { String(Int($0.rounded())) } ?? "unknown").
        """
        let body = ChatRequest(
            model: "gpt-4o-mini",
            messages: [ChatMessage(role: "user", content: prompt)],
            temperature: 0.1,
            topP: 0.9,
            maxTokens: 850,
            responseFormat: ResponseFormat(type: "json_object")
        )

        var request = URLRequest(url: try backendURL(path: "v1/chat/completions"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(bitwiseAppToken, forHTTPHeaderField: "X-APP-TOKEN")
        request.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await URLSession.shared.data(for: request)
        try validate(response)
        let decoded = try JSONDecoder().decode(ChatResponse.self, from: data)
        guard let content = decoded.choices.first?.message.content else {
            throw AppError.emptyAIResponse
        }
        return Self.formatAIContent(content)
    }

    func fetchAvailability(for product: Product) async throws -> [RetailerAvailability] {
        let (data, response) = try await URLSession.shared.data(from: try retailerURL(product: product, endpoint: "availability"))
        try validate(response)
        return (try? JSONDecoder().decode(RetailerResults<RetailerAvailability>.self, from: data).results) ?? []
    }

    func fetchAlternatives(for product: Product) async throws -> [RetailerAlternative] {
        let (data, response) = try await URLSession.shared.data(from: try retailerURL(product: product, endpoint: "alternatives"))
        try validate(response)
        return (try? JSONDecoder().decode(RetailerResults<RetailerAlternative>.self, from: data).results) ?? []
    }

    private func backendURL(path: String) throws -> URL {
        let trimmed = backendBaseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        let base = trimmed.hasSuffix("/") ? trimmed : trimmed + "/"
        guard let url = URL(string: base + path) else { throw AppError.invalidURL }
        return url
    }

    private func retailerURL(product: Product, endpoint: String) throws -> URL {
        let path = "api/retail/products/\(product.barcode)/\(endpoint)"
        var components = URLComponents(url: try backendURL(path: path), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "productName", value: product.displayName),
            URLQueryItem(name: "brand", value: product.brands ?? ""),
            URLQueryItem(name: "category", value: product.categories ?? "")
        ]
        guard let url = components?.url else { throw AppError.invalidURL }
        return url
    }

    private func validate(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse else { return }
        guard (200..<300).contains(http.statusCode) else {
            throw AppError.server("Server returned HTTP \(http.statusCode)")
        }
    }

    private static func formatAIContent(_ content: String) -> String {
        guard let data = content.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return content
        }
        let keys = ["why_this_rating", "why", "watch", "what_to_watch", "recommendation", "sources", "scientific_sources"]
        let parts = keys.compactMap { key -> String? in
            guard let value = json[key] else { return nil }
            return "\(key.replacingOccurrences(of: "_", with: " ").capitalized)\n\(value)"
        }
        return parts.isEmpty ? content : parts.joined(separator: "\n\n")
    }
}

struct Product: Identifiable, Codable, Hashable {
    var id: String { barcode }
    var barcode: String
    var productName: String?
    var brands: String?
    var quantity: String?
    var imageUrl: String?
    var labels: String?
    var labelsTags: [String]
    var packaging: String?
    var categories: String?
    var servingSize: String?
    var nutriscoreGrade: String?
    var novaGroup: Int?
    var ecoscoreGrade: String?
    var ingredients: [String]
    var nutriments: Nutriments?
    var healthScore: Double?
    var userIngredientRiskScore: Int

    var displayName: String {
        let name = productName?.trimmingCharacters(in: .whitespacesAndNewlines)
        return name?.isEmpty == false ? name! : "Unknown Product"
    }

    var shortName: String {
        displayName.count > 24 ? String(displayName.prefix(22)) + "..." : displayName
    }

    var imageURL: URL? {
        guard let imageUrl else { return nil }
        return URL(string: imageUrl)
    }

    var allLabels: String {
        ([labels, packaging, categories] + labelsTags).compactMap { $0 }.joined(separator: ", ")
    }

    func withScore(_ score: Int) -> Product {
        var copy = self
        copy.healthScore = score
        return copy
    }
}

struct Nutriments: Codable, Hashable {
    var energy: Double?
    var fat: Double?
    var saturatedFat: Double?
    var carbohydrates: Double?
    var sugars: Double?
    var addedSugars: Double?
    var fiber: Double?
    var proteins: Double?
    var salt: Double?
    var sodium: Double?

    enum CodingKeys: String, CodingKey {
        case energy = "energy-kcal_100g"
        case fat = "fat_100g"
        case saturatedFat = "saturated-fat_100g"
        case carbohydrates = "carbohydrates_100g"
        case sugars = "sugars_100g"
        case addedSugars = "added-sugars_100g"
        case fiber = "fiber_100g"
        case proteins = "proteins_100g"
        case salt = "salt_100g"
        case sodium = "sodium_100g"
    }
}

struct OpenFoodFactsResponse: Decodable {
    var status: Int
    var product: OpenFoodFactsProduct?
}

struct OpenFoodFactsProduct: Decodable {
    var code: String?
    var productName: String?
    var brands: String?
    var quantity: String?
    var imageUrl: String?
    var labels: String?
    var labelsEn: String?
    var labelsTags: [String]?
    var packaging: String?
    var categories: String?
    var servingSize: String?
    var nutriscoreGrade: String?
    var novaGroup: Int?
    var ecoscoreGrade: String?
    var ingredientsText: String?
    var nutriments: Nutriments?

    enum CodingKeys: String, CodingKey {
        case code
        case productName = "product_name"
        case brands
        case quantity
        case imageUrl = "image_url"
        case labels
        case labelsEn = "labels_en"
        case labelsTags = "labels_tags"
        case packaging
        case categories
        case servingSize = "serving_size"
        case nutriscoreGrade = "nutriscore_grade"
        case novaGroup = "nova_group"
        case ecoscoreGrade = "ecoscore_grade"
        case ingredientsText = "ingredients_text"
        case nutriments
    }

    func toProduct(fallbackBarcode: String) -> Product {
        let labelText = [labels, labelsEn].compactMap { $0 }.joined(separator: ", ")
        let ingredients = (ingredientsText ?? "")
            .components(separatedBy: CharacterSet(charactersIn: ",;"))
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        return Product(
            barcode: code ?? fallbackBarcode,
            productName: productName,
            brands: brands,
            quantity: quantity,
            imageUrl: imageUrl,
            labels: labelText.isEmpty ? nil : labelText,
            labelsTags: labelsTags ?? [],
            packaging: packaging,
            categories: categories,
            servingSize: servingSize,
            nutriscoreGrade: nutriscoreGrade,
            novaGroup: novaGroup,
            ecoscoreGrade: ecoscoreGrade,
            ingredients: ingredients,
            nutriments: nutriments,
            healthScore: nil,
            userIngredientRiskScore: 0
        )
    }
}

struct ChatRequest: Encodable {
    var model: String
    var messages: [ChatMessage]
    var temperature: Double
    var topP: Double
    var maxTokens: Int
    var responseFormat: ResponseFormat

    enum CodingKeys: String, CodingKey {
        case model, messages, temperature
        case topP = "top_p"
        case maxTokens = "max_tokens"
        case responseFormat = "response_format"
    }
}

struct ChatMessage: Codable {
    var role: String
    var content: String
}

struct ResponseFormat: Encodable {
    var type: String
}

struct ChatResponse: Decodable {
    var choices: [ChatChoice]
}

struct ChatChoice: Decodable {
    var message: ChatMessage
}

struct RetailerResults<T: Decodable>: Decodable {
    var results: [T]
}

struct RetailerAvailability: Identifiable, Codable {
    var id: String { "\(retailerName ?? "retailer")-\(priceDisplay)-\(distance ?? "")" }
    var retailerName: String?
    var providerName: String?
    var availabilityStatus: String?
    var price: String?
    var distance: String?
    var fulfillment: String?
    var productUrl: String?
    var note: String?
    var available: Bool?
    var priceValue: Double?
    var distanceValue: Double?

    var retailer: String { retailerName?.isEmpty == false ? retailerName! : "Retailer" }
    var status: String { availabilityStatus?.isEmpty == false ? availabilityStatus! : "Availability unknown" }

    var priceDisplay: String {
        if let price, !price.isEmpty { return price }
        guard let priceValue else { return "Price unavailable" }
        return "$\(String(format: "%.2f", priceValue))"
    }
}

struct RetailerAlternative: Identifiable, Codable {
    var id: String { "\(productName ?? "alternative")-\(reason ?? "")" }
    var productName: String?
    var brand: String?
    var reason: String?
    var healthSignal: String?
    var retailerHint: String?
    var productUrl: String?
    var imageUrl: String?
    var healthScore: Int?
    var priceValue: Double?
    var distanceValue: Double?

    var name: String { productName?.isEmpty == false ? productName! : "Alternative product" }
    var reasonText: String { reason?.isEmpty == false ? reason! : "Suggested as a comparable option." }
}

struct ProductAnalysis {
    var score: Int
    var verdict: String
    var healthyIngredients: [String]
    var concernIngredients: [String]
    var classifications: [String]
}

enum ProductAnalyzer {
    static func analyze(_ product: Product) -> ProductAnalysis {
        var score = 100
        var healthy: [String] = []
        var concerns: [String] = []
        var classifications: [String] = []
        let ingredients = product.ingredients
        let lowered = ingredients.map { $0.lowercased() }

        if let grade = product.nutriscoreGrade?.lowercased() {
            if ["d", "e"].contains(grade) {
                score -= grade == "e" ? 35 : 25
                classifications.append("Low Nutri-Score grade")
            } else if ["a", "b"].contains(grade) {
                score += 4
            }
        }

        if let nova = product.novaGroup {
            if nova >= 4 {
                score -= 25
                classifications.append("Ultra-processed food (NOVA 4)")
            } else if nova == 3 {
                score -= 12
                classifications.append("Processed food (NOVA 3)")
            }
        }

        if let calories = product.nutriments?.energy {
            if calories > 300 {
                score -= 15
                classifications.append("High calorie density")
            } else if calories < 120 {
                score += 3
            }
        }

        if let sugars = product.nutriments?.sugars, sugars >= 22.5 {
            score -= 18
            concerns.append("high sugar content")
        }

        if let addedSugars = product.nutriments?.addedSugars, addedSugars > 0 {
            score -= 18
            concerns.append("added sugar")
        }

        for ingredient in lowered {
            if ingredient.contains("sugar") || ingredient.contains("corn syrup") || ingredient.contains("fructose") {
                concerns.append(displayIngredient(ingredient, original: ingredients))
                score -= 12
            } else if ingredient.contains("aspartame") || ingredient.contains("sucralose") || ingredient.contains("acesulfame") {
                concerns.append(displayIngredient(ingredient, original: ingredients))
                score -= 12
            } else if ingredient.contains("artificial") || ingredient.contains("color") || ingredient.contains("e150") {
                concerns.append(displayIngredient(ingredient, original: ingredients))
                score -= 10
            } else if ingredient.contains("whole") || ingredient.contains("organic") || ingredient.contains("fiber") || ingredient.contains("protein") {
                healthy.append(displayIngredient(ingredient, original: ingredients))
            }
        }

        if healthy.isEmpty {
            healthy.append("None detected")
        }
        if concerns.isEmpty {
            concerns.append("None detected")
        }
        if classifications.isEmpty {
            classifications.append("No major processing classification detected")
        }

        let finalScore = min(100, max(0, score))
        return ProductAnalysis(
            score: finalScore,
            verdict: finalScore >= 70 ? "Healthy" : "Not Healthy",
            healthyIngredients: unique(healthy),
            concernIngredients: unique(concerns),
            classifications: unique(classifications)
        )
    }

    private static func unique(_ values: [String]) -> [String] {
        var seen = Set<String>()
        return values.filter { seen.insert($0.lowercased()).inserted }
    }

    private static func displayIngredient(_ normalized: String, original: [String]) -> String {
        original.first { $0.lowercased() == normalized } ?? normalized
    }
}

struct RiskItem: Identifiable {
    var id: String { product.barcode }
    var product: Product
    var aiRisk: Int?
    var userRisk: Int
    var calorieRisk: Int?
    var combinedRisk: Int
}

struct RiskStats {
    var itemCount: Int
    var averageCombinedRisk: Int
    var averageAiRisk: Int
    var averageUserRisk: Int
    var averageHealthScore: Int
    var averageCalories: Int
    var totalCalories: Int
    var dailyCaloriesPercent: Int
    var healthScoreCount: Int
    var calorieCount: Int
    var userRiskCount: Int
    var lowRiskCount: Int
    var moderateRiskCount: Int
    var highRiskCount: Int
}

struct RiskDistributionEntry: Identifiable {
    var id: String { label }
    var label: String
    var count: Int
    var color: Color
}

enum PantryRiskScorer {
    static func score(products: [Product]) -> [RiskItem] {
        products.map { product in
            let analysis = ProductAnalyzer.analyze(product)
            let healthScore = product.healthScore ?? analysis.score
            let aiRisk = 100 - healthScore
            let userRisk = max(0, min(100, product.userIngredientRiskScore))
            let calorieRisk = calorieRisk(product.nutriments?.energy)
            var total = Double(aiRisk) * 0.55
            var weight = 0.55
            if userRisk > 0 {
                total += Double(userRisk) * 0.25
                weight += 0.25
            }
            if let calorieRisk {
                total += Double(calorieRisk) * 0.20
                weight += 0.20
            }
            return RiskItem(
                product: product,
                aiRisk: aiRisk,
                userRisk: userRisk,
                calorieRisk: calorieRisk,
                combinedRisk: Int((total / weight).rounded())
            )
        }
        .sorted { $0.combinedRisk > $1.combinedRisk }
    }

    static func stats(for products: [Product]) -> RiskStats {
        let items = score(products: products)
        guard !items.isEmpty else {
            return RiskStats(itemCount: 0, averageCombinedRisk: 0, averageAiRisk: 0, averageUserRisk: 0, averageHealthScore: 0, averageCalories: 0, totalCalories: 0, dailyCaloriesPercent: 0, healthScoreCount: 0, calorieCount: 0, userRiskCount: 0, lowRiskCount: 0, moderateRiskCount: 0, highRiskCount: 0)
        }
        let healthScores = products.map { $0.healthScore ?? ProductAnalyzer.analyze($0).score }
        let calories = products.compactMap { $0.nutriments?.energy }
        let userRisks = items.map(\.userRisk).filter { $0 > 0 }
        let totalCalories = Int(calories.reduce(0, +).rounded())
        return RiskStats(
            itemCount: items.count,
            averageCombinedRisk: average(items.map(\.combinedRisk)),
            averageAiRisk: average(items.compactMap(\.aiRisk)),
            averageUserRisk: average(userRisks),
            averageHealthScore: average(healthScores),
            averageCalories: average(calories.map { Int($0.rounded()) }),
            totalCalories: totalCalories,
            dailyCaloriesPercent: Int((Double(totalCalories) / Double(recommendedDailyCalories) * 100).rounded()),
            healthScoreCount: healthScores.count,
            calorieCount: calories.count,
            userRiskCount: userRisks.count,
            lowRiskCount: items.filter { $0.combinedRisk < 40 }.count,
            moderateRiskCount: items.filter { (40..<70).contains($0.combinedRisk) }.count,
            highRiskCount: items.filter { $0.combinedRisk >= 70 }.count
        )
    }

    static func distribution(for products: [Product]) -> [RiskDistributionEntry] {
        let items = score(products: products)
        return [
            RiskDistributionEntry(label: "Low", count: items.filter { $0.combinedRisk < 40 }.count, color: .green),
            RiskDistributionEntry(label: "Moderate", count: items.filter { (40..<70).contains($0.combinedRisk) }.count, color: .orange),
            RiskDistributionEntry(label: "High", count: items.filter { $0.combinedRisk >= 70 }.count, color: .red)
        ]
    }

    private static func calorieRisk(_ calories: Double?) -> Int? {
        guard let calories else { return nil }
        let value = max(0, calories)
        if value <= 120 {
            return clamp(Int((value / 120 * 20).rounded()))
        }
        if value <= 250 {
            return clamp(20 + Int(((value - 120) / 130 * 35).rounded()))
        }
        return clamp(55 + Int((min(45, (value - 250) / 300 * 45)).rounded()))
    }

    private static func average(_ values: [Int]) -> Int {
        guard !values.isEmpty else { return 0 }
        return Int((Double(values.reduce(0, +)) / Double(values.count)).rounded())
    }

    private static func clamp(_ value: Int) -> Int {
        max(0, min(100, value))
    }
}

extension RiskStats {
    var distribution: [RiskDistributionEntry] {
        [
            RiskDistributionEntry(label: "Low", count: lowRiskCount, color: .green),
            RiskDistributionEntry(label: "Moderate", count: moderateRiskCount, color: .orange),
            RiskDistributionEntry(label: "High", count: highRiskCount, color: .red)
        ]
    }
}

enum AppError: LocalizedError {
    case invalidURL
    case productNotFound
    case emptyAIResponse
    case server(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "The URL is invalid."
        case .productNotFound:
            return "Open Food Facts did not return this barcode."
        case .emptyAIResponse:
            return "Bitwise AI returned an empty response."
        case .server(let message):
            return message
        }
    }
}

struct ActionTile: View {
    let title: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: icon)
                .font(.title)
            Text(title)
                .font(.headline)
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, minHeight: 110, alignment: .leading)
        .padding()
        .background(color.gradient)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

struct ScoreSummaryGrid: View {
    let stats: RiskStats

    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            StatCard(value: "\(stats.averageCombinedRisk)", label: "Combined Risk", color: .red)
            StatCard(value: "\(stats.averageHealthScore)", label: "Health", color: .green)
            StatCard(value: stats.averageUserRisk == 0 ? "--" : "\(stats.averageUserRisk)", label: "Users", color: .orange)
            StatCard(value: "\(stats.dailyCaloriesPercent)%", label: "Daily Calories", color: .purple)
        }
    }
}

struct StatCard: View {
    let value: String
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title.bold())
                .foregroundStyle(color)
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, minHeight: 82)
        .background(.background)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(.blue.opacity(0.15)))
    }
}

struct ProductRow: View {
    let product: Product

    var body: some View {
        HStack(spacing: 12) {
            AsyncImage(url: product.imageURL) { image in
                image.resizable().scaledToFit()
            } placeholder: {
                Image(systemName: "shippingbox")
            }
            .frame(width: 44, height: 44)
            .background(.thinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 4) {
                Text(product.displayName)
                    .font(.headline)
                    .lineLimit(1)
                Text("Score \(ProductAnalyzer.analyze(product).score)/100")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.vertical, 6)
    }
}

struct PantryProductCell: View {
    let product: Product
    let onRiskChanged: (Int) -> Void
    @State private var risk: Double

    init(product: Product, onRiskChanged: @escaping (Int) -> Void) {
        self.product = product
        self.onRiskChanged = onRiskChanged
        _risk = State(initialValue: Double(product.userIngredientRiskScore))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ProductRow(product: product)
            HStack {
                Text("User concern")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Slider(value: $risk, in: 0...100, step: 1)
                    .onChange(of: risk) { _, newValue in
                        onRiskChanged(Int(newValue))
                    }
                Text("\(Int(risk))")
                    .font(.caption.monospacedDigit())
                    .frame(width: 32)
            }
        }
    }
}

struct BitwiseCard: View {
    let product: Product
    let aiText: String?
    let error: String?
    let loading: Bool
    let onLoad: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "sparkles")
                    .font(.title2)
                    .foregroundStyle(.purple)
                Text("Bitwise AI")
                    .font(.headline)
                    .foregroundStyle(.purple)
                Spacer()
                Button("Explain") { onLoad() }
                    .buttonStyle(.bordered)
                    .disabled(loading)
            }

            if loading {
                ProgressView("Generating explanation")
            } else if let error {
                Text("Bitwise AI is unavailable. \(error)")
                    .foregroundStyle(.red)
            } else if let aiText {
                Text(aiText)
                    .font(.callout)
            } else {
                Text(defaultExplanation(for: product))
                    .font(.callout)
            }
        }
        .padding()
        .background(Color.purple.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func defaultExplanation(for product: Product) -> String {
        let analysis = ProductAnalyzer.analyze(product)
        return """
        Why this rating
        \(product.displayName) is rated \(analysis.verdict.lowercased()) because the app found: \(analysis.concernIngredients.joined(separator: ", ")).

        What to watch
        \(analysis.classifications.joined(separator: ", ")).

        Recommendation
        Compare the nutrition panel, ingredients, and pantry score before making it an everyday item.
        """
    }
}

struct IngredientClassificationView: View {
    let analysis: ProductAnalysis

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Ingredients")
            LabeledText(label: "Healthy ingredients", value: analysis.healthyIngredients.joined(separator: ", "), color: .green)
            LabeledText(label: "Concern ingredients", value: analysis.concernIngredients.joined(separator: ", "), color: .red)
            LabeledText(label: "Classifications", value: analysis.classifications.joined(separator: ", "), color: .purple)
        }
    }
}

struct NutritionFactsView: View {
    let nutriments: Nutriments?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(title: "Nutrition Facts")
            NutritionRow(name: "Calories", value: nutriments?.energy, unit: "kcal")
            NutritionRow(name: "Fat", value: nutriments?.fat, unit: "g")
            NutritionRow(name: "Saturated Fat", value: nutriments?.saturatedFat, unit: "g")
            NutritionRow(name: "Carbohydrates", value: nutriments?.carbohydrates, unit: "g")
            NutritionRow(name: "Sugars", value: nutriments?.sugars, unit: "g")
            NutritionRow(name: "Fiber", value: nutriments?.fiber, unit: "g")
            NutritionRow(name: "Proteins", value: nutriments?.proteins, unit: "g")
            NutritionRow(name: "Salt", value: nutriments?.salt, unit: "g")
        }
    }
}

struct NutritionRow: View {
    let name: String
    let value: Double?
    let unit: String

    var body: some View {
        HStack {
            Text(name)
            Spacer()
            Text(value.map { "\(String(format: "%.1f", $0)) \(unit)" } ?? "--")
                .foregroundStyle(.secondary)
        }
        .font(.callout)
    }
}

struct ScoreChips: View {
    let product: Product

    var body: some View {
        HStack {
            Chip(text: "Nutri-Score: \(product.nutriscoreGrade?.uppercased() ?? "--")", color: nutriColor)
            Chip(text: "NOVA: \(product.novaGroup.map(String.init) ?? "--")", color: novaColor)
            Chip(text: "Eco: \(product.ecoscoreGrade?.uppercased() ?? "--")", color: .green)
        }
    }

    private var nutriColor: Color {
        switch product.nutriscoreGrade?.lowercased() {
        case "a", "b": return .green
        case "c": return .orange
        case "d", "e": return .red
        default: return .gray
        }
    }

    private var novaColor: Color {
        guard let nova = product.novaGroup else { return .gray }
        return nova >= 4 ? .red : nova == 3 ? .orange : .green
    }
}

struct Chip: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption.bold())
            .foregroundStyle(.white)
            .lineLimit(1)
            .minimumScaleFactor(0.75)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(color)
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

struct CertificationBadgesView: View {
    let labels: String

    private var badges: [CertificationBadge] {
        let lower = labels.lowercased()
        var result: [CertificationBadge] = []
        if lower.contains("organic") { result.append(.init(name: "USDA Organic", asset: "cert_usda_organic")) }
        if lower.contains("non-gmo") || lower.contains("non gmo") { result.append(.init(name: "Non-GMO Project", asset: "cert_non_gmo_project_verified")) }
        if lower.contains("green dot") { result.append(.init(name: "Green Dot", asset: "cert_green_dot")) }
        if lower.contains("triman") { result.append(.init(name: "Triman", asset: "cert_triman")) }
        if lower.contains("gluten-free") || lower.contains("gluten free") { result.append(.init(name: "Gluten Free", asset: "cert_gluten_free")) }
        if lower.contains("fair trade") { result.append(.init(name: "Fair Trade", asset: "cert_fair_trade")) }
        if lower.contains("halal") { result.append(.init(name: "Halal", asset: "cert_halal")) }
        if lower.contains("vegan") { result.append(.init(name: "Vegan", asset: "cert_vegan")) }
        if lower.contains("rainforest") { result.append(.init(name: "Rainforest Alliance", asset: "cert_rainforest")) }
        if lower.contains("b corp") || lower.contains("b-corp") { result.append(.init(name: "B Corp", asset: "cert_b_corp")) }
        if lower.contains("whole grain") { result.append(.init(name: "Whole Grain", asset: "cert_whole_grain")) }
        if lower.contains("animal welfare") { result.append(.init(name: "Animal Welfare", asset: "cert_animal_welfare")) }
        if lower.contains("regenerative") { result.append(.init(name: "Regenerative", asset: "cert_regenerative")) }
        return result
    }

    var body: some View {
        if !badges.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(badges) { badge in
                        Image(badge.asset)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                            .accessibilityLabel(badge.name)
                    }
                }
            }
        }
    }
}

struct CertificationBadge: Identifiable, Hashable {
    var id: String { asset }
    var name: String
    var asset: String
}

struct FlowLayout<Data: RandomAccessCollection, Content: View>: View where Data.Element: Hashable {
    let items: Data
    let content: (Data.Element) -> Content

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 92), spacing: 6)], alignment: .leading, spacing: 6) {
            ForEach(Array(items), id: \.self) { item in
                content(item)
            }
        }
    }
}

struct SectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.headline)
    }
}

struct LabeledText: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 4) {
            Text("\(label):")
                .foregroundStyle(color)
                .font(.callout.bold())
            Text(value.isEmpty ? "None detected" : value)
                .font(.callout)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct EmptyStateView: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "tray")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(title)
                .font(.headline)
            Text(message)
                .font(.callout)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding()
    }
}

extension View {
    func insightPanel() -> some View {
        self
            .padding()
            .background(.background)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(.blue.opacity(0.15)))
    }
}

struct AdditiveInfo: Identifiable {
    var id: String { code }
    var code: String
    var name: String
    var risk: String
    var note: String

    var riskColor: Color {
        switch risk {
        case "High": return .red
        case "Moderate": return .orange
        default: return .green
        }
    }
}

let additiveDatabase = [
    AdditiveInfo(code: "E150d", name: "Sulphite ammonia caramel", risk: "Moderate", note: "Color additive commonly found in dark sodas."),
    AdditiveInfo(code: "E330", name: "Citric acid", risk: "Low", note: "Common acidity regulator."),
    AdditiveInfo(code: "Aspartame", name: "Artificial sweetener", risk: "Moderate", note: "Sweetener to watch for in diet drinks."),
    AdditiveInfo(code: "Potassium benzoate", name: "Preservative", risk: "Moderate", note: "Preservative used in acidic beverages."),
    AdditiveInfo(code: "Added sugar", name: "Sugar added during processing", risk: "High", note: "Raises pantry risk and lowers health score."),
    AdditiveInfo(code: "Artificial flavor", name: "Synthetic flavoring", risk: "Moderate", note: "Flagged as a concern ingredient in product explanations.")
]
