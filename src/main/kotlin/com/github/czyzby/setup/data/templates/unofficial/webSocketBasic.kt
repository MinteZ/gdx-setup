package com.github.czyzby.setup.data.templates.unofficial

import com.github.czyzby.setup.data.libs.unofficial.VisUI
import com.github.czyzby.setup.data.libs.unofficial.Websocket
import com.github.czyzby.setup.data.project.Project
import com.github.czyzby.setup.data.templates.Template
import com.github.czyzby.setup.views.ProjectTemplate

/**
 * Shows basic web sockets extension usage. Uses VisUI to create simple GUI.
 * @author MJ
 */
@ProjectTemplate
class WebSocketBasicTemplate : Template {
    override val id = "webSocketBasicTemplate"

    override fun apply(project: Project) {
        super.apply(project)
        // Initiating dependencies:
        VisUI().initiate(project)
        Websocket().initiate(project)
    }

    override fun getApplicationListenerContent(project: Project): String = """package ${project.basic.rootPackage};

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketAdapter;
import com.github.czyzby.websocket.WebSocketListener;
import com.github.czyzby.websocket.data.WebSocketCloseCode;
import com.github.czyzby.websocket.data.WebSocketException;
import com.github.czyzby.websocket.net.ExtendedNet;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class ${project.basic.mainClass} extends ApplicationAdapter {
    /** Default screen size. */
    public static int WIDTH = 640, HEIGHT = 480;

    private Stage stage;
    private WebSocket webSocket;

    // Widgets:
    private Button connectingButton;
    private Button sendingButton;
    private Button disconnectingButton;

    private VisTextField hostInput;
    private VisTextField portInput;
    private Button secureButton;

    private VisTextField input;
    private VisLabel status;

    @Override
    public void create() {
        // Loading VisUI skin assets:
        VisUI.load(VisUI.SkinScale.X2);

        stage = new Stage(new FitViewport(WIDTH, HEIGHT));
        Gdx.input.setInputProcessor(stage);

        VisWindow window = new VisWindow("Web sockets test");
        TableUtils.setSpacingDefaults(window);
        window.setFillParent(true);

        // Buttons row:
        connectingButton = new VisTextButton("Open");
        sendingButton = new VisTextButton("Send");
        sendingButton.setDisabled(true);
        disconnectingButton = new VisTextButton("Close");
        disconnectingButton.setDisabled(true);
        VisTable table = new VisTable(true);
        table.defaults().growX();
        table.add(connectingButton);
        table.add(sendingButton);
        table.add(disconnectingButton);
        addListeners();
        window.add(table).growX().row();

        // Address rows:
        hostInput = new VisTextField("echo.websocket.org");
        hostInput.setMessageText("Enter host.");
        table = new VisTable(true);
        table.add("Host:");
        table.add(hostInput).growX();
        window.add(table).growX().row();

        secureButton = new VisCheckBox("Secure");
        portInput = new VisTextField("80");
        portInput.setTextFieldFilter(new VisTextField.TextFieldFilter.DigitsOnlyFilter());
        portInput.setMessageText("Enter port.");
        table = new VisTable(true);
        table.add("Port:");
        table.add(portInput);
        table.add(secureButton);
        window.add(table).growX().row();

        // Input row:
        input = new VisTextField("", "small");
        input.setMessageText("Enter packet content.");
        input.setDisabled(true);
        window.add(input).growX().row();

        // Status label:
        status = new VisLabel("Connect to the server to send a message.", "small");
        status.setColor(VisUI.getSkin().getColor("vis-blue"));
        status.setWrap(true);
        window.add(status).width(WIDTH - 10f).expandY().align(Align.top);

        window.pack();
        window.centerWindow();
        stage.addActor(window.fadeIn());
    }

    private void addListeners() {
        connectingButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                connect();
            }
        });
        sendingButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendMessage();
            }
        });
        disconnectingButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                disconnect();
            }
        });
    }

    public void connect() {
        if (hostInput.isEmpty() || portInput.isEmpty()) {
            status.setText("Fill host and port. echo.websocket.org hosts a free web socket echo server at 80.");
        } else if (webSocket == null || !webSocket.isOpen()) {
            String host = hostInput.getText();
            int port = Integer.parseInt(portInput.getText());
            ExtendedNet net = ExtendedNet.getNet();
            if (secureButton.isChecked()) {
                webSocket = net.newSecureWebSocket(host, port);
            } else {
                webSocket = net.newWebSocket(host, port);
            }
            webSocket.addListener(getWebSocketListener());
            status.setText("Connecting...");
            try {
                webSocket.connect();
            } catch (WebSocketException exception) {
                status.setText("Cannot connect: " + exception.getMessage());
                Gdx.app.error("WebSocket", "Cannot connect.", exception);
            }
        }
    }

    private WebSocketListener getWebSocketListener() {
        return new WebSocketAdapter() {
            @Override
            public boolean onOpen(WebSocket webSocket) {
                status.setText("Connected.");
                connectingButton.setDisabled(true);
                sendingButton.setDisabled(false);
                disconnectingButton.setDisabled(false);
                input.setDisabled(false);
                return FULLY_HANDLED;
            }

            @Override
            public boolean onMessage(WebSocket webSocket, String packet) {
                status.setText("Received message: " + packet);
                return FULLY_HANDLED;
            }

            @Override
            public boolean onClose(WebSocket webSocket, WebSocketCloseCode code, String reason) {
                status.setText("Disconnected.");
                connectingButton.setDisabled(false);
                sendingButton.setDisabled(true);
                disconnectingButton.setDisabled(true);
                input.setDisabled(true);
                return FULLY_HANDLED;
            }
        };
    }

    public void sendMessage() {
        String message = input.getText();
        if (webSocket != null && webSocket.isOpen()) {
            status.setText("Sent message: " + message);
            webSocket.send(message);
            input.clearText();
        }
    }

    public void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.close();
            } catch (WebSocketException exception) {
                Gdx.app.log("WebSocket", "Unable to close web socket.", exception);
            } finally {
                webSocket = null;
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void dispose() {
        VisUI.dispose();
        stage.dispose();
        disconnect();
    }
}"""

    override fun getAndroidLauncherContent(project: Project): String = """package ${project.basic.rootPackage}.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import ${project.basic.rootPackage}.${project.basic.mainClass};

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initiating web sockets module:
        CommonWebSockets.initiate();
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        initialize(new ${project.basic.mainClass}(), configuration);
    }
}"""

    override fun getDesktopLauncherContent(project: Project): String = """package ${project.basic.rootPackage}.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import ${project.basic.rootPackage}.${project.basic.mainClass};

/** Launches the desktop (LWJGL) application. */
public class DesktopLauncher {
    public static void main(final String[] args) {
        // Initiating web sockets module:
        CommonWebSockets.initiate();
        createApplication();
    }

    private static LwjglApplication createApplication() {
        return new LwjglApplication(new ${project.basic.mainClass}(), getDefaultConfiguration());
    }

    private static LwjglApplicationConfiguration getDefaultConfiguration() {
        final LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
        configuration.title = "${project.basic.name}";
        configuration.width = ${project.basic.mainClass}.WIDTH;
        configuration.height = ${project.basic.mainClass}.HEIGHT;
        return configuration;
    }
}"""

    override fun getGwtLauncherContent(project: Project): String = """package ${project.basic.rootPackage}.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.czyzby.websocket.GwtWebSockets;
import ${project.basic.rootPackage}.${project.basic.mainClass};

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration configuration = new GwtApplicationConfiguration(${project.basic.mainClass}.WIDTH, ${project.basic.mainClass}.HEIGHT);
        return configuration;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        // Initiating GWT web sockets module:
        GwtWebSockets.initiate();
        return new ${project.basic.mainClass}();
    }
}"""
}
