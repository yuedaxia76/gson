/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gson;

import java.io.IOException;
import java.math.BigDecimal;

import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.internal.Primitives;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import java.util.regex.Pattern;

/**
 * An enumeration that defines two standard number reading strategies and a
 * couple of number strategies to overcome some historical Gson limitations
 * while deserializing numbers as {@link Object} and {@link Number}.
 *
 * @see ToNumberStrategy
 */
public enum ToNumberPolicy implements ToNumberStrategy {

    /**
     * Using this policy will ensure that numbers will be read as {@link Double}
     * values. This is the default strategy used during deserialization numbers
     * as {@link Object} using
     * {@link com.google.gson.internal.bind.ObjectTypeAdapter} in earlier
     * versions of Gson.
     */
    DOUBLE {
        @Override
        public Double toNumber(JsonReader in) throws IOException {
            return in.nextDouble();
        }
    },
    /**
     * Using this policy will ensure that numbers will be read as
     * {@link LazilyParsedNumber} values. This is the default strategy using
     * during deserialization numbers as {@link Number} using
     * {@link com.google.gson.internal.bind.TypeAdapters#NUMBER}.
     */
    LAZILY_PARSED_NUMBER {
        @Override
        public LazilyParsedNumber toNumber(JsonReader in) throws IOException {
            return new LazilyParsedNumber(in.nextString());
        }
    },
    /**
     * Using this policy will ensure that numbers will be read as {@link Long}
     * or {@link Double} values depending on how JSON numbers are represented:
     * {@link Long} if the JSON number can fit the {@link Long} range, or
     * {@link Double} if the JSON number can fit the {@link Double} range and
     * the value cannot be read as {@link Long}. If the parsed double-precision
     * number results in a positive or negative infinity
     * ({@link Double#isInfinite()}) or a NaN ({@link Double#isNaN()}) value,
     * {@link MalformedJsonException} is thrown.
     */
    LONG_OR_DOUBLE {
        @Override
        public Number toNumber(JsonReader in) throws IOException, JsonParseException {
            final String value = in.nextString();

            if (Primitives.isLong(value)) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException longEx) {
                    return toDouble(value, in);
                }
            } else {
                return toDouble(value, in);
            }

        }

        Number toDouble(final String value, JsonReader in) throws MalformedJsonException {
            try {
                final Double d = Double.valueOf(value);
                if (d.isInfinite() || d.isNaN()) {
                    throw new MalformedJsonException("JSON forbids NaN and infinities: " + d + in);
                }
                return d;
            } catch (NumberFormatException doubleEx) {
                throw new JsonParseException("Cannot parse " + value);
            }
        }
    },
    /**
     * Using this policy will ensure that numbers will be read as numbers of
     * arbitrary length using {@link BigDecimal}.
     */
    BIG_DECIMAL {
        @Override
        public BigDecimal toNumber(JsonReader in) throws IOException {
            return new BigDecimal(in.nextString());
        }
    }

}
