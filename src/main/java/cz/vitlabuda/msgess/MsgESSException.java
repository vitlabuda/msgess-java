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

/**
 * The only exception thrown by the MsgESS class.
 */
public class MsgESSException extends Exception {
    private final Exception originalException;

    /**
     * Initializes a new MsgESSException instance carrying another exception. This constructor should be used when
     * wrapping another exception with this class.
     *
     * @param message The error message.
     * @param originalException The exception to carry.
     */
    public MsgESSException(String message, Exception originalException) {
        super(message);
        this.originalException = originalException;
    }

    /**
     * Initializes a new MsgESSException instance.
     *
     * @param message The error message.
     */
    public MsgESSException(String message) {
        super(message);
        this.originalException = null;
    }

    /**
     * Gets the exception that is carried by the instance. Returns null if no other exception is carried.
     *
     * @return The carried exception or null, if no exception is carried.
     */
    public Exception getOriginalException() {
        return originalException;
    }
}
