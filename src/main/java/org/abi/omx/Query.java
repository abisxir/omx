package org.abi.omx;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by abi on 20.05.15.
 */
public abstract class Query<T> extends RawQuery<T> {

    private String fields;
    private String tables;
    private String criteria;
    private Object[] values;
    private String orders;
    private String groups;
    private Long offset;
    private Long limit;
    private String havingCriteria;
    private Object[] havingArgs;
    protected Query(Schema schema, Class<T> cls) {
        super(schema, cls);
    }

    public Query<T> select(String fields) {
        this.fields = fields;
        return this;
    }

    public Query<T> from(String tables) {
        this.tables = tables;
        return this;
    }

    public Query<T> where(String criteria, Object... values) {
        this.criteria = criteria;
        this.values = values;
        return this;
    }

    public Query<T> orderBy(String orders) {
        this.orders = orders;
        return this;
    }

    public Query<T> groupBy(String groups) {
        this.groups = groups;
        return this;
    }

    public Query<T> having(String havingCriteria, Object... havingArgs) {
        this.havingCriteria = havingCriteria;
        this.havingArgs = havingArgs;
        return this;
    }

    public Query<T> offset(long offset) {
        this.offset = offset;
        return this;
    }

    public Query<T> limit(long limit) {
        this.limit = limit;
        return this;
    }

    @Override
    protected String compile() {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("SELECT %s FROM %s ", fields, tables));
        if (criteria != null && criteria.length() > 0) {
            builder.append(String.format("WHERE %s", criteria));
        }

        if (this.groups != null && this.groups.length() > 0) {
            builder.append(String.format("\n\t group by %s", this.groups));
            if (this.havingCriteria != null && havingCriteria.length() > 0) {
                builder.append(String.format("having %s", havingCriteria));
            }
        }

        if (this.orders != null && this.orders.length() > 0) {
            builder.append(String.format("\n\t order by %s", this.orders));
        }

        if (this.limit == null && this.offset != null) {
            this.limit = Long.MAX_VALUE;
        }

        if(this.limit != null) {
            builder.append("\n LIMIT ? OFFSET ? ");
        }

        return builder.toString();
    }

    @Override
    public Iterable<Object> getValues() {
        List<Object> args = new ArrayList<Object>();

        if(values != null) {
            for (Object v : values) {
                args.add(v);
            }
        }

        if (groups != null) {
            if (havingArgs != null) {
                for (Object v : havingArgs) {
                    args.add(v);
                }
            }
        }

        if (this.limit == null && this.offset != null) {
            this.limit = Long.MAX_VALUE;
        }

        if (this.limit != null) {
            args.add(limit);
            if (this.offset == null) {
                this.offset = new Long(0);
            }
            args.add(this.offset);
        }

        return args;
    }

}
