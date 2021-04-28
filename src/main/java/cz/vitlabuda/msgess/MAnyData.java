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

import java.io.Serializable;

/**
 * Wrapper class around received or sent data, containing the data themselves, their data type and the message class.
 */
public class MAnyData extends Message implements Serializable {
    public enum DataType {
        BINARY,
        STRING,
        JSON_ARRAY,
        JSON_OBJECT
    }

    private final DataType dataType;
    private final Object data;

    /**
     * Initializes a new MAnyData instance.
     *
     * @param dataType The carried data's type.
     * @param data The data to carry.
     * @param messageClass The message class to carry.
     */
    public MAnyData(DataType dataType, Object data, int messageClass) {
        super(messageClass);
        this.dataType = dataType;
        this.data = data;
    }

    /**
     * Gets the carried data's type passed to the constructor.
     *
     * @return The carried data's type passed to the constructor.
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Gets the data passed to the constructor. The return value should be cast according to the data type.
     *
     * @return The data passed to the constructor. Should be cast according to the data type.
     */
    public Object getData() {
        return data;
    }
}
