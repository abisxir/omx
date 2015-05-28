package org.abi.omx;

import android.database.Cursor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abi on 20.05.15.
 */
public class Mapper {
    private static Mapper instance = new Mapper();
    private Map<Class<?>, TypeResolver<?>> types;

    private Mapper() {
        this.types = new HashMap<Class<?>, TypeResolver<?>>();

        // Handling Strings
        this.registerTypeResolver(String.class, new TypeResolver<String>() {
            @Override
            public String getSQLType() {
                return "TEXT";
            }

            @Override
            public String fromSQL(Cursor cursor, int index) {
                return cursor.getString(index);
            }

            @Override
            public Object toSQL(String value) {
                return value;
            }
        });

        // Handling bytes
        this.registerTypeResolver(Byte.class, new ByteTypeResolver());
        this.registerTypeResolver(byte.class, new ByteTypeResolver());

        // Handling shorts
        this.registerTypeResolver(Short.class, new ShortTypeResolver());
        this.registerTypeResolver(short.class, new ShortTypeResolver());

        // Handling ints
        this.registerTypeResolver(Integer.class, new IntegerTypeResolver());
        this.registerTypeResolver(int.class, new IntegerTypeResolver());

        // Handling longs
        this.registerTypeResolver(Long.class, new LongTypeResolver());
        this.registerTypeResolver(long.class, new LongTypeResolver());

        // Handling doubles
        this.registerTypeResolver(Double.class, new DoubleTypeResolver());
        this.registerTypeResolver(double.class, new DoubleTypeResolver());

        // Handling floats
        this.registerTypeResolver(Float.class, new FloatTypeResolver());
        this.registerTypeResolver(float.class, new FloatTypeResolver());

        // Handling byte arrays
        this.registerTypeResolver(byte[].class, new TypeResolver<byte[]>() {
            @Override
            public String getSQLType() {
                return "BINARY";
            }

            @Override
            public byte[] fromSQL(Cursor cursor, int index) {
                return cursor.getBlob(index);
            }

            @Override
            public Object toSQL(byte[] value) {
                return value;
            }
        });

        this.registerTypeResolver(ByteBuffer.class, new TypeResolver<ByteBuffer>() {
            @Override
            public String getSQLType() {
                return "BINARY";
            }

            @Override
            public ByteBuffer fromSQL(Cursor cursor, int index) {
                byte[] buffer = cursor.getBlob(index);
                if (buffer == null) {
                    return null;
                }
                return ByteBuffer.wrap(buffer);
            }

            @Override
            public Object toSQL(ByteBuffer value) {
                return value.array();
            }
        });

    }

    public static Mapper getInstance() {
        return instance;
    }

    public void registerTypeResolver(Class<?> cls, TypeResolver<?> resolver) {
        this.types.put(cls, resolver);
    }

    public <T> TypeResolver<T> getTypeResolver(Class<T> cls) throws OmxException {
        TypeResolver<?> resolver = this.types.get(cls);
        if (resolver == null) {
            throw new OmxException(String.format("Can find a resolver for class [%s]", cls.getName()));
        }
        return (TypeResolver<T>) resolver;
    }

    public <T> T mapOne(Cursor cursor, Class<T> cls) throws OmxException {
        if (cursor.getCount() == 0) {
            return null;
        }

        if (cursor.getCount() > 1) {
            throw new OmxException("More than one row to fetch.");
        }
        cursor.moveToFirst();
        return this.mapTo(cursor, cls);
    }

    private  <T> T mapTo(Cursor cursor, Class<T> cls) throws OmxException {
        if (this.types.containsKey(cls)) {
            if (cursor.getColumnCount() > 1) {
                throw new OmxException("More than one column to extract.");
            }
            TypeResolver<T> resolver = this.getTypeResolver(cls);
            return resolver.fromSQL(cursor, 0);
        }
        if (Entity.class.isAssignableFrom(cls)) {
            Class<? extends Entity> entityClass = (Class<? extends Entity>) cls;
            TableDescriptor tableDescriptor = ReflectionHelper.getInstance().probeTable(entityClass);
            return (T) this.map(cursor, entityClass, tableDescriptor);
        }

        throw new OmxException(String.format("Cannot map type [%s].", cls.getName()));
    }

    public <T> QueryResult<T> mapMany(Cursor cursor, Class<T> cls) throws OmxException {
        DefaultQueryResult<T> results = new DefaultQueryResult<T>();
        cursor.moveToFirst();
        for (int pos = 0; pos < cursor.getCount(); pos++) {
            results.add(this.mapTo(cursor, cls));
            cursor.moveToNext();
        }
        return results;
    }

    public List<List<Map.Entry<String, Object>>> mapMany(Cursor cursor) throws OmxException {
        ArrayList<List<Map.Entry<String, Object>>> results = new ArrayList();
        for (int pos = 0; pos < cursor.getCount(); pos++) {
            cursor.moveToPosition(pos);
            List<Map.Entry<String, Object>> entries = new ArrayList();
            for (int index = 0; index < cursor.getColumnCount(); index++) {
                String key = cursor.getColumnName(index);
                Object value = this.getColumnValue(cursor, index);
                entries.add(new DefaultEntry(key, value));
            }
            results.add(entries);
        }
        return results;
    }

    private Object getColumnValue(Cursor cursor, int index) {
        switch (cursor.getType(index)) {
            case Cursor.FIELD_TYPE_BLOB:
                return cursor.getBlob(index);
            case Cursor.FIELD_TYPE_FLOAT:
                return cursor.getDouble(index);
            case Cursor.FIELD_TYPE_INTEGER:
                return cursor.getInt(index);
            case Cursor.FIELD_TYPE_STRING:
                return cursor.getString(index);
        }
        return null;
    }

    private <T> T map(Cursor cursor, Class<T> cls, TableDescriptor tableDescriptor) throws OmxException {
        try {
            T entity = cls.newInstance();
            for (FieldDescriptor fieldDescriptor : tableDescriptor.fieldDescriptors) {
                int index = cursor.getColumnIndex(fieldDescriptor.name);
                //System.out.println(String.format("%s:%d", fieldDescriptor.name, index));
                if (index >= 0) {
                    Object value = this.getTypeResolver(fieldDescriptor.type).fromSQL(cursor, index);
                    fieldDescriptor.field.set(entity, value);
                }
            }
            return entity;
        } catch (IllegalAccessException e) {
            throw new OmxException(e);
        } catch (InstantiationException e) {
            throw new OmxException(e);
        }
    }

    public interface TypeResolver<T> {
        String getSQLType();

        T fromSQL(Cursor cursor, int index);

        Object toSQL(T value);
    }

}

class ByteTypeResolver implements Mapper.TypeResolver<Byte> {
    @Override
    public String getSQLType() {
        return "INTEGER";
    }

    @Override
    public Byte fromSQL(Cursor cursor, int index) {
        return (byte) cursor.getShort(index);
    }

    @Override
    public Object toSQL(Byte value) {
        return value;
    }
}

class ShortTypeResolver implements Mapper.TypeResolver<Short> {
    @Override
    public String getSQLType() {
        return "INTEGER";
    }

    @Override
    public Short fromSQL(Cursor cursor, int index) {
        return cursor.getShort(index);
    }

    @Override
    public Object toSQL(Short value) {
        return value;
    }
}

class IntegerTypeResolver implements Mapper.TypeResolver<Integer> {
    @Override
    public String getSQLType() {
        return "INTEGER";
    }

    @Override
    public Integer fromSQL(Cursor cursor, int index) {
        return cursor.getInt(index);
    }

    @Override
    public Object toSQL(Integer value) {
        return value;
    }
}

class LongTypeResolver implements Mapper.TypeResolver<Long> {
    @Override
    public String getSQLType() {
        return "NUMERIC";
    }

    @Override
    public Long fromSQL(Cursor cursor, int index) {
        return cursor.getLong(index);
    }

    @Override
    public Object toSQL(Long value) {
        return value;
    }
}

class DoubleTypeResolver implements Mapper.TypeResolver<Double> {
    @Override
    public String getSQLType() {
        return "REAL";
    }

    @Override
    public Double fromSQL(Cursor cursor, int index) {
        return cursor.getDouble(index);
    }

    @Override
    public Object toSQL(Double value) {
        return value;
    }
}

class FloatTypeResolver implements Mapper.TypeResolver<Float> {
    @Override
    public String getSQLType() {
        return "REAL";
    }

    @Override
    public Float fromSQL(Cursor cursor, int index) {
        return ((Double) cursor.getDouble(index)).floatValue();
    }

    @Override
    public Object toSQL(Float value) {
        return value.doubleValue();
    }
}

class DefaultQueryResult<T> extends ArrayList<T> implements QueryResult<T> {
    @Override
    public int count() {
        return this.size();
    }
}

class DefaultEntry implements Map.Entry<String, Object> {
    private String key;
    private Object value;

    public DefaultEntry(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Object setValue(Object newValue) {
        Object old = this.value;
        this.value = newValue;
        return old;
    }

}