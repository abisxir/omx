package org.abi.omx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abi on 04.05.15.
 */
class TableCreationQueryBuilder {
    private TableDescriptor tableDescriptor;
    private List<FieldDescriptor> fieldDescriptors;

    public TableCreationQueryBuilder(Class<? extends Entity> entityClass) throws OmxException {
        this.tableDescriptor = ReflectionHelper.getInstance().probeTable(entityClass);
        this.fieldDescriptors = this.tableDescriptor.fieldDescriptors;
    }

    public String compile() throws OmxException {
        String table = this.compileTable();
        String fields = this.compileFields();
        String primaryKeys = this.compilePrimaryKeys();
        String indexes = this.compileIndexes();
        return String.format("%s (%s %s); %s", table, fields, primaryKeys, indexes);
    }

    private String compileTable() {
        return String.format("CREATE TABLE IF NOT EXISTS %s", tableDescriptor.name);
    }

    private String compilePrimaryKeys() throws OmxException {
        String content = "";
        for (FieldDescriptor descriptor : fieldDescriptors) {
            if (descriptor.pk != null) {
                if (content.length() > 0) {
                    content = content + ", " + descriptor.name;
                } else {
                    content = descriptor.name;
                }
            }
        }
        if (content.length() == 0) {
            throw new OmxException(String.format("You must define a primary key for table [%s].", this.tableDescriptor.name));
        }
        return String.format("PRIMARY KEY (%s)", content);
    }

    private String compileIndexes() throws OmxException {
        Map<String, List<String>> indexes = new HashMap<String, List<String>>();
        Map<String, List<String>> unqiueIndexes = new HashMap<String, List<String>>();
        for (FieldDescriptor descriptor : fieldDescriptors) {
            if (descriptor.index != null) {
                String indexName = descriptor.index.name();
                List<String> fields = indexes.get(indexName);
                if (fields == null) {
                    fields = new ArrayList<String>();
                    indexes.put(indexName, fields);
                }
                String content = descriptor.name;
                if (descriptor.index.order() != null) {
                    content = content + " " + descriptor.index.order();
                }
                fields.add(content);
            }
            if (descriptor.uniqueIndex != null) {
                String indexName = descriptor.uniqueIndex.name();
                List<String> fields = unqiueIndexes.get(indexName);
                if (fields == null) {
                    fields = new ArrayList<String>();
                    unqiueIndexes.put(indexName, fields);
                }
                String content = descriptor.name;
                if (descriptor.uniqueIndex.order() != null) {
                    content = content + " " + descriptor.uniqueIndex.order();
                }
                fields.add(content);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : indexes.entrySet()) {
            /*
            Checking to ensure that there is no conflict with the unique indexes
             */
            if (unqiueIndexes.containsKey(entry.getKey())) {
                String message =
                        String.format("Entity [%s] has index [%s] which conflicts with the name of a unique index.",
                                tableDescriptor.name,
                                entry.getKey());
                throw new OmxException(message);
            }
            builder.append(String.format("\nCREATE INDEX %s ON %s ( ", entry.getKey(), tableDescriptor.name));
            String content = "";
            for (String item : entry.getValue()) {
                if (content.length() > 0) {
                    content = " ," + item;
                } else {
                    content = item;
                }
            }
            builder.append(content);
            builder.append(");\n");
        }
        for (Map.Entry<String, List<String>> entry : unqiueIndexes.entrySet()) {
            builder.append(String.format("\nCREATE UNIQUE INDEX %s ON %s ( ", entry.getKey(), tableDescriptor.name));
            String content = "";
            for (String item : entry.getValue()) {
                if (content.length() > 0) {
                    content = " ," + item;
                } else {
                    content = item;
                }
            }
            builder.append(content);
            builder.append(");\n");
        }
        return builder.toString();
    }

    private String compileFields() {
        StringBuilder builder = new StringBuilder();
        for (FieldDescriptor descriptor : fieldDescriptors) {
            String start = String.format("%s %s ", descriptor.name, descriptor.sqlType);
            if (descriptor.column.unqiue()) {
                start = start + "UNIQUE ";
            }
            if (descriptor.reference != null) {
                // TODO:
            }
            builder.append(start);
            builder.append(", \n");
        }
        return builder.toString();
    }

}
