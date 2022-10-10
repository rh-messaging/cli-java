import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class AmqxMain {
    @Tag("external")
    @Test
    fun `queue properties`() {
        com.redhat.amqx.main.Main.main(
            """queue --host 127.0.0.1:1099 --action properties --name "test_transaction_commit_rollback_commit""".split(" ").toTypedArray()
        )
    }
}
