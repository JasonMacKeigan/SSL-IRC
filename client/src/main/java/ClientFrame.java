import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;

/**
 * Created by Jason MacKeigan on 2017-06-30 at 4:44 AM
 */
public class ClientFrame extends Application {

    private final Client client;

    private Scene infoScene;

    private Scene viewScene;

    private Stage stage;

    public ClientFrame() throws Exception {
        client = new Client(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        loadInfo();

        loadView();

        primaryStage.setResizable(false);

        primaryStage.setScene(infoScene);

        primaryStage.show();

        client.afterFrameInit();
    }

    private void loadInfo() throws Exception {
        FXMLLoader infoSceneLoader = new FXMLLoader(ClientFrame.class.getResource("info_stage.fxml"));

        infoSceneLoader.setController(this);

        infoScene = new Scene(infoSceneLoader.load(), 600, 320);

        ComboBox<String> comboBox = (ComboBox<String>) infoScene.lookup("#selectHost");

        for (Host host : Host.values()) {
            comboBox.getItems().add(host.getIdentifier());
        }
    }

    private void loadView() throws Exception {
        FXMLLoader viewSceneLoader = new FXMLLoader(getClass().getResource("view_stage.fxml"));

        viewSceneLoader.setController(this);

        viewScene = new Scene(viewSceneLoader.load(), 600, 320);

        TextField field = (TextField) viewScene.lookup("#enterMessageField");

        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onSendMessage();
            }
        });
    }

    public void addOnline(String username) {
        ListView<String> listView = (ListView<String>) viewScene.lookup("#onlineUsers");

        if (listView.getItems().stream().noneMatch(item -> item.equalsIgnoreCase(username))) {
            listView.getItems().add(username);
            listView.refresh();
        }
    }

    public void removeOnline(String username) {
        ListView<String> listView = (ListView<String>) viewScene.lookup("#onlineUsers");

        listView.getItems().removeIf(user -> user.equalsIgnoreCase(username));

        listView.refresh();
    }

    public void setUsernameFieldText(String text) {
        TextField textField = ((TextField) infoScene.lookup("#usernameField"));

        textField.setText(text);
    }

    public void setInfoSceneOutput(String output) {
        Label label = (Label) infoScene.lookup("#outputLabel");

        label.setText(output);
    }

    public void appendMessage(String message) {
        TextArea messageTextArea = (TextArea) viewScene.lookup("#messageTextArea");

        if (!messageTextArea.getText().isEmpty()) {
            messageTextArea.appendText(System.lineSeparator());
        }
        messageTextArea.appendText(message);
    }

    public void onUsernameFieldTextUpdate() {
        String selectedUsername = ((TextField) infoScene.lookup("#usernameField")).getText();

        client.setUsername(selectedUsername);
    }

    public void onComboBoxAction() {
        ComboBox<String> comboBox = (ComboBox<String>) infoScene.lookup("#selectHost");

        String selected = comboBox.getSelectionModel().getSelectedItem();

        if (selected == null) {
            System.out.println("No selected.");
        } else {
            System.out.println("Selected: " + selected);
        }
        for (Host host : Host.values()) {
            if (host.getIdentifier().equals(selected)) {
                client.setHost(host);
                System.out.println("Set host to: " + host.getIdentifier());
                break;
            }
        }
    }

    public void onSendMessage() {
        TextField field = (TextField) viewScene.lookup("#enterMessageField");

        String message = field.getText();

        if (message == null || message.isEmpty()) {
            return;
        }
        field.clear();
        client.writeMessage(message);
    }

    public void logout() {
        TextField field = (TextField) viewScene.lookup("#enterMessageField");

        field.clear();

        ListView<String> onlineList = (ListView<String>) viewScene.lookup("#onlineUsers");

        onlineList.getItems().clear();

        onlineList.refresh();

        client.logout();

        showInfo();
    }

    public void login() {
        if (client.getChannel() != null && (client.getChannel().isOpen() || client.getChannel().isActive())) {
            setInfoSceneOutput("You are already connecting to server...please wait");
            return;
        }
        setInfoSceneOutput("Attempting to connect to " + client.getHost().getIdentifier());
        client.connect();
    }

    public void showInfo() {
        stage.setScene(infoScene);
    }

    public void showView() {
        stage.setScene(viewScene);
    }
}
