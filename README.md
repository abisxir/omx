# omx
Lightweight Object Mapper for Android

The aim of project is to create a lightweight Object Mapper to ease working with SQLite database. One import issue is
that this library does not handle relation between objects so it is not an ORM. It simply helps developers to create
multiple SQLite database files (Schema). Then the developer can create several tables inside each schema. The project is
in progress and it is the first unstable :) release.

# Usage

* Installation
* Initialization
* Creating schemas
* Declaring entities
* Creating tables
* Saving records
* Fetching records
* Updating records
* Removing 
* Queries

To install the library download the source or use git to clone. You will need ant to build the jar file. Type the following
command and it will generate the jar file. Add the jar file to your dependencies and use the library as you want.

    > ant jar

To initialize the database just pass application context to initialize method.

    Database.getInstance().initialize(context);

After that you can create your schema by giving just a name. The result is a SQLite database file. It is going to add .db 
to the given name. As an instance, in this case you will have demo.db in databases directory.

    Schema schema = Database.getInstance().createSchema("demo");


Before creating your tables, you need to define each table as an entity class. The following code describes how you can 
declare an entity:

    @Table(name="customer")
    public class CustomerEntity extends Entity {
        @Column
        @PrimaryKey
        public int code;

        @Column(name="first_name")
        public String firstName;

        @Column(name="last_name")
        public String lastName;
    }

To create your tables just add this line after schema creation:

    schema.createTable(CustomerEntity.class);
    
Consider that the framework is not aware of changes to alter tables. It just creates a table when it is not already
created. Then if you want to force to create the table as you have some changes, drop the table first:

    schema.dropTable(CustomerEntity.class);

As you know, reflection is slow in non-scripting languages like Java or C#. To help the framework (it is optional) you
can build a factory (during table creation) that helps to instantiate entities.

    schema.createTable(CustomerEntity.class, new Entity.Factory() {
        @Override
        public Entity create() {
            return new CustomerEntity();
        }
    });

To manipulate database you need a store object. A store object creates, deletes, updates, gets, and queries the entities.
You can get store objects from schema:

    Store<CustomerEntity> store = schema.getStore(CustomerEntity.class);
    
You can directly get store objects from database too. If you have just one schema then you should code like this:

    Store<CustomerEntity> store = Database.getInstance().getStore(CustomerEntity.class);
    
Otherwise you need to specify the schema:

    Store<CustomerEntity> store = Database.getInstance().getStore("demo", CustomerEntity.class);
    
The following lines create a new customer. It would throw an exception, if the customer was already created.

    CustomerEntity entity = new CustomerEntity();

    entity.code = 1001;
    entity.firstName = "Savalan";
    entity.lastName = "X";

    store.create(entity);

If you call the save method, it first checks the previous record using primary key, if there is a customer with the 
defined code it will update the previous data, otherwise it will create a new one:

    store.save(entity);
    
To fetch the saved records you can call get method. As it supports multiple primary keys so you can pass more than one 
one parameter as primary keys according to the order of fields in you entity class.

    CustomerEntity e = store.get(1001);
    
In order to update records, just change the values and call the update method.

    e.lastName = "XY";
    store.update(e);
    
To remove records just call remove method like this:

    store.remove(e);
    
To enable transactions you need to begin a transaction and commit it to flush changes or rollback it to avoid data changes.
    
    Transaction trx = schema.begin();
    
    try {
        store.create(e);
        store.update(c);
        store.remove(d);
        
        ...
    
        trx.commit();
    } catch(Exception e) {
        trx.rollback();
        throw e;
    }
    
The query mechanism is simple. However, it depends you to the name of the database fields. You can access queries using
store objects or directly from schema:

    Query<CustomerEntity> query = store.query();
    QueryResult<CustomerEntity> results =
        query.select("*")
            .from("customer")
            .where("code > ?", 100)
            .orderBy("last_name asc")
            .getMany();
    for(CustomerEntity entity:results) {
        // Do what you want here
    }
    
You can get queries from schema objects and map the results to simple objects:

    Query<String> query = schema.query(String.class);
    query.select("last_name").from("customer").where("code < ?", 100).limit(5);
    long count = query.count();
    for(String lastName:query.getMany()) {
        // Your code...
    }
    
If the result is complicated then you need to create entities to map the result. The following code shows how to get 
last names and their count:
    
    @Table
    public class CustomerResult extends Entity {
        @Column(name="last_name")
        public String lastName;
        
        @Column(name="last_name_count")
        public int count;
    }
    
    Query<CustomerResult> query = 
        schema.query(CustomerResult.class)
            .select("last_name, count(*) as last_name_count")
            .from("customer")
            .groupBy("last_name")
            .orderBy("last_name_count asc");
            
     for(CustomerResult result:query.getMany()) {
        // Your code...
     }
     
Please consider that this the first release to just make the source available for developers. Many things need to 
change and before release 1.0.0 some classes are going to change. First, I am going to fix the reported bugs.

Have fun.