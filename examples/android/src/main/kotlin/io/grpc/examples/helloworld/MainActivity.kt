package io.grpc.examples.helloworld

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(color = MaterialTheme.colors.background) {
                Greeter()
            }
        }
    }
}

@Composable
fun Greeter() {
    val context = ContextAmbient.current

    val channel = remember {
        val url = URL(context.getString(R.string.server_url))
        val port = if (url.port == -1) url.defaultPort else url.port

        println("Connecting to ${url.host}:$port")

        val builder = ManagedChannelBuilder.forAddress(url.host, port)
        if (url.protocol == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        builder.executor(Dispatchers.Default.asExecutor()).build()
    }

    val greeter = GreeterGrpcKt.GreeterCoroutineStub(channel)

    val nameState = remember { mutableStateOf(TextFieldValue()) }

    val responseState = remember { mutableStateOf("") }

    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    onActive {
        onDispose {
            channel.shutdownNow()
            scope.cancel()
        }
    }

    Column(Modifier.fillMaxWidth().fillMaxHeight(), Arrangement.Top, Alignment.CenterHorizontally) {
        Text(context.getString(R.string.name_hint), modifier = Modifier.padding(top = 10.dp))
        OutlinedTextField(nameState.value, { nameState.value = it })

        Button({
            scope.launch {
                try {
                    val request = HelloRequest.newBuilder().setName(nameState.value.text).build()
                    val response = greeter.sayHello(request)
                    responseState.value = response.message
                } catch (e: Exception) {
                    responseState.value = e.message ?: "Unknown Error"
                    e.printStackTrace()
                }
            }
        }, Modifier.padding(10.dp)) {
            Text(context.getString(R.string.send_request))
        }

        if (responseState.value.isNotEmpty()) {
            Text(context.getString(R.string.server_response), modifier = Modifier.padding(top = 10.dp))
            Text(responseState.value)
        }
    }
}
