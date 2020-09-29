package java.lang;

public class SymbolDatabase implements com.sun.squawk.vm.SquawkOpcodes {

    private String name;
    private SymbolMember[][] members = new SymbolMember[4][];
    private boolean locked;

    public void lock() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    private void checkLock() {
        if (locked) {
            throw new Error("SymbolDatabase write lock error");
        }
    }

    public SymbolDatabase() {
        deleteAllMembers();
    }

    public SymbolDatabase(String name) {
        this();
        setName(name);
    }

    public void setMemberCount(int section, int count) {
        members[section] = new SymbolMember[count];
        for (int i = 0 ; i < count ; i++) {
            members[section][i] = new SymbolMember();
        }
    }

    public int getMemberCount(int section) {
        return members[section].length;
    }

    public void     setName(String name)                                            { checkLock(); this.name = name;                                }
    public void     setMemberAccess(int section, int member, int access)            { checkLock(); members[section][member].access = access;        }
    public void     setMemberOffset(int section, int member, int offset)            { checkLock(); members[section][member].offset = offset;        }
    public void     setMemberName(int section, int member, String name)             { checkLock(); members[section][member].name = name;            }
    public void     setMemberType(int section, int member, int type)                { checkLock(); members[section][member].type = type;            }
    public void     setMemberParmCount(int section, int member, int count)          { checkLock(); members[section][member].parms = new int[count]; }
    public void     setMemberParmType(int section, int member, int parm, int type)  { checkLock(); members[section][member].parms[parm] = type;     }

    public String   getName()                                                       { return name;                                                  }
    public int      getMemberAccess(int section, int member)                        { return members[section][member].access;                       }
    public int      getMemberOffset(int section, int member)                        { return members[section][member].offset;                       }
    public String   getMemberName(int section, int member)                          { return members[section][member].name;                         }
    public int      getMemberType(int section, int member)                          { return members[section][member].type;                         }
    public int      getMemberParmCount(int section, int member)                     { return members[section][member].parms.length;                 }
    public int      getMemberParmType(int section, int member, int parm)            { return members[section][member].parms[parm];                  }

    private void deleteMember(int section, int member) {
        checkLock();
        SymbolMember[] oldMembers = members[section];
        SymbolMember[] newMembers = new SymbolMember[oldMembers.length - 1];
        System.arraycopy(oldMembers, 0,          newMembers, 0,      member);
        System.arraycopy(oldMembers, member + 1, newMembers, member, newMembers.length - member);
        members[section] = newMembers;
    }

    public void deleteAllMembers() {
        members = new SymbolMember[4][];
        for (int i = 0 ; i < 4 ; i++) {
            members[i] = new SymbolMember[0];
        }
    }

    public void edit(SymbolDatabaseEditor editor) {
        for (int i = 0 ; i < 4 ; i++) {
            for (int j = 0 ; j < getMemberCount(i) ; j++) {
                if (editor.editMember(this, i, j) == false) {
                    deleteMember(i, j);
                    --j;
                }
            }
        }
    }

    Symbol getSymbol(int section, int member) {
        return new Symbol(this, section, member);
    }

    void freeSymbol(Symbol symbol) {
    }

}

class SymbolMember {
    int access;
    int offset;
    String name;
    int type;
    int[] parms;
}
