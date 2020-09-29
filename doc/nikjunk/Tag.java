import java.io.*;
import java.util.*;

/* ------------------------------------------------------------------------ *\
 *                                   Tag                                    *
\* ------------------------------------------------------------------------ */

public abstract class Tag {

    /*
     * read
     */
    private static int read(InputStream in) throws IOException {
        int ch = in.read();
//System.out.print((char)ch);
        return ch;
    }

    /*
     * assume
     */
    protected static void assume(boolean b) {
       if (!b) {
           throw new RuntimeException("Assume failure");
       }
    }

    /*
     * assume
     */
    protected static void assume(boolean b, String msg) {
       if (!b) {
           throw new RuntimeException("Assume failure: "+msg);
       }
    }

    /*
     * create
     */
    public static Tag create(InputStream in) throws IOException {
        return create(in, null);
    }

    /*
     * create
     */
    private static Tag create(InputStream in, String callersTag) throws IOException {
        return create(in, callersTag, read(in));
    }

    /*
     * create
     */
    private static Tag create(InputStream in, String callersTag, int ch) throws IOException {

        StringBuffer sb     = new StringBuffer();
        boolean hasContents = true;
        boolean isEndTag    = false;

        for (;;) {

           /*
            * Loose any white space
            */
            while (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                ch = read(in);
            }

           /*
            * Return null at EOF
            */
            if (ch == -1) {
                assume(callersTag == null);
                return null;
            }

           /*
            * If the first character is not a '<' then this must be data of some kind
            */
            if (ch != '<') {
                while (ch != '<') {
                    sb.append((char)ch);
                    ch = read(in);
                }
                assume(Tag.create(in, callersTag, '<') == null);
                return new DataTag(sb.toString());
            }

           /*
            * Get the first character of the tag name
            */
            ch = read(in);

           /*
            * If it is a '!' then this is a comment
            */
            if (ch == '!') {
                while (ch != '>') {
                    ch = read(in);
                }
                ch = read(in);
                continue;
            }

            break;
        }

       /*
        * If it is a '/' then this is an end tag
        */
        if (ch == '/') {
            isEndTag = true;
            ch = read(in);
        }

       /*
        * Read to the end of the tag name
        */
        while (ch != '>') {
            assume(ch != -1);

           /*
            * If this is a '/' then it may be the last character of a tag In this case this
            * a simple tag with no end pair. Otherwise it may be a '/' in a keyword as in
            * <suite xmlns="http://www.sun.com/squawk/version/1.1">
            */
            if (ch == '/') {
                ch = read(in);
                if (ch == '>') {
                    hasContents = false;
                    break;
                }
                sb.append('/');
            }
            sb.append((char)ch);
            ch = read(in);
        }

       /*
        * Turn it into a string and check the length
        */
        String name = sb.toString();
        assume(name.length() > 0);

       /*
        * If it is an end tag then check the name and return null
        */
        if (isEndTag) {
            assume(hasContents); // i.e is </foo> not </foo/>
            assume(name.equals(callersTag), name+" != "+callersTag);
            return null;
        }

       /*
        * Split the tag name from any keywords
        */
        int index = name.indexOf(' ');
        String keyw = "";
        if (index > 0) {
            keyw = name.substring(index);
            name = name.substring(0, index);
        }

       /*
        * Get the contence if there is any
        */
        if (hasContents) {
            return new ContentTag(name, keyw, Tag.create(in, name), Tag.create(in, callersTag));  // name, keywords, content, next
        } else {
            return new SimpleTag( name, keyw,                       Tag.create(in, callersTag));  // name, keywords, next
        }

    }

    /*
     * main
     */
    public static void main(String[] args) throws IOException {
        System.out.print("The graph is:" + Tag.create(System.in));
    }

    /*
     * toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        appendTo(sb, 0);
        sb.append('\n');
        return sb.toString();
    }

    /*
     * tab
     */
    protected void tab(StringBuffer sb, int level) {
        sb.append('\n');
        for (int i = 0 ; i < level ; i++) {
             sb.append("  ");
        }
    }

    /*
     * isName
     */
    public boolean isName(String name) {
        throw new RuntimeException("Invalid call");
    }

    /*
     * checkName
     */
    public void checkName(String name) {
        throw new RuntimeException("Invalid call");
    }

    /*
     * getBytecode
     */
    public int getBytecode() {
        throw new RuntimeException("Invalid call");
    }

    /*
     * getContent
     */
    public Tag getContent() {
        throw new RuntimeException("Invalid call");
    }

    /*
     * getNext
     */
    public Tag getNext() {
        throw new RuntimeException("Invalid call");
    }

    /*
     * getData
     */
    public String getData() {
        throw new RuntimeException("Invalid call");
    }

    /*
     * appendTo
     */
    public abstract void appendTo(StringBuffer sb, int level);


/* ------------------------------------------------------------------------ *\
 *                                 SimpleTag                                *
\* ------------------------------------------------------------------------ */

    private static class SimpleTag extends Tag {

        String name;
        String keyw;
        Tag    next;

        /*
         * SimpleTag
         */
        SimpleTag(String name, String keyw, Tag next) {
            this.name = name;
            this.keyw = keyw;
            this.next = next;
        }

        /*
         * isName
         */
        public boolean isName(String name) {
            return this.name.equals(name);
        }

        /*
         * checkName
         */
        public void checkName(String name) {
            assume(isName(name), "Expecting "+name+ " not "+this.name);
        }

        /*
         * getBytecode
         */
        public int getBytecode() {
            Integer i = (Integer)bytecodes.get(name);
            assume(i != null);
            return i.intValue();
        }


        /*
         * getNext
         */
        public Tag getNext() {
            return next;
        }

        /*
         * appendTo
         */
        public void appendTo(StringBuffer sb, int level) {
            tab(sb, level);
            sb.append('<');
            sb.append(name);
            sb.append(keyw);
            sb.append("/>");
            if (next != null) {
                next.appendTo(sb, level);
            }
        }
    }

/* ------------------------------------------------------------------------ *\
 *                                ContentTag                                *
\* ------------------------------------------------------------------------ */

    private static class ContentTag extends SimpleTag {

        Tag content;

        /*
         * ContentTag
         */
        ContentTag(String name, String keyw, Tag content, Tag next) {
            super(name, keyw, next);
            this.content = content;
        }

        /*
         * getContext
         */
        public Tag getContent() {
            return content;
        }

        /*
         * appendTo
         */
        public void appendTo(StringBuffer sb, int level) {
            tab(sb, level);
            sb.append('<');
            sb.append(name);
            sb.append(keyw);
            sb.append('>');
            if (content != null) {
                content.appendTo(sb, level+1);
            }
            tab(sb, level);
            sb.append("</");
            sb.append(name);
            sb.append('>');
            if (next != null) {
                next.appendTo(sb, level);
            }
        }

    }

/* ------------------------------------------------------------------------ *\
 *                                  DataTag                                 *
\* ------------------------------------------------------------------------ */

    private static class DataTag extends Tag {

        String data;

        /*
         * DataTag
         */
        DataTag(String data) {
            this.data = data;
        }

        /*
         * getData
         */
        public String getData() {
            return data;
        }

        /*
         * appendTo
         */
        public void appendTo(StringBuffer sb, int level) {
            tab(sb, level);
            sb.append(data);
        }
    }


/* ------------------------------------------------------------------------ *\
 *                                 Bytecodes                                *
\* ------------------------------------------------------------------------ */

    static Hashtable bytecodes = new Hashtable();

    static {
        bytecodes.put("load",     new Integer(1));
        bytecodes.put("iconst_3", new Integer(2));
        bytecodes.put("iadd",     new Integer(3));
        bytecodes.put("return",   new Integer(4));
    }

}