module network_storage_project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires io.netty.all;
    requires java.datatransfer;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens client to javafx.fxml;
    exports client;

    opens common.messages to javafx.fxml;
    exports common.messages;
}