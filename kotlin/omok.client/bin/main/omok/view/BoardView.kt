package omok.view

import javafx.scene.canvas.Canvas
import javafx.scene.control.Alert
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import omok.model.GameClient

class BoardView(
    private val client: GameClient,
) : Pane() {
    private val canvas = Canvas(600.0, 600.0)
    private val gc = canvas.graphicsContext2D
    private val board = Array(19) { IntArray(19) }
    private var currentPlayer = 1

    init {
        children.add(canvas)
        draw()

        // GameClient로부터 받은 돌 놓기 정보를 처리
        client.onStonePlaced = { x, y, player ->
            board[y][x] = player
            currentPlayer = if (player == 1) 2 else 1
            draw()
        }

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED) {
            val x = (it.x / (canvas.width / 19)).toInt()
            val y = (it.y / (canvas.height / 19)).toInt()

            // 서버에 돌 놓기 요청
            if (client.placeStone(x, y)) {
                client.playerId?.let { id ->
                    var pc = 1
                    if (client.playerColor == "white") {
                        pc = 2
                    }
                }
            }
        }
    }

    fun draw() {
        gc.fill = Color.BEIGE
        gc.fillRect(0.0, 0.0, canvas.width, canvas.height)

        // Draw board
        gc.stroke = Color.BLACK
        for (i in 0..18) {
            gc.strokeLine(i * (canvas.width / 19) + 20, 20.0, i * (canvas.width / 19) + 20, canvas.height - 20)
            gc.strokeLine(20.0, i * (canvas.height / 19) + 20, canvas.width - 20, i * (canvas.height / 19) + 20)
        }

        // Draw stones
        for (y in 0..18) {
            for (x in 0..18) {
                if (board[y][x] != 0) {
                    gc.fill = if (board[y][x] == 1) Color.BLACK else Color.WHITE
                    gc.fillOval(x * (canvas.width / 19) + 5, y * (canvas.height / 19) + 5, 30.0, 30.0)
                }
            }
        }
    }

    fun reset() {
        for (i in board.indices) {
            for (j in board[i].indices) {
                board[i][j] = 0
            }
        }
        currentPlayer = 1
        draw()
    }
}