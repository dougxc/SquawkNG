package javax.microedition.rms;

public class RecordEnumeration {

    public RecordEnumeration() {
        throw new Error();
    }

    public boolean hasNextElement() { return false; }

    public int nextRecordId() throws Error {
        throw new Error();
    }


/*
    public int numRecords() { return 0; }

    public byte[] nextRecord() {
        throw new RecordStoreNotOpenException();
    }

    public int nextRecordId() throws InvalidRecordIDException;

    public byte[] previousRecord() throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException;

    public int previousRecordId() throws InvalidRecordIDException;

    public boolean hasNextElement();
    public boolean hasPreviousElement();
    public void reset();
    public void rebuild();
    public void keepUpdated(boolean keepUpdated);
    public boolean isKeptUpdated();
    public void destroy();
*/
}
