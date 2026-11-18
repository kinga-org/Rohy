// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "Rohy",
    platforms: [.macOS(.v14)],
    products: [
        .library(name: "RohyCore", targets: ["RohyCore"]),
        .executable(name: "Rohy", targets: ["Rohy"]),
        .executable(name: "rohy-harness", targets: ["rohy-harness"]),
    ],
    targets: [
        .target(name: "RohyCore", swiftSettings: [.swiftLanguageMode(.v5)]),
        .target(name: "RohyServer", dependencies: ["RohyCore"], swiftSettings: [.swiftLanguageMode(.v5)]),
        .executableTarget(name: "Rohy", dependencies: ["RohyCore", "RohyServer"], swiftSettings: [.swiftLanguageMode(.v5)]),
        .executableTarget(name: "rohy-harness", dependencies: ["RohyCore", "RohyServer"], swiftSettings: [.swiftLanguageMode(.v5)]),
        .testTarget(name: "RohyCoreTests", dependencies: ["RohyCore"], swiftSettings: [.swiftLanguageMode(.v5)]),
    ]
)
