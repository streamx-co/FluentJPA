package co.streamx.fluent.JPA;

interface Strings {

    static boolean isNullOrEmpty(CharSequence seq) {
        return seq == null
                || (seq instanceof UnboundCharSequence ? ((UnboundCharSequence) seq).isEmpty() : seq.length() == 0);
    }

    static boolean equals(CharSequence source,
                          CharSequence prefix) {
        if (source == null) {
            return prefix == null;
        } else if (prefix == null)
            return false;

        return compare(source, 0, prefix, 0, source.length()) == 0;
    }

    static boolean startsWith(CharSequence left,
                              CharSequence right) {
        int length = right.length();
        if (length > left.length())
            return false;
        return compare(left, 0, right, 0, length) == 0;
    }

    static int compare(CharSequence lseq,
                       int lstart,
                       CharSequence rseq,
                       int rstart,
                       int length) {
        for (int i = 0; i < length; i++) {
            char l = lseq.charAt(lstart + i);
            char r = rseq.charAt(rstart + i);
            if (l != r)
                return l - r;
        }

        return 0;
    }

    static int lastIndexOf(CharSequence source,
                           char ch) {
        for (int i = source.length() - 1; i >= 0; i--) {
            if (source.charAt(i) == ch) {
                return i;
            }
        }
        return -1;
    }

    static int indexOf(CharSequence source,
                       char ch) {
        return indexOf(source, ch, 0);
    }

    static int indexOf(CharSequence source,
                       char ch,
                       int start) {
        for (int i = start; i < source.length(); i++) {
            if (source.charAt(i) == ch) {
                return i;
            }
        }
        return -1;
    }

    static CharSequence trim(CharSequence source) {
        if (source == null)
            return source;
        int len = source.length();
        if (len == 0)
            return source;
        int start = 0;
        while (start < len && (source.charAt(start) <= ' '))
            start++;
        while (len > start && (source.charAt(--len) <= ' '))
            ;

        return source.subSequence(start, ++len);
    }
}
