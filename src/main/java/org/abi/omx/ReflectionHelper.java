package org.abi.omx;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abi on 04.05.15.
 */
class ReflectionHelper {
    private static ReflectionHelper instance = new ReflectionHelper();
    private final Map<Class<? extends Entity>, TableDescriptor> classCache;
    //private Map<Class<?>, String> types;
    private Map<Class<? extends Entity>, Entity.Factory> factories;

    private ReflectionHelper() {
        this.classCache = new HashMap<Class<? extends Entity>, TableDescriptor>();
        this.factories = new HashMap<Class<? extends Entity>, Entity.Factory>();
    }

    public static ReflectionHelper getInstance() {
        return instance;
    }

    public void setFactrory(Class<? extends Entity> cls, Entity.Factory factory) {
        this.factories.put(cls, factory);
    }

    public Object[] getValues(TableDescriptor descriptor, Entity target) throws OmxException {
        List<Object> values = new ArrayList<Object>();
        for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptors) {
            try {
                values.add(fieldDescriptor.field.get(target));
            } catch (IllegalAccessException e) {
                throw new OmxException(e);
            }
        }
        return values.toArray();
    }

    public TableDescriptor probeTable(Class<? extends Entity> cls) throws OmxException {
        TableDescriptor descriptor = this.classCache.get(cls);
        if (descriptor == null) {
            synchronized (this.classCache) {
                descriptor = this.classCache.get(cls);
                if (descriptor == null) {
                    Table table = cls.getAnnotation(Table.class);
                    if (table == null) {
                        throw new OmxException(String.format("Entity [%s] has not @Table annotation.", cls.getName()));
                    }
                    descriptor = new TableDescriptor();
                    this.classCache.put(cls, descriptor);

                    descriptor.table = table;
                    descriptor.name = table.name();
                    if (descriptor.name.length() == 0) {
                        descriptor.name = cls.getName();
                    }
                    descriptor.fieldDescriptors = this.probeFields(cls);
                    descriptor.columns = this.createColumns(descriptor);
                    descriptor.insertSQL = this.createInsertSQL(descriptor);
                    descriptor.updateSQL = this.createUpdateSQL(descriptor);
                    descriptor.deleteSQL = this.createDeleteSQL(descriptor);
                    descriptor.getSQL = this.createGetSQL(descriptor);
                    descriptor.getWhereClause = this.createGetWhereClause(descriptor);
                }
            }
        }
        descriptor.factory = this.factories.get(cls);
        return descriptor;
    }

    private String createDeleteSQL(TableDescriptor descriptor) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("DELETE FROM %s ", descriptor.name));
        StringBuilder where = new StringBuilder();
        for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptors) {
            if (fieldDescriptor.pk != null) {
                if (where.toString().length() == 0) {
                    where.append(String.format(" %s=?", fieldDescriptor.name));
                } else {
                    where.append(String.format(", %s=?", fieldDescriptor.name));
                }
            }
        }
        builder.append(" WHERE ");
        builder.append(where);
        return builder.toString();
    }

    private String createUpdateSQL(TableDescriptor descriptor) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("UPDATE %s SET", descriptor.name));
        StringBuilder set = new StringBuilder();
        StringBuilder where = new StringBuilder();
        for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptors) {
            if (fieldDescriptor.pk == null) {
                if (set.toString().length() == 0) {
                    set.append(String.format(" %s=?", fieldDescriptor.name));
                } else {
                    set.append(String.format(", %s=?", fieldDescriptor.name));
                }
            } else {
                if (where.toString().length() == 0) {
                    where.append(String.format(" %s=?", fieldDescriptor.name));
                } else {
                    where.append(String.format(", %s=?", fieldDescriptor.name));
                }
            }
        }
        builder.append(set);
        builder.append(" WHERE ");
        builder.append(where);
        return builder.toString();
    }

    private String createGetSQL(TableDescriptor descriptor) {
        String getWhereClause = this.createGetWhereClause(descriptor);
        StringBuilder builder = new StringBuilder();
        for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptors) {
            if (builder.toString().length() == 0) {
                builder.append("SELECT ");
                builder.append(fieldDescriptor.name);
            } else {
                builder.append(", ");
                builder.append(fieldDescriptor.name);
            }
        }
        builder.append(" FROM ");
        builder.append(descriptor.name);
        builder.append(" \n WHERE ");

        builder.append(getWhereClause);
        return builder.toString();
    }

    private String[] createColumns(TableDescriptor descriptor) {
        List<String> columns = new ArrayList<String>();
        for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptors) {
            columns.add(fieldDescriptor.name);
        }
        String[] results = new String[columns.size()];
        columns.toArray(results);
        return results;
    }

    private String createGetWhereClause(TableDescriptor descriptor) {
        String conditions = "";
        for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptors) {
            if (fieldDescriptor.pk != null) {
                if (conditions.length() != 0) {
                    conditions += " and " + fieldDescriptor.name + " = ? ";
                } else {
                    conditions += fieldDescriptor.name + " = ? ";
                }
            }
        }
        return String.format("%s", conditions);
    }

    private String createInsertSQL(TableDescriptor descriptor) {
        String insertFields = "";
        String insertValues = "";
        for (FieldDescriptor fieldDescriptor : descriptor.fieldDescriptors) {
            if (insertFields.length() != 0) {
                insertFields += ", " + fieldDescriptor.name;
                insertValues += ", ?";
            } else {
                insertFields += fieldDescriptor.name;
                insertValues += "?";
            }
        }
        return String.format("INSERT INTO \n %s (%s) \n VALUES (%s);", descriptor.name, insertFields, insertValues);
    }

    private List<FieldDescriptor> probeFields(Class<? extends Entity> cls) throws OmxException {
        List<FieldDescriptor> fieldDescriptors = new ArrayList<FieldDescriptor>();

        for (Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                FieldDescriptor descriptor = new FieldDescriptor();
                descriptor.field = field;
                descriptor.name = field.getName();
                descriptor.column = column;
                if (descriptor.column.name().length() > 0) {
                    descriptor.name = descriptor.column.name();
                }
                descriptor.index = field.getAnnotation(Index.class);
                descriptor.pk = field.getAnnotation(PrimaryKey.class);
                descriptor.uniqueIndex = field.getAnnotation(UniqueIndex.class);
                descriptor.reference = field.getAnnotation(Reference.class);
                descriptor.type = field.getType();
                descriptor.sqlType = Mapper.getInstance().getTypeResolver(descriptor.type).getSQLType();
                if (descriptor.sqlType == null) {
                    throw new OmxException(String.format("Field [%s] of Entity [%s] with Type [%s] can not be mapped to SQL types.", descriptor.name, cls.getName(), descriptor.type.getName()));
                }
                fieldDescriptors.add(descriptor);
            }
        }

        if (fieldDescriptors.size() == 0) {
            throw new OmxException(String.format("Entity [%s] has not defined columns.", cls.getName()));
        }
        return fieldDescriptors;
    }

}

class ColumnDescriptor {
    public String name;
    public Field field;
    public Column column;
}

class FieldDescriptor extends ColumnDescriptor {
    public Index index;
    public UniqueIndex uniqueIndex;
    public Class<?> type;
    public String sqlType;
    public PrimaryKey pk;
    public Reference reference;
}

class ModelDescriptor {
    public String name;
    public List<FieldDescriptor> fieldDescriptors;
    public Entity.Factory factory;
}

class TableDescriptor extends ModelDescriptor {
    public Table table;
    public String insertSQL;
    public String updateSQL;
    public String deleteSQL;
    public String[] columns;
    public String getWhereClause;
    public String getSQL;
}
