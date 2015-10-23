package org.abi.omx;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by abi on 05.05.15.
 */
public abstract class Transaction {
    private boolean rolledBack;
    private boolean committed;
    private boolean isTransactionStarted;
    private SQLiteDatabase db;

    public Transaction(SQLiteDatabase db) {
        this.db = db;
        this.isTransactionStarted = false;
        this.committed = false;
        this.rolledBack = false;
        if (!db.inTransaction()) {
            db.beginTransaction();
        }
    }

    public void commit() {
        if (this.isTransactionStarted) {
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
            this.committed = true;
        }
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public boolean isRolledBack() {
        return this.rolledBack;
    }

    public void rollback() {
        if (this.isTransactionStarted) {
            this.rolledBack = true;
            this.db.endTransaction();
        }
    }
}
