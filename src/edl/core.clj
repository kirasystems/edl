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

(defn primary-where
  [schema table]
  (if-let [pkey (get-in schema [table :primary-key :column_name])]
    (str pkey " = ?")
    (throw (ex-info (str table " has no primary key") {:type :missing-primary-key :table table}))))

(defmacro get-record
  "Get a database record by primary key."
  [schema db table pkey [columns]]
  (let [table (name table)]
    (validate-table schema table)
    (when columns (validate-columns schema table columns))
    (let [q (str "SELECT " (generate-columns columns) " FROM " table " WHERE " (primary-where schema table))]
      `(first (j/query ~db [~q ~pkey])))))

(defmacro get-field
  "Get a database field by primary key and column name."
  [schema db table pkey column]
  (let [table (name table), col (name column)]
    (validate-table schema table)
    (validate-column schema table col)
    (let [q (str "SELECT " col " FROM " table " WHERE " (primary-where schema table))]
      `(~(keyword column) (first (j/query ~db [~q ~pkey]))))))

(defmacro update-record!
  "Update a database record by primary key."
  [schema db table pkey update-map]
  (let [table (name table)]
    (validate-table schema table)
    (validate-columns schema table (keys update-map))
    (let [where (primary-where schema table)]
      `(first (j/update! ~db ~(keyword table) ~update-map [~where ~pkey])))))

(defn table-map
  "Creates a map from a database table."
  [schema db table key-column val-column]
  (let [table (name table)
        kcol  (name (if (vector? key-column) (first key-column) key-column))
        vcol  (name (if (vector? val-column) (first val-column) val-column))]
    (validate-table schema table)
    (validate-column schema table kcol)
    (validate-column schema table vcol)
    (let [recs (j/query db [(str "SELECT " kcol ", " vcol " FROM " table)])
          keys (map (if (vector? key-column) (comp (second key-column) (first key-column)) key-column) recs)
          vals (map (if (vector? val-column) (comp (second val-column) (first val-column)) val-column) recs)]
      (zipmap keys vals))))

(defmacro defschema
  [symbol db schema-name]
  `(def ~symbol (denormalized-schema (get-schema ~db ~schema-name))))

(defmacro create-dml
  [symbol]
  `(do
     (defmacro ~'get-record
       [db# table# pkey# & columns#]
       `(get-record ~~symbol ~db# ~table# ~pkey# ~columns#))
     (defmacro ~'get-field
       [db# table# pkey# column#]
       `(get-field ~~symbol ~db# ~table# ~pkey# ~column#))
     (defmacro ~'update-record!
       [db# table# pkey# update-map#]
       `(update-record! ~~symbol ~db# ~table# ~pkey# ~update-map#))))
