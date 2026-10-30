// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "linphonesw",
    platforms: [
        .macOS(.v10_14)
    ],
    products: [
        .library(
            name: "linphonesw",
            targets: ["linphonesw"]
        )
    ],
    targets: [
        
			.binaryTarget(
				name: "ZXing",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/ZXing.xcframework.zip",
				checksum: "2a0869c0d702af0597e59b0a99fa5e293d791b7f587089380966e6b092bcbbac"
			),
			
			.binaryTarget(
				name: "bctoolbox-tester",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/bctoolbox-tester.xcframework.zip",
				checksum: "cde3b644d71845ae39530c1664455e0dd6cbc7fc5034c04d55dbda5d949a2aab"
			),
			
			.binaryTarget(
				name: "bctoolbox",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/bctoolbox.xcframework.zip",
				checksum: "b7da99a340602fa0998c8df2bb087f2a9463e597b2cb1ab80f005600803fc9a7"
			),
			
			.binaryTarget(
				name: "belcard",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/belcard.xcframework.zip",
				checksum: "74aa91ee4f30a4597385aa74652d694d7c29c39f4caa94be8081cd0024dd1c15"
			),
			
			.binaryTarget(
				name: "belle-sip",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/belle-sip.xcframework.zip",
				checksum: "fbb9039a52c53915ecec7d1664d3de75ee494ad0d017aa6c550c03394c01968e"
			),
			
			.binaryTarget(
				name: "belr",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/belr.xcframework.zip",
				checksum: "d70eedbc3e956890d2129aa3a59a68ab7354a53a29c207cafe033271700c7a72"
			),
			
			.binaryTarget(
				name: "lime",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/lime.xcframework.zip",
				checksum: "9b6357fa5b7de709c3e3f4e03566de9d5c7d8f950593c0057bebbfd3a89826a3"
			),
			
			.binaryTarget(
				name: "linphone",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/linphone.xcframework.zip",
				checksum: "98087a162b01d8da51e8401ae787aa31206d9919f3331c4e2f077030b4c041fb"
			),
			
			.binaryTarget(
				name: "mediastreamer2",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/mediastreamer2.xcframework.zip",
				checksum: "11e763723626c0fb4d6be871fb426a74fd3169bed396be2f21be4101a0578fd0"
			),
			
			.binaryTarget(
				name: "msamr",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/msamr.xcframework.zip",
				checksum: "10d6f2c5366cdabc3d4b1f0a7d893a8eabe988cd23b9df6d50ff8c68e1299c7e"
			),
			
			.binaryTarget(
				name: "mscodec2",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/mscodec2.xcframework.zip",
				checksum: "506d2882b85109839a63b0bbaa26ae2f45ee39ca261590e5552c1b548359992b"
			),
			
			.binaryTarget(
				name: "msopenh264",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/msopenh264.xcframework.zip",
				checksum: "2a0c9cadf6c4e4eef7bbd14a2407be8fbd426b433e317b27de24c10e2cb0ef3b"
			),
			
			.binaryTarget(
				name: "ortp",
				url: "https://download.linphone.org/releases/macosx/sdk/spm//linphone-sdk-swift-macos-5.5.39/XCFrameworks/ortp.xcframework.zip",
				checksum: "b503413d1520cc8f668e700268577a66173da574d1836e76757bc94c3505cac2"
			),
			
		.target(
			name: "linphonexcframeworks",
			dependencies: ["ZXing", "bctoolbox-tester", "bctoolbox", "belcard", "belle-sip", "belr", "lime", "linphone", "mediastreamer2", "msamr", "mscodec2", "msopenh264", "ortp"]
		),

		.target(
			name: "linphonesw",
			dependencies: ["linphonexcframeworks"]
		)
    ]
)

