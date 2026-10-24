package moe.chenxy.hyperpods.ui

import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import moe.chenxy.hyperpods.BuildConfig
import moe.chenxy.hyperpods.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Verify attribution and external destinations without opening a browser. */
@RunWith(Parameterized::class)
class AboutPageTest(private val theme: Int) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "theme={0}")
        fun themes() = listOf(arrayOf(1), arrayOf(2))
    }

    @get:Rule val compose = createAndroidComposeRule<DashboardPreviewActivity>()
    private val launched = mutableListOf<Intent>()

    @Before fun showAbout() {
        compose.runOnUiThread {
            val context = object : ContextWrapper(compose.activity) {
                override fun startActivity(intent: Intent) { launched.add(intent) }
            }
            compose.activity.setContent {
                AppTheme(theme) {
                    CompositionLocalProvider(LocalContext provides context) {
                        AboutPage(PaddingValues())
                    }
                }
            }
        }
    }

    @Test fun sourceAndLicenseAttributionsRemainVisibleWithoutSponsorEntries() {
        for (label in listOf(R.string.about_project_maintainer, R.string.project_repository,
            R.string.about_sources, R.string.about_sources_description, R.string.about_licenses,
            R.string.about_license_description)) {
            compose.revealSetting(compose.activity.getString(label)).assertIsDisplayed()
        }
        compose.onNodeWithText("AFDIAN", substring = true, ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("afdian.com", substring = true).assertDoesNotExist()
    }

    @Test fun eachRepositoryLinkOpensItsOwnRepository() {
        val repositories = listOf("https://github.com/HXLH50K/PodsOnDroid",
            "https://github.com/Art-Chen/HyperPods", "https://github.com/Mo-SeTian/HyperPods",
            "https://github.com/librepods-org/librepods", "https://github.com/adolfintel/OpenPods",
            "https://github.com/HXLH50K/PodsOnDroid/blob/main/LICENSE")
        listOf(R.string.project_repository, R.string.about_upstream_source, R.string.about_hyperpods_fork,
            R.string.about_librepods, R.string.about_openpods, R.string.about_view_license)
            .forEach { compose.revealSetting(compose.activity.getString(it)).assertIsDisplayed().performClick() }
        compose.runOnIdle {
            assertEquals(repositories, launched.map { it.dataString })
            assertEquals(List(repositories.size) { Intent.ACTION_VIEW }, launched.map { it.action })
        }
    }

    @Test fun referenceAddressesAreVisible() {
        for (url in listOf("https://github.com/Art-Chen/HyperPods",
            "https://github.com/Mo-SeTian/HyperPods", "https://github.com/librepods-org/librepods",
            "https://github.com/adolfintel/OpenPods")) {
            compose.revealSetting(url).assertIsDisplayed()
        }
    }

    @Test fun aboutShowsTheTaglineWithoutBuildOrImportHistory() {
        compose.onNodeWithText("Please Open Door, Siri.").assertIsDisplayed()
        compose.onNodeWithText(BuildConfig.VERSION_NAME).assertIsDisplayed()
        compose.onNodeWithText(BuildConfig.BUILD_REVISION, substring = true).assertDoesNotExist()
        compose.revealSetting(compose.activity.getString(R.string.about_sources_description)).assertIsDisplayed()
        compose.onNodeWithText("a44bc10e387f527062b9ba2b8f90b154b2e61248", substring = true).assertDoesNotExist()
    }

}
