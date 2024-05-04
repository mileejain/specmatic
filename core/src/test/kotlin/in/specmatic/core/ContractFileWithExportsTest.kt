package `in`.specmatic.core

import `in`.specmatic.osAgnosticPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ContractFileWithExportsTest {
    @Test
    fun `uses the sibling property to generate a canonical path for the provided path`() {
        val contractFileWithExports =
            ContractFileWithExports("contract.spec", AnchorFile("/path/to/hello/../hello/something/../world"))

        assertThat(osAgnosticPath(contractFileWithExports.absolutePath)).isEqualTo(osAgnosticPath("/path/to/hello/contract.spec"))
    }
}