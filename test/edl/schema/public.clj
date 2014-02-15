(ns edl.schema.public
  (:require
    [edl.core :refer [load-schema]]))

;; Values of this map can be set with environ. The load-schema
;; macro evals the database connection map before using it.
(def db {:classname   "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname     "//localhost/schema_test"
         :user        "testuser"
         :password    "testpass"})

(load-schema db "public")

