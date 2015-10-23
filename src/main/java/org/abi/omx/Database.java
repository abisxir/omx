package org.abi.omx;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by abi on 18.05.15.
 */
public class Database {
    private static Database instance = new Database();
    private Context context;
    private List<Schema> schemas;
    private Map<String, Schema> schemasMap;

    private Database() {
    }

    public static Database getInstance() {
        return instance;
    }

    public void initialize(Context context) throws OmxException {
        if (this.context != null) {
            throw new OmxException("Database is already initialised.");
        }
        this.context = context;
        this.schemas = new ArrayList<Schema>();
        this.schemasMap = new HashMap<String, Schema>();
    }

    public Schema createSchema(String name) {
        Schema schema = this.schemasMap.get(name);
        if (schema == null) {
            schema = new DefaultSchema(context, name);
            this.schemas.add(schema);
            this.schemasMap.put(name, schema);
        }
        return schema;
    }

    public Schema getSchema(String name) throws OmxException {
        if (name == null) {
            if (this.schemas.size() == 0) {
                throw new OmxException("You need to create your schemas first.");
            }
            return this.schemas.get(0);
        }
        Schema schema = this.schemasMap.get(name);
        if (schema == null) {
            throw new OmxException(String.format("Schema [%s] not found.", name));
        }
        return schema;
    }

    public Schema getSchema() throws OmxException {
        return this.schemasMap.get(null);
    }

    public <T extends Entity> Store getStore(Class<T> cls) throws OmxException {
        return this.getStore(null, cls);
    }

    public <T extends Entity> Store getStore(String schemaName, Class<T> cls) throws OmxException {
        Schema schema = this.getSchema(schemaName);
        return schema.getStore(cls);
    }
}

class DefaultSchema extends Schema {
    public DefaultSchema(Context context, String name) {
        super(context, name);
    }
}
