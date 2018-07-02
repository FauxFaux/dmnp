package com.goeswhere.dmnp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegexpTrieTest {
    @Test
    void testSimple() {
        assertEquals("(?:badger|pony)", new RegexpTrie().add("pony").add("badger").toString());
        assertEquals("p(?:ony|wny)", new RegexpTrie().add("pony").add("pwny").toString());
        assertEquals("omg (?:horses|ponies)", new RegexpTrie().add("omg ponies").add("omg horses").toString());
    }
}
