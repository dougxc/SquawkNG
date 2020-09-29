package com.sun.squawk.io.connections;

import java.io.*;
import javax.microedition.io.*;

public interface ClasspathConnection extends Connection {

    public InputStream openInputStream(String name) throws IOException;

}

