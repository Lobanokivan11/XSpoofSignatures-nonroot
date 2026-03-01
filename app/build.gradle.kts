plugins {
	id("com.android.application")
}

android {
	namespace = "dev.rushii.xspoofsignatures"
	compileSdk = 35

	defaultConfig {
		applicationId = "dev.rushii.xspoofsignatures"
		minSdk = 30
		targetSdk = 35
		versionCode = 2
		versionName = "1.0.1-noroot"
	}

	signingConfigs {
		create("release") {
            storeFile = file("../sign.keystore")
            storePassword = "369852"
            keyAlias = "lob"
            keyPassword = "369852"

			enableV1Signing = true
			enableV2Signing = true
			enableV3Signing = true
			enableV4Signing = true
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro",
			)

			if (System.getenv("RELEASE") == "true") {
				signingConfig = signingConfigs.getByName("release")
			} else {
				signingConfig = signingConfigs.getByName("debug")
			}
		}
	}
}

dependencies {
	compileOnly("de.robv.android.xposed:api:82")
}
