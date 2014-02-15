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

(defn- validate-columns
  [schema table columns]
  (if (seq columns)  (doseq [c columns] (validate-column schema table (name c)))
    (throw (Exception. (str "invalid columns argument " columns)))))

(defn- generate-columns
  [columns]
  (if (seq columns) (string/join "," (map name columns)) "*"))

(defmacro get-record
  "Get a database record by primary key."
  [schema db table pkey [columns]]
  (let [table (name table)]
    (validate-table schema table)
    (when columns (validate-columns schema table columns))
    (let [pcol (get-in schema [table :primary-key :column_name])
          q    (str "SELECT " (generate-columns columns) " FROM " table " WHERE " pcol " = ?")]
      `(first (j/query ~db [~q ~pkey])))))

(defmacro get-field
  "Get a database field by primary key and column name."
  [schema db table pkey column]
  (let [table (name table), col (name column)]
    (validate-table schema table)
    (validate-column schema table col)
    (let [pcol (get-in schema [table :primary-key :column_name])
          q    (str "SELECT " col " FROM " table " WHERE " pcol " = ?")]
      `(~(keyword column) (first (j/query ~db [~q ~pkey]))))))

(defmacro update-record!
  "Update a database record by primary key."
  [schema db table pkey update-map]
  (let [table (name table)]
    (validate-table schema table)
    (validate-columns schema table (keys update-map))
    (let [pcol  (get-in schema [table :primary-key :column_name])
          where (str pcol " = ?")]
      `(first (j/update! ~db ~(keyword table) ~update-map [~where ~pkey])))))

(defmacro load-schema
  [db schema-name]
  `(do
     (def ~'schema (denormalized-schema (get-schema ~db ~schema-name)))
     (defmacro ~'get-record
       [db# table# pkey# & columns#]
       `(get-record ~~'schema ~db# ~table# ~pkey# ~columns#))
     (defmacro ~'get-field
       [db# table# pkey# column#]
       `(get-field ~~'schema ~db# ~table# ~pkey# ~column#))
     (defmacro ~'update-record!
       [db# table# pkey# update-map#]
       `(update-record! ~~'schema ~db# ~table# ~pkey# ~update-map#))))
