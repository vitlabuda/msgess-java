# MsgESS
The **MsgESS** (Message Exchange over Stream Sockets) [messages] is a **Java** library and network protocol which allows applications to send and receive different types of data (raw binary data, UTF-8 strings, JSON, ...) in the form of messages reliably over any stream socket (a socket with the SOCK_STREAM type, e.g. TCP or Unix sockets). Each message can be assigned a message class that allows the app using the library to multiplex message channels.


## Usage
You can either use the precompiled JAR file (for language level 8) from the [out](out) directory, or you can compile the source code yourself. The code was written in IntelliJ IDEA Community Edition.

The class with all the important methods is called `MsgESS`. All classes' public APIs are documented using Javadoc.

Usage example can be found in the [src/test](src/test) directory.


## Other versions
The library is also available for these programming languages, with the network protocol being interoperable between them:
* [Python 3](https://github.com/vitlabuda/msgess-python)


## Licensing
This project is licensed under the 3-clause BSD license. See the [LICENSE](LICENSE) file for details.

Written by [Vít Labuda](https://vitlabuda.cz/).
