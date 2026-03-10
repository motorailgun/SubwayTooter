plugins {
    id("buildLogic.StAndroidLib")
}

android {
    namespace = "com.jrummyapps.android.colorpicker"
    resourcePrefix = "cpv_"

    buildFeatures {
        viewBinding = true
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // dismissSafe, systemService, View.gone() など
    implementation(project(":base"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.annotation)
    implementation(libs.google.flexbox)
}
