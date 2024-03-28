package com.wilinz.socketdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wilinz.socketdemo.ui.theme.SocketdemoTheme
import java.net.InetSocketAddress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SocketdemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold {
                        Column(
                            modifier = Modifier
                                .padding(it)
                                .widthIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val vm: MainViewModel = viewModel()
                            SelectionContainer {
                                Text(
                                    text = "客户端与服务端需要处于同一局域网，可以开热点，本机ip如下（请选择和对方最相似的一个ip）: \n${
                                        vm.ips.joinToString(
                                            separator = "\n"
                                        )
                                    }"
                                )
                            }
                            TextButton(onClick = { vm.getIPs() }) {
                                Text(text = "点击刷新ip")
                            }

                            Text(text = "----------如果本机作为服务器----------")
                            ElevatedButton(
                                onClick = {
                                    vm.startSocketServer()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "启动服务器")
                            }
                            Text(text = "----------如果本机作为服务器----------")

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(text = "----------如果本机作为客户端----------")
                            var addr by remember {
                                mutableStateOf("")
                            }
                            OutlinedTextField(
                                value = addr,
                                onValueChange = { addr = it },
                                placeholder = {
                                    Text(text = "例如：192.168.1.1")
                                },
                                label = {
                                    Text(text = "输入对方ip")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            ElevatedButton(onClick = {
                                vm.connectSocketServer(addr)
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(text = "连接服务器")
                            }

                            Text(text = "----------如果本机作为客户端----------")

                            Spacer(modifier = Modifier.height(32.dp))
                            var msg by remember {
                                mutableStateOf("")
                            }
                            OutlinedTextField(
                                value = msg,
                                onValueChange = { msg = it },
                                placeholder = {
                                    Text(text = "输入消息")
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        vm.sendMessage(msg)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "发送"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )

                            Text(text = "当前已连接用户：")
                            LazyColumn(
                                modifier = Modifier
                                    .height(50.dp)
                                    .fillMaxWidth()
                            ) {
                                items(vm.serverChannels) {
                                    val inetSocketAddress =
                                        it.remoteAddress() as InetSocketAddress
                                    Text(text = "$inetSocketAddress")
                                }
                            }

                            Text(text = "消息列表")
                            LazyColumn(
                                modifier = Modifier
                                    .height(500.dp)
                                    .fillMaxWidth()
                            ) {
                                items(vm.messages) {
                                    Text(text = it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SocketdemoTheme {
        Greeting("Android")
    }
}