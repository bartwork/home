// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "linphonesw",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(
            name: "linphonesw",
            targets: ["linphonesw"]
        )
    ],
    targets: [
        
			.binaryTarget(
				name: "bctoolbox-ios",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/bctoolbox-ios.xcframework.zip",
				checksum: "af34e1ee7e6b2d01866bd5147c1fc82626f1110b950f62a052f801253cfe6430"
			),
			
			.binaryTarget(
				name: "bctoolbox-tester",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/bctoolbox-tester.xcframework.zip",
				checksum: "85900fe8a8080aa5a0ff8c18a2401759678ee54e67e63c432a238f331ddf4b3b"
			),
			
			.binaryTarget(
				name: "bctoolbox",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/bctoolbox.xcframework.zip",
				checksum: "a768186b6d6600671e49cdf399f9e13398b1fc9c114de4ee69eb05602b153688"
			),
			
			.binaryTarget(
				name: "belcard",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/belcard.xcframework.zip",
				checksum: "8c67706d1b1ea773c0aa1f1430fe8075267e4ea21644f2246c2b9829a7f81948"
			),
			
			.binaryTarget(
				name: "belle-sip",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/belle-sip.xcframework.zip",
				checksum: "06f9163d25191343337c57f5e13cf4c0627643ba8e244b47a5f9835beacb5a5b"
			),
			
			.binaryTarget(
				name: "belr",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/belr.xcframework.zip",
				checksum: "8b3d5b5b86f86f1ec6cb87c4f02b0564f0754d874e22a012fc47cb8a8a5224e3"
			),
			
			.binaryTarget(
				name: "lime",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/lime.xcframework.zip",
				checksum: "86b370186bc7819d87690f87bb8ceb163f033c8ff5f0a43cd5e9881b00365732"
			),
			
			.binaryTarget(
				name: "linphone",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/linphone.xcframework.zip",
				checksum: "2bf5832c0a9171cc05941a1108441bd82186d0c62f5436b39535a364718029b2"
			),
			
			.binaryTarget(
				name: "linphonetester",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/linphonetester.xcframework.zip",
				checksum: "b77ea49555418a98fde1934490c34ad9f2f39c26d21eb888bdebb121b36f896b"
			),
			
			.binaryTarget(
				name: "mbedcrypto",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/mbedcrypto.xcframework.zip",
				checksum: "8c6c8f82ba583d6cb08e2623a9fadc3afb61303e37bfdd4c91380875a2f1fa64"
			),
			
			.binaryTarget(
				name: "mbedtls",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/mbedtls.xcframework.zip",
				checksum: "760a63eaad199794c95f31d2b383028be005e653efc642cb57d250f1e5f7c835"
			),
			
			.binaryTarget(
				name: "mbedx509",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/mbedx509.xcframework.zip",
				checksum: "bad11278c3d09170f77e418589cdbb41e4d6f9f16c13b7495e1981f93f5b4103"
			),
			
			.binaryTarget(
				name: "mediastreamer2",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/mediastreamer2.xcframework.zip",
				checksum: "524c925bc1db6e1247bd8746d59e30d040485e142c739bc4031331543f676f2f"
			),
			
			.binaryTarget(
				name: "msamr",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/msamr.xcframework.zip",
				checksum: "a7625db3c7cf58cbf944fb2209cea07166bb1e9d630189a821663308263367cb"
			),
			
			.binaryTarget(
				name: "mscodec2",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/mscodec2.xcframework.zip",
				checksum: "2c55cf3d86717f314352403f7c7aeb6e6e6de22c5681e6c1126773605f8fc1ae"
			),
			
			.binaryTarget(
				name: "msopenh264",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/msopenh264.xcframework.zip",
				checksum: "77c7f005b9d2ca0676fd5c7160a64fd41eb7788ff46b7551e36bfd393afe3e22"
			),
			
			.binaryTarget(
				name: "ortp",
				url: "https://download.linphone.org/releases/ios//spm//linphone-sdk-swift-ios-5.5.39/XCFrameworks/ortp.xcframework.zip",
				checksum: "e4b4b426e35babcc4939a6197304d65e3f01359c1ad27e129a26c6c21842a24a"
			),
			
		.target(
			name: "linphonexcframeworks",
			dependencies: ["bctoolbox-ios", "bctoolbox-tester", "bctoolbox", "belcard", "belle-sip", "belr", "lime", "linphone", "linphonetester", "mbedcrypto", "mbedtls", "mbedx509", "mediastreamer2", "msamr", "mscodec2", "msopenh264", "ortp"]
		),

		.target(
			name: "linphonesw",
			dependencies: ["linphonexcframeworks"]
		)
    ]
)

