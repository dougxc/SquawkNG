package javax.microedition.rms;

public class RecordStore {

    public static RecordStore openRecordStore(String name, boolean b) throws RecordStoreException {
        return null;
    }

    public RecordEnumeration enumerateRecords(Object x, Object y, boolean b) throws RecordStoreException {
        throw new Error();
    }

    public int getRecordSize(int ndx) {
        throw new Error();
    }

    public int getNumRecords() {
        throw new Error();
    }

    public int getNextRecordID() {
        throw new Error();
    }

    public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException {
        throw new Error();
    }

    public int getRecord(int recordId, byte[] buffer, int offset) {
        throw new Error();
    }

    public byte[] getRecord(int recordId) {
        throw new Error();
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws RecordStoreException {
        throw new Error();
    }

    public void closeRecordStore() throws RecordStoreException {
        throw new Error();
    }

}