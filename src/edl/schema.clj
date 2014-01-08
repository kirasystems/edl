(ns edl.schema
  (:require
    [clojure.java.jdbc :as j]
    [clojure.pprint :refer [pprint]]
    [clojure.set :refer [join index]]))

(defn get-table*
  "Get a table from the information_schema views corresponding to a particular schema."
  [db schema-name table]
  (j/query db [(str "SELECT * FROM information_schema." (name table) " WHERE table_schema = ?") schema-name]))

(def tables
  [:tables
   :table_constraints
   :columns
   :key_column_usage])

(defn get-schema
  "Get a schema from a database."
  [db schema-name]
  (reduce #(assoc %1 %2 (get-table* db schema-name %2)) {} tables))

(defn- primary-key-records
  [schema]
  (join
    (filter #(= "PRIMARY KEY" (:constraint_type %)) (:table_constraints schema))
    (:key_column_usage schema)))

(defn- primary-keys
  [schema]
  (let [pkeys (primary-key-records schema)]
    (zipmap (map :table_name pkeys) (map :column_name pkeys))))

(defn- column-map
  [t columns]
  (let [table-columns (filter #(= (:table_name t) (:table_name %)) columns)]
    (zipmap (map :column_name table-columns) table-columns)))

(defn- tables-with-columns
  [schema]
  (let [columns (:columns schema)]
    (for [t (:tables schema)] (assoc t :columns (column-map t columns)))))

(defn- add-primary-keys
  [schema tables]
  (let [pkeys (primary-keys schema)]
    (for [t tables]
      (assoc t :primary-key (get pkeys (:table_name t))))))

(defn denormalized-schema
  "Build a denormalized schema map with table names as first level keys."
  [schema]
  (let [tables (->> (tables-with-columns schema) (add-primary-keys schema))]
    (zipmap (map :table_name tables) tables)))
