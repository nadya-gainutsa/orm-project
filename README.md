# orm-project

HW 27. Part 1
* Add JDBC Driver dependency
* Create a custom SessionFactory class that accepts DataSource and has a createSession method
* Create a custom Session class that accepts DataSource and has methods find(Class entityType, Object id) and close()
* Create custom annotations Table, Column
* Implement method find using JDBC API
* Introduce session cache
* Store loaded entities to the map when it’s loaded
* Don’t call the DB if entity with given id is already loaded, return value from the map instead

HW 27. Part 2
Upgrade your Session to introduce entity update based on “the dirty checking” mechanism

* Create another map that will store an entity snapshot copy (initial column values)
(This map should exist on the session level)
* On session close, compare current entity with its initial snapshot copy and if at least one field has changed, perform UPDATE statement
