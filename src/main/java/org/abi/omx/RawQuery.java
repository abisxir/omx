package org.abi.omx;

import android.database.Cursor;

/**
 * Created by abi on 21.05.15.
 */
public abstract class RawQuery<T> {

    private Class<T> cls;
    private Schema schema;

    protected RawQuery(Schema schema, Class<T> cls) {
        this.cls = cls;
        this.schema = schema;
    }

    protected Schema getSchema() {
        return schema;
    }

    protected Class<T> getResultClass() {
        return cls;
    }

    protected abstract String compile();

    protected abstract Iterable<Object> getValues();

    public long count() throws OmxException {
        String statment = this.compile();
        Iterable<Object> values = this.getValues();
        String sql = String.format("SELECT count(*) FROM (%s)", statment);
        Cursor cursor =
                this.getSchema().executeSQL(sql, values);
        try {
            Long result = Mapper.getInstance().mapOne(cursor, Long.class);
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public T getOne() throws OmxException {
        Cursor cursor = this.run();
        try {
            T result = Mapper.getInstance().mapOne(cursor, this.getResultClass());
            return result;
        } finally {
            if (cursor == null) {
                cursor.close();
            }
        }
    }

    public QueryResult<T> getMany() throws OmxException {
        Cursor cursor = this.run();
        try {
            return Mapper.getInstance().mapMany(cursor, this.getResultClass());
        } finally {
            if (cursor == null) {
                cursor.close();
            }
        }
    }

    public Cursor run() {
        return this.getSchema().executeSQL(this.compile(), this.getValues());
    }

}
