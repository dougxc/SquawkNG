package java.lang;

public class Symbol {

    private SymbolDatabase database;
    private int section;
    private int member;

    public Symbol(SymbolDatabase database, int section, int member) {
        this.database = database;
        this.section  = section;
        this.member   = member;
    }

    public void    setAccess(int access)                    { database.setMemberAccess(section, member, access);        }
    public void    setOffset(int offset)                    { database.setMemberOffset(section, member, offset);        }
    public void    setName(String name)                     { database.setMemberName(section, member, name);            }
    public void    setType(int type)                        { database.setMemberType(section, member, type);            }
    public void    setParmType(int parm, int type)          { database.setMemberParmType(section, member, parm, type);  }

    public int     getAccess()                              { return database.getMemberAccess(section, member);         }
    public int     getMemberOffset()                        { return database.getMemberOffset(section, member);         }
    public String  getName()                                { return database.getMemberName(section, member);           }
    public int     getType(int section, int member)         { return database.getMemberType(section, member);           }
    public int     getParmCount(int section, int member)    { return database.getMemberParmCount(section, member);      }
    public int     getParmType(int parm)                    { return database.getMemberParmType(section, member, parm); }

    public void free() {
        SymbolDatabase d = database;
        database = null;
        d.freeSymbol(this);
    }
}

