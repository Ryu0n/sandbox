package omok

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import omok.model.GameClient
import omok.view.BoardView

class Main : Application() {
    override fun start(primaryStage: Stage) {
        val chatArea = TextArea()
        val idLabel = Label()
        val roomLabel = Label()
        chatArea.isEditable = false
        chatArea.prefRowCount = 5
        chatArea.prefWidth = 100.0

        val client = GameClient(chatArea, idLabel, roomLabel)
        val boardView = BoardView(client)

        val attendGameButton = Button("Attend to Game Room")
        val exitGameButton = Button("Exit from Game Room")
        val sendButton = Button("Send")
        val chatInput = TextField()

        // 초기에는 버튼 비활성화
        attendGameButton.isDisable = true
        exitGameButton.isDisable = true
        sendButton.isDisable = true
        chatInput.isDisable = true

        // GameClient가 준비되면 UI 활성화
        client.onReady = {
            attendGameButton.isDisable = false
            exitGameButton.isDisable = false
            sendButton.isDisable = false
            chatInput.isDisable = false
            chatArea.appendText("[CLIENT] Connected to server. You can now join a game.\n")
        }

        // 애플리케이션 시작 시 서버 리스닝 시작
        client.initialize()

        attendGameButton.setOnAction {
            boardView.reset()
            client.attendGame()
        }

        exitGameButton.setOnAction {
            boardView.reset()
            client.exitGame()
        }

        val root = BorderPane()
        root.center = boardView

        val buttonBox = HBox(
            10.0, attendGameButton, exitGameButton, idLabel, roomLabel
        )
        root.bottom = buttonBox

        chatInput.promptText = "Please enter your message"

        chatInput.setOnAction {
            val message = chatInput.text
            if (message.isNotBlank()) {
                chatInput.clear()
                client.sendMessage(message)
            }
        }

        sendButton.setOnAction {
            val message = chatInput.text
            if (message.isNotBlank()) {
                chatInput.clear()
                client.sendMessage(message)
            }
        }

        client.onMessageReceived = { message ->
            chatArea.appendText("$message\n")
        }

        val chatSendBox = HBox(5.0, chatInput, sendButton)
        chatSendBox.alignment = Pos.BOTTOM_CENTER

        val chatBox = VBox(10.0, chatArea, chatSendBox)
        VBox.setVgrow(chatArea, Priority.ALWAYS)
        chatBox.prefWidth = 500.0

        root.right = chatBox

        val scene = Scene(root)

        primaryStage.title = "Omok"
        primaryStage.scene = scene
        primaryStage.show()

        client.onGameEnd = { winner ->
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Game Over"
            alert.headerText = null
            alert.contentText = "Player $winner wins!"
            alert.showAndWait()
            boardView.reset()
        }
    }
}

fun main() {
    Application.launch(Main::class.java)
}