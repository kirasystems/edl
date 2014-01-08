(ns edl.core
  (:require
    [clojure.java.jdbc :as j]
    [clojure.string :as string]
    [environ.core :refer [env]]
    [edl.schema :refer [get-schema denormalized-schema]]))

(defn- validate-table
  [schema table]
  (if-not (get schema table) (throw (Exception. (str "table " table " does not exist.")))))

(defn- validate-column
  [schema table column]
  (if-not (get-in schema [table :columns column])
    (throw (Exception. (str "no column " column " in table " table \.)))))

(defn- validate-fields
  [schema table fields]
  (cond
    (keyword? fields) (validate-column schema table (name fields))
    (vector? fields)  (doseq [c fields] (validate-column schema table (name c)))
    :else             (throw (Exception. (str "invalid field argument " fields)))))

(defn- generate-fields
  [fields]
  (cond
    (keyword? fields) (name fields)
    (vector? fields)  (string/join "," (map name fields))
    :else              "*"))

(defmacro get-record
  "Get a database record by primary key."
  [schema db table pkey [fields]]
  (let [table (name table)]
    (validate-table schema table)
    (when fields (validate-fields schema table fields))
    (let [pcol   (get-in schema [table :primary-key])
          q      (str "SELECT " (generate-fields fields) " FROM " table " WHERE " pcol " = ?")]
      (if (keyword? fields)
        `(~fields (first (j/query ~db [~q ~pkey])))
        `(first (j/query ~db [~q ~pkey]))))))

(defmacro load-schema
  [db schema-name]
  `(do
     (def ~'schema (denormalized-schema (get-schema ~db ~schema-name)))
     (defmacro ~'get-record
       [db# table# pkey# & fields#]
       `(get-record ~~'schema ~db# ~table# ~pkey# ~fields#))))
