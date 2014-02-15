(ns edl.schema
  (:require
    [clojure.java.jdbc :as j]
    [clojure.pprint :refer [pprint]]
    [clojure.set :refer [join index]]))

(defn get-tables
  [dbm schema-name]
  (j/result-set-seq (.getTables dbm nil schema-name nil (into-array String ["TABLE"]))))

(defn get-columns
  [dbm schema-name]
  (j/result-set-seq (.getColumns dbm nil schema-name nil nil)))

(defn get-primary-keys
  [dbm schema-name]
  (j/result-set-seq (.getPrimaryKeys dbm nil schema-name nil)))

(defn get-schema
  "Get a schema from a database."
  [db schema-name]
  (with-open [c (j/get-connection db)]
    (let [dbm (.getMetaData c)]
      {:tables       (get-tables dbm schema-name)
       :columns      (get-columns dbm schema-name)
       :primary-keys (get-primary-keys dbm schema-name)})))

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
  (let [pkeys (:primary-keys schema)]
    (for [t tables]
      (assoc t :primary-key (some #(if (= (:table_name t) (:table_name %)) %) pkeys)))))

(defn denormalized-schema
  "Build a denormalized schema map with table names as first level keys."
  [schema]
  (let [tables (->> (tables-with-columns schema) (add-primary-keys schema))]
    (zipmap (map :table_name tables) tables)))

(comment
  (require '[clojure.java.jdbc :as j] '[clojure.pprint :refer [pprint]])
  (def conn (j/get-connection db))
  (def dbmeta (.getMetaData conn))
  (j/result-set-seq (.getSchemas dbmeta))
  (j/result-set-seq (.getTableTypes dbmeta))
  (j/result-set-seq (.getTables dbmeta nil "public" nil (into-array String ["TABLE"])))
  (pprint (j/result-set-seq (.getColumns dbmeta nil "public" nil nil)))
  (pprint (j/result-set-seq (.getPrimaryKeys dbmeta nil "public" nil)))

  (def schema (get-schema db "public"))
  (pprint (denormalized-schema schema))
  )
