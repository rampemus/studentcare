package org.utu.studentcare.javafx;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

// Oheisluokka, EI tarvitse ymmärtää, muokata eikä käyttää omassa koodissa työn tekemiseksi

/**
 * Basic GUI console.
 * <p>
 * This class is final. Modification is not necessary.
 */
public class FXConsole extends TextArea {
    private volatile boolean active = true;

    private void updateText(String newText) {
        if (newText.startsWith("\033c")) {
            setText(newText.substring(2));
        } else {
            appendText(newText);
        }

        // enforce a maximum content length
        // otherwise the control would become unresponsive
        String s = getText();
        if (s.length()>8000) {
            setText(s.substring(s.length()-8000));
        }

        //auto-scroll
        setScrollTop(Double.MAX_VALUE);
        positionCaret(getLength());
    }

    private final OutputStream stream = new OutputStream() {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(200);
        private final StringBuffer buffer2 = new StringBuffer();
        boolean wasESC;

        int push(String s) {
            synchronized (buffer2) {
                int oldLen = buffer2.length();
                buffer2.append(s);
                return oldLen;
            }
        }

        int clearAndPush(String s) {
            synchronized (buffer2) {
                int oldLen = buffer2.length();
                buffer2.setLength(0);
                buffer2.append(s);
                return oldLen;
            }
        }

        String pop2() {
            synchronized (buffer2) {
                String s = buffer2.toString();
                buffer2.setLength(0);
                return s;
            }
        }
        String pop() {
            synchronized (buffer) {
                String s = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                buffer.reset();
                return s;
            }
        }

        @Override
        public void write(int b) {
            synchronized (buffer) {
                // handle ANSI console reset
                if (wasESC && b == 'c') {
                    buffer.reset();
                    if (clearAndPush("\033c") == 0)
                        Platform.runLater(() -> updateText(pop2()));
                    return;
                }
                wasESC = b == '\033';
                buffer.write(b);

                if (b == '\n')
                    if (push(pop()) == 0)
                        Platform.runLater(() -> updateText(pop2()));
            }
        }
    };


    public final LinkedBlockingQueue<String> commands = new LinkedBlockingQueue<>(10);

    public final TextField inputField = new TextField() {{
        setOnKeyReleased(k -> {
            if (k.getCode() == KeyCode.ENTER) {
                commands.offer(inputField.getText());
                inputField.clear();
            }
        });
    }};

    public void close() {
        active = false;
    }

    public FXConsole() {
        PrintStream originalOut = System.out;

        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                originalOut.write(b);
                stream.write(b);
            }
        }));
        Font f;
        f = Font.font("Source Code pro", FontWeight.BOLD, 14);
        if (f == null)
            f = Font.font("Courier New", FontWeight.BOLD, 14);
        if (f != null)
            setFont(f);

        setEditable(false);
        setFocusTraversable(false);
        inputField.requestFocus();
        new Thread(() -> {
            Scanner s = new Scanner(System.in);
            while(active) {
                commands.offer(s.nextLine());
                Thread.yield();
            }
        }).start();
    }
}
