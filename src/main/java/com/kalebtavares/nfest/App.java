package com.kalebtavares.nfest;

import com.kalebtavares.nfest.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Inicializa Banco de Dados (Cria tabelas se não existirem)
        DatabaseConfig.inicializarBanco();

        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/main_view.fxml"));
        Parent root = fxmlLoader.load();

        scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.setTitle("Sistema Fiscal NFe - Calculadora ST (MS)");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}