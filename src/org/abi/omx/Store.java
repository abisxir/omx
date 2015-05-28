package org.abi.omx;


import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by abi on 06.05.15.
 */
public abstract class Store<T extends Entity> {

    protected abstract Schema getSchema();

    protected abstract Class<? extends Entity> getEntityClass();

    protected abstract TableDescriptor getTableDescriptor();

    public void create(T entity) throws OmxException {
        Object[] values =
                ReflectionHelper.getInstance().getValues(this.getTableDescriptor(), entity);
        this.getSchema().getRawDatabase().execSQL(this.getTableDescriptor().insertSQL, values);
    }

    public void remove(T entity) throws OmxException {
        TableDescriptor tableDescriptor = this.getTableDescriptor();
        Object[] pks = this.getPKs(entity);

        this.getSchema().getRawDatabase().delete(tableDescriptor.name,
                tableDescriptor.getWhereClause,
                Utils.objectArrayToStringArray(pks));
    }

    public void update(T entity) throws OmxException {
        try {
            TableDescriptor tableDescriptor = this.getTableDescriptor();
            ContentValues values = new ContentValues();
            for (FieldDescriptor fieldDescriptor : tableDescriptor.fieldDescriptors) {
                if (fieldDescriptor.pk == null) {
                    values.put(fieldDescriptor.name, fieldDescriptor.field.get(entity).toString());
                }
            }
            this.getSchema().getRawDatabase().update(tableDescriptor.name,
                    values,
                    tableDescriptor.getWhereClause,
                    Utils.objectArrayToStringArray(this.getPKs(entity)));
        } catch (IllegalAccessException e) {
            throw new OmxException(e);
        }
    }

    public void save(T entity) throws OmxException {
        Object[] pks = this.getPKs(entity);
        T old = this.get(pks);
        if (old == null) {
            this.create(entity);
        } else {
            this.update(entity);
        }
    }

    private Object[] getPKs(T entity) throws OmxException {
        try {
            List<Object> pks = new ArrayList<Object>();
            for (FieldDescriptor fieldDescriptor : getTableDescriptor().fieldDescriptors) {
                if (fieldDescriptor.pk != null) {
                    pks.add(fieldDescriptor.field.get(entity));
                }
            }
            return pks.toArray();
        } catch (IllegalAccessException e) {
            throw new OmxException(e);
        }
    }

    public T get(Object... args) throws OmxException {
        List<Object> values = new ArrayList<Object>();
        for (Object value : args) {
            values.add(value);
        }
        Cursor cursor =
                this.getSchema().executeSQL(this.getTableDescriptor().getSQL, values);
        try {
            if (cursor == null || cursor.getCount() == 0) {
                return null;
            }
            if (cursor.getCount() > 1) {
                throw new OmxException(String.format("Entity [%s] : there is more than one result to return [%d].",
                        this.getTableDescriptor().name,
                        cursor.getCount()));
            }

            return (T) Mapper.getInstance().mapOne(cursor, this.getEntityClass());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public QueryResult<T> find() throws OmxException {
        return this.find(null);
    }

    public QueryResult<T> find(String where, Object... args) throws OmxException {
        RawQuery<T> query =
                this.query().select("*").from(this.getTableDescriptor().name).where(where, args);
        return query.getMany();
    }

    public Query<T> query() {
        return (Query<T>)this.getSchema().query(this.getEntityClass());
    }

    public Transaction begin() {
        return this.getSchema().begin();
    }

}

