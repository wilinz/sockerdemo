package com.wilinz.socketdemo

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Enumeration
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random


fun ChannelPipeline.addDefaultHandler() {
    addLast(LengthFieldBasedFrameDecoder(Int.MAX_VALUE, 0, 4, 0, 4))
    // 编码器，用于在发送消息前加上长度字段，参数分别为：长度字段长度
    addLast(LengthFieldPrepender(4))

    // 字符串解码和编码
    addLast(StringDecoder(Charset.forName("UTF-8")))
    addLast(StringEncoder(Charset.forName("UTF-8")))
}


class MainViewModel(application: Application) : AndroidViewModel(application) {

    var clientChannel: Channel? = null
    val serverChannels = mutableStateListOf<Channel>()

    val ips = mutableStateListOf<String>()
    val messages = mutableStateListOf<String>()

    init {
        getIPs()
    }

    fun getIPs() {
        ips.clear()
        ips.addAll(getAllIPv4s())
    }

    fun sendMessage(msg: String) {
        messages.add(msg)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                clientChannel?.writeAndFlush(msg)
                withContext(Dispatchers.Main) {
                    serverChannels.forEach {
                        launch(Dispatchers.IO) {
                            it.writeAndFlush(msg)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "发送失败：${e}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    fun connectSocketServer(host: String) {

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val group: EventLoopGroup = NioEventLoopGroup()
                try {
                    val bootstrap = Bootstrap().apply {
                        group(group)
                        channel(NioSocketChannel::class.java)
                        option(ChannelOption.SO_KEEPALIVE, true)
                        handler(object : ChannelInitializer<SocketChannel>() {
                            override fun initChannel(ch: SocketChannel) {
                                ch.pipeline().apply {
                                    addDefaultHandler()
                                    addLast(ClientHandler())
                                }
                            }
                        })
                    }

                    // Start the client.
                    val channelFuture = bootstrap.connect(host, 8080).sync()

                    clientChannel = channelFuture.channel()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "连接成功", Toast.LENGTH_SHORT)
                            .show()
                    }
                    // Wait until the connection is closed.
                    channelFuture.channel().closeFuture().sync()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "连接失败：${e}", Toast.LENGTH_SHORT)
                            .show()
                    }
                } finally {
                    group.shutdownGracefully()
                }
            }
        }
    }

    fun startSocketServer() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                val bossGroup = NioEventLoopGroup(1)
                val workerGroup = NioEventLoopGroup()
                try {
                    val bootstrap = ServerBootstrap().apply {
                        group(bossGroup, workerGroup)
                        channel(NioServerSocketChannel::class.java)
                        option(ChannelOption.SO_BACKLOG, 128)
                        childOption(ChannelOption.SO_KEEPALIVE, true)
                        childHandler(object : ChannelInitializer<SocketChannel>() {
                            override fun initChannel(ch: SocketChannel) {
                                ch.pipeline().apply {
                                    addDefaultHandler()
                                    addLast(ServerHandler())
                                }
                            }
                        }) // 使用新的Initializer
                    }

                    val channelFuture = bootstrap.bind(8080).sync()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "服务器启动成功", Toast.LENGTH_SHORT)
                            .show()
                    }
                    channelFuture.channel().closeFuture().sync()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            getApplication(),
                            "服务器启动失败：${e}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } finally {
                    workerGroup.shutdownGracefully()
                    bossGroup.shutdownGracefully()
                }
            }

        }

    }


    @SuppressLint("NewApi")
    val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun InetSocketAddress.toAddressString() = "$hostString:$port"

    inner class ServerHandler : ChannelInboundHandlerAdapter() {

        override fun channelActive(ctx: ChannelHandlerContext) {
            viewModelScope.launch(Dispatchers.Main) {
                serverChannels.add(ctx.channel())
            }
            super.channelActive(ctx)
        }

        @SuppressLint("NewApi")
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            // 获取客户端的InetSocketAddress
            // 获取客户端的InetSocketAddress
            val inetSocketAddress = ctx.channel().remoteAddress() as InetSocketAddress

            // 接收到客户端消息
            val message = msg as String
            val time = ZonedDateTime.now().format(dateTimeFormatter)
            messages.add("$time: ${inetSocketAddress.toAddressString()} 发来消息：$message")
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            viewModelScope.launch(Dispatchers.Main) {
                serverChannels.remove(ctx.channel())
            }
            super.channelInactive(ctx)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }

    inner class ClientHandler : ChannelInboundHandlerAdapter() {
        @SuppressLint("NewApi")
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            // 接收到服务端消息
            val inetSocketAddress = ctx.channel().remoteAddress() as InetSocketAddress
            val message = msg as String
            val time = ZonedDateTime.now().format(dateTimeFormatter)
            messages.add("$time: ${inetSocketAddress.toAddressString()} 发来消息：$message")
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }
}


fun getAllIPv4s(): List<String> {
    val ips = mutableListOf<String>()

    try {
        val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface: NetworkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    ips.add(address.hostAddress)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ips
}
