package com.goeswhere.dmnp.util;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * A literal translation of Dan Kogai's Regexp-Trie-0.02, from Perl (what was I thinking).
 *
 * <p>Does <b>not</b> cope with special characters.
 */
public class RegexpTrie {

    private static class CMap extends TreeMap<Character, CMap> {
        // typedef
        private final static CMap NULLARY = new CMap() {{
            put('\0', null);
        }};
    }

    private final CMap mymap = new CMap();

    public RegexpTrie add(String string) {
        CMap ref = mymap;

        for (char c : string.toCharArray()) {
            if (!ref.containsKey(c))
                ref.put(c, new CMap());
            ref = ref.get(c);
        }
        ref.put('\0', null); // { '' => 1 } as terminator
        return this;
    }

    private static String buildRegexp(CMap from) {
        if (from.equals(CMap.NULLARY)) // # terminator
            return "";

        final List<String> aalt = new ArrayList<>(), acc = new ArrayList<>();
        boolean $q = false;
        for (char c : from.keySet()) {
            final char quoted = quotemeta(c);
            final CMap get = from.get(c);
            if (null != get) {
                final String inner = buildRegexp(get);
                final String quoteds = String.valueOf(quoted);

                if (null != inner) {
                    aalt.add(quoteds + inner);
                } else {
                    acc.add(quoteds);
                }
            } else {
                $q = true;
            }
        }
        boolean $cconly = aalt.isEmpty();
        if (!acc.isEmpty())
            aalt.add(acc.size() == 1 ? acc.get(0) : "[" + join("", acc) + "]");
        String ret = aalt.size() == 1 ? aalt.get(0) : "(?:" + join("|", aalt) + ")";
        if ($q)
            ret = $cconly ? ret + "?" : "(?:" + ret + ")?";
        return ret;
    }

    @Override
    public String toString() {
        String $str = buildRegexp(mymap);
        return qr($str);
    }

    private static String join(String string, Iterable<?> acc) {
        return Joiner.on(string).join(acc);
    }

    private static String qr(String $str) {
        return $str;
    }

    private static char quotemeta(char $char) {
        return $char;
    }
}
