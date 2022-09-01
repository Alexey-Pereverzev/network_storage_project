module network_storage_project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires io.netty.all;
    requires java.datatransfer;

    opens client to javafx.fxml;
    exports client;

    opens common to javafx.fxml;
    exports common;
}