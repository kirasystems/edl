(ns edl.schema.public
  (:require
    [edl.core :refer [defschema create-dml table-map]]))

;; Values of this map can be set with environ. The load-schema
;; macro evals the database connection map before using it.
(def db {:classname   "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname     "//localhost/schema_test"
         :user        "testuser"
         :password    "testpass"})

;; This operates *at-compile-time*. This way the schema is accessible to
;; all macros used later.
(defschema schema db "public")

;; Creates get-record, get-field, and update-record!
(create-dml schema)

;; Create a map from a database table.
(def categories (table-map schema db :categories [:description keyword] :id))