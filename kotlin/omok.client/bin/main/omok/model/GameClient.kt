package omok.model

import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket
import java.nio.charset.Charset

class GameClient(
    private val chatArea: TextArea,
    private val idLabel: Label,
    private val roomLabel: Label,
    private val host: String = "localhost",
    private val port: Int = 9090,
) {
    private val socket = Socket(host, port)
    private val inputStream = socket.inputStream
    private val outputStream = socket.outputStream
    private val buffer = StringBuilder()

    var playerId: String? = null
    lateinit var playerColor: String

    var onStonePlaced: ((Int, Int, Int) -> Unit)? = null
    var onGameEnd: ((String) -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null
    var onReady: (() -> Unit)? = null

    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            startListening()
        }
    }

    private fun startListening() {
        try {
            val tempBuffer = ByteArray(1024)
            while (socket.isConnected) {
                val bytesRead = inputStream.read(tempBuffer)
                if (bytesRead == -1) break

                buffer.append(String(tempBuffer, 0, bytesRead, Charset.defaultCharset()))

                while (true) {
                    val packet = extractNextPacketFromBuffer() ?: break
                    processPacket(packet)
                }
            }
        } catch (e: Exception) {
            Platform.runLater {
                chatArea.appendText("[CLIENT] Connection error: ${e.message}\n")
            }
        } finally {
            Platform.runLater {
                chatArea.appendText("[CLIENT] Disconnected from server.\n")
            }
            socket.close()
        }
    }

    private fun extractNextPacketFromBuffer(): String? {
        val startIndex = buffer.indexOf("<")
        val endIndex = buffer.indexOf(">")
        return if (startIndex != -1 && endIndex > startIndex) {
            val packet = buffer.substring(startIndex, endIndex + 1)
            buffer.delete(0, endIndex + 1)
            packet
        } else {
            if (endIndex < startIndex && endIndex != -1) {
                buffer.delete(0, startIndex)
            }
            null
        }
    }

    private fun processPacket(packet: String) {
        val command = packet.substringAfter("<").substringBefore(":")
        val payload = packet.removeSurrounding("<", ">").split(":").drop(1)

        if (playerId == null && command != "NOTIFY" && payload.isNotEmpty()) {
            playerId = payload[0]
            Platform.runLater { onReady?.invoke() }
        }

        Platform.runLater {
            when (command) {
                "SET_PLAYER_ID" -> {
                    playerId = payload[0]
                    chatArea.appendText("[SYSTEM] Your player ID is $playerId.\n")
                    idLabel.text = "Player ID: $playerId"
                }
                "SET_ROOM" -> {
                    val roomName = payload[0]
                    roomLabel.text = "Room: $roomName"
                }
                "SET_COLOR" -> {
                    playerColor = payload[0]
                    chatArea.appendText("[SYSTEM] You are playing as $playerColor.\n")
                }
                "COORDINATE" -> {
                    val x = payload[0].toInt()
                    val y = payload[1].toInt()
                    val player = payload[2].toInt()
                    onStonePlaced?.invoke(x, y, player)
                }
                "MESSAGE" -> {
                    onMessageReceived?.invoke(payload[0])
                }
                "NOTIFY" -> {
                    chatArea.appendText("${payload[1]}\n")
                }
                "MATCH_RESULT" -> {
                    val winnerId = payload[0]
                    chatArea.appendText("[SYSTEM] Game over! Player $winnerId wins!\n")
                    onGameEnd?.invoke(winnerId.toString())
                }
                else -> {
                    chatArea.appendText("Unknown command: $command\n")
                }
            }
        }
    }

    fun sendPacket(data: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                outputStream.write(data.toByteArray(Charset.defaultCharset()))
                outputStream.flush()
            } catch (e: Exception) {
                Platform.runLater {
                    chatArea.appendText("[CLIENT] Failed to send packet: ${e.message}\n")
                }
            }
        }
    }

    fun attendGame() {
        sendPacket("<ATTENDANCE:roomId>")
    }

    fun exitGame() {
        sendPacket("<EXIT:roomId>")
    }

    fun sendMessage(message: String) {
        playerId?.let { id ->
            val packet = "<MESSAGE:[${id}] ${message}>"
            sendPacket(packet)
        }
    }

    fun placeStone(x: Int, y: Int): Boolean {
        var pc = "1"
        if (playerColor == "white") {
            pc = "2"
        }
        sendPacket("<COORDINATE:$x:$y:$pc>")
        return true
    }
}
