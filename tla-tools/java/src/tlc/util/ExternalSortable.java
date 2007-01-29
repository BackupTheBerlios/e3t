// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlc.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ExternalSortable extends Comparable {
  public BigInt read(InputStream in) throws IOException;
  public void write(OutputStream out) throws IOException;
}

