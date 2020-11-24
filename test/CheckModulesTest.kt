import dita.dev.appModules
import org.junit.Test
import org.junit.experimental.categories.Category
import org.koin.test.category.CheckModuleTest
import org.koin.test.check.checkModules

@Category(CheckModuleTest::class)
class CheckModulesTest {

    @Test
    fun `check modules`() = checkModules {
        modules(appModules)
    }
}