# edl

A library supporting SQL schema inspection with a DSL for synthesis of common entity queries.

## Introduction

This library demonstrates how one can pull a schema from an SQL database using the
information_schema standard and use it to do compile time query synthesis and error checking.
At this time it is a proof of concept and subject to change.

Most popular SQL databases support information_schema tables. However, support varies. For
example, the H2 database does not support several common tables and instead defines it's
own non-standard information_schema.constraints table. This library was designed to work
with PostgreSQL.

Also note that do to the nature of compile time operation, this library poses challenges
for unit testing. In particular, fixtures that modify the database will not work as if
the required tables and columns for a query are not present, the query will not even
compile. Similarly, database connection information must be provided to load-schema in a
a way that is evalable. This means that you can use things like environ to pull in
sensitive information, but you cannot set it via command line arguments parsed by main.

The get-record macro takes a standard jdbc db var. The db-spec passed to load-schema
does not have to be the same as used in subsequent get-record calls. This way we retain
full support for jdbc and anything that works with it: connection pools, transactions,
persistent connections, etc.

## Examples

```clojure
;; Loaded in testdb.sql from project root
;; > postgres=# create database edl_testdb;
;; $ psql -Upostgres edl_testdb < ../edl/testdb.sql

;; Each schema is intended to be loaded into its own namespace.
;; The following assumes that your user is a superuser.

user=> (ns app.schema.public (:require [edl.core :as edl]))
nil

app.schema.public=> (def db "jdbc:postgresql://localhost:5432/edl_testdb")
#'app.schema.public/db

;; creates "schema" and "get-record" in current namespace
app.schema.public=> (edl/load-schema db "public")
#'app.schema.public/get-record

;; We can then use it from our other namespaces.
app.schema.public=> (ns app (:require [app.schema.public :as pub :refer [get-record db]]))
nil

;; Inspecting the schema.
app=> (:primary-key (get pub/schema "users"))
"id"

app=> (map first (:columns (get pub/schema "users")))
("email" "name" "id")

;; Synthesizing queries. Notice that the primary key is automatically discovered.
app=> (macroexpand '(get-record db :users 1))
(clojure.core/first (clojure.java.jdbc/query pub/db ["SELECT * FROM users WHERE id = ?" 1]))

app=> (get-record db :users 1)
{:email "mary@domain.com", :name "Mary Smith", :id 1}

;; Constrain fields.
app=> (get-record db :users 2 [:name :email])
{:email "john@domain.com", :name "John Doe"}

;; Compile time checking of database tables and columns.
app=> (get-record db :foo 1)

Exception table foo does not exist.  edl.core/validate-table (core.clj:10)

user=> (get-record db :users 1 [:name :email :phone])

Exception no column phone in table users.  edl.core/validate-column (core.clj:15)
```

## License

Copyright © 2014 DiligenceEngine Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
