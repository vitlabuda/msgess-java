package cz.vitlabuda.msgess;

/*
SPDX-License-Identifier: BSD-3-Clause

Copyright (c) 2021 Vít Labuda. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:
 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
    disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other materials provided with the distribution.
 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
    products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.*;

/**
 * The MsgESS (Message Exchange over Stream Sockets) [messages] is a library and network protocol which allows
 * applications to send and receive different types of data (raw binary data, UTF-8 strings, JSON, ...) in the form
 * of messages reliably over any stream socket (a socket with the SOCK_STREAM type, e.g. TCP or Unix sockets). Each
 * message can be assigned a message class that allows the app using the library to multiplex message channels.
 */
public class MsgESS {
    public static final int LIBRARY_VERSION = 1;
    public static final int PROTOCOL_VERSION = 3;

    private static final byte[] MAGIC_HEADER_STRING = "MsgESSbegin".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MAGIC_FOOTER_STRING = "MsgESSend".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PROTOCOL_VERSION_BYTES = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(PROTOCOL_VERSION).array();

    private static final byte DATA_TYPE_BINARY = 1;
    private static final byte DATA_TYPE_STRING = 2;
    private static final byte DATA_TYPE_JSON_ARRAY = 3;
    private static final byte DATA_TYPE_JSON_OBJECT = 4;

    private final Socket socket;
    private boolean compressMessages = true;
    private int maxMessageSize = 25000000;

    /**
     * Initializes a new MsgESS instance.
     *
     * @param socket The socket to receive and send messages through.
     */
    public MsgESS(Socket socket) {
        this.socket = socket;
    }

    /**
     * Gets the socket passed to the constructor.
     *
     * @return The socket passed to the constructor.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Turns the message compression on or off.
     *
     * @param compressMessages Turn the message compression on or off.
     */
    public void setCompressMessages(boolean compressMessages) {
        this.compressMessages = compressMessages;
    }

    /**
     * Set the maximum accepted message size while receiving.
     *
     * @param maxMessageSize The new maximum message size in bytes.
     * @throws MsgESSException If the specified maximum message size is negative.
     */
    public void setMaxMessageSize(int maxMessageSize) throws MsgESSException {
        if(maxMessageSize < 0)
            throw new MsgESSException("The new maximum message size is invalid!");

        this.maxMessageSize = maxMessageSize;
    }


    /**
     * Send a message with binary data in its body to the socket.
     *
     * @param binaryData The MBinaryData object with the message to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendBinaryData(MBinaryData binaryData) throws MsgESSException {
        sendBinaryData(binaryData, DATA_TYPE_BINARY);
    }

    /**
     * Receive a message with binary data in its body from the socket. Blocks until a full message is received.
     *
     * @return The MBinaryData object with the received message.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MBinaryData receiveBinaryData() throws MsgESSException {
        return receiveBinaryData(DATA_TYPE_BINARY);
    }

    /**
     * Send a message with an UTF-8 string in its body to the socket.
     *
     * @param string The MString object with the message to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendString(MString string) throws MsgESSException {
        sendString(string, DATA_TYPE_STRING);
    }

    /**
     * Receive a message with an UTF-8 string in its body from the socket. Blocks until a full message is received.
     *
     * @return The MString object with the received message.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MString receiveString() throws MsgESSException {
        return receiveString(DATA_TYPE_STRING);
    }

    /**
     * Send a message with a serialized JSON array in its body to the socket.
     *
     * @param jsonArray The MJSONArray object with the message to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendJSONArray(MJSONArray jsonArray) throws MsgESSException {
        MString string = new MString(jsonArray.getJSONArray().toString(), jsonArray.getMessageClass());
        sendString(string, DATA_TYPE_JSON_ARRAY);
    }

    /**
     * Receive a message with a serialized JSON array in its body from the socket. Blocks until a full message is received.
     *
     * @return The MJSONArray object with the received message.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MJSONArray receiveJSONArray() throws MsgESSException {
        MString string = receiveString(DATA_TYPE_JSON_ARRAY);

        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(string.getString());
        } catch (JSONException e) {
            throw new MsgESSException("Failed to deserialize the received JSON array!", e);
        }

        return new MJSONArray(jsonArray, string.getMessageClass());
    }

    /**
     * Send a message with a serialized JSON object in its body to the socket.
     *
     * @param jsonObject The MJSONObject object with the message to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendJSONObject(MJSONObject jsonObject) throws MsgESSException {
        MString string = new MString(jsonObject.getJSONObject().toString(), jsonObject.getMessageClass());
        sendString(string, DATA_TYPE_JSON_OBJECT);
    }

    /**
     * Receive a message with a serialized JSON object in its body from the socket. Blocks until a full message is received.
     *
     * @return The MJSONObject object with the received message.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MJSONObject receiveJSONObject() throws MsgESSException {
        MString string = receiveString(DATA_TYPE_JSON_OBJECT);

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(string.getString());
        } catch (JSONException e) {
            throw new MsgESSException("Failed to deserialize the received JSON object!", e);
        }

        return new MJSONObject(jsonObject, string.getMessageClass());
    }



    private void sendBinaryData(MBinaryData binaryData, byte dataType) throws MsgESSException {
        // --- prepare data
        byte[] bytes = binaryData.getBinaryData();
        if(compressMessages) {
            bytes = compressBytes(bytes);
            System.gc();
        }

        byte[] bytesLength = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bytes.length).array();
        byte[] messageClass = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(binaryData.getMessageClass()).array();
        byte isMessageCompressed = (byte) (compressMessages ? 1 : 0);

        // --- assemble message
        // message header = magic string (11b), protocol version (4b), raw bytes length (4b),
        //   user-defined message class (4b), is message compressed? (1b), data type (1b) -> 25 bytes in total
        // message footer = magic string (9b) -> 9 bytes in total
        byte[] message = new byte[bytes.length + 34];
        System.arraycopy(MAGIC_HEADER_STRING, 0, message, 0, 11);
        System.arraycopy(PROTOCOL_VERSION_BYTES, 0, message, 11, 4);
        System.arraycopy(bytesLength, 0, message, 15, 4);
        System.arraycopy(messageClass, 0, message, 19, 4);
        message[23] = isMessageCompressed;
        message[24] = dataType;
        System.arraycopy(bytes, 0, message, 25, bytes.length);
        System.arraycopy(MAGIC_FOOTER_STRING, 0, message, 25 + bytes.length, 9);

        // --- clean up useless & potentially huge data
        bytes = null;
        System.gc();

        // --- send message
        sendAllBytesToSocket(message);
    }

    private MBinaryData receiveBinaryData(byte dataType) throws MsgESSException {
        /// --- receive, check and parse header (see sendBinaryData(MBinaryData, byte) for header items)
        byte[] header = receiveNBytesFromSocket(25);

        if(!Arrays.equals(Arrays.copyOfRange(header, 0, 11), MAGIC_HEADER_STRING))
            throw new MsgESSException("The received message has an invalid magic header string!");

        if(!Arrays.equals(Arrays.copyOfRange(header, 11, 15), PROTOCOL_VERSION_BYTES))
            throw new MsgESSException("The remote host uses an incompatible protocol version!");

        int bytesLength = ByteBuffer.wrap(header, 15, 4).order(ByteOrder.BIG_ENDIAN).getInt();
        if(bytesLength < 0)
            throw new MsgESSException("The received message's length is invalid!");
        if(bytesLength > this.maxMessageSize)
            throw new MsgESSException("The received message is too big!");

        int messageClass = ByteBuffer.wrap(header, 19,  4).order(ByteOrder.BIG_ENDIAN).getInt();
        if(messageClass < 0)
            throw new MsgESSException("The received message's class is invalid!");

        boolean isMessageCompressed = (header[23] != 0);

        if(header[24] != dataType)
            throw new MsgESSException("The received message has an invalid data type!");

        // --- receive and possibly decompress body
        byte[] bytes = receiveNBytesFromSocket(bytesLength);
        if(isMessageCompressed) {
            bytes = decompressBytes(bytes);
            System.gc();
        }

        // --- receive and check footer
        byte[] footer = receiveNBytesFromSocket(9);
        if(!Arrays.equals(MAGIC_FOOTER_STRING, footer))
            throw new MsgESSException("The received message has an invalid magic footer string!");

        return new MBinaryData(bytes, messageClass);
    }

    private void sendString(MString string, byte dataType) throws MsgESSException {
        MBinaryData binaryData = new MBinaryData(string.getString().getBytes(StandardCharsets.UTF_8), string.getMessageClass());
        sendBinaryData(binaryData, dataType);
    }

    private MString receiveString(byte dataType) throws MsgESSException {
        MBinaryData binaryData = receiveBinaryData(dataType);

        String string = new String(binaryData.getBinaryData(), StandardCharsets.UTF_8);

        return new MString(string, binaryData.getMessageClass());
    }



    private byte[] compressBytes(byte[] uncompressedBytes) throws MsgESSException {
        try (
            ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream compressor = new GZIPOutputStream(compressedOutputStream)
        ) {
            compressor.write(uncompressedBytes);
            compressor.finish();
            compressor.flush();

            return compressedOutputStream.toByteArray();

        } catch (IOException e) {
            throw new MsgESSException("Failed to compress the data!", e);
        }
    }

    private byte[] decompressBytes(byte[] compressedBytes) throws MsgESSException {
        try (
            ByteArrayOutputStream decompressedOutputStream = new ByteArrayOutputStream();
            ByteArrayInputStream compressedInputStream = new ByteArrayInputStream(compressedBytes);
            GZIPInputStream decompressor = new GZIPInputStream(compressedInputStream)
        ) {
            byte[] buffer = new byte[4096];
            int len;

            while((len = decompressor.read(buffer)) > 0)
                decompressedOutputStream.write(buffer, 0, len);

            decompressedOutputStream.flush();

            return decompressedOutputStream.toByteArray();

        } catch (IOException e) {
            throw new MsgESSException("Failed to decompress the data!", e);
        }
    }

    private void sendAllBytesToSocket(byte[] bytes) throws MsgESSException {
        try {
            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(bytes);
            outputStream.flush();

        } catch (IOException e) {
            throw new MsgESSException("Failed to send data to the socket!", e);
        }
    }

    private byte[] receiveNBytesFromSocket(int n) throws MsgESSException {
        byte[] bytes = new byte[n];

        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());

            inputStream.readFully(bytes);

            return bytes;

        } catch (IOException e) {
            throw new MsgESSException("Failed to receive data from the socket!", e);
        }
    }
}
