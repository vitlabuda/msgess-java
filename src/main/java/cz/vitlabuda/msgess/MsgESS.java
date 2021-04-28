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
    public static final int LIBRARY_VERSION = 2;
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
     */
    public void setMaxMessageSize(int maxMessageSize) {
        if(maxMessageSize < 0)
            throw new MsgESSRuntimeException("The new maximum message size is invalid!");

        this.maxMessageSize = maxMessageSize;
    }


    /**
     * Send a message with binary data in its body to the socket.
     *
     * @param binaryData The MBinaryData object with the message to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendBinaryData(MBinaryData binaryData) throws MsgESSException {
        MAnyData data = new MAnyData(MAnyData.DataType.BINARY, binaryData.getBinaryData(), binaryData.getMessageClass());
        sendAnyData(data);
    }

    /**
     * Receive a message with binary data in its body from the socket. Blocks until a full message is received.
     *
     * @return The MBinaryData object with the received message.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MBinaryData receiveBinaryData() throws MsgESSException {
        MAnyData data = receiveAnyData();
        if(data.getDataType() != MAnyData.DataType.BINARY)
            throw new MsgESSException("The received message has an unwanted data type!");

        return new MBinaryData((byte[]) data.getData(), data.getMessageClass());
    }

    /**
     * Send a message with an UTF-8 string in its body to the socket.
     *
     * @param string The MString object with the message to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendString(MString string) throws MsgESSException {
        MAnyData data = new MAnyData(MAnyData.DataType.STRING, string.getString(), string.getMessageClass());
        sendAnyData(data);
    }

    /**
     * Receive a message with an UTF-8 string in its body from the socket. Blocks until a full message is received.
     *
     * @return The MString object with the received message.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MString receiveString() throws MsgESSException {
        MAnyData data = receiveAnyData();
        if(data.getDataType() != MAnyData.DataType.STRING)
            throw new MsgESSException("The received message has an unwanted data type!");

        return new MString((String) data.getData(), data.getMessageClass());
    }

    /**
     * Send a message with a serialized JSON array in its body to the socket.
     *
     * @param jsonArray The MJSONArray object with the message to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendJSONArray(MJSONArray jsonArray) throws MsgESSException {
        MAnyData data = new MAnyData(MAnyData.DataType.JSON_ARRAY, jsonArray.getJSONArray(), jsonArray.getMessageClass());
        sendAnyData(data);
    }

    /**
     * Receive a message with a serialized JSON array in its body from the socket. Blocks until a full message is received.
     *
     * @return The MJSONArray object with the received message.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MJSONArray receiveJSONArray() throws MsgESSException {
        MAnyData data = receiveAnyData();
        if(data.getDataType() != MAnyData.DataType.JSON_ARRAY)
            throw new MsgESSException("The received message has an unwanted data type!");

        return new MJSONArray((JSONArray) data.getData(), data.getMessageClass());
    }

    /**
     * Send a message with a serialized JSON object in its body to the socket.
     *
     * @param jsonObject The MJSONObject object with the message to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendJSONObject(MJSONObject jsonObject) throws MsgESSException {
        MAnyData data = new MAnyData(MAnyData.DataType.JSON_OBJECT, jsonObject.getJSONObject(), jsonObject.getMessageClass());
        sendAnyData(data);
    }

    /**
     * Receive a message with a serialized JSON object in its body from the socket. Blocks until a full message is received.
     *
     * @return The MJSONObject object with the received message.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MJSONObject receiveJSONObject() throws MsgESSException {
        MAnyData data = receiveAnyData();
        if(data.getDataType() != MAnyData.DataType.JSON_OBJECT)
            throw new MsgESSException("The received message has an unwanted data type!");

        return new MJSONObject((JSONObject) data.getData(), data.getMessageClass());
    }

    /**
     * Send a message with any data type in its body to the socket. The data type is specified in the MAnyData instance.
     *
     * @param data The MAnyData object with the message and its data type to send.
     * @throws MsgESSException If any error is encountered during the sending process.
     */
    public void sendAnyData(MAnyData data) throws MsgESSException {
        // --- prepare data
        byte[] bytes;
        byte dataType;
        switch (data.getDataType()) {
            case BINARY:
                dataType = DATA_TYPE_BINARY;
                bytes = (byte[]) data.getData();
                break;

            case STRING:
                dataType = DATA_TYPE_STRING;
                bytes = convertStringToBytes((String) data.getData());
                break;

            case JSON_ARRAY:
                dataType = DATA_TYPE_JSON_ARRAY;
                bytes = convertStringToBytes(((JSONArray) data.getData()).toString()); // type cast is unnecessarily done because we want to make sure that the data type is right
                break;

            case JSON_OBJECT:
                dataType = DATA_TYPE_JSON_OBJECT;
                bytes = convertStringToBytes(((JSONObject) data.getData()).toString()); // type cast is unnecessarily done because we want to make sure that the data type is right
                break;

            default:
                throw new MsgESSRuntimeException("The data type in MAnyData is invalid! (this should never happen)");
        }

        if(compressMessages) {
            bytes = compressBytes(bytes);
            System.gc();
        }

        byte[] bytesLength = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(bytes.length).array();
        byte[] messageClass = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(data.getMessageClass()).array();
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

    /**
     * Receive a message with any data type in its body from the socket. Blocks until a full message is received. The data type is specified in the MAnyData instance.
     *
     * @return The MAnyData object with the received message and its data type.
     * @throws MsgESSException If any error is encountered during the receiving process.
     */
    public MAnyData receiveAnyData() throws MsgESSException {
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

        byte dataType = header[24];


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


        // --- disassemble message
        MAnyData data;
        switch (dataType) {
            case DATA_TYPE_BINARY:
                data = new MAnyData(MAnyData.DataType.BINARY, bytes, messageClass);
                break;

            case DATA_TYPE_STRING:
                data = new MAnyData(MAnyData.DataType.STRING, convertBytesToString(bytes), messageClass);
                break;

            case DATA_TYPE_JSON_ARRAY:
                try {
                    data = new MAnyData(MAnyData.DataType.JSON_ARRAY, new JSONArray(convertBytesToString(bytes)), messageClass);
                } catch (JSONException e) {
                    throw new MsgESSException("Failed to deserialize the received JSON array!", e);
                }
                break;

            case DATA_TYPE_JSON_OBJECT:
                try {
                    data = new MAnyData(MAnyData.DataType.JSON_OBJECT, new JSONObject(convertBytesToString(bytes)), messageClass);
                } catch (JSONException e) {
                    throw new MsgESSException("Failed to deserialize the received JSON object!", e);
                }
                break;

            default:
                throw new MsgESSException("The received message has an invalid data type!");
        }

        return data;
    }



    private byte[] convertStringToBytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    private String convertBytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
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
