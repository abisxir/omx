package org.abi.omx;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by abi on 04.05.15.
 */
public abstract class Schema {
    private String name;
    private SQLiteDatabase rawDatabase;

    public Schema(Context context, String name) {
        if (!name.endsWith(".db")) {
            name = name + ".db";
        }
        this.name = name;
        File file = context.getDatabasePath(name);
        debug(String.format("Creating database in [%s] ...", file.getPath()));
        this.rawDatabase = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
        debug("Database created.");
    }

    public void createTable(Class<? extends Entity> cls) throws OmxException {
        this.createTable(cls, null);
    }

    public void createTable(Class<? extends Entity> cls, Entity.Factory factory) throws OmxException {
        if (factory != null) {
            ReflectionHelper.getInstance().setFactrory(cls, factory);
        }

        String ddl = new TableCreationQueryBuilder(cls).compile();
        try {
            this.executeDDL(ddl);
        } catch (SQLException e) {
            throw new OmxException(e);
        }
    }

    public void dropTable(Class<? extends Entity> cls) throws OmxException {
        TableDescriptor descriptor = ReflectionHelper.getInstance().probeTable(cls);
        this.executeDDL("DROP TABLE IF EXISTS " + descriptor.name);
    }

    public SQLiteDatabase getRawDatabase() {
        return this.rawDatabase;
    }

    private String getTag() {
        return String.format("Database[%s]", name);
    }

    private void debug(String message) {
        Log.d(this.getTag(), message);
    }

    public <T extends Entity> Store getStore(Class<T> cls) throws OmxException {
        return new DefaultStore<T>(this, cls);
    }

    public void executeDDL(String ddl) {
        debug(ddl);
        this.rawDatabase.execSQL(ddl);
    }

    public Cursor executeSQL(String sql, Iterable<Object> args) {
        long start = System.nanoTime();
        List<String> strings = new ArrayList<String>();
        for (Object value : args) {
            if (value != null) {
                strings.add(value.toString());
            } else {
                strings.add(null);
            }
        }
        String[] values = new String[strings.size()];
        strings.toArray(values);
        Cursor cursor = this.rawDatabase.rawQuery(sql, values);
        double elapsed = (System.nanoTime() - start) / 1000000000.0;
        debug(String.format("->\n%s\n\t%s\n\t(Elapsed time:[%fs])", sql, args, elapsed));
        return cursor;
    }

    public Transaction begin() {
        return new DefaultTransaction(this.getRawDatabase());
    }

    public <T> Query<T> query(Class<T> cls) {
        return new DefaultQuery<T>(this, cls);
    }

    public <T> RawQuery<T> rawQuery(Class<T> cls, String sql, Object... args) {
        return new DefaultRawQuery<T>(this, cls, sql, args);
    }
}

class DefaultTransaction extends Transaction {
    public DefaultTransaction(SQLiteDatabase db) {
        super(db);
    }
}

class DefaultStore<T extends Entity> extends Store<T> {
    private Schema _schema;
    private Class<? extends Entity> _entityClass;
    private TableDescriptor _tableDescriptor;

    public DefaultStore(Schema schema, Class<T> cls) throws OmxException {
        this._schema = schema;
        this._entityClass = cls;
        this._tableDescriptor = ReflectionHelper.getInstance().probeTable(cls);
    }

    @Override
    protected Schema getSchema() {
        return this._schema;
    }

    @Override
    protected Class<? extends Entity> getEntityClass() {
        return this._entityClass;
    }

    @Override
    protected TableDescriptor getTableDescriptor() {
        return this._tableDescriptor;
    }

}

class DefaultRawQuery<T> extends RawQuery<T> {

    private String sql;
    private List<Object> args;

    public DefaultRawQuery(Schema schema, Class<T> cls, String sql, Object[] args) {
        super(schema, cls);
        this.args = new ArrayList<Object>();
        for (Object v : args) {
            this.args.add(v);
        }
    }

    @Override
    protected String compile() {
        return sql;
    }

    @Override
    protected Iterable<Object> getValues() {
        return args;
    }

}

class DefaultQuery<T> extends Query<T> {
    protected DefaultQuery(Schema schema, Class<T> cls) {
        super(schema, cls);
    }
}